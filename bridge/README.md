# ApTracker Bridge

An always-on service that keeps the notifications working **even when the phone
app is closed**. It runs on your Raspberry Pi (or any always-on box), holds a
persistent spectator connection to your Archipelago room(s), and forwards every
item you receive to:

- **Supabase** (`activity` table) → the in-app history + live feed, and
- **ntfy** → a push notification on your phone.

The phone app never needs to hold a socket open in the background. It just reads
Supabase and subscribes to the ntfy topic.

```
Archipelago server(s) ──WS(Tracker)──► bridge ──► Supabase.activity
                                          │
                                          └──────► ntfy topic ──► phone push
```

## How it works

- Reads your `rooms` and `slots` from Supabase every `SYNC_INTERVAL_SECONDS`, so
  adding a room/slot in the app is picked up automatically — no restart needed.
- One connection **per room** (a single spectator watches every tracked slot in
  that room via the global `PrintJSON` stream).
- Connects with `tags: ["Tracker"]` and `items_handling: 0`, so it never kicks
  your real game client and never gets a replayed backlog on reconnect.
- Resolves numeric item/location IDs to real names using the server's
  `DataPackage`.
- Classifies each event from the tracked slot's point of view: `received`
  (someone sent you an item), `sent` (you found someone's item), `hint`.
- Applies each slot's notification preferences (progression / useful / filler,
  suppress local / others). Only `received` + `hint` push to your phone; `sent`
  is recorded for the feed but doesn't buzz you.

## Setup

### 1. Apply the DB migration

Run `supabase/migrations/20240102000000_ntfy_and_indexes.sql` against your
Supabase project (SQL editor or `supabase db push`). It adds `rooms.ntfy_topic`,
an index on `activity`, and puts `activity` in the realtime publication.

### 2. Configure

```bash
git clone <your-repo> ~/ApTracker      # or copy just the bridge/ folder
cd ~/ApTracker/bridge
cp .env.example .env
nano .env
```

Fill in:

- `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` (Supabase → Settings → API →
  **service_role** key — keep it secret, it stays on the Pi and bypasses RLS).
- `NTFY_BASE_URL` — `https://ntfy.sh`, or `http://ntfy:80` if you run the bundled
  ntfy container (see below).
- `NTFY_DEFAULT_TOPIC` — a random, hard-to-guess string. Anyone who knows a topic
  can read its messages, so treat it like a password. You can also set a
  per-room topic in the `rooms.ntfy_topic` column (the app can manage this).

### 3. Run it

#### Option A — Docker (recommended, fully isolated)

The compose stack runs on its own network, so it won't mix with your other Pi
integrations.

```bash
# Bridge only, using an external ntfy (e.g. ntfy.sh):
docker compose up -d --build

# …or bridge + a self-hosted ntfy on the Pi (set NTFY_BASE_URL=http://ntfy:80 in .env):
docker compose --profile ntfy up -d --build

docker compose logs -f aptracker-bridge      # follow the logs
docker compose pull && docker compose up -d  # update later
```

You should see `watching room '...'` and `tracking as spectator` once it
connects. Trigger an item send in-game and watch it log the event.

#### Option B — Python venv + systemd (no Docker)

```bash
sudo apt update && sudo apt install -y python3 python3-venv
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# Test in the foreground:
.venv/bin/python -m aptracker_bridge

# Then install as a service (edit paths/User in the unit first if needed):
sudo cp deploy/aptracker-bridge.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now aptracker-bridge
journalctl -u aptracker-bridge -f
```

## Receiving notifications on the phone

1. Install the **ntfy** app (Android / iOS) or use the in-app subscription if you
   wire it up.
2. Subscribe to your topic (`NTFY_DEFAULT_TOPIC`, or a room's `ntfy_topic`). For a
   self-hosted ntfy on the Pi, set the app's server to `http://<your-pi-ip>:8080`
   (for `https://ntfy.sh` no server change is needed).
3. That's it — pushes arrive whether or not ApTracker is open.

## Which push method should I use? (ntfy vs Firebase)

**Short answer: use ntfy.sh.** It's what this project ships with and it's the right
choice for a personal / friends deployment where users are in different homes.

| | **ntfy.sh** (recommended now) | **Firebase / FCM** (future upgrade) |
|---|---|---|
| Setup effort | Minimal — the bridge already does it | High — Firebase project, SDK in the app, token wiring |
| Cost | Free | Free tier is fine, **but iOS push needs a paid Apple Developer account ($99/yr)** |
| Works when app is closed | Yes | Yes |
| Where notifications appear | In the separate **ntfy** app | Natively, as **ApTracker** with your icon |
| Reaches users in other homes | Yes (public server) | Yes |
| Extra app for the user | Yes (ntfy) | No |

The catch with Firebase most people hit: on Android it's just an SDK + a token, but
**iOS push over FCM still requires Apple's APNs**, which means a paid Apple Developer
account and certificate setup. For a hobby project that alone usually makes ntfy the
better trade-off.

**How a future Firebase path would work** (nothing to do now — the `push_tokens`
table already exists for it):

1. Add the Firebase/FCM SDK to the app; on launch it gets a device *token* and saves
   it to Supabase `push_tokens` (per user/device).
2. The bridge, instead of (or in addition to) posting to ntfy, reads those tokens and
   calls Firebase's HTTP v1 API with a service-account key to push to each device.
3. The app shows the notification natively (needs `POST_NOTIFICATIONS` on Android 13+
   and APNs configured for iOS).

So: **ntfy.sh today**; revisit Firebase only if this grows into a polished, published
app and you're OK paying for the Apple Developer account. Switching later doesn't waste
anything — the bridge and DB are already structured for both.

### If you later want self-hosted push without ntfy.sh
Run the bundled `--profile ntfy` container and expose it with a **Cloudflare Tunnel**
(no router ports to open) so remote phones can reach it. Middle ground between ntfy.sh
and full Firebase.

## Notes / limits

- **Item/location names** depend on the server sending a `DataPackage`; unknown
  IDs fall back to `Item <id>` / `Location <id>`.
- **Slot names must match** the names in the Archipelago room exactly (case
  sensitive). If all tracked names in a room are invalid, the bridge logs
  `all tracked slot names were refused` and retries on the next sync.
- **One bridge instance** should run per deployment; two would double-insert
  activity.
- For a LAN server over plain `ws://`, no TLS config is needed. Set
  `AP_TLS_INSECURE=true` only for a self-signed `wss://` server.
