#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Capsule Mod - Publish script for CurseForge and Modrinth
# =============================================================================
#
# Prerequisites:
#   - Set environment variables:
#       CURSEFORGE_TOKEN  - API token from https://authors-old.curseforge.com/account/api-tokens
#       MODRINTH_TOKEN    - PAT from https://modrinth.com/settings/pats
#                           Required scopes: CREATE_VERSION, VERSION_WRITE
#       CURSEFORGE_PROJECT_ID - Numeric project ID (found on CurseForge project page sidebar)
#
# Usage:
#   ./publish.sh [--dry-run] <jar-file> [release|beta|alpha]
#
# Examples:
#   ./publish.sh --dry-run neoforge/build/libs/Capsule-neoforge-1.21.1-9.0.42.jar beta
#   ./publish.sh fabric/build/libs/Capsule-fabric-1.21.1-9.0.42.jar release
#
# Options:
#   --dry-run  Show the commands that would be executed without actually uploading
#
# The loader and version are inferred from the jar filename, one jar per call.
# Format: Capsule-<neoforge|fabric>-<mcversion>-<major>.<minor>.<BUILD_ID>.jar
# The Fabric jar is published with its required dependencies: Fabric API and
# Forge Config API Port.
# =============================================================================

DRY_RUN=false
if [ "${1:-}" = "--dry-run" ]; then
    DRY_RUN=true
    shift
fi

# Jar file is the first positional argument
JAR_FILE="${1:-}"
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: No jar file specified."
    echo ""
    echo "Usage: ./publish.sh [--dry-run] <jar-file> [release|beta|alpha]"
    echo "Example: ./publish.sh neoforge/build/libs/Capsule-neoforge-1.21.1-9.0.42.jar release"
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: File not found: $JAR_FILE"
    exit 1
fi

RELEASE_TYPE="${2:-release}"
MODRINTH_PROJECT_ID="Pt0JOpyz"

# Extract loader and version info from jar name: Capsule-fabric-1.21.1-9.0.42.jar -> fabric, 1.21.1-9.0.42
JAR_NAME=$(basename "$JAR_FILE")
LOADER=$(echo "$JAR_NAME" | sed -n 's/^Capsule-\(neoforge\|fabric\)-.*\.jar$/\1/p')
VERSION=$(echo "$JAR_NAME" | sed 's/^Capsule-[a-z]*-//; s/\.jar$//')
# Extract MC version: 1.21.1-9.0.42 -> 1.21.1
MINECRAFT_VERSION=$(echo "$VERSION" | sed 's/-.*//')

# Loader name as listed by CurseForge, and required dependencies (Modrinth project ids, CurseForge slugs)
case "$LOADER" in
    neoforge)
        LOADER_NAME="NeoForge"
        MR_DEPENDENCIES='[]'
        CF_RELATIONS='[]'
        ;;
    fabric)
        LOADER_NAME="Fabric"
        # Fabric API, Forge Config API Port
        MR_DEPENDENCIES='[{"project_id": "P7dR8mSH", "dependency_type": "required"}, {"project_id": "ohNO6lps", "dependency_type": "required"}]'
        CF_RELATIONS='[{"slug": "fabric-api", "type": "requiredDependency"}, {"slug": "forge-config-api-port-fabric", "type": "requiredDependency"}]'
        ;;
    *)
        echo "ERROR: Cannot read the loader from $JAR_NAME, expected Capsule-<neoforge|fabric>-<mcversion>-<version>.jar"
        exit 1
        ;;
esac

# Validate env vars (skip in dry-run mode)
if [ "$DRY_RUN" = false ]; then
    for var in CURSEFORGE_TOKEN MODRINTH_TOKEN CURSEFORGE_PROJECT_ID; do
        if [ -z "${!var:-}" ]; then
            echo "ERROR: $var is not set."
            echo ""
            echo "Required environment variables:"
            echo "  CURSEFORGE_TOKEN       - from https://authors-old.curseforge.com/account/api-tokens"
            echo "  MODRINTH_TOKEN         - from https://modrinth.com/settings/pats"
            echo "  CURSEFORGE_PROJECT_ID  - numeric ID from your CurseForge project page"
            exit 1
        fi
    done
fi

