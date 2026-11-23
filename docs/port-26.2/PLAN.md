# Port plan: Capsule 1.21.1 → Minecraft 26.2, then 26.3

Branches: `dev-1.26.2` for phases 0 to 9, then `dev-1.26.3` (created from `dev-1.26.2` once 26.2 is done) for phase
10. CircleCI only builds branches matching `/[0-9]+\..*/` (`.circleci/config.yml`), and CircleCI anchors that pattern
on the whole branch name, so the `dev-*` branches are not built. Push to a `26.2` or `26.3` branch only once the
definition of done (§8) is met: those branches are built, and published after the manual approval of the `hold` job.

This plan is written for the person or model doing the implementation. Read §1 to §4 before touching code. The
detailed, per-API instructions live in `inventory/` (§2): this file orders the work, settles conflicts between the
inventories and lists what has to be retested.

## 0. Rules for the implementation

- Port **directly** to 26.2, then 26.3 (phase 10). Never build an intermediate Minecraft version. The inventories describe every vanilla
  change between 1.21.1 and 26.2, version by version; apply the **final 26.2 form** of each API (§3).
- Commits: author `Lythom <contact@samuel-bouchet.fr>`, no `Co-Authored-By` or other attribution line. English, in the
  style of the existing history: an imperative subject, then a body that explains why and what was checked. One commit
  per phase at least (§5), more if a phase is long. Push to `dev-1.26.2` only.
- Line numbers in the inventories are those of commit `5cebb7c` (1.21.1). They are hints: find the code with `grep`,
  and check that the line says what the inventory says before changing it. A few are wrong.
- Items marked **VERIFY**, **UNVERIFIED** or "verify" were not checked against the 26.2 jars. Check them before
  applying (§4). Section `26.2.md` and most of `loaders-build.md` were checked with `javap` on the real 26.2 jars.
- Do not "improve" behavior during the port. A behavior change needs its own commit and a CHANGELOG line. The
  exceptions decided here are in §1.
- Keep the docs true: `docs/TESTING.md`, `docs/RECIPES.md`, `README.md`, `CHANGELOG.md` and `BACKLOG.md` must describe
  26.2 at the end (phase 9).

## 1. Decisions

Proposed in this plan. Change them only after asking the maintainer.

| # | Decision | Why |
|---|---|---|
| D1 | Target **26.2** (released 2026-06-16) first, then **26.3** (released 2026-09-15) right after, in phase 10. 26.4 is in snapshots. | A user asked for 26.2. The 26.3 step is about 4–5 days (`inventory/26.3.md`), so both are released together or one after the other. |
| D2 | One toolchain for 26.2 and 26.3, the newest that works: **Gradle 9.9.2**, ModDevGradle **2.0.148**, Fabric Loom **`net.fabricmc.fabric-loom` 1.19.4** (the non-remapping plugin), Java 25, Fabric Loader **0.20.1**. 26.2: NeoForge **26.2.0.95**, NeoForm **26.2-2**, Fabric API **0.161.0+26.2**. 26.3: NeoForge **26.3.0.115**, NeoForm **26.3-1**, Fabric API: the newest *release* for 26.3 (0.166.2+26.3 on 2026-11-23; 0.167.0 is a beta). Parchment is removed: there is none for 26.x, and the game ships Mojang names. | Built for real on a skeleton shaped like Capsule (`toolchain-trial/`, results in `inventory/26.3-loaders-build.md` §2): 26.2 and 26.3 build with no deprecation warning, and the 26.3 GameTest servers run. Loom 1.19 needs Gradle ≥ 9.8, Loom 1.20 (alpha) needs 9.9. Fabric's own example uses `1.19-SNAPSHOT`, which is older than 1.19.4. |
| D3 | Since 1.21.2 the server no longer sends recipes to the client, so the creative tab, the recipe viewers and the prefab recipes (`PrefabsBlueprintAggregatorRecipe`) lose their client-side data. That migration is not optional; the decision is **how**: through the **loader recipe-sync APIs** (NeoForge `OnDatapackSyncEvent#sendRecipes(RecipeType.CRAFTING)` + `RecipesReceivedEvent`; Fabric `RecipeSynchronization` + `ClientRecipeSynchronizedEvent`), not through a Capsule payload. Fabric's `ClientPacketListenerMixin` is deleted. | Both loaders provide it, it is less code than a payload of our own, and JEI/REI read the same synced recipes. Details: `1.21.2.md` §6.1, `loaders-build.md` §3.1, §3.2, §4.1, §4.2, §4.8. |
| D4 | **EMI is dropped** on this branch: no EMI build exists for 26.x. Keep `common/.../plugins/emi/` and `clientsmoke/EmiProbe.java` but exclude them from compilation (a `sourceSets` exclude with a comment), and remove `emi` from `-PrecipeViewer` and the scripts. | It can neither be compiled nor tested. Bring it back when EMI publishes 26.x. |
| D5 | The known-incompatibility mods without a 26.2 build (Refined Storage, Mekanism, Immersive Engineering, Super Factory Manager) leave `-Pincompat`. Their test classes are excluded from compilation (comment), and their `gradle.properties` entries are deleted. Corail Tombstone stays (it has a 26.2 build). | §6. |
| D6 | `CapsuleSavedData` moves to the server-wide storage (`MinecraftServer#getDataStorage()`), id `capsule:capsule_data`. No migration of the old per-dimension `capsuleData.dat` is needed. | 26.1 moved saved data files anyway (`26.1.md` §10.1). The counter only seeds blueprint names, and `StructureSaver#getBlueprintUniqueName` skips names already taken, so a counter restarting at 0 cannot overwrite a template. |
| D7 | **No migration** of config files written by an older Capsule (`config/capsule/prefab_blueprint_recipe.json` with the old ingredient format and `show_in_tooltip`, `blueprint_whitelist.json`, starters, loot templates). A config file that does not parse is reported: one server log error naming the file, what failed and that deleting it restores the default, and the feature that depends on it is skipped. No crash, no silent loss. What must be right is the JSON **bundled** with Capsule (`data/capsule/initialconfig/**`, recipes, advancements): a GameTest parses every bundled file (phase 8). | Capsule 10.x for 26.x is a new major version: there is no "previous" config to carry over (`BACKLOG.md`, "Decided, not doing": "Updated default templates never reach existing installs: fine, users delete the config folder"). The legacy-format branch of `Blueprint.java` (the "uses legacy format" warning, line 255) becomes this error. |
| D8 | **Done on branch `1.21.1`** (commit `5002f14`): the item predicates of the three recipe advancements (`data/capsule/advancement/recipes/tools/{captureblock,capsule,capsule_wood}.json`) were `{"item": …}` / `{"tag": …}`. They are now `{"items": "minecraft:compass"}` / `{"items": "#c:ender_pearls"}` / `{"items": "#minecraft:planks"}` inside the `inventory_changed` `items` list. The GameTest `RecipeTests#recipeAdvancementsWaitForTheirItem` checks that each criterion matches its item and not dirt. The port takes both from there; 26.3 also renames `recipe_unlocked`'s `"recipe"` to `"recipes"` (phase 10). | An item predicate reads `items` and ignores unknown keys, so the old predicates had no condition: the recipes were unlocked by the first item picked up. The test failed before the fix and passes after (109 NeoForge, 107 Fabric GameTests). |
| D9 | `capsule_version=10.0` for 26.x (jars `Capsule-<loader>-26.2-10.0.<build>.jar`, then `26.3-10.0.x`). The CHANGELOG entry is `26.2-10.0.x`. | A new major version goes with the new Minecraft numbering, and states that 10.x does not carry configs over (D7). |

