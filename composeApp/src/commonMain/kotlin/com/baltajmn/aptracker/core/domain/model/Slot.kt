package com.baltajmn.aptracker.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Slot(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("room_id") val roomId: String,
    @SerialName("slot_name") val slotName: String,
    @SerialName("game_name") val gameName: String? = null,
    @SerialName("notify_enabled") val notifyEnabled: Boolean = true,
    @SerialName("notify_progression") val notifyProgression: Boolean = true,
    @SerialName("notify_useful") val notifyUseful: Boolean = true,
    @SerialName("notify_filler") val notifyFiller: Boolean = false,
    @SerialName("suppress_local") val suppressLocal: Boolean = false,
    @SerialName("suppress_others") val suppressOthers: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)
