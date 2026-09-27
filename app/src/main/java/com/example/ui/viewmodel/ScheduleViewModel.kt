package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GapGptService
import com.example.data.api.WordPressService
import com.example.data.db.AppDatabase
import com.example.data.model.*
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

enum class ScheduleTypeFilter(val title: String) {
    ALL("همه نوع"),
    MEETING("جلسات کاری"),
    REMINDER("یادآوری‌ها"),
    TASK("وظایف و کارها"),
    EVENT("رویدادها")
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val gapGptService = GapGptService(getApiKey = { prefs.apiKey })
    val repository = ScheduleRepository(application, database.scheduleDao(), gapGptService)
    val voiceManager = VoiceManager(application)

    val wordPressService = WordPressService(
        getSiteUrl = { prefs.wpSiteUrl },
        getUsername = { prefs.wpUsername },
        getAppPassword = { prefs.wpAppPassword },
        getConsumerKey = { prefs.wcConsumerKey },
        getConsumerSecret = { prefs.wcConsumerSecret }
    )

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    private val _viewMode = MutableStateFlow(ViewMode.CALENDAR)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _activeFilter = MutableStateFlow(ScheduleFilter.ALL)
    val activeFilter: StateFlow<ScheduleFilter> = _activeFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow(ScheduleTypeFilter.ALL)
    val selectedTypeFilter: StateFlow<ScheduleTypeFilter> = _selectedTypeFilter.asStateFlow()

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

    // WordPress & WooCommerce States
    private val _showWordPressDialog = MutableStateFlow(false)
    val showWordPressDialog: StateFlow<Boolean> = _showWordPressDialog.asStateFlow()

    private val _wpConnectionStatus = MutableStateFlow<WpConnectionStatus?>(null)
    val wpConnectionStatus: StateFlow<WpConnectionStatus?> = _wpConnectionStatus.asStateFlow()

    private val _wpPosts = MutableStateFlow<List<WpPost>>(emptyList())
    val wpPosts: StateFlow<List<WpPost>> = _wpPosts.asStateFlow()

    private val _wcOrders = MutableStateFlow<List<WcOrder>>(emptyList())
    val wcOrders: StateFlow<List<WcOrder>> = _wcOrders.asStateFlow()

    private val _wcSalesReport = MutableStateFlow<WcSalesReport?>(null)
    val wcSalesReport: StateFlow<WcSalesReport?> = _wcSalesReport.asStateFlow()

    private val _isWpLoading = MutableStateFlow(false)
    val isWpLoading: StateFlow<Boolean> = _isWpLoading.asStateFlow()

    val wpSiteUrl: String get() = prefs.wpSiteUrl
    val wpUsername: String get() = prefs.wpUsername
    val wpAppPassword: String get() = prefs.wpAppPassword
    val wcConsumerKey: String get() = prefs.wcConsumerKey
    val wcConsumerSecret: String get() = prefs.wcConsumerSecret
    val isWordPressConfigured: Boolean get() = prefs.isWordPressConfigured()

    val pwaUrl: String get() = prefs.pwaUrl

    fun savePwaUrl(newUrl: String) {
        prefs.pwaUrl = newUrl.trim()
        emitToast("✅ آدرس PWA وب‌اپلیکیشن ذخیره شد.")
    }

    fun resetPwaUrl() {
        prefs.pwaUrl = AppPreferences.DEFAULT_PWA_URL
        emitToast("آدرس PWA به مقدار اولیه ابری بازنشانی شد.")
    }