## 2. The inventories

`inventory/` holds one file per primer slice. Each file reads a NeoForged primer in full and lists the Capsule code it
affects, with `path:line`, the old and new API and a snippet of the new form. Each file ends with the primer sections
judged not relevant. The primers are at
`https://github.com/neoforged/.github/tree/main/primers/<version>/index.md`.

| File | Covers |
|---|---|
| `inventory/1.21.2.md` | 1.21.1 → 1.21.2/1.21.3: recipes rewritten and no longer synced, ingredient JSON, `setId` on item and block properties, `InteractionResult`, enchantable, `neighborChanged(Orientation)`, level heights, `EntitySpawnReason`, registry accessors |
| `inventory/1.21.4-1.21.5.md` | 1.21.4: client item definitions `assets/capsule/items/*.json`, range select property, tint sources. 1.21.5: NBT getters returning `Optional`, GameTest overhaul, `TooltipDisplay`, `SavedDataType`, render pipelines, `BlockStateModel`, block entity removal |
| `inventory/1.21.6-1.21.10.md` | 1.21.6: `ValueInput`/`ValueOutput`, GUI, render types, `ServerPlayer#level()`. 1.21.9: block entity renderer render states, input events, `NameAndId`, `isClientSide()`, reload listeners. 1.21.7, 1.21.8, 1.21.10: nothing |
| `inventory/1.21.11.md` | `ResourceLocation` → `Identifier`, `ResourceKey#identifier()`, package moves, permissions, game rules, line width, gizmos, `FakeWorld` time access |
| `inventory/26.1.md` | Java 25 and the unobfuscated game, recipe serializer records, `ItemStackTemplate`, dye component, loot types, world clocks, saved data storage, block model rendering, `GuiGraphicsExtractor`, `FakeWorld`/`LevelAccessor`, small renames |
| `inventory/26.2.md` | `Gui`/`Hud`, `MultiBufferSource` removed (submit nodes), `ShapeRenderer` removed, pipeline API (bind groups, reversed depth), `EntityTypes`, public `BlockEntityType` constructor, `EntitySpawnRequest`, beds without block entity, pack format. Checked against the 26.2 jars |
| `inventory/loaders-build.md` | Target versions, the build file by file, NeoForge 21.1 → 26.2 and Fabric API changes in the loader glue, GameTest registration per loader, CI, publishing, scripts. Checked against the NeoForge 26.2.0.95 sources and Fabric API 0.161.0. Its tooling versions (Gradle 9.8.2/Loom 1.19.4 vs 9.7.1/1.18.4) are superseded by D2 |
| `inventory/26.3.md` | 26.2 → 26.3 (phase 10): SDL instead of GLFW, `com.mojang.renderpearl`, order-independent transparency (a crash without OIT pipelines), loot containers and number providers, `recipe_unlocked` `recipes`, recipes as a registry, pack format, whitelist, beds. Checked with javap against the 26.3 jar |
| `inventory/26.3-loaders-build.md` | 26.3 versions, the toolchain trial (§2), NeoForge 26.2 → 26.3 (`ModConfig.Type` `LOCAL`/`SYNCED`, `neoforge.items` removed, render hooks and Improved Transparency) and Fabric API 0.161 → 0.167 (additions only) |
| `toolchain-trial/` | The skeleton the toolchain was built with: Capsule's layout (`common` on NeoForm, `neoforge` with ModDevGradle, `fabric` with non-remapping Loom, `buildSrc` conventions). A reference for the phase 0 build files |
| `central-mirror.init.gradle` | Only for sandboxes where Maven Central answers 429 to Gradle: `./gradlew --init-script docs/port-26.2/central-mirror.init.gradle …` points Central at Google's mirror. Not part of the build |

