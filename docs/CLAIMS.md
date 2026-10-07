# Claim protection (#91)

How captures and deploys respect protection mods on Minecraft 1.21.1, and why it is done this way. Study made on
2026-10-06; signatures checked with `javap` on the jars named below.

## The question

A capture removes up to 255³ blocks and a deploy places as many. Until Capsule 9.0 each position was checked with a
simulated `BlockEvent.EntityPlaceEvent` (dirt) on NeoForge, or `CommonProtection.canPlaceBlock` on Fabric: one query
per block, and nothing on Fabric for mods without Common Protection API. Firing a real `BreakEvent` per block would let
every protection mod veto, but it also reaches quests, statistics, vein miners and drop modifiers, and costs one event
per block. There is no "query only" protection event on NeoForge: each mod checks its own claims inside its event
listeners. Common Protection API is the only cross-mod query API, on Fabric only, and it answers per position.

Most claim mods keep their claims as chunks or boxes, so the cheap way is to ask each mod for the claims intersecting
the capsule's box, once per chunk or per claim, through its own public API: one small adapter per mod, compiled
against the API and used only when the mod is loaded, like `SecurityCraftOwnerCheck`.

## Mods

Downloads: Modrinth API (`/v2/project`, `/v2/search`), all versions. FTB Chunks and Cadmus are not on Modrinth; their
CurseForge counts were not collected (CurseForge is not scraped).

| Mod | Loaders (1.21.1) | Downloads | Claims | Query API (jar checked) | Query cost | Listens to |
|---|---|---|---|---|---|---|
| Open Parties and Claims | NeoForge, Fabric | 23.0 M | chunk columns, owner + party + allies | `OpenPACServerAPI.get(MinecraftServer)`; `.getServerClaimsManager().get(ResourceLocation dim, int chunkX, int chunkZ)` → `IPlayerChunkClaimAPI` or null; `.getChunkProtection().hasChunkAccess(UUID, ResourceLocation, int, int)` (0.32.7, both loaders) | two map lookups per chunk, by player id | NeoForge `EntityPlaceEvent`, `BreakEvent`; Fabric: own mixins, no Common Protection API |
| Yet Another World Protector | NeoForge, Fabric | 267 k | admin regions (boxes) with flags | `FlagEvaluator.processCheck(FlagCheckRequest)`, per position (0.6.3-beta4) | per position | NeoForge `EntityPlaceEvent`, `BreakEvent` |
| Flan | NeoForge, Fabric | 249 k | boxes (claims from a minimum Y up, sub-claims), permission groups | API (`io.github.flemmli97.flan.api`): `ClaimHandler.canInteract(ServerPlayer, BlockPos, ResourceLocation)`, per position, which is `ClaimHandler.getPermissionStorage(player's ServerLevel).getForPermissionCheck(BlockPos)` → `IPermissionContainer` (the claim at the position, or one container for the world outside claims), then `.canInteract(ServerPlayer, ResourceLocation, BlockPos)`; `BuiltinPermission.BREAK` (1.12.8, both loaders). Its other classes (`ClaimStorage`, `Claim`, `ClaimBox`) are internals | one map lookup and one permission check per position | NeoForge `BreakEvent`, `EntityPlaceEvent`, `EntityMultiPlaceEvent`; Fabric: Common Protection API provider |
| Hey That's Mine | Fabric | 71 k | container locks, not claims | – | – | – |
| Get Off My Lawn ReServed | Fabric | 63 k | boxes around claim anchors, in an R-tree | `ClaimUtils.getClaimsInBox(LevelReader, BlockPos, BlockPos)` → `Selection<Entry<ClaimBox, Claim>>`; `ClaimBox.minecraftBox()` → `AABB`; `Claim.hasPermission(UUID)`; `ClaimUtils.isInAdminMode(Player)` (1.13.1+1.21) | one R-tree query, one check per claim | Common Protection API provider |
| FTB Chunks | NeoForge, Fabric | not on Modrinth | chunk columns, teams | `FTBChunksAPI.api().getManager().getChunk(ChunkDimPos)` → `ClaimedChunk` or null; `.getTeamData().isTeamMember(UUID)`, `isAlly(UUID)`, `canPlayerUse(ServerPlayer, PrivacyProperty)`; `ClaimedChunkManager.shouldPreventInteraction(Entity, InteractionHand, BlockPos, Protection, Entity)` (NeoForge 2101.1.22, maven.ftb.dev) | one lookup per chunk | Architectury block events: NeoForge `EntityPlaceEvent`, `BreakEvent` |
| Cadmus (Argonauts teams) | NeoForge, Fabric | not on Modrinth | chunk columns | not checked: no jar on a public maven | – | – |
| Common Protection API | Fabric library | (maven.nucleoid.xyz) | – | `CommonProtection.canPlaceBlock` / `canBreakBlock(Level, BlockPos, GameProfile, Player)` per position; `isAreaProtected(Level, AABB)` for anyone, not per player (1.0.0) | per position, every provider | implemented by Flan and Get Off My Lawn, not by Open Parties and Claims or FTB Chunks |

