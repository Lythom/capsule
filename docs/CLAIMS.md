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
the capsule's box, once per chunk or per claim, through its own API: one small adapter per mod, weakly coupled
(reflection, used only when the mod is loaded), like `SecurityCraftOwnerCheck`.

## Mods

Downloads: Modrinth API (`/v2/project`, `/v2/search`), all versions. FTB Chunks and Cadmus are not on Modrinth; their
CurseForge counts were not collected (CurseForge is not scraped).

| Mod | Loaders (1.21.1) | Downloads | Claims | Query API (jar checked) | Query cost | Listens to |
|---|---|---|---|---|---|---|
| Open Parties and Claims | NeoForge, Fabric | 23.0 M | chunk columns, owner + party + allies | `OpenPACServerAPI.get(MinecraftServer)`; `.getServerClaimsManager().get(ResourceLocation dim, int chunkX, int chunkZ)` → `IPlayerChunkClaimAPI` or null; `.getChunkProtection().hasChunkAccess(UUID, ResourceLocation, int, int)` (0.32.7, both loaders) | two map lookups per chunk, by player id | NeoForge `EntityPlaceEvent`, `BreakEvent`; Fabric: own mixins, no Common Protection API |
| Yet Another World Protector | NeoForge, Fabric | 267 k | admin regions (boxes) with flags | `FlagEvaluator.processCheck(FlagCheckRequest)`, per position (0.6.3-beta4) | per position | NeoForge `EntityPlaceEvent`, `BreakEvent` |
| Flan | NeoForge, Fabric | 249 k | boxes (claims from a minimum Y up, sub-claims), permission groups | API: `ClaimHandler.canInteract(ServerPlayer, BlockPos, ResourceLocation)`, per position. Public internals: `ClaimStorage.get(ServerLevel).getClaimsAt(int chunkX, int chunkZ)` → `List<Claim>`; `Claim.getDimensions()` → `ClaimBox(minX, minY, minZ, maxX, maxY, maxZ)`; `Claim.getAllSubclaims()`; `Claim.canInteract(ServerPlayer, ResourceLocation, BlockPos, boolean message)`; `BuiltinPermission.BREAK` (1.12.8, both loaders) | one map lookup per chunk, one permission check per claim | NeoForge `BreakEvent`, `EntityPlaceEvent`, `EntityMultiPlaceEvent`; Fabric: Common Protection API provider |
| Hey That's Mine | Fabric | 71 k | container locks, not claims | – | – | – |
| Get Off My Lawn ReServed | Fabric | 63 k | boxes around claim anchors, in an R-tree | `ClaimUtils.getClaimsInBox(LevelReader, BlockPos, BlockPos)` → `Selection<Entry<ClaimBox, Claim>>`; `ClaimBox.minecraftBox()` → `AABB`; `Claim.hasPermission(UUID)`; `ClaimUtils.isInAdminMode(Player)` (1.13.1+1.21) | one R-tree query, one check per claim | Common Protection API provider |
| FTB Chunks | NeoForge, Fabric | not on Modrinth | chunk columns, teams | `FTBChunksAPI.api().getManager().getChunk(ChunkDimPos)` → `ClaimedChunk` or null; `.getTeamData().isTeamMember(UUID)`, `isAlly(UUID)`, `canPlayerUse(ServerPlayer, PrivacyProperty)`; `ClaimedChunkManager.shouldPreventInteraction(Entity, InteractionHand, BlockPos, Protection, Entity)` (NeoForge 2101.1.22, maven.ftb.dev) | one lookup per chunk | Architectury block events: NeoForge `EntityPlaceEvent`, `BreakEvent` |
| Cadmus (Argonauts teams) | NeoForge, Fabric | not on Modrinth | chunk columns | not checked: no jar on a public maven | – | – |
| Common Protection API | Fabric library | (maven.nucleoid.xyz) | – | `CommonProtection.canPlaceBlock` / `canBreakBlock(Level, BlockPos, GameProfile, Player)` per position; `isAreaProtected(Level, AABB)` for anyone, not per player (1.0.0) | per position, every provider | implemented by Flan and Get Off My Lawn, not by Open Parties and Claims or FTB Chunks |

## Chosen approach

`capsule.plugins.claims.Claims.denied(level, box, player)` returns a test of the positions the player may not change,
computed before the capture or deploy touches any block:

1. **Adapters** (`ClaimAdapter`), loaded on first use when their mod id is loaded, their API resolved once by reflection:
   - `OpenPartiesAndClaimsAdapter` (both loaders): per chunk of the box, the chunk claim and `hasChunkAccess` by player id.
   - `FlanAdapter` (both loaders): the claims of each chunk of the box, then one `canInteract(player, BREAK, …)` per claim
     and sub-claim intersecting the box. A claim is asked at a position outside it, so that it answers for itself and
     not for one of its sub-claims.
   - `GetOffMyLawnAdapter` (Fabric): one `getClaimsInBox`, one `hasPermission` per claim (all allowed in admin mode).

   Each returns claims as boxes with an allowed flag. Within a mod the last claim containing a position decides
   (sub-claims come after their claim); across mods any refusal wins. `Claims.register` lets another mod add an adapter.
