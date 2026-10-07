package capsule.gametest;

import capsule.Config;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.storage.diskdrive.AbstractDiskDriveBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertNeverCaptured;
import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.block;
import static capsule.gametest.CapsuleTestUtils.item;

/**
 * Refined Storage: machines lost their data when moved (1.12). Capsule excludes refinedstorage: by default. Without the
 * exclusion, a Refined Storage 2 disk drive moves with its disk, whose content the level keeps by disk id.
 * Registered when Refined Storage is loaded (-Pincompat), see docs/TESTING.md.
 */
public class RefinedStorageTests {

    private static final BlockPos DRIVE = new BlockPos(2, 1, 2);

    /**
     * A disk drive holding a 1k disk with 16 diamonds, next to a controller.
     */
    private static void diskDrive(GameTestHelper helper) {
        helper.setBlock(DRIVE, block("refinedstorage:disk_drive"));
        helper.setBlock(DRIVE.east(), block("refinedstorage:controller"));
        ItemStack disk = new ItemStack(item("refinedstorage:1k_storage_disk"));
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, DRIVE.north());
        // a disk gets its storage in a player inventory
        disk.inventoryTick(helper.getLevel(), player, 0, false);
        CapsuleTestUtils.removePlayer(player);
        storage(helper, disk).insert(ItemResource.ofItemStack(new ItemStack(Items.DIAMOND)), 16, Action.EXECUTE, Actor.EMPTY);
        drive(helper, DRIVE).getDiskInventory().setItem(0, disk);
    }

    private static AbstractDiskDriveBlockEntity drive(GameTestHelper helper, BlockPos pos) {
        return (AbstractDiskDriveBlockEntity) helper.getBlockEntity(pos);
    }

    private static SerializableStorage storage(GameTestHelper helper, ItemStack disk) {
        return ((StorageContainerItem) disk.getItem()).resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), disk)
                .orElseThrow(() -> new AssertionError("no storage for " + disk));
    }

    @GameTest(template = "empty")
    public static void refinedStorageIsExcludedByDefault(GameTestHelper helper) {
        diskDrive(helper);
        assertNeverCaptured(helper, new BlockPos(1, 1, 1), 3, DRIVE, DRIVE.east());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "incompatconfig")
    public static void diskDriveKeepsItsDiskWhenNotExcluded(GameTestHelper helper) {
        diskDrive(helper);
        List<Block> excluded = Config.excludedBlocks;
        Config.excludedBlocks = excluded.stream().filter(block -> !BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("refinedstorage")).toList();
        ItemStack capsule;
        try {
            capsule = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 3);
        } finally {
            Config.excludedBlocks = excluded;
        }
        helper.assertBlockNotPresent(block("refinedstorage:disk_drive"), DRIVE);
        assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(5, 0, 5), null), "deploy should succeed");

        BlockPos moved = DRIVE.offset(3, 0, 3);
        helper.assertBlockPresent(block("refinedstorage:controller"), moved.east());
        ItemStack disk = drive(helper, moved).getDiskInventory().getItem(0);
        assertTrue(helper, !disk.isEmpty(), "the deployed disk drive has no disk");
        assertTrue(helper, storage(helper, disk).getStored() == 16, "the disk holds " + storage(helper, disk).getAll() + " instead of 16 diamonds");
        helper.succeed();
    }
}
