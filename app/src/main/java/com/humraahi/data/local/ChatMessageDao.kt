package com.humraahi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query(
        """
        SELECT * FROM chat_messages
        WHERE tripId = :tripId
        ORDER BY timestamp ASC
        """
    )
    fun observeMessages(tripId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE tripId = :tripId")
    suspend fun deleteMessagesForTrip(tripId: String)

    @Transaction
    suspend fun replaceMessages(
        tripId: String,
        messages: List<ChatMessageEntity>
    ) {
        deleteMessagesForTrip(tripId)
        insertMessages(messages)
    }
}
