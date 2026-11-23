#!/usr/bin/env bash
set -uo pipefail

# =============================================================================
# Every automated validation layer, with one PASS/FAIL summary
# =============================================================================
#
# Usage:
#   scripts/validate-all.sh [--iris] [--modded] [--modpack] [--incompat] [--all]
#
# Runs, in order: the build (jars, loader import check), the unit tests, the GameTests of
# each loader, the mod-compat GameTests (-PmodCompat) of each loader, the GameTests on the
# release jars (scripts/prod-gametest.sh, on Fabric also with the claim mods) and its failure
# proof, the release jars on real servers (scripts/prod-smoke.sh), and the client smoke test
# of each loader with JEI and REI (scripts/client-smoke.sh). Slower variants, off by default:
#   --iris     client smoke with Iris, Sodium and the MakeUp Ultra Fast shader pack
#   --modded   client smoke with the mods of the modded block issues (#81, #94, #117, #76):
#              not available on Minecraft 26.2 yet (Ad Astra and Farmer's Delight have no
#              26.2 build), the step is reported SKIP
#   --modpack  client smoke in a production NeoForge client with a modpack (MODPACK, a
#              Modrinth slug[:version id] or a .mrpack file). No default on 26.2: the 1.21.1
#              pack (Forgeulously Optimized) has no 26.x version, the step is SKIP without MODPACK
#   --incompat GameTests with the mod of the known incompatibilities that has a 26.2 build
#              (Corail Tombstone, -Pincompat), also on the NeoForge release jar
#   --all      all of them
# MODCOMPAT_LATEST=1 runs the mod-compat and incompatibility steps with the newest Modrinth
# versions of their mods instead of the pinned ones (-PmodCompatLatest).
#
# Each step takes the machine-wide lock /tmp/capsule-heavy.lock, so at most one Minecraft
# runs at a time. Logs: build/validate-all/<step>.log and summary.txt; client smoke runs:
# build/client-smoke/<step>/ (screenshots, report.txt, console.log, client.log).
# Downloads are cached outside the repository (CAPSULE_CACHE, default
# ~/.cache/capsule-validation). GRADLE_ARGS adds arguments to every Gradle call.
# Needs Java 25 on PATH (the release jars run on real servers with it), python3, curl, xvfb-run
# and network access. Exit code 0 when every step passes (SKIP rows do not fail the run).
# =============================================================================

source "$(dirname "$0")/lib.sh"
cd "$ROOT"
LOGS="$ROOT/build/validate-all"
SHOTS="$ROOT/build/client-smoke"
LOCK=/tmp/capsule-heavy.lock
GRADLE=(./gradlew ${GRADLE_ARGS:-})
COMPAT=(${MODCOMPAT_LATEST:+-PmodCompatLatest})

IRIS='' MODDED='' MODPACK_RUN='' INCOMPAT=''
for arg in "$@"; do
    case "$arg" in
        --iris) IRIS=1 ;;
        --modded) MODDED=1 ;;
        --modpack) MODPACK_RUN=1 ;;
        --incompat) INCOMPAT=1 ;;
        --all) IRIS=1 MODDED=1 MODPACK_RUN=1 INCOMPAT=1 ;;
        *) sed -n '7,29p' "$0"; exit 1 ;;
    esac
done
for tool in java python3 curl xvfb-run flock; do
    command -v "$tool" > /dev/null || { echo "Missing $tool"; exit 1; }
done
java_major="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p')"
[ "${java_major:-0}" -ge 25 ] 2> /dev/null || { echo "Java 25 is needed on PATH (found ${java_major:-none})"; exit 1; }

rm -rf "$LOGS"
mkdir -p "$LOGS"
ROWS=()
FAILED=0

