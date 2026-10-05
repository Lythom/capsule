# dev-1.21.1 plan

Inputs: issue triage (all 45 open issues, see `docs/ISSUE_TRIAGE.md` once consolidated) and state-of-the-art
research (`docs/RESEARCH.md`). Branch: `dev-1.21.1` (never push to `1.21.1`, it triggers the CircleCI release
pipeline). All commits authored **and** committed as `Lythom <contact@samuel-bouchet.fr>`, no `Co-Authored-By`.

## Key decisions

| Topic | Decision | Why |
|---|---|---|
| Build tool | Migrate NeoGradle 7.0 → **ModDevGradle 2.0.x** (`net.neoforged.moddev`), Gradle ≥ 8.10 (whatever the chosen MDG/Loom versions require) | MDG is what official MDKs and every multiloader template use; built-in `unitTest`, gameTestServer run, `legacyforge` plugin for a future Forge 1.20.1 build |
| Loaders for 1.21.1 | **NeoForge + Fabric** from one codebase, MultiLoader layout (`common` / `neoforge` / `fabric`) | On Modrinth, 1.21.1 packs are NeoForge 2111 / Fabric 3032 / **Forge 25** (CurseForge not measurable from here). See "Deviations from the request" |
| Abstraction | `common` compiled against vanilla (MDG `neoFormVersion`), loader glue behind small `capsule.platform.*` interfaces loaded with `java.util.ServiceLoader` (jaredlll08 MultiLoader-Template pattern). No Architectury API dependency | Explicit code, no extra library on NeoForge, same pattern scales to Forge 1.20.1 later. Fabric jar requires Fabric API (declared in `fabric.mod.json` and in publish metadata) |
| Config | Config definition behind a small platform interface in common; NeoForge implements it with `ModConfigSpec`; Fabric uses **Forge Config API Port** (declared dependency) or, if unavailable/unsuitable, a minimal TOML/JSON implementation. Same keys and file name on both loaders | Keeps `common` free of loader imports (S5) |
| GameTests packaging | GameTest classes live in a dedicated `gametest` source set / test-only module wired into dev runs, not in release jars | S4 forbids dev-only code in release jars |
| Multi-version (later) | Stonecutter on top of the multiloader layout; targets 1.20.1 (Forge + Fabric), 1.21.1 (NeoForge + Fabric), latest 26.x (NeoForge + Fabric) | Evidence in `docs/RESEARCH.md`; tracked in `docs/VERSIONS.md` + BACKLOG, **not implemented now** |
| Tests | 3 layers: JUnit (pure logic), **GameTests** in common (vanilla `GameTestHelper`) registered on both loaders and run headless by `runGameTestServer`-like tasks, **client visual smoke test** (real client under Xvfb + Mesa llvmpipe, automated screenshots) | Verified in this container: the 1.21.1 NeoForge client renders to the title screen under `xvfb-run` |

## Deviations from the request

- **Forge 1.21.1 is not built.** The request said "NeoForge, Forge, if possible Fabric; if no abstraction layer, NeoForge + Forge".
  An abstraction approach exists (MultiLoader layout), and for **1.21.1** Forge is nearly absent from modpacks
  (Modrinth: 25 Forge packs vs 2111 NeoForge and 3032 Fabric; ATM10, BMC5, Cobblemon packs are NeoForge/Fabric).
  Fabric therefore brings far more reach for the same cost. Forge stays first-class for **1.20.1** (Capsule's most
  downloaded version, 369k on Modrinth) in the multi-version phase. A costed BACKLOG entry describes how to add a
  Forge 1.21.1 module (ForgeGradle 6, MultiLoader-Template `1.21.1` branch) if the owner wants it anyway.
- **Fabric go/no-go**: if the Fabric GameTests and the Fabric production-jar smoke test are not green at the end of
  phase 5, the branch ships NeoForge only from the new layout and the Fabric work goes to BACKLOG with its status.

