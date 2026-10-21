package capsule.gametest;

import capsule.items.CapsuleItem;
import capsule.structure.AbsolutePositions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.Optional;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;
import static capsule.gametest.CapsuleTestUtils.template;

public class AbsolutePositionTests {

    private static final BlockPos HIVE = new BlockPos(1, 1, 1), LODESTONE = new BlockPos(2, 1, 1), CHEST = new BlockPos(3, 1, 1);
    private static final BlockPos OUTSIDE = new BlockPos(0, 1, 0);

    @GameTest(template = "empty")
    public static void positionsInsideTheCapsuleFollowItsRotatedDeployment(GameTestHelper helper) {
        helper.setBlock(HIVE, Blocks.BEEHIVE);
        helper.setBlock(LODESTONE, Blocks.LODESTONE);
        helper.setBlock(CHEST, Blocks.CHEST);
        BlockEntity hive = helper.getBlockEntity(HIVE);
        CompoundTag hiveTag = hive.saveWithoutMetadata(helper.getLevel().registryAccess());
        hiveTag.put("flower_pos", NbtUtils.writeBlockPos(helper.absolutePos(LODESTONE)));
        hive.loadWithComponents(hiveTag, helper.getLevel().registryAccess());
        ChestBlockEntity chest = helper.getBlockEntity(CHEST);
        chest.setItem(0, compass(helper, LODESTONE));
        chest.setItem(1, compass(helper, OUTSIDE));

        ItemStack capsule = capture(helper, HIVE, 3);
        CompoundTag captured = template(helper, capsule).getPalette().stream()
                .filter(b -> b.state().is(Blocks.BEEHIVE)).findFirst().orElseThrow().nbt();
        assertTrue(helper, NbtUtils.writeBlockPos(LODESTONE.subtract(HIVE)).equals(captured.get("flower_pos")), "the template should store the flower relative to the capture: " + captured);
        assertTrue(helper, captured.contains(AbsolutePositions.RELATIVE_PATHS), "the template should list the relative positions: " + captured);

        CapsuleItem.setPlacement(capsule, new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90));
        assertTrue(helper, deploy(helper, capsule, new BlockPos(6, 0, 6), null), "deploy should succeed");

        BlockPos lodestone = find(helper, Blocks.LODESTONE, new BlockPos(5, 1, 5));
        BlockPos hivePos = find(helper, Blocks.BEEHIVE, new BlockPos(5, 1, 5));
        BlockPos chestPos = find(helper, Blocks.CHEST, new BlockPos(5, 1, 5));
        CompoundTag deployedHive = helper.getLevel().getBlockEntity(helper.absolutePos(hivePos)).saveWithoutMetadata(helper.getLevel().registryAccess());
        assertTrue(helper, NbtUtils.writeBlockPos(helper.absolutePos(lodestone)).equals(deployedHive.get("flower_pos")), "the hive should point to the deployed lodestone " + helper.absolutePos(lodestone) + ": " + deployedHive);
        assertTrue(helper, !deployedHive.contains(AbsolutePositions.RELATIVE_PATHS), "the deployed hive keeps the template paths: " + deployedHive);
        ChestBlockEntity deployedChest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
        assertTrue(helper, target(deployedChest.getItem(0)).equals(helper.absolutePos(lodestone)), "the compass should point to the deployed lodestone: " + deployedChest.getItem(0));
        assertTrue(helper, target(deployedChest.getItem(1)).equals(helper.absolutePos(OUTSIDE)), "a compass pointing outside the capsule keeps its target: " + deployedChest.getItem(1));
        helper.succeed();
    }

    private static ItemStack compass(GameTestHelper helper, BlockPos target) {
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(target))), true));
        return compass;
    }

    private static BlockPos target(ItemStack compass) {
        return compass.get(DataComponents.LODESTONE_TRACKER).target().orElseThrow().pos();
    }

    private static BlockPos find(GameTestHelper helper, Block block, BlockPos corner) {
        return BlockPos.betweenClosedStream(corner, corner.offset(2, 2, 2))
                .filter(pos -> helper.getBlockState(pos).is(block))
                .map(BlockPos::immutable)
                .findFirst()
                .orElseThrow(() -> new AssertionError(block + " was not deployed"));
    }
}
