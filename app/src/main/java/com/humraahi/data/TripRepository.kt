package com.humraahi.data

import android.content.Context
import android.database.sqlite.SQLiteException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import com.humraahi.data.local.HumraahiDatabase
import com.humraahi.data.local.TripEntity
import com.humraahi.model.Trip
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TripRepository(context: Context) {
    private val db = Firebase.firestore
    private val tripDao = HumraahiDatabase.getInstance(context).tripDao()

    private val _syncErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncErrors = _syncErrors.asSharedFlow()

    fun observeTrips(userId: String): Flow<List<Trip>> = channelFlow {
        val syncJob = launch {
            observeRemoteTrips(userId).collect { trips ->
                val entities = trips.map { trip ->
                    TripEntity.fromTrip(userId, trip)
                }

                try {
                    tripDao.replaceTrips(userId, entities)
                } catch (error: SQLiteException) {
                    _syncErrors.tryEmit(
                        error.message ?: "Trips could not be saved for offline use."
                    )
                }
            }
        }

        try {
            tripDao.observeTrips(userId)
                .map { entities -> entities.map(TripEntity::toTrip) }
                .collect { trips -> send(trips) }
        } finally {
            syncJob.cancel()
        }
    }

    private fun observeRemoteTrips(userId: String): Flow<List<Trip>> = callbackFlow {
        val listener = db.collection("trips")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncErrors.tryEmit(error.message ?: "Trips could not be synchronized.")
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.data?.let { Trip.fromMap(document.id, it) }
                    }
                    .orEmpty()

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
