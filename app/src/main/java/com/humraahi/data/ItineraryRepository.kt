package com.humraahi.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.humraahi.model.Itinerary
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

    private fun itineraryDocument(tripId: String) =
        db.collection("trips")
            .document(tripId)
            .collection("itineraries")
            .document("current")
}