echo "Publishing: $JAR_NAME"
echo "Version:    $VERSION"
echo "MC version: $MINECRAFT_VERSION"
echo "Type:       $RELEASE_TYPE"
echo "Loader:     $LOADER_NAME"
if [ "$DRY_RUN" = true ]; then
    echo "Mode:       DRY RUN (no uploads will be made)"
fi
echo ""

# Extract latest changelog entry from CHANGELOG.md
# Takes everything between the first and second "**X.Y.Z" headers
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
CHANGELOG_FILE="$SCRIPT_DIR/CHANGELOG.md"

if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "ERROR: CHANGELOG.md not found at $CHANGELOG_FILE"
    exit 1
fi

CHANGELOG=$(awk '/^\*\*[0-9]/{if(found) exit; found=1; next} found' "$CHANGELOG_FILE")
CHANGELOG="$CHANGELOG

Full changelog: https://github.com/Lythom/capsule/blob/master/CHANGELOG.md"

echo "Changelog preview:"
echo "$CHANGELOG" | head -8
echo ""

# Escape changelog for JSON
JSON_CHANGELOG=$(echo "$CHANGELOG" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read()))")

if [ "$DRY_RUN" = true ]; then
    # ---- Dry run: display commands ----
    echo "==== CurseForge ===="
    echo ""
    echo "# 1. Fetch game version IDs"
    echo "curl -s -H 'X-Api-Token: \$CURSEFORGE_TOKEN' \\"
    echo "    'https://minecraft.curseforge.com/api/game/versions'"
    echo ""
    echo "# 2. Upload file (replace <MC_VERSION_ID> and <LOADER_ID> by the ids of '$MINECRAFT_VERSION' and '$LOADER_NAME' from step 1)"
    cat <<DRYEOF
curl -X POST \\
    -H 'X-Api-Token: \$CURSEFORGE_TOKEN' \\
    -F 'metadata={
  "changelog": $JSON_CHANGELOG,
  "changelogType": "markdown",
  "displayName": "Capsule $VERSION ($LOADER_NAME)",
  "gameVersions": [<MC_VERSION_ID>, <LOADER_ID>],
  "releaseType": "$RELEASE_TYPE",
  "relations": {"projects": $CF_RELATIONS}
};type=application/json' \\
    -F 'file=@$JAR_FILE;type=application/java-archive' \\
    'https://minecraft.curseforge.com/api/projects/\$CURSEFORGE_PROJECT_ID/upload-file'
DRYEOF

    echo ""
    echo "==== Modrinth ===="
    echo ""
    cat <<DRYEOF
curl -X POST \\
    -H 'Authorization: \$MODRINTH_TOKEN' \\
    -F 'data={
  "name": "Capsule $VERSION ($LOADER_NAME)",
  "version_number": "$VERSION-$LOADER",
  "changelog": $JSON_CHANGELOG,
  "dependencies": $MR_DEPENDENCIES,
  "game_versions": ["$MINECRAFT_VERSION"],
  "version_type": "$RELEASE_TYPE",
  "loaders": ["$LOADER"],
  "featured": true,
  "project_id": "$MODRINTH_PROJECT_ID",
  "file_parts": ["mod_file"],
  "status": "listed"
};type=application/json' \\
    -F 'mod_file=@$JAR_FILE;type=application/java-archive' \\
    'https://api.modrinth.com/v2/version'
DRYEOF

    echo ""
    echo "Dry run complete. No uploads were made."
