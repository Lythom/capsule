# dev-1.21.1 – round 2 plan

Source: the owner's decisions on every `BACKLOG.md` entry (2026-10-06). Same rules as round 1 (`docs/PLAN.md`): work on
`dev-1.21.1` (backports on `dev-1.20`, `dev-1.18`, `dev-1.16`), never push a version branch, commits authored and
committed by `Lythom <contact@samuel-bouchet.fr>` without trailer, one focused commit per subject, every commit builds,
bug fixes come with a test seen failing first.

Owner decisions already taken: 26.1.2 and 26.2 only added to the version list (not built in this round); backports
carry bug fixes only; Loyalty is obtained like on a trident (table + anvil, any level); the blueprint whitelist stays a
JSON config (updated, no tag); Forge 1.21.1 is dropped.

## Workstreams and order

| Stream | Branch | Subjects | Runs |
|---|---|---|---|
| A – gameplay and data | `dev-1.21.1` | G1–G12 | first |
| B – claim protection | `dev-1.21.1` | P1 | after A |
| C – rendering | `dev-1.21.1` | R1–R4 | after A, in parallel with B (own worktree, merged by the coordinator) |
| D – integrations and test tooling | `dev-1.21.1` | T1–T4 | after B and C |
| E1 – backports of round 1 | `dev-1.20`, `dev-1.18`, `dev-1.16` | K1a | in parallel with A (first step: install Temurin 8 and 17) |
| E2 – backports of round 2 | same | K1b | after A and B are on `dev-1.21.1` |
| Final | – | docs D1, independent review of every condition below | last |

Resource rules (4 cores, 15 GB): every GameTest, client smoke or prod smoke run takes a machine-wide lock
(`flock /tmp/capsule-heavy.lock …`) so at most one Minecraft instance runs at a time; Gradle daemons capped with
`org.gradle.jvmargs`; stream E does its ForgeGradle decompile setup while only stream A runs. Streams B and C keep
their changes to separate files where possible; `CHANGELOG.md` and the docs listed in D1 are edited by the
coordinator only, from the streams' reports.

## Success conditions

### A – gameplay and data

