# dev-1.21.1 plan

Inputs: issue triage (all 45 open issues, see `docs/ISSUE_TRIAGE.md` once consolidated) and state-of-the-art
research (`docs/RESEARCH.md`). Branch: `dev-1.21.1` (never push to `1.21.1`, it triggers the CircleCI release
pipeline). All commits authored **and** committed as `Lythom <contact@samuel-bouchet.fr>`, no `Co-Authored-By`.

## Key decisions

| Topic | Decision | Why |
|---|---|---|
| Build tool | Migrate NeoGradle 7.0 → **ModDevGradle 2.0.x** (`net.neoforged.moddev`), Gradle ≥ 8.10 (whatever the chosen MDG/Loom versions require) | MDG is what official MDKs and every multiloader template use; built-in `unitTest`, gameTestServer run, `legacyforge` plugin for a future Forge 1.20.1 build |
| Loaders for 1.21.1 | **NeoForge + Fabric** from one codebase, MultiLoader layout (`common` / `neoforge` / `fabric`) | On Modrinth, 1.21.1 packs are NeoForge 2111 / Fabric 3032 / **Forge 25**. Forge 1.21.1 is not worth a third platform module now (→ BACKLOG). Architectury API dropped Forge on 1.21 anyway |
| Abstraction | `common` compiled against vanilla (MDG `neoFormVersion`), loader glue behind small `capsule.platform.*` interfaces loaded with `java.util.ServiceLoader` (jaredlll08 MultiLoader-Template pattern). No Architectury API runtime dependency | Zero extra runtime dependency for players, explicit code, same pattern scales to Forge 1.20.1 later |
| Config on Fabric | Keep NeoForge `ModConfigSpec` code in common if possible through **Forge Config API Port** (Fuzs) on Fabric; otherwise a minimal platform config interface | One config definition, same TOML file layout on both loaders |
| Multi-version (later) | Stonecutter on top of the multiloader layout; targets 1.20.1 (Forge + Fabric), 1.21.1 (NeoForge + Fabric), latest 26.x (NeoForge + Fabric) | Evidence in `docs/RESEARCH.md`; tracked in `docs/VERSIONS.md` + BACKLOG, **not implemented now** |
| Tests | 3 layers: JUnit (pure logic), **GameTests** in common (vanilla `GameTestHelper`) registered on both loaders and run headless by `runGameTestServer`-like tasks, **client visual smoke test** (real client under Xvfb + Mesa llvmpipe, automated screenshots) | Verified in this container: the 1.21.1 NeoForge client renders to the title screen under `xvfb-run` |

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
   - #91: protection hardening – server-side validation of `CapsuleThrowQueryToServer` (instant flag, reach), fire `BreakEvent` (or platform equivalent) per captured block for claim mods
   - #81/#117/#76/#94: preview rendering crash protection (per-block guards, wireframe fallback)
   - #75: `undeployDelay` / activation timers based on `level.getGameTime()` instead of `player.tickCount`
   - #56: blocks without item (potted plants…) must not be free in blueprints
   - extra findings: N1 invalid template file name crashes login; N3a/N3b `excludedBlocks` tag entries; N5 regex `replaceAll(".nbt")`; reload listener registered only on the physical client (`CapsuleMod.java` `CapsuleForgeSubscriber`, `value = Dist.CLIENT`) so dedicated servers never refresh templates on `/reload`; delete dead `data/forge/tags/items/ingots/*`
   - Anything that turns out bigger or riskier than expected → BACKLOG with the analysis, not a half fix.
