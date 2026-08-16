package com.humraahi.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import com.humraahi.model.Trip
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val db = Firebase.firestore

    fun observeTrips(userId: String): Flow<List<Trip>> = callbackFlow {
        val listener = db.collection("trips")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.data?.let { Trip.fromMap(document.id, it) }
                    }
                    .orEmpty()
                    .sortedBy { it.startDate }

                trySend(trips)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createTrip(trip: Trip) {
        db.collection("trips")
            .document(trip.id)
            .set(trip.toMap())
            .await()
    }

    suspend fun joinTrip(tripId: String, userId: String) {
        db.collection("trips")
            .document(tripId)
            .update("memberIds", FieldValue.arrayUnion(userId))
            .await()
    }
}
