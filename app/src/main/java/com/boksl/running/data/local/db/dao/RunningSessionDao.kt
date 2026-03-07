package com.boksl.running.data.local.db.dao

import androidx.paging.PagingSource
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
    @Query(
        """
        SELECT
            SUM(distance_meters) AS totalDistanceMeters,
            SUM(duration_millis) AS totalDurationMillis,
            SUM(calorie_kcal) AS totalCaloriesKcal
        FROM running_sessions
        WHERE status = :status
        """,
    )
    fun observeHomeSummary(status: SessionStatus = SessionStatus.SAVED): Flow<HomeSummaryProjection?>

    @Query(
        """
        SELECT
            strftime('%Y-%m', started_at_epoch_millis / 1000, 'unixepoch', 'localtime') AS yearMonth,
            SUM(distance_meters) AS totalDistanceMeters,
            SUM(duration_millis) AS totalDurationMillis
        FROM running_sessions
        WHERE status = :status
        GROUP BY yearMonth
        ORDER BY yearMonth DESC
        """,
    )
    fun observeMonthlyStats(status: SessionStatus = SessionStatus.SAVED): Flow<List<MonthlyStatsProjection>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RunningSessionEntity): Long

    @Update
    suspend fun update(entity: RunningSessionEntity)

    @Query("SELECT * FROM running_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): RunningSessionEntity?

    @Query("SELECT * FROM running_sessions WHERE id = :sessionId LIMIT 1")
    fun observeById(sessionId: Long): Flow<RunningSessionEntity?>

    @Query(
        """
        SELECT * FROM running_sessions
        WHERE status = :status
        ORDER BY started_at_epoch_millis DESC
        """,
    )
    fun pagingSourceByStatus(status: SessionStatus): PagingSource<Int, RunningSessionEntity>

    @Query("SELECT * FROM running_sessions ORDER BY started_at_epoch_millis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RunningSessionEntity>>

    @Query("SELECT * FROM running_sessions WHERE status = :status ORDER BY started_at_epoch_millis DESC LIMIT 1")
    suspend fun getLatestByStatus(status: SessionStatus): RunningSessionEntity?

    @Query("SELECT id FROM running_sessions WHERE external_id LIKE :externalIdPattern ORDER BY started_at_epoch_millis DESC")
    suspend fun getIdsByExternalIdPattern(externalIdPattern: String): List<Long>

    @Query("DELETE FROM running_sessions WHERE external_id LIKE :externalIdPattern")
    suspend fun deleteByExternalIdPattern(externalIdPattern: String): Int

    @Query("DELETE FROM running_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)
}
