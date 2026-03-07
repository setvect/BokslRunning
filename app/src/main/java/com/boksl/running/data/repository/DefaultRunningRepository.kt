package com.boksl.running.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.boksl.running.core.di.IoDispatcher
import com.boksl.running.data.local.db.dao.HomeSummaryProjection
import com.boksl.running.data.local.db.dao.MonthlyStatsProjection
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.mapper.toDomain
import com.boksl.running.data.mapper.toEntity
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.RunningRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class DefaultRunningRepository
    @Inject
    constructor(
        private val runningSessionDao: RunningSessionDao,
        private val trackPointDao: TrackPointDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val clock: Clock,
    ) : RunningRepository {
        override fun observeHomeSummary(): Flow<HomeSummary> =
            runningSessionDao
                .observeHomeSummary()
                .map { projection -> projection.toHomeSummary() }
                .flowOn(ioDispatcher)

        override fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>> =
            runningSessionDao
                .observeMonthlyStats()
                .map { projections ->
                    projections.toMonthlyStats(
                        currentMonth = YearMonth.now(clock),
                    )
                }.flowOn(ioDispatcher)

        override fun observeSession(sessionId: Long): Flow<RunningSession?> =
            runningSessionDao
                .observeById(sessionId)
                .map { entity -> entity?.toDomain() }
                .flowOn(ioDispatcher)

        override fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = HISTORY_PAGE_SIZE,
                        initialLoadSize = INITIAL_HISTORY_LOAD_SIZE,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = { runningSessionDao.pagingSourceByStatus(SessionStatus.SAVED) },
            ).flow.map { pagingData ->
                pagingData.map { entity -> entity.toDomain() }
            }

        override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> =
            runningSessionDao
                .observeRecent(limit)
                .map { sessions -> sessions.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> =
            trackPointDao
                .observeBySessionId(sessionId)
                .map { points -> points.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun getSession(sessionId: Long): RunningSession? =
            withContext(ioDispatcher) {
                runningSessionDao.getById(sessionId)?.toDomain()
            }

        override suspend fun getActiveSession(): RunningSession? =
            withContext(ioDispatcher) {
                runningSessionDao.getLatestByStatus(SessionStatus.IN_PROGRESS)?.toDomain()
            }

        override suspend fun insertSession(session: RunningSession): Long =
            withContext(ioDispatcher) {
                val validatedSession = prepareSessionForInsert(session)
                runningSessionDao.insert(validatedSession.toEntity())
            }

        override suspend fun updateSession(session: RunningSession) {
            withContext(ioDispatcher) {
                require(session.id > 0L) { "session.id must be > 0 for update." }
                validateStats(session.stats)
                validateSessionTimestamps(
                    startedAtEpochMillis = session.startedAtEpochMillis,
                    endedAtEpochMillis = session.endedAtEpochMillis,
                )

                runningSessionDao.update(session.toEntity())
            }
        }

        override suspend fun insertTrackPoints(points: List<TrackPoint>) {
            if (points.isEmpty()) return

            withContext(ioDispatcher) {
                val preparedPoints = points.map(::prepareTrackPointForInsert)
                trackPointDao.insertAll(preparedPoints.map { it.toEntity() })
            }
        }

        override suspend fun deleteSession(sessionId: Long) {
            withContext(ioDispatcher) {
                runningSessionDao.deleteById(sessionId)
            }
        }

        private fun prepareSessionForInsert(session: RunningSession): RunningSession {
            validateStats(session.stats)
            validateSessionTimestamps(
                startedAtEpochMillis = session.startedAtEpochMillis,
                endedAtEpochMillis = session.endedAtEpochMillis,
            )

            return session.copy(
                id = 0L,
                externalId = session.externalId.ifBlank { UUID.randomUUID().toString() },
            )
        }

        private fun prepareTrackPointForInsert(point: TrackPoint): TrackPoint {
            require(point.sessionId > 0L) { "track point sessionId must be > 0." }
            require(point.sequence >= 0) { "track point sequence must be >= 0." }
            require(point.latitude in MIN_LATITUDE..MAX_LATITUDE) {
                "latitude must be in [$MIN_LATITUDE, $MAX_LATITUDE]."
            }
            require(point.longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
                "longitude must be in [$MIN_LONGITUDE, $MAX_LONGITUDE]."
            }
            point.accuracyMeters?.let {
                require(it >= 0f) { "accuracyMeters must be >= 0." }
            }
            point.speedMps?.let {
                require(it >= 0f) { "speedMps must be >= 0." }
            }
            require(point.recordedAtEpochMillis >= 0L) { "recordedAtEpochMillis must be >= 0." }

            return point.copy(
                id = 0L,
                externalId = point.externalId.ifBlank { UUID.randomUUID().toString() },
            )
        }

        private companion object {
            const val MIN_LATITUDE = -90.0
            const val MAX_LATITUDE = 90.0
            const val MIN_LONGITUDE = -180.0
            const val MAX_LONGITUDE = 180.0
            const val HISTORY_PAGE_SIZE = 20
            const val INITIAL_HISTORY_LOAD_SIZE = 40
        }
    }

private fun validateStats(stats: RunStats) {
    require(stats.durationMillis >= 0L) { "durationMillis must be >= 0." }
    require(stats.distanceMeters >= 0.0) { "distanceMeters must be >= 0." }
    stats.averagePaceSecPerKm?.let {
        require(it >= 0.0) { "averagePaceSecPerKm must be >= 0." }
    }
    require(stats.maxSpeedMps >= 0.0) { "maxSpeedMps must be >= 0." }
    stats.calorieKcal?.let {
        require(it >= 0.0) { "calorieKcal must be >= 0." }
    }
}

private fun validateSessionTimestamps(
    startedAtEpochMillis: Long,
    endedAtEpochMillis: Long?,
) {
    require(startedAtEpochMillis >= 0L) { "startedAtEpochMillis must be >= 0." }
    endedAtEpochMillis?.let {
        require(it >= startedAtEpochMillis) {
            "endedAtEpochMillis must be greater than or equal to startedAtEpochMillis."
        }
    }
}

private fun HomeSummaryProjection?.toHomeSummary(): HomeSummary {
    val totalDistanceMeters = this?.totalDistanceMeters ?: 0.0
    val totalDurationMillis = this?.totalDurationMillis ?: 0L
    val totalCaloriesKcal = this?.totalCaloriesKcal ?: 0.0
    val averageSpeedMps =
        if (totalDurationMillis > 0L) {
            totalDistanceMeters / (totalDurationMillis / 1_000.0)
        } else {
            0.0
        }

    return HomeSummary(
        totalDistanceMeters = totalDistanceMeters,
        totalDurationMillis = totalDurationMillis,
        averageSpeedMps = averageSpeedMps,
        totalCaloriesKcal = totalCaloriesKcal,
    )
}

private fun List<MonthlyStatsProjection>.toMonthlyStats(
    currentMonth: YearMonth,
): List<MonthlyStatsPoint> {
    val minimumStartMonth = currentMonth.minusMonths(MINIMUM_MONTH_COUNT.toLong() - 1L)
    val firstMonth = minOfOrNull { projection -> YearMonth.parse(projection.yearMonth) } ?: minimumStartMonth
    val startMonth = minOf(firstMonth, minimumStartMonth)
    val monthCount = startMonth.monthsUntilInclusive(currentMonth)
    val months = (0 until monthCount).map { offset -> startMonth.plusMonths(offset.toLong()) }

    val aggregatedByMonth =
        associate { projection ->
            val month = YearMonth.parse(projection.yearMonth)
            month to projection.toMonthlyStatsPoint(month)
        }

    return months.map { month ->
        aggregatedByMonth[month]
            ?: MonthlyStatsPoint(
                yearMonth = month,
                totalDistanceMeters = 0.0,
                totalDurationMillis = 0L,
                averageSpeedMps = 0.0,
            )
    }
}

private fun YearMonth.monthsUntilInclusive(other: YearMonth): Int =
    ((other.year - year) * 12 + other.monthValue - monthValue) + 1

private const val MINIMUM_MONTH_COUNT = 6

private fun MonthlyStatsProjection.toMonthlyStatsPoint(yearMonth: YearMonth): MonthlyStatsPoint {
    val totalDistanceMeters = totalDistanceMeters ?: 0.0
    val totalDurationMillis = totalDurationMillis ?: 0L
    val averageSpeedMps =
        if (totalDurationMillis > 0L) {
            totalDistanceMeters / (totalDurationMillis / 1_000.0)
        } else {
            0.0
        }

    return MonthlyStatsPoint(
        yearMonth = yearMonth,
        totalDistanceMeters = totalDistanceMeters,
        totalDurationMillis = totalDurationMillis,
        averageSpeedMps = averageSpeedMps,
    )
}
