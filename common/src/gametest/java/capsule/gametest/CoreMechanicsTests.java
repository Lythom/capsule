package capsule.gametest;

import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.blocks.CapsuleBlocks;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;
import static capsule.gametest.CapsuleTestUtils.template;

public class CoreMechanicsTests {

    private static final BlockPos CORNER = new BlockPos(1, 1, 1);
    private static final BlockPos ANCHOR = new BlockPos(5, 0, 5);

    /**
     * Stone at the center bottom of a 3x3x3 area starting at CORNER, planks above it.
     */
    private static ItemStack captureStoneAndPlanks(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        helper.setBlock(2, 2, 2, Blocks.OAK_PLANKS);
        return capture(helper, CORNER, 3);
    }

    @GameTest(template = "empty")
    public static void captureIntoEmptyCapsule(GameTestHelper helper) {
        ItemStack capsule = captureStoneAndPlanks(helper);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "capsule should be linked");
        assertTrue(helper, CapsuleItem.getStructureName(capsule) != null, "capsule should have a structure name");
        helper.assertBlockNotPresent(Blocks.STONE, 2, 1, 2);
        helper.assertBlockNotPresent(Blocks.OAK_PLANKS, 2, 2, 2);
        assertTrue(helper, template(helper, capsule).getPalette().size() == 2, "template should hold 2 blocks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deployLinkedCapsule(GameTestHelper helper) {
        ItemStack capsule = captureStoneAndPlanks(helper);

        assertTrue(helper, deploy(helper, capsule, ANCHOR, null), "deploy should succeed");

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED), "capsule should be deployed");
        helper.assertBlockPresent(Blocks.STONE, 5, 1, 5);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 2, 5);
        assertTrue(helper, template(helper, capsule).getPalette().isEmpty(), "deployed template must be emptied (recovery dupe protection)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void undeployDeployedCapsule(GameTestHelper helper) {
        ItemStack capsule = captureStoneAndPlanks(helper);
        deploy(helper, capsule, ANCHOR, null);

        Capsule.resentToCapsule(capsule, helper.getLevel(), null);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "capsule should be linked again");
        helper.assertBlockNotPresent(Blocks.STONE, 5, 1, 5);
        helper.assertBlockNotPresent(Blocks.OAK_PLANKS, 5, 2, 5);
        assertTrue(helper, template(helper, capsule).getPalette().size() == 2, "template should hold 2 blocks again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void excludedBlocksStayInPlace(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.SPAWNER);
        helper.setBlock(2, 2, 2, Blocks.STONE);

        ItemStack capsule = capture(helper, CORNER, 3);

        helper.assertBlockPresent(Blocks.SPAWNER, 2, 1, 2);
        helper.assertBlockNotPresent(Blocks.STONE, 2, 2, 2);
        assertTrue(helper, template(helper, capsule).getPalette().size() == 1, "only the stone should be captured");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overridableBlocksAreReplaced(GameTestHelper helper) {
        ItemStack capsule = captureStoneAndPlanks(helper);
        helper.setBlock(5, 0, 5, Blocks.GRASS_BLOCK);
        helper.setBlock(5, 1, 5, Blocks.SHORT_GRASS);

        assertTrue(helper, deploy(helper, capsule, ANCHOR, null), "deploy over grass should succeed");

        helper.assertBlockPresent(Blocks.STONE, 5, 1, 5);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void solidBlocksPreventDeploy(GameTestHelper helper) {
        ItemStack capsule = captureStoneAndPlanks(helper);
        helper.setBlock(5, 2, 5, Blocks.DIRT);

        assertTrue(helper, !deploy(helper, capsule, ANCHOR, null), "deploy into dirt should fail");

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "capsule should stay linked");
        helper.assertBlockNotPresent(Blocks.STONE, 5, 1, 5);
        helper.assertBlockPresent(Blocks.DIRT, 5, 2, 5);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintConsumesMaterials(GameTestHelper helper) {
        ItemStack source = captureStoneAndPlanks(helper);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 1));
        ItemStack blueprint = Capsule.newLinkedCapsuleItemStack(CapsuleItem.getStructureName(source), 0, 0, 3, false, null, 0);
        CapsuleItem.setBlueprint(blueprint);
        CapsuleItem.setState(blueprint, CapsuleState.DEPLOYED);
        CapsuleItem.duplicateBlueprintTemplate(blueprint, helper.getLevel(), player);
        assertTrue(helper, !CapsuleItem.getStructureName(blueprint).equals(CapsuleItem.getStructureName(source)), "blueprint should own a template copy");

        Map<StructureSaver.ItemStackKey, Integer> missing = Capsule.reloadBlueprint(blueprint, helper.getLevel(), player);
        assertTrue(helper, missing != null && missing.size() == 2, "both materials should be missing, got " + missing);
        assertTrue(helper, CapsuleItem.hasState(blueprint, CapsuleState.DEPLOYED), "blueprint should stay uncharged");

        player.getInventory().add(new ItemStack(Items.STONE));
        player.getInventory().add(new ItemStack(Items.OAK_PLANKS));
        missing = Capsule.reloadBlueprint(blueprint, helper.getLevel(), player);
        assertTrue(helper, missing != null && missing.isEmpty(), "nothing should be missing, got " + missing);
        assertTrue(helper, CapsuleItem.hasState(blueprint, CapsuleState.BLUEPRINT), "blueprint should be charged");
        assertTrue(helper, player.getInventory().countItem(Items.STONE) == 0 && player.getInventory().countItem(Items.OAK_PLANKS) == 0, "materials should be consumed");

        assertTrue(helper, deploy(helper, blueprint, ANCHOR, player), "blueprint deploy should succeed");
        helper.assertBlockPresent(Blocks.STONE, 5, 1, 5);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 2, 5);
        assertTrue(helper, template(helper, blueprint).getPalette().size() == 2, "blueprint template is kept");
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recoveryCapsuleEmptiesSourceTemplate(GameTestHelper helper) {
        ItemStack linked = captureStoneAndPlanks(helper);
        ItemStack recovery = linked.copy();
        CapsuleItem.setOneUse(recovery);

        assertTrue(helper, deploy(helper, recovery, ANCHOR, null), "recovery deploy should succeed");

        helper.assertBlockPresent(Blocks.STONE, 5, 1, 5);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 2, 5);
        assertTrue(helper, template(helper, linked).getPalette().isEmpty(), "source template must be emptied to prevent dupes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rewardCapsuleDeploysTwice(GameTestHelper helper) {
        ItemStack reward = Capsule.newRewardCapsuleItemStack("config/capsule/starters/_stater_oak_hut", 0, 0, 3, null, null);

        assertTrue(helper, deploy(helper, reward, new BlockPos(2, 0, 2), null), "first reward deploy should succeed");
        assertTrue(helper, deploy(helper, reward, new BlockPos(6, 0, 6), null), "second reward deploy should succeed");

        assertTrue(helper, CapsuleItem.hasState(reward, CapsuleState.ONE_USE), "reward capsule state is unchanged");
        assertTrue(helper, countBlocks(helper, new BlockPos(1, 1, 1), new BlockPos(3, 3, 3)) > 10, "first hut should be deployed");
        assertTrue(helper, countBlocks(helper, new BlockPos(5, 1, 5), new BlockPos(7, 3, 7)) > 10, "second hut should be deployed");
        helper.succeed();
    }

    private static long countBlocks(GameTestHelper helper, BlockPos from, BlockPos to) {
        return BlockPos.betweenClosedStream(from, to).filter(p -> !helper.getBlockState(p).isAir()).count();
    }

    @GameTest(template = "empty")
    public static void thrownCapsuleCapturesAboveCaptureBase(GameTestHelper helper) {
        BlockPos markerPos = new BlockPos(4, 1, 4);
        helper.setBlock(markerPos, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        helper.setBlock(4, 2, 4, Blocks.STONE);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        CapsuleItem.setState(capsule, CapsuleState.EMPTY_ACTIVATED);
        Vec3 pos = helper.absoluteVec(new Vec3(4.5, 4, 4.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, capsule, 0, 0, 0);
        helper.getLevel().addFreshEntity(entity);

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.STONE, 4, 2, 4);
            helper.assertBlockPresent(CapsuleBlocks.CAPSULE_MARKER.get(), markerPos);
            assertTrue(helper, CapsuleItem.hasState(entity.getItem(), CapsuleState.LINKED), "thrown capsule should be linked");
        });
    }

    /**
     * Chest boats are captured and removed without dropping their content, also when a failed deploy removes the boats
     * it placed (#113).
     */
    @GameTest(template = "empty")
    public static void containerEntitiesDropNothingWhenRemoved(GameTestHelper helper) {
        for (int x : new int[]{1, 3}) {
            ChestBoat boat = EntityType.CHEST_BOAT.create(helper.getLevel());
            Vec3 pos = helper.absoluteVec(new Vec3(x + 0.5, 1, 2.5));
            boat.moveTo(pos.x, pos.y, pos.z);
            boat.setItem(0, new ItemStack(Items.DIAMOND));
            helper.getLevel().addFreshEntity(boat);
        }
        ItemStack capsule = capture(helper, CORNER, 3);
        assertTrue(helper, helper.getEntities(EntityType.CHEST_BOAT).isEmpty(), "the boats are captured");
        assertTrue(helper, helper.getEntities(EntityType.ITEM).isEmpty(), "the capture dropped " + helper.getEntities(EntityType.ITEM));
        // throws once a boat is placed, as a modded entity crashing during the deploy
        StructurePlaceSettings crashing = new StructurePlaceSettings() {
            @Override
            public Mirror getMirror() {
                if (!helper.getEntities(EntityType.CHEST_BOAT).isEmpty()) throw new IllegalStateException("test crash");
                return super.getMirror();
            }
        };
        assertTrue(helper, !StructureSaver.deploy(capsule, helper.getLevel(), null, helper.absolutePos(new BlockPos(5, 1, 5)), crashing), "the deploy fails");
        assertTrue(helper, helper.getEntities(EntityType.CHEST_BOAT).isEmpty(), "the failed deploy removes its boats");
        assertTrue(helper, helper.getEntities(EntityType.ITEM).isEmpty(), "the failed deploy dropped " + helper.getEntities(EntityType.ITEM));
        assertTrue(helper, deploy(helper, capsule, ANCHOR, null), "the capsule keeps its boats");
        List<ChestBoat> boats = helper.getEntities(EntityType.CHEST_BOAT);
        assertTrue(helper, boats.size() == 2 && boats.stream().allMatch(b -> b.getItem(0).is(Items.DIAMOND)), "the boats keep their diamonds: " + boats);
        helper.succeed();
    }
}
