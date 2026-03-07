package com.boksl.running.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

class ProfilePreferencesDataSourceTest {
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var preferencesFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataSource: ProfilePreferencesDataSource

    @Before
    fun setUp() {
        preferencesFile = File.createTempFile("profile-preferences", ".preferences_pb")
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { preferencesFile },
            )
        dataSource = ProfilePreferencesDataSource(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        preferencesFile.delete()
    }

    @Test
    fun saveObserveAndClearProfile() =
        runTest {
            assertNull(dataSource.observeProfile().first())

            val profile =
                Profile(
                    weightKg = 68.5f,
                    gender = Gender.MALE,
                    age = 30,
                    updatedAtEpochMillis = 1_000L,
                )

            dataSource.saveProfile(profile)
            assertEquals(profile, dataSource.observeProfile().first())

            dataSource.clearProfile()
            assertNull(dataSource.observeProfile().first())
        }

    @Test
    fun appPreferenceFlagsReadWrite() =
        runTest {
            val initial = dataSource.observeAppPreferences().first()
            assertFalse(initial.onboardingCompleted)
            assertFalse(initial.locationRationaleShown)

            dataSource.setOnboardingCompleted(completed = true)
            dataSource.setLocationRationaleShown(shown = true)

            val updated = dataSource.observeAppPreferences().first()
            assertTrue(updated.onboardingCompleted)
            assertTrue(updated.locationRationaleShown)
        }
}
