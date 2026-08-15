package com.humraahi.model
import java.util.UUID

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val members: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val createdBy: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "destination" to destination,
        "startDate" to startDate,
        "endDate" to endDate,
        "members" to members,
        "memberIds" to memberIds,
        "createdBy" to createdBy
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): Trip = Trip(
            id = id,
            destination = map["destination"] as? String ?: "",
            startDate = map["startDate"] as? String ?: "",
            endDate = map["endDate"] as? String ?: "",
            members = (map["members"] as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty(),
            memberIds = (map["memberIds"] as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty(),
            createdBy = map["createdBy"] as? String ?: ""
        )
    }
}
