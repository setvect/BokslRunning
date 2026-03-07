package com.boksl.running.data.local.db.converter

import androidx.room.TypeConverter
import com.boksl.running.domain.model.SessionStatus

class SessionStatusConverter {
    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus =
        SessionStatus.entries.firstOrNull { it.name == value } ?: SessionStatus.DISCARDED
}
