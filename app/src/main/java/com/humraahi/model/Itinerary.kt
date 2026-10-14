package com.humraahi.model

import java.util.UUID

data class Itinerary(
    val tripId: String,
    val days: List<ItineraryDay> = emptyList()
) {
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