4. **Multiloader restructure**: `common` (all game logic, assets, data, GameTests), `neoforge` and `fabric` (glue only). `git mv` to keep history. No `net.neoforged`/`net.fabricmc` import in `common` (enforced by a build check).
5. **Fabric platform**: registries, networking (Fabric payload API), events (Fabric API callbacks; mixins only where no hook exists), item storage (Transfer API instead of `IItemHandler`), loot table modification, client hooks (world render, color handlers, item properties, BER), config, JEI plugin on Fabric. GameTests run on Fabric too (`fabric-gametest-api-v1`).
6. **Client visual smoke test**: dev-only harness (not shipped in release jars) that, when a system property is set, creates/loads a flat test world, runs a scripted scenario (give capsules of each state, open inventory, hold a linked capsule to show the preview, deploy/undeploy, capture base), saves screenshots to `build/screenshots/`, then quits. Wrapper script `scripts/client-smoke.sh <loader>` using `xvfb-run`. Fabric at least boots to a world with screenshots.
7. **Mod interaction smoke test** (best effort): run the NeoForge gametest server with a few mods from issues on the runtime classpath (JEI, SecurityCraft, Waystones, Sophisticated Storage if resolvable from Modrinth maven) and record results.
8. **CI + release**: `.circleci/config.yml` builds the new layout, runs unit + game tests, stores both jars; `publish.sh` publishes one jar per loader (dry-run verified). `CHANGELOG.md` entry.
9. **Docs**: `BACKLOG.md`, `docs/ISSUE_TRIAGE.md`, `docs/TESTING.md`, `docs/MANUAL_VALIDATION.md`, `docs/VERSIONS.md`, README update.

## Success conditions

- **S1 Git hygiene** – work only on `dev-1.21.1`, pushed to origin; `1.21.1` untouched; every commit author and committer `Lythom <contact@samuel-bouchet.fr>`, no `Co-Authored-By` trailer; commits are focused (build / one bug / restructure / docs) and each builds.
- **S2 Triage complete** – all 45 open issues listed in `docs/ISSUE_TRIAGE.md` with a classification (CONFIRMED-FIXED / CONFIRMED-BACKLOG / OBSOLETE / ENHANCEMENT / DOUBT) and justification; every ENHANCEMENT and DOUBT, plus every confirmed bug not fixed, has an entry in `BACKLOG.md`.
- **S3 Bug fixes proven** – each fixed bug has an automated regression test (GameTest or JUnit) that was observed failing before the fix and passing after (recorded in the commit message or triage doc). Fixes are minimal and do not change saved data formats.
- **S4 Builds** – `./gradlew build` from a clean clone succeeds and produces `Capsule-neoforge-1.21.1-<ver>.jar` and `Capsule-fabric-1.21.1-<ver>.jar` (exact naming may differ but loader + MC version must be explicit), each with correct metadata, embedding the common classes/resources, and without dev-only test harness code.
- **S5 Single codebase** – all gameplay logic, assets and data live in `common`; loader modules only contain platform glue; a build check fails if `common` imports `net.neoforged` or `net.fabricmc`.
- **S6 Mechanics covered by GameTests, green on both loaders** – at least: capture into empty capsule; deploy linked capsule; undeploy; excluded blocks not captured / overridable blocks replaced; blueprint deploy consuming materials (and refusing when missing); one-use/recovery capsule; reward template deploy; capture base as dispenser; main recipes (dye, upgrade, clear, recovery, blueprint) give expected outputs; plus the regression tests of S3. The tasks exit non-zero on any failure.
- **S7 Visual validation** – the client smoke scenario runs unattended on NeoForge under Xvfb and produces screenshots in which a reviewer can see: capsule item sprites (empty, linked, deployed, one-use, blueprint, with colors), the capture marker block, the deploy preview, and a deployed structure. Fabric client reaches an in-game world with capsules rendered. Screenshots are inspected and findings recorded in `docs/MANUAL_VALIDATION.md`.
- **S8 Manual validation list** – `docs/MANUAL_VALIDATION.md` lists exactly what could not be validated automatically (e.g. multiplayer latency, third-party mods not tested, shaders), derived from `TEST.md`.
- **S9 CI/release ready** – CircleCI config and `publish.sh` updated for two jars and validated (`publish.sh --dry-run` on each jar; CircleCI YAML parses). Nothing is published.
- **S10 Compatibility** – mod id, registry ids, data component names, recipe ids, config keys and template folders unchanged, so existing 1.21.1 worlds and configs keep working.
- **S11 Docs** – `docs/TESTING.md` explains how to run every test layer locally (and in this container), `docs/VERSIONS.md` gives the target versions/loaders roadmap with evidence, README mentions loaders.