# step <id> <description> <command...>: runs the command holding the lock, records PASS or FAIL
exec 9> "$LOCK"
step() {
    local id="$1" name="$2" log="$LOGS/$1.log" start=$SECONDS status=0
    shift 2
    echo "== $name"
    flock 9
    "$@" > "$log" 2>&1 || status=$?
    flock -u 9
    local result=PASS detail pattern
    [ "$status" -eq 0 ] || { result=FAIL; FAILED=1; }
    for pattern in '^(OK|FAIL): .*' 'All [0-9]+ required tests passed|[0-9]+ required tests failed' 'BUILD (SUCCESSFUL|FAILED)'; do
        detail="$(grep -aoE "$pattern" "$log" | tail -n 1 | cut -c1-80)"
        [ -z "$detail" ] || break
    done
    log="${log#"$ROOT"/}"
    if [ -f "$SHOTS/$id/report.txt" ]; then
        local report="$SHOTS/$id/report.txt"
        detail="$(grep -c '^PASS' "$report") checks passed, $(grep -c '^FAIL' "$report") failed, $(grep -c '^SKIP' "$report") skipped"
        log="$log, build/client-smoke/$id/"
    fi
    ROWS+=("$result|$name|$(printf '%dm%02ds' $(( (SECONDS - start) / 60 )) $(( (SECONDS - start) % 60 )))|$detail|$log")
    echo "   $result $detail"
}

# skip <id> <description> <reason>: records a step that cannot run
skip() {
    ROWS+=("SKIP|$2|0m00s|$3|-")
    echo "== $2"
    echo "   SKIP $3"
}

# expect_failure <command...>: runs the command with the failing test on, succeeds when it reports that one failure
expect_failure() {
    local out status=0
    out="$(FAIL_ON_PURPOSE=1 "$@" 2>&1)" || status=$?
    echo "$out"
    [ "$status" -ne 0 ] && grep -q '1 required tests failed' <<< "$out" && grep -q -- '- capsule:failsonpurpose' <<< "$out" || return 1
    echo "OK: the failing test was reported (exit status $status)"
}

# smoke <id> <loader> <viewer> [VAR=value...]: a client smoke run, output in build/client-smoke/<id>
smoke() {
    local id="$1" loader="$2" viewer="$3"
    shift 3
    step "$id" "client smoke $id" env RECIPE_VIEWER="$viewer" OUT="$SHOTS/$id" "$@" scripts/client-smoke.sh "$loader"
}

# mods <dir> <sha1...>: Modrinth files, by sha1 (stable even if a version is renamed)
mods() {
    local dir="$1"
    shift
    mkdir -p "$dir"
    for sha1 in "$@"; do
        modrinth_file "$sha1" "$dir" > /dev/null
    done
}

# compat_mods <dir> <loader> <project:sha1...>: the mods of a mod-compat run, their newest versions with MODCOMPAT_LATEST
compat_mods() {
    local dir="$1" loader="$2" mod
    shift 2
    rm -rf "$dir"
    mkdir -p "$dir"
    for mod in "$@"; do
        if [ -n "${MODCOMPAT_LATEST:-}" ]; then modrinth_latest "${mod%%:*}" "$loader" "$dir"; else modrinth_file "${mod#*:}" "$dir"; fi > /dev/null
    done
}

VERSION="$MC_VERSION-$(prop capsule_version).SNAPSHOT"
NEO_JAR="neoforge/build/libs/Capsule-neoforge-$VERSION.jar"
FABRIC_JAR="fabric/build/libs/Capsule-fabric-$VERSION.jar"

step build "build: jars and loader import check" "${GRADLE[@]}" assemble :common:check -Pbuild_id=SNAPSHOT
step unit-tests "unit tests (both loaders)" "${GRADLE[@]}" :neoforge:test :fabric:test
step gametest-neoforge "GameTests NeoForge" "${GRADLE[@]}" :neoforge:runGameTestServer
step gametest-fabric "GameTests Fabric" "${GRADLE[@]}" :fabric:runGameTestServer
step modcompat-neoforge "mod-compat GameTests NeoForge" "${GRADLE[@]}" :neoforge:runGameTestServer -PmodCompat "${COMPAT[@]}"
step modcompat-fabric "mod-compat GameTests Fabric" "${GRADLE[@]}" :fabric:runGameTestServer -PmodCompat "${COMPAT[@]}"
step prod-gametest-neoforge "GameTests on the NeoForge release jar" scripts/prod-gametest.sh "$NEO_JAR"
step prod-gametest-fabric "GameTests on the Fabric release jar" scripts/prod-gametest.sh "$FABRIC_JAR"
# Get Off My Lawn ReServed 1.21.0+26.2 with Polymer 0.17.5+26.2 (it requires polymer-core), Open Parties and Claims
# 0.33.0, Flan 26.2-1.12.8: GOML runs on real servers only
compat_mods "$CACHE/modcompat/fabric" fabric goml-reserved:1fb10a769a0f338b62ff23aeb25c61955a96565b \
    polymer:115afd5dc50584d1ac294fe71b5aeb4e9138e434 \
    open-parties-and-claims:138c08aecb5c3a1d57ea18054fda8735bbc2fa1e flan:4919b45aa772eaae4eff13253e9632b9b9deebbb
