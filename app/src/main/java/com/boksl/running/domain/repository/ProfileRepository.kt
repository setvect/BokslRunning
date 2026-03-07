package com.boksl.running.domain.repository

import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>

    fun observeAppPreferences(): Flow<AppPreferences>

    suspend fun saveProfile(profile: Profile)

    suspend fun clearProfile()

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setLocationRationaleShown(shown: Boolean)
}
