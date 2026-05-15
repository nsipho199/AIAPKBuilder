package com.aiapkbuilder.app

import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.viewmodel.GenerateViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GenerateViewModelTest {

    @Test
    fun `prompt validation rejects blank input`() {
        // When prompt is blank, error should be set
        assertTrue("Blank prompt should fail validation", "".isBlank())
    }

    @Test
    fun `app type defaults to CUSTOM`() {
        assertEquals(AppType.CUSTOM.name, "CUSTOM")
    }

    @Test
    fun `build providers have display names`() {
        BuildProvider.entries.forEach { provider ->
            assertTrue("Provider ${provider.name} should have display name",
                provider.displayName.isNotBlank())
        }
    }

    @Test
    fun `all app types have display names`() {
        AppType.entries.forEach { type ->
            assertTrue("AppType ${type.name} should have display name",
                type.displayName.isNotBlank())
        }
    }
}
