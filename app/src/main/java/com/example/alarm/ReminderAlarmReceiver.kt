package com.example.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.ScheduleItem
import com.example.util.PersianDateUtil

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        Log.d("ReminderAlarmReceiver", "Alarm received: ${intent.action}")

        val itemId = intent.getLongExtra(AlarmHelper.EXTRA_ITEM_ID, -1L)
        val title = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_TITLE) ?: "یادآوری برنامه کاری"
        val type = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_TYPE) ?: ScheduleItem.TYPE_TASK
        val timeMillis = intent.getLongExtra(AlarmHelper.EXTRA_ITEM_TIME, 0L)
        val notes = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_NOTES)

        AlarmHelper.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_alarm_id", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (itemId > 0) itemId.toInt() else System.currentTimeMillis().toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeLabel = ScheduleItem.getPersianTypeLabel(type)
        val timeLabel = if (timeMillis > 0) PersianDateUtil.formatPersianDateTime(timeMillis) else ""
        val contentText = buildString {
            append("موعد $typeLabel فرا رسید")
            if (timeLabel.isNotBlank()) {
                append(" ($timeLabel)")
            }
            if (!notes.isNullOrBlank()) {
                append(" - $notes")
            }
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, AlarmHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 600, 250, 600))
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = if (itemId > 0) itemId.toInt() else (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
