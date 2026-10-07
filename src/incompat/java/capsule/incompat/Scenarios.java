package capsule.incompat;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireType;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import capsule.Config;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static capsule.incompat.Scenario.block;
import static capsule.incompat.Scenario.item;

/**
 * The scenarios of the known incompatibilities: today's behavior, a change shows as a FAIL line.
 */
class Scenarios {
    private static final BlockPos MOVE = new BlockPos(8, 0, 0);

    /**
     * Corail Tombstone: a moved player grave duplicated its items. Tombstone adds its graves to capsule:excluded.
     */
    static void tombstoneGraves(Scenario s) {
        BlockPos grave = new BlockPos(2, 1, 2);
        s.set(grave, block("tombstone:grave_simple"));
        s.neverCaptured(new BlockPos(1, 1, 1), 3, grave);
    }

    private static final BlockPos DRIVE = new BlockPos(2, 1, 2);

    /**
     * A disk drive holding a 1k disk with 16 diamonds, next to a controller.
     */
    private static void diskDrive(Scenario s) {
        s.set(DRIVE, block("refinedstorage:disk_drive"));
        s.set(DRIVE.east(), block("refinedstorage:controller"));
        ItemStack disk = new ItemStack(item("refinedstorage:1k_storage_disk"));
        // a disk gets its storage in a player inventory
        disk.inventoryTick(s.level, FakePlayerFactory.getMinecraft(s.level), 0, false);
        storage(s, disk).insert(new ItemStack(Items.DIAMOND, 16), 16, Action.PERFORM);
        ((DiskDriveBlockEntity) s.blockEntity(DRIVE)).getNode().getDisks().insertItem(0, disk, false);
    }

    @SuppressWarnings("unchecked")
    private static IStorageDisk<ItemStack> storage(Scenario s, ItemStack disk) {
        return (IStorageDisk<ItemStack>) API.instance().getStorageDiskManager(s.level).getByStack(disk);
    }

    /**
     * Refined Storage: machines lose their data when moved, its network nodes are kept by the level, by position.
     * Capsule excludes refinedstorage: by default.
     */
    static void refinedStorageExcluded(Scenario s) {
        diskDrive(s);
        s.neverCaptured(new BlockPos(1, 1, 1), 3, DRIVE, DRIVE.east());
    }

    /**
     * Without refinedstorage: in the config, forge:relocation_not_supported still leaves them in place: a moved disk
     * drive would lose its disk, the level keeps the network nodes by position.
     */
    static void refinedStorageNotInTheConfig(Scenario s) {
        diskDrive(s);
        List<Block> excluded = Config.excludedBlocks, opExcluded = Config.opExcludedBlocks;
        Config.excludedBlocks = withoutNamespace(excluded, "refinedstorage");
        Config.opExcludedBlocks = withoutNamespace(opExcluded, "refinedstorage");
        try {
            s.neverCaptured(new BlockPos(1, 1, 1), 3, DRIVE, DRIVE.east());
        } finally {
            Config.excludedBlocks = excluded;
            Config.opExcludedBlocks = opExcluded;
        }
    }

    private static List<Block> withoutNamespace(List<Block> blocks, String namespace) {
        return blocks.stream().filter(block -> !namespace(block.defaultBlockState()).equals(namespace)).toList();
    }

    private static String namespace(BlockState state) {
        return ForgeRegistries.BLOCKS.getKey(state.getBlock()).getNamespace();
    }

    /**
     * Mekanism: a moved Digital Miner could not be broken and stopped working, its bounding blocks pointing to its old
     * position. Mekanism tags the miner and its bounding blocks forge:relocation_not_supported, in capsule:excluded.
     */
    static void mekanismDigitalMiner(Scenario s) {
        s.floor(7);
        s.placeOn(new BlockPos(3, 0, 3), new ItemStack(item("mekanism:digital_miner")));
        BlockPos[] miner = blocksOf(s, "mekanism", new BlockPos(6, 7, 6));
        s.check("the Digital Miner is placed with its bounding blocks", miner.length > 1, miner.length + " blocks");
        s.neverCaptured(new BlockPos(0, 1, 0), 7, miner);
    }

