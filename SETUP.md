# Puesta en marcha de ApTracker

Guía práctica para dejarlo funcionando: un juego de Archipelago compartido, cada
jugador en su casa, con la info y las notificaciones en el móvil.

## Cómo funciona (en 30 segundos)

```
Servidor Archipelago ──► Raspberry (bridge en Docker) ──► Supabase (nube) ──► App móvil (ves los checks)
                                     └────────────────────► ntfy.sh ─────────► Notificación en el móvil
```

- La **Raspberry** está siempre encendida y vigila la partida por ti (aunque tengáis
  el PC apagado). Solo hace conexiones **salientes**: no hay que abrir puertos.
- **Supabase** (nube) guarda la actividad; la **app** la lee desde cualquier sitio.
- **ntfy** manda el aviso al móvil. Usamos **ntfy.sh** (público) porque cada uno está
  en un pueblo distinto.

Solo una persona (el que tiene la Raspberry) hace la parte de servidor. Cada jugador
solo instala la app y se suscribe a su aviso.

---

## Parte A — Montaje único (lo hace quien tiene la Raspberry)

### 1. Supabase (una vez)
1. En tu proyecto de Supabase, abre **SQL Editor** y ejecuta, en orden, estos dos
   ficheros:
   - `supabase/migrations/20240101000000_initial.sql`
   - `supabase/migrations/20240102000000_ntfy_and_indexes.sql`
2. En **Settings → API** copia:
   - **Project URL** y **anon key** → van en la app.
   - **service_role key** → va en la Raspberry (es secreta, no la metas en la app).

### 2. La app (compilar y repartir)
1. Comprueba que `composeApp/.../core/data/SupabaseConfig.kt` tiene tu **URL** y tu
   **anon key** (este fichero está en `.gitignore`, no se sube).
2. Genera el APK:
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```
   El APK queda en `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
3. Pásaselo a tus colegas (Android). *(iOS: hay que compilar desde Xcode, es más lío;
   si alguien va con iPhone, me dices y lo vemos.)*

### 3. La Raspberry (el bridge, en Docker)
```bash
git clone <este-repo> ~/ApTracker
cd ~/ApTracker/bridge
cp .env.example .env
nano .env
```
Rellena en `.env`:
- `SUPABASE_URL` = tu Project URL
- `SUPABASE_SERVICE_ROLE_KEY` = la service_role key
- `NTFY_DEFAULT_TOPIC` = una palabra aleatoria difícil de adivinar (respaldo; cada sala
  genera además su propio topic)
- `NTFY_BASE_URL` = déjalo en `https://ntfy.sh`

Arranca:
```bash
make up        # = docker compose up -d --build
make logs      # para ver que conecta ("watching room…", "tracking as spectator")
```
Se reinicia solo al encender la Pi. Para actualizar en el futuro: `make update`.

---

## Parte B — Lo que hace cada jugador (incluido tú)

1. Instala el **APK** y abre ApTracker. **Regístrate** (email + contraseña).
2. **Add Room**: nombre libre, `host` y `port` del **servidor de la partida**
   (p. ej. `archipelago.gg` y el puerto que os dé, o la IP/puerto si lo aloja alguien).
   Contraseña de sala si la hay. Se genera un **topic de notificación** automático.
3. Entra en la sala y **añade tu Slot**: el **nombre exacto** de tu jugador en la
   partida (respeta mayúsculas).
4. Instala la app **ntfy** (Android/iOS) y **suscríbete** al topic que ves en la
   pantalla de tu sala (botón *Copy*). Servidor: `https://ntfy.sh` (por defecto, no hay
   que cambiar nada).

Listo. Cuando alguien te desbloquee un ítem, te llega la notificación y lo ves en la
app aunque no estuvieras mirando.

> Importante: cada jugador crea **su propia sala** apuntando al **mismo servidor** y
> añade **solo su slot**. Así cada uno recibe sus avisos por separado. La Raspberry se
> encarga de vigilarlas todas.

---

## Ajustes y problemas típicos

- **Qué te avisa**: por defecto, ítems de *progresión* y *útiles* que te desbloquean.
  Puedes afinar por slot en la pestaña **Notifications** (progression / useful / filler).
- **No llegan avisos**: revisa que el nombre del slot es idéntico al de la partida, que
  la Raspberry está encendida (`make logs`) y que te suscribiste al topic correcto en
  ntfy.
- **Veo "all tracked slot names were refused"** en los logs: el nombre del slot no
  existe en esa partida; corrígelo en la app (se reintenta solo).
- **Nombres como "Item 1234"**: el servidor aún no envió el catálogo de nombres; suele
  resolverse solo al reconectar.

Para el detalle técnico del bridge y las opciones de notificación (ntfy vs Firebase),
mira [`bridge/README.md`](bridge/README.md).
