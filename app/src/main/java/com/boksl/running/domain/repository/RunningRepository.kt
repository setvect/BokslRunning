package com.boksl.running.domain.repository

import androidx.paging.PagingData
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

interface RunningRepository {
    fun observeHomeSummary(): Flow<HomeSummary>

    fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>>

    fun observeSession(sessionId: Long): Flow<RunningSession?>

    fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>>

    fun observeRecentSessions(limit: Int = 50): Flow<List<RunningSession>>

    fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>>

    suspend fun getSession(sessionId: Long): RunningSession?

    suspend fun getActiveSession(): RunningSession?

    suspend fun insertSession(session: RunningSession): Long

    suspend fun updateSession(session: RunningSession)

    suspend fun insertTrackPoints(points: List<TrackPoint>)

    suspend fun deleteSession(sessionId: Long)
}
