package capsule.gametest;

import capsule.plugins.claims.Claims;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.stream.Stream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Get Off My Lawn (Fabric, release jar servers only: the dev runs do not load the mods nested in its jar): boxes around
 * claim anchors, trusted players may build.
 */
public class GetOffMyLawnTests {

    @GameTest(template = "empty17", batch = "getoffmylawn", timeoutTicks = 200)
    public static void getOffMyLawnVetoesStrangers(GameTestHelper helper) {
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 16));
        ServerPlayer member = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 16));
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(16, 1, 0));

        // placed by the owner like a player does, its claim covers 4 blocks around it (5 towards positive coordinates)
        BlockPos anchor = new BlockPos(4, 1, 4);
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("goml", "makeshift_claim_anchor"));
        int radius = GetOffMyLawn.CONFIG.makeshiftRadius;
        GetOffMyLawn.CONFIG.makeshiftRadius = 4;
        helper.setBlock(anchor, block);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(anchor), block.defaultBlockState(), owner, new ItemStack(block));
        GetOffMyLawn.CONFIG.makeshiftRadius = radius;
        var claims = ClaimUtils.getClaimsWithOrigin(helper.getLevel(), helper.absolutePos(anchor));
        assertTrue(helper, claims.count() == 1, "the anchor should claim its area");
        claims.forEach(claim -> claim.getValue().trust(member.getUUID()));

        // above the largest survival capsule, mods without adapter are probed at the center of each chunk column, here
        // above the claim
        BlockPos claimed = helper.absolutePos(new BlockPos(2, 1, 2));
        assertTrue(helper, Claims.denied(helper.getLevel(), BoundingBox.fromCorners(claimed, claimed.above(Claims.PER_BLOCK_MAX_SIZE)), stranger).test(claimed),
                "above " + Claims.PER_BLOCK_MAX_SIZE + " the claim should still be denied to strangers");

        new ClaimModScenario(owner, member, stranger, new BlockPos(2, 1, 2), new BlockPos(4, 1, 2), new BlockPos(6, 1, 2), new BlockPos(13, 1, 13),
                new BlockPos(2, 1, 6), new BlockPos(6, 1, 6), new BlockPos(2, 1, 8), new BlockPos(6, 1, 8), new BlockPos(13, 1, 16))
                .run(helper, () -> {
                    helper.setBlock(anchor, Blocks.AIR);
                    Stream.of(owner, member, stranger).forEach(CapsuleTestUtils::removePlayer);
                });
    }
}
