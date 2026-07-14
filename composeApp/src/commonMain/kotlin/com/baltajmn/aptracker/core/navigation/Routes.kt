package com.baltajmn.aptracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

@Serializable
object RoomListRoute

@Serializable
data class RoomDetailRoute(val roomId: String, val roomName: String)

@Serializable
data class AddRoomRoute(val roomId: String? = null) // null = create, non-null = edit

@Serializable
data class SlotDetailRoute(val slotId: String, val slotName: String, val roomId: String)

@Serializable
object NotificationPrefsRoute

@Serializable
object SettingsRoute
