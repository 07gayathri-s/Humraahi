package com.humraahi.ui.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddActivityDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveSubmitsEnteredNameAndDefaultDay() {
        var submitted: List<String>? = null
        composeRule.setContent {
            MaterialTheme {
                AddActivityDialog(
                    saveState = ItinerarySaveState.Idle,
                    onDismiss = {},
                    onSave = { day, title, time, location, notes ->
                        submitted = listOf(day, title, time, location, notes)
                    }
                )
            }
        }
        composeRule.onNodeWithText("Activity name").performTextInput("Beach visit")

        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("1", "Beach visit", "", "", ""), submitted)
        }
    }

    @Test
    fun failedSaveKeepsEnteredNameAndAllowsRetry() {
        val saveState = mutableStateOf<ItinerarySaveState>(ItinerarySaveState.Idle)
        var attempts = 0
        composeRule.setContent {
            MaterialTheme {
                AddActivityDialog(
                    saveState = saveState.value,
                    onDismiss = {},
                    onSave = { _, _, _, _, _ ->
                        attempts++
                        saveState.value = ItinerarySaveState.Error("Connection failed")
                    }
                )
            }
        }
        composeRule.onNodeWithText("Activity name").performTextInput("Beach visit")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Beach visit").assertIsDisplayed()
        composeRule.onNodeWithText("Connection failed").assertExists()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle { assertEquals(2, attempts) }
    }

    @Test
    fun savingDisablesSaveCancelAndInput() {
        composeRule.setContent {
            MaterialTheme {
                AddActivityDialog(
                    saveState = ItinerarySaveState.Saving,
                    onDismiss = {},
                    onSave = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Saving...").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
        composeRule.onNodeWithText("Activity name").assertIsNotEnabled()
    }
}
