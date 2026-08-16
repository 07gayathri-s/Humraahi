package com.humraahi.data.local

import com.humraahi.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

class TripEntityTest {
    @Test
    fun entityRoundTripPreservesCachedTripFields() {
        val trip = Trip(
            id = "trip-1",
            destination = "Goa",
            startDate = "2026-12-20",
            endDate = "2026-12-25",
            createdBy = "user-1"
        )

        val entity = TripEntity.fromTrip("user-1", trip)

        assertEquals("user-1", entity.cachedForUserId)
        assertEquals(trip, entity.toTrip())
    }
}
