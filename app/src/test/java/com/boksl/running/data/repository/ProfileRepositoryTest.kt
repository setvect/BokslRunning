package com.boksl.running.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ProfileRepositoryTest {
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var preferencesFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DefaultProfileRepository

    @Before
    fun setUp() {
        preferencesFile = File.createTempFile("profile-repository", ".preferences_pb")
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { preferencesFile },
            )
        repository = DefaultProfileRepository(ProfilePreferencesDataSource(dataStore))
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        preferencesFile.delete()
    }

    @Test
    fun saveAndClearProfileThroughRepository() =
        runTest {
            val profile =
                Profile(
                    weightKg = 70.0f,
                    gender = Gender.FEMALE,
                    age = 28,
                    updatedAtEpochMillis = 3_000L,
                )

            repository.saveProfile(profile)
            assertEquals(profile, repository.observeProfile().first())

            repository.clearProfile()
            assertNull(repository.observeProfile().first())
        }

    @Test
    fun appPreferenceFlagsThroughRepository() =
        runTest {
            val initial = repository.observeAppPreferences().first()
            assertFalse(initial.onboardingCompleted)
            assertFalse(initial.locationRationaleShown)

            repository.setOnboardingCompleted(completed = true)
            repository.setLocationRationaleShown(shown = true)

            val updated = repository.observeAppPreferences().first()
            assertTrue(updated.onboardingCompleted)
            assertTrue(updated.locationRationaleShown)
        }
}