else
    # ---- Fetch CurseForge game version IDs ----
    echo "Fetching CurseForge game version IDs..."

    CF_VERSIONS_JSON=$(curl -s -H "X-Api-Token: $CURSEFORGE_TOKEN" \
        "https://minecraft.curseforge.com/api/game/versions")

    # Find the ID for Minecraft version
    CF_MC_VERSION_ID=$(echo "$CF_VERSIONS_JSON" | python3 -c "
import json, sys
versions = json.load(sys.stdin)
for v in versions:
    if v['name'] == '$MINECRAFT_VERSION':
        print(v['id'])
        break
" 2>/dev/null || echo "")

    # Find the ID for the loader
    CF_LOADER_ID=$(echo "$CF_VERSIONS_JSON" | python3 -c "
import json, sys
versions = json.load(sys.stdin)
for v in versions:
    if v['name'].lower() == '$LOADER':
        print(v['id'])
        break
" 2>/dev/null || echo "")

    if [ -z "$CF_MC_VERSION_ID" ]; then
        echo "WARNING: Could not find CurseForge version ID for $MINECRAFT_VERSION"
        echo "Available versions:"
        echo "$CF_VERSIONS_JSON" | python3 -c "
import json, sys
for v in json.load(sys.stdin):
    if '1.21' in v['name']:
        print(f\"  {v['id']}: {v['name']}\")
" 2>/dev/null
        echo "Set CF_MC_VERSION_ID manually and re-run, or check the API response."
        exit 1
    fi

    echo "CurseForge MC version ID: $CF_MC_VERSION_ID"
    echo "CurseForge $LOADER_NAME ID: $CF_LOADER_ID"

    # Build gameVersions array: without the loader the file would be listed for every loader
    if [ -z "$CF_LOADER_ID" ]; then
        echo "ERROR: CurseForge loader ID not found for $LOADER_NAME"
        exit 1
    fi
    CF_GAME_VERSIONS="[$CF_MC_VERSION_ID, $CF_LOADER_ID]"

    # ---- Upload to CurseForge ----
    echo ""
    echo "Uploading to CurseForge..."

    CF_METADATA=$(cat <<METAEOF
{
  "changelog": $JSON_CHANGELOG,
  "changelogType": "markdown",
  "displayName": "Capsule $VERSION ($LOADER_NAME)",
  "gameVersions": $CF_GAME_VERSIONS,
  "releaseType": "$RELEASE_TYPE",
  "relations": {"projects": $CF_RELATIONS}
}
METAEOF
    )

    CF_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "X-Api-Token: $CURSEFORGE_TOKEN" \
        -F "metadata=$CF_METADATA;type=application/json" \
        -F "file=@$JAR_FILE;type=application/java-archive" \
        "https://minecraft.curseforge.com/api/projects/$CURSEFORGE_PROJECT_ID/upload-file")

    CF_HTTP_CODE=$(echo "$CF_RESPONSE" | tail -1)
    CF_BODY=$(echo "$CF_RESPONSE" | sed '$d')

    if [ "$CF_HTTP_CODE" -ge 200 ] && [ "$CF_HTTP_CODE" -lt 300 ]; then
        echo "CurseForge: SUCCESS (HTTP $CF_HTTP_CODE)"
        echo "Response: $CF_BODY"
    else
        echo "CurseForge: FAILED (HTTP $CF_HTTP_CODE)"
        echo "Response: $CF_BODY"
    fi

    # ---- Upload to Modrinth ----
    echo ""
    echo "Uploading to Modrinth..."

    MR_DATA=$(cat <<MREOF
{
  "name": "Capsule $VERSION ($LOADER_NAME)",
  "version_number": "$VERSION-$LOADER",
  "changelog": $JSON_CHANGELOG,
  "dependencies": $MR_DEPENDENCIES,
  "game_versions": ["$MINECRAFT_VERSION"],
  "version_type": "$RELEASE_TYPE",
  "loaders": ["$LOADER"],
  "featured": true,
  "project_id": "$MODRINTH_PROJECT_ID",
  "file_parts": ["mod_file"],
  "status": "listed"
}
MREOF
    )

    MR_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: $MODRINTH_TOKEN" \
        -F "data=$MR_DATA;type=application/json" \
        -F "mod_file=@$JAR_FILE;type=application/java-archive" \
        "https://api.modrinth.com/v2/version")

    MR_HTTP_CODE=$(echo "$MR_RESPONSE" | tail -1)
    MR_BODY=$(echo "$MR_RESPONSE" | sed '$d')

    if [ "$MR_HTTP_CODE" -ge 200 ] && [ "$MR_HTTP_CODE" -lt 300 ]; then
        echo "Modrinth: SUCCESS (HTTP $MR_HTTP_CODE)"
        echo "Response: $MR_BODY"
    else
        echo "Modrinth: FAILED (HTTP $MR_HTTP_CODE)"
        echo "Response: $MR_BODY"
    fi

    echo ""
    echo "Done! Check your mod pages:"
    echo "  CurseForge: https://www.curseforge.com/minecraft/mc-mods/capsule/files"
    echo "  Modrinth:   https://modrinth.com/mod/capsule/versions"
fi
