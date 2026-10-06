# Minecraft versions and loaders

Which Minecraft versions and loaders Capsule should be built for: where modpacks are today, whether or not Capsule is
already in them. Sources, all queried 2026-10-05:

- **Modrinth API** (official public API, 82 requests): modpacks per version and loader, and the ones still alive
  (project updated since 2026-04-05, with or without 10k downloads).
- **CurseForge search pages** (a few dozen page views): modpacks per version and loader. CurseForge caps counts at
  "10,000+" and blocks automated access, so it was not queried further.
- **What modders say**: NeoForge and Fabric announcements, linked below.

A pack counts under every version and loader it has ever had a file for, and Modrinth's update date is per project,
not per version: a pack now on 26.x still counts as "active" for the older versions it once supported.

## Evidence

Modpacks updated in the last 6 months, Modrinth (all / with ≥ 10k downloads), and CurseForge modpack counts:

| MC | Modrinth Forge | Modrinth NeoForge | Modrinth Fabric | Modrinth active ≥ 10k, all loaders | CurseForge Forge | CurseForge NeoForge | CurseForge Fabric |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1.12.2 | 58 / 20 | – | – | 20 | 10,000+ | – | 22 |
| 1.16.5 | 51 / 20 | – | 89 / 34 | 54 | 10,000+ | 5 | 496 |
| 1.18.2 | 39 / 22 | – | 100 / 44 | 66 | 8,255 | – | 1,218 |
| 1.19.2 | 77 / 26 | – | 140 / 58 | 84 | 10,000+ | – | 2,382 |
| **1.20.1** | **934 / 123** | 53 / 11 | **912 / 184** | **318** | **10,000+** | 298 | **4,708** |
| **1.21.1** | 10 / 4 | **1,291 / 91** | **1,124 / 231** | **326** | 709 | **10,000+** | **3,002** |
| 1.21.4 | 3 / 2 | 34 / 16 | 686 / 164 | 182 | – | 148 | 582 |
| 1.21.8 | 3 / 2 | 42 / 18 | 778 / 164 | 184 | – | 177 | 466 |
| 1.21.10 | 4 / 3 | 31 / 14 | 867 / 172 | 189 | – | 215 | 508 |
| 1.21.11 | 11 / 2 | 62 / 18 | 2,046 / 227 | 247 | 96 | 305 | 1,033 |
| 26.1.2 | 7 / 1 | 65 / 17 | 1,315 / 178 | 196 | 47 | 234 | 500 |
| 26.2 | 3 / 1 | 53 / 11 | 1,404 / 173 | 185 | – | 144 | 625 |
| 26.3 | 2 / 1 | 11 / 7 | 381 / 85 | 93 | – | 22 | 142 |

- **1.21.1 and 1.20.1 are where the living packs are**, about equal on Modrinth and both at the CurseForge cap for
  their main loader. 1.21.1 is NeoForge + Fabric (ATM10, Craftoria, StoneBlock 4, BMC5, Cobblemon packs), 1.20.1 is
  Forge + Fabric (ATM9, BMC4, Prominence II). Forge 1.21.1 and NeoForge 1.20.1 are marginal.
- **Newer 1.21.x and 26.x are Fabric-heavy** on Modrinth, mostly performance and vanilla+ packs; NeoForge content packs
  there are still few (ATM11, NeoForge 26.1.2, is in alpha).
- **1.12.2 to 1.19.2** keep many CurseForge packs from their time, but few are still updated.

What modders say:

- NeoForge, 26.1 release post (2026-03-24): "There are strong indications that 26.1 will be the next stable modding
  version, replacing 1.21.1, so I encourage all modders to port their mods to 26.1", flagged as a personal educated
  guess, not an official team statement (https://neoforged.net/news/26.1release/).
- 1.21.11 is the last obfuscated version and 26.1 the first unobfuscated one; Yarn stops at 1.21.11
  (https://fabricmc.net/2025/12/05/12111.html, https://fabricmc.net/2026/03/14/261.html). Porting work therefore
  concentrates on 26.1.x rather than on the 1.21.2–1.21.11 releases.
- Content mods are following: Create's Fabric port (Create Fly) and ATM11 have 26.1.2 builds.

## Targets, in order

| Order | Target | Loaders | Status | Why |
|---|---|---|---|---|
| 1 | 1.21.1 | NeoForge + Fabric | this branch (`dev-1.21.1`) | most living packs today (326 active ≥ 10k on Modrinth, 10,000+ NeoForge on CurseForge) |
| 2 | 1.20.1 | Forge + Fabric | to do (Stonecutter); bug fixes backported to Forge on `dev-1.20` (`docs/BACKPORTS.md`) | as many living packs as 1.21.1 on Modrinth and the largest Forge base on CurseForge |
| 3 | 26.1.2 + 26.2 | NeoForge + Fabric | to do (Stonecutter, later round) | 26.1.2: the likely next stable modding version (NeoForge), where ATM11 and the content mods are going; may overtake 1.20.1 once packs migrate. 26.2: asked directly by a user (owner's decision, 2026-10-06), as many active packs as 26.1.2 on Modrinth (185 against 196), expected small effort once 26.1 builds |
| — | 1.21.2–1.21.11, 26.3+ | — | not planned | Fabric performance packs mostly; follow the next stable version instead of each release |
| — | 1.21.1 Forge | — | dropped | owner's decision (2026-10-06), see "Deviation" below |
| — | 1.19.2 and older | — | not built | few packs still updated; existing Capsule releases cover them |

### Deviation: no Forge 1.21.1 build

The request asked for NeoForge and Forge (and Fabric if possible). For 1.21.1, Forge is a small minority of packs: 709
on CurseForge and 25 on Modrinth (10 updated in the last 6 months), against 10,000+ and 1,291 NeoForge, 3,002 and 1,124
Fabric. So the second loader of this branch is Fabric: far more reach for the same work. Forge stays first-class for
1.20.1. The owner dropped Forge 1.21.1 on 2026-10-06 (BACKLOG, "Decided, not doing"). Cost if it ever comes back: one
more platform module built with ForgeGradle 6 as in the MultiLoader-Template `1.21.1` branch (ModDevGradle legacyforge
stops at 1.20.1), Forge implementations of the platform interfaces, and its own GameTest and production-jar runs.

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
