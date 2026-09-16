package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules all upcoming active alarms after system reboot or restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Re-scheduling active alarms after boot...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val schedules = db.scheduleDao().getAllSchedules().first()
                    val now = System.currentTimeMillis()
                    for (item in schedules) {
                        if (item.isAlarmEnabled && item.dateTimeMillis != null && item.dateTimeMillis > now && !item.isCompleted) {
                            AlarmHelper.scheduleAlarm(context, item)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
