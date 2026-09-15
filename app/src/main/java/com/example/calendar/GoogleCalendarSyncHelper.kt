package com.example.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ScheduleItem
import java.util.TimeZone

object GoogleCalendarSyncHelper {

    fun hasCalendarPermissions(context: Context): Boolean {
        val write = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return write && read
    }

    /**
     * Launches the native Google Calendar event editor with pre-filled fields.
     * Works seamlessly without requiring runtime permissions.
     */
    fun openInGoogleCalendar(context: Context, item: ScheduleItem): Boolean {
        val startTime = item.dateTimeMillis ?: System.currentTimeMillis()
        val durationMillis = item.durationMinutes * 60 * 1000L
        val endTime = startTime + durationMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, item.title)
            val desc = buildString {
                append("نوع: ${ScheduleItem.getPersianTypeLabel(item.type)}\n")
                if (!item.notes.isNullOrBlank()) {
                    append("یادداشت: ${item.notes}\n")
                }
                append("ثبت شده با دستیار صوتی یادآور")
            }
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            if (!item.location.isNullOrBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, item.location)
            }
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("GoogleCalendarSync", "Failed to launch calendar intent: ${e.message}")
            false
        }
    }

    /**
     * Direct background sync via Android Calendar Provider if permissions granted.
     * Returns the inserted event ID, or null if failed.
     */
    fun insertDirectlyToCalendar(context: Context, item: ScheduleItem): Long? {
        if (!hasCalendarPermissions(context)) return null

        val startTime = item.dateTimeMillis ?: System.currentTimeMillis()
        val durationMillis = item.durationMinutes * 60 * 1000L
        val endTime = startTime + durationMillis

        try {
            val cr = context.contentResolver

            // Find primary Google account calendar or default calendar
            var calendarId = -1L
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.IS_PRIMARY
            )
            val cursor = cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val accountType = it.getString(2) ?: ""
                    val isPrimary = if (it.columnCount > 3) it.getInt(3) else 0

                    if (accountType.equals("com.google", ignoreCase = true) || isPrimary == 1) {
                        calendarId = id
                        break
                    } else if (calendarId == -1L) {
                        calendarId = id
                    }
                }
            }

            if (calendarId == -1L) {
                Log.w("GoogleCalendarSync", "No calendar found on device")
                return null
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.TITLE, item.title)
                put(
                    CalendarContract.Events.DESCRIPTION,
                    item.notes ?: "ثبت شده با دستیار صوتی یادآور"
                )
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (!item.location.isNullOrBlank()) {
                    put(CalendarContract.Events.EVENT_LOCATION, item.location)
                }
            }

            val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
            val eventId = ContentUris.parseId(uri)

            // Add reminder notification to Google Calendar
            if (item.isAlarmEnabled && item.reminderMinutesBefore > 0) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, item.reminderMinutesBefore)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            Log.d("GoogleCalendarSync", "Event inserted to Google Calendar with ID: $eventId")
            return eventId
        } catch (e: SecurityException) {
            Log.e("GoogleCalendarSync", "SecurityException: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e("GoogleCalendarSync", "Exception: ${e.message}")
            return null
        }
    }
}
