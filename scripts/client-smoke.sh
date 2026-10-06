#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Client smoke test: plays a capsule scenario in a real dev client and takes screenshots
# =============================================================================
#
# Usage:
#   scripts/client-smoke.sh neoforge|fabric
#
# Runs the dev client of the loader (Gradle task runClientSmoke, -Dcapsule.clientsmoke=true)
# under xvfb-run when no display is available. The dev only harness (common/src/gametest,
# package capsule.clientsmoke) creates a flat creative world, captures and deploys a
# structure, shows inventories, previews and JEI, checks every screenshot and the log,
# writes a report and quits.
#
# Output: build/client-smoke/<loader>/ with the screenshots, report.txt and the client log.
# Exit code 0 when every check passed (on NeoForge, JEI must be loaded and show the
# capsule recipes). TIMEOUT (seconds, default 1500) bounds the whole run, build included.
# EXTRA_MODS (space separated jar files) adds mods to the run, e.g. the jars of a modpack:
# NeoForge 1.21.1 dev runs load production mod jars from the mods folder.
# SHADER_PACK (a shader pack zip) turns it on with Iris, which EXTRA_MODS must bring with Sodium.
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOADER="${1:-}"
TIMEOUT="${TIMEOUT:-1500}"

case "$LOADER" in
    neoforge|fabric) ;;
    *) echo "Usage: scripts/client-smoke.sh neoforge|fabric"; exit 1 ;;
esac

RUN_DIR="$ROOT/$LOADER/runs/clientSmoke"
OUT="$ROOT/build/client-smoke/$LOADER"
rm -rf "$RUN_DIR/screenshots/capsule-smoke" "$RUN_DIR/mods" "$RUN_DIR/shaderpacks" "$RUN_DIR/config/iris.properties" "$OUT"
mkdir -p "$OUT" "$RUN_DIR/mods"
for jar in ${EXTRA_MODS:-}; do
    cp "$jar" "$RUN_DIR/mods/"
done
if [ -n "${SHADER_PACK:-}" ]; then
    mkdir -p "$RUN_DIR/shaderpacks" "$RUN_DIR/config"
    cp "$SHADER_PACK" "$RUN_DIR/shaderpacks/"
    printf 'enableShaders=true\nshaderPack=%s\n' "$(basename "$SHADER_PACK")" > "$RUN_DIR/config/iris.properties"
fi

GRADLE=("$ROOT/gradlew" -p "$ROOT" ":$LOADER:runClientSmoke")
if [ -z "${DISPLAY:-}" ]; then
    GRADLE=(xvfb-run -a -s "-screen 0 1280x720x24" "${GRADLE[@]}")
fi

echo "== $LOADER client smoke test (log: $OUT/console.log)"
status=0
timeout --kill-after=60 "$TIMEOUT" "${GRADLE[@]}" > "$OUT/console.log" 2>&1 || status=$?

cp "$RUN_DIR"/screenshots/capsule-smoke/* "$OUT/" 2>/dev/null || true
cp "$RUN_DIR/logs/latest.log" "$OUT/client.log" 2>/dev/null || true

failures=()
[ "$status" -eq 0 ] || failures+=("the client exited with status $status")
if [ -f "$OUT/report.txt" ]; then
    cat "$OUT/report.txt"
    grep -q '^RESULT PASS' "$OUT/report.txt" || failures+=("a check failed")
    if [ "$LOADER" = neoforge ]; then
        grep -q '^PASS JEI' "$OUT/report.txt" || failures+=("JEI did not show the capsule recipes")
    fi
else
    failures+=("no report: the scenario did not finish")
    tail -n 40 "$OUT/console.log"
fi

if [ "${#failures[@]}" -gt 0 ]; then
    printf 'FAIL: %s\n' "${failures[@]}"
    exit 1
fi
echo "OK: $LOADER client smoke test passed, screenshots in $OUT"
