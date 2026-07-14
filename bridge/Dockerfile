# Small, multi-arch (works on Raspberry Pi arm64/armv7) Python image.
FROM python:3.12-slim

# Don't buffer stdout/stderr so logs show up immediately in `docker logs`.
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

WORKDIR /app

# Install deps first for better layer caching.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# App code.
COPY aptracker_bridge ./aptracker_bridge

# Run as an unprivileged user.
RUN useradd --create-home --uid 10001 appuser
USER appuser

CMD ["python", "-m", "aptracker_bridge"]