The files mention "other agents": they were written in parallel, one per slice. Where a file says a topic belongs to
another agent, look for it in the file of the slice in the table above.

## 3. Final form per area (which inventory wins)

Several APIs changed more than once between 1.21.1 and 26.2. **The latest Minecraft version wins**: earlier sections
explain the intent and the intermediate steps, but do not write their code. The table names, per area, the sections
that give the final form, then the ones to read for context only.

| Area | Final form | Context only (do not apply their code) |
|---|---|---|
| Build, Java 25, mappings, access transformer / widener header, mixins | `loaders-build.md` §1–§2, `26.1.md` §1 | — |
| `ResourceLocation` → `Identifier`, `ResourceKey#identifier()`, `Util`/`FileUtil` packages, entity subpackages | `1.21.11.md` §1–§5 (sed commands given) | — |
| Item / block properties `setId`, `useBlockDescriptionPrefix` | `1.21.2.md` §2 | — |
| `BlockEntityType` construction | `26.2.md` §11 (the constructor is public again: delete the AT/AW entries) | `1.21.2.md` §3 |
| `Item#use`, enchantability, `inventoryTick`, tooltips, `DyedItemColor` | `1.21.2.md` §1, §5; `1.21.4-1.21.5.md` §2.3, §2.5 | — |
| Recipes (classes, serializers, `assemble`, results) | `26.1.md` §5–§8 | `1.21.2.md` §6.2, §6.4–§6.6 (for what was removed and why) |
| Ingredient JSON (recipe files, prefab JSON + D7 migration) | `1.21.2.md` §6.3; `1.21.4-1.21.5.md` §2.5 (`show_in_tooltip`) | — |
| Recipes reaching the client | D3: `loaders-build.md` §3.1, §3.2, §4.1, §4.2, §4.8 | `1.21.2.md` §6.1 (Capsule payload option: not chosen) |
| NBT getters (`getIntOr`, `getCompoundOrEmpty`…) | `1.21.4-1.21.5.md` §2.6 | — |
| Block entity and entity save/load (`BlockEntityCapture`, `CapsuleTemplate`, `Capsule.thrower`) | `1.21.6-1.21.10.md` §1.5, then `26.2.md` §12 (`EntitySpawnRequest`) | `1.21.4-1.21.5.md` §2.6 for those sites, `1.21.2.md` §8 |
| Saved data | D6 + `26.1.md` §10.1 | `1.21.4-1.21.5.md` §2.7, `1.21.11.md` §15 |
| Game rules, world creation in tests | `1.21.11.md` §13 + `26.1.md` §10.3, §9, §10.2 | `1.21.2.md` §15 |
| Loot | `26.1.md` §2 | `1.21.2.md` §17 |
| Commands and permissions | `1.21.11.md` §12; `1.21.4-1.21.5.md` §2.12 (`ClickEvent`/`HoverEvent`); `26.1.md` §12.4 (`ItemInput`) | — |
| Player names and profiles | `1.21.6-1.21.10.md` §4.8 (`NameAndId`), `loaders-build.md` (Common Protection API 2.0) | — |
| Client item models, `capsule:state` property, colors | `1.21.4-1.21.5.md` §1.2–§1.4 + `loaders-build.md` §3.2, §4.2 (registration events) | — |
| Ghost preview, capture animation, line boxes, block entity renderer: **all world rendering** | Hook: **extract + submit** (NeoForge `ExtractLevelRenderStateEvent` + `SubmitCustomGeometryEvent`, Fabric `LevelRenderEvents.END_EXTRACTION` + `COLLECT_SUBMITS`), never `AfterTranslucentBlocks` / `AFTER_TRANSLUCENT_TERRAIN` nor direct drawing (`26.2.md` §3 path B): in 26.3 those do not run when Improved Transparency is on (`26.3-loaders-build.md` §3). Then `26.2.md` §3–§8 (submit nodes, `submitShapeOutline`, pipelines with bind groups and **reversed depth**), `26.1.md` §11.2, §11.3, §11.5, §11.6, §13.1 (block tessellation, quads, fluid models, tint getter, `FakeWorld`), `1.21.6-1.21.10.md` §4.1 (render state of `CaptureBER`), §4.3 (atlas) | `1.21.2.md` §10–§11, `1.21.4-1.21.5.md` §2.8–§2.9, `1.21.6-1.21.10.md` §1.2–§1.4, §1.10, §4.2, §4.12, `1.21.11.md` §6–§10, `26.1.md` §11.7 |
| GUI (`LabelGui`, test screens) | `26.2.md` §2 (`Minecraft.gui`), `26.1.md` §11.4 (`GuiGraphicsExtractor`), `1.21.6-1.21.10.md` §4.6 (input events), §1.1 (alpha in colors) | — |
| GameTests (framework, registration, environments) | `1.21.4-1.21.5.md` §2.4 + `26.1.md` §12.6 + `loaders-build.md` §3.9, §4.9 | — |
| Loader glue (events, payloads, config, creative tabs, reload listeners, capabilities, fluids) | `loaders-build.md` §3–§4 | `1.21.6-1.21.10.md` §4.9, `1.21.2.md` §13 |
| Data and resources (`pack.mcmeta`, whitelist) | `26.2.md` §1, §13 | pack sections of the other files |
| CI, publishing, scripts | `loaders-build.md` §5 | — |

