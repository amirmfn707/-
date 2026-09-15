package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String = TYPE_TASK, // "meeting", "reminder", "task", "event"
    val dateTimeMillis: Long? = null,
    val durationMinutes: Int = 30,
    val notes: String? = null,
    val location: String? = null,
    val isAlarmEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 15,
    val isGoogleCalendarSynced: Boolean = false,
    val googleCalendarEventId: Long? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val originalVoiceTranscript: String? = null
) {
    companion object {
        const val TYPE_MEETING = "meeting"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_TASK = "task"
        const val TYPE_EVENT = "event"

        fun getPersianTypeLabel(type: String): String {
            return when (type.lowercase()) {
                TYPE_MEETING -> "جلسه کاری"
                TYPE_REMINDER -> "یادآوری"
                TYPE_EVENT -> "رویداد"
                else -> "کار"
            }
        }
    }
}
