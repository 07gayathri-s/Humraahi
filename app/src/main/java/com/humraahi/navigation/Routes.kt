package com.humraahi.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val NEW_TRIP = "new_trip"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val PROFILE = "profile"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"
}
