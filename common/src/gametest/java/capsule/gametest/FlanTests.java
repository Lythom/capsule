package capsule.gametest;

import capsule.plugins.claims.Claims;
import io.github.flemmli97.flan.api.permission.BuiltinPermission;
import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.ClaimStorage;
import io.github.flemmli97.flan.config.ConfigHandler;
import io.github.flemmli97.flan.player.ClaimMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.stream.Stream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Flan (-PmodCompat): box claims, a claim group may build.
 */
public class FlanTests {

    @GameTest(template = "empty17", batch = "flan", timeoutTicks = 200)
    public static void flanVetoesStrangers(GameTestHelper helper) {
        vetoesStrangers(helper, () -> {});
    }

    /**
     * With defaultClaimDepth -1, Flan's claims reach 10 blocks below the world. Its own batch: the config is global.
     */
    @GameTest(template = "empty17", batch = "flandepth", timeoutTicks = 200)
    public static void flanVetoesStrangersInClaimsReachingBelowTheWorld(GameTestHelper helper) {
        int depth = ConfigHandler.CONFIG.defaultClaimDepth;
        ConfigHandler.CONFIG.defaultClaimDepth = -1;
        vetoesStrangers(helper, () -> ConfigHandler.CONFIG.defaultClaimDepth = depth);
    }

    private static void vetoesStrangers(GameTestHelper helper, Runnable restore) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 0));
        ServerPlayer member = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 16));
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(16, 1, 0));
        ClaimStorage storage = ClaimStorage.get(helper.getLevel());
        Claim claim = storage.createAdminClaim(helper.absolutePos(new BlockPos(2, 0, 2)), helper.absolutePos(new BlockPos(10, 0, 10)), helper.getLevel(), false);
        assertTrue(helper, storage.transferOwner(claim, owner.getUUID()), "the claim should be given to its owner");
        claim.editPerms(owner, "builders", BuiltinPermission.BREAK, 1);
        claim.setPlayerGroup(member.getUUID(), "builders", true);

        // outside the claim box but next to it, in a chunk it may share
        new ClaimModScenario(owner, member, stranger, new BlockPos(3, 1, 3), new BlockPos(5, 1, 3), new BlockPos(7, 1, 3), new BlockPos(12, 1, 12),
                new BlockPos(3, 1, 7), new BlockPos(8, 1, 7), new BlockPos(3, 1, 10), new BlockPos(8, 1, 10), new BlockPos(12, 1, 15))
                .run(helper, () -> {
                    storage.deleteClaim(claim, true, ClaimMode.DEFAULT, helper.getLevel());
                    Stream.of(owner, member, stranger).forEach(CapsuleTestUtils::removePlayer);
                    restore.run();
                });
    }

    /**
     * Flan's API answers per position: it is asked for each position up to the largest survival capsule, above for the
     * center of each chunk column. Its own batch: the boxes reach the areas of other tests.
     */
    @GameTest(template = "empty17", batch = "flangranularity")
    public static void flanIsAskedPerChunkColumnAboveTheLargestSurvivalCapsule(GameTestHelper helper) {
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        // boxes from a chunk corner: the center of their first chunk column is 8 blocks away
        BlockPos min = new ChunkPos(helper.absolutePos(new BlockPos(8, 1, 8))).getWorldPosition().atY(helper.absolutePos(BlockPos.ZERO).getY() + 1);
        BlockPos center = min.offset(8, Claims.PER_BLOCK_MAX_SIZE / 2 + 1, 8);
        BlockPos outside = center.west(8).north(8);
        ClaimStorage storage = ClaimStorage.get(helper.getLevel());
        Claim claim = storage.createAdminClaim(center.offset(-1, 0, -1), center.offset(1, 0, 1), helper.getLevel(), false);
        try {
            BoundingBox survival = BoundingBox.fromCorners(min, min.offset(Claims.PER_BLOCK_MAX_SIZE - 1, Claims.PER_BLOCK_MAX_SIZE - 1, Claims.PER_BLOCK_MAX_SIZE - 1));
            assertTrue(helper, Claims.denied(helper.getLevel(), survival, stranger).test(center), "up to " + Claims.PER_BLOCK_MAX_SIZE + " the claim is denied");
            assertTrue(helper, !Claims.denied(helper.getLevel(), survival, stranger).test(outside), "up to " + Claims.PER_BLOCK_MAX_SIZE + " the rest of its chunk column is allowed");
            BoundingBox overpowered = BoundingBox.fromCorners(min, min.offset(Claims.PER_BLOCK_MAX_SIZE, Claims.PER_BLOCK_MAX_SIZE, Claims.PER_BLOCK_MAX_SIZE));
            assertTrue(helper, Claims.denied(helper.getLevel(), overpowered, stranger).test(outside), "above " + Claims.PER_BLOCK_MAX_SIZE + " the chunk column whose center is claimed is denied");
        } finally {
            storage.deleteClaim(claim, true, ClaimMode.DEFAULT, helper.getLevel());
            CapsuleTestUtils.removePlayer(stranger);
        }
        helper.succeed();
    }

    /**
     * Claims the columns of box for owner, until the returned task runs.
     */
    static Runnable claim(GameTestHelper helper, BoundingBox box, ServerPlayer owner) {
        ClaimStorage storage = ClaimStorage.get(helper.getLevel());
        Claim claim = storage.createAdminClaim(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.minY(), box.maxZ()), helper.getLevel(), false);
        storage.transferOwner(claim, owner.getUUID());
        return () -> storage.deleteClaim(claim, true, ClaimMode.DEFAULT, helper.getLevel());
    }
}
