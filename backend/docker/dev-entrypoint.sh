#!/bin/sh
set -eu

cd /app

source_fingerprint() {
  # Include mtime and size so even a same-content save triggers Maven compilation.
  find src/main/java src/main/resources -type f -printf '%T@ %s %p\n' 2>/dev/null \
    | sort \
    | sha256sum \
    | cut -d ' ' -f 1
}

echo "[dev] Initial Maven compile..."
mvn -q -DskipTests compile

echo "[dev] Starting Spring Boot with DevTools..."
mvn -q spring-boot:run &
APP_PID=$!

shutdown() {
  echo "[dev] Stopping Spring Boot..."
  kill "$APP_PID" 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
}
trap shutdown INT TERM EXIT

LAST_FINGERPRINT=$(source_fingerprint)

while kill -0 "$APP_PID" 2>/dev/null; do
  sleep 1
  CURRENT_FINGERPRINT=$(source_fingerprint)
  if [ "$CURRENT_FINGERPRINT" != "$LAST_FINGERPRINT" ]; then
    echo "[dev] Source change detected; compiling..."
    if mvn -q -DskipTests compile; then
      LAST_FINGERPRINT=$CURRENT_FINGERPRINT
      echo "[dev] Compile complete; DevTools will restart the application."
    else
      echo "[dev] Compile failed; fix the errors to retry."
    fi
  fi
done

wait "$APP_PID"
