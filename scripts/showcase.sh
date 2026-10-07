#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Showcase: records the 9.1 features in a real dev client, for the illustrated changelog
# =============================================================================
#
# Usage:
#   scripts/showcase.sh neoforge|fabric [scene,scene...]
#
# Runs the client smoke harness (Gradle task runClientSmoke) with -Pshowcase: instead of the
# smoke scenario, capsule.clientsmoke.Showcase stages one scene per feature in a flat world and
# records each with ffmpeg (x11grab of the Xvfb display), plus PNG stills. Scenes: title,
# modmenu, capture (always played), preview, deploy, loyalty, enchanting, lava, claim (needs
# Flan in EXTRA_MODS), blueprint, viewer. Default: all.
#
# Output: build/showcase/<loader>/ (or OUT): the PNG stills, the .mkv recordings and a GIF of each
# (GIF_WIDTH, default 960, at GIF_FPS, default 15). RECIPE_VIEWER, EXTRA_MODS, TIMEOUT and
# GRADLE_ARGS as in scripts/client-smoke.sh. Needs ffmpeg. Dev only: nothing of it is in the jars.
# =============================================================================

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOADER="${1:-}"
SCENES="${2:-}"
TIMEOUT="${TIMEOUT:-1800}"
RECIPE_VIEWER="${RECIPE_VIEWER:-jei}"
GIF_WIDTH="${GIF_WIDTH:-960}"
GIF_FPS="${GIF_FPS:-15}"

case "$LOADER" in
    neoforge|fabric) ;;
    *) echo "Usage: scripts/showcase.sh neoforge|fabric [scene,scene...]"; exit 1 ;;
esac
command -v ffmpeg > /dev/null || { echo "ffmpeg is needed"; exit 1; }

OUT="${OUT:-$ROOT/build/showcase/$LOADER}"
RUN_DIR="$ROOT/$LOADER/runs/clientSmoke"
rm -rf "$OUT" "$RUN_DIR/screenshots/capsule-showcase" "$RUN_DIR/mods"
mkdir -p "$OUT" "$RUN_DIR/mods"
for jar in ${EXTRA_MODS:-}; do
    cp "$jar" "$RUN_DIR/mods/"
done

RUN=("$ROOT/gradlew" -p "$ROOT" ${GRADLE_ARGS:-} ":$LOADER:runClientSmoke" "-PrecipeViewer=$RECIPE_VIEWER" "-Pshowcase=$SCENES")
if [ -z "${DISPLAY:-}" ]; then
    RUN=(xvfb-run -a -s "-screen 0 1280x720x24" "${RUN[@]}")
fi
echo "== $LOADER showcase ${SCENES:-(all scenes)} (log: $OUT/console.log)"
status=0
timeout --kill-after=60 "$TIMEOUT" "${RUN[@]}" > "$OUT/console.log" 2>&1 || status=$?

cp "$RUN_DIR"/screenshots/capsule-showcase/* "$OUT/" 2>/dev/null || true
cp "$RUN_DIR/logs/latest.log" "$OUT/client.log" 2>/dev/null || true
[ -f "$OUT/report.txt" ] && grep -v '^PASS' "$OUT/report.txt" || true

for video in "$OUT"/*.mkv; do
    [ -f "$video" ] || continue
    gif="${video%.mkv}.gif"
    ffmpeg -loglevel error -y -i "$video" -vf "fps=$GIF_FPS,scale=$GIF_WIDTH:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" "$gif"
    echo "$(basename "$gif"): $(du -h "$gif" | cut -f1)"
done
[ "$status" -eq 0 ] || { echo "FAIL: the client exited with status $status"; exit 1; }
echo "OK: showcase in $OUT"