## Chosen approach

`capsule.plugins.claims.Claims.denied(level, box, player)` returns a test of the positions the player may not change,
computed before the capture or deploy touches any block:

1. **Adapters** (`ClaimAdapter`), loaded if their mod id is loaded, compiled against the public API of their mod:
   - `OpenPartiesAndClaimsAdapter` (both loaders): per chunk of the box, the chunk claim and `hasChunkAccess` by player id.
   - `FlanAdapter` (both loaders): Flan's API answers per position only, so it is asked like the generic probe below:
     each position of the box up to the largest survival capsule (33 by default, below), the center of each chunk column of the box above, whose answer applies
     to the column. A position whose permission container is not the world's is in a claim, where the player's `BREAK`
     permission decides (sub-claims included). The world's is the container of two positions 1024 blocks below the
     world at opposite corners of it, which no claim spans: claims reach 10 blocks below the world when Flan's
     `defaultClaimDepth` is -1. The storage is the one of the
     capture's level: `ClaimHandler.canInteract` would take the player's, another dimension for a player who changed
     dimension while their capsule flew.
   - `GetOffMyLawnAdapter` (Fabric, in the `fabric` project): one `getClaimsInBox`, one `hasPermission` per claim (all
     allowed in admin mode).

   Each returns claims as boxes with an allowed flag (Flan: one per run of positions with the same answer along y, or
   per chunk column). Within a mod the last claim containing a position decides
   (sub-claims come after their claim); across mods any refusal wins. `Claims.register` lets another mod add an adapter.
