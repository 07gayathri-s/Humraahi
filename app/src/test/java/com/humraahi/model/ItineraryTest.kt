package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ItineraryTest {
    @Test
    fun addingFirstActivityCreatesDayWithoutChangingOriginal() {
        val itinerary = Itinerary(tripId = "trip-1")
        val activity = ItineraryActivity(id = "activity-1", title = "Beach visit")

        val updated = itinerary.withActivity(1, activity)

        assertEquals("trip-1", updated.tripId)
        assertEquals(1, updated.days.single().dayNumber)
        assertEquals(listOf(activity), updated.days.single().activities)
        assertEquals(emptyList<ItineraryDay>(), itinerary.days)
    }

    @Test
    fun addingToExistingDayPreservesDetailsAndOtherDays() {
        val firstActivity = ItineraryActivity(id = "activity-1", title = "Check-in")
        val newActivity = ItineraryActivity(id = "activity-2", title = "Beach visit")
        val firstDay = ItineraryDay(
            id = "day-1",
            dayNumber = 1,
            date = "2026-10-20",
            title = "Arrival",
            activities = listOf(firstActivity)
        )
        val secondDay = ItineraryDay(id = "day-2", dayNumber = 2, title = "Explore")
        val itinerary = Itinerary(tripId = "trip-1", days = listOf(firstDay, secondDay))

        val updated = itinerary.withActivity(1, newActivity)

        assertEquals(
            firstDay.copy(activities = listOf(firstActivity, newActivity)),
            updated.days.first()
        )
        assertEquals(secondDay, updated.days.last())
        assertEquals(listOf(firstActivity), itinerary.days.first().activities)
    }

    @Test
    fun addingNewDayPlacesItInDayNumberOrder() {
        val firstDay = ItineraryDay(id = "day-1", dayNumber = 1)
        val thirdDay = ItineraryDay(id = "day-3", dayNumber = 3)
        val itinerary = Itinerary(tripId = "trip-1", days = listOf(firstDay, thirdDay))
        val activity = ItineraryActivity(id = "activity-1", title = "Explore")

        val updated = itinerary.withActivity(2, activity)

        assertEquals(listOf(1, 2, 3), updated.days.map { it.dayNumber })
        assertEquals(firstDay, updated.days.first())
        assertEquals(thirdDay, updated.days.last())
        assertEquals(listOf(activity), updated.days[1].activities)
    }

    @Test
    fun addingMultipleActivitiesToSameDayDoesNotCreateDuplicateDays() {
        val first = ItineraryActivity(id = "activity-1", title = "Check-in")
        val second = ItineraryActivity(id = "activity-2", title = "Beach visit")

        val updated = Itinerary(tripId = "trip-1")
            .withActivity(1, first)
            .withActivity(1, second)

        assertEquals(1, updated.days.size)
        assertEquals(listOf(first, second), updated.days.single().activities)
    }

    @Test
    fun addingActivityRejectsInvalidDayAndBlankTitle() {
        val itinerary = Itinerary(tripId = "trip-1")
        val activity = ItineraryActivity(title = "Explore")

        assertThrows(IllegalArgumentException::class.java) {
            itinerary.withActivity(0, activity)
        }
        assertThrows(IllegalArgumentException::class.java) {
            itinerary.withActivity(-1, activity)
        }
        assertThrows(IllegalArgumentException::class.java) {
            itinerary.withActivity(1, ItineraryActivity(title = " "))
        }
    }

    @Test
    fun mapRoundTripPreservesDayAndActivityOrder() {
        val itinerary = Itinerary(
            tripId = "trip-1",
            days = listOf(
                ItineraryDay(
                    id = "day-1",
                    dayNumber = 1,
                    date = "2026-09-12",
                    title = "Arrival and sunset",
                    activities = listOf(
                        ItineraryActivity(
                            id = "activity-1",
                            startTime = "15:00",
                            title = "Hotel check-in",
                            location = "Candolim"
                        ),
                        ItineraryActivity(
                            id = "activity-2",
                            startTime = "18:00",
                            title = "Sunset at the beach",
                            location = "Sinquerim Beach",
                            notes = "Leave the hotel by 17:30."
                        )
                    )
                ),
                ItineraryDay(
                    id = "day-2",
                    dayNumber = 2,
                    title = "Explore North Goa"
                )
            )
        )

        val restored = Itinerary.fromMap(itinerary.tripId, itinerary.toMap())

        assertEquals(itinerary, restored)
        assertEquals("activity-1", restored.days.first().activities.first().id)
        assertEquals("day-2", restored.days.last().id)
    }

    @Test
    fun optionalFieldsCanBeAbsent() {
        val activity = ItineraryActivity.fromMap(
            mapOf(
                "id" to "activity-1",
                "title" to "Explore the market"
            )
        )

        assertNull(activity.startTime)
        assertNull(activity.location)
        assertNull(activity.notes)
    }

    @Test
    fun emptyItineraryRoundTripPreservesTripId() {
        val itinerary = Itinerary(tripId = "trip-1")

        val restored = Itinerary.fromMap(itinerary.tripId, itinerary.toMap())

        assertEquals(itinerary, restored)
        assertEquals(emptyList<ItineraryDay>(), restored.days)
    }

    @Test
    fun firestoreLongDayNumberIsReadAsInt() {
        val itinerary = Itinerary.fromMap(
            tripId = "trip-1",
            map = mapOf(
                "days" to listOf(
                    mapOf(
                        "id" to "day-1",
                        "dayNumber" to 1L,
                        "title" to "Arrival",
                        "activities" to emptyList<Map<String, Any>>()
                    )
                )
            )
        )

        assertEquals(1, itinerary.days.single().dayNumber)
        assertEquals("day-1", itinerary.days.single().id)
    }

    @Test
    fun serializationOmitsUnknownOptionalFields() {
        val activity = ItineraryActivity(
            id = "activity-1",
            title = "Explore the market"
        )
        val day = ItineraryDay(id = "day-1", dayNumber = 1)

        val activityMap = activity.toMap()

        assertFalse(activityMap.containsKey("startTime"))
        assertFalse(activityMap.containsKey("location"))
        assertFalse(activityMap.containsKey("notes"))
        assertFalse(day.toMap().containsKey("date"))
        assertEquals(activity, ItineraryActivity.fromMap(activityMap))
    }
}
