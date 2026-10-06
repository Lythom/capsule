#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Production jar smoke test: boots a real Forge dedicated server with the jar
# =============================================================================
#
# Usage:
#   scripts/prod-smoke-forge.sh <jar-file>...
#
# Example:
#   JAVA_HOME=/path/to/jdk17 scripts/prod-smoke-forge.sh build/libs/Capsule-1.20.1-8.0.BUILD_ID.jar
#
# For each jar, in a temporary directory: installs the Forge server of the version used
# by build.gradle with the official installer (--installServer), adds the jar, accepts
# the EULA, starts the server with nogui, waits for "Done", runs "help capsule", checks
# the log and stops the server.
#
# Checks: the server reaches "Done", the capsule command is registered, the capsule config
# files are created, the log has no error mentioning capsule and capsule asks no update checker.
# Uses $JAVA_HOME/bin/java when JAVA_HOME is set (Java 8 for 1.16.5, 17 for 1.18.2 and
# 1.20.1), java otherwise. Needs curl and python3.
# Set KEEP_SERVER=1 to keep the server directories (printed at the end).
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${TIMEOUT:-600}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

FORGE_VERSION="$(sed -n "s/.*minecraft 'net.minecraftforge:forge:\([^']*\)'.*/\1/p" "$ROOT/build.gradle")"

download() {
    echo "  download $1"
    curl -fsSL --retry 3 -o "$2" "$1"
}

setup_forge() {
    local dir="$1"
    download "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE_VERSION/forge-$FORGE_VERSION-installer.jar" "$dir/installer.jar"
    (cd "$dir" && "$JAVA" -jar installer.jar --installServer > installer.log 2>&1)
    rm "$dir/installer.jar"
    if [ -f "$dir/libraries/net/minecraftforge/forge/$FORGE_VERSION/unix_args.txt" ]; then
        # 1.17 and later: the installer writes the arguments of the server
        START=("$JAVA" -Xmx2G -Dforge.logging.mojang.level=debug "@libraries/net/minecraftforge/forge/$FORGE_VERSION/unix_args.txt" nogui)
    else
        START=("$JAVA" -Xmx2G -Dforge.logging.mojang.level=debug -jar "forge-$FORGE_VERSION.jar" nogui)
    fi
}

free_port() {
    python3 -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'
}

smoke() {
    local jar dir log
    jar="$(realpath "$1")"
    dir="$(mktemp -d "${TMPDIR:-/tmp}/smoke-forge.XXXXXX")"
    log="$dir/console.log"
    echo "== forge $FORGE_VERSION: $(basename "$jar") in $dir"

    mkdir -p "$dir/mods"
    setup_forge "$dir"
    cp "$jar" "$dir/mods/"
    echo "eula=true" > "$dir/eula.txt"
    printf 'server-port=%s\nonline-mode=false\nspawn-protection=0\n' "$(free_port)" > "$dir/server.properties"

    mkfifo "$dir/console"
    (cd "$dir" && exec "${START[@]}" < console > console.log 2>&1) &
    local pid=$!
    exec 3> "$dir/console"

    local waited=0
    until grep -q 'Done (' "$log" 2>/dev/null; do
        if ! kill -0 "$pid" 2>/dev/null || [ "$waited" -ge "$TIMEOUT" ]; then
            echo "FAIL: the server did not reach Done"
            tail -n 40 "$log"
            exec 3>&-
            kill "$pid" 2>/dev/null || true
            return 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    grep 'Done (' "$log"

    echo "help capsule" >&3
    sleep 3
    echo "stop" >&3
    exec 3>&-
    local status=0
    wait "$pid" || status=$?

    local failures=()
    grep -q '/capsule giveEmpty' "$log" || failures+=("the capsule command is not registered")
    [ -f "$dir/config/capsule-common.toml" ] || failures+=("config/capsule-common.toml was not created")
    [ -d "$dir/config/capsule/loot" ] || failures+=("config/capsule/loot was not populated")
    local errors
    errors="$(grep -hiE 'ERROR|Exception|Caused by' "$log" "$dir/logs/latest.log" | grep -i 'capsule' || true)"
    [ -z "$errors" ] || failures+=("errors in the log:"$'\n'"$errors")
    ! grep -q '\[capsule\] Starting version check' "$log" "$dir/logs/latest.log" || failures+=("capsule asks an update checker")
    [ "$status" -eq 0 ] || failures+=("the server exited with status $status")
    grep -q 'Stopping server' "$log" || failures+=("the server did not stop cleanly")

    grep -h -m1 -E 'Found valid mod file Capsule|Found mod file Capsule' "$dir/logs/debug.log" "$dir/logs/latest.log" 2>/dev/null | head -n 1 || true
    grep '/capsule giveEmpty' "$log" | head -n 1
    # recipes with unmet conditions (addon capsules without their ingot), logged with Minecraft's debug level
    echo "  skipped capsule recipes: $(grep -ho 'Skipping loading recipe capsule:[a-z_]*' "$dir/logs/debug.log" | sed 's/.*capsule://' | sort | tr '\n' ' ')"

    if [ "${#failures[@]}" -gt 0 ]; then
        printf 'FAIL: %s\n' "${failures[@]}"
        echo "   log: $log"
        return 1
    fi
    echo "OK: forge $FORGE_VERSION server booted with $(basename "$jar") and stopped cleanly"
    [ -n "${KEEP_SERVER:-}" ] || rm -rf "$dir"
}

if [ "$#" -eq 0 ]; then
    echo "Usage: scripts/prod-smoke-forge.sh <jar-file>..."
    exit 1
fi

result=0
for jar in "$@"; do
    smoke "$jar" || result=1
done
exit "$result"
