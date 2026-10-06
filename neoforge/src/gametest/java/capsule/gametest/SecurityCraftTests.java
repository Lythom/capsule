package capsule.gametest;

import capsule.helpers.Capsule;
import capsule.plugins.securitycraft.SecurityCraftOwnerCheck;
import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class SecurityCraftTests {

    private static final BlockPos BLOCK = new BlockPos(1, 1, 1);

    private static ServerPlayer placeOwnedBlock(GameTestHelper helper) {
        Block reinforcedStone = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("securitycraft", "reinforced_stone"));
        helper.setBlock(BLOCK, reinforcedStone);
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        IOwnable ownable = helper.getBlockEntity(BLOCK);
        ownable.setOwner(owner.getStringUUID(), owner.getName().getString());
        return owner;
    }

    private static ItemStack captureAs(GameTestHelper helper, ServerPlayer player) {
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(1);
        Capsule.captureAtPosition(capsule, player, 1, helper.getLevel(), helper.absolutePos(BLOCK));
        return capsule;
    }

    /**
     * SecurityCraft tags its blocks c:relocation_not_supported, part of capsule:excluded.
     */
    @GameTest(template = "empty")
    public static void othersCannotCaptureSecurityCraftBlocks(GameTestHelper helper) {
        Block reinforced = placeOwnedBlock(helper).level().getBlockState(helper.absolutePos(BLOCK)).getBlock();
        ServerPlayer other = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 5));

        captureAs(helper, other);

        helper.assertBlockPresent(reinforced, BLOCK);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyOwnersPassTheSecurityCraftOwnerCheck(GameTestHelper helper) {
        ServerPlayer owner = placeOwnedBlock(helper);
        ServerPlayer other = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 5));

        assertTrue(helper, SecurityCraftOwnerCheck.canTakeBlock(helper.getLevel(), helper.absolutePos(BLOCK), owner), "the owner can take the block");
        assertTrue(helper, !SecurityCraftOwnerCheck.canTakeBlock(helper.getLevel(), helper.absolutePos(BLOCK), other), "another player cannot take the block");
        helper.succeed();
    }
}
