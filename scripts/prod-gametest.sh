#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# GameTests on the release jars: runs every capsule GameTest on a real dedicated server
# =============================================================================
#
# Usage:
#   scripts/prod-gametest.sh <release-jar>...
#
# Example:
#   ./gradlew build
#   scripts/prod-gametest.sh neoforge/build/libs/Capsule-neoforge-*.jar fabric/build/libs/Capsule-fabric-*.jar
#
# For each release jar (loader read from the file name), in a temporary directory: the
# server of scripts/prod-smoke.sh with the release jar and its test mod jar, which
# ./gradlew build writes to <loader>/build/test-mod/<release jar name>-gametest.jar, then
# runs the GameTest server instead of the game server:
#   - NeoForge: -Dneoforge.gameTestServer=true (the test mod registers the tests, which
#     NeoForge only does outside production), with SecurityCraft for its tests
#   - Fabric: -Dfabric-api.gametest, with the GameTest module of Fabric API (not in its
#     release jar); the test mod is remapped like the release jar
#
# Checks: the server reports "All <n> required tests passed". Exit code 0 when every jar
# passes. FAIL_ON_PURPOSE=1 adds a test that always fails (-Dcapsule.gametest.failOnPurpose=true),
# to see a failure reported. TIMEOUT (seconds, default 1200) bounds each server,
# EXTRA_MODS adds jars to the mods folder, KEEP_SERVER=1 keeps the server directories.
# =============================================================================

source "$(dirname "$0")/lib.sh"
TIMEOUT="${TIMEOUT:-1200}"

gametest() {
    local jar loader test_jar dir log
    jar="$(realpath "$1")"
    loader="$(loader_of "$jar")" || return 1
    test_jar="$(dirname "$jar")/../test-mod/$(basename "$jar" .jar)-gametest.jar"
    if [ ! -f "$test_jar" ]; then
        echo "FAIL: no test mod jar $test_jar (./gradlew build writes it)"
        return 1
    fi
    dir="$(mktemp -d "${TMPDIR:-/tmp}/gametest-$loader.XXXXXX")"
    log="$dir/console.log"
    echo "== $loader: $(basename "$jar") + $(basename "$test_jar") in $dir"

    local jvm_args=()
    [ -z "${FAIL_ON_PURPOSE:-}" ] || jvm_args+=(-Dcapsule.gametest.failOnPurpose=true)
    mkdir -p "$dir/mods"
    if [ "$loader" = neoforge ]; then
        setup_neoforge "$dir" "${jvm_args[@]}" -Dneoforge.gameTestServer=true
        modrinth_jar security-craft "$(prop securitycraft_version)" "$dir/mods"
    else
        setup_fabric "$dir" "${jvm_args[@]}" -Dfabric-api.gametest "-Dfabric-api.gametest.report-file=$dir/report.xml"
        local maven=https://maven.fabricmc.net/net/fabricmc/fabric-api module
        module="$(curl -fsSL "$maven/fabric-api/$FABRIC_API_VERSION/fabric-api-$FABRIC_API_VERSION.pom" \
            | grep -A1 '<artifactId>fabric-gametest-api-v1<' | sed -n 's|.*<version>\(.*\)</version>.*|\1|p')"
        download "$maven/fabric-gametest-api-v1/$module/fabric-gametest-api-v1-$module.jar" "$dir/mods/fabric-gametest-api-v1-$module.jar"
    fi
    cp "$jar" "$test_jar" "$dir/mods/"
    for extra in ${EXTRA_MODS:-}; do
        cp "$extra" "$dir/mods/"
    done
    echo "eula=true" > "$dir/eula.txt"
    printf 'server-port=%s\nonline-mode=false\n' "$(free_port)" > "$dir/server.properties"

    local status=0
    (cd "$dir" && timeout --kill-after=30 "$TIMEOUT" "${START[@]}" < /dev/null > console.log 2>&1) || status=$?

    grep -E 'required tests (passed|failed)|\]:    - ' "$log" || true
    if ! grep -qE 'All [0-9]+ required tests passed' "$log"; then
        grep -E 'failed at|failed!' "$log" | head -n 20 || true
        echo "FAIL: $loader GameTests on the release jar (exit status $status), log: $log"
        return 1
    fi
    echo "OK: $loader, $(grep -oE 'All [0-9]+ required tests passed' "$log" | tail -n 1) on $(basename "$jar")"
    [ -n "${KEEP_SERVER:-}" ] || rm -rf "$dir"
}

if [ "$#" -eq 0 ]; then
    echo "Usage: scripts/prod-gametest.sh <release-jar>..."
    exit 1
fi

result=0
for jar in "$@"; do
    gametest "$jar" || result=1
done
exit "$result"