## Fabric risk list (phase 5)

| NeoForge feature used | Fabric plan |
|---|---|
| `Item#onEntityItemUpdate` (throw-to-deploy core mechanic), `onItemUseFirst` | common mixin on `ItemEntity#tick` calling a common hook; `UseBlockCallback` or `useOn` |
| Cancellable `EntityJoinLevelEvent`, `Level.restoringBlockSnapshots` (drop/XP suppression during capture) | common mixin on `ServerLevel#addFreshEntity` (or `Block#popResource` / `ExperienceOrb.award`) guarded by the capture flag |
| `BlockRenderDispatcher#tesselateWithAO` with `ModelData` | vanilla signature behind a client platform call |
| `RecipesUpdatedEvent` (prefab recipes for JEI) | `ClientPlayConnectionEvents`/recipe sync hook or mixin on `ClientPacketListener#handleUpdateRecipes` |
| `IItemHandler` capabilities (blueprint material sources) | prefer vanilla `Container`/`WorldlyContainer` in common; loader-specific storage lookup behind the platform interface (NeoForge capabilities / Fabric Transfer API) |
| `FluidType` client extensions (preview fluids) | `FluidRenderHandlerRegistry` behind a client platform call |
| `LivingEquipmentChangeEvent`, `LootTableLoadEvent`, `ClientChatReceivedEvent`, `RenderLevelStageEvent`, color handlers, item properties, BER, reload listeners, commands, payloads, creative tabs | direct Fabric API equivalents (`inventoryTick`, `LootTableEvents.MODIFY`, `ClientReceiveMessageEvents`, `WorldRenderEvents`, `ColorProviderRegistry`, `ItemProperties`, `BlockEntityRenderers`, `ResourceManagerHelper`, `CommandRegistrationCallback`, `PayloadTypeRegistry`, `ItemGroupEvents`) |
| `CommonHooks` place/break permission checks | NeoForge: events; Fabric: `PlayerBlockBreakEvents.BEFORE` probe or Common Protection API if available |
| `c:` convention tags in recipes | verify every recipe's tag ingredients resolve on both loaders (S6 test) |

## Phases

1. **Build migration** (NeoGradle → MDG, NeoForge 21.1.77 → latest 21.1.x, JEI → latest 19.x). Behavior unchanged.
   `neoforge.mods.toml` gets realistic version ranges (lower bound = built-against NeoForge minor, MC `[1.21.1,1.21.2)`).
2. **Test infrastructure**: gameTestServer run (exit code = failed tests), JUnit, baseline GameTests for the core
   mechanics (see S6), a `./gradlew` task chain usable in CI.
