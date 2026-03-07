package com.boksl.running.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.boksl.running.domain.model.SessionStatus

@Entity(
    tableName = "running_sessions",
    indices = [
        Index(value = ["external_id"], unique = true),
        Index(value = ["status", "started_at_epoch_millis"]),
        Index(value = ["started_at_epoch_millis"]),
    ],
)
data class RunningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    @ColumnInfo(name = "status")
    val status: SessionStatus,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at_epoch_millis")
    val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "duration_millis", defaultValue = "0")
    val durationMillis: Long,
    @ColumnInfo(name = "distance_meters", defaultValue = "0")
    val distanceMeters: Double,
    @ColumnInfo(name = "average_pace_sec_per_km")
    val averagePaceSecPerKm: Double?,
    @ColumnInfo(name = "max_speed_mps", defaultValue = "0")
    val maxSpeedMps: Double,
    @ColumnInfo(name = "calorie_kcal")
    val calorieKcal: Double?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
