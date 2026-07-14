# Convenience wrapper around docker compose for the Pi.
#   make up      -> build + start the bridge (uses ntfy.sh by default)
#   make up-ntfy -> also start a self-hosted ntfy (LAN only)
#   make logs    -> follow the bridge logs
#   make down    -> stop everything
#   make update  -> pull latest code and restart

.PHONY: up up-ntfy down logs update ps

up:
	docker compose up -d --build

up-ntfy:
	docker compose --profile ntfy up -d --build

down:
	docker compose down

logs:
	docker compose logs -f aptracker-bridge

ps:
	docker compose ps

update:
	git pull && docker compose up -d --build
