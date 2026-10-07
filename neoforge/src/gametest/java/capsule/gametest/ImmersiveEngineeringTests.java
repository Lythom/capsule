package capsule.gametest;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static capsule.gametest.CapsuleTestUtils.assertNeverCaptured;
import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.block;

/**
 * Immersive Engineering: wires disappeared when their connectors were moved, the level keeps them, not the connectors.
 * Immersive Engineering tags its connectors c:relocation_not_supported, which Capsule excludes: connectors and wires
 * stay in place.
 * Registered when Immersive Engineering is loaded (-Pincompat), see docs/TESTING.md.
 */
public class ImmersiveEngineeringTests {

    @GameTest(template = "empty")
    public static void wiredConnectorsAreNeverCaptured(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 1, 1), second = new BlockPos(3, 1, 1);
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(4, 0, 4), Blocks.STONE.defaultBlockState());
        BlockState connector = block("immersiveengineering:connector_lv").defaultBlockState().trySetValue(BlockStateProperties.FACING, Direction.DOWN);
        helper.setBlock(first, connector);
        helper.setBlock(second, connector);
        GlobalWireNetwork net = GlobalWireNetwork.getNetwork(helper.getLevel());
        ConnectionPoint a = new ConnectionPoint(helper.absolutePos(first), 0), b = new ConnectionPoint(helper.absolutePos(second), 0);
        net.addConnection(new Connection(WireType.COPPER, a, b, net));

        assertNeverCaptured(helper, new BlockPos(0, 1, 0), 5, first, second);
        assertTrue(helper, net.getLocalNet(a).getConnections(a).stream().anyMatch(wire -> wire.getOtherEnd(a).equals(b)), "the wire between the connectors is gone");
        helper.succeed();
    }
}
