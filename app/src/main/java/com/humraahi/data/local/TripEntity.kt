package com.humraahi.data.local

import androidx.room.Entity
import com.humraahi.model.Trip

@Entity(
    tableName = "cached_trips",
    primaryKeys = ["cachedForUserId", "id"]
)
data class TripEntity(
    val cachedForUserId: String,
    val id: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val createdBy: String
) {
    fun toTrip() = Trip(
        id = id,
        destination = destination,
        startDate = startDate,
        endDate = endDate,
        createdBy = createdBy
    )

    companion object {
        fun fromTrip(userId: String, trip: Trip) = TripEntity(
            cachedForUserId = userId,
            id = trip.id,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            createdBy = trip.createdBy
        )
    }
}
