package com.humraahi.model
import java.util.UUID

data class Trip(
        val id: String = UUID.randomUUID().toString(),
        val destination: String = "",
        val startDate: String = "",
        val endDate: String = "",
        val members: List<String> = emptyList()
)
