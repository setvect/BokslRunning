package com.boksl.running.domain.model

data class Profile(
    val weightKg: Float,
    val gender: Gender,
    val age: Int,
    val updatedAtEpochMillis: Long,
)
