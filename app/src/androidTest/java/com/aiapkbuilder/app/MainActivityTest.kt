package com.aiapkbuilder.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_displays_app_title() {
        composeTestRule.onNodeWithText("AI APK Builder").assertIsDisplayed()
    }

    @Test
    fun bottomNav_has_four_items() {
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun generate_button_is_clickable() {
        composeTestRule.onNodeWithText("Generate New App").performClick()
        composeTestRule.onNodeWithText("Generate App").assertIsDisplayed()
    }
}
