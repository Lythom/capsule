# Minecraft versions and loaders

Which Minecraft versions and loaders Capsule should be built for, with the evidence behind the choice. CurseForge
numbers come from the CurseForge search page and the Modpack Index API (modpackindex.com, which indexes CurseForge and
Modrinth); Modrinth numbers from `docs/RESEARCH.md`. All queried 2026-10-05. A pack counts under every version and
loader it has ever had a file for, and CurseForge caps its counts at "10,000+".

## Evidence

CurseForge is where Capsule's players are: 9.4M of its ~9.8M all-time downloads.

| Build | CurseForge | Modrinth |
|---|---:|---:|
| 1.12.2 Forge | ~3.89M | 3,501 |
| 1.16.5 Forge | ~2.54M | 907 |
| 1.20.1 Forge | ~1.71M | 369,091 |
| 1.18.2 Forge | ~633K | 239 |
| 1.19 / 1.19.2 Forge | ~135K | 179 |
| 1.21.1 NeoForge (since 2026-03) | ~12K | 3,233 |
| 1.20.4 NeoForge | 803 | 106 |

Modpacks by version and loader. CurseForge columns from CurseForge search; "~" is estimated from the Modpack Index
count of CurseForge packs minus the other loaders.

| MC | CF Forge | CF NeoForge | CF Fabric | Modrinth Forge / NeoForge / Fabric | CF packs ≥10k downloads, updated in the last 6 months |
|---|---:|---:|---:|---|---:|
| 1.12.2 | ~29,000 | – | 22 | 191 / 0 / 3 | 96 |
| 1.16.5 | ~16,700 | 5 | 496 | 142 / 0 / 181 | 110 |
| 1.18.2 | 8,255 | – | 1,218 | 168 / 0 / 269 | 124 |
| 1.19.2 | ~12,000 | – | 2,382 | 363 / 1 / 629 | 155 |
| **1.20.1** | **~38,000** | 298 | **4,708** | 3,015 / 263 / 4,290 | **614** |
| **1.21.1** | 709 | **~11,000** | **3,002** | 25 / 2,111 / 3,032 | **409** |
| 1.21.4 | – | 148 | 582 | 13 / 75 / 1,825 | 56 |
| 1.21.11 | 96 | 305 | 1,033 | 19 / 82 / 2,834 | 94 |
| 26.1.2 | 47 | 234 | 500 | 7 / 65 / 1,313 | 88 |
| 26.2 | – | 144 | 625 | 3 / 52 / 1,400 | n/a |
| 26.3 | – | 22 | 142 | 2 / 10 / 373 | n/a |

- On CurseForge, 1.21.1 is a NeoForge version (about 3.7 packs per Fabric pack) and 1.20.1 a Forge version (about 8
  per Fabric pack). Fabric has about the same number of packs on both sites, so it matters more on Modrinth.
- Top 100 CurseForge packs by downloads, by their latest file: 1.20.1 Forge 19 and Fabric 6, 1.12.2 Forge 21,
  1.21.1 NeoForge 11 and Fabric 4, 1.16.5 Forge 11, 1.18.2 7, 1.19.2 6, 26.x 3.
- Content packs sit on 1.20.1 Forge (ATM9, BMC4, Prominence II on Fabric) and 1.21.1 NeoForge (ATM10 22.1M,
  Pixelmon 20.1M, StoneBlock 4, BMC5, Craftoria) or Fabric (Cobbleverse, Cobblemon packs). Forge 1.21.1 packs are
  small (largest ~300K downloads) and often also tagged NeoForge.
- 26.x is still small on CurseForge; ATM11 (NeoForge 26.1.2, 1.0M downloads) is the one large content pack. The 26.x
  Fabric packs are mostly performance and vanilla+ packs.
- Forge Config API Port, which the Fabric build needs, has 82.4M CurseForge and 67.3M Modrinth downloads and is in 13
  of the 14 largest (≥90 mods) of the 60 most downloaded 1.21.1 Fabric packs on Modrinth.

## Targets, in order

| Order | Target | Loaders | Status | Why |
|---|---|---|---|---|
| 1 | 1.21.1 | NeoForge + Fabric | this branch (`dev-1.21.1`) | ~11,000 NeoForge packs on CurseForge plus 2,111 on Modrinth; Fabric 3,002 CF + 3,032 Modrinth |
| 2 | 1.20.1 | Forge, then Fabric | to do (Stonecutter) | largest pack version (~38,000 Forge packs on CurseForge) and Capsule's most used live build (1.7M CF downloads); Fabric adds 4,708 CF + 4,290 Modrinth packs |
| 3 | latest 26.x | NeoForge + Fabric | to do (Stonecutter) | where new packs go once the content mods (Create, ATM11) have moved; still < 1,000 packs per loader |
| optional | 1.18.2, 1.19.2 | Forge | not planned | 10–15k CurseForge packs each, existing releases; same MDG legacyforge script as 1.20.1 if asked |
| — | 1.21.1 Forge | — | not built | see "Deviation" below |
| — | 1.16.5 and older | — | not built | big on CurseForge (1.12.2 and 1.16.5 are Capsule's top CF builds) but served by the existing releases; Java 8 and very different APIs |

1.21.1 comes first because the codebase was already on it; 1.20.1 next because it is where the players are. The
confirmed fixes of this branch should reach 1.20.1 users through it (BACKLOG "Backport the confirmed fixes to 1.20.1").

### Deviation: no Forge 1.21.1 build

The request asked for NeoForge and Forge (and Fabric if possible). For 1.21.1, Forge is a small minority of packs: 709
on CurseForge (many also tagged NeoForge, the largest ~300K downloads) and 25 on Modrinth, against ~11,000 + 2,111
NeoForge and 3,002 + 3,032 Fabric. So the second loader of this branch is Fabric: far more reach for the same work.
Forge stays first-class for 1.20.1. The cost of a Forge 1.21.1 module is in BACKLOG ("Forge 1.21.1 module").

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
