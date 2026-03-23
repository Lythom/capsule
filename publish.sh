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
#   ./publish.sh --dry-run build/libs/Capsule-1.21.1-9.0.42.jar beta
#   ./publish.sh build/libs/Capsule-1.21.1-9.0.42.jar release
#
# Options:
#   --dry-run  Show the commands that would be executed without actually uploading
#
# The version is inferred from the jar filename.
# Format: Capsule-<mcversion>-<major>.<minor>.<BUILD_ID>.jar
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
    echo "Example: ./publish.sh build/libs/Capsule-1.21.1-9.0.42.jar release"
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: File not found: $JAR_FILE"
    exit 1
fi

RELEASE_TYPE="${2:-release}"
MODRINTH_PROJECT_ID="Pt0JOpyz"
LOADER="neoforge"

# Extract version info from jar name: Capsule-1.21.1-9.0.42.jar -> 1.21.1-9.0.42
JAR_NAME=$(basename "$JAR_FILE")
VERSION=$(echo "$JAR_NAME" | sed 's/^Capsule-//; s/\.jar$//')
# Extract MC version: 1.21.1-9.0.42 -> 1.21.1
MINECRAFT_VERSION=$(echo "$VERSION" | sed 's/-.*//')

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
echo "Loader:     $LOADER"
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
    echo "# 2. Upload file (replace <MC_VERSION_ID> and <NEOFORGE_ID> from step 1)"
    cat <<DRYEOF
curl -X POST \\
    -H 'X-Api-Token: \$CURSEFORGE_TOKEN' \\
    -F 'metadata={
  "changelog": $JSON_CHANGELOG,
  "changelogType": "markdown",
  "displayName": "Capsule $VERSION",
  "gameVersions": [<MC_VERSION_ID>, <NEOFORGE_ID>],
  "releaseType": "$RELEASE_TYPE"
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
  "name": "Capsule $VERSION",
  "version_number": "$VERSION",
  "changelog": $JSON_CHANGELOG,
  "dependencies": [],
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

    # Find the ID for NeoForge loader
    CF_NEOFORGE_ID=$(echo "$CF_VERSIONS_JSON" | python3 -c "
import json, sys
versions = json.load(sys.stdin)
for v in versions:
    if 'neoforge' in v['name'].lower() or 'NeoForge' in v['name']:
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
    echo "CurseForge NeoForge ID:   $CF_NEOFORGE_ID"

    # Build gameVersions array
    if [ -n "$CF_NEOFORGE_ID" ]; then
        CF_GAME_VERSIONS="[$CF_MC_VERSION_ID, $CF_NEOFORGE_ID]"
    else
        echo "WARNING: NeoForge loader ID not found, uploading with MC version only"
        CF_GAME_VERSIONS="[$CF_MC_VERSION_ID]"
    fi

    # ---- Upload to CurseForge ----
    echo ""
    echo "Uploading to CurseForge..."

    CF_METADATA=$(cat <<METAEOF
{
  "changelog": $JSON_CHANGELOG,
  "changelogType": "markdown",
  "displayName": "Capsule $VERSION",
  "gameVersions": $CF_GAME_VERSIONS,
  "releaseType": "$RELEASE_TYPE"
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
  "name": "Capsule $VERSION",
  "version_number": "$VERSION",
  "changelog": $JSON_CHANGELOG,
  "dependencies": [],
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
