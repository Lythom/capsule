#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Production jar smoke test: boots a real dedicated server of the jar's loader
# =============================================================================
#
# Usage:
#   scripts/prod-smoke.sh <jar-file>...
#
# Examples:
#   scripts/prod-smoke.sh neoforge/build/libs/Capsule-neoforge-1.21.1-9.0.SNAPSHOT.jar
#   scripts/prod-smoke.sh neoforge/build/libs/Capsule-*.jar fabric/build/libs/Capsule-*.jar
#
# For each jar (loader read from the file name), in a temporary directory:
#   - NeoForge: the official installer (--installServer) of neo_version
#   - Fabric: the Fabric server launcher for fabric_loader_version, with Fabric API and
#     Forge Config API Port, the jar's required dependencies
# then accepts the EULA, starts the server with nogui, waits for "Done", runs
# "help capsule", checks the log and stops the server.
#
# Checks: the server reaches "Done", the capsule command is registered, the capsule config
# files are created, and the log has no error mentioning capsule or a mixin.
# Versions come from gradle.properties. Needs java 21, curl and python3.
# Set KEEP_SERVER=1 to keep the server directories (printed at the end).
# EXTRA_MODS (space separated jar files) adds other mods to the server, to check that they
# boot together.
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${TIMEOUT:-600}"

prop() {
    grep "^$1=" "$ROOT/gradle.properties" | cut -d= -f2-
}

MC_VERSION="$(prop minecraft_version)"
NEO_VERSION="$(prop neo_version)"
FABRIC_LOADER_VERSION="$(prop fabric_loader_version)"
FABRIC_API_VERSION="$(prop fabric_api_version)"
FCAP_VERSION="$(prop forge_config_api_port_version)"

download() {
    echo "  download $1"
    curl -fsSL --retry 3 -o "$2" "$1"
}

setup_neoforge() {
    local dir="$1"
    download "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEO_VERSION/neoforge-$NEO_VERSION-installer.jar" "$dir/installer.jar"
    (cd "$dir" && java -jar installer.jar --installServer . > installer.log 2>&1)
    rm "$dir/installer.jar"
    echo "-Xmx2G" >> "$dir/user_jvm_args.txt"
    START=(./run.sh nogui)
}

setup_fabric() {
    local dir="$1"
    local installer
    installer="$(curl -fsSL https://meta.fabricmc.net/v2/versions/installer | python3 -c 'import json,sys; print(next(v["version"] for v in json.load(sys.stdin) if v["stable"]))')"
    download "https://meta.fabricmc.net/v2/versions/loader/$MC_VERSION/$FABRIC_LOADER_VERSION/$installer/server/jar" "$dir/fabric-server-launch.jar"
    download "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$FABRIC_API_VERSION/fabric-api-$FABRIC_API_VERSION.jar" "$dir/mods/fabric-api-$FABRIC_API_VERSION.jar"
    download "https://api.modrinth.com/maven/maven/modrinth/forge-config-api-port/$FCAP_VERSION/forge-config-api-port-$FCAP_VERSION.jar" "$dir/mods/forge-config-api-port-$FCAP_VERSION.jar"
    START=(java -Xmx2G -jar fabric-server-launch.jar nogui)
}

free_port() {
    python3 -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'
}

smoke() {
    local jar loader dir log
    jar="$(realpath "$1")"
    case "$(basename "$jar")" in
        *neoforge*) loader=neoforge ;;
        *fabric*) loader=fabric ;;
        *) echo "ERROR: cannot tell the loader of $jar"; return 1 ;;
    esac
    dir="$(mktemp -d "${TMPDIR:-/tmp}/smoke-$loader.XXXXXX")"
    log="$dir/console.log"
    echo "== $loader: $(basename "$jar") in $dir"

    mkdir -p "$dir/mods"
    "setup_$loader" "$dir"
    cp "$jar" "$dir/mods/"
    for extra in ${EXTRA_MODS:-}; do
        cp "$extra" "$dir/mods/"
    done
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
    errors="$(grep -iE 'ERROR|Exception|Caused by' "$log" | grep -iE 'capsule|mixin' || true)"
    [ -z "$errors" ] || failures+=("errors in the log:"$'\n'"$errors")
    ! grep -q '\[capsule\] Starting version check' "$log" || failures+=("capsule asks an update checker")
    [ "$status" -eq 0 ] || failures+=("the server exited with status $status")
    grep -q 'Stopping server' "$log" || failures+=("the server did not stop cleanly")

    if [ "$loader" = neoforge ]; then
        grep -m1 -E '^\s+Capsule .*\(capsule\)' "$dir/logs/latest.log" || true
    else
        grep -E '^\s+(- |\\-- )(capsule|common-protection-api|fabric-api|forgeconfigapiport) ' "$log" || true
    fi
    grep '/capsule giveEmpty' "$log" | head -n 1

    if [ "${#failures[@]}" -gt 0 ]; then
        printf 'FAIL: %s\n' "${failures[@]}"
        echo "   log: $log"
        return 1
    fi
    echo "OK: $loader server booted with $(basename "$jar") and stopped cleanly"
    [ -n "${KEEP_SERVER:-}" ] || rm -rf "$dir"
}

if [ "$#" -eq 0 ]; then
    echo "Usage: scripts/prod-smoke.sh <jar-file>..."
    exit 1
fi

result=0
for jar in "$@"; do
    smoke "$jar" || result=1
done
exit "$result"