    /**
     * The positions of the blocks of a mod in the box from 0, 1, 0 to max.
     */
    private static BlockPos[] blocksOf(Scenario s, String namespace, BlockPos max) {
        return BlockPos.betweenClosedStream(new BlockPos(0, 1, 0), max).filter(pos -> namespace(s.get(pos)).equals(namespace)).map(BlockPos::immutable).toArray(BlockPos[]::new);
    }

    static void mekanismBin(Scenario s) {
        movesWithContent(s, block("mekanism:basic_bin"), new ItemStack(Items.COBBLESTONE, 64));
    }

    /**
     * Immersive Engineering: wires disappeared when their connectors were deployed elsewhere, the level keeps them by
     * position. Immersive Engineering 1.20 tags the block entities of its connectors forge:relocation_not_supported, not
     * the blocks: Capsule excludes the connectors by id.
     */
    static void immersiveEngineeringWires(Scenario s) {
        BlockPos first = new BlockPos(1, 1, 1), second = new BlockPos(3, 1, 1);
        s.floor(5);
        Block connector = block("immersiveengineering:connector_lv");
        BlockState down = connector.defaultBlockState().setValue((DirectionProperty) connector.getStateDefinition().getProperty("facing"), Direction.DOWN);
        s.set(first, down);
        s.set(second, down);
        GlobalWireNetwork net = GlobalWireNetwork.getNetwork(s.level);
        net.addConnection(new Connection(WireType.COPPER, new ConnectionPoint(s.abs(first), 0), new ConnectionPoint(s.abs(second), 0), net));
        s.check("the connectors are wired", wired(s, net, first, second), "");
        s.neverCaptured(new BlockPos(0, 1, 0), 5, first, second);
        s.check("the connectors are still wired", wired(s, net, first, second), "");
    }

    private static boolean wired(Scenario s, GlobalWireNetwork net, BlockPos first, BlockPos second) {
        ConnectionPoint a = new ConnectionPoint(s.abs(first), 0), b = new ConnectionPoint(s.abs(second), 0);
        return net.getLocalNet(a).getConnections(a).stream().anyMatch(wire -> wire.getOtherEnd(a).equals(b));
    }

    /**
     * GregTech: machines lost their data when moved (1.12). Their namespace is gtceu: Capsule no longer lists the
     * 1.12 gregtech:machine.
     */
    static void gregTechMachine(Scenario s) {
        movesWithContent(s, block("gtceu:lv_electric_furnace"), new ItemStack(Items.RAW_IRON, 8));
    }

    private static final String PROGRAM = "EVERY 20 TICKS DO INPUT FROM a OUTPUT TO b END";
    private static final BlockPos A = new BlockPos(1, 1, 1), MANAGER = new BlockPos(3, 1, 1), B = new BlockPos(5, 1, 1), SFM_MOVE = new BlockPos(0, 0, 8);

