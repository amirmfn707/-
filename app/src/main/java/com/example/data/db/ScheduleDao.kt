package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY CASE WHEN dateTimeMillis IS NULL THEN 1 ELSE 0 END, dateTimeMillis ASC, createdAtMillis DESC")
    fun getAllSchedules(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleItem?

    @Query("SELECT * FROM schedules WHERE dateTimeMillis IS NOT NULL AND dateTimeMillis > :nowMillis AND isAlarmEnabled = 1 AND isCompleted = 0 ORDER BY dateTimeMillis ASC")
    suspend fun getPendingAlarms(nowMillis: Long): List<ScheduleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: ScheduleItem): Long

    @Update
    suspend fun updateSchedule(item: ScheduleItem)

    @Delete
    suspend fun deleteSchedule(item: ScheduleItem)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("UPDATE schedules SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, completed: Boolean)

    @Query("UPDATE schedules SET isGoogleCalendarSynced = :synced, googleCalendarEventId = :eventId WHERE id = :id")
    suspend fun updateGoogleCalendarStatus(id: Long, synced: Boolean, eventId: Long?)

    @Query("UPDATE schedules SET isAlarmEnabled = :enabled WHERE id = :id")
    suspend fun updateAlarmStatus(id: Long, enabled: Boolean)
}
