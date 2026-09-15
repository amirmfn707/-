package com.example.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleItem
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2
import com.example.util.PersianDateUtil

@Composable
fun ScheduleCalendarView(
    schedules: List<ScheduleItem>,
    onToggleComplete: (ScheduleItem) -> Unit,
    onToggleAlarm: (ScheduleItem) -> Unit,
    onSyncGoogleCalendar: (ScheduleItem) -> Unit,
    onDelete: (ScheduleItem) -> Unit,
    onEdit: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (schedules.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = TextMuted2,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هنوز برنامه‌ای در این لیست ثبت نشده است.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Text(
                    text = "می‌توانید به صورت صوتی بگویید تا هوش مصنوعی آن را زمان‌بندی کند.",
                    color = TextMuted2,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    val order = listOf("امروز", "فردا", "پس‌فردا", "این هفته", "آینده", "بدون زمان", "گذشته")
    val grouped = schedules.groupBy { PersianDateUtil.getRelativeDayLabel(it.dateTimeMillis) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        order.forEach { groupKey ->
            val itemsInGroup = grouped[groupKey]
            if (!itemsInGroup.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (groupKey) {
                                            "امروز" -> AmberPrimary
                                            "فردا" -> CyanAccent
                                            "این هفته" -> EmeraldAccent
                                            else -> TextMuted2
                                        }
                                    )
                            )
                            Text(
                                text = groupKey,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkLight
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PanelRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                        ) {
                            Text(
                                text = PersianDateUtil.toPersianDigits("${itemsInGroup.size} مورد"),
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Cards in group
                    itemsInGroup.forEach { item ->
                        ScheduleItemCard(
                            item = item,
                            onToggleComplete = { onToggleComplete(item) },
                            onToggleAlarm = { onToggleAlarm(item) },
                            onSyncGoogleCalendar = { onSyncGoogleCalendar(item) },
                            onDelete = { onDelete(item) },
                            onEdit = { onEdit(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    item: ScheduleItem,
    onToggleComplete: () -> Unit,
    onToggleAlarm: () -> Unit,
    onSyncGoogleCalendar: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (item.type.lowercase()) {
        ScheduleItem.TYPE_MEETING -> CyanAccent
        ScheduleItem.TYPE_REMINDER -> CoralAccent
        ScheduleItem.TYPE_EVENT -> EmeraldAccent
        else -> AmberPrimary
    }

    val countdown = PersianDateUtil.getCountdownLabel(item.dateTimeMillis)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelBackground)
            .border(1.dp, if (item.isCompleted) BorderLine.copy(alpha = 0.5f) else BorderLine, RoundedCornerShape(18.dp))
            .clickable { onEdit() }
            .padding(14.dp)
            .testTag("schedule_card_${item.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: Checkbox, Title, Type Badge, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AmberPrimary,
                        uncheckedColor = TextMuted
                    ),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isCompleted) TextMuted else InkLight,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = ScheduleItem.getPersianTypeLabel(item.type),
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = TextMuted2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle row: DateTime & Countdown
            if (item.dateTimeMillis != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = PersianDateUtil.formatPersianDateTime(item.dateTimeMillis),
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    if (countdown.isNotBlank() && !item.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PanelRaised,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine)
                        ) {
                            Text(
                                text = countdown,
                                fontSize = 10.5.sp,
                                color = AmberPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Notes / details if present
            if (!item.notes.isNullOrBlank()) {
                Text(
                    text = item.notes,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }

            // Bottom action row: Alarm status & Google Calendar sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Alarm toggle chip
                if (item.dateTimeMillis != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (item.isAlarmEnabled) AmberPrimary.copy(alpha = 0.12f) else PanelRaised,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (item.isAlarmEnabled) AmberPrimary.copy(alpha = 0.4f) else BorderLine
                        ),
                        modifier = Modifier.clickable { onToggleAlarm() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isAlarmEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                                contentDescription = null,
                                tint = if (item.isAlarmEnabled) AmberPrimary else TextMuted2,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (item.isAlarmEnabled) {
                                    PersianDateUtil.toPersianDigits("آلارم ${item.reminderMinutesBefore} دقیقه قبل")
                                } else {
                                    "آلارم خاموش"
                                },
                                fontSize = 11.sp,
                                color = if (item.isAlarmEnabled) AmberPrimary else TextMuted2
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Google Calendar sync button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.isGoogleCalendarSynced) CyanAccent.copy(alpha = 0.12f) else PanelRaised,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (item.isGoogleCalendarSynced) CyanAccent.copy(alpha = 0.4f) else BorderLine
                    ),
                    modifier = Modifier.clickable { onSyncGoogleCalendar() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isGoogleCalendarSynced) Icons.Outlined.CloudDone else Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = if (item.isGoogleCalendarSynced) CyanAccent else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (item.isGoogleCalendarSynced) "همگام با تقویم گوگل" else "همگام‌سازی با گوگل",
                            fontSize = 11.sp,
                            color = if (item.isGoogleCalendarSynced) CyanAccent else TextMuted
                        )
                    }
                }
            }
        }
    }
}
