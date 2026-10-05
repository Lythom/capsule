# Minecraft versions and loaders

Which Minecraft versions and loaders Capsule should be built for, with the evidence behind the choice. Numbers come
from `docs/RESEARCH.md` (Modrinth API queries of 2026-10-05); CurseForge could not be queried by script, so packs that
live only there (mostly 1.12.2 and 1.16.5) are under-represented.

## Evidence

Capsule downloads on Modrinth, all time, by build:

| Build | Downloads |
|---|---:|
| 1.20.1 Forge | 369,091 |
| 1.12.2 Forge | 3,501 |
| 1.21.1 NeoForge | 3,233 |
| 1.16.5 Forge | 907 |
| 1.18.2 Forge | 239 |
| 1.19.2 Forge | 179 |
| 1.20.4 NeoForge | 106 |

Modrinth modpacks by Minecraft version and loader:

| MC | Modpacks | NeoForge | Forge | Fabric |
|---|---:|---:|---:|---:|
| 1.12.2 | 194 | 0 | 191 | 3 |
| 1.16.5 | 296 | 0 | 142 | 181 |
| 1.18.2 | 437 | 0 | 168 | 269 |
| 1.19.2 | 1,068 | 1 | 363 | 629 |
| **1.20.1** | **7,331** | 263 | **3,015** | **4,290** |
| 1.20.4 | 1,259 | 41 | 13 | 1,199 |
| **1.21.1** | **5,015** | **2,111** | 25 | **3,032** |
| 1.21.4 | 1,872 | 75 | 13 | 1,825 |
| 1.21.11 | 2,883 | 82 | 19 | 2,834 |
| 26.1.2 | 1,343 | 65 | 7 | 1,313 |
| 26.2 | 1,421 | 52 | 3 | 1,400 |
| 26.3 | 377 | 10 | 2 | 373 |

- Content packs sit on 1.20.1 Forge (ATM9, Better MC 4) and 1.21.1 NeoForge (ATM10, Better MC 5) or Fabric (the
  Cobblemon packs). Create is still on 1.21.1.
- The 26.x packs are mostly Fabric performance and vanilla+ packs; ATM11 (NeoForge 26.1.2) is in beta.
- Among the 300 most downloaded Modrinth packs, by highest supported version: 1.20.1 87, 1.21.1 57, 26.2 37, 26.3 33,
  1.21.11 24.

## Targets, in order

| Order | Target | Loaders | Status | Why |
|---|---|---|---|---|
| 1 | 1.21.1 | NeoForge + Fabric | this branch (`dev-1.21.1`) | second biggest pack version; 2,111 NeoForge and 3,032 Fabric packs |
| 2 | 1.20.1 | Forge + Fabric | to do (Stonecutter) | Capsule's most downloaded version by far; 3,015 Forge and 4,290 Fabric packs |
| 3 | latest 26.x | NeoForge + Fabric | to do (Stonecutter) | where new packs go once the content mods (Create, ATM11) have moved |
| — | 1.21.1 Forge | — | not built | see "Deviation" below |
| — | 1.16.5 and older | — | not built | Java 8, very different APIs, small audience on Modrinth |

1.21.1 comes first because the codebase was already on it; 1.20.1 next because it is where the players are. The
confirmed fixes of this branch should reach 1.20.1 users through it (BACKLOG "Backport the confirmed fixes to 1.20.1").

### Deviation: no Forge 1.21.1 build

The request asked for NeoForge and Forge (and Fabric if possible). For 1.21.1, Forge is nearly absent from packs (25
Modrinth packs against 2,111 NeoForge and 3,032 Fabric), so the second loader of this branch is Fabric: far more reach
for the same work. Forge stays first-class for 1.20.1. The cost of a Forge 1.21.1 module is in BACKLOG ("Forge 1.21.1
module").

## How to build several versions

Stonecutter on top of the current multiloader layout (`common`, one project per loader, platform interfaces loaded
with `ServiceLoader`):

- one build script per loader: ModDevGradle for NeoForge 1.21.1 and 26.x, ModDevGradle `legacyforge` for Forge 1.20.1
  (it supports Forge 1.17 to 1.20.1 only), Loom for Fabric;
- version specific code behind `//? if >=1.21.1 {` style comments, kept small by the platform interfaces;
- the official Stonecutter multiloader template (codeberg.org/stonecutter/template-multiloader) as reference.

The MultiLoader-Template alternative needs one branch per Minecraft version, which copies code between versions.

## Porting notes

- **1.20.1**: no data components. Capsule stores its state in `minecraft:custom_data` through `NBTHelper`, and the base
  color in `minecraft:dyed_color` (`MinecraftNBT`); on 1.20.1 both become plain item NBT (`getTag()`, `display.color`).
  Networking uses `SimpleChannel` instead of payloads, registries `DeferredRegister` from Forge, enchantments are
  classes instead of data driven (`recall` must become an `Enchantment` subclass again), recipes and loot tables use the
  older JSON formats (`item` + `nbt` results), and Java 17.
- **1.21.5+**: GameTest was rewritten as data driven `test_instance` registry entries with test environments and a new
  `/test` command; `CapsuleGameTests` and the loader registrations must be rewritten (the test bodies mostly survive).
- **1.21.11**: `ResourceLocation` is renamed `Identifier` (a plain replacement with Stonecutter).
- **26.x**: Java 25, unobfuscated game (no intermediary or reobfuscation, Parchment optional), NeoForge versions with
  four parts (`26.1.2.114`), Loom 1.18+ which needs Gradle running on Java 25.
- Fabric client GameTests (`fabric-client-gametest-api-v1`) exist from 1.21.4; on 1.21.1 the client smoke test of this
  branch (`scripts/client-smoke.sh`) fills that role.