- **G1 Loyalty replaces Recall (#123, #96)** – On both loaders: a capsule accepts Loyalty from the enchanting table and
  from a book on the anvil; any Loyalty level brings a thrown capsule back like Recall did; capsules already enchanted
  with Recall keep working; Recall is no longer obtainable (not in `#minecraft:in_enchanting_table`, `non_treasure`,
  loot or trades); other trident-only enchantments (Impaling, Riptide, Channeling) still cannot be applied to a
  capsule. The return logic no longer scans every item entity of every level each tick: it tracks the capsule entities
  that joined a level (including entities loaded from disk, removed on unload); it tracks every thrown capsule, so a
  later "deploy on landing" fallback for ItemPhysic (BACKLOG) can reuse it, and stays in `common` for easy backports.
  Only capsules are recalled (`#capsule:enchantable/recall` items): a dropped Loyalty trident is not. Loader hooks:
  NeoForge `supportsEnchantment` and `isPrimaryItemFor`; Fabric `EnchantmentEvents.ALLOW_ENCHANTING` for both
  contexts. "Not obtainable" means in survival (creative still lists every enchanted book). Lang strings and recipe
  viewer information pages say Loyalty. GameTests cover: table offers Loyalty for a capsule, anvil book works, Impaling
  refused, Loyalty capsule returns, legacy Recall capsule returns, a dropped Loyalty trident is not recalled, and a
  scan-free implementation (e.g. a counter of tracked entities).
- **G2 Fire-proof capsules (#123)** – capsule items do not burn in fire or lava (GameTest: a capsule thrown into lava
  survives, and still deploys and is recalled).
- **G3 Recipes for materials and a vanilla 13³ capsule (#120)** – a documented tier table (material, size, base and
  material colors chosen from the tier and the material's dominant color) covering every vanilla material that fits
  (at least a vanilla recipe giving a 13³ capsule) and the common modded metals missing today (e.g. steel, zinc, brass,
  aluminum, osmium, uranium – each guarded by a non-empty tag load condition on both loaders). A test datapack (or a
  JSON-level test) fills the modded tags so the recipe GameTest validates every recipe; JEI shows them (client smoke).
  The tier table is a proposal for the owner's review (listed in the round report).
- **G4 Blueprint whitelist update (#101)** – `blueprint_whitelist.json` lists every vanilla 1.21.1 block entity block
  that can be safely recreated from its item (campfires, signs and hanging signs with their text, banners, heads,
  shulker boxes, ender chest, decorated pot, chiseled bookshelf, crafter, sculk blocks…), drops dead ids, and a GameTest
  lists every vanilla block entity type as either whitelisted or explicitly excluded with a reason, so new ones are
  noticed. No inventory is ever kept: a dupe GameTest deploys a blueprint of a filled shulker box, chiseled bookshelf,
  decorated pot, crafter, campfire and lectern and checks they come out empty. Existing installs: documented in
  CHANGELOG ("delete `config/capsule` to get the new defaults").
- **G5 Starter hut crafting table (#126)** – the item frame no longer covers the crafting table's only reachable face
  in the affected starter templates; GameTest: after deploying each starter, every crafting table has a reachable face
  without an attached item frame.
- **G6 Prefab blueprint explicit error (#124)** – an unresolvable prefab source template logs an error naming the
  template and the searched paths, and the player gets a chat message; no empty blueprint is produced silently. Test.
- **G7 Deploy failure reports (#108)** – closed as cannot-reproduce, as the backlog proposed (every bundled template
  deploys in `everyBundledTemplateDeploys`): triage updated with a ready-to-post comment, no code change.
- **G8 Blueprint crafting dupe and prefab refund slots (#84)** – crafting through a real `CraftingMenu` (normal click and
  shift-click, several items) consumes exactly the expected ingredients for the blueprint, blueprint change and prefab
  recipes, including a modified `prefab_blueprint_recipe.json` pattern; refunds are derived from the recipe, not from
  hard-coded slots. Fixed if a dupe or wrong refund is found (test fails first).
- **G9 Sponge v3 and every supported format (#70)** – tests load and deploy fixtures for vanilla `.nbt`, MCEdit
  `.schematic`, Sponge v1, v2 and v3 (`.schem` and `.schematic`), including a chest with content and an entity where the
  format allows; v2 and v3 fixtures are real WorldEdit exports (generated in a dev runtime, committed as small test
  resources; if WorldEdit cannot be scripted headless, write them with WorldEdit's own clipboard writer inside a dev
  GameTest); `.schem` files are discovered by the template managers.
- **G10 Item frames "invalid position" (#round-1 finding)** – deploying a template with item frames logs no
  "Block-attached entity at invalid position" and the frames end up on the right blocks; test captures the log.
- **G11 Tooltip "Dyed" line** – capsule tooltips no longer end with "Dyed" (test on the stack's tooltip lines or the dyed
  color component's tooltip flag).
- **G12 Update checker URL** – `updateJSONURL` removed; no version-check warning in the NeoForge log.

### B – claim protection

- **P1 Claim mods veto captures and deploys (#91 part 2)** – a short study in `docs/CLAIMS.md`: the most used protection
  mods on NeoForge and Fabric 1.21.1 (FTB Chunks, Open Parties and Claims, Flan, Cadmus, Common Protection API, …), their
  public query APIs, and the chosen approach. Requirements:
  - **performance first**: cost proportional to the number of chunks or intersecting claim regions touched, not to the
    number of blocks (Flan and GOML claims are rectangles); a GameTest with a counting test adapter shows at most one
    query per chunk or region for the largest capsule size;
  - **weak coupling**: one small adapter per supported mod, used only when that mod is loaded (no hard or Gradle runtime
    dependency for players, reflection or optional compile-only API), Capsule works unchanged without them; an adapter
    that throws is disabled with one log line and never crashes the server;
  - **coverage**: captures and deploys, by a player and by a capture base (new bases remember the player who placed
    them and are checked as that player; bases placed before this version keep the previous behavior, documented);
  - **proof**: real protection mods from Modrinth in the mod-compat GameTest runtimes (e.g. Open Parties and Claims and
    Flan, at least one on Fabric): a non-member capture inside a claim is refused, a member's is allowed, outside a
    claim is allowed. Capture bases checked through a FakePlayer or a UUID based API.
  - No per-block `BreakEvent` is fired; the plan for the current per-block dirt `EntityPlaceEvent` probe on NeoForge is
    stated (kept, replaced or limited) with its cost.

### C – rendering

- **R1 Translucent preview (#88)** – the full preview renders ghost blocks translucent, without z-order artifacts
  against terrain, water, glass and the deployed structure in the client smoke screenshots (both loaders); if a
  translucency approach shows artifacts, keep opaque and document why.
- **R2 Capture "sucked in" animation (#106)** – a client-side animation (and particles) plays when a capture happens,
  driven by the existing undeploy notification, disabled by a client config entry (`captureAnimation`, default on, in
  `capsule-client.toml`); the template content is not on the client at capture time, so the animation uses what the
  client sees (a snapshot of the captured blocks, or a shrinking box as fallback). The client smoke captures frames with
  the option on and checks a render counter is non-zero with it on and zero with it off; no extra server cost.
- **R3 Preview with modded blocks (#81, #117, #76, #94)** – the client smoke runs with the mods named in these issues
  when a 1.21.1 build exists on Modrinth or a public maven (otherwise recorded as blocked) (Integrated Dynamics, Ad Astra, Mob Grinding Utils, a farmland mod such as Farmer's
  Delight): a template containing their blocks previews and deploys with no crash and no capsule error in the log;
  screenshots reviewed; issues reclassified from "hardened" to "verified" or with a precise remaining problem.
- **R4 Preview with shaders (#69)** – after R1–R3, client smoke with Iris (+ Sodium) and one popular shader pack on
  each loader where Iris exists (a lightweight pack at low resolution, longer timeout under llvmpipe): the preview, the
  capture base wireframe and the animation are visible; fixed if not, or documented precisely if the cause is in Iris.
  For R1, R2 and R4 the reviewer looks at the named screenshots and records what it sees.

### D – integrations and test tooling

- **T1 REI and EMI** – Capsule recipes (including dynamic prefab and blueprint recipes) and information pages show in
  REI and in EMI on Fabric; on NeoForge too only if it costs almost nothing (said in the report); the client smoke
  asserts it for each viewer like it does for JEI. Any Loom/Gradle upgrade needed (Loom 1.18 needs Gradle on Java 25)
  is done first in stream D and checked against ModDevGradle, CircleCI images and the S4 jar outputs.
- **T2 GameTests on the release jars** – a script runs the GameTests against each built release jar on a real dedicated
  server of its loader (NeoForge with `-Dneoforge.enableGameTest=true -Dneoforge.gameTestServer=true` and a separate
  test-mod jar, remapped for Fabric) and exits non-zero on failure, proven with a deliberately failing test.
- **T3 JEI on Fabric and Connector modpacks in the client smoke** – the client smoke runs JEI on Fabric (installing a
  Java 25 runtime for Gradle if Loom requires it) and a NeoForge modpack that uses Sinytra Connector, reusing T2's
  harness jar in a production NeoForge client (installer, plus HeadlessMC if useful); documenting a blocker is only
  acceptable after that attempt.
- **T4 One command for the reviewer** – `scripts/validate-all.sh` runs every automated layer (build, unit tests,
  GameTests on both loaders, mod-compat GameTests, release-jar GameTests, client smoke on both loaders with each recipe
  viewer, prod smoke) and prints one PASS/FAIL summary with the paths of logs and screenshots.

### E – backports

- **K1a / K1b Confirmed fixes on 1.20.1, 1.18.2 and 1.16.5** – for each bug fix of round 1 (K1a) then round 2 (K1b)
  that applies to the version: ported on `dev-1.20` / `dev-1.18` / `dev-1.16` (created from `1.20` / `1.18` / `1.16`,
  built with Temurin 17 / 17 / 8 through `-Dorg.gradle.java.home`); each branch builds and its release jar boots on a real
  dedicated server (a Forge-installer variant of `prod-smoke.sh`); a backport matrix in `docs/BACKPORTS.md` (on
  `dev-1.21.1`) gives, per fix and version, the commit or "not applicable" with the reason (e.g. enhancements and
  "Dyed", which needs data components from 1.20.5). The old branches have no GameTest setup: a minimal JUnit test where
  the fix is pure logic, otherwise the boot test plus the reasoning in `BACKPORTS.md` (documented exception to the
  test-first rule). Each branch's CHANGELOG lists the ported fixes. Nothing is pushed to `1.20`, `1.18` or `1.16`.

### Final

- **D1 Docs** – `docs/VERSIONS.md` adds 26.2 next to 26.1.2 in the targets; `BACKLOG.md` keeps only what is still to do
  (ItemPhysic kept for later, "keep compatibility easy"), and records the owner's won't-do decisions (Forge 1.21.1,
  experience orb merge, default template refresh: delete the config folder, reward template rewrite: intended, capture
  base redstone look: intended) and the Fabric Transfer API storage gap; `docs/ISSUE_TRIAGE.md` (with ready-to-post
  comments), `docs/MANUAL_VALIDATION.md`, `docs/TESTING.md` and the CHANGELOG reflect the round, including the new config
  keys and the capture-base placer UUID stored in saved data.
- **Review** – an independent agent runs `scripts/validate-all.sh` from a clean clone and checks every condition above
  with evidence; defects are fixed before the round is reported.

## Round 2b – owner decisions of 2026-10-06 on the round 2 report

- **L1 No lapis tier** – the lapis capsule recipe is removed (blueprints are blue, avoid confusion); the rest of
  `docs/RECIPES.md` is approved; capsule bodies stay white unless dyed. Recipe tests and docs updated.
- **L2 No identity, no access inside claims** – every capture or deploy without an identity (vanilla dispensers,
  capture bases placed before 9.1, any path without a player or placer) is refused inside a claim (adapters and the
  generic probe), and still allowed outside claims. GameTests on both loaders (counting adapter + a real mod in
  modCompat).
- **L3 Protection checked per block** – a single block can be protected, so the generic probe (mods without adapter) must
  answer per block, not per chunk column. Measure, on 1.21.1 and on 1.16.5, the cost of per-block vs per-chunk probing
  for capture sizes 3, 11, 31 and 255 (OP), and record the numbers in `docs/CLAIMS.md`. Per-block is adopted when it
  stays within one server tick (50 ms) for survival sizes (≤ 31, the largest upgraded capsule); the 255 OP figure is
  reported for the owner. Adapters stay exact per block (Flan, GOML boxes) or per chunk where the mod's claims are
  chunks (OPAC).
- **L4 Backport releases prepared** – `dev-1.20`, `dev-1.18`, `dev-1.16` carry L2 and L3 (per block where measured
  acceptable; 1.16 keeps per block), a final CHANGELOG entry, a working publish path (`publish.sh` with the Forge loader
  and the right game version, dry-run on each built jar) and a CI config that builds on merge into the version branch;
  release candidate jars built, boot-tested and handed to the owner. Nothing is pushed to `1.20`, `1.18`, `1.16`.