step prod-modcompat-fabric "Fabric release jar with the claim mods" \
    env EXTRA_MODS="$(ls "$CACHE/modcompat/fabric"/*.jar | tr '\n' ' ')" scripts/prod-gametest.sh "$FABRIC_JAR"
step prod-gametest-proof-neoforge "NeoForge release jar: failure reported" expect_failure scripts/prod-gametest.sh "$NEO_JAR"
step prod-gametest-proof-fabric "Fabric release jar: failure reported" expect_failure scripts/prod-gametest.sh "$FABRIC_JAR"
step prod-smoke "release jars on dedicated servers" scripts/prod-smoke.sh "$NEO_JAR" "$FABRIC_JAR"
for loader in neoforge fabric; do
    for viewer in jei rei; do
        smoke "$loader-$viewer" "$loader" "$viewer"
    done
done

if [ -n "$IRIS" ]; then
    # Iris 1.11.4 and Sodium 0.9.2 for 26.2, MakeUp Ultra Fast 9.5g
    mods "$CACHE/iris/neoforge" decfbe06505849620da8f1381c88af1e122ecdd3 e0a7838c8f733fecf0f2cb729e3439f70c21809f
    mods "$CACHE/iris/fabric" bd0b24731a94b8fe164531e6345718d30ab08ed2 62a70961a5b3e06938e06ab5f2dc4c5a51cf3323
    mods "$CACHE/iris/pack" 57938ae6e1c83a2758b2e5dab80061d5002e88e7
    for loader in neoforge fabric; do
        smoke "$loader-iris" "$loader" jei TIMEOUT=3000 SHADER_PACK="$(ls "$CACHE"/iris/pack/*.zip)" \
            EXTRA_MODS="$(ls "$CACHE/iris/$loader"/*.jar | tr '\n' ' ')"
    done
fi

if [ -n "$MODDED" ]; then
    # The 1.21.1 run loaded Integrated Dynamics, Ad Astra and Farmer's Delight: Ad Astra and Farmer's Delight (NeoForge)
    # have no 26.2 build (BACKLOG.md)
    skip neoforge-modded "client smoke neoforge-modded" "no 26.2 build of the modded block mods"
    skip fabric-modded "client smoke fabric-modded" "no 26.2 build of the modded block mods"
fi

if [ -n "$INCOMPAT" ]; then
    step incompat-neoforge "incompatibility GameTests NeoForge" "${GRADLE[@]}" :neoforge:runGameTestServer -Pincompat "${COMPAT[@]}"
    # Corail Tombstone 9.9.4 for 26.2. Refined Storage, Mekanism, Immersive Engineering and Super Factory Manager have
    # no 26.2 build: not tested (their GameTests are out of the build, neoforge/build.gradle)
    compat_mods "$CACHE/incompat/neoforge" neoforge corail-tombstone:08ebdd2ecf6cba8ff389e3d123432a8e2a6466cd
    step prod-incompat-neoforge "NeoForge release jar, incompatible mods" \
        env EXTRA_MODS="$(ls "$CACHE/incompat/neoforge"/*.jar | tr '\n' ' ')" scripts/prod-gametest.sh "$NEO_JAR"
fi

if [ -n "$MODPACK_RUN" ]; then
    if [ -n "${MODPACK:-}" ]; then
        smoke neoforge-modpack neoforge jei MODPACK="$MODPACK"
    else
        skip neoforge-modpack "client smoke neoforge-modpack" "no MODPACK given (no 26.2 version of the 1.21.1 pack)"
    fi
fi

{
    echo
    echo "== Summary (logs in $LOGS, screenshots in $SHOTS)"
    printf '%-6s %-42s %6s  %-80s %s\n' RESULT STEP TIME DETAIL "LOG, SCREENSHOTS"
    for row in "${ROWS[@]}"; do
        IFS="|" read -r result name duration detail log <<< "$row"
        printf '%-6s %-42s %6s  %-80s %s\n' "$result" "$name" "$duration" "$detail" "$log"
    done
} | tee "$LOGS/summary.txt"
exit "$FAILED"
