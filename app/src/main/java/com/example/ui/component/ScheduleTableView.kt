package com.example.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun ScheduleTableView(
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
                    text = "هیچ برنامه‌ای در این دسته‌بندی یافت نشد.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Text(
                    text = "با لمس دکمه میکروفون بالا می‌توانید برنامه جدید ثبت کنید.",
                    color = TextMuted2,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelBackground)
            .border(1.dp, BorderLine, RoundedCornerShape(18.dp))
            .horizontalScroll(horizontalScrollState)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .background(PanelRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("وضعیت", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(60.dp))
            Text("نوع", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(80.dp))
            Text("عنوان برنامه", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(180.dp))
            Text("تاریخ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(110.dp))
            Text("ساعت", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(70.dp))
            Text("آلارم", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(70.dp))
            Text("تقویم گوگل", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(90.dp))
            Text("عملیات", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AmberPrimary, modifier = Modifier.width(60.dp))
        }

        HorizontalDivider(color = BorderLine, thickness = 1.dp)

        // Table Rows
        schedules.forEachIndexed { index, item ->
            val rowBg = if (index % 2 == 0) PanelBackground else PanelRaised.copy(alpha = 0.5f)
            Row(
                modifier = Modifier
                    .background(rowBg)
                    .clickable { onEdit(item) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox status
                Box(modifier = Modifier.width(60.dp)) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggleComplete(item) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AmberPrimary,
                            uncheckedColor = TextMuted
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Type Badge
                Box(modifier = Modifier.width(80.dp)) {
                    val badgeColor = when (item.type.lowercase()) {
                        ScheduleItem.TYPE_MEETING -> CyanAccent
                        ScheduleItem.TYPE_REMINDER -> CoralAccent
                        ScheduleItem.TYPE_EVENT -> EmeraldAccent
                        else -> AmberPrimary
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = ScheduleItem.getPersianTypeLabel(item.type),
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Title
                Column(modifier = Modifier.width(180.dp).padding(end = 8.dp)) {
                    Text(
                        text = item.title,
                        color = if (item.isCompleted) TextMuted else InkLight,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.notes.isNullOrBlank()) {
                        Text(
                            text = item.notes,
                            color = TextMuted2,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Date
                Text(
                    text = PersianDateUtil.formatPersianDateOnly(item.dateTimeMillis),
                    color = TextMuted,
                    fontSize = 12.5.sp,
                    modifier = Modifier.width(110.dp)
                )

                // Time
                Text(
                    text = PersianDateUtil.formatPersianTimeOnly(item.dateTimeMillis),
                    color = InkLight,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(70.dp)
                )

                // Alarm Toggle
                Box(modifier = Modifier.width(70.dp)) {
                    if (item.dateTimeMillis != null) {
                        IconButton(
                            onClick = { onToggleAlarm(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isAlarmEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                                contentDescription = "تغییر وضعیت آلارم",
                                tint = if (item.isAlarmEnabled) AmberPrimary else TextMuted2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Text("-", color = TextMuted2, fontSize = 12.sp)
                    }
                }

                // Google Calendar Sync
                Box(modifier = Modifier.width(90.dp)) {
                    IconButton(
                        onClick = { onSyncGoogleCalendar(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isGoogleCalendarSynced) Icons.Outlined.CloudDone else Icons.Outlined.Sync,
                            contentDescription = "همگام‌سازی با تقویم گوگل",
                            tint = if (item.isGoogleCalendarSynced) CyanAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Delete
                Box(modifier = Modifier.width(60.dp)) {
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف برنامه",
                            tint = CoralAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderLine.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }
}
