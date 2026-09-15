package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GapGptService
import com.example.data.db.AppDatabase
import com.example.data.model.ScheduleItem
import com.example.data.pref.AppPreferences
import com.example.data.repository.ScheduleRepository
import com.example.util.PersianDateUtil
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ViewMode {
    TABLE,     // جدول زمان‌بندی
    CALENDAR   // گاه‌شمار / لیست تقویم
}

enum class ScheduleFilter {
    ALL,
    TODAY,
    TOMORROW,
    UPCOMING,
    COMPLETED
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val gapGptService = GapGptService(getApiKey = { prefs.apiKey })
    val repository = ScheduleRepository(application, database.scheduleDao(), gapGptService)
    val voiceManager = VoiceManager(application)

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    private val _viewMode = MutableStateFlow(ViewMode.CALENDAR)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _activeFilter = MutableStateFlow(ScheduleFilter.ALL)
    val activeFilter: StateFlow<ScheduleFilter> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.apiKey)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _defaultAlarmMinutes = MutableStateFlow(prefs.defaultAlarmMinutes)
    val defaultAlarmMinutes: StateFlow<Int> = _defaultAlarmMinutes.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(!prefs.hasApiKey())
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _editingItem = MutableStateFlow<ScheduleItem?>(null)
    val editingItem: StateFlow<ScheduleItem?> = _editingItem.asStateFlow()

    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Filtered and searched schedules stream
    val filteredSchedules: StateFlow<List<ScheduleItem>> = combine(
        repository.allSchedules,
        _activeFilter,
        _searchQuery
    ) { schedules, filter, query ->
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startOfTomorrow = Calendar.getInstance().apply {
            timeInMillis = endOfToday.timeInMillis + 1
        }
        val endOfTomorrow = Calendar.getInstance().apply {
            timeInMillis = startOfTomorrow.timeInMillis + (24 * 60 * 60 * 1000) - 1
        }

        schedules.filter { item ->
            // Filter by completion / date
            val matchesFilter = when (filter) {
                ScheduleFilter.ALL -> true
                ScheduleFilter.TODAY -> {
                    item.dateTimeMillis != null &&
                            item.dateTimeMillis in startOfToday.timeInMillis..endOfToday.timeInMillis
                }
                ScheduleFilter.TOMORROW -> {
                    item.dateTimeMillis != null &&
                            item.dateTimeMillis in startOfTomorrow.timeInMillis..endOfTomorrow.timeInMillis
                }
                ScheduleFilter.UPCOMING -> {
                    !item.isCompleted && (item.dateTimeMillis == null || item.dateTimeMillis >= startOfToday.timeInMillis)
                }
                ScheduleFilter.COMPLETED -> item.isCompleted
            }

            // Search query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        (item.notes?.contains(query, ignoreCase = true) == true)
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe voice recognizer success
        viewModelScope.launch {
            voiceManager.voiceState.collect { state ->
                if (state is VoiceState.Success) {
                    processUserInput(state.recognizedText)
                    voiceManager.resetState()
                }
            }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setFilter(filter: ScheduleFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowAddEditDialog(show: Boolean, item: ScheduleItem? = null) {
        _editingItem.value = item
        _showAddEditDialog.value = show
    }

    fun saveSettings(newApiKey: String, defaultAlarmMins: Int) {
        prefs.apiKey = newApiKey
        prefs.defaultAlarmMinutes = defaultAlarmMins
        _apiKey.value = newApiKey
        _defaultAlarmMinutes.value = defaultAlarmMins
        _showSettingsDialog.value = false
        emitToast("تنظیمات با موفقیت ذخیره شد")
    }

    fun startVoiceListening() {
        if (!prefs.hasApiKey()) {
            _showSettingsDialog.value = true
            emitToast("ابتدا کلید API پلتفرم GapGPT را وارد کنید")
            return
        }
        voiceManager.startListening("fa-IR")
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    fun processUserInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        if (!prefs.hasApiKey()) {
            _showSettingsDialog.value = true
            emitToast("لطفاً ابتدا کلید API را در تنظیمات وارد نمایید.")
            return
        }

        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "در حال درک و زمان‌بندی با هوش مصنوعی…"

            val result = repository.parseVoiceWithAI(trimmed, prefs.aiModel)

            result.onSuccess { parsed ->
                val newItem = ScheduleItem(
                    title = parsed.title,
                    type = parsed.type,
                    dateTimeMillis = parsed.dateTimeMillis,
                    durationMinutes = parsed.durationMinutes,
                    notes = parsed.notes,
                    reminderMinutesBefore = if (parsed.reminderMinutesBefore > 0) parsed.reminderMinutesBefore else prefs.defaultAlarmMinutes,
                    isAlarmEnabled = parsed.dateTimeMillis != null,
                    originalVoiceTranscript = trimmed
                )

                val id = repository.insertSchedule(newItem)
                _isAiProcessing.value = false
                _aiStatusMessage.value = null

                val timeStr = if (parsed.dateTimeMillis != null) {
                    "برای ${PersianDateUtil.formatPersianDateTime(parsed.dateTimeMillis)}"
                } else {
                    "بدون زمان مشخص"
                }
                emitToast("ثبت شد: ${parsed.title} ($timeStr)")
            }.onFailure { err ->
                _isAiProcessing.value = false
                _aiStatusMessage.value = null
                val message = err.message ?: "خطا در پردازش هوش مصنوعی"
                emitToast(message)
                if (message.contains("401") || message.contains("کلید")) {
                    _showSettingsDialog.value = true
                }
            }
        }
    }

    fun toggleCompleted(item: ScheduleItem) {
        viewModelScope.launch {
            repository.toggleCompleted(item)
            val statusStr = if (!item.isCompleted) "انجام شد" else "بازگردانده شد"
            emitToast("${item.title}: $statusStr")
        }
    }

    fun toggleAlarm(item: ScheduleItem) {
        viewModelScope.launch {
            repository.toggleAlarm(item)
            val statusStr = if (!item.isAlarmEnabled) "هشدار فعال شد" else "هشدار غیرفعال شد"
            emitToast(statusStr)
        }
    }

    fun deleteSchedule(item: ScheduleItem) {
        viewModelScope.launch {
            repository.deleteSchedule(item)
            emitToast("برنامه '${item.title}' حذف شد")
        }
    }

    fun saveManualSchedule(
        id: Long = 0,
        title: String,
        type: String,
        dateTimeMillis: Long?,
        durationMinutes: Int,
        notes: String?,
        reminderMinutesBefore: Int,
        isAlarmEnabled: Boolean
    ) {
        viewModelScope.launch {
            val item = ScheduleItem(
                id = id,
                title = title.ifBlank { "برنامه کاری" },
                type = type,
                dateTimeMillis = dateTimeMillis,
                durationMinutes = durationMinutes,
                notes = notes,
                reminderMinutesBefore = reminderMinutesBefore,
                isAlarmEnabled = isAlarmEnabled && dateTimeMillis != null
            )
            if (id > 0) {
                repository.updateSchedule(item)
                emitToast("برنامه ویرایش شد")
            } else {
                repository.insertSchedule(item)
                emitToast("برنامه کاری با موفقیت اضافه شد")
            }
            _showAddEditDialog.value = false
            _editingItem.value = null
        }
    }

    fun syncWithGoogleCalendar(item: ScheduleItem) {
        viewModelScope.launch {
            val success = repository.syncWithGoogleCalendar(item)
            if (success) {
                emitToast("رویداد به تقویم گوگل اضافه شد")
            } else {
                emitToast("خطا در همگام‌سازی با تقویم گوگل")
            }
        }
    }

    fun syncAllUpcomingToGoogleCalendar() {
        viewModelScope.launch {
            val currentList = filteredSchedules.value
            val syncedCount = repository.syncAllUpcomingToGoogleCalendar(currentList)
            if (syncedCount > 0) {
                emitToast("$syncedCount رویداد با تقویم گوگل همگام‌سازی شد")
            } else {
                emitToast("رویداد جدیدی برای همگام‌سازی مستقیم یافت نشد یا دسترسی تقویم لازم است.")
            }
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.stopListening()
    }
}