2. **Generic probe** for mods without adapter, through the loader hook kept from before (`Platform.canPlaceBlock`: a
   dirt `EntityPlaceEvent` on NeoForge, `CommonProtection.canPlaceBlock` on Fabric), outside adapter claims:
   - up to the largest survival capsule (`Claims.perBlockMaxSize`: the largest crafted tier plus
     `capsuleUpgradesLimit` × 2, 33 by default; the size is the box's largest side): each
     tested position, when it is tested;
   - above (OP capsules): one query per chunk column of the box, at the column's center or a corner that no adapter
     claim covers. Columns fully covered by adapter claims are not probed. The answer applies to the column's positions
     outside adapter claims.
3. The test of a position is a chunk lookup in the adapter claims, then one probe query up to the largest survival capsule, a lookup in the
   column results above.

The capture removes only the allowed positions, as before (protected blocks stay in the world and leave the template);
a deploy, or the undeploy of a blueprint, is refused with "not allowed" if any position is denied, and the player gets
no other message.

### Cost

Per capture or deploy, for a box of `c` chunk columns crossing `r` claims:

| | queries |
|---|---|
| before | one `EntityPlaceEvent` (or Common Protection API call) per block: up to 16.6 M for a 255³ capture |
| Open Parties and Claims | 2 map lookups per chunk: `2c` |
| Flan | one map lookup per position up to size 33 (at most 35 937), plus a permission check inside claims; above, the same per chunk column: at most `c` |
| Get Off My Lawn | 1 R-tree query + `r` permission checks |
| generic probe, size up to 33 | one event (or Common Protection API call) per tested position outside adapter claims: at most 35 937 |
| generic probe, size above 33 | at most `c` events (or Common Protection API calls); none in chunks covered by adapter claims |
| per block | one hash lookup and a few box tests, plus the probe event up to size 33 |

The largest capsule (255) spans at most 17 × 17 = 289 chunk columns. `ClaimTests.claimQueriesScaleWithChunksAndRegionsNotBlocks`
checks it with counting adapters and a counting probe: exactly one query per chunk for a chunk mod, one for a region mod
and one probe for the only chunk column they leave unclaimed on a 255³ box, and testing all its 16.6 M positions
queries nothing more; without adapter claims, testing every position of a 33³ box probes each of them (35 937), of a
34³ box each chunk column once, and 21 and 22 with 4 upgrades.

### The per-block dirt `EntityPlaceEvent` probe on NeoForge

Kept per block, outside adapter claims, for captures and deploys up to the largest survival capsule (33 by default): it stays exact for every mod listening
to the event, single protected blocks included. **Limited** above (OP capsules) to one event per chunk column,
outside adapter claims (and none in columns covered by them): at most 289 events for the largest capsule instead of
16.6 M. That stays exact for chunk claim mods without adapter (FTB Chunks, and Cadmus if it cancels the event); for box
based mods without adapter (YAWP regions) and single protected blocks it is approximate: a region or block not
containing the probe position is missed, a region containing it denies the whole column. On Fabric, the Common
Protection API call follows the same rule.

### Per block or per chunk column (measured)

A single block can be protected, so the owner asked for the generic probe per block if it stays within one server
tick (50 ms) for survival sizes (up to 31, the largest upgraded capsule). `ClaimProbeBenchmark` measures it: the probe
(`Platform.canPlaceBlock`, as a stranger) for every position of a full N³ box (the worst case: a capture probes the
blocks it takes, a deploy the blocks it places) and once per chunk column, outside claims and inside a claim of Flan
and of Open Parties and Claims covering the whole box (no adapter involved: it stands for a claim mod without adapter).
It is a GameTest of its own batch, registered only with `-PclaimBenchmark`:

```
flock /tmp/capsule-heavy.lock ./gradlew :neoforge:runGameTestServer -PclaimBenchmark [-PmodCompat]
flock /tmp/capsule-heavy.lock ./gradlew :fabric:runGameTestServer -PclaimBenchmark [-PmodCompat]
grep -a "claim probe benchmark" <loader>/runs/gameTestServer/logs/latest.log
```

Milliseconds per capture, median of 7 runs (3 for 255), per block / per chunk column, measured on 2026-10-06 on this
container (4 cores of a Xeon at 2.1 GHz, dev GameTest server, Java 21). Probes per block: 27, 1331, 29 791 and
16 581 375; per chunk column: 1, 2 to 4, 6 to 9 and 272 to 289, depending on where the box meets the chunk borders.

| Loader | Protection | 3 | 11 | 31 | 255 |
|---|---|---|---|---|---|
| NeoForge | no claim mod (SecurityCraft listens) | 0.02 / 0.00 | 0.68 / 0.01 | 6.41 / 0.04 | 2 305 / 0.53 |
| NeoForge | Flan and Open Parties and Claims loaded, no claim | 0.03 / 0.00 | 1.04 / 0.01 | 8.98 / 0.04 (other runs 10.8, 18.8, 20.7) | 4 215 / 0.58 |
| NeoForge | inside a Flan claim, every probe denied | 1.67 / 0.06 | 8.92 / 0.04 | **51.76** / 0.05 (other runs 49.43, 47.35) | 24 259 / 1.01 |
| NeoForge | inside an Open Parties and Claims claim | 0.01 / 0.00 | 0.36 / 0.00 | 6.85 / 0.02 | 4 230 / 0.58 |
| Fabric | no claim mod | 0.00 / 0.00 | 0.16 / 0.00 | 1.77 / 0.01 | 135 / 0.11 |
| Fabric | Flan and Open Parties and Claims loaded, no claim | 0.03 / 0.00 | 1.27 / 0.01 | 6.35 / 0.01 | 2 802 / 0.15 |
| Fabric | inside a Flan claim, every probe denied | 0.13 / 0.01 | 1.12 / 0.01 | 9.80 / 0.01 | 3 322 / 0.17 |
| Fabric | inside an Open Parties and Claims claim | 0.01 / 0.00 | 0.21 / 0.00 | 4.71 / 0.01 | 2 713 / 0.14 |

- Open Parties and Claims denied none of the probes: on NeoForge it lets this placement event through for a stranger,
  and on Fabric it does not implement Common Protection API. Its adapter is what protects its claims.
- Inside both claims at once, NeoForge 31: 51.5 ms. Denied events send the player packets (Flan's refusal): queued on
  the test player's connection instead of dropped (`CapsuleTestUtils.survivalPlayer` now drops them), 63.3 ms for 31,
  and the 255 box ran out of memory (4 GB).

Per block stays far below one tick in every case but one: a full 31³ capture inside a claim that denies through the
NeoForge placement event takes 47 to 52 ms (median of the three runs 49.4 ms): the probes alone fill the tick, and one
run in three goes over it. The 255 OP capture would take 2.3 to 4.2 s per block on NeoForge outside claims, 24 s inside
a denying claim, 0.1 to 3.3 s on Fabric.

**Decision (owner, round 2b L3): per block up to size 31, per chunk column above.** The worst case above (a claim mod
without adapter, a full 31³ capture inside its claim) is accepted; above 31 the probe stays per chunk column, to avoid
multi-second freezes with OP capsules.
Adapters: Open Parties and Claims per chunk, Get Off My Lawn exact boxes; Flan, whose public API answers per
position, by the same rule as the probe (per block up to 31, per chunk column above).

**Decision (owner): Flan and the generic probe check per chunk column above size 31: accepted, capsules that big are
admin-only (OP).**

**Decision (owner, before the 9.0 release): the limit is the largest capsule obtainable in survival**, computed from
the loaded recipes and config by `Claims.perBlockMaxSize`: the largest crafted tier (the shaped recipes of a
non-overpowered capsule, without empty tag ingredient: netherite 13 by default) plus `capsuleUpgradesLimit` × 2 (10
upgrades by default): 33. A pack adding a larger tier or more upgrades moves it. It chooses the predicate of
`Claims.denied` (and of the Flan adapter). A full 33³ box is 35 937 probes, 1.2 times the 31³ measured above.
The largest tier is read once per set of recipes (`/reload` loads new ones), from copies of the recipe results;
`capsuleUpgradesLimit` is read on each call.

**Decision (owner): no cap on the per block limit**, modpack makers choose their upgrade limit. The probes grow with the
cube of the size: from the measures above, 0.2 to 0.3 µs per probe outside claims and 1.7 µs inside a denying claim
on NeoForge (0.06 to 0.3 µs on Fabric). 20 upgrades (53, 148 877 probes, 5 times 31³): about 30 to 45 ms per
capture or deploy outside claims and 0.26 s inside a denying claim on NeoForge (10 to 30 ms and 50 ms on Fabric); 50
upgrades (113, 1.4 million probes, 48 times 31³): about 0.3 to 0.45 s and 2.5 s (0.1 to 0.3 s and 0.5 s on Fabric).
The server freezes for that time, once per capture or deploy of the largest capsules.

### Adapter cost (measured)

`ClaimProbeBenchmark` also times `Claims.denied` for the whole box, which asks the adapters (and probes the chunk
columns above 31), with Flan and Open Parties and Claims loaded (`-PmodCompat`). Milliseconds, median of 7 runs (3 for
255), measured on 2026-10-07 on the same container:

| Loader | Box | 3 | 11 | 31 | 255 |
|---|---|---|---|---|---|
| NeoForge | no claim | 0.04 | 0.36 | 0.86 | 3.37 |
| NeoForge | inside a Flan claim (29 791 positions denied at 31) | 0.11 | 0.55 | 4.39 | 1.51 |
| NeoForge | inside an Open Parties and Claims claim | 0.03 | 0.14 | 0.74 | 4.69 |
| Fabric | no claim | 0.01 | 0.13 | 0.77 | 1.30 |
| Fabric | inside a Flan claim (29 791 positions denied at 31) | 0.15 | 1.18 | 5.67 | 1.33 |
| Fabric | inside an Open Parties and Claims claim | 0.03 | 0.11 | 0.85 | 2.86 |

Flan per position stays below 6 ms for a full 31³ box inside a claim, an eighth of a tick, and about 1 ms per chunk
column above.

### Coupling and failures

- No runtime dependency for players: Capsule compiles against the public APIs (`compileOnly`, never shipped):
  `xaero.pac.common.server.api.OpenPACServerAPI`, `IServerClaimsManagerAPI`, `IChunkProtectionAPI`;
  `io.github.flemmli97.flan.api` (`ClaimHandler`, `IPermissionStorage`, `IPermissionContainer`, `BuiltinPermission`);
  `draylar.goml.api.ClaimUtils`, `ClaimBox`, `Claim` (and the R-tree `Selection` and `Entry` it returns); SecurityCraft's
  `IOwnable`. No reflection. The API signatures only use Minecraft types: `common` and `neoforge` compile against the
  NeoForge jars (Mojang names), `fabric` against the Fabric jars remapped by Loom (`modCompileOnly`), and the release
  jar refers to intermediary names like the rest of Capsule (checked with the GameTests of the release jars, below).
  Get Off My Lawn is Fabric only, so its adapter is in the `fabric` project; SecurityCraft has no Fabric build, its
  NeoForge jar compiles the common check, which never loads it on Fabric.
- Each adapter is a class of its own, created by a lambda only when its mod is loaded (`Claims.load`), so the JVM never
  resolves a missing mod's classes. `SecurityCraftOwnerCheck` calls `Owners`, which uses `IOwnable`, only when
  SecurityCraft is loaded.
- **Failures refuse, never allow** (owner decision): when a protection mod is loaded but Capsule cannot use its
  adapter, its claims are unknown, so every capture and deploy is refused until Capsule is updated, instead of
  ignoring the mod. The server never crashes.
  - An adapter that cannot be created when its mod loads is replaced by a marker refusing every capture and deploy,
    and one error line names the mod, its version and the error: "Captures and deploys are refused: Capsule cannot
    check the claims of Flan 1.12.8, its API was not found (java.lang.NoClassDefFoundError: …)".
  - An adapter that throws during a query refuses that capture or deploy and stays registered: the next one asks it
    again. The error and its stack trace are logged once per adapter, not per operation. A class or method renamed
    by a new mod version shows here, as a `LinkageError` (`NoSuchMethodError`, `NoClassDefFoundError`) on the first
    query.
  - The acting player is told in chat (`capsule.error.claimCheckFailed`: "Capsule cannot check the claims of Flan:
    captures and deploys are refused. Please report this incompatibility."). Dispensers and capture bases (fake
    players) tell nobody.
  - The adapters are asked before the capture or deploy changes the world (`Claims.denied` returns null when one
    fails), so a refused operation leaves the blocks, entities and capsule as they were.
  - `Claims.load(modId, factory)` loads the adapter of another mod the same way.

### Who is checked

- A player throwing a capsule or using an instant capsule: that player, or a fake player with their profile if they
  went offline before it landed (before: not checked). The thrower is the UUID the item entity saves, which
  `ItemEntity.getOwner` does not resolve once the player is offline or in another dimension.
- A capture base (`BlockEntityCapture`): it remembers the player who placed it (`placer` UUID in its saved data, set in
  `setPlacedBy`) and its captures and deploys are checked as a fake player with that profile, without chat feedback.
  A capture base deployed from a capsule (or blueprint) acts for the player who deployed it, so a captured base does
  not keep acting for its first owner.
- Nobody: vanilla dispensers, capture bases placed before Capsule 9.0 (no placer: re-placing the base gives it one) and
  any other capture or deploy without a player. Nobody may change a position inside an adapter claim, whatever the
  claim allows strangers or fake players. The generic probe asks for an anonymous fake player (`[Capsule]`, a fixed
  UUID that no claim lists as a member), so mods without adapter answer for a stranger. Outside claims nothing
  changes.
- Fake players: NeoForge `FakePlayerFactory.get`, Fabric API `FakePlayer.get` (`Platform.fakePlayer`), with the profile
  from the server profile cache. Flan treats a fake player whose profile is unknown to the server with its "fake player"
  permission.

### Not covered

- Open Parties and Claims block and entity exception lists (a claim allowing strangers to break some blocks): access is
  checked per chunk, so strangers are refused anyway.
- FTB Chunks and Cadmus on Fabric (they do not implement Common Protection API): an adapter can be added from the API
  above, but neither mod is on Modrinth, so it could not be tested in the GameTest runtimes.

## Tests

- `ClaimTests` (common GameTests, both loaders, every build): the counting test above; a test probe (`TestProbe`,
  answering the NeoForge placement event and Common Protection API in the GameTest mods) protecting one block of the
  bottom layer, away from the column centers, stays after a stranger's size 3 capture, and is not seen by the probe of a
  32 wide box (per chunk column, expected); a stranger's capture keeps the
  claimed blocks and their deploy is refused with one query each, the owner's are allowed; a capsule thrown by a player
  who then went offline is refused in a claim; capture bases are checked as the player who placed them, also offline;
  inside a claim open to everybody, a capture base placed before 9.0, a vanilla dispenser and a capture without player
  are refused, and outside it the base and the dispenser deploy; the placer is saved with the base and a deployed base
  acts for its deployer.
- `ClaimTests.refusedBlueprintsOnlyGiveTheClaimMessage`: a stranger's blueprint undeploy and deploy in a claim, refused
  by the claim or by an adapter that fails, leave the blocks and only tell "not allowed" or that the claims cannot be
  checked, not that the area does not match the blueprint.
- `ClaimTests`, batch `claimfailures` (their adapters refuse everywhere): with an adapter throwing on every query, a
  player's capture and deploy are refused, the block stays, the capsule stays empty, the adapter is asked again by the
  second operation and the player is told each time; with the marker of a mod whose API is missing (`Claims.load` with
  a failing factory), the captures of a player and without player are refused and the player is told.
- `OpenPartiesAndClaimsTests` and `FlanTests` (common GameTests, registered when the mod is loaded, `-PmodCompat` on
  both loaders, mods from the Modrinth maven): inside a real claim a stranger's capture and capture base are refused, a
  party or claim group member's and the owner's capture and the owner's capture base are allowed; a capture base placed
  before 9.0 and a vanilla dispenser are refused; outside the claim (the next chunk for Open Parties and Claims, the next
  blocks of the same chunk for Flan) the stranger's capture, a capture without player and a vanilla dispenser are
  allowed. `FlanTests.flanVetoesStrangersInClaimsReachingBelowTheWorld` runs the same with Flan's `defaultClaimDepth`
  -1. `FlanTests.flanIsAskedPerChunkColumnAboveTheLargestSurvivalCapsule`: a small Flan claim around the center of
  a chunk column denies only itself in a box as wide as the largest survival capsule (33), and the whole column in a
  box one wider.
- `GetOffMyLawnTests` (Fabric test mod, registered when Get Off My Lawn is loaded, release jar only): the same scenario
  in the claim of an anchor placed by the owner, with a trusted player as member; and a stranger is denied the claim in
  a 32 tall box, whose chunk column is probed above the claim. Get Off My Lawn also answers Common Protection API, so the
  generic probe alone passes the scenario up to the largest survival capsule: without the adapter, only the box one
  taller fails.
- The same GameTests run on the release jars with the real mods (`EXTRA_MODS` of `scripts/prod-gametest.sh`), so the
  API calls are checked in the remapped Fabric jar too. Get Off My Lawn runs there only: it nests 10 libraries (Cardinal
  Components, Polymer, sgui, placeholder-api, rtree, Common Protection API, …) that Fabric Loader loads from its jar on
  a real server but the Loom dev runs do not, so it cannot start in `runGameTestServer -PmodCompat`.
- The GameTest server has no profile cache (real servers do, and both mods need it): `GameTestProfiles` gives it an
  offline one before the server starts, and the scenario adds the test players to it, as players who joined before.
