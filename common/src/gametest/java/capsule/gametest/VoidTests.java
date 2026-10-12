package capsule.gametest;

import capsule.items.CapsuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class VoidTests {

    private static ItemEntity dropBelowTheWorld(GameTestHelper helper, ServerPlayer thrower) {
        ServerLevel level = helper.getLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        ItemEntity entity = new ItemEntity(level, pos.x, level.getMinBuildHeight() - 1, pos.z, CapsuleTestUtils.emptyCapsule(3), 0, 0, 0);
        if (thrower != null) entity.setThrower(thrower);
        level.addFreshEntity(entity);
        return entity;
    }

    @GameTest(template = "empty")
    public static void capsuleFallingOutOfTheWorldGoesBackToItsThrower(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        ItemEntity entity = dropBelowTheWorld(helper, player);

        helper.succeedWhen(() -> {
            assertTrue(helper, entity.isRemoved(), "the capsule should leave the void");
            assertTrue(helper, player.getInventory().contains(stack -> stack.getItem() instanceof CapsuleItem), "the capsule should be back in the inventory");
            CapsuleTestUtils.removePlayer(player);
        });
    }

    @GameTest(template = "empty")
    public static void capsuleWithoutThrowerFallingOutOfTheWorldGoesToTheSpawn(GameTestHelper helper) {
        ItemEntity entity = dropBelowTheWorld(helper, null);

        BlockPos spawn = helper.getLevel().getSharedSpawnPos();

        helper.succeedWhen(() -> {
            BlockPos pos = entity.blockPosition();
            assertTrue(helper, !entity.isRemoved(), "the capsule should still exist");
            assertTrue(helper, pos.getX() == spawn.getX() && pos.getZ() == spawn.getZ() && pos.getY() >= helper.getLevel().getMinBuildHeight(),
                    "the capsule should be at the spawn " + spawn + ", got " + pos);
        });
    }
}
