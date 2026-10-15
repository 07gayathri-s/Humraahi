package com.humraahi.ui.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.humraahi.model.TripDateRange
import com.humraahi.model.ItineraryActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityDialogTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val dateRange = TripDateRange("2026-10-20", "2026-10-22")

    @Test
    fun saveSubmitsEnteredNameAndTripStartDate() {
        var submitted: List<String>? = null
        composeRule.setContent {
            MaterialTheme {
                ActivityDialog(
                    dateRange = dateRange,
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
            assertEquals(listOf("2026-10-20", "Beach visit", "", "", ""), submitted)
        }
    }

    @Test
    fun failedSaveKeepsEnteredNameAndAllowsRetry() {
        val saveState = mutableStateOf<ItinerarySaveState>(ItinerarySaveState.Idle)
        var attempts = 0
        composeRule.setContent {
            MaterialTheme {
                ActivityDialog(
                    dateRange = dateRange,
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
                ActivityDialog(
                    dateRange = dateRange,
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

    @Test
    fun editingPrefillsDetailsAndKeepsChangesAcrossRecreation() {
        val target = ActivityEditTarget(
            "day-2", "2026-10-21",
            ItineraryActivity(
                id = "activity-1", title = "Beach visit", startTime = "18:00", location = "Goa"
            )
        )
        var submitted: List<String>? = null
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MaterialTheme {
                ActivityDialog(
                    dateRange = dateRange,
                    saveState = ItinerarySaveState.Idle,
                    editTarget = target,
                    onDismiss = {},
                    onSave = { date, title, time, location, notes ->
                        submitted = listOf(date, title, time, location, notes)
                    }
                )
            }
        }
        composeRule.onNodeWithText("Edit activity").assertIsDisplayed()
        composeRule.onNodeWithText("Oct 21, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Beach visit").performTextReplacement("Sunset")
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Sunset").assertIsDisplayed()

        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("2026-10-21", "Sunset", "18:00", "Goa", ""), submitted)
        }
    }

    @Test
    fun pickerRestrictsDatesToTripAndSelectionIsSubmitted() {
        var submittedDate: String? = null
        composeRule.setContent {
            MaterialTheme {
                ActivityDialog(
                    dateRange = dateRange,
                    saveState = ItinerarySaveState.Idle,
                    onDismiss = {},
                    onSave = { date, _, _, _, _ -> submittedDate = date }
                )
            }
        }
        composeRule.onNodeWithContentDescription("Choose activity date").performClick()
        composeRule.onNodeWithText("19").assertIsNotEnabled()
        composeRule.onNodeWithText("23").assertIsNotEnabled()
        composeRule.onNodeWithText("21").performClick()
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.onNodeWithText("Oct 21, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle { assertEquals("2026-10-21", submittedDate) }
    }
}
