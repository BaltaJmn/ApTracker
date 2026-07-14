# ApTracker

A mobile tracker for [Archipelago](https://archipelago.gg) multiworlds with **push
notifications when someone unlocks an item for you** — even while your phone is in
your pocket and your PC is off.

Built with **Kotlin Multiplatform + Compose Multiplatform** (Android & iOS), a
**Supabase** backend, and a small always-on **bridge** (Docker) that watches your
games 24/7 from a Raspberry Pi or any always-on box.

Inspired by [wrjones104/ap-tracker](https://github.com/wrjones104/ap-tracker).

## How it works

```
Archipelago server ──WS (Tracker tag)──► bridge (Docker, 24/7) ──► Supabase ──► mobile app (live feed)
                                              │
                                              ├──► ntfy ──► push (iOS & Android)
                                              └──► FCM  ──► native push (Android, optional)
```

- The **bridge** connects to each room as a read-only spectator (`Tracker` tag), so
  it never conflicts with your real game client. It resolves item/location names via
  the server's DataPackage and classifies every event per tracked slot.
- The **app** is a viewer: it reads the activity feed from Supabase (with Realtime
  live updates) and manages your rooms, slots, and notification preferences.
- Notifications are filtered per slot: progression / useful / filler, with options
  to suppress your own finds or others'.
- The bridge only makes **outbound** connections — no ports to open at home.

## Notifications

| Platform | How |
|---|---|
| Android (app built with Firebase) | Native push via FCM — no extra app needed |
| Android (no Firebase) | [ntfy](https://ntfy.sh) app, subscribe to your room's topic |
| iOS | [ntfy](https://ntfy.sh) app, subscribe to your room's topic |

Each room gets a random, hard-to-guess ntfy topic (shown in the app with a copy
button). ntfy.sh works for players spread across different homes with zero setup.

## Self-hosting (one person per group does this)

### 1. Supabase (free tier is fine)

1. Create a project at [supabase.com](https://supabase.com).
2. In the SQL Editor, run both files from [`supabase/migrations/`](supabase/migrations) in order.
3. Optionally disable **Authentication → Providers → Email → Confirm email** so
   friends can sign up without a confirmation mail.

### 2. Build the app

**Option A — GitHub Actions (no Android Studio needed):** fork this repo, add
repository secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY` (plus optionally
`GOOGLE_SERVICES_JSON` for native push and `KEYSTORE_*` for signing — see the
header of [`.github/workflows/release.yml`](.github/workflows/release.yml)),
then push a tag:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

The signed APK appears attached to the release a few minutes later. Your group
downloads it straight from your fork's **Releases** page.

**Option B — local build:**

```bash
cp composeApp/src/commonMain/kotlin/com/baltajmn/aptracker/core/data/SupabaseConfig.kt.template \
   composeApp/src/commonMain/kotlin/com/baltajmn/aptracker/core/data/SupabaseConfig.kt
# edit it: your project URL + anon key (Settings → API)

./gradlew :composeApp:assembleDebug
# APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk — share it with your group
```

iOS: open [`iosApp/`](iosApp) in Xcode and run (requires a Mac).

**Optional — native Android push (FCM):** create a free Firebase project, add an
Android app with package `com.baltajmn.aptracker`, download `google-services.json`
into `composeApp/`, and rebuild. Without it the app builds fine and Android users
just use ntfy. See [`composeApp/google-services.json.template`](composeApp/google-services.json.template).

### 3. Run the bridge (any always-on box: Raspberry Pi, NAS, VPS…)

```bash
cd bridge
cp .env.example .env      # fill in: Supabase URL + service_role key, ntfy topic
docker compose up -d --build
docker compose logs -f aptracker-bridge   # expect: "watching room…", "tracking as spectator"
```

Full bridge docs (self-hosted ntfy, FCM service account, systemd alternative):
[`bridge/README.md`](bridge/README.md). Spanish quick-start: [`SETUP.md`](SETUP.md).

### 4. Each player

1. Install the APK, sign up.
2. **Add Room** → the Archipelago server's host + port (+ room password if any).
3. Open the room → **add your slot** (exact player name, case-sensitive).
4. Subscribe to the room's notification topic in the ntfy app (skip if using
   native FCM push).

The bridge picks up new rooms/slots automatically within a minute.

## Repo layout

```
composeApp/   Kotlin Multiplatform app (Android + iOS, Compose Multiplatform)
bridge/       Python bridge: Archipelago WS → Supabase + ntfy/FCM (Docker)
supabase/     SQL migrations (schema + RLS)
iosApp/       Xcode entry point for the iOS app
```

## Security model

- The app ships the Supabase **anon key** (public by design); Row Level Security
  ensures each user only sees their own rooms/slots/activity.
- The **service_role key** and the Firebase **service account** live only on the
  bridge box (`bridge/.env` + `bridge/service-account.json`, both gitignored and
  dockerignored). The bridge admin can read the group's room passwords — run one
  bridge per friend group, by someone the group trusts.

## License

[MIT](LICENSE)
