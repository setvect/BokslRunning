package com.boksl.running.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = RunningSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["external_id"], unique = true),
        Index(value = ["session_id", "sequence"], unique = true),
        Index(value = ["session_id", "recorded_at_epoch_millis"]),
    ],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "sequence")
    val sequence: Int,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "altitude_meters")
    val altitudeMeters: Double?,
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Float?,
    @ColumnInfo(name = "speed_mps")
    val speedMps: Float?,
    @ColumnInfo(name = "recorded_at_epoch_millis")
    val recordedAtEpochMillis: Long,
)
