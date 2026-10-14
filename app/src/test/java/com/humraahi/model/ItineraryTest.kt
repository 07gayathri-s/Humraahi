package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ItineraryTest {
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
