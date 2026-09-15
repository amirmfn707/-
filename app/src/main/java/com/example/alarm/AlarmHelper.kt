package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.data.model.ScheduleItem
import com.example.util.PersianDateUtil

object AlarmHelper {
    const val CHANNEL_ID = "reminder_alarms_channel"
    const val CHANNEL_NAME = "هشدارهای زمان‌بندی کاری"
    const val ACTION_REMINDER_ALARM = "com.example.ACTION_REMINDER_ALARM"

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TITLE = "extra_item_title"
    const val EXTRA_ITEM_TYPE = "extra_item_type"
    const val EXTRA_ITEM_TIME = "extra_item_time"
    const val EXTRA_ITEM_NOTES = "extra_item_notes"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان و آلارم برنامه‌ها و جلسات کاری قبل از رسیدن موعد"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    enableLights(true)
                    setSound(soundUri, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleAlarm(context: Context, item: ScheduleItem) {
        if (!item.isAlarmEnabled || item.dateTimeMillis == null || item.isCompleted) {
            cancelAlarm(context, item.id)
            return
        }

        val eventTime = item.dateTimeMillis
        val advanceMillis = item.reminderMinutesBefore * 60 * 1000L
        val triggerTime = eventTime - advanceMillis

        // If trigger time has already passed, don't schedule past alarm
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d("AlarmHelper", "Trigger time is in the past for item: ${item.title}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_ITEM_TITLE, item.title)
            putExtra(EXTRA_ITEM_TYPE, item.type)
            putExtra(EXTRA_ITEM_TIME, item.dateTimeMillis)
            putExtra(EXTRA_ITEM_NOTES, item.notes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("AlarmHelper", "Scheduled alarm for '${item.title}' at: $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmHelper", "SecurityException scheduling alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, itemId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmHelper", "Cancelled alarm for item id: $itemId")
        }
    }
}