2. **Generic probe** for mods without adapter: one query per chunk column of the box, at the column's center or a corner
   that no adapter claim covers, through the loader hook kept from before (`Platform.canPlaceBlock`: a dirt
   `EntityPlaceEvent` on NeoForge, `CommonProtection.canPlaceBlock` on Fabric). Columns fully covered by adapter claims
   are not probed. The answer applies to the column's positions outside adapter claims.
3. The test of a position is a chunk lookup in the claims and probe results: no query.

The capture removes only the allowed positions, as before (protected blocks stay in the world and leave the template);
a deploy is refused with "not allowed" if any position is denied.

### Cost

Per capture or deploy, for a box of `c` chunk columns crossing `r` claims:

| | queries |
|---|---|
| before | one `EntityPlaceEvent` (or Common Protection API call) per block: up to 16.6 M for a 255³ capture |
| Open Parties and Claims | 2 map lookups per chunk: `2c` |
| Flan | `c` map lookups + `r` permission checks |
| Get Off My Lawn | 1 R-tree query + `r` permission checks |
| generic probe | at most `c` events (or Common Protection API calls); none in chunks covered by adapter claims |
| per block | one hash lookup and a few box tests, no event |

The largest capsule (255) spans at most 17 × 17 = 289 chunk columns. `ClaimTests.claimQueriesScaleWithChunksAndRegionsNotBlocks`
checks it with counting adapters: exactly one query per chunk for a chunk mod and one for a region mod on a 255³ box,
and testing all its 16.6 M positions queries nothing more.

### The per-block dirt `EntityPlaceEvent` probe on NeoForge

**Limited** to one event per chunk column, outside adapter claims (and none in columns covered by them). Cost: at most
289 events for the largest capsule instead of one per block. It stays exact for chunk claim mods without adapter (FTB
Chunks, and Cadmus if it cancels the event) and keeps vanilla-like protections that listen to the event. For box based
mods without adapter (YAWP regions) it is approximate: a region not containing the probe position is missed, a region
containing it denies the whole column. On Fabric, the Common Protection API call follows the same rule.

### Weak coupling and failures

- No Gradle or runtime dependency for players: adapters use reflection on class and method names verified above;
  Minecraft types in the signatures are class literals, so the lookups also work on Fabric's intermediary names.
- An adapter whose API is not found is not loaded (one error line: "Captures and deploys ignore the claims of …").
- An adapter that throws during a query is removed (one error line: "Captures and deploys now ignore the claims of …")
  and that one operation is refused; the server never crashes. A renamed method in a future mod version therefore
  disables its adapter until Capsule is updated; the generic probe still covers the mod on NeoForge.

### Who is checked

- A player throwing a capsule or using an instant capsule: that player, or a fake player with their profile if they
  went offline before it landed (before: not checked).
- A capture base (`BlockEntityCapture`): it remembers the player who placed it (`placer` UUID in its saved data, set in
  `setPlacedBy`) and its captures and deploys are checked as a fake player with that profile, without chat feedback.
  A capture base deployed from a capsule (or blueprint) acts for the player who deployed it, so a captured base does
  not keep acting for its first owner. Bases placed before Capsule 9.0 have no placer and are not checked, as before;
  vanilla dispensers neither.
- Fake players: NeoForge `FakePlayerFactory.get`, Fabric API `FakePlayer.get` (`Platform.fakePlayer`), with the profile
  from the server profile cache. Flan treats a fake player whose profile is unknown to the server with its "fake player"
  permission.

### Not covered

- Open Parties and Claims block and entity exception lists (a claim allowing strangers to break some blocks): access is
  checked per chunk, so strangers are refused anyway.
- FTB Chunks and Cadmus on Fabric (they do not implement Common Protection API): an adapter can be added from the API
  above, but neither mod is on Modrinth, so it could not be tested in the GameTest runtimes.
- Vanilla dispensers and capture bases placed before 9.0 act for nobody: claims do not apply to them.

## Tests

- `ClaimTests` (common GameTests, both loaders, every build): the counting test above; a stranger's capture keeps the
  claimed blocks and their deploy is refused with one query each, the owner's are allowed; capture bases are checked as
  the player who placed them, also offline, legacy bases are not checked; the placer is saved with the base and a
  deployed base acts for its deployer.
- `OpenPartiesAndClaimsTests` and `FlanTests` (common GameTests, registered when the mod is loaded, `-PmodCompat` on
  both loaders, mods from the Modrinth maven): inside a real claim a stranger's capture and capture base are refused, a
  party or claim group member's and the owner's capture and the owner's capture base are allowed; outside the claim (the
  next chunk for Open Parties and Claims, the next blocks of the same chunk for Flan) the stranger's capture is allowed.
- Get Off My Lawn: its adapter is checked against the jar with `javap` only. Its dev runtime fails: the mods it nests
  (Cardinal Components, Polymer, …) are not loaded from its jar by the Loom dev runs, and most are not on Modrinth.
- The GameTest server has no profile cache (real servers do, and both mods need it): `GameTestProfiles` gives it an
  offline one before the server starts, and the scenario adds the test players to it, as players who joined before.
