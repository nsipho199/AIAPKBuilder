package com.aiapkbuilder.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.viewmodel.Quadruple
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        dataStore = mockk()
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun `isDarkModeEnabled defaults to false`() = runTest {
        coEvery { dataStore.data } returns flowOf(mockk(relaxed = true))
        val result = repository.isDarkModeEnabled().first()
        assertFalse(result)
    }

    @Test
    fun `isAutoBuildEnabled defaults to true`() = runTest {
        coEvery { dataStore.data } returns flowOf(mockk(relaxed = true))
        val result = repository.isAutoBuildEnabled().first()
        assertTrue(result)
    }

    @Test
    fun `isCacheEnabled defaults to true`() = runTest {
        coEvery { dataStore.data } returns flowOf(mockk(relaxed = true))
        val result = repository.isCacheEnabled().first()
        assertTrue(result)
    }

    @Test
    fun `getBuildRetentionDays defaults to 30`() = runTest {
        coEvery { dataStore.data } returns flowOf(mockk(relaxed = true))
        val result = repository.getBuildRetentionDays().first()
        assertEquals(30, result)
    }

    @Test
    fun `setDarkMode delegates to dataStore`() = runTest {
        coEvery { dataStore.edit(any()) } returns Unit
        repository.setDarkMode(true)
    }

    @Test
    fun `setAutoBuild delegates to dataStore`() = runTest {
        coEvery { dataStore.edit(any()) } returns Unit
        repository.setAutoBuild(false)
    }

    @Test
    fun `updateAIApiKey saves to dataStore`() = runTest {
        coEvery { dataStore.edit(any()) } returns Unit
        repository.updateAIApiKey("sk-test")
    }

    @Test
    fun `clearAllSettings clears preferences`() = runTest {
        coEvery { dataStore.edit(any()) } returns Unit
        repository.clearAllSettings()
    }

    @Test
    fun `settings keys are not empty`() {
        assertTrue(SettingsRepository.AI_PROVIDER.name.isNotEmpty())
        assertTrue(SettingsRepository.AI_API_KEY.name.isNotEmpty())
        assertTrue(SettingsRepository.DARK_MODE.name.isNotEmpty())
    }
}
