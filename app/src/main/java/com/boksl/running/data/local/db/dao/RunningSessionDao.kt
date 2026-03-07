package com.boksl.running.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RunningSessionEntity): Long

    @Update
    suspend fun update(entity: RunningSessionEntity)

    @Query("SELECT * FROM running_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): RunningSessionEntity?

    @Query("SELECT * FROM running_sessions WHERE id = :sessionId LIMIT 1")
    fun observeById(sessionId: Long): Flow<RunningSessionEntity?>

    @Query("SELECT * FROM running_sessions ORDER BY started_at_epoch_millis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RunningSessionEntity>>

    @Query("SELECT * FROM running_sessions WHERE status = :status ORDER BY started_at_epoch_millis DESC LIMIT 1")
    suspend fun getLatestByStatus(status: SessionStatus): RunningSessionEntity?

    @Query("DELETE FROM running_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)
}
