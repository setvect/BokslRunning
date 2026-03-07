package com.boksl.running.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val profileWeightKg = floatPreferencesKey("profile_weight_kg")
            val profileGender = stringPreferencesKey("profile_gender")
            val profileAge = intPreferencesKey("profile_age")
            val profileUpdatedAtEpochMillis = longPreferencesKey("profile_updated_at_epoch_millis")
            val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
            val locationRationaleShown = booleanPreferencesKey("location_rationale_shown")
        }

        fun observeProfile(): Flow<Profile?> =
            dataStore.data.map { preferences ->
                val weight = preferences[Keys.profileWeightKg]
                val gender = preferences[Keys.profileGender]
                val age = preferences[Keys.profileAge]
                val updatedAt = preferences[Keys.profileUpdatedAtEpochMillis]

                if (weight == null) return@map null
                if (gender == null) return@map null
                if (age == null) return@map null
                if (updatedAt == null) return@map null

                Profile(
                    weightKg = weight,
                    gender = gender.toGender(),
                    age = age,
                    updatedAtEpochMillis = updatedAt,
                )
            }

        fun observeAppPreferences(): Flow<AppPreferences> =
            dataStore.data.map { preferences ->
                AppPreferences(
                    onboardingCompleted = preferences[Keys.onboardingCompleted] ?: false,
                    locationRationaleShown = preferences[Keys.locationRationaleShown] ?: false,
                )
            }

        suspend fun saveProfile(profile: Profile) {
            dataStore.edit { preferences ->
                preferences[Keys.profileWeightKg] = profile.weightKg
                preferences[Keys.profileGender] = profile.gender.name
                preferences[Keys.profileAge] = profile.age
                preferences[Keys.profileUpdatedAtEpochMillis] = profile.updatedAtEpochMillis
            }
        }

        suspend fun clearProfile() {
            dataStore.edit { preferences ->
                preferences.remove(Keys.profileWeightKg)
                preferences.remove(Keys.profileGender)
                preferences.remove(Keys.profileAge)
                preferences.remove(Keys.profileUpdatedAtEpochMillis)
            }
        }

        suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.onboardingCompleted] = completed
            }
        }

        suspend fun setLocationRationaleShown(shown: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.locationRationaleShown] = shown
            }
        }

        private fun String.toGender(): Gender = Gender.entries.firstOrNull { it.name == this } ?: Gender.UNSPECIFIED
    }
