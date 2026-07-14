-- ntfy push support + activity read performance

-- Per-room ntfy topic. When null, the bridge falls back to NTFY_DEFAULT_TOPIC.
-- The app shows this topic so the user can subscribe from the ntfy mobile app.
alter table rooms
    add column if not exists ntfy_topic text;

-- Activity is read filtered by room and shown newest-first.
create index if not exists activity_room_id_timestamp_idx
    on activity (room_id, timestamp desc);

-- The mobile app subscribes to activity inserts over Realtime; make sure the
-- table is in the realtime publication (no-op if already added).
do $$
begin
    alter publication supabase_realtime add table activity;
exception
    when duplicate_object then null;
    when undefined_object then null;
end $$;
