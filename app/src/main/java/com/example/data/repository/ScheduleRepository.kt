package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmHelper
import com.example.calendar.GoogleCalendarSyncHelper
import com.example.data.api.GapGptService
import com.example.data.api.ParsedScheduleResult
import com.example.data.db.ScheduleDao
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val context: Context,
    private val dao: ScheduleDao,
    private val gapGptService: GapGptService
) {
    val allSchedules: Flow<List<ScheduleItem>> = dao.getAllSchedules()

    suspend fun insertSchedule(item: ScheduleItem): Long {
        val id = dao.insertSchedule(item)
        val created = item.copy(id = id)
        if (created.isAlarmEnabled && created.dateTimeMillis != null) {
            AlarmHelper.scheduleAlarm(context, created)
        }
        return id
    }

    suspend fun updateSchedule(item: ScheduleItem) {
        dao.updateSchedule(item)
        if (item.isAlarmEnabled && item.dateTimeMillis != null && !item.isCompleted) {
            AlarmHelper.scheduleAlarm(context, item)
        } else {
            AlarmHelper.cancelAlarm(context, item.id)
        }
    }

    suspend fun deleteSchedule(item: ScheduleItem) {
        AlarmHelper.cancelAlarm(context, item.id)
        dao.deleteSchedule(item)
    }

    suspend fun deleteScheduleById(id: Long) {
        AlarmHelper.cancelAlarm(context, id)
        dao.deleteScheduleById(id)
    }

    suspend fun toggleCompleted(item: ScheduleItem) {
        val newStatus = !item.isCompleted
        dao.updateCompletionStatus(item.id, newStatus)
        if (newStatus) {
            AlarmHelper.cancelAlarm(context, item.id)
        } else if (item.isAlarmEnabled && item.dateTimeMillis != null) {
            AlarmHelper.scheduleAlarm(context, item.copy(isCompleted = false))
        }
    }

    suspend fun toggleAlarm(item: ScheduleItem) {
        val newAlarmState = !item.isAlarmEnabled
        dao.updateAlarmStatus(item.id, newAlarmState)
        if (newAlarmState && item.dateTimeMillis != null && !item.isCompleted) {
            AlarmHelper.scheduleAlarm(context, item.copy(isAlarmEnabled = true))
        } else {
            AlarmHelper.cancelAlarm(context, item.id)
        }
    }

    suspend fun syncWithGoogleCalendar(item: ScheduleItem): Boolean {
        // Try direct sync if calendar permissions granted
        val eventId = GoogleCalendarSyncHelper.insertDirectlyToCalendar(context, item)
        if (eventId != null) {
            dao.updateGoogleCalendarStatus(item.id, true, eventId)
            return true
        }
        // Fallback or user choice: open intent
        val launched = GoogleCalendarSyncHelper.openInGoogleCalendar(context, item)
        if (launched) {
            dao.updateGoogleCalendarStatus(item.id, true, null)
        }
        return launched
    }

    suspend fun syncAllUpcomingToGoogleCalendar(items: List<ScheduleItem>): Int {
        var count = 0
        for (item in items) {
            if (item.dateTimeMillis != null && !item.isCompleted && !item.isGoogleCalendarSynced) {
                val eventId = GoogleCalendarSyncHelper.insertDirectlyToCalendar(context, item)
                if (eventId != null) {
                    dao.updateGoogleCalendarStatus(item.id, true, eventId)
                    count++
                }
            }
        }
        return count
    }

    suspend fun parseVoiceWithAI(transcript: String, modelName: String): Result<ParsedScheduleResult> {
        return gapGptService.parseVoiceSchedule(transcript, modelName)
    }

    suspend fun parseVoiceSchedulesWithAI(transcript: String, modelName: String): Result<List<ParsedScheduleResult>> {
        return gapGptService.parseVoiceSchedules(transcript, modelName)
    }
}
