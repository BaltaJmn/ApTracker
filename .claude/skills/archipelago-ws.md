# Archipelago WebSocket Protocol

Archipelago uses a WebSocket-based JSON protocol on `ws://host:port` (default 38281).

## Connection sequence
1. Connect to `ws://host:port`
2. Server sends `RoomInfo` packet
3. Client sends `Connect` packet
4. Server sends `Connected` or `ConnectionRefused`

## Key packets (client → server)
```json
// Connect
[{"cmd": "Connect", "game": "", "name": "SlotName", "password": "pass",
  "version": {"major":0,"minor":5,"build":0,"class":"Version"},
  "items_handling": 7, "tags": ["NoText"], "uuid": "...", "slot_data": false}]

// LocationChecks — mark locations as checked
[{"cmd": "LocationChecks", "locations": [42, 43, 44]}]

// LocationScouts — query items at locations without checking
[{"cmd": "LocationScouts", "locations": [42], "create_as_hint": 0}]
```

## Key packets (server → client)
```json
// Connected
[{"cmd": "Connected", "team": 0, "slot": 1, "players": [...],
  "missing_locations": [...], "checked_locations": [...],
  "slot_info": {"1": {"name": "PlayerName", "game": "GameName", "type": 1}},
  "slot_data": {}}]

// ReceivedItems
[{"cmd": "ReceivedItems", "index": 0,
  "items": [{"item": 1234, "location": 5678, "player": 2, "flags": 1}]}]

// PrintJSON — game events as structured messages
[{"cmd": "PrintJSON", "data": [...], "type": "ItemSend",
  "receiving": 1, "item": {"item": 1234, "location": 5678, "player": 2, "flags": 1}}]
```

## Item flags (bitmask)
- `0b001 = 1` — Progression (blockers/required items)
- `0b010 = 2` — Useful (helpful but not required)
- `0b100 = 4` — Trap
- `0` — Filler (junk)

## items_handling values
- `0` — No items
- `1` — Own items sent by others
- `3` — Own + starting inventory
- `7` — All items (recommended)

## PrintJSON types for filtering
- `ItemSend` — someone found an item for another player
- `ItemCheat` — item granted via command
- `Hint` — hint event
- `Join`/`Part`/`Chat` — connection events (suppressed with `NoText` tag)

## ArchipelagoWebSocketClient location
`core/network/ArchipelagoWebSocketClient.kt`
Uses `ktor-client-websockets`. Exposes `Flow<ArchipelagoEvent>` for the UI layer.
