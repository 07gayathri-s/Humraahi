package com.humraahi.util

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatForStorage(epochMillis: Long): String =
        formatter("yyyy-MM-dd").format(Date(epochMillis))

    fun formatForDisplay(epochMillis: Long): String =
        formatter("MMM d, yyyy").format(Date(epochMillis))

    fun parseStorageDate(value: String): Long {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) {
            "Invalid date: $value. Expected yyyy-MM-dd."
        }
        val position = ParsePosition(0)
        val date = formatter("yyyy-MM-dd").apply { isLenient = false }.parse(value, position)
        require(date != null && position.index == value.length) {
            "Invalid date: $value. Expected a real calendar date."
        }
        return date.time
    }

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = utc
    }
}
