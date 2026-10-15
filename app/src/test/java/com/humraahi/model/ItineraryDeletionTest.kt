package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ItineraryDeletionTest {
    private val activity = ItineraryActivity(id = "activity-1", title = "Beach")
    private val another = ItineraryActivity(id = "activity-2", title = "Dinner")
    private val day = ItineraryDay(
        id = "day-1", dayNumber = 1, date = "2026-10-20", title = "Arrival",
        activities = listOf(activity, another)
    )
    private val laterDay = ItineraryDay(
        id = "day-3", dayNumber = 3, date = "2026-10-22",
        activities = listOf(ItineraryActivity(id = "activity-3", title = "Market"))
    )
    private val itinerary = Itinerary("trip-1", listOf(day, laterDay))

    @Test
    fun removesOnlySelectedActivityAndPreservesOriginalAndDayMetadata() {
        val updated = itinerary.withoutActivity(day.id, activity)

        assertEquals(listOf(day.copy(activities = listOf(another)), laterDay), updated.days)
        assertEquals("trip-1", updated.tripId)
        assertEquals(listOf(activity, another), itinerary.days.first().activities)
    }

    @Test
    fun removesEmptySourceDayWithoutRenumberingOtherDatesOrRemovingOtherEmptyDays() {
        val emptyDay = ItineraryDay(id = "empty", dayNumber = 2, date = "2026-10-21")
        val latest = itinerary.copy(
            days = listOf(day.copy(activities = listOf(activity)), emptyDay, laterDay)
        )

        assertEquals(listOf(emptyDay, laterDay), latest.withoutActivity(day.id, activity).days)
    }

    @Test
    fun deletingFinalActivityLeavesEmptyItinerary() {
        val single = Itinerary("trip-1", listOf(day.copy(activities = listOf(activity))))

        assertEquals(Itinerary("trip-1"), single.withoutActivity(day.id, activity))
    }

    @Test
    fun retainsConcurrentAdditionsAndEditsToOtherActivities() {
        val added = ItineraryActivity(id = "new", title = "Coffee")
        val edited = another.copy(notes = "Book a table")
        val latest = itinerary.copy(
            days = listOf(day.copy(activities = listOf(activity, edited, added)), laterDay)
        )

        assertEquals(
            listOf(edited, added),
            latest.withoutActivity(day.id, activity).days.first().activities
        )
    }

    @Test
    fun rejectsChangedActivityAndLeavesLatestPlanUntouched() {
        val changed = activity.copy(notes = "Bring water")
        val latest = itinerary.copy(days = listOf(day.copy(activities = listOf(changed))))

        val error = assertThrows(IllegalStateException::class.java) {
            latest.withoutActivity(day.id, activity)
        }

        assertEquals(
            "This activity was changed by another member. Close and reopen it before continuing.",
            error.message
        )
        assertEquals(listOf(changed), latest.days.single().activities)
    }

    @Test
    fun rejectsMissingDayRemovedActivityAndMovedActivity() {
        assertThrows(IllegalStateException::class.java) {
            itinerary.withoutActivity("missing", activity)
        }
        val removed = itinerary.withoutActivity(day.id, activity)
        assertThrows(IllegalStateException::class.java) {
            removed.withoutActivity(day.id, activity)
        }
        val moved = itinerary.withUpdatedActivity(day.id, activity, 3, "2026-10-22", activity)
        assertThrows(IllegalStateException::class.java) {
            moved.withoutActivity(day.id, activity)
        }
    }
}
