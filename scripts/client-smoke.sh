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
# under xvfb-run when no display is available. The harness (common/src/gametest, package
# capsule.clientsmoke, never in the release jar) creates a flat creative world, captures
# and deploys a structure, shows inventories, previews and a recipe viewer, checks every
# screenshot and the log, writes a report and quits.
#
# Output: build/client-smoke/<loader>/ (or OUT) with the screenshots, report.txt and the
# client log. Exit code 0 when every check passed, including the recipe viewer's.
# RECIPE_VIEWER (jei, rei or emi, default jei) picks the recipe viewer of the run.
# TIMEOUT (seconds, default 1500) bounds the whole run, build included. GRADLE_ARGS adds
# arguments to the Gradle call (e.g. "-I mirror.gradle" when Maven Central rate limits).
# EXTRA_MODS (space separated jar files) adds mods to the run, e.g. the jars of a modpack:
# NeoForge 1.21.1 dev runs load production mod jars from the mods folder.
# SHADER_PACK (a shader pack zip) turns it on with Iris, which EXTRA_MODS must bring with Sodium.
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOADER="${1:-}"
TIMEOUT="${TIMEOUT:-1500}"
RECIPE_VIEWER="${RECIPE_VIEWER:-jei}"

case "$LOADER" in
    neoforge|fabric) ;;
    *) echo "Usage: scripts/client-smoke.sh neoforge|fabric"; exit 1 ;;
esac
case "$RECIPE_VIEWER" in
    jei|rei|emi) VIEWER_NAME="${RECIPE_VIEWER^^}" ;;
    *) echo "RECIPE_VIEWER must be jei, rei or emi"; exit 1 ;;
esac

OUT="${OUT:-$ROOT/build/client-smoke/$LOADER}"
rm -rf "$OUT"
mkdir -p "$OUT"
RUN_DIR="$ROOT/$LOADER/runs/clientSmoke"
rm -rf "$RUN_DIR/screenshots/capsule-smoke" "$RUN_DIR/mods" "$RUN_DIR/shaderpacks" "$RUN_DIR/config/iris.properties"
RUN=("$ROOT/gradlew" -p "$ROOT" ${GRADLE_ARGS:-} ":$LOADER:runClientSmoke" "-PrecipeViewer=$RECIPE_VIEWER")
echo "== $LOADER client smoke test with $VIEWER_NAME (log: $OUT/console.log)"
mkdir -p "$RUN_DIR/mods"
for jar in ${EXTRA_MODS:-}; do
    cp "$jar" "$RUN_DIR/mods/"
done
if [ -n "${SHADER_PACK:-}" ]; then
    mkdir -p "$RUN_DIR/shaderpacks" "$RUN_DIR/config"
    cp "$SHADER_PACK" "$RUN_DIR/shaderpacks/"
    printf 'enableShaders=true\nshaderPack=%s\n' "$(basename "$SHADER_PACK")" > "$RUN_DIR/config/iris.properties"
fi

if [ -z "${DISPLAY:-}" ]; then
    RUN=(xvfb-run -a -s "-screen 0 1280x720x24" "${RUN[@]}")
fi
status=0
timeout --kill-after=60 "$TIMEOUT" "${RUN[@]}" > "$OUT/console.log" 2>&1 || status=$?

cp "$RUN_DIR"/screenshots/capsule-smoke/* "$OUT/" 2>/dev/null || true
cp "$RUN_DIR/logs/latest.log" "$OUT/client.log" 2>/dev/null || true

failures=()
[ "$status" -eq 0 ] || failures+=("the client exited with status $status")
if [ -f "$OUT/report.txt" ]; then
    cat "$OUT/report.txt"
    grep -q '^RESULT PASS' "$OUT/report.txt" || failures+=("a check failed")
    grep -q "^PASS $VIEWER_NAME shows the capsule recipes" "$OUT/report.txt" || failures+=("$VIEWER_NAME did not show the capsule recipes")
else
    failures+=("no report: the scenario did not finish")
    tail -n 40 "$OUT/console.log"
fi

if [ "${#failures[@]}" -gt 0 ]; then
    printf 'FAIL: %s\n' "${failures[@]}"
    exit 1
fi
echo "OK: $LOADER client smoke test passed, screenshots in $OUT"