3. **Bug fixes**, one commit per issue (or tight group), each with a regression test that fails without the fix:
   - #125/#71-family: `resolvePath` portability check only on the relative part + `writeToFile`/`deleteTemplate` never crash the server
   - #122: experience orbs from captured furnaces (cancel `ExperienceOrb` during capture, clear `recipesUsed`)
   - #119: SecurityCraft owner check (reflection on `IOwnable`, refuse on error)
   - #116: deploy anchor on overridable blocks (snow layer, grass) – shared helper for preview + server
   - #115: block-entity NBT shared between template cache and deployed blocks (`.copy()`)
   - #109: `CapsuleLootEntry` registers its own `LootPoolEntryType` + codec
   - #100: `_uncommon_well.nbt` infested blocks + test "no bundled template contains `infested_*`"
   - #98: recall must not pick up a capsule before it had a chance to deploy
   - #90: recall enchant obtainable in survival (`#minecraft:in_enchanting_table`, `#minecraft:non_treasure`); dead configs removed or wired
   - #89: blind deploy one block too high (`Spacial.findBottomBlock`)
   - #91 (part 1): server-side validation of `CapsuleThrowQueryToServer` (instant flag, reach/position). Per-block `BreakEvent` for claim mods → BACKLOG (side effects on quest/XP/vein-miner mods, cost on large captures; to be designed as an opt-in or a per-chunk probe, with a decision for the playerless dispenser path)
   - #119: only if the reflected SecurityCraft 1.21.1 API names are verified against a real SecurityCraft 1.21.1 jar; "refuse on error" applies to SecurityCraft blocks only; otherwise BACKLOG
   - #70 (Sponge v3 `.schem`): BACKLOG (format support, not a regression)
   - #81/#117/#76/#94: preview rendering crash protection (per-block guards, wireframe fallback) – reported as **hardened**, not fixed (no reliable fail-before test; documented exception to S3)
   - #75: `undeployDelay` / activation timers based on `level.getGameTime()` instead of `player.tickCount`; legacy stored values must not block undeploy
   - #56: blocks without item (potted plants…) must not be free in blueprints
   - extra findings: N1 invalid template file name crashes login; N3a/N3b `excludedBlocks` tag entries; N5 regex `replaceAll(".nbt")`; reload listener registered only on the physical client (`CapsuleMod.java` `CapsuleForgeSubscriber`, `value = Dist.CLIENT`) so dedicated servers never refresh templates on `/reload`; delete dead `data/forge/tags/items/ingots/*`
   - Anything that turns out bigger or riskier than expected → BACKLOG with the analysis, not a half fix.
4. **Multiloader restructure**: `common` (all game logic, assets, data, GameTests), `neoforge` and `fabric` (glue only). `git mv` to keep history. No `net.neoforged`/`net.fabricmc` import in `common` (enforced by a build check).
5. **Fabric platform**: registries, networking (Fabric payload API), events (Fabric API callbacks; mixins only where no hook exists), item storage (Transfer API instead of `IItemHandler`), loot table modification, client hooks (world render, color handlers, item properties, BER), config, JEI plugin on Fabric. GameTests run on Fabric too (`fabric-gametest-api-v1`).
6. **Client visual smoke test**: dev-only harness (not shipped in release jars) that, when a system property is set, creates/loads a flat test world, runs a scripted scenario (give capsules of each state, open inventory, hold a linked capsule to show the preview, deploy/undeploy, capture base), saves screenshots to `build/screenshots/`, then quits. Wrapper script `scripts/client-smoke.sh <loader>` using `xvfb-run`. Fabric at least boots to a world with screenshots.
   The harness also asserts automatically: no missing model/texture warnings for the `capsule` namespace in the client log, JEI plugin loaded, no "missing texture" magenta/black pattern in capsule item slots.