## 4. Checking an API against 26.2

The game is no longer obfuscated, so the real classes can be read directly:

- **Compiler first.** Most VERIFY items resolve themselves at compile time. Do not guess a signature: read it.
- **Decompiled sources:** ModDevGradle produces them for the IDE. In IntelliJ, open the `net.minecraft` class from the
  `neoforge` or `common` module.
- **javap without an IDE:** get the 26.2 client jar from Mojang's manifest
  (`https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` → version `26.2` → `downloads.client.url`), then
  `javap -cp client.jar -p net.minecraft.client.renderer.RenderPipelines`. For NeoForge and Fabric API classes, use
  their jars from the Gradle cache (`~/.gradle/caches`).
- **The primers** (§2) explain the intent of a change when a signature alone does not.

## 5. Phases

Effort is for one person who knows the code: about 19–27 days for 26.2 (phases 0 to 9), then 4–5 days for 26.3
(phase 10). Most phases do not compile on their own: `common` only compiles once
phases 1 to 6 are done. Use `./gradlew :common:compileJava` as a to-do list, package by package, and commit at the end
of each phase even if the build is still red. Phase 7 is the first that should give a green `./gradlew build`.

### Phase 0: build on 26.2 (1–2 days)

`loaders-build.md` §1–§2 and §2.10–§2.13; `26.1.md` §1; decisions D2, D4, D5.

1. `gradle.properties`: versions and Modrinth ids from `loaders-build.md` §1 and §2.1, `capsule_version=10.0` (D9).
   Delete Parchment, `emi_version` and the D5 mods.
2. Gradle wrapper 9.9.2 (`./gradlew wrapper --gradle-version 9.9.2`, so the wrapper jar and scripts follow). Root
   `build.gradle`: `net.fabricmc.fabric-loom` 1.19.4 instead of `net.fabricmc.fabric-loom-remap` 1.18.2 (D2).
   `buildSrc` conventions: Java 25 toolchain, no Parchment. `toolchain-trial/` has build files that work with these
   versions; copy from them rather than guess.
3. `common/build.gradle`: NeoForm 26.2-2; JEI/REI 26.2 artifacts; EMI excluded (D4); claim mods at their 26.2 ids.
4. `neoforge/build.gradle`: NeoForge 26.2.0.95; `gametest*` dependencies at their 26.2 ids; D5 mods removed along with
   their test classes (exclude plus comment).
5. `fabric/build.gradle`: no `mappings`, no Parchment repository; every `mod*` configuration becomes its plain form;
   `RemapJarTask` becomes `Jar`. Forge Config API Port 26.2.1 (`rSd3GiG8`); Common Protection API 2.0.0.
6. `neoforge.mods.toml`, `fabric.mod.json`: version ranges (Minecraft 26.2, NeoForge 26.2, Java 25, Fabric Loader
   0.20, Fabric API). `capsule.mixins.json`: `JAVA_25`; remove `ClientPacketListenerMixin` (D3).
7. `capsule.accesswidener`: header with `official` names. Remove the `RenderType`/`CompositeState`/`BlockEntityType`
   entries from it and from `common/accesstransformer.cfg`: their targets have changed (§3). Add back, in phase 6,
   only what the new code really needs.
8. `pack.mcmeta`: `26.2.md` §1.

Gate: `./gradlew :common:compileJava --dry-run` resolves every dependency, and `./gradlew :common:compileJava` runs
(compile errors are expected). Commit: "Build against Minecraft 26.2".

### Phase 1: mechanical renames, all source sets (0.5–1 day)

Do these with `sed` and the commands the inventories give, then check the diff. Include `common/src/gametest` and
`common/src/test`, and both loaders.

- `Identifier`, `IdentifierException`, `isAllowedInIdentifier`, `ResourceKey#identifier()` (not on `TagKey`):
  `1.21.11.md` §1–§2.
- Packages: `net.minecraft.util.Util`, `net.minecraft.util.FileUtil`, `Painting`, `ChestBoat`; remove
  `MethodsReturnNonnullByDefault`: `1.21.11.md` §3–§5.
- `isClientSide()`, `ServerPlayer#level()`, `getCommandSenderWorld()` → `level()`, `Entity#getServer()` →
  `level().getServer()`: `1.21.6-1.21.10.md` §1.6, §1.7, §4.7, §4.10.
- `getMinY`/`getMaxY` (the maximum is one lower now), `EntitySpawnReason`, registry accessors (`getValue`…):
  `1.21.2.md` §7–§9.
