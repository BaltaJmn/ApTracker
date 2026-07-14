package com.baltajmn.aptracker.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityEvent(
    val id: String = "",
    @SerialName("room_id") val roomId: String,
    @SerialName("slot_name") val slotName: String? = null,
    @SerialName("event_type") val eventType: String, // received | sent | hint
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("item_flags") val itemFlags: Int = 0, // 1=progression, 2=useful, 4=trap
    val timestamp: String = ""
) {
    val isProgression: Boolean get() = itemFlags and 1 != 0
    val isUseful: Boolean get() = itemFlags and 2 != 0
    val isTrap: Boolean get() = itemFlags and 4 != 0
}
