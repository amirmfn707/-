package com.example.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.ScheduleItem
import com.example.util.PersianDateUtil

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        Log.d("ReminderAlarmReceiver", "Alarm received: action=${intent.action}")

        val itemId = intent.getLongExtra(AlarmHelper.EXTRA_ITEM_ID, -1L)
        val title = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_TITLE) ?: "یادآوری برنامه کاری"
        val type = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_TYPE) ?: ScheduleItem.TYPE_TASK
        val timeMillis = intent.getLongExtra(AlarmHelper.EXTRA_ITEM_TIME, 0L)
        val notes = intent.getStringExtra(AlarmHelper.EXTRA_ITEM_NOTES)

        // Ensure notification channel exists
        AlarmHelper.createNotificationChannel(context)

        // 1. Play real alarm ringtone with USAGE_ALARM (bypasses silent/vibrate profiles)
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("ReminderAlarmReceiver", "Failed to play ringtone", e)
        }

        // 2. Trigger vibration pattern
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 700, 300, 700, 300, 700),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 700, 300, 700, 300, 700), -1)
            }
        } catch (e: Exception) {
            Log.e("ReminderAlarmReceiver", "Failed to vibrate", e)
        }

        // 3. Build Full-Screen / Heads-up Notification
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

        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, AlarmHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setSound(alarmSoundUri)
            .setVibrate(longArrayOf(0, 700, 300, 700, 300, 700))
            .setContentIntent(pendingIntent)
            .build()

        // 4. Post notification if permission granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("ReminderAlarmReceiver", "POST_NOTIFICATIONS not granted; audio alarm played")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (itemId > 0) itemId.toInt() else (System.currentTimeMillis() % 100000).toInt()
        notificationManager?.notify(notificationId, notification)
    }
}

