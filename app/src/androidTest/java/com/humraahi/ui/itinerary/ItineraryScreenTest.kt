package com.humraahi.ui.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.model.ItineraryDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItineraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: ItineraryUiState,
        onRetry: () -> Unit = {},
        onAddActivity: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                ItineraryContent(
                    uiState = state,
                    onRetry = onRetry,
                    onAddActivity = onAddActivity
                )
            }
        }
    }

    @Test
    fun loadingShowsProgressIndicator() {
        show(ItineraryUiState.Loading)

        composeRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        ).assertIsDisplayed()
    }

    @Test
    fun emptyShowsExplanation() {
        show(ItineraryUiState.Empty)

        composeRule.onNodeWithText(
            "No itinerary yet. Add your first activity to start planning."
        ).assertIsDisplayed()
    }

    @Test
    fun emptyPlanCanStartAddingActivity() {
        var additions = 0
        show(ItineraryUiState.Empty, onAddActivity = { additions++ })

        composeRule.onNodeWithText("Add activity").performClick()

        composeRule.runOnIdle { assertEquals(1, additions) }
    }

    @Test
    fun errorShowsMessageAndRetryCallsHandler() {
        var retries = 0
        show(ItineraryUiState.Error("Permission denied"), onRetry = { retries++ })

        composeRule.onNodeWithText("Permission denied").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun successShowsDayAndActivityDetailsInSavedOrder() {
        val itinerary = Itinerary(
            tripId = "trip-1",
            days = listOf(
                ItineraryDay(
                    id = "day-1",
                    dayNumber = 1,
                    date = "2026-10-20",
                    title = "Arrival",
                    activities = listOf(
                        ItineraryActivity(
                            id = "activity-1",
                            startTime = "15:00",
                            title = "Hotel check-in",
                            location = "Candolim",
                            notes = "Bring your booking confirmation."
                        ),
                        ItineraryActivity(id = "activity-2", title = "Sunset at the beach")
                    )
                )
            )
        )
        show(ItineraryUiState.Success(itinerary))

        listOf(
            "Day 1",
            "2026-10-20",
            "Arrival",
            "15:00",
            "Hotel check-in",
            "Candolim",
            "Bring your booking confirmation.",
            "Sunset at the beach"
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        val checkIn = composeRule.onNodeWithText("Hotel check-in").fetchSemanticsNode()
        val sunset = composeRule.onNodeWithText("Sunset at the beach").fetchSemanticsNode()
        assertTrue(checkIn.boundsInRoot.top < sunset.boundsInRoot.top)
    }

    @Test
    fun dayWithoutActivitiesShowsExplanation() {
        show(
            ItineraryUiState.Success(
                Itinerary(
                    tripId = "trip-1",
                    days = listOf(ItineraryDay(id = "day-1", dayNumber = 1))
                )
            )
        )

        composeRule.onNodeWithText("Day 1").assertIsDisplayed()
        composeRule.onNodeWithText("No activities planned for this day.").assertIsDisplayed()
    }
}
