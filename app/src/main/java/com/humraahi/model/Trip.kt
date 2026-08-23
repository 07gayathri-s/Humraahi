package com.humraahi.model
import java.util.UUID

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val members: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val createdBy: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "destination" to destination,
        "startDate" to startDate,
        "endDate" to endDate,
        "members" to members,
        "memberIds" to memberIds,
        "memberNames" to memberNames,
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
            memberNames = (map["memberNames"] as? Map<*, *>)
                ?.mapNotNull { (id, name) ->
                    if (id is String && name is String) id to name else null
                }
                ?.toMap()
                .orEmpty(),
            createdBy = map["createdBy"] as? String ?: ""
        )
    }
}
