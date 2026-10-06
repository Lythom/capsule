package capsule.gametest;

import io.github.flemmli97.flan.api.permission.BuiltinPermission;
import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.ClaimStorage;
import io.github.flemmli97.flan.player.ClaimMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Stream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Flan (-PmodCompat): box claims, a claim group may build.
 */
public class FlanTests {

    @GameTest(template = "empty17", batch = "flan", timeoutTicks = 200)
    public static void flanVetoesStrangers(GameTestHelper helper) {
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
                new BlockPos(3, 1, 7), new BlockPos(8, 1, 7))
                .run(helper, () -> {
                    storage.deleteClaim(claim, true, ClaimMode.DEFAULT, helper.getLevel());
                    Stream.of(owner, member, stranger).forEach(CapsuleTestUtils::removePlayer);
                });
    }
}
