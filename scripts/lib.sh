# Sourced by the validation scripts: versions, cached downloads and real server installs.
# Downloads go to CAPSULE_CACHE (default ~/.cache/capsule-validation), outside the repository.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE="${CAPSULE_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/capsule-validation}"
mkdir -p "$CACHE"
USER_AGENT='Lythom/capsule (contact@samuel-bouchet.fr)'

prop() {
    grep "^$1=" "$ROOT/gradle.properties" | cut -d= -f2-
}

MC_VERSION="$(prop minecraft_version)"
NEO_VERSION="$(prop neo_version)"
FABRIC_LOADER_VERSION="$(prop fabric_loader_version)"
FABRIC_API_VERSION="$(prop fabric_api_version)"
FCAP_VERSION="$(prop forge_config_api_port_version)"

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

# modrinth_jar <project> <version id or number> <dir>: a mod jar from the Modrinth maven
modrinth_jar() {
    download "https://api.modrinth.com/maven/maven/modrinth/$1/$2/$1-$2.jar" "$3/$1-$2.jar"
}

# modrinth_file <sha1> <dir>: the Modrinth file with this sha1
modrinth_file() {
    modrinth_url "$(curl -fsSL -A "$USER_AGENT" "https://api.modrinth.com/v2/version_file/$1?algorithm=sha1" \
        | python3 -c 'import json,sys; print(next(f["url"] for f in json.load(sys.stdin)["files"] if f["hashes"]["sha1"] == sys.argv[1]))' "$1")" "$2"
}

# modrinth_latest <project> <loader> <dir>: the primary file of the newest version of the project for this loader and
# Minecraft version
modrinth_latest() {
    modrinth_url "$(curl -fsSL -A "$USER_AGENT" "https://api.modrinth.com/v2/project/$1/version?loaders=%5B%22$2%22%5D&game_versions=%5B%22$MC_VERSION%22%5D" \
        | python3 -c 'import json,sys; print(next(f["url"] for f in json.load(sys.stdin)[0]["files"] if f["primary"]))')" "$3"
}

# modrinth_url <url> <dir>: a Modrinth file, named without spaces or brackets (EXTRA_MODS is split on spaces)
modrinth_url() {
    download "$1" "$2/$(basename "$1" | python3 -c 'import re,sys,urllib.parse; print(re.sub(r"[ ()\[\]]", "_", urllib.parse.unquote(sys.stdin.read().strip())))')"
}

loader_of() {
    case "$(basename "$1")" in
        *neoforge*) echo neoforge ;;
        *fabric*) echo fabric ;;
        *) echo "ERROR: cannot tell the loader of $1" >&2; return 1 ;;
    esac
}

# setup_<loader> <dir> [jvm argument...]: installs a server in <dir> and sets START, the command starting it there
setup_neoforge() {
    local dir="$1"
    shift
    local server="$CACHE/neoforge-server-$NEO_VERSION"
    if [ ! -f "$server/run.sh" ]; then
        rm -rf "$server"
        mkdir -p "$server"
        download "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEO_VERSION/neoforge-$NEO_VERSION-installer.jar" "$server/installer.jar"
        (cd "$server" && java -jar installer.jar --installServer . > installer.log 2>&1)
        rm "$server/installer.jar"
    fi
    cp -r "$server/." "$dir/"
    printf '\n%s' -Xmx2G "$@" >> "$dir/user_jvm_args.txt"
    START=(./run.sh nogui)
}

setup_fabric() {
    local dir="$1"
    shift
    local installer
    installer="$(curl -fsSL https://meta.fabricmc.net/v2/versions/installer | python3 -c 'import json,sys; print(next(v["version"] for v in json.load(sys.stdin) if v["stable"]))')"
    download "https://meta.fabricmc.net/v2/versions/loader/$MC_VERSION/$FABRIC_LOADER_VERSION/$installer/server/jar" "$dir/fabric-server-launch.jar"
    mkdir -p "$dir/mods"
    download "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$FABRIC_API_VERSION/fabric-api-$FABRIC_API_VERSION.jar" "$dir/mods/fabric-api-$FABRIC_API_VERSION.jar"
    modrinth_jar forge-config-api-port "$FCAP_VERSION" "$dir/mods"
    START=(java -Xmx2G "$@" -jar fabric-server-launch.jar nogui)
}

free_port() {
    python3 -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'
}
