package com.humraahi.ui.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.humraahi.model.ItineraryActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteActivityDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: ItinerarySaveState = ItinerarySaveState.Idle,
        onDismiss: () -> Unit = {},
        onConfirm: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                DeleteActivityDialog(
                    activity = ItineraryActivity(id = "activity-1", title = "Beach visit"),
                    saveState = state,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            }
        }
    }

    @Test
    fun openingDialogDoesNotDeleteAndCancelOnlyDismisses() {
        var deletions = 0
        var dismissals = 0
        show(onDismiss = { dismissals++ }, onConfirm = { deletions++ })

        composeRule.onNodeWithText("Delete activity?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Delete \"Beach visit\" for everyone? This cannot be undone. " +
                "If this is the last activity on its date, that date section will also be removed."
        ).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, deletions) }
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, deletions)
        }
    }

    @Test
    fun confirmRequestsDeletion() {
        var deletions = 0
        show(onConfirm = { deletions++ })

        composeRule.onNodeWithText("Delete").performClick()

        composeRule.runOnIdle { assertEquals(1, deletions) }
    }

    @Test
    fun pendingDeletionDisablesConfirmationAndCancel() {
        show(ItinerarySaveState.Saving)

        composeRule.onNodeWithText("Deleting...").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun failureShowsErrorAndAllowsRetryOrCancel() {
        var deletions = 0
        var dismissals = 0
        show(
            ItinerarySaveState.Error("Connection failed"),
            onDismiss = { dismissals++ },
            onConfirm = { deletions++ }
        )

        composeRule.onNodeWithText("Connection failed").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.runOnIdle {
            assertEquals(1, deletions)
            assertEquals(1, dismissals)
        }
    }
}
