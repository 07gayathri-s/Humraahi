package com.humraahi.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.ktx.Firebase
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.model.Trip
import com.humraahi.model.TripDateRange
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ItineraryRepository {
    private val db = Firebase.firestore

    fun observeItinerary(tripId: String): Flow<Itinerary?> = callbackFlow {
        val listener = itineraryDocument(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    close(IllegalStateException("Itinerary snapshot was not received."))
                    return@addSnapshotListener
                }

                trySend(snapshot.data?.let { Itinerary.fromMap(tripId, it) })
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveItinerary(itinerary: Itinerary) {
        itineraryDocument(itinerary.tripId)
            .set(itinerary.toMap())
            .await()
    }

    suspend fun addActivity(tripId: String, date: String, activity: ItineraryActivity) {
        val document = itineraryDocument(tripId)
        db.runTransaction { transaction ->
            val dayNumber = readTripDates(transaction, tripId).dayNumber(date)
            val snapshot = transaction.get(document)
            val itinerary = snapshot.data?.let { Itinerary.fromMap(tripId, it) }
                ?: Itinerary(tripId = tripId)
            transaction.set(document, itinerary.withActivity(dayNumber, activity, date).toMap())
        }.await()
    }

    suspend fun updateActivity(
        tripId: String,
        sourceDayId: String,
        originalActivity: ItineraryActivity,
        date: String,
        activity: ItineraryActivity
    ) {
        val document = itineraryDocument(tripId)
        db.runTransaction { transaction ->
            val dayNumber = readTripDates(transaction, tripId).dayNumber(date)
            val snapshot = transaction.get(document)
            val itinerary = snapshot.data?.let { Itinerary.fromMap(tripId, it) }
                ?: error("This itinerary no longer exists. Close and reopen it.")
            val updated = itinerary.withUpdatedActivity(
                sourceDayId, originalActivity, dayNumber, date, activity
            )
            transaction.set(document, updated.toMap())
        }.await()
    }

    suspend fun deleteActivity(
        tripId: String,
        sourceDayId: String,
        originalActivity: ItineraryActivity
    ) {
        val document = itineraryDocument(tripId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            val itinerary = snapshot.data?.let { Itinerary.fromMap(tripId, it) }
                ?: error("This itinerary no longer exists. Close and reopen it.")
            transaction.set(
                document,
                itinerary.withoutActivity(sourceDayId, originalActivity).toMap()
            )
        }.await()
    }

    private fun readTripDates(transaction: Transaction, tripId: String): TripDateRange {
        val snapshot = transaction.get(db.collection("trips").document(tripId))
        val trip = snapshot.data?.let { Trip.fromMap(tripId, it) }
            ?: error("This trip no longer exists.")
        return TripDateRange(trip.startDate, trip.endDate)
    }

    private fun itineraryDocument(tripId: String) =
        db.collection("trips")
            .document(tripId)
            .collection("itineraries")
            .document("current")
}
