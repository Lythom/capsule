package capsule.gametest;

import capsule.api.CapsuleApi;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.recipes.ClearCapsuleRecipe;
import capsule.structure.Deployments.Deployment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.function.Predicate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class ApiTests {

    // anchors of 3x3x3 cubes whose lowest corners hold the gold block
    private static final BlockPos SOURCE_ANCHOR = new BlockPos(2, 0, 2);
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos DEST_ANCHOR = new BlockPos(6, 0, 6);
    private static final BlockPos DEST = new BlockPos(5, 1, 5);

    private static ItemStack goldCapsule(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.GOLD_BLOCK);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        assertTrue(helper, CapsuleApi.capture(capsule, helper.getLevel(), helper.absolutePos(SOURCE_ANCHOR), null), "the capture should succeed");
        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, SOURCE);
        return capsule;
    }

    private static List<Deployment> deploymentsAtDest(GameTestHelper helper) {
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(DEST), helper.absolutePos(DEST.offset(2, 2, 2)));
        return CapsuleApi.deploymentsIntersecting(helper.getLevel(), box);
    }

    @GameTest(template = "empty")
    public static void deployPlacesTheContentAboveTheAnchor(GameTestHelper helper) {
        ItemStack capsule = goldCapsule(helper);

        assertTrue(helper, CapsuleApi.deploy(capsule, helper.getLevel(), helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null), "the deploy should succeed");

        helper.assertBlockPresent(Blocks.GOLD_BLOCK, DEST);
        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED), "the capsule should be deployed");
        assertTrue(helper, deploymentsAtDest(helper).size() == 1, "the deployment should be registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void undeployWithTheCapsuleForgetsTheDeployment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack capsule = goldCapsule(helper);
        CapsuleApi.deploy(capsule, level, helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null);

        assertTrue(helper, CapsuleApi.undeploy(capsule, level, null), "the undeploy should succeed");

        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, DEST);
        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "the capsule should be linked");
        assertTrue(helper, deploymentsAtDest(helper).isEmpty(), "the deployment should be forgotten");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oneUseCapsuleIsConsumedAndNotRegistered(GameTestHelper helper) {
        ItemStack recovery = goldCapsule(helper).copy();
        CapsuleItem.setOneUse(recovery);

        assertTrue(helper, CapsuleApi.deploy(recovery, helper.getLevel(), helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null), "the deploy should succeed");

        helper.assertBlockPresent(Blocks.GOLD_BLOCK, DEST);
        assertTrue(helper, recovery.isEmpty(), "the one-use capsule should be consumed");
        assertTrue(helper, deploymentsAtDest(helper).isEmpty(), "a one-use deployment should not be registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deployRefusesABlockedArea(GameTestHelper helper) {
        ItemStack capsule = goldCapsule(helper);
        helper.setBlock(DEST, Blocks.STONE);

        assertTrue(helper, !CapsuleApi.deploy(capsule, helper.getLevel(), helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null), "the deploy should be refused");

        helper.assertBlockPresent(Blocks.STONE, DEST);
        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "the capsule should stay linked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deployedContentLeavesOutTheBlocksAlreadyThere(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack capsule = goldCapsule(helper);
        BlockPos stone = DEST.offset(1, 1, 1);
        helper.setBlock(stone, Blocks.STONE);
        CapsuleApi.deploy(capsule, level, helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null);

        Predicate<BlockPos> deployedContent = CapsuleApi.deployedContent(deploymentsAtDest(helper).getFirst(), level.getServer());

        assertTrue(helper, deployedContent.test(helper.absolutePos(DEST)), "the gold block is deployed content");
        assertTrue(helper, !deployedContent.test(helper.absolutePos(stone)), "the stone was there before the deploy");
        assertTrue(helper, !deployedContent.test(helper.absolutePos(DEST.offset(3, 0, 0))), "outside the box is not deployed content");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyingADeployedCapsuleByCraftingForgetsTheDeployment(GameTestHelper helper) {
        ItemStack capsule = goldCapsule(helper);
        CapsuleApi.deploy(capsule, helper.getLevel(), helper.absolutePos(DEST_ANCHOR), Rotation.NONE, null);

        NonNullList<ItemStack> remaining = clear(capsule);

        assertTrue(helper, remaining.getFirst().isEmpty(), "a deployed capsule gives no recovery capsule");
        assertTrue(helper, deploymentsAtDest(helper).isEmpty(), "the deployment should be forgotten");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, DEST);
        helper.succeed();
    }

    private static NonNullList<ItemStack> clear(ItemStack capsule) {
        return new ClearCapsuleRecipe(CraftingBookCategory.MISC).getRemainingItems(CraftingInput.of(1, 1, List.of(capsule)));
    }
}