- `snapTo`, `updateNeighborsAt`, `hasInfiniteMaterials`, `Inventory` accessors: `1.21.4-1.21.5.md` §2.3, §2.10.
- `FarmlandBlock`, `ContainerInput`, `typeHolder`, `ChunkPos` records, `getRandom()`: `26.1.md` §4, §12.1–§12.5.
- `mainCamera()`, `sectionRenderDispatcher()` (delete the unused `dispatcher` field), `EntityTypes`, dye and glass
  collections in tests: `26.2.md` §6, §10, §11.
- `Camera#position()`: `1.21.11.md` §11.

Commit: "Apply the mechanical renames up to Minecraft 26.2".

### Phase 2: items, blocks, registration (1.5–2 days)

`1.21.2.md` §1–§5; `1.21.4-1.21.5.md` §1.6, §1.7, §2.2, §2.3, §2.5, §2.10, §2.12; `26.2.md` §11, §13.

- `setId` on the properties of the capsule item, the capture base block and its block item
  (`useBlockDescriptionPrefix()`); `BlockEntityType` through its public constructor.
- `CapsuleItem#use` returns `InteractionResult`; `.enchantable(5)`; the one-use check moves to `isPrimaryItemFor`.
- `BlockCapsuleMarker#neighborChanged` with `Orientation`, **with `@Override`**: today it has none, and the new
  signature would otherwise silently stop being called.
- `inventoryTick`, `appendHoverText` with `TooltipDisplay`, the hidden "Dyed" line (`TOOLTIP_DISPLAY` component),
  `saveToItem`, `getCloneItemStack`, `Clearable`/`preRemoveSideEffects`, `ClickEvent`/`HoverEvent` records.

Commit: "Port items, blocks and registration to 26.2".

### Phase 3: NBT, block entity and entity data, saved data (2–3 days)

`1.21.4-1.21.5.md` §2.6 (132 call lines, `getXOr` rules, UUIDs through `UUIDUtil.CODEC`, `TagParser`);
`1.21.6-1.21.10.md` §1.5; `26.2.md` §12; D6.

- `CapsuleTemplate` is a copy of vanilla `StructureTemplate`. Compare it with the 26.2 `StructureTemplate`
  (decompiled, §4) method by method: copy the vanilla changes where Capsule did not diverge, and keep Capsule's
  differences (schematic formats, exclusions, `DataFixers` upgrades at lines 860–966).
- Data saved by 1.21.1 must still load: capsule items (`minecraft:custom_data` keys read by `NBTHelper`), capture
  base data (`placer` UUID), templates `.nbt` and schematics. Templates are upgraded by
  `DataFixTypes.STRUCTURE.updateToCurrentVersion` (`CapsuleTemplateManager:164`), schematics by
  `CapsuleTemplate#updateToCurrentVersion` (`CapsuleTemplate:940-966`). Keep both: they are what lets a template saved
  by 1.21.1 (or by 1.12) deploy on 26.2.

Commit: "Port NBT, block entity and template data to 26.2".

### Phase 4: recipes (2–3 days)

`26.1.md` §5–§8 (final form); `1.21.2.md` §6 (context, ingredient JSON); `1.21.4-1.21.5.md` §1.8, §2.5; D3, D7.

- The 8 recipe classes and 7 serializers (records, `Recipe.CommonInfo`, `CraftingBookInfo`, `ItemStackTemplate`
  results, `assemble` without registries, `placementInfo`).
- `DyeCapsuleRecipe` reads `DataComponents.DYE`.
- The 34 recipe JSON files, `initialconfig/root/prefab_blueprint_recipe.json` and the code that writes ingredients
  (`PrefabsBlueprintAggregatorRecipe.FixIngredient`). A `prefab_blueprint_recipe.json` that does not parse is reported
  as D7 says; no conversion of the old format.
- Ingredients with components (`RecipeViewerContent`): use the loader's component ingredient (NeoForge
  `DataComponentIngredient`, Fabric `DefaultCustomIngredients.components`) through the `Platform` interface.
- Client side (D3): `CapsuleItems.registerRecipesClient` takes the synced recipes.
- Advancements: take the D8 fix from branch `1.21.1` (merge or cherry-pick it into `dev-1.26.2` before this phase).

Commit: "Port the recipes to 26.2".

### Phase 5: loot, commands, claims, network, the rest of the server (1.5–2 days)

`26.1.md` §2, §12.4; `1.21.11.md` §12, §13; `1.21.6-1.21.10.md` §4.8; `loaders-build.md` §3.3–§3.5, §4.3–§4.7.

- `CapsuleLootEntry` codec; `CapsuleCommand` permissions (`Commands.hasPermission(...)`) and the `/give` string
  (`ItemInput` no longer serializes itself); game rules in `StructureSaver`.
- Claims: `Claims`, `ClaimAdapter`, `FlanAdapter`, `OpenPartiesAndClaimsAdapter`, `GetOffMyLawnAdapter` and Common
  Protection API 2.0 (`NameAndId`). Compile against the 26.2 jars of these mods (Modrinth ids in §6) and read their
  APIs: `docs/CLAIMS.md` documents what each adapter must answer.
- Payloads: `PacketDistributor` → `ClientPacketDistributor` (NeoForge), `serverboundPlay`/`clientboundPlay` (Fabric).

Commit: "Port loot, commands, claims and networking to 26.2".

### Phase 6: client (5–7 days; the riskiest phase)

