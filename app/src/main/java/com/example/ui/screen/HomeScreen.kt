package com.example.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.component.AddEditScheduleDialog
import com.example.ui.component.ScheduleCalendarView
import com.example.ui.component.ScheduleTableView
import com.example.ui.component.SettingsDialog
import com.example.ui.component.VoiceAssistantOrb
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2
import com.example.ui.viewmodel.ScheduleFilter
import com.example.ui.viewmodel.ScheduleViewModel
import com.example.ui.viewmodel.ViewMode
import com.example.util.PersianDateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val filteredSchedules by viewModel.filteredSchedules.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val defaultAlarmMinutes by viewModel.defaultAlarmMinutes.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingItem by viewModel.editingItem.collectAsStateWithLifecycle()

    // Permission launchers
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        } else {
            Toast.makeText(context, "برای ضبط صدا مجوز میکروفون الزامی است", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.all { it }
        if (granted) {
            viewModel.syncAllUpcomingToGoogleCalendar()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Collect toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AmberPrimary)
                            )
                            Text(
                                text = "دستیار صوتی یادآور",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkLight
                            )
                        }
                        Text(
                            text = PersianDateUtil.getCurrentPersianDescription(),
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                        )
                    }
                },
                actions = {
                    // Google Calendar Sync All
                    IconButton(
                        onClick = {
                            val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                            val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                            if (writeGranted && readGranted) {
                                viewModel.syncAllUpcomingToGoogleCalendar()
                            } else {
                                calendarLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.WRITE_CALENDAR,
                                        Manifest.permission.READ_CALENDAR
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("sync_all_google_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "همگام‌سازی همه با تقویم گوگل",
                            tint = CyanAccent
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات کلید GapGPT",
                            tint = AmberPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = InkLight
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddEditDialog(true, null) },
                containerColor = AmberPrimary,
                contentColor = OnAmber,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن دستی برنامه کاری",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Voice Assistant Hero Card
            VoiceAssistantOrb(
                voiceState = voiceState,
                isAiProcessing = isAiProcessing,
                aiStatusMessage = aiStatusMessage,
                onStartVoice = {
                    val hasAudioPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasAudioPerm) {
                        viewModel.startVoiceListening()
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopVoice = { viewModel.stopVoiceListening() },
                onSubmitText = { viewModel.processUserInput(it) }
            )

            // View Mode Switcher (Table View vs Calendar View)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PanelRaised,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Calendar View Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewMode == ViewMode.CALENDAR) AmberPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setViewMode(ViewMode.CALENDAR) }
                            .testTag("tab_calendar_view")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (viewMode == ViewMode.CALENDAR) OnAmber else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "گاه‌شمار و تقویم",
                                fontSize = 13.sp,
                                fontWeight = if (viewMode == ViewMode.CALENDAR) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == ViewMode.CALENDAR) OnAmber else TextMuted
                            )
                        }
                    }

                    // Table View Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewMode == ViewMode.TABLE) AmberPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setViewMode(ViewMode.TABLE) }
                            .testTag("tab_table_view")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = if (viewMode == ViewMode.TABLE) OnAmber else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جدول زمان‌بندی",
                                fontSize = 13.sp,
                                fontWeight = if (viewMode == ViewMode.TABLE) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == ViewMode.TABLE) OnAmber else TextMuted
                            )
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ScheduleFilter.ALL to "همه",
                    ScheduleFilter.TODAY to "امروز",
                    ScheduleFilter.TOMORROW to "فردا",
                    ScheduleFilter.UPCOMING to "آینده",
                    ScheduleFilter.COMPLETED to "انجام‌شده"
                ).forEach { (filter, label) ->
                    val isSelected = activeFilter == filter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AmberPrimary.copy(alpha = 0.2f) else PanelBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AmberPrimary else BorderLine
                        ),
                        modifier = Modifier.clickable { viewModel.setFilter(filter) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AmberPrimary else TextMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Content Area: Calendar View or Table View
            when (viewMode) {
                ViewMode.CALENDAR -> {
                    ScheduleCalendarView(
                        schedules = filteredSchedules,
                        onToggleComplete = { viewModel.toggleCompleted(it) },
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onSyncGoogleCalendar = { viewModel.syncWithGoogleCalendar(it) },
                        onDelete = { viewModel.deleteSchedule(it) },
                        onEdit = { viewModel.setShowAddEditDialog(true, it) }
                    )
                }
                ViewMode.TABLE -> {
                    ScheduleTableView(
                        schedules = filteredSchedules,
                        onToggleComplete = { viewModel.toggleCompleted(it) },
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onSyncGoogleCalendar = { viewModel.syncWithGoogleCalendar(it) },
                        onDelete = { viewModel.deleteSchedule(it) },
                        onEdit = { viewModel.setShowAddEditDialog(true, it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = apiKey,
            currentDefaultAlarmMinutes = defaultAlarmMinutes,
            onSave = { key, mins ->
                viewModel.saveSettings(key, mins)
            },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    // Add / Edit Manual Schedule Dialog
    if (showAddEditDialog) {
        AddEditScheduleDialog(
            initialItem = editingItem,
            defaultAlarmMinutes = defaultAlarmMinutes,
            onSave = { id, title, type, dateTimeMillis, duration, notes, reminderMins, isAlarm ->
                viewModel.saveManualSchedule(
                    id = id,
                    title = title,
                    type = type,
                    dateTimeMillis = dateTimeMillis,
                    durationMinutes = duration,
                    notes = notes,
                    reminderMinutesBefore = reminderMins,
                    isAlarmEnabled = isAlarm
                )
            },
            onDismiss = { viewModel.setShowAddEditDialog(false) }
        )
    }
}
