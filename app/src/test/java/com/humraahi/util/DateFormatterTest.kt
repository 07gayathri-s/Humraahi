package com.humraahi.util

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {
    private val dateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(2026, Calendar.DECEMBER, 20)
        timeInMillis
    }

    @Test
    fun formatsCanonicalDateForFirestore() {
        assertEquals("2026-12-20", DateFormatter.formatForStorage(dateMillis))
    }

    @Test
    fun formatsFriendlyDateForDisplay() {
        assertEquals("Dec 20, 2026", DateFormatter.formatForDisplay(dateMillis))
    }
}
