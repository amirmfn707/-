package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.ScheduleItem

object AlarmHelper {
    const val CHANNEL_ID = "vira_alarms_channel_v3"
    const val CHANNEL_NAME = "هشدارهای زمان‌بندی ویرا"
    const val ACTION_REMINDER_ALARM = "com.example.ACTION_REMINDER_ALARM"

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TITLE = "extra_item_title"
    const val EXTRA_ITEM_TYPE = "extra_item_type"
    const val EXTRA_ITEM_TIME = "extra_item_time"
    const val EXTRA_ITEM_NOTES = "extra_item_notes"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان و آلارم برنامه‌ها و جلسات کاری قبل از رسیدن موعد"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 250, 600, 250, 600)
                    enableLights(true)
                    setSound(alarmSoundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleAlarm(context: Context, item: ScheduleItem) {
        createNotificationChannel(context)

        if (!item.isAlarmEnabled || item.dateTimeMillis == null || item.isCompleted) {
            cancelAlarm(context, item.id)
            return
        }

        val eventTime = item.dateTimeMillis
        val advanceMillis = item.reminderMinutesBefore * 60 * 1000L
        val advanceTriggerTime = eventTime - advanceMillis
        val now = System.currentTimeMillis()

        // If the whole event is in the past, don't schedule
        if (eventTime <= now) {
            Log.d("AlarmHelper", "Event time is in the past for item: ${item.title}")
            return
        }

        // Determine optimal trigger time:
        // 1. If advance reminder time is in the future, trigger then.
        // 2. If advance reminder time has passed (e.g. appointment is in 5 min, but reminder is 15 min),
        //    trigger at the event time itself (or in 2 seconds if event is imminent).
        val triggerTime = if (advanceTriggerTime > now) {
            advanceTriggerTime
        } else {
            maxOf(now + 2000L, eventTime)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Intent to trigger ReminderAlarmReceiver
        val receiverIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
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
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show Intent for AlarmClockInfo (tapping status bar alarm opens app)
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_alarm_id", item.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (item.id + 100000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // setAlarmClock is the Android gold standard:
            // 1. Fires with millisecond accuracy, immune to Doze mode and battery savers.
            // 2. Displays the alarm clock icon in the Android status bar.
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmHelper", "setAlarmClock scheduled for '${item.title}' at: $triggerTime (now=$now)")
        } catch (e: Exception) {
            Log.w("AlarmHelper", "setAlarmClock failed (${e.message}), attempting fallback")
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
            } catch (e2: Exception) {
                Log.e("AlarmHelper", "Fallback alarm scheduling failed: ${e2.message}")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
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

