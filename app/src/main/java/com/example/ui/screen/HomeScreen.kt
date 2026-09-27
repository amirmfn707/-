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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.alarm.AlarmHelper
import com.example.ui.component.AddEditScheduleDialog
import com.example.ui.component.IosPwaDialog
import com.example.ui.component.ScheduleCalendarView
import com.example.ui.component.ScheduleTableView
import com.example.ui.component.SettingsDialog
import com.example.ui.component.VoiceAssistantOrb
import com.example.ui.component.WordPressManagerDialog
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2
import com.example.ui.viewmodel.ScheduleFilter
import com.example.ui.viewmodel.ScheduleTypeFilter
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
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val typeCounts by viewModel.typeCounts.collectAsStateWithLifecycle()
    val filteredSchedules by viewModel.filteredSchedules.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val defaultAlarmMinutes by viewModel.defaultAlarmMinutes.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingItem by viewModel.editingItem.collectAsStateWithLifecycle()

    val showWordPressDialog by viewModel.showWordPressDialog.collectAsStateWithLifecycle()
    val wpConnectionStatus by viewModel.wpConnectionStatus.collectAsStateWithLifecycle()
    val wpPosts by viewModel.wpPosts.collectAsStateWithLifecycle()
    val wcOrders by viewModel.wcOrders.collectAsStateWithLifecycle()
    val wcSalesReport by viewModel.wcSalesReport.collectAsStateWithLifecycle()
    val isWpLoading by viewModel.isWpLoading.collectAsStateWithLifecycle()

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
        AlarmHelper.createNotificationChannel(context)
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

    var showIosDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PanelRaised)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "آرم برنامه",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = "دستیار صوتی ویرا",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkLight
                            )
                        }
                        Text(
                            text = PersianDateUtil.getCurrentPersianDescription(),
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 42.dp, top = 2.dp)
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

                    // iOS / Web PWA Button
                    IconButton(
                        onClick = { showIosDialog = true },
                        modifier = Modifier.testTag("ios_pwa_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = "نسخه آیفون و تحت وب",
                            tint = CyanAccent
                        )
                    }

                    // WordPress & WooCommerce Manager Button
                    IconButton(
                        onClick = { viewModel.setShowWordPressDialog(true) },
                        modifier = Modifier.testTag("wordpress_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "مدیریت وردپرس و ووکامرس",
                            tint = if (viewModel.isWordPressConfigured) EmeraldAccent else CyanAccent
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
                    ScheduleFilter.ALL to "همه زمان‌ها",
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
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AmberPrimary else TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Task Type Filter Chips Row (Distinct behaviors & categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val typeChips = listOf(
                    Triple(ScheduleTypeFilter.ALL, "همه نوع", AmberPrimary),
                    Triple(ScheduleTypeFilter.MEETING, "💼 جلسات کاری (${typeCounts[ScheduleTypeFilter.MEETING] ?: 0})", CyanAccent),
                    Triple(ScheduleTypeFilter.REMINDER, "⏰ یادآوری‌ها (${typeCounts[ScheduleTypeFilter.REMINDER] ?: 0})", CoralAccent),
                    Triple(ScheduleTypeFilter.TASK, "📝 وظایف (${typeCounts[ScheduleTypeFilter.TASK] ?: 0})", EmeraldAccent),
                    Triple(ScheduleTypeFilter.EVENT, "🎪 رویدادها (${typeCounts[ScheduleTypeFilter.EVENT] ?: 0})", Color(0xFFC084FC))
                )
                typeChips.forEach { (typeFilter, label, accentColor) ->
                    val isSelected = selectedTypeFilter == typeFilter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.22f) else PanelRaised.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else BorderLine
                        ),
                        modifier = Modifier.clickable { viewModel.setTypeFilter(typeFilter) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else TextMuted,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
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

    // iOS PWA Information & Share Dialog
    if (showIosDialog) {
        IosPwaDialog(
            initialPwaUrl = viewModel.pwaUrl,
            onSavePwaUrl = { viewModel.savePwaUrl(it) },
            onResetPwaUrl = { viewModel.resetPwaUrl() },
            onDismissRequest = { showIosDialog = false }
        )
    }

    // WordPress & WooCommerce Management Dialog
    if (showWordPressDialog) {
        WordPressManagerDialog(
            initialSiteUrl = viewModel.wpSiteUrl,
            initialUsername = viewModel.wpUsername,
            initialAppPassword = viewModel.wpAppPassword,
            initialConsumerKey = viewModel.wcConsumerKey,
            initialConsumerSecret = viewModel.wcConsumerSecret,
            connectionStatus = wpConnectionStatus,
            orders = wcOrders,
            posts = wpPosts,
            salesReport = wcSalesReport,
            isLoading = isWpLoading,
            onSaveConfig = { url, user, pass, ck, cs ->
                viewModel.saveWordPressConfig(url, user, pass, ck, cs)
            },
            onRefresh = { viewModel.loadWordPressData() },
            onCreatePost = { title, content, status, scheduledDateIso ->
                viewModel.createWordPressPost(title, content, status, scheduledDateIso)
            },
            onCreateCoupon = { code, amount, type ->
                viewModel.createWooCoupon(code, amount, type)
            },
            onUpdateOrderStatus = { orderId, status ->
                viewModel.updateOrderStatus(orderId, status)
            },
            onAddOrderToSchedule = { order ->
                viewModel.addOrderToSchedule(order)
            },
            onDismiss = { viewModel.setShowWordPressDialog(false) }
        )
    }
}
