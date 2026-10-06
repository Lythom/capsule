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
| E – backports | `dev-1.20`, `dev-1.18`, `dev-1.16` | K1 | in parallel with A–D |
| Final | – | docs D1, independent review of every condition below | last |

## Success conditions

### A – gameplay and data

- **G1 Loyalty replaces Recall (#123, #96)** – On both loaders: a capsule accepts Loyalty from the enchanting table and
  from a book on the anvil; any Loyalty level brings a thrown capsule back like Recall did; capsules already enchanted
  with Recall keep working; Recall is no longer obtainable (not in `#minecraft:in_enchanting_table`, `non_treasure`,
  loot or trades); other trident-only enchantments (Impaling, Riptide, Channeling) still cannot be applied to a
  capsule. The return logic no longer scans every item entity of every level each tick: it tracks the capsule entities
  that joined a level. GameTests cover: table offers Loyalty for a capsule, anvil book works, Impaling refused, Loyalty
  capsule returns, legacy Recall capsule returns, and a scan-free implementation (e.g. a counter of tracked entities).
- **G2 Fire-proof capsules (#123)** – capsule items do not burn in fire or lava (GameTest: item entity in lava survives
  N ticks).
- **G3 Recipes for materials and a vanilla 13³ capsule (#120)** – a documented tier table (material, size, base and
  material colors chosen from the tier and the material's dominant color) covering every vanilla material that fits
  (at least a vanilla recipe giving a 13³ capsule) and the common modded metals missing today (e.g. steel, zinc, brass,
  aluminum, osmium, uranium – each guarded by a non-empty tag load condition on both loaders). The recipe GameTest
  validates them all; JEI shows them (client smoke).
- **G4 Blueprint whitelist update (#101)** – `blueprint_whitelist.json` lists every vanilla 1.21.1 block entity block
  that can be safely recreated from its item (campfires, signs and hanging signs with their text, banners, heads,
  shulker boxes, ender chest, decorated pot, chiseled bookshelf, crafter, sculk blocks…), drops dead ids, and a GameTest
  lists every vanilla block entity type as either whitelisted or explicitly excluded with a reason, so new ones are
  noticed. Existing installs: documented in CHANGELOG ("delete `config/capsule` to get the new defaults").
- **G5 Starter hut crafting table (#126)** – the item frame no longer covers the crafting table's only reachable face
  in the affected starter templates; GameTest: after deploying each starter, every crafting table has a reachable face
  without an attached item frame.
- **G6 Prefab blueprint explicit error (#124)** – an unresolvable prefab source template logs an error naming the
  template and the searched paths, and the player gets a chat message; no empty blueprint is produced silently. Test.
- **G7 Deploy failure reports (#108)** – when a deploy fails, the player gets the reason (blocking blocks count and
  first position, or the exception message) and the server logs the full exception with the template name. GameTest
  forcing a failure checks the message.
- **G8 Blueprint crafting dupe and prefab refund slots (#84)** – crafting through a real `CraftingMenu` (normal click and
  shift-click, several items) consumes exactly the expected ingredients for the blueprint, blueprint change and prefab
  recipes, including a modified `prefab_blueprint_recipe.json` pattern; refunds are derived from the recipe, not from
  hard-coded slots. Fixed if a dupe or wrong refund is found (test fails first).
- **G9 Sponge v3 and every supported format (#70)** – tests load and deploy fixtures for vanilla `.nbt`, MCEdit
  `.schematic`, Sponge v1, v2 and v3 (`.schem` and `.schematic`), including a chest with content and an entity where the
  format allows; v2 and v3 fixtures are real WorldEdit exports (generated in a dev runtime, committed as small test
  resources); `.schem` files are discovered by the template managers.
- **G10 Item frames "invalid position" (#round-1 finding)** – deploying a template with item frames logs no
  "Block-attached entity at invalid position" and the frames end up on the right blocks; test captures the log.
- **G11 Tooltip "Dyed" line** – capsule tooltips no longer end with "Dyed" (test on the stack's tooltip lines or the dyed
  color component's tooltip flag).
- **G12 Update checker URL** – `updateJSONURL` removed; no version-check warning in the NeoForge log.

### B – claim protection

- **P1 Claim mods veto captures and deploys (#91 part 2)** – a short study in `docs/CLAIMS.md`: the most used protection
  mods on NeoForge and Fabric 1.21.1 (FTB Chunks, Open Parties and Claims, Flan, Cadmus, Common Protection API, …), their
  public query APIs, and the chosen approach. Requirements:
  - **performance first**: cost proportional to the number of claim regions or chunks touched, not to the number of
    blocks; a GameTest with a counting test adapter shows at most one query per chunk for the largest capsule size;
  - **weak coupling**: one small adapter per supported mod, used only when that mod is loaded (no hard or Gradle runtime
    dependency for players, reflection or optional compile-only API), Capsule works unchanged without them; an adapter
    that throws is disabled with one log line and never crashes the server;
  - **coverage**: captures and deploys, by a player and by a capture base (new bases remember the player who placed
    them and are checked as that player; bases placed before this version keep the previous behavior, documented);
  - **proof**: at least one real protection mod in the mod-compat GameTest runtime: a non-member capture inside a claim
    is refused, a member's is allowed, outside a claim is allowed.
  - No per-block `BreakEvent` is fired.

### C – rendering

- **R1 Translucent preview (#88)** – the full preview renders ghost blocks translucent, without z-order artifacts
  against terrain, water, glass and the deployed structure in the client smoke screenshots (both loaders); if a
  translucency approach shows artifacts, keep opaque and document why.
- **R2 Capture "sucked in" animation (#106)** – a client-side animation (and particles) plays when a capture happens,
  driven by the existing undeploy notification, disabled by a client config entry (`captureAnimation`, default on);
  client smoke captures frames of it with the option on and checks nothing renders with it off; no extra server cost.
- **R3 Preview with modded blocks (#81, #117, #76, #94)** – the client smoke runs with the mods named in these issues
  when a 1.21.1 build exists (Integrated Dynamics, Ad Astra, Mob Grinding Utils, a farmland mod such as Farmer's
  Delight): a template containing their blocks previews and deploys with no crash and no capsule error in the log;
  screenshots reviewed; issues reclassified from "hardened" to "verified" or with a precise remaining problem.
- **R4 Preview with shaders (#69)** – after R1–R3, client smoke with Iris (+ Sodium) and one popular shader pack on
  each loader where Iris exists: the preview, the capture base wireframe and the animation are visible; fixed if not,
  or documented precisely if the cause is in Iris.

### D – integrations and test tooling

- **T1 REI and EMI** – Capsule recipes (including dynamic prefab and blueprint recipes) and information pages show in
  REI and in EMI on both loaders; the client smoke asserts it for each viewer like it does for JEI.
- **T2 GameTests on the release jars** – a script runs the GameTests against each built release jar on a real dedicated
  server of its loader and exits non-zero on failure.
- **T3 JEI on Fabric and Connector modpacks in the client smoke** – the client smoke runs JEI on Fabric (installing a
  Java 25 runtime for Gradle if Loom requires it) and a NeoForge modpack that uses Sinytra Connector, or documents the
  exact blocker.
- **T4 One command for the reviewer** – `scripts/validate-all.sh` runs every automated layer (build, unit tests,
  GameTests on both loaders, mod-compat GameTests, release-jar GameTests, client smoke on both loaders with each recipe
  viewer, prod smoke) and prints one PASS/FAIL summary with the paths of logs and screenshots.

### E – backports

- **K1 Confirmed fixes on 1.20.1, 1.18.2 and 1.16.5** – for each fix of rounds 1 and 2 that applies to the version: ported
  on `dev-1.20` / `dev-1.18` / `dev-1.16` (created from `1.20` / `1.18` / `1.16`), each branch builds and its release jar
  boots on a real dedicated server; a backport matrix in `docs/BACKPORTS.md` (on `dev-1.21.1`) gives, per fix and version,
  the commit or "not applicable" with the reason; each branch's CHANGELOG lists the ported fixes.

### Final

- **D1 Docs** – `docs/VERSIONS.md` adds 26.2 next to 26.1.2 in the targets; `BACKLOG.md` keeps only what is still to do
  (ItemPhysic kept for later, "keep compatibility easy"), and records the owner's won't-do decisions (Forge 1.21.1,
  experience orb merge, default template refresh: delete the config folder, reward template rewrite: intended, capture
  base redstone look: intended); `docs/ISSUE_TRIAGE.md`, `docs/MANUAL_VALIDATION.md`, `docs/TESTING.md` and the
  CHANGELOG reflect the round.
- **Review** – an independent agent runs `scripts/validate-all.sh` from a clean clone and checks every condition above
  with evidence; defects are fixed before the round is reported.
