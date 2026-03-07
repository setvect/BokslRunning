package com.boksl.running.data.repository

import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProfileRepository
    @Inject
    constructor(
        private val profilePreferencesDataSource: ProfilePreferencesDataSource,
    ) : ProfileRepository {
        override fun observeProfile(): Flow<Profile?> = profilePreferencesDataSource.observeProfile()

        override fun observeAppPreferences(): Flow<AppPreferences> =
            profilePreferencesDataSource.observeAppPreferences()

        override suspend fun saveProfile(profile: Profile) {
            validateProfile(profile)
            profilePreferencesDataSource.saveProfile(profile)
        }

        override suspend fun clearProfile() {
            profilePreferencesDataSource.clearProfile()
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            profilePreferencesDataSource.setOnboardingCompleted(completed)
        }

        override suspend fun setLocationRationaleShown(shown: Boolean) {
            profilePreferencesDataSource.setLocationRationaleShown(shown)
        }

        private fun validateProfile(profile: Profile) {
            require(profile.weightKg > 0f) { "weightKg must be positive." }
            require(profile.age > 0) { "age must be positive." }
            require(profile.updatedAtEpochMillis >= 0L) { "updatedAtEpochMillis must be >= 0." }
        }
    }
