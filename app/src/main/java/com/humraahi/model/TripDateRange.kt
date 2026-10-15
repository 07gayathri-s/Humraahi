package com.humraahi.model

import com.humraahi.util.DateFormatter
import java.util.concurrent.TimeUnit

data class TripDateRange(val startDate: String, val endDate: String) {
    val startMillis: Long = DateFormatter.parseStorageDate(startDate)
    val endMillis: Long = DateFormatter.parseStorageDate(endDate)

    init {
        require(endMillis >= startMillis) { "Trip end date must not precede its start date." }
    }

    val numberOfDays: Int = TimeUnit.MILLISECONDS.toDays(endMillis - startMillis).toInt() + 1

    fun dayNumber(date: String): Int {
        val millis = DateFormatter.parseStorageDate(date)
        require(millis in startMillis..endMillis) {
            "Choose a date between $startDate and $endDate."
        }
        return TimeUnit.MILLISECONDS.toDays(millis - startMillis).toInt() + 1
    }

    fun dateForDay(dayNumber: Int): String {
        require(dayNumber in 1..numberOfDays) { "This day is outside the trip's dates." }
        return DateFormatter.formatForStorage(
            startMillis + TimeUnit.DAYS.toMillis(dayNumber.toLong() - 1)
        )
    }
}
