package com.humraahi.ui.itinerary

import com.humraahi.model.ItineraryActivity

data class ActivityEditTarget(
    val dayId: String,
    val date: String?,
    val activity: ItineraryActivity
)
