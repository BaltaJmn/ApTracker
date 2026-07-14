# Supabase-kt Patterns

## Auth operations
```kotlin
// Login
supabase.auth.signInWith(Email) {
    email = userEmail
    password = userPassword
}

// Register
supabase.auth.signUpWith(Email) {
    email = userEmail
    password = userPassword
}

// Current session
val session = supabase.auth.currentSessionOrNull()
val userId = supabase.auth.currentUserOrNull()?.id

// Logout
supabase.auth.signOut()
```

## PostgREST queries
```kotlin
// Select all
val rooms = supabase.from("rooms").select().decodeList<Room>()

// Filter by user
val rooms = supabase.from("rooms")
    .select { filter { eq("user_id", userId) } }
    .decodeList<Room>()

// Insert
supabase.from("rooms").insert(newRoom)

// Update
supabase.from("rooms").update({ set("name", newName) }) { filter { eq("id", roomId) } }

// Delete
supabase.from("rooms").delete { filter { eq("id", roomId) } }
```

## Realtime subscriptions
```kotlin
val channel = supabase.realtime.channel("activity-$roomId")
channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
    table = "activity"
    filter = "room_id=eq.$roomId"
}.collect { action ->
    val event = action.decodeRecord<ActivityEvent>()
    // handle event
}
supabase.realtime.connect()
channel.subscribe()
// Cleanup: channel.unsubscribe(); supabase.realtime.disconnect()
```

## Model annotations
```kotlin
@Serializable
data class Room(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String,
    val host: String,
    val port: Int = 38281,
    val password: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
```

## Supabase client location
`core/data/SupabaseClientProvider.kt` — singleton via Koin `single { ... }`
Credentials stored in `local.properties` (gitignored), exposed via BuildConfig.
