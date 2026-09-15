package com.example.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ScheduleItem
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderLine
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.InkLight
import com.example.ui.theme.OnAmber
import com.example.ui.theme.PanelBackground
import com.example.ui.theme.PanelRaised
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextMuted2
import com.example.util.PersianDateUtil
import java.util.Calendar

@Composable
fun AddEditScheduleDialog(
    initialItem: ScheduleItem?,
    defaultAlarmMinutes: Int,
    onSave: (
        id: Long,
        title: String,
        type: String,
        dateTimeMillis: Long?,
        durationMinutes: Int,
        notes: String?,
        reminderMinutesBefore: Int,
        isAlarmEnabled: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var type by remember { mutableStateOf(initialItem?.type ?: ScheduleItem.TYPE_TASK) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var isAlarmEnabled by remember { mutableStateOf(initialItem?.isAlarmEnabled ?: true) }
    var reminderMinutesBefore by remember {
        mutableStateOf(initialItem?.reminderMinutesBefore ?: defaultAlarmMinutes)
    }

    // Date/time setup
    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialItem?.dateTimeMillis != null) {
                timeInMillis = initialItem.dateTimeMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var hasSpecificTime by remember { mutableStateOf(initialItem?.dateTimeMillis != null) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var dayOffset by remember { mutableStateOf(0) } // 0 = today, 1 = tomorrow, 2 = 2 days

    val typeOptions = listOf(
        ScheduleItem.TYPE_TASK to "کار",
        ScheduleItem.TYPE_MEETING to "جلسه کاری",
        ScheduleItem.TYPE_REMINDER to "یادآوری",
        ScheduleItem.TYPE_EVENT to "رویداد"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PanelBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLine),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialItem != null) "ویرایش برنامه کاری" else "افزودن برنامه جدید",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkLight
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderLine)

                // Title Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("عنوان برنامه", fontSize = 13.sp, color = InkLight, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("مثلاً: جلسه بازبینی قرارداد", color = TextMuted2, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PanelRaised,
                            unfocusedContainerColor = PanelRaised,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = InkLight,
                            unfocusedTextColor = InkLight
                        )
                    )
                }

                // Type Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نوع برنامه", fontSize = 13.sp, color = InkLight, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        typeOptions.forEach { (tKey, tLabel) ->
                            val isSelected = type == tKey
                            val color = when (tKey) {
                                ScheduleItem.TYPE_MEETING -> CyanAccent
                                ScheduleItem.TYPE_REMINDER -> CoralAccent
                                ScheduleItem.TYPE_EVENT -> EmeraldAccent
                                else -> AmberPrimary
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else PanelRaised,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) color else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { type = tKey }
                            ) {
                                Text(
                                    text = tLabel,
                                    color = if (isSelected) color else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Specific time toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("دارای زمان و موعد مشخص", fontSize = 13.sp, color = InkLight)
                    Switch(
                        checked = hasSpecificTime,
                        onCheckedChange = { hasSpecificTime = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberPrimary,
                            checkedTrackColor = AmberPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                if (hasSpecificTime) {
                    // Day Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "امروز", 1 to "فردا", 2 to "پس‌فردا").forEach { (offset, label) ->
                            val isSelected = dayOffset == offset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AmberPrimary.copy(alpha = 0.2f) else PanelRaised,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AmberPrimary else BorderLine
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { dayOffset = offset }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AmberPrimary else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Time Hour & Minute Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ساعت:", fontSize = 13.sp, color = TextMuted)

                        // Hour selector chips
                        listOf(9, 10, 11, 14, 16, 18, 20).forEach { h ->
                            val isSelected = selectedHour == h
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AmberPrimary else PanelRaised,
                                modifier = Modifier.clickable { selectedHour = h }
                            ) {
                                Text(
                                    text = PersianDateUtil.toPersianDigits(h.toString()),
                                    color = if (isSelected) OnAmber else InkLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Alarm Advance Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                            Text("آلارم قبل از موعد:", fontSize = 12.5.sp, color = InkLight)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0 to "موعد", 15 to "۱۵د", 30 to "۳۰د", 60 to "۱س").forEach { (m, l) ->
                                val isSelected = reminderMinutesBefore == m
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) AmberPrimary else PanelRaised,
                                    modifier = Modifier.clickable { reminderMinutesBefore = m }
                                ) {
                                    Text(
                                        text = l,
                                        color = if (isSelected) OnAmber else TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("یادداشت و توضیحات اضافی", fontSize = 13.sp, color = InkLight)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("مکان، شرکت‌کنندگان یا نکات مهم…", color = TextMuted2, fontSize = 12.5.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PanelRaised,
                            unfocusedContainerColor = PanelRaised,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = BorderLine,
                            focusedTextColor = InkLight,
                            unfocusedTextColor = InkLight
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Save button
                Button(
                    onClick = {
                        val computedMillis = if (hasSpecificTime) {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, dayOffset)
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            cal.timeInMillis
                        } else null

                        onSave(
                            initialItem?.id ?: 0L,
                            title,
                            type,
                            computedMillis,
                            30,
                            notes.ifBlank { null },
                            reminderMinutesBefore,
                            isAlarmEnabled
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_schedule_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = OnAmber
                    )
                ) {
                    Text(
                        text = if (initialItem != null) "ذخیره تغییرات" else "افزودن برنامه",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
