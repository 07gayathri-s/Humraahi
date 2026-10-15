package com.humraahi.model

import java.util.UUID

data class Itinerary(
    val tripId: String,
    val days: List<ItineraryDay> = emptyList()
) {
    fun withActivity(
        dayNumber: Int,
        activity: ItineraryActivity,
        date: String? = null
    ): Itinerary {
        require(dayNumber > 0) { "Day number must be at least 1." }
        require(activity.title.isNotBlank()) { "Activity name cannot be empty." }

        val existingDay = days.firstOrNull {
            if (date == null) it.dayNumber == dayNumber
            else it.date == date || (it.date == null && it.dayNumber == dayNumber)
        }
        val updatedDays = if (existingDay == null) {
            (days + ItineraryDay(dayNumber = dayNumber, date = date, activities = listOf(activity)))
                .sortedBy { it.dayNumber }
        } else {
            days.map { day ->
                if (day.id == existingDay.id) {
                    day.copy(
                        dayNumber = dayNumber,
                        date = date ?: day.date,
                        activities = day.activities + activity
                    )
                } else {
                    day
                }
            }
        }
        return copy(days = updatedDays)
    }

    fun withUpdatedActivity(
        sourceDayId: String,
        originalActivity: ItineraryActivity,
        dayNumber: Int,
        date: String,
        activity: ItineraryActivity
    ): Itinerary {
        require(dayNumber > 0) { "Day number must be at least 1." }
        require(activity.title.isNotBlank()) { "Activity name cannot be empty." }
        require(activity.id == originalActivity.id) { "An edit must preserve the activity ID." }
        val sourceDay = days.firstOrNull { it.id == sourceDayId }
            ?: error("This day no longer exists. Close and reopen the activity.")
        val current = sourceDay.activities.firstOrNull { it.id == originalActivity.id }
            ?: error("This activity no longer exists on this day. Close and reopen it.")
        check(current == originalActivity) {
            "This activity was changed by another member. Close and reopen it before editing."
        }
        val destination = days.firstOrNull {
            it.date == date || (it.date == null && it.dayNumber == dayNumber)
        }
        if (destination?.id == sourceDay.id) {
            return copy(days = days.map { day ->
                if (day.id == sourceDay.id) {
                    day.copy(
                        dayNumber = dayNumber,
                        date = date,
                        activities = day.activities.map { if (it.id == activity.id) activity else it }
                    )
                } else day
            })
        }
        val withoutOriginal = copy(days = days.map { day ->
            if (day.id == sourceDay.id) {
                day.copy(activities = day.activities.filterNot { it.id == originalActivity.id })
            } else day
        })
        return withoutOriginal.withActivity(dayNumber, activity, date)
    }

    fun toMap(): Map<String, Any> = mapOf(
        "days" to days.map(ItineraryDay::toMap)
    )

    companion object {
        fun fromMap(tripId: String, map: Map<String, Any>): Itinerary =
            Itinerary(
                tripId = tripId,
                days = (map["days"] as? List<*>)
                    ?.mapNotNull { day ->
                        (day as? Map<*, *>)?.toStringKeyMap()
                    }
                    ?.map(ItineraryDay::fromMap)
                    .orEmpty()
            )
    }
}

data class ItineraryDay(
    val id: String = UUID.randomUUID().toString(),
    val dayNumber: Int,
    val date: String? = null,
    val title: String = "",
    val activities: List<ItineraryActivity> = emptyList()
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("id", id)
        put("dayNumber", dayNumber)
        date?.let { put("date", it) }
        put("title", title)
        put("activities", activities.map(ItineraryActivity::toMap))
    }

    companion object {
        fun fromMap(map: Map<String, Any>): ItineraryDay =
            ItineraryDay(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                dayNumber = (map["dayNumber"] as? Number)?.toInt() ?: 0,
                date = map["date"] as? String,
                title = map["title"] as? String ?: "",
                activities = (map["activities"] as? List<*>)
                    ?.mapNotNull { activity ->
                        (activity as? Map<*, *>)?.toStringKeyMap()
                    }
                    ?.map(ItineraryActivity::fromMap)
                    .orEmpty()
            )
    }
}

data class ItineraryActivity(
    val id: String = UUID.randomUUID().toString(),
    val startTime: String? = null,
    val title: String,
    val location: String? = null,
    val notes: String? = null
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("id", id)
        startTime?.let { put("startTime", it) }
        put("title", title)
        location?.let { put("location", it) }
        notes?.let { put("notes", it) }
    }

    companion object {
        fun fromMap(map: Map<String, Any>): ItineraryActivity =
            ItineraryActivity(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                startTime = map["startTime"] as? String,
                title = map["title"] as? String ?: "",
                location = map["location"] as? String,
                notes = map["notes"] as? String
            )
    }
}

private fun Map<*, *>.toStringKeyMap(): Map<String, Any> =
    entries.mapNotNull { (key, value) ->
        if (key is String && value != null) key to value else null
    }.toMap()
