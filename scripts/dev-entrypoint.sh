#!/usr/bin/env bash
# Dev hot-reload: keep spring-boot:run alive; recompile on src changes so DevTools restarts.
set -euo pipefail
cd /app

echo ">>> Resolving Maven dependencies..."
mvn -B -DskipTests dependency:resolve

echo ">>> Starting Spring Boot (DevTools + remote debug on :5005)..."
mvn -B -DskipTests spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" &
BOOT_PID=$!

checksum() {
  find src -type f \( \
      -name '*.java' -o -name '*.xml' -o \
      -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \
    \) -print0 2>/dev/null \
    | sort -z \
    | xargs -0 cat 2>/dev/null \
    | md5sum \
    | awk '{print $1}'
}

LAST="$(checksum || true)"
echo ">>> Watching src/ for changes (poll every 2s)..."

while kill -0 "$BOOT_PID" 2>/dev/null; do
  sleep 2
  CUR="$(checksum || true)"
  if [[ -n "$CUR" && "$CUR" != "$LAST" ]]; then
    echo ""
    echo ">>> Source changed, recompiling..."
    if mvn -B -q -DskipTests compile; then
      echo ">>> Compile OK — DevTools will restart the application"
    else
      echo ">>> Compile failed — fix errors and save again"
    fi
    LAST="$CUR"
  fi
done

wait "$BOOT_PID"