    /**
     * Super Factory Manager: the manager crashed the game when moved (1.12). Its namespace is sfm: Capsule no longer
     * lists the 1.12 superfactorymanager:. The disk labels the inventories by position.
     */
    static void superFactoryManager(Scenario s) {
        s.set(A, Blocks.CHEST);
        s.set(A.east(), block("sfm:cable"));
        s.set(MANAGER, block("sfm:manager"));
        s.set(MANAGER.east(), block("sfm:cable"));
        s.set(B, Blocks.CHEST);
        ((Container) s.blockEntity(A)).setItem(0, new ItemStack(Items.DIAMOND, 8));
        ItemStack disk = new ItemStack(item("sfm:disk"));
        DiskItem.setProgram(disk, PROGRAM);
        LabelPositionHolder.empty().add("a", s.abs(A)).add("b", s.abs(B)).save(disk);
        ((Container) s.blockEntity(MANAGER)).setItem(0, disk);
        s.later(45, () -> {
            s.check("the program moves the diamonds", count(s, B, Items.DIAMOND) == 8, count(s, B, Items.DIAMOND));
            ((Container) s.blockEntity(A)).setItem(0, new ItemStack(Items.EMERALD, 8));
            ItemStack capsule = s.capture(new BlockPos(1, 1, 0), 5, false);
            s.check("a capsule takes the manager", capsule != null && s.get(MANAGER).isAir(), s.get(MANAGER));
            s.check("the capsule deploys", s.deploy(capsule, new BlockPos(1, 1, 0).offset(SFM_MOVE), 5), "");
            ManagerBlockEntity manager = (ManagerBlockEntity) s.blockEntity(MANAGER.offset(SFM_MOVE));
            s.check("the deployed manager keeps its program", PROGRAM.equals(manager.getProgramString()), manager.getProgramString());
            s.later(45, () -> {
                int moved = count(s, B.offset(SFM_MOVE), Items.EMERALD);
                s.check("the deployed program moves nothing, its labels hold the original positions", moved == 0, moved + " emeralds moved, " + LabelPositionHolder.from(manager.getDisk()));
                ItemStack movedDisk = manager.getDisk();
                LabelPositionHolder.from(movedDisk).clear().add("a", s.abs(A.offset(SFM_MOVE))).add("b", s.abs(B.offset(SFM_MOVE))).save(movedDisk);
                ((Container) manager).setItem(0, movedDisk);
                s.later(45, () -> s.check("labelling the deployed inventories again is enough", count(s, B.offset(SFM_MOVE), Items.EMERALD) == 8, count(s, B.offset(SFM_MOVE), Items.EMERALD)));
            });
        });
    }

    private static int count(Scenario s, BlockPos chest, net.minecraft.world.item.Item item) {
        return ((Container) s.blockEntity(chest)).countItem(item);
    }

    /**
     * Blood Magic: the alchemy table disappeared when deployed elsewhere, its two halves hold each other's position.
     * Capsule excludes bloodmagic:alchemytable (bloodmagic:alchemy_table before, which matched nothing).
     */
    static void bloodMagicAlchemyTable(Scenario s) {
        s.floor(5);
        s.placeOn(new BlockPos(2, 0, 2), new ItemStack(item("bloodmagic:alchemytable")));
        BlockPos[] halves = blocksOf(s, "bloodmagic", new BlockPos(4, 2, 4));
        s.check("the alchemy table is placed with its two halves", halves.length == 2, List.of(halves));
        s.neverCaptured(new BlockPos(0, 1, 0), 5, halves);
    }

    /**
     * Waystones: moved waystones left ghost blocks and broke doors (#121). Waystones 1.20 has no relocation tag: Capsule
     * excludes waystones: by default.
     */
    static void waystone(Scenario s) {
        s.floor(3);
        s.placeOn(new BlockPos(1, 0, 1), new ItemStack(item("waystones:waystone")));
        BlockPos[] waystone = blocksOf(s, "waystones", new BlockPos(2, 2, 2));
        s.check("the waystone is placed with its two halves", waystone.length == 2, List.of(waystone));
        s.neverCaptured(new BlockPos(0, 1, 0), 3, waystone);
    }

    /**
     * A block holding content moves with it: captured alone, deployed elsewhere, it holds the same content.
     */
    private static void movesWithContent(Scenario s, Block block, ItemStack content) {
        BlockPos source = new BlockPos(1, 1, 1);
        s.set(source, block);
        s.check("the " + ForgeRegistries.BLOCKS.getKey(block) + " takes " + content, s.items(source).anyMatch(handler -> ItemHandlerHelper.insertItem(handler, content.copy(), false).isEmpty()), "");
        ItemStack capsule = s.capture(source, 1, false);
        s.check("a capsule takes it", capsule != null && s.get(source).isAir(), s.get(source));
        s.check("the capsule deploys", s.deploy(capsule, source.offset(MOVE), 1), "");
        int count = s.count(source.offset(MOVE), content.getItem());
        s.check("the deployed block holds its " + content, count == content.getCount(), count);
    }
}
