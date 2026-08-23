package com.humraahi.navigation

object Routes {
    private const val TRIP_INVITE_BASE_URL = "https://humraahi-ed56b.web.app/trip"

    const val LOGIN = "login"
    const val HOME = "home"
    const val NEW_TRIP = "new_trip"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val PROFILE = "profile"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"

    fun tripInviteUrl(tripId: String) = "$TRIP_INVITE_BASE_URL/$tripId"
}
