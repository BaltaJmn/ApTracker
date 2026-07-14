# KMP Patterns

## expect/actual for platform differences
```kotlin
// commonMain
expect fun createPlatformSpecificThing(): Thing

// androidMain
actual fun createPlatformSpecificThing(): Thing = AndroidThing()

// iosMain
actual fun createPlatformSpecificThing(): Thing = IOSThing()
```

## Ktor engine per platform
- `commonMain`: `ktor-client-core`, `ktor-client-websockets`
- `androidMain`: `ktor-client-android`
- `iosMain`: `ktor-client-darwin`

## Supabase-kt initialization (commonMain)
```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}
```

## Source set dependency rules
- Only add to `commonMain` if available for all platforms
- Platform-specific engines/SDKs go in `androidMain` / `iosMain`
- Test helpers go in `commonTest`

## Compose Resources
Access via generated `Res` object: `Res.drawable.icon`, `Res.string.title`
Resources live in `commonMain/composeResources/`