See the "world rendering" and "GUI" rows of §3. Write the 26.2 form directly. The `26.1.md` §11 and `1.21.11.md` §6
code would have to be rewritten again.

1. Client items: `assets/capsule/items/*.json` with a `range_dispatch` on the `capsule:state` property (thresholds 0 to
   7), a `capsule:layer` tint source calling `CapsuleItem.getColorFromItemstack`, and their registration on both
   loaders. Remove the `overrides` from `models/item/capsule.json`.
2. `LabelGui`: `GuiGraphicsExtractor`, input events, `Minecraft.gui`, colors with alpha.
3. `CustomRenderType`: the complete 26.2 version of `26.2.md` §7 (GHOST_DEPTH / GHOST, based on vanilla `CUTOUT_BLOCK`
   / `TRANSLUCENT_BLOCK`). Depth is reversed in 26.2: flip every depth comparison. Register the pipelines
   (`RegisterRenderPipelinesEvent` on NeoForge).
4. Ghost preview (`CapsulePreviewHandler`, `CapsuleTemplateRenderer`, `FakeWorld`, `TranslucentVertexConsumer`):
   gather what to draw during extraction (Fabric `LevelRenderEvents.END_EXTRACTION`, NeoForge
   `ExtractLevelRenderStateEvent`), then submit it through the frame's `SubmitNodeCollector` (Fabric
   `LevelRenderEvents.COLLECT_SUBMITS`, NeoForge `SubmitCustomGeometryEvent`). This is the path that keeps working
   with 26.3's Improved Transparency (§3). Block tessellation goes through `ModelBlockRenderer#tesselateBlock` and fluids through
   `FluidModel`. `FakeWorld` implements `LevelAccessor`, `LevelReader` and the client `BlockAndTintGetter`
   (`26.1.md` §11.6, §13.1).
5. Boxes (`RendererUtils`, `CaptureAnimation`, `CaptureBER`): `submitShapeOutline(...)`; `CaptureBER` with a render
   state (`1.21.6-1.21.10.md` §4.1, adapted to 26.2 submits). Delete the `RenderSystem` helpers
   (`doOverlayPrologue`, `doWirePrologue`, `pushColor`/`popColor`…).
6. No raw OpenGL anywhere: 26.2 has an experimental Vulkan backend (`26.2.md` §8).

