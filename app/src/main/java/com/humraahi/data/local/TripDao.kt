package com.humraahi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query(
        """
        SELECT * FROM cached_trips
        WHERE cachedForUserId = :userId
        ORDER BY startDate ASC
        """
    )
    fun observeTrips(userId: String): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Query("DELETE FROM cached_trips WHERE cachedForUserId = :userId")
    suspend fun deleteTripsForUser(userId: String)

    @Transaction
    suspend fun replaceTrips(userId: String, trips: List<TripEntity>) {
        deleteTripsForUser(userId)
        insertTrips(trips)
    }
}