    // Counts for each type
    val typeCounts: StateFlow<Map<ScheduleTypeFilter, Int>> = repository.allSchedules.combine(_activeFilter) { schedules, filter ->
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

        val baseList = schedules.filter { item ->
            when (filter) {
                ScheduleFilter.ALL -> true
                ScheduleFilter.TODAY -> item.dateTimeMillis != null && item.dateTimeMillis in startOfToday.timeInMillis..endOfToday.timeInMillis
                ScheduleFilter.TOMORROW -> item.dateTimeMillis != null && item.dateTimeMillis in startOfTomorrow.timeInMillis..endOfTomorrow.timeInMillis
                ScheduleFilter.UPCOMING -> !item.isCompleted && (item.dateTimeMillis == null || item.dateTimeMillis >= startOfToday.timeInMillis)
                ScheduleFilter.COMPLETED -> item.isCompleted
            }
        }

        mapOf(
            ScheduleTypeFilter.ALL to baseList.size,
            ScheduleTypeFilter.MEETING to baseList.count { it.type.equals(ScheduleItem.TYPE_MEETING, ignoreCase = true) },
            ScheduleTypeFilter.REMINDER to baseList.count { it.type.equals(ScheduleItem.TYPE_REMINDER, ignoreCase = true) },
            ScheduleTypeFilter.TASK to baseList.count { it.type.equals(ScheduleItem.TYPE_TASK, ignoreCase = true) },
            ScheduleTypeFilter.EVENT to baseList.count { it.type.equals(ScheduleItem.TYPE_EVENT, ignoreCase = true) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered and searched schedules stream
    val filteredSchedules: StateFlow<List<ScheduleItem>> = combine(
        repository.allSchedules,
        _activeFilter,
        _selectedTypeFilter,
        _searchQuery
    ) { schedules, filter, typeFilter, query ->
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

            // Filter by type
            val matchesType = when (typeFilter) {
                ScheduleTypeFilter.ALL -> true
                ScheduleTypeFilter.MEETING -> item.type.equals(ScheduleItem.TYPE_MEETING, ignoreCase = true)
                ScheduleTypeFilter.REMINDER -> item.type.equals(ScheduleItem.TYPE_REMINDER, ignoreCase = true)
                ScheduleTypeFilter.TASK -> item.type.equals(ScheduleItem.TYPE_TASK, ignoreCase = true)
                ScheduleTypeFilter.EVENT -> item.type.equals(ScheduleItem.TYPE_EVENT, ignoreCase = true)
            }

            // Search query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        (item.notes?.contains(query, ignoreCase = true) == true)
            }

            matchesFilter && matchesType && matchesQuery
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

    fun setTypeFilter(filter: ScheduleTypeFilter) {
        _selectedTypeFilter.value = filter
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

            val result = repository.parseVoiceSchedulesWithAI(trimmed, prefs.aiModel)

            result.onSuccess { parsedList ->
                if (parsedList.isEmpty()) {
                    _isAiProcessing.value = false
                    _aiStatusMessage.value = null
                    emitToast("برنامه‌ای در پیام شما استخراج نشد.")
                    return@onSuccess
                }

                val insertedSummary = mutableListOf<String>()
                var alarmCount = 0

                for (parsed in parsedList) {
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

                    repository.insertSchedule(newItem)
                    if (newItem.isAlarmEnabled) {
                        alarmCount++
                    }

                    val timeLabel = if (parsed.dateTimeMillis != null) {
                        PersianDateUtil.formatPersianTimeOnly(parsed.dateTimeMillis)
                    } else "بدون زمان"
                    insertedSummary.add("${parsed.title} ($timeLabel)")
                }

                _isAiProcessing.value = false
                _aiStatusMessage.value = null

                // Check for WordPress & WooCommerce Voice Actions
                val wpActionItem = parsedList.firstOrNull { it.actionType != null }
                if (wpActionItem != null && wpActionItem.actionType != null) {
                    when (wpActionItem.actionType) {
                        "wp_create_post" -> {
                            if (!isWordPressConfigured) {
                                _showWordPressDialog.value = true
                                emitToast("برای ثبت مقاله ابتدا مشخصات سایت وردپرس را در بخش تنظیمات وارد کنید.")
                            } else {
                                val pTitle = wpActionItem.postTitle ?: wpActionItem.title
                                val pContent = wpActionItem.postContent ?: "محتوای تولید شده توسط دستیار صوتی ویرا."
                                val pStatus = wpActionItem.postStatus ?: "draft"
                                val pDateIso = wpActionItem.postPublishIsoDateTime
                                viewModelScope.launch {
                                    val postRes = wordPressService.createPost(
                                        title = pTitle,
                                        content = pContent,
                                        status = pStatus,
                                        excerpt = null,
                                        scheduledDateIso = pDateIso
                                    )
                                    postRes.onSuccess { post ->
                                        val statusDesc = when (post.status) {
                                            "publish" -> "مستقیماً در سایت منتشر شد! 🚀"
                                            "future" -> "برای انتشار در تاریخ ${pDateIso ?: "مشخص شده"} زمان‌بندی شد! ⏰"
                                            else -> "به عنوان پیش‌نویس در سایت ثبت شد 📝"
                                        }
                                        emitToast("✅ مقاله «${post.title}» $statusDesc")
                                        loadWordPressData()
                                        _showWordPressDialog.value = true
                                    }.onFailure { err ->
                                        emitToast("خطا در ایجاد پست در وردپرس: ${err.message}")
                                    }
                                }
                            }
                        }
                        "wc_get_orders" -> {
                            if (!isWordPressConfigured) {
                                _showWordPressDialog.value = true
                                emitToast("برای مشاهده سفارش‌ها ابتدا تنظیمات ووکامرس را وارد کنید.")
                            } else {
                                loadWordPressData()
                                _showWordPressDialog.value = true
                                emitToast("در حال دریافت آخرین سفارش‌های ووکامرس…")
                            }
                        }
                        "wc_sales_report" -> {
                            if (!isWordPressConfigured) {
                                _showWordPressDialog.value = true
                                emitToast("برای دریافت گزارش فروش ابتدا تنظیمات ووکامرس را وارد کنید.")
                            } else {
                                loadWordPressData()
                                _showWordPressDialog.value = true
                                emitToast("در حال استعلام گزارش فروش ووکامرس…")
                            }
                        }
                        "wc_create_coupon" -> {
                            if (!isWordPressConfigured) {
                                _showWordPressDialog.value = true
                                emitToast("برای ساخت کد تخفیف ابتدا تنظیمات ووکامرس را وارد کنید.")
                            } else {
                                val cCode = wpActionItem.couponCode ?: "OFFER"
                                val cAmount = wpActionItem.couponAmount ?: "15"
                                viewModelScope.launch {
                                    val cRes = wordPressService.createCoupon(cCode, cAmount)
                                    cRes.onSuccess { cp ->
                                        emitToast("🎉 کد تخفیف ${cp.code} با مبلغ/درصد ${cp.amount} در ووکامرس ایجاد شد!")
                                        _showWordPressDialog.value = true
                                    }.onFailure { err ->
                                        emitToast("خطا در ایجاد کد تخفیف: ${err.message}")
                                    }
                                }
                            }
                        }
                    }
                }

                if (parsedList.size == 1) {
                    val single = parsedList[0]
                    if (single.actionType == null) {
                        val timeStr = if (single.dateTimeMillis != null) {
                            "برای ${PersianDateUtil.formatPersianDateTime(single.dateTimeMillis)}"
                        } else {
                            "بدون زمان مشخص"
                        }
                        val alarmNotice = if (single.dateTimeMillis != null) " • ⏰ آلارم فعال شد" else ""
                        emitToast("ثبت شد: ${single.title} ($timeStr)$alarmNotice")
                    }
                } else {
                    val alarmNotice = if (alarmCount > 0) " (⏰ $alarmCount آلارم فعال شد)" else ""
                    emitToast("✅ ${PersianDateUtil.toPersianDigits(parsedList.size.toString())} برنامه کاری ثبت شد$alarmNotice:\n${insertedSummary.joinToString(" • ")}")
                }
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
            val statusStr = if (!item.isAlarmEnabled) {
                if (item.dateTimeMillis != null) {
                    val timeStr = PersianDateUtil.formatPersianDateTime(item.dateTimeMillis)
                    "⏰ هشدار برای $timeStr فعال شد"
                } else {
                    "⏰ هشدار فعال شد"
                }
            } else {
                "🔕 هشدار غیرفعال شد"
            }
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

    fun setShowWordPressDialog(show: Boolean) {
        _showWordPressDialog.value = show
        if (show && isWordPressConfigured) {
            loadWordPressData()
        }
    }

    fun saveWordPressConfig(
        siteUrl: String,
        username: String,
        appPassword: String,
        consumerKey: String,
        consumerSecret: String
    ) {
        prefs.wpSiteUrl = siteUrl
        prefs.wpUsername = username
        prefs.wpAppPassword = appPassword
        prefs.wcConsumerKey = consumerKey
        prefs.wcConsumerSecret = consumerSecret
        emitToast("تنظیمات وردپرس و ووکامرس ذخیره شد.")
        testWordPressConnection()
    }

    fun testWordPressConnection() {
        viewModelScope.launch {
            _isWpLoading.value = true
            val res = wordPressService.testConnection()
            _isWpLoading.value = false
            res.onSuccess { status ->
                _wpConnectionStatus.value = status
                emitToast("✅ اتصال به سایت وردپرس (${status.siteName}) برقرار شد.")
                loadWordPressData()
            }.onFailure { err ->
                _wpConnectionStatus.value = WpConnectionStatus(
                    isConnected = false,
                    errorMessage = err.message
                )
                emitToast("❌ خطا در اتصال: ${err.message}")
            }
        }
    }

    fun loadWordPressData() {
        viewModelScope.launch {
            if (!isWordPressConfigured) return@launch
            _isWpLoading.value = true

            // Fetch orders
            val ordersRes = wordPressService.getRecentOrders()
            ordersRes.onSuccess { _wcOrders.value = it }

            // Fetch posts
            val postsRes = wordPressService.getRecentPosts()
            postsRes.onSuccess { _wpPosts.value = it }

            // Fetch sales report
            val salesRes = wordPressService.getSalesReport()
            salesRes.onSuccess { _wcSalesReport.value = it }

            _isWpLoading.value = false
        }
    }

    fun createWordPressPost(
        title: String,
        content: String,
        status: String = "draft",
        scheduledDateIso: String? = null
    ) {
        viewModelScope.launch {
            _isWpLoading.value = true
            val res = wordPressService.createPost(
                title = title,
                content = content,
                status = status,
                excerpt = null,
                scheduledDateIso = scheduledDateIso
            )
            _isWpLoading.value = false
            res.onSuccess { post ->
                val desc = when (post.status) {
                    "publish" -> "منتشر شد 🚀"
                    "future" -> "برای تاریخ ${scheduledDateIso ?: "مشخص شده"} زمان‌بندی شد ⏰"
                    else -> "به عنوان پیش‌نویس ذخیره شد 📝"
                }
                emitToast("✅ پست «${post.title}» در سایت $desc")
                loadWordPressData()
            }.onFailure { err ->
                emitToast("خطا در ایجاد پست: ${err.message}")
            }
        }
    }

    fun createWooCoupon(code: String, amount: String, discountType: String = "percent") {
        viewModelScope.launch {
            _isWpLoading.value = true
            val res = wordPressService.createCoupon(code, amount, discountType)
            _isWpLoading.value = false
            res.onSuccess { coupon ->
                emitToast("🎉 کد تخفیف ${coupon.code} با موفقیت در ووکامرس ساخته شد.")
            }.onFailure { err ->
                emitToast("خطا در ایجاد کد تخفیف: ${err.message}")
            }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch {
            _isWpLoading.value = true
            val res = wordPressService.updateOrderStatus(orderId, newStatus)
            _isWpLoading.value = false
            res.onSuccess {
                emitToast("وضعیت سفارش تغییر یافت.")
                loadWordPressData()
            }.onFailure { err ->
                emitToast("خطا در به‌روزرسانی سفارش: ${err.message}")
            }
        }
    }

    fun addOrderToSchedule(order: WcOrder) {
        viewModelScope.launch {
            val title = "📦 ارسال سفارش #${order.number} (${order.customerName})"
            val notes = "مبلغ: ${order.total} ${order.currency}\nاقلام: ${order.itemsSummary}\nآدرس: ${order.shippingAddress ?: "ثبت نشده"}\nتلفن: ${order.customerPhone ?: "ثبت نشده"}"
            val item = ScheduleItem(
                title = title,
                type = ScheduleItem.TYPE_TASK,
                dateTimeMillis = System.currentTimeMillis() + 3600_000,
                durationMinutes = 30,
                notes = notes,
                reminderMinutesBefore = prefs.defaultAlarmMinutes,
                isAlarmEnabled = true
            )
            repository.insertSchedule(item)
            emitToast("سفارش #${order.number} به یادآوری‌های کاری شما اضافه شد.")
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
