package com.humraahi.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatForStorage(epochMillis: Long): String =
        formatter("yyyy-MM-dd").format(Date(epochMillis))

    fun formatForDisplay(epochMillis: Long): String =
        formatter("MMM d, yyyy").format(Date(epochMillis))

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = utc
    }
}
