package com.boksl.running.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.boksl.running.data.local.db.converter.SessionStatusConverter
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.data.local.db.entity.TrackPointEntity

@Database(
    entities = [RunningSessionEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SessionStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runningSessionDao(): RunningSessionDao

    abstract fun trackPointDao(): TrackPointDao
}