7. **Mod interaction smoke test** (best effort): run the NeoForge gametest server with a few mods from issues on the runtime classpath (JEI, SecurityCraft, Waystones, Sophisticated Storage if resolvable from Modrinth maven) and record results. If practical, also boot the client with a small Modrinth modpack under Xvfb.
8. **CI + release**: `.circleci/config.yml` builds the new layout, runs unit + game tests, stores both jars; the whole workflow (build, `hold`, `publish`) is filtered to branch `1.21.1` only, so pushing `dev-1.21.1` triggers nothing. `BUILD_ID` comes from a Gradle property instead of `sed` on `build.gradle`. `publish.sh` publishes one jar per loader with loader + dependency metadata (Fabric API required on Fabric), dry-run verified on both jars. `CHANGELOG.md` entry.
9. **Docs**: `BACKLOG.md` (including: Forge 1.21.1 module cost, backport of confirmed fixes to the 1.20.1 branch, REI/EMI on Fabric, #91 claim events, #70), `docs/ISSUE_TRIAGE.md` (with ready-to-post summaries per issue; nothing is posted on GitHub), `docs/ISSUE_TRIAGE.md`, `docs/TESTING.md`, `docs/MANUAL_VALIDATION.md`, `docs/VERSIONS.md`, README update.

## Success conditions

- **S1 Git hygiene** – work only on `dev-1.21.1`, pushed to origin; `1.21.1` untouched; every commit author and committer `Lythom <contact@samuel-bouchet.fr>`, no `Co-Authored-By` trailer; commits are focused (build / one bug / restructure / docs) and each builds.
- **S2 Triage complete** – all 45 open issues listed in `docs/ISSUE_TRIAGE.md` with a classification (CONFIRMED-FIXED / CONFIRMED-BACKLOG / OBSOLETE / ENHANCEMENT / DOUBT) and justification; every ENHANCEMENT and DOUBT, plus every confirmed bug not fixed, has an entry in `BACKLOG.md`.
- **S3 Bug fixes proven** – each fixed bug has an automated regression test (GameTest or JUnit) that was observed failing before the fix and passing after (recorded in the commit message or triage doc). Documented exceptions: client-render hardening (#81 family), reported as "hardened". Fixes are minimal and do not change saved data formats.
- **S4 Builds** – `./gradlew build` from a clean clone succeeds and produces `Capsule-neoforge-1.21.1-<ver>.jar` and `Capsule-fabric-1.21.1-<ver>.jar` (exact naming may differ but loader + MC version must be explicit), each with correct metadata, embedding the common classes/resources, and without dev-only test harness or GameTest code.
- **S4b Production jars boot** – each **built release jar** (not the dev run) is started on a real dedicated server of its loader (Fabric: with Fabric API and declared deps), reaches "Done", loads the mod, and stops cleanly; ideally the GameTests run against it.
- **S5 Single codebase** – all gameplay logic, assets and data live in `common`; loader modules only contain platform glue; a build check fails if `common` imports `net.neoforged` or `net.fabricmc`.
- **S6 Mechanics covered by GameTests, green on both loaders** – at least: capture into empty capsule; deploy linked capsule; undeploy; excluded blocks not captured / overridable blocks replaced; blueprint deploy consuming materials (and refusing when missing); one-use/recovery capsule; reward template deploy; capture base as dispenser; main recipes (dye, upgrade, clear, recovery, blueprint) give expected outputs; plus the regression tests of S3; every bundled template (starters, loot, prefabs) deploys without error; every recipe loads and its tag ingredients are non-empty on each loader. The tasks exit non-zero on any failure.
- **S7 Visual validation** – the client smoke scenario runs unattended on NeoForge under Xvfb and produces screenshots in which a reviewer can see: capsule item sprites (empty, linked, deployed, one-use, blueprint, with colors), the capture marker block, the deploy preview, and a deployed structure. Fabric client reaches an in-game world with capsules rendered. Automatic assertions (log has no missing model/texture for `capsule`, JEI plugin loaded, no missing-texture pattern in item slots) make the run fail on regressions. Screenshots are inspected and findings recorded in `docs/MANUAL_VALIDATION.md`.
- **S8 Manual validation list** – `docs/MANUAL_VALIDATION.md` lists exactly what could not be validated automatically (e.g. multiplayer latency, third-party mods not tested, shaders), derived from `TEST.md`.
- **S9 CI/release ready** – CircleCI config and `publish.sh` updated for two jars and validated (`publish.sh --dry-run` on each jar; CircleCI YAML parses); publish only reachable from branch `1.21.1`. Nothing is published.
- **S10 Compatibility** – mod id, registry ids, data component names, recipe ids, config keys and template folders unchanged, so existing 1.21.1 worlds and configs keep working.
- **S10b Fabric/NeoForge parity** – every feature works on both loaders or the difference is documented in `docs/MANUAL_VALIDATION.md`/BACKLOG (JEI on Fabric optional; REI/EMI in BACKLOG).
- **S11 Docs** – `docs/TESTING.md` explains how to run every test layer locally (and in this container), `docs/VERSIONS.md` gives the target versions/loaders roadmap with evidence, README mentions loaders.
