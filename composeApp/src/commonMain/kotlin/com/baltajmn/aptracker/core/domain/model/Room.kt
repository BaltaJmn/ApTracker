package com.baltajmn.aptracker.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String,
    val host: String,
    val port: Int = 38281,
    val password: String? = null,
    @SerialName("ntfy_topic") val ntfyTopic: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
