package com.humraahi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TripDateRangeTest {
    @Test
    fun tripBoundsAreInclusiveAndDayNumberIsDerived() {
        val range = TripDateRange("2026-10-20", "2026-10-22")

        assertEquals(3, range.numberOfDays)
        assertEquals(1, range.dayNumber("2026-10-20"))
        assertEquals(2, range.dayNumber("2026-10-21"))
        assertEquals(3, range.dayNumber("2026-10-22"))
        assertEquals("2026-10-20", range.dateForDay(1))
        assertEquals("2026-10-22", range.dateForDay(3))
    }

    @Test
    fun rejectsDatesAndDayNumbersOutsideTrip() {
        val range = TripDateRange("2026-10-20", "2026-10-22")

        listOf("2026-10-19", "2026-10-23").forEach { date ->
            assertThrows(IllegalArgumentException::class.java) { range.dayNumber(date) }
        }
        listOf(0, 4).forEach { day ->
            assertThrows(IllegalArgumentException::class.java) { range.dateForDay(day) }
        }
    }

    @Test
    fun handlesSingleDayTripAndCrossYearRange() {
        assertEquals(1, TripDateRange("2026-10-20", "2026-10-20").numberOfDays)
        val range = TripDateRange("2026-12-31", "2027-01-02")
        assertEquals(3, range.dayNumber("2027-01-02"))
        assertEquals("2027-01-01", range.dateForDay(2))
    }

    @Test
    fun handlesLeapDayAndDaylightSavingDatesUsingUtc() {
        val leap = TripDateRange("2028-02-28", "2028-03-01")
        assertEquals(3, leap.numberOfDays)
        assertEquals("2028-02-29", leap.dateForDay(2))
        val daylightSaving = TripDateRange("2026-03-07", "2026-03-10")
        assertEquals(4, daylightSaving.numberOfDays)
        assertEquals(4, daylightSaving.dayNumber("2026-03-10"))
    }

    @Test
    fun rejectsInvalidTripDates() {
        assertThrows(IllegalArgumentException::class.java) {
            TripDateRange("2026-10-22", "2026-10-20")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TripDateRange("2026-02-30", "2026-03-01")
        }
    }
}
