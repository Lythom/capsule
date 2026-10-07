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
#   ./gradlew build incompatJar
#   JAVA_HOME=/path/to/jdk17 INCOMPAT=1 scripts/prod-smoke-forge.sh build/libs/Capsule-1.20.1-8.0.BUILD_ID.jar
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
# Set KEEP_SERVER=1 to keep the server directories (printed at the end). EXTRA_MODS adds jars
# to the mods folder. Downloads are cached in CAPSULE_CACHE (default ~/.cache/capsule-validation).
#
# INCOMPAT=1 also plays the known incompatibility scenarios: the server runs Forge
# incompat_forge_version with the mods of incompat_mods (gradle.properties, their newest
# Modrinth versions with LATEST=1) and the scenario mod build/incompat/<jar name>-incompat.jar
# (./gradlew incompatJar), with -Dcapsule.incompat=true. Each check logs a PASS or FAIL line;
# the jar fails when a check fails or the scenarios do not finish.
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${TIMEOUT:-600}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
CACHE="${CAPSULE_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/capsule-validation}"
USER_AGENT='Lythom/capsule (contact@samuel-bouchet.fr)'

prop() {
    grep "^$1=" "$ROOT/gradle.properties" | cut -d= -f2-
}

if [ -n "${INCOMPAT:-}" ]; then
    FORGE_VERSION="$(prop incompat_forge_version)"
else
    FORGE_VERSION="$(sed -n "s/.*minecraft 'net.minecraftforge:forge:\([^']*\)'.*/\1/p" "$ROOT/build.gradle")"
fi
MC_VERSION="${FORGE_VERSION%%-*}"

# download <url> <file>: copies the cached file, downloading it on first use
download() {
    local cached="$CACHE/downloads/$(printf '%s' "$1" | sed 's|^https\?://||; s|[^A-Za-z0-9._-]|_|g')"
    if [ ! -f "$cached" ]; then
        echo "  download $1"
        mkdir -p "$CACHE/downloads"
        curl -fsSL --retry 3 -A "$USER_AGENT" -o "$cached.part" "$1"
        mv "$cached.part" "$cached"
    fi
    cp "$cached" "$2"
}

# incompat_mods <dir>: the mods of incompat_mods, pinned or with LATEST=1 the newest for Forge and MC_VERSION
incompat_mods() {
    local mod slug version
    for mod in $(prop incompat_mods | tr ',' ' '); do
        slug="${mod%%:*}" version="${mod#*:}"
        if [ -n "${LATEST:-}" ]; then
            version="$(curl -fsSL -A "$USER_AGENT" "https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22forge%22%5D&game_versions=%5B%22$MC_VERSION%22%5D" \
                | python3 -c 'import json,sys; v=json.load(sys.stdin)[0]; print(v["id"]); print(sys.argv[1] + ": " + v["version_number"] + " (" + v["id"] + "), pinned " + sys.argv[2], file=sys.stderr)' "$slug" "$version")"
        fi
        download "https://api.modrinth.com/maven/maven/modrinth/$slug/$version/$slug-$version.jar" "$1/$slug-$version.jar"
    done
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
    for extra in ${EXTRA_MODS:-}; do
        cp "$extra" "$dir/mods/"
    done
    if [ -n "${INCOMPAT:-}" ]; then
        local scenarios
        scenarios="$(dirname "$jar")/../incompat/$(basename "$jar" .jar)-incompat.jar"
        if [ ! -f "$scenarios" ]; then
            echo "FAIL: no scenario mod jar $scenarios (./gradlew incompatJar writes it)"
            return 1
        fi
        cp "$scenarios" "$dir/mods/"
        incompat_mods "$dir/mods"
        # the last -Xmx wins: GregTech needs more memory
        START=("${START[@]:0:2}" -Xmx4G -Dcapsule.incompat=true "${START[@]:2}")
    fi
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

    local failures=()
    if [ -n "${INCOMPAT:-}" ]; then
        until grep -q 'capsule incompat: [0-9]* checks' "$log"; do
            if ! kill -0 "$pid" 2>/dev/null || [ "$waited" -ge "$TIMEOUT" ]; then
                failures+=("the incompatibility scenarios did not finish")
                break
            fi
            sleep 2
            waited=$((waited + 2))
        done
        grep -oE '(PASS|FAIL|SKIP) \| .*|capsule incompat: .*' "$log" || true
        grep -q 'capsule incompat: [0-9]* checks, 0 failed' "$log" || failures+=("an incompatibility check failed")
    fi

    echo "help capsule" >&3
    sleep 3
    echo "stop" >&3
    exec 3>&-
    local status=0
    wait "$pid" || status=$?

    grep -q '/capsule giveEmpty' "$log" || failures+=("the capsule command is not registered")
    [ -f "$dir/config/capsule-common.toml" ] || failures+=("config/capsule-common.toml was not created")
    [ -d "$dir/config/capsule/loot" ] || failures+=("config/capsule/loot was not populated")
    local errors
    errors="$(grep -hiE 'ERROR|Exception|Caused by' "$log" "$dir/logs/latest.log" | grep -i 'capsule' | grep -vE '(PASS|FAIL) \| ' || true)"
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
