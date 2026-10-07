package capsule.gametest;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.blocks.CapsuleBlocks;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.plugins.claims.ClaimAdapter;
import capsule.plugins.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;

/**
 * Claim checks through test adapters. Each adapter only answers for its own test area, so a test failing before it
 * unregisters its adapter cannot disturb the others.
 */
public class ClaimTests {

    /**
     * Claims one box of the test area for an owner, or for everybody when owner is null.
     */
    static class TestClaim implements ClaimAdapter {
        final BoundingBox area;
        final BoundingBox claimed;
        @Nullable
        final UUID owner;
        final AtomicInteger queries = new AtomicInteger();
        final List<UUID> askedFor = new ArrayList<>();

        TestClaim(GameTestHelper helper, BlockPos from, BlockPos to, @Nullable UUID owner) {
            this.area = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(8, 8, 8)));
            this.claimed = BoundingBox.fromCorners(helper.absolutePos(from), helper.absolutePos(to));
            this.owner = owner;
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
            if (!box.intersects(area)) return List.of();
            queries.incrementAndGet();
            askedFor.add(player.getUUID());
            return List.of(new Claim(claimed, owner == null || player.getUUID().equals(owner)));
        }
    }

    static int columns(BoundingBox box) {
        return ((box.maxX() >> 4) - (box.minX() >> 4) + 1) * ((box.maxZ() >> 4) - (box.minZ() >> 4) + 1);
    }

    static long countDenied(Predicate<BlockPos> denied, BoundingBox box) {
        long count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (denied.test(pos)) count++;
        }
        return count;
    }

    @GameTest(template = "empty", batch = "claimscale")
    public static void claimQueriesScaleWithChunksAndRegionsNotBlocks(GameTestHelper helper) {
        int size = CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
        BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
        BoundingBox box = BoundingBox.fromCorners(min, min.offset(size - 1, size - 1, size - 1));
        ChunkPos deniedChunk = new ChunkPos(min);
        ChunkPos unclaimedChunk = new ChunkPos(box.maxX() >> 4, box.maxZ() >> 4);
        AtomicInteger chunkQueries = new AtomicInteger();
        AtomicInteger regionQueries = new AtomicInteger();
        BoundingBox region = BoundingBox.fromCorners(min.offset(1, 0, 1), min.offset(40, 10, 40));
        BoundingBox subRegion = BoundingBox.fromCorners(min.offset(34, 0, 34), min.offset(36, 10, 36));
        // a chunk claim mod denying the first chunk and leaving the last one unclaimed, and a box claim mod denying a region except a sub-region
        ClaimAdapter chunks = new ClaimAdapter() {
            public String name() {
                return "chunks";
            }

            public List<Claim> claims(ServerLevel level, BoundingBox queried, ServerPlayer player) {
                if (!queried.equals(box)) return List.of();
                return ClaimAdapter.perChunk(level, queried, (x, z) -> {
                    chunkQueries.incrementAndGet();
                    return x == unclaimedChunk.x && z == unclaimedChunk.z ? null : x != deniedChunk.x || z != deniedChunk.z;
                });
            }
        };
        ClaimAdapter regions = new ClaimAdapter() {
            public String name() {
                return "regions";
            }

            public List<Claim> claims(ServerLevel level, BoundingBox queried, ServerPlayer player) {
                if (!queried.equals(box)) return List.of();
                regionQueries.incrementAndGet();
                return List.of(new Claim(region, false), new Claim(subRegion, true));
            }
        };
        Claims.register(chunks);
        Claims.register(regions);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        try (TestProbe probe = new TestProbe(box, Set.of())) {
            Predicate<BlockPos> denied = Claims.denied(helper.getLevel(), box, player);
            int chunkCount = columns(box);
            assertTrue(helper, chunkQueries.get() == chunkCount, "one query per chunk expected (" + chunkCount + "), got " + chunkQueries.get());
            assertTrue(helper, regionQueries.get() == 1, "one query for the regions expected, got " + regionQueries.get());
            assertTrue(helper, probe.queries.get() == 1, "one probe for the only chunk column without adapter claim expected, got " + probe.queries.get());

            assertTrue(helper, denied.test(min), "a denied chunk is denied");
            assertTrue(helper, !denied.test(min.offset(size - 1, 0, size - 1)), "an unclaimed chunk the probe allows is allowed");
            assertTrue(helper, denied.test(min.offset(30, 0, 30)), "a region denied by a mod is denied in a chunk allowed by another");
            assertTrue(helper, !denied.test(subRegion.getCenter()), "a sub-region overrides its region");

            long deniedCount = countDenied(denied, box);
            assertTrue(helper, deniedCount > 0 && chunkQueries.get() == chunkCount && regionQueries.get() == 1 && probe.queries.get() == 1,
                    "testing the " + box.getXSpan() * box.getYSpan() * box.getZSpan() + " positions queries nothing more");
        } finally {
            Claims.unregister(chunks);
            Claims.unregister(regions);
        }

        // without adapter claims, the probe asks every position up to the largest survival capsule, each chunk column above:
        // netherite (13) with every upgrade (2 each)
        int upgradeLimit = Config.upgradeLimit;
        try {
            for (int limit : new int[]{10, 4}) {
                Config.upgradeLimit = limit;
                int largest = 13 + 2 * limit;
                assertTrue(helper, Claims.perBlockMaxSize(helper.getLevel().getServer()) == largest, limit + " upgrades: the largest survival capsule is " + largest + ", not " + Claims.perBlockMaxSize(helper.getLevel().getServer()));
                for (int probedSize : new int[]{largest, largest + 1}) {
                    BoundingBox probed = BoundingBox.fromCorners(min.above(), min.above().offset(probedSize - 1, probedSize - 1, probedSize - 1));
                    try (TestProbe probe = new TestProbe(probed, Set.of())) {
                        countDenied(Claims.denied(helper.getLevel(), probed, player), probed);
                        int expected = probedSize <= largest ? probedSize * probedSize * probedSize : columns(probed);
                        assertTrue(helper, probe.queries.get() == expected, limit + " upgrades, size " + probedSize + ": " + expected + " probes expected, got " + probe.queries.get());
                    }
                }
            }
        } finally {
            Config.upgradeLimit = upgradeLimit;
            CapsuleTestUtils.removePlayer(player);
        }
        helper.succeed();
    }

    /**
     * A claim mod without adapter protecting one block, away from where its chunk column would be probed.
     */
    @GameTest(template = "empty", batch = "claimprobe")
    public static void aSingleProtectedBlockStaysUpToTheLargestSurvivalCapsule(GameTestHelper helper) {
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        BlockPos corner = new BlockPos(1, 1, 1);
        // on the bottom layer, never the center of a column, where the box would be probed per chunk column
        BlockPos protectedRelative = new BlockPos(2, 1, 1);
        BlockPos protectedBlock = helper.absolutePos(protectedRelative);
        CapsuleTestUtils.fill(helper, corner, corner.offset(2, 2, 2), Blocks.STONE.defaultBlockState());
        try (TestProbe probe = new TestProbe(new BoundingBox(protectedBlock), Set.of(protectedBlock))) {
            assertTrue(helper, Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(3), stranger, 3, helper.getLevel(), helper.absolutePos(corner)), "capture should run");
            helper.assertBlockPresent(Blocks.STONE, protectedRelative);
            helper.assertBlockNotPresent(Blocks.STONE, 3, 1, 1);

            // expected: above the largest survival capsule (OP capsules) the probe stays per chunk column and misses a single block
            int largest = Claims.perBlockMaxSize(helper.getLevel().getServer());
            BoundingBox overpowered = BoundingBox.fromCorners(protectedBlock, protectedBlock.offset(largest, largest, largest));
            assertTrue(helper, !Claims.denied(helper.getLevel(), overpowered, stranger).test(protectedBlock), "above " + largest + " a single protected block is not seen");
        } finally {
            CapsuleTestUtils.removePlayer(stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "claims")
    public static void claimsVetoCapturesAndDeploysOfOthers(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        ServerPlayer other = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 0), messages);
        helper.setBlock(1, 1, 5, Blocks.OAK_PLANKS);
        ItemStack planks = capture(helper, new BlockPos(1, 1, 5), 1);
        TestClaim claim = new TestClaim(helper, new BlockPos(1, 1, 1), new BlockPos(1, 4, 3), owner.getUUID());
        Claims.register(claim);
        try {
            CapsuleTestUtils.fill(helper, new BlockPos(1, 1, 1), new BlockPos(3, 1, 3), Blocks.STONE.defaultBlockState());
            ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
            assertTrue(helper, Capsule.captureAtPosition(capsule, other, 3, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "capture should run");
            assertTrue(helper, claim.queries.get() == 1, "a capture asks the claims once, got " + claim.queries.get());
            helper.assertBlockPresent(Blocks.STONE, 1, 1, 2);
            helper.assertBlockNotPresent(Blocks.STONE, 2, 1, 2);

            assertTrue(helper, !CapsuleTestUtils.deploy(helper, planks, new BlockPos(1, 1, 2), other), "deploy in the claim of another player is refused");
            assertTrue(helper, claim.queries.get() == 2, "a deploy asks the claims once, got " + claim.queries.get());
            assertTrue(helper, messages.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals("capsule.error.notAllowed")), "the player is told");
            assertTrue(helper, CapsuleTestUtils.deploy(helper, planks, new BlockPos(1, 1, 2), owner), "deploy in its own claim is allowed");
            helper.assertBlockPresent(Blocks.OAK_PLANKS, 1, 2, 2);

            ItemStack ownerCapsule = CapsuleTestUtils.emptyCapsule(3);
            assertTrue(helper, Capsule.captureAtPosition(ownerCapsule, owner, 3, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "capture should run");
            helper.assertBlockNotPresent(Blocks.STONE, 1, 1, 2);
        } finally {
            Claims.unregister(claim);
            CapsuleTestUtils.removePlayer(owner);
            CapsuleTestUtils.removePlayer(other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "claims")
    public static void capsulesThrownByOfflinePlayersAreChecked(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        ServerPlayer thrower = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 0));
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        TestClaim claim = new TestClaim(helper, new BlockPos(0, 1, 0), new BlockPos(8, 4, 8), owner.getUUID());
        Claims.register(claim);
        try {
            Vec3 pos = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
            ItemEntity thrown = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, capsule);
            thrown.setThrower(thrower);
            CapsuleTestUtils.removePlayer(thrower);

            Capsule.handleItemEntityOnGround(thrown, capsule);
            helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
            assertTrue(helper, claim.askedFor.contains(thrower.getUUID()), "the claims are asked for the offline thrower");
        } finally {
            Claims.unregister(claim);
            CapsuleTestUtils.removePlayer(owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "claims")
    public static void failedDeploysAreRolledBackInClaims(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        TestClaim claim = new TestClaim(helper, new BlockPos(0, 1, 0), new BlockPos(8, 4, 8), owner.getUUID());
        Claims.register(claim);
        // throws once the blocks are placed, as a modded block crashing during the deploy
        StructurePlaceSettings crashing = new StructurePlaceSettings() {
            @Override
            public boolean getKnownShape() {
                throw new IllegalStateException("test crash");
            }
        };
        try {
            assertTrue(helper, !StructureSaver.deploy(capsule, helper.getLevel(), owner, helper.absolutePos(new BlockPos(4, 1, 4)), crashing), "the deploy fails");
            helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
            assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(4, 0, 4), owner), "the capsule keeps its content");
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
        } finally {
            Claims.unregister(claim);
            CapsuleTestUtils.removePlayer(owner);
        }
        helper.succeed();
    }

    static List<String> keys(List<Component> messages) {
        return messages.stream().map(m -> m.getContents() instanceof TranslatableContents t ? t.getKey() : m.getString()).toList();
    }

    /**
     * Blueprint undeploys and deploys refused by the claims, or because they cannot be checked, only say so.
     */
    @GameTest(template = "empty", batch = "claims")
    public static void refusedBlueprintsOnlyGiveTheClaimMessage(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        List<Component> messages = new ArrayList<>();
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 0), messages);
        helper.setBlock(1, 1, 1, Blocks.STONE);
        ItemStack blueprint = Capsule.newLinkedCapsuleItemStack(CapsuleItem.getStructureName(capture(helper, new BlockPos(1, 1, 1), 1)), 0, 0, 1, false, null, 0);
        CapsuleItem.setBlueprint(blueprint);
        CapsuleItem.setState(blueprint, CapsuleState.DEPLOYED);
        CapsuleItem.duplicateBlueprintTemplate(blueprint, helper.getLevel(), owner);
        owner.getInventory().add(new ItemStack(Items.STONE));
        Capsule.reloadBlueprint(blueprint, helper.getLevel(), owner);
        TestClaim claim = new TestClaim(helper, new BlockPos(0, 1, 0), new BlockPos(8, 4, 8), owner.getUUID());
        BoundingBox area = claim.area;
        ClaimAdapter failing = new ClaimAdapter() {
            public String name() {
                return "failing";
            }

            public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
                if (!box.intersects(area)) return List.of();
                throw new IllegalStateException("test.ClaimApi.claims()");
            }
        };
        Claims.register(claim);
        try {
            assertTrue(helper, CapsuleTestUtils.deploy(helper, blueprint, new BlockPos(4, 0, 4), owner), "the owner deploys the blueprint in their claim");
            for (ClaimAdapter refusing : List.of(claim, failing)) {
                if (refusing == failing) Claims.register(failing);
                messages.clear();
                Capsule.resentToCapsule(blueprint, helper.getLevel(), stranger);
                helper.assertBlockPresent(Blocks.STONE, 4, 1, 4);
                assertTrue(helper, CapsuleItem.hasState(blueprint, CapsuleState.DEPLOYED), "the blueprint stays deployed");
                String expected = refusing == claim ? "capsule.error.notAllowed" : "capsule.error.claimCheckFailed";
                assertTrue(helper, keys(messages).equals(List.of(expected)), "an undeploy refused by " + refusing.name() + " only tells " + expected + ", got " + keys(messages));
            }
            Claims.unregister(failing);
            Capsule.resentToCapsule(blueprint, helper.getLevel(), owner);
            helper.assertBlockNotPresent(Blocks.STONE, 4, 1, 4);
            assertTrue(helper, CapsuleItem.hasState(blueprint, CapsuleState.BLUEPRINT), "the owner undeploys the blueprint");

            for (ClaimAdapter refusing : List.of(claim, failing)) {
                if (refusing == failing) Claims.register(failing);
                messages.clear();
                assertTrue(helper, !CapsuleTestUtils.deploy(helper, blueprint, new BlockPos(4, 0, 4), stranger), "the deploy is refused by " + refusing.name());
                String expected = refusing == claim ? "capsule.error.notAllowed" : "capsule.error.claimCheckFailed";
                assertTrue(helper, keys(messages).equals(List.of(expected)), "a deploy refused by " + refusing.name() + " only tells " + expected + ", got " + keys(messages));
            }
        } finally {
            Claims.unregister(claim);
            Claims.unregister(failing);
            CapsuleTestUtils.removePlayer(owner);
            CapsuleTestUtils.removePlayer(stranger);
        }
        helper.succeed();
    }

    static long claimCheckFailures(List<Component> messages, String mod) {
        return messages.stream().filter(m -> m.getContents() instanceof TranslatableContents t
                && t.getKey().equals("capsule.error.claimCheckFailed") && t.getArgs().length == 1 && mod.equals(t.getArgs()[0])).count();
    }

    /**
     * A protection mod whose API changed: its adapter throws. It refuses everything, so it has a batch of its own.
     */
    @GameTest(template = "empty", batch = "claimfailures")
    public static void failingClaimChecksRefuseCapturesAndDeploys(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8), messages);
        helper.setBlock(1, 1, 5, Blocks.OAK_PLANKS);
        ItemStack planks = capture(helper, new BlockPos(1, 1, 5), 1);
        AtomicInteger queries = new AtomicInteger();
        ClaimAdapter failing = new ClaimAdapter() {
            public String name() {
                return "failing";
            }

            public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer actor) {
                queries.incrementAndGet();
                throw new NoSuchMethodError("test.ClaimApi.claims()");
            }
        };
        Claims.register(failing);
        try {
            helper.setBlock(1, 1, 1, Blocks.STONE);
            ItemStack capsule = CapsuleTestUtils.emptyCapsule(1);
            assertTrue(helper, !Capsule.captureAtPosition(capsule, player, 1, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "the capture is refused");
            helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "the capsule stays empty");

            assertTrue(helper, !CapsuleTestUtils.deploy(helper, planks, new BlockPos(4, 0, 4), player), "the deploy is refused");
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, 4, 1, 4);
            assertTrue(helper, queries.get() == 2, "the adapter stays registered and is asked again, got " + queries.get() + " queries");
            assertTrue(helper, claimCheckFailures(messages, "failing") == 2, "the player is told each time, got " + messages);
        } finally {
            Claims.unregister(failing);
            CapsuleTestUtils.removePlayer(player);
        }
        helper.succeed();
    }

    /**
     * A protection mod loaded without the API its adapter needs.
     */
    @GameTest(template = "empty", batch = "claimfailures")
    public static void protectionModsWithoutTheirApiRefuseCapturesAndDeploys(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8), messages);
        ClaimAdapter unusable = Claims.load(CapsuleMod.MODID, () -> {
            throw new NoClassDefFoundError("test/ClaimApi");
        });
        try {
            assertTrue(helper, unusable != null, "a loaded mod without its API is registered as unusable");
            CapsuleTestUtils.fill(helper, new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), Blocks.STONE.defaultBlockState());
            assertTrue(helper, !Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), player, 1, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "the capture is refused");
            assertTrue(helper, !Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), null, 1, helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1))), "the capture without player is refused");
            helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
            helper.assertBlockPresent(Blocks.STONE, 2, 1, 1);
            assertTrue(helper, claimCheckFailures(messages, unusable.name()) == 1, "the player is told, got " + messages);
        } finally {
            Claims.unregister(unusable);
            CapsuleTestUtils.removePlayer(player);
        }
        helper.succeed();
    }

    static BlockEntityCapture captureBase(GameTestHelper helper, BlockPos pos, @Nullable ServerPlayer placer) {
        helper.setBlock(pos, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        if (placer != null) {
            helper.getBlockState(pos).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), placer, ItemStack.EMPTY);
        }
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty", batch = "claims", timeoutTicks = 200)
    public static void captureBasesActAsThePlayerWhoPlacedThem(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        ServerPlayer other = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 0));
        BlockPos ownerBase = new BlockPos(1, 1, 2);
        BlockPos otherBase = new BlockPos(4, 1, 2);
        for (BlockPos pos : List.of(ownerBase, otherBase)) {
            helper.setBlock(pos.north(2), Blocks.STONE);
            ItemStack capsule = capture(helper, pos.north(2), 1);
            captureBase(helper, pos, pos == ownerBase ? owner : other).setItem(0, capsule);
        }
        TestClaim claim = new TestClaim(helper, new BlockPos(0, 2, 2), new BlockPos(8, 2, 2), owner.getUUID());
        Claims.register(claim);
        BlockEntityCapture placed = helper.getBlockEntity(ownerBase);
        assertTrue(helper, owner.getUUID().equals(placed.getPlacer()), "the base remembers who placed it");
        BlockEntityCapture reloaded = (BlockEntityCapture) BlockEntityCapture.loadStatic(placed.getBlockPos(), placed.getBlockState(),
                placed.saveWithId(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        assertTrue(helper, reloaded != null && owner.getUUID().equals(reloaded.getPlacer()), "the placer is saved with the base");
        reloaded.setRemoved();
        CapsuleTestUtils.removePlayer(owner);
        CapsuleTestUtils.removePlayer(other);

        helper.startSequence()
                .thenExecute(() -> List.of(ownerBase, otherBase).forEach(pos -> helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK)))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.STONE, ownerBase.above()))
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertBlockNotPresent(Blocks.STONE, otherBase.above());
                    BlockEntityCapture refused = helper.getBlockEntity(otherBase);
                    assertTrue(helper, CapsuleItem.hasState(refused.getItem(0), CapsuleState.LINKED), "the refused capsule stays linked");
                    assertTrue(helper, claim.askedFor.contains(owner.getUUID()) && claim.askedFor.contains(other.getUUID()), "bases are checked as their placer, even offline");
                    assertTrue(helper, claim.queries.get() == 2, "each base asks the claims once, got " + claim.queries.get());
                    Claims.unregister(claim);
                })
                .thenSucceed();
    }

    static DispenserBlockEntity dispenser(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty", batch = "claims", timeoutTicks = 200)
    public static void capsulesUsedByNobodyAreRefusedInClaims(GameTestHelper helper) {
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        BlockPos legacyBaseInside = new BlockPos(1, 1, 2);
        BlockPos dispenserInside = new BlockPos(4, 1, 2);
        BlockPos legacyBaseOutside = new BlockPos(1, 1, 6);
        BlockPos dispenserOutside = new BlockPos(4, 1, 6);
        List<BlockPos> machines = List.of(legacyBaseInside, dispenserInside, legacyBaseOutside, dispenserOutside);
        for (BlockPos pos : machines) {
            helper.setBlock(pos.north(2), Blocks.STONE);
            ItemStack capsule = capture(helper, pos.north(2), 1);
            (pos.getX() == 1 ? captureBase(helper, pos, null) : dispenser(helper, pos)).setItem(0, capsule);
        }
        // everybody may build in this claim, but nobody
        TestClaim claim = new TestClaim(helper, new BlockPos(0, 2, 0), new BlockPos(8, 2, 3), null);
        Claims.register(claim);
        helper.setBlock(7, 2, 0, Blocks.STONE);
        helper.setBlock(7, 2, 2, Blocks.STONE);
        Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), stranger, 1, helper.getLevel(), helper.absolutePos(new BlockPos(7, 2, 0)));
        Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), null, 1, helper.getLevel(), helper.absolutePos(new BlockPos(7, 2, 2)));
        CapsuleTestUtils.removePlayer(stranger);
        helper.assertBlockNotPresent(Blocks.STONE, 7, 2, 0);
        helper.assertBlockPresent(Blocks.STONE, 7, 2, 2);

        helper.startSequence()
                .thenExecute(() -> machines.forEach(pos -> helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK)))
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, legacyBaseOutside.above());
                    helper.assertBlockPresent(Blocks.STONE, dispenserOutside.above());
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    Claims.unregister(claim);
                    for (BlockPos pos : List.of(legacyBaseInside, dispenserInside)) {
                        helper.assertBlockNotPresent(Blocks.STONE, pos.above());
                        DispenserBlockEntity machine = helper.getBlockEntity(pos);
                        assertTrue(helper, CapsuleItem.hasState(machine.getItem(0), CapsuleState.LINKED), "the refused capsule stays linked");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "claims")
    public static void deployedCaptureBasesActAsTheirDeployer(GameTestHelper helper) {
        ServerPlayer placer = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        ServerPlayer deployer = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 0));
        captureBase(helper, new BlockPos(1, 1, 1), placer);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(5, 0, 5), deployer), "deploy should succeed");
        BlockEntityCapture deployed = helper.getBlockEntity(new BlockPos(5, 1, 5));
        assertTrue(helper, deployer.getUUID().equals(deployed.getPlacer()), "a deployed base acts for its deployer, not " + deployed.getPlacer());
        CapsuleTestUtils.removePlayer(placer);
        CapsuleTestUtils.removePlayer(deployer);
        helper.succeed();
    }
}
