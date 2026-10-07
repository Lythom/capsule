package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static capsule.gametest.CapsuleTestUtils.assertNeverCaptured;
import static capsule.gametest.CapsuleTestUtils.block;

/**
 * Corail Tombstone: a moved player grave duplicated its items. Tombstone adds its graves to capsule:excluded and Capsule
 * excludes tombstone:player_graves, so no capsule takes them.
 * Registered when Tombstone is loaded (-Pincompat), see docs/TESTING.md.
 */
public class TombstoneTests {

    @GameTest(template = "empty")
    public static void playerGravesAreNeverCaptured(GameTestHelper helper) {
        BlockPos grave = new BlockPos(2, 1, 2);
        helper.setBlock(grave, block("tombstone:grave_simple"));
        assertNeverCaptured(helper, new BlockPos(1, 1, 1), 3, grave);
        helper.succeed();
    }
}
