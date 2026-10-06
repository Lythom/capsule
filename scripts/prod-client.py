#!/usr/bin/env python3
"""Production NeoForge client, the way a launcher installs and starts it (no Gradle, no dev classpath).

Usage:
  prod-client.py modpack <modrinth project slug>[:<version id>] | <file.mrpack>  <game-dir>
      installs the client files and overrides of a Modrinth modpack in <game-dir>, prints its NeoForge version
  prod-client.py launch <neoforge-version> <game-dir> [jvm argument...] [-- game argument...]
      installs the NeoForge client once (official installer, vanilla libraries and assets without sounds), then
      replaces this process with the client

Downloads are checked against their sha1 and kept in CAPSULE_CACHE (default ~/.cache/capsule-validation).
"""
import hashlib
import io
import json
import os
import shutil
import subprocess
import sys
import urllib.request
import uuid
import zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

USER_AGENT = 'Lythom/capsule (contact@samuel-bouchet.fr)'
CACHE = Path(os.environ.get('CAPSULE_CACHE') or Path(os.environ.get('XDG_CACHE_HOME') or Path.home() / '.cache') / 'capsule-validation')


def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': USER_AGENT}), timeout=120).read()


def download(url, dest, sha1=None):
    """Writes url to dest unless dest already has the expected content."""
    dest = Path(dest)
    if dest.exists() and (sha1 is None or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    data = fetch(url)
    if sha1 and hashlib.sha1(data).hexdigest() != sha1:
        raise RuntimeError(f'sha1 mismatch for {url}')
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return dest


def install_file(file, game_dir):
    """A modpack file, from the cache."""
    sha1 = file['hashes']['sha1']
    target = Path(game_dir) / file['path']
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(download(file['downloads'][0], CACHE / 'sha1' / sha1, sha1), target)


def modpack(pack, game_dir):
    if pack.endswith('.mrpack'):
        data = Path(pack).read_bytes()
    else:
        slug, _, version = pack.partition(':')
        versions = json.loads(fetch(f'https://api.modrinth.com/v2/project/{slug}/version'
                                    '?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D'))
        chosen = next(v for v in versions if not version or v['id'] == version)
        file = next(f for f in chosen['files'] if f['primary'])
        data = download(file['url'], CACHE / 'modpacks' / file['filename'], file['hashes']['sha1']).read_bytes()
    pack_zip = zipfile.ZipFile(io.BytesIO(data))
    index = json.loads(pack_zip.read('modrinth.index.json'))
    files = [f for f in index['files'] if f.get('env', {}).get('client') != 'unsupported']
    with ThreadPoolExecutor(8) as pool:
        list(pool.map(lambda f: install_file(f, game_dir), files))
    for name in pack_zip.namelist():
        for prefix in ('overrides/', 'client-overrides/'):
            if name.startswith(prefix) and not name.endswith('/'):
                target = Path(game_dir) / name[len(prefix):]
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(pack_zip.read(name))
    print(index['dependencies']['neoforge'])


def allowed(rules):
    """Launcher rules: the last matching rule decides, on Linux with no optional feature (demo, quick play...)."""
    result = not rules
    for rule in rules or []:
        if rule.get('os', {}).get('name', 'linux') == 'linux' and not rule.get('features'):
            result = rule['action'] == 'allow'
    return result


def install(neo_version):
    instance = CACHE / f'neoforge-client-{neo_version}'
    neo_json = instance / 'versions' / f'neoforge-{neo_version}' / f'neoforge-{neo_version}.json'
    if not neo_json.exists():
        instance.mkdir(parents=True, exist_ok=True)
        (instance / 'launcher_profiles.json').write_text('{"profiles": {}}')
        installer = download(f'https://maven.neoforged.net/releases/net/neoforged/neoforge/{neo_version}/neoforge-{neo_version}-installer.jar',
                             CACHE / 'downloads' / f'neoforge-{neo_version}-installer.jar')
        with open(instance / 'installer.log', 'w') as log:
            subprocess.run(['java', '-jar', str(installer), '--installClient', str(instance)], cwd=instance, stdout=log, stderr=log, check=True)
    neo = json.loads(neo_json.read_text())
    vanilla_json = instance / 'versions' / neo['inheritsFrom'] / f"{neo['inheritsFrom']}.json"
    if not vanilla_json.exists():
        manifest = json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        entry = next(v for v in manifest['versions'] if v['id'] == neo['inheritsFrom'])
        download(entry['url'], vanilla_json, entry['sha1'])
    vanilla = json.loads(vanilla_json.read_text())
    libraries = [lib for lib in neo['libraries'] + vanilla['libraries'] if allowed(lib.get('rules'))]
    artifacts = [lib['downloads']['artifact'] for lib in libraries]
    index = vanilla['assetIndex']
    index_file = download(index['url'], instance / 'assets' / 'indexes' / f"{index['id']}.json", index['sha1'])
    objects = [o['hash'] for name, o in json.loads(index_file.read_text())['objects'].items()
               if not name.startswith(('minecraft/sounds/', 'minecraft/music/'))]
    with ThreadPoolExecutor(8) as pool:
        list(pool.map(lambda a: a['url'] and download(a['url'], instance / 'libraries' / a['path'], a['sha1']), artifacts))
        list(pool.map(lambda h: download(f'https://resources.download.minecraft.net/{h[:2]}/{h}',
                                         instance / 'assets' / 'objects' / h[:2] / h, h), objects))
    return instance, neo, vanilla, artifacts


def launch(neo_version, game_dir, extra):
    instance, neo, vanilla, artifacts = install(neo_version)
    jvm_extra, game_extra = (extra[:extra.index('--')], extra[extra.index('--') + 1:]) if '--' in extra else (extra, [])
    seen, classpath = set(), []
    for artifact in artifacts:
        path = str(instance / 'libraries' / artifact['path'])
        if path not in seen:
            seen.add(path)
            classpath.append(path)
    values = {
        'natives_directory': str(instance / 'natives'), 'launcher_name': 'capsule-validation', 'launcher_version': '1',
        'classpath': os.pathsep.join(classpath), 'library_directory': str(instance / 'libraries'),
        'classpath_separator': os.pathsep, 'version_name': neo['id'], 'game_directory': str(Path(game_dir).resolve()),
        'assets_root': str(instance / 'assets'), 'assets_index_name': vanilla['assetIndex']['id'],
        'auth_player_name': 'Smoke', 'auth_uuid': str(uuid.uuid3(uuid.NAMESPACE_OID, 'Smoke')), 'auth_access_token': '0',
        'clientid': '', 'auth_xuid': '', 'user_type': 'msa', 'version_type': 'release',
    }

    def arguments(kind):
        result = []
        for spec in vanilla['arguments'][kind] + neo['arguments'][kind]:
            if isinstance(spec, dict):
                if not allowed(spec['rules']):
                    continue
                spec = spec['value']
            for arg in spec if isinstance(spec, list) else [spec]:
                for key, value in values.items():
                    arg = arg.replace('${' + key + '}', value)
                result.append(arg)
        return result

    Path(game_dir).mkdir(parents=True, exist_ok=True)
    command = ['java', '-Xmx3G', *arguments('jvm'), *jvm_extra, neo['mainClass'], *arguments('game'), *game_extra]
    os.chdir(game_dir)
    os.execvp(command[0], command)


if __name__ == '__main__':
    if len(sys.argv) >= 4 and sys.argv[1] == 'modpack':
        modpack(sys.argv[2], sys.argv[3])
    elif len(sys.argv) >= 4 and sys.argv[1] == 'launch':
        launch(sys.argv[2], sys.argv[3], sys.argv[4:])
    else:
        sys.exit(__doc__)
