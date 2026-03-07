package com.boksl.running.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boksl.running.data.local.db.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<TrackPointEntity>): List<Long>

    @Query("SELECT * FROM track_points WHERE session_id = :sessionId ORDER BY sequence ASC")
    fun observeBySessionId(sessionId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE session_id = :sessionId ORDER BY sequence ASC")
    suspend fun getBySessionId(sessionId: Long): List<TrackPointEntity>

    @Query("DELETE FROM track_points WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
