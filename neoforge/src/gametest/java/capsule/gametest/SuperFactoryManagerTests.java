package capsule.gametest;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.block;
import static capsule.gametest.CapsuleTestUtils.item;

/**
 * Super Factory Manager: the manager crashed the game when moved (1.12). A manager now moves with its disk and program,
 * but the disk labels the inventories by position, so the program still points to the original inventories.
 * Registered when Super Factory Manager is loaded (-Pincompat), see docs/TESTING.md.
 */
public class SuperFactoryManagerTests {

    private static final String PROGRAM = "EVERY 20 TICKS DO INPUT FROM a OUTPUT TO b END";
    private static final BlockPos A = new BlockPos(1, 1, 1), MANAGER = new BlockPos(3, 1, 1), B = new BlockPos(5, 1, 1);
    private static final BlockPos MOVE = new BlockPos(7, 0, 8);

    private static int count(GameTestHelper helper, BlockPos chest, Item item) {
        return ((Container) helper.getBlockEntity(chest)).countItem(item);
    }

    @GameTest(template = "empty17", timeoutTicks = 250)
    public static void managerMovesWithItsProgram(GameTestHelper helper) {
        helper.setBlock(A, Blocks.CHEST);
        helper.setBlock(A.east(), block("sfm:cable"));
        helper.setBlock(MANAGER, block("sfm:manager"));
        helper.setBlock(MANAGER.east(), block("sfm:cable"));
        helper.setBlock(B, Blocks.CHEST);
        ((Container) helper.getBlockEntity(A)).setItem(0, new ItemStack(Items.DIAMOND, 8));
        ItemStack disk = new ItemStack(item("sfm:disk"));
        DiskItem.setProgram(disk, PROGRAM);
        LabelPositionHolder.empty().add("a", helper.absolutePos(A)).add("b", helper.absolutePos(B)).save(disk);
        ((ManagerBlockEntity) helper.getBlockEntity(MANAGER)).setItem(0, disk);

        helper.runAfterDelay(45, () -> {
            assertTrue(helper, count(helper, B, Items.DIAMOND) == 8, "the program should move the diamonds from a to b before the capture");
            ((Container) helper.getBlockEntity(A)).setItem(0, new ItemStack(Items.EMERALD, 8));
            ItemStack capsule = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 0), 5);
            assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(10, 0, 10), null), "deploy should succeed");

            ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(MANAGER.offset(MOVE));
            assertTrue(helper, PROGRAM.equals(manager.getProgramString()), "the deployed manager runs " + manager.getProgramString());
            helper.runAfterDelay(45, () -> {
                int moved = count(helper, B.offset(MOVE), Items.EMERALD);
                assertTrue(helper, moved == 0 && count(helper, A.offset(MOVE), Items.EMERALD) == 8,
                        "the deployed program moved " + moved + " emeralds: its labels follow the deploy now, " + LabelPositionHolder.from(manager.getDisk()));
                // labelling the deployed inventories again (label gun) is enough
                ItemStack movedDisk = manager.getDisk();
                LabelPositionHolder.from(movedDisk).clear().add("a", helper.absolutePos(A.offset(MOVE))).add("b", helper.absolutePos(B.offset(MOVE))).save(movedDisk);
                manager.setItem(0, movedDisk);
                helper.runAfterDelay(45, () -> {
                    assertTrue(helper, count(helper, B.offset(MOVE), Items.EMERALD) == 8, "the relabelled program should move the emeralds");
                    helper.succeed();
                });
            });
        });
    }
}
