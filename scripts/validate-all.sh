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
# of each loader with JEI, REI and EMI (scripts/client-smoke.sh). Slower variants, off by default:
#   --iris     client smoke with Iris, Sodium and the MakeUp Ultra Fast shader pack
#   --modded   client smoke with the mods of the modded block issues (#81, #94, #117, #76)
#   --modpack  client smoke in a production NeoForge client with a Sinytra Connector
#              modpack (MODPACK, default forgeulously-optimized:mPRwXMh4)
#   --incompat GameTests with the mods of the known incompatibilities (-Pincompat), also on
#              the NeoForge release jar
#   --all      all of them
# MODCOMPAT_LATEST=1 runs the mod-compat and incompatibility steps with the newest Modrinth
# versions of their mods instead of the pinned ones (-PmodCompatLatest).
#
# Each step takes the machine-wide lock /tmp/capsule-heavy.lock, so at most one Minecraft
# runs at a time. Logs: build/validate-all/<step>.log and summary.txt; client smoke runs:
# build/client-smoke/<step>/ (screenshots, report.txt, console.log, client.log).
# Downloads are cached outside the repository (CAPSULE_CACHE, default
# ~/.cache/capsule-validation). GRADLE_ARGS adds arguments to every Gradle call.
# Needs Java 21, python3, curl, xvfb-run and network access; Gradle runs on a Java 25 it
# downloads itself (gradle/gradle-daemon-jvm.properties). Exit code 0 when every step passes.
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
        *) sed -n '7,25p' "$0"; exit 1 ;;
    esac
done
for tool in java python3 curl xvfb-run flock; do
    command -v "$tool" > /dev/null || { echo "Missing $tool"; exit 1; }
done

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

# expect_failure <command...>: runs the command with the failing test on, succeeds when it reports that one failure
expect_failure() {
    local out status=0
    out="$(FAIL_ON_PURPOSE=1 "$@" 2>&1)" || status=$?
    echo "$out"
    [ "$status" -ne 0 ] && grep -q '1 required tests failed' <<< "$out" && grep -q -- '- failsonpurpose' <<< "$out" || return 1
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
# Get Off My Lawn ReServed 1.13.1+1.21, Open Parties and Claims 0.32.7, Flan 1.12.8: GOML runs on real servers only
compat_mods "$CACHE/modcompat/fabric" fabric goml-reserved:3c513317d251589d7c13f01f5beb2e4020387999 \
    open-parties-and-claims:b992b683056fab2c3f462907901fdc4054a26529 flan:debe440eeb5440765115b1d50269df9014ba12ea
step prod-modcompat-fabric "Fabric release jar with the claim mods" \
    env EXTRA_MODS="$(ls "$CACHE/modcompat/fabric"/*.jar | tr '\n' ' ')" scripts/prod-gametest.sh "$FABRIC_JAR"
step prod-gametest-proof-neoforge "NeoForge release jar: failure reported" expect_failure scripts/prod-gametest.sh "$NEO_JAR"
step prod-gametest-proof-fabric "Fabric release jar: failure reported" expect_failure scripts/prod-gametest.sh "$FABRIC_JAR"
step prod-smoke "release jars on dedicated servers" scripts/prod-smoke.sh "$NEO_JAR" "$FABRIC_JAR"
for loader in neoforge fabric; do
    for viewer in jei rei emi; do
        smoke "$loader-$viewer" "$loader" "$viewer"
    done
done

if [ -n "$IRIS" ]; then
    # Iris 1.8.12 / 1.8.8 and Sodium 0.6.13, MakeUp Ultra Fast 9.5g
    mods "$CACHE/iris/neoforge" a3e6355915c7d3b2bc392724795113e51d289378 38af70fa4dc4b2aaac636e92fdba3bedd5a025e1
    mods "$CACHE/iris/fabric" 4e0df4011503b4b36ba69877ea7247c1a47fea00 928a2598178c3a58b0638bab842f467d2e49251a
    mods "$CACHE/iris/pack" 57938ae6e1c83a2758b2e5dab80061d5002e88e7
    for loader in neoforge fabric; do
        smoke "$loader-iris" "$loader" jei TIMEOUT=3000 SHADER_PACK="$(ls "$CACHE"/iris/pack/*.zip)" \
            EXTRA_MODS="$(ls "$CACHE/iris/$loader"/*.jar | tr '\n' ' ')"
    done
fi

if [ -n "$MODDED" ]; then
    # NeoForge: Integrated Dynamics 1.38.0 (Cyclops Core, Common Capabilities, Integrated Tunnels, Refined Storage),
    # Ad Astra 1.16.26 (Resourceful Lib, Resourceful Config, Common Storage Lib), Farmer's Delight 1.3.4
    mods "$CACHE/modded/neoforge" e9616abb2fbca9e86e93c5c5b792e5c66b0c5518 e91a27152acfaed1f6992525c16907ad354b711d \
        2bc5b853dda5e0fa121017dee218a9f49309c109 6c01dd38de67d1ce1d36ad14ef7eb6dc87907321 59d8e734ccb6eaab4cdf04093c55872af7a3e23a \
        b6c4560cc625a6847e016af576954ff1f0431d99 24389c8e48ff0e8594e4686b750c494c66a5f8df 6bf2b6ea9c071c00a2abcdc552667fb6f7d21031 \
        e0ba85038480d50a3816d5920014bfcccdbe7329 27675ddfcb8cc37e6ea1ab6c15051c7949d94657
    # Fabric: Ad Astra 1.16.26 (same libraries), Farmer's Delight Refabricated 3.2.8
    mods "$CACHE/modded/fabric" 905627aaeb61b5f0f5b36c376de43302d7099169 cb67e1302b5116953f45556cc61dbcf40d986ecd \
        f6a0df7cf464d1c94458ef075bead82033add9c9 e04f6271136d36c75f5cb2bb58f6fd9efd52cf68 7adf7fbfbc29b87546b286a5d9a59358dfeca1e8
    for loader in neoforge fabric; do
        smoke "$loader-modded" "$loader" jei EXTRA_MODS="$(ls "$CACHE/modded/$loader"/*.jar | tr '\n' ' ')"
    done
fi

if [ -n "$INCOMPAT" ]; then
    step incompat-neoforge "incompatibility GameTests NeoForge" "${GRADLE[@]}" :neoforge:runGameTestServer -Pincompat "${COMPAT[@]}"
    # Refined Storage 2.0.9, Mekanism 10.7.19.85, Immersive Engineering 12.4.2-194, Super Factory Manager 4.34.0,
    # Corail Tombstone 9.5.6
    compat_mods "$CACHE/incompat/neoforge" neoforge refined-storage:59d8e734ccb6eaab4cdf04093c55872af7a3e23a \
        mekanism:b78945c40cfe7640408f3fd1e44da385a8c8b805 immersiveengineering:a4e90c2df8009040f6d022433c5d76635944dd59 \
        super-factory-manager:1c170062c486bd25a1161571b6c3cf9e6371611b corail-tombstone:d830d16caa20b0d23a44ed6b1d339bc22afc2460
    step prod-incompat-neoforge "NeoForge release jar, incompatible mods" \
        env EXTRA_MODS="$(ls "$CACHE/incompat/neoforge"/*.jar | tr '\n' ' ')" scripts/prod-gametest.sh "$NEO_JAR"
fi

if [ -n "$MODPACK_RUN" ]; then
    smoke neoforge-modpack neoforge jei MODPACK="${MODPACK:-forgeulously-optimized:mPRwXMh4}"
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
