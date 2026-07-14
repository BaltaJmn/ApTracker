# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew :composeApp:assembleDebug          # Android debug build
./gradlew :composeApp:assembleRelease        # Android release build
./gradlew :composeApp:testDebugUnitTest      # Run unit tests
./gradlew clean                              # Clean build
# iOS: build from Xcode using iosApp/ directory
```

## Architecture

**Kotlin Multiplatform + Compose Multiplatform** app for Android & iOS. Single `composeApp` module, organized by packages:

```
composeApp/src/commonMain/kotlin/com/baltajmn/aptracker/
├── App.kt                    # KoinApplication { NavHost }
├── feature/
│   ├── auth/                 # Login / Signup (Supabase Auth)
│   ├── rooms/                # CRUD Archipelago server connections
│   ├── slots/                # Player slots + real-time WebSocket
│   ├── notifications/        # Per-slot notification preferences
│   └── settings/             # Account, logout
└── core/
    ├── ui/                   # Material3 theme, shared Composables
    ├── navigation/           # Routes (type-safe @Serializable objects), NavHost
    ├── network/              # ArchipelagoWebSocketClient (Ktor WS)
    ├── data/                 # Supabase client, repository implementations
    └── domain/               # Models, repository interfaces, use cases
```

Each feature follows **MVVM**: `Screen → ViewModel → UseCase → Repository(interface) → SupabaseRepo(impl)`

See detailed patterns in:
- `.claude/rules/compose-arch.md` — MVVM conventions, ViewModel, state hoisting
- `.claude/rules/supabase-kt.md` — Auth, PostgREST, Realtime patterns
- `.claude/rules/kmp-patterns.md` — expect/actual, source sets, Ktor engines
- `.claude/skills/archipelago-ws.md` — Archipelago WebSocket protocol reference

## Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Compose Multiplatform | 1.10.0 | Shared UI |
| Kotlin | 2.3.0 | Language |
| Supabase-kt (BOM) | 3.3.0 | Auth + DB + Realtime |
| Ktor | 3.3.3 | HTTP + WebSocket client |
| Koin | 4.1.1 | Dependency injection |
| Navigation Compose KMP | 2.9.2 | Type-safe navigation |
| kotlinx.serialization | 1.10.0 | JSON |
| Material3 | 1.10.0-alpha05 | Design system |
| Min SDK | 24 | Android 7.0+ |

## Key Conventions

- **DI**: Koin modules in `core/di/`. Use `koinViewModel()` in screens.
- **State**: `StateFlow<UiState>` in ViewModels, `collectAsStateWithLifecycle()` in screens.
- **Routes**: `@Serializable` objects/data classes in `core/navigation/Routes.kt`.
- **Supabase models**: `@Serializable` data classes with `@SerialName` for snake_case fields.
- **Platform engines**: `ktor-client-android` in `androidMain`, `ktor-client-darwin` in `iosMain`.
- **Supabase credentials**: `core/data/SupabaseConfig.kt` (gitignored; copy from `SupabaseConfig.kt.template`).
- **Firebase (optional)**: `composeApp/google-services.json` (gitignored; plugin applied conditionally — builds fine without it).

## Supabase Schema

Tables: `rooms`, `slots`, `activity`, `push_tokens` — all with RLS (`user_id = auth.uid()`).
SQL schema in `supabase/migrations/` (create when needed).

## WebSocket (Archipelago)
The app does NOT open WebSockets — it is a viewer over Supabase (REST + Realtime).
The Archipelago connection lives in `bridge/` (Python, Docker): one spectator
connection per room with `tags: ["Tracker"]`, DataPackage name resolution, and
push via ntfy + optional FCM. See `bridge/README.md` and `.claude/skills/archipelago-ws.md`.
