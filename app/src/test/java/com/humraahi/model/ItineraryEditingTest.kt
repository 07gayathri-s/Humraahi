package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ItineraryEditingTest {
    private val original = ItineraryActivity(id = "activity-1", title = "Beach", startTime = "18:00")
    private val another = ItineraryActivity(id = "activity-2", title = "Dinner")
    private val firstDay = ItineraryDay(
        id = "day-1", dayNumber = 1, title = "Arrival", activities = listOf(original, another)
    )
    private val secondDay = ItineraryDay(
        id = "day-2", dayNumber = 2, date = "2026-10-21", title = "Explore",
        activities = listOf(ItineraryActivity(id = "activity-3", title = "Market"))
    )
    private val itinerary = Itinerary(tripId = "trip-1", days = listOf(firstDay, secondDay))
    private val edited = original.copy(title = "Sunset", startTime = "18:30", location = "Goa")

    @Test
    fun editPreservesIdentityPositionDayDetailsAndOriginalObject() {
        val updated = itinerary.withUpdatedActivity("day-1", original, 1, "2026-10-20", edited)

        assertEquals(
            firstDay.copy(date = "2026-10-20", activities = listOf(edited, another)),
            updated.days.first()
        )
        assertEquals(secondDay, updated.days.last())
        assertEquals(original, itinerary.days.first().activities.first())
    }

    @Test
    fun movingToExistingDatePreservesOtherActivitiesAndDayMetadata() {
        val updated = itinerary.withUpdatedActivity("day-1", original, 2, "2026-10-21", edited)

        assertEquals(firstDay.copy(activities = listOf(another)), updated.days.first())
        assertEquals(
            secondDay.copy(activities = secondDay.activities + edited),
            updated.days.last()
        )
        assertEquals(1, updated.days.flatMap { it.activities }.count { it.id == original.id })
    }

    @Test
    fun movingToNewDateCreatesDayAndRetainsEmptySourceDay() {
        val singleActivityPlan = itinerary.copy(days = listOf(firstDay.copy(activities = listOf(original))))

        val updated = singleActivityPlan.withUpdatedActivity(
            "day-1", original, 3, "2026-10-22", edited
        )

        assertEquals(listOf(1, 3), updated.days.map { it.dayNumber })
        assertEquals("Arrival", updated.days.first().title)
        assertEquals(emptyList<ItineraryActivity>(), updated.days.first().activities)
        assertEquals("2026-10-22", updated.days.last().date)
        assertEquals(listOf(edited), updated.days.last().activities)
    }

    @Test
    fun editingRetainsOtherMembersConcurrentChanges() {
        val addedByFriend = ItineraryActivity(id = "activity-4", title = "Coffee")
        val latest = itinerary.copy(
            days = listOf(firstDay.copy(activities = firstDay.activities + addedByFriend), secondDay)
        )

        val updated = latest.withUpdatedActivity("day-1", original, 1, "2026-10-20", edited)

        assertEquals(listOf(edited, another, addedByFriend), updated.days.first().activities)
    }

    @Test
    fun rejectsConcurrentChangeToSameActivity() {
        val latest = itinerary.copy(
            days = listOf(firstDay.copy(activities = listOf(original.copy(title = "Changed"), another)))
        )

        assertThrows(IllegalStateException::class.java) {
            latest.withUpdatedActivity("day-1", original, 1, "2026-10-20", edited)
        }
    }

    @Test
    fun rejectsRemovedDayOrActivityInsteadOfRecreatingIt() {
        assertThrows(IllegalStateException::class.java) {
            itinerary.withUpdatedActivity("missing-day", original, 1, "2026-10-20", edited)
        }
        val removed = itinerary.copy(days = listOf(firstDay.copy(activities = listOf(another))))
        assertThrows(IllegalStateException::class.java) {
            removed.withUpdatedActivity("day-1", original, 1, "2026-10-20", edited)
        }
    }

    @Test
    fun rejectsChangedIdAndBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            itinerary.withUpdatedActivity(
                "day-1", original, 1, "2026-10-20", edited.copy(id = "different-id")
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            itinerary.withUpdatedActivity("day-1", original, 1, "2026-10-20", edited.copy(title = " "))
        }
    }

    @Test
    fun addingToLegacyNumberedDayAssignsDateWithoutCreatingDuplicateDay() {
        val updated = itinerary.withActivity(1, edited.copy(id = "new-activity"), "2026-10-20")

        assertEquals(2, updated.days.size)
        assertEquals("day-1", updated.days.first().id)
        assertEquals("2026-10-20", updated.days.first().date)
        assertEquals(3, updated.days.first().activities.size)
    }
}