Check by eye, with screenshots from the client smoke test (phase 8). Compare with `docs/screenshots/`: the
translucent full preview (#88), the wireframe preview, the red error boxes, the recall box and the capture animation
(#106). Check on both loaders and with Iris.

Commit: one per step above.

### Phase 7: loader glue (1.5–2 days)

`loaders-build.md` §3 (NeoForge) and §4 (Fabric).

- NeoForge: event classes (`AddServerReloadListenersEvent`, recipe events),
  capabilities (`Capabilities.Item`, `ResourceHandler` and transactions in `NeoForgePlatform`/`ContainerItemSource`;
  migrate completely, with no `IItemHandler` wrapper left: 26.3 deletes the whole `net.neoforged.neoforge.items`
  package),
  fluids through `FluidModel` (the code can move to `common`), no `EventBusSubscriber.Bus`, no static
  `FMLLoader.isProduction()`.
- Fabric: `END_LEVEL_TICK`, `ServerLevelEvents`, `LevelRenderEvents`, `FabricCreativeModeTab`, resource loader v1,
  recipe sync, the remaining mixins checked against 26.2 (`MinecraftMixin` targets `startAttack`, which 1.21.11
  restructured: `1.21.11.md` §16).
- Forge Config API Port: package `api.v5` (`ConfigRegistry`, `ModConfigEvents`).

Gate: `./gradlew build` is green, and both loaders start a dev client and a dev server (`runClient`, `runServer`) and
open a world. Commit.

### Phase 8: tests (3–4 days)

- Unit tests: `./gradlew :common:test`.
- GameTests: the registry-based framework (`1.21.4-1.21.5.md` §2.4, `26.1.md` §12.6, `loaders-build.md` §3.9, §4.9).
  The test methods (about 126) keep their bodies; each `batch` becomes an environment. `getBlockEntity(pos, Class)`,
  `fail(Component)`. Production NeoForge GameTests (`ProductionGameTests`, `scripts/prod-gametest.sh`): NeoForge 26.2
  disables GameTests in production, so follow `loaders-build.md` §3.9. Its approach is UNVERIFIED: if it fails, record
  that in `BACKLOG.md` and keep the dev-runtime GameTests.
- Data: `blueprint_whitelist.json` without the beds, with `minecraft:potent_sulfur` (`26.2.md` §13). Compare the
  whitelist with every 26.2 `BlockEntityType` (`BlockEntityTypes`) for other additions and removals since 1.21.1.
- Bundled JSON (D7): a GameTest reads every file under `data/capsule/initialconfig/**` with the code that reads it
  from `config/` (prefab recipe, whitelist, starters, loot templates) and fails on any error or warning. Another one
  feeds a broken `prefab_blueprint_recipe.json` and checks the error is logged and nothing crashes. Keep
  `RecipeTests#recipeAdvancementsWaitForTheirItem` (D8) and `everyCapsuleRecipeLoadsWithResolvedIngredients`.
- Client smoke and showcase (`ClientSmokeTest`, `Showcase`, `Scenario`): `Minecraft.gui`, world clocks, server-side
  weather, `LevelSettings`, screenshots (`26.2.md` §2, `26.1.md` §9–§10, `1.21.6-1.21.10.md` §1.9,
  `1.21.11.md` §17). The `capsule:state` check (`ClientSmokeTest:590-600`) reads the new item model property.
- Number of tests: the totals in `docs/TESTING.md` change (EMI and the D5 tests are gone). Update them from the real
  runs.

Gate: the GameTests pass on both loaders, with and without `-PmodCompat`. Commit.

### Phase 9: compatibility, scripts, CI, docs (2–3 days)

- Run the compatibility matrix (§6), then `scripts/validate-all.sh --iris --incompat` (§8).
- Scripts (`loaders-build.md` §5.5): Java 25, `prod-client.py` without the hard-coded 1.21.1, no EMI and no D5 mods in
  `validate-all.sh`.
- CI and publishing (`loaders-build.md` §5.1–§5.4): CircleCI image `cimg/openjdk:25.0.5`, `Dockerfile` on
  `eclipse-temurin:25-jdk`, `publish.sh` game version filter. CurseForge's name for the game version is UNVERIFIED:
  check it on the `/api/game/versions` endpoint before the first publish. `Jenkinsfile` targets branch 1.12: ask the
  maintainer whether to delete it.
- Docs: `CHANGELOG.md` (a `26.2-10.0.x` entry: what changed for players, configs of 9.x not carried over (D7), EMI
  and known incompatibilities not available), `docs/TESTING.md` (versions, totals, the matrix result), `README.md`, `BACKLOG.md` (whatever was left
  out).

### Phase 10: Minecraft 26.3 (4–5 days, branch `dev-1.26.3`)

Start from a 26.2 branch that meets §8. `inventory/26.3.md` (vanilla) and `inventory/26.3-loaders-build.md`
(versions, loaders) give every change; their effort section splits it into about half code and half retesting. The
section numbers below are those of `inventory/26.3.md`.

- Versions (D2): NeoForge 26.3.0.115, NeoForm 26.3-1, Fabric API (newest release for 26.3), the 26.3 Modrinth ids of
  §6. Same Gradle, ModDevGradle and Loom.
- Rendering: `com.mojang.blaze3d` → `com.mojang.renderpearl` (sed in §3), the 26.3 `CustomRenderType` of §4,
  `setOitPipelines(RenderPipelines.OIT_TRANSLUCENT_BLOCK)` on the ghost colour pass and
  `RenderTypes.linesTranslucentNoDepthWrite()` for the recall box (§5), `VertexConsumer#setUv3` in
  `TranslucentVertexConsumer` (§6).
- Input: no `org.lwjgl.glfw`; `LabelGui` tests `event.isEscape() || event.isConfirmation()`, `Showcase` uses
  `InputConstants` (§2).
- Loot (§8): `CapsuleLootEntry` on `SingleEntryContainerBase`, `ContextIntProviders`/`ContextFloatProviders`, and
  `"condition": {...}` instead of `"conditions": [...]` in `loot_table/blocks/capsulemarker.json` (otherwise the capture
  base silently stops surviving explosions as configured).
- Data: advancements `"recipe"` → `"recipes"` (§9), pack format (§1), whitelist with the poplar signs and shelf, beds
  through `AbstractBedBlock` in `Blueprint` (§15).
- Small changes: `RecipeMap` in the tests (§10), `FriendlyByteBuf` lists (§12), `getStructureTemplateManager`,
  `Player#drop` (§13), `TestData` with a dimension (§14).
- NeoForge: `ModConfig.Type.COMMON` no longer exists. Register with
  `registerConfig(ModConfig.Type.LOCAL, spec, "capsule-common.toml")`, so the file name does not change.
  Fabric's Forge Config API Port still has `COMMON`.
- Retest everything of phases 8 and 9 on 26.3, the client smoke test with Improved Transparency on and off, and
  the §6 matrix with its 26.3 column. CHANGELOG: a `26.3-10.0.x` entry.

## 6. Compatibility to retest

Rule: a mod tested on 1.21.1 that has a 26.2 build is retested with its latest 26.2 build. A mod without a 26.2 build
is not tested, and `docs/TESTING.md` says so. Since it worked on 1.21.1, we can be fairly confident in it. Versions as
of 2026-11-23, from the Modrinth API (26.2 version ids in `inventory/loaders-build.md` §1; 26.3 ids below). Take the
newest build again when the phase starts.

| Mod | Tested on 1.21.1 | 26.2 build | 26.3 build (Modrinth id) | How to test |
|---|---|---|---|---|
| JEI | 19.57.0.451 | 30.48.0.244 (beta), NeoForge + Fabric | 31.23.0.129 (beta) (`lUxPAD1g` / `W90vrxwW`) | client smoke, `RECIPE_VIEWER=jei`, both loaders |
| REI (+ Architectury, Cloth Config) | 16.0.799 | 26.2.820 | 26.3.824 (`uwE0PJmc` / `rKsAE6bX`), Architectury 22.0.6, Cloth Config 26.3.159 | client smoke, `RECIPE_VIEWER=rei`, both loaders |
| EMI | 1.1.24 | **none** | **none** | not tested; excluded (D4) |
| SecurityCraft | v1.10.2.1 | v1.10.2.1 (NeoForge) | v1.10.2.1-beta4 (beta) (`mgxwefx4`, NeoForge) | NeoForge GameTests (`SecurityCraftTests`) |
| Waystones + Balm | pinned 1.21.1 | 26.2.0.14 / 26.2.0.9 | 26.3.0.3 / 26.3.0.3 (`CEfraVFf` / `QcBcRJZb`) | `-PmodCompat` NeoForge |
| Sophisticated Storage + Core | pinned 1.21.1 | 26.2-1.5.133 / 26.2-1.5.40 (NeoForge) | 26.3-1.5.134 / 26.3-1.5.42 (`on0Ziwnf` / `8fAHwWo2`) | `-PmodCompat` NeoForge |
| WorldEdit | 7.3.8 | 7.4.5 | 7.4.6 (`ByRNkO83`) | `-PmodCompat` NeoForge (writes the schematic fixtures) |
| Open Parties and Claims | 0.32.7 | 0.33.0 (beta), NeoForge + Fabric | 0.33.0 (beta) (`VhEuA4pj` / `KBCCg0os`) | `-PmodCompat` both loaders, Fabric release jar |
| Flan | 1.12.8 | 26.2-1.12.8, NeoForge + Fabric | 26.3-1.12.9 (`TL00Frpw` / `8PVcH9Li`) | `-PmodCompat` both loaders, Fabric release jar |
| Get Off My Lawn ReServed | 1.13.1 | 1.21.0+26.2 (Fabric) | 1.22.0+26.3-rc-1 (`p3z2M9AO`, built for 26.3-rc-1: check that it loads on 26.3) | Fabric release jar with the claim mods |
| Common Protection API | 1.0.0 | 2.0.0 | 2.0.0 (declares no Minecraft version) | Fabric GameTests (`TestProbe`) |
| Forge Config API Port | 21.1.6 | 26.2.1 | 26.3.1 (`JpKvrr9J` Fabric) | every Fabric run |
| Corail Tombstone | 9.5.6 | 9.9.4 (NeoForge) | **none** | 26.2: `-Pincompat` NeoForge; 26.3: not tested |
| Refined Storage | 2.0.9 | **none** (3.2.1 exists for 26.1.2) | **none** | not tested (D5) |
| Mekanism | 10.7.19.85 | **none** | **none** | not tested (D5) |
| Immersive Engineering | 12.4.2-194 | **none** | **none** | not tested (D5) |
| Super Factory Manager | 4.34.0 | **none** (4.34.0 exists for 26.1.2) | **none** | not tested (D5) |
| Iris + Sodium | Iris + Sodium of the 1.21.1 runs | 1.11.4 / 0.9.2, NeoForge + Fabric | 1.11.7 / 0.9.4-beta.1 (beta) (`Ag5S66CF`, `vTN4NRGW` / `LHznwVbu`, `6etTPjD5`) | `validate-all.sh --iris` |
| Modpack (Forgeulously Optimized 1.1.4) | 49 mods, Connector | **none** for 26.x | **none** | not tested; optionally pick a 26.2/26.3 NeoForge pack |

`-PmodCompatLatest` already picks the newest Modrinth version of each mod. Pin the versions above in
`gradle.properties`, and run `-PmodCompatLatest` once at the end.

## 7. Risks

- **World rendering** (phase 6) changed six times between 1.21.1 and 26.2, and the 26.2 pipeline API is new. The
  translucent preview is a mod feature (#88), not a vanilla one. Do not fall back to drawing directly (`26.2.md` §3,
  path B): it does not run in 26.3 with Improved Transparency. If submits cannot draw it as before, ask the maintainer
  before changing how it looks.
- **26.3 Improved Transparency**: a blended render type without OIT pipelines crashes the client when the option is on
  (`26.3.md` §5). The client smoke test must run with the option on and off.
- **26.3 SDL instead of GLFW**: key and mouse codes changed (`26.3.md` §2). Every input in `LabelGui` and in the smoke
  test scenario must be replayed on 26.3.
- **The recipe API** changed twice (1.21.2, 26.1). `PrefabsBlueprintAggregatorRecipe` relied on a network-codec
  trick that no longer exists. Re-read it fully before porting it.
- **Production NeoForge GameTests** rely on undocumented NeoForge behavior (`BACKLOG.md`, "Production NeoForge
  GameTests depend on NeoForge internals": "A NeoForge update can break it. Effort: S per update"). On 26.2 the
  workaround is new and unverified.
- **Beta dependencies**: JEI and Open Parties and Claims for 26.2 are published as beta.

## 8. Definition of done

- `./gradlew build` green; `scripts/validate-all.sh --iris --incompat` with every step PASS except the ones §6 marks
  "not tested". Paste its summary table into `docs/TESTING.md` like the 1.21.1 one.
- `docs/MANUAL_VALIDATION.md` played once on each loader.
- A 1.21.1 world with capsules (empty, linked, deployed, blueprint, one-use, reward) and capture bases opens on 26.2.
  Its capsules keep their content and deploy it.
- The docs of §5 phase 9 updated. `BACKLOG.md` lists what was left out (EMI, the D5 mods, the production GameTests if
  they could not be kept).
- 26.3 (phase 10): the same list on `dev-1.26.3`, plus the client smoke test with Improved Transparency on, and a
  26.2 world opening on 26.3.
