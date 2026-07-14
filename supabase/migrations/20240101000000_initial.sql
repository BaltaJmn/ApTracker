-- ApTracker initial schema

-- Rooms: Archipelago server connection details
create table if not exists rooms (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    host text not null,
    port integer not null default 38281,
    password text,
    created_at timestamptz not null default now()
);

alter table rooms enable row level security;

create policy "rooms: owner full access"
    on rooms
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- Slots: tracked player slots within a room
create table if not exists slots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    room_id uuid not null references rooms(id) on delete cascade,
    slot_name text not null,
    game_name text,
    notify_enabled boolean not null default true,
    notify_progression boolean not null default true,
    notify_useful boolean not null default true,
    notify_filler boolean not null default false,
    suppress_local boolean not null default false,
    suppress_others boolean not null default false,
    created_at timestamptz not null default now()
);

alter table slots enable row level security;

create policy "slots: owner full access"
    on slots
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- Activity: item send/receive events per slot
create table if not exists activity (
    id uuid primary key default gen_random_uuid(),
    room_id uuid not null references rooms(id) on delete cascade,
    slot_name text,
    event_type text not null, -- 'received' | 'sent' | 'hint'
    item_name text,
    location_name text,
    sender_name text,
    receiver_name text,
    item_flags integer not null default 0, -- 1=progression, 2=useful, 4=trap
    timestamp timestamptz not null default now()
);

alter table activity enable row level security;

create policy "activity: owner full access"
    on activity
    for all
    using (
        room_id in (
            select id from rooms where user_id = auth.uid()
        )
    );

-- Push tokens: FCM/APNs tokens for push notifications
create table if not exists push_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    token text not null unique,
    platform text not null, -- 'android' | 'ios'
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table push_tokens enable row level security;

create policy "push_tokens: owner full access"
    on push_tokens
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());
