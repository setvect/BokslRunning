package com.boksl.running.data.export

import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import java.time.Clock

internal suspend fun buildExportBundle(
    runningSessionDao: RunningSessionDao,
    trackPointDao: TrackPointDao,
    profilePreferencesDataSource: ProfilePreferencesDataSource,
    clock: Clock,
    sessionStatus: SessionStatus = SessionStatus.SAVED,
): ExportBundleDto {
    val profile = profilePreferencesDataSource.observeProfile().first()?.toExportDto()
    val appPreferences = profilePreferencesDataSource.observeAppPreferences().first().toExportDto()
    val sessionEntities = runningSessionDao.getByStatus(sessionStatus)
    val sessionDtos =
        sessionEntities.map { session ->
            val trackPoints =
                trackPointDao
                    .getBySessionId(session.id)
                    .map { trackPoint -> trackPoint.toExportDto() }
            session.toExportDto(trackPoints)
        }

    return ExportBundleDto(
        schemaVersion = EXPORT_SCHEMA_VERSION,
        exportedAtEpochMillis = clock.millis(),
        profile = profile,
        appPreferences = appPreferences,
        sessions = sessionDtos,
    )
}

internal const val EXPORT_SCHEMA_VERSION = 1
