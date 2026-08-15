package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TripTest {
    @Test
    fun firestoreMapRoundTripPreservesTripData() {
        val original = Trip(
            id = "trip-1",
            destination = "Goa",
            startDate = "2026-12-20",
            endDate = "2026-12-25",
            members = listOf("Gayathri"),
            memberIds = listOf("user-123"),
            createdBy = "user-123"
        )

        val restored = Trip.fromMap(original.id, original.toMap())

        assertEquals(original, restored)
    }
}
