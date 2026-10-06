package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.platform.Services;
import capsule.structure.CapsuleTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Every structure file format Capsule reads, with the same 3x3x3 scene: a stone floor, a chest holding 5 diamonds in
 * its middle, and an armor stand in a corner where the format has entities. The fixtures are in data/capsule/schematics:
 * vanilla.nbt (structure block), sponge_v2 and sponge_v3 exported by WorldEdit 7.3.8 (see WorldEditTests on NeoForge),
 * mcedit.schematic and sponge_v1.schem written by hand after the format specifications.
 */
public class SchematicTests {

    public static void buildScene(GameTestHelper helper, BlockPos corner) {
        CapsuleTestUtils.fill(helper, corner, corner.offset(2, 0, 2), Blocks.STONE.defaultBlockState());
        helper.setBlock(corner.offset(1, 1, 1), Blocks.CHEST);
        ((Container) helper.getBlockEntity(corner.offset(1, 1, 1))).setItem(0, new ItemStack(Items.DIAMOND, 5));
        Vec3 standPos = helper.absoluteVec(Vec3.atBottomCenterOf(corner.offset(0, 1, 0)));
        ArmorStand stand = new ArmorStand(helper.getLevel(), standPos.x, standPos.y, standPos.z);
        helper.getLevel().addFreshEntity(stand);
    }

    public static List<String> sceneProblems(GameTestHelper helper, BlockPos corner, boolean withEntity) {
        return sceneProblems(helper, corner, withEntity, true);
    }

    private static List<String> sceneProblems(GameTestHelper helper, BlockPos corner, boolean withEntity, boolean withChestContent) {
        List<String> problems = new ArrayList<>();
        BlockPos.betweenClosed(corner, corner.offset(2, 0, 2)).forEach(pos -> {
            if (!helper.getBlockState(pos).is(Blocks.STONE)) problems.add("no stone at " + pos);
        });
        BlockPos chest = corner.offset(1, 1, 1);
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(chest)) instanceof Container container)) {
            problems.add("no chest at " + chest + ", got " + helper.getBlockState(chest));
        } else if (withChestContent && container.countItem(Items.DIAMOND) != 5) {
            problems.add("the chest holds " + container.countItem(Items.DIAMOND) + " diamonds instead of 5");
        }
        AABB box = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(corner)), Vec3.atLowerCornerOf(helper.absolutePos(corner.offset(3, 3, 3))));
        List<ArmorStand> stands = helper.getLevel().getEntities(EntityType.ARMOR_STAND, box, e -> true);
        if (withEntity && stands.size() != 1) problems.add(stands.size() + " armor stands instead of 1");
        return problems;
    }

    private static void deploysTheScene(GameTestHelper helper, String path, boolean withEntity) {
        deploysTheScene(helper, path, withEntity, true);
    }

    private static void deploysTheScene(GameTestHelper helper, String path, boolean withEntity, boolean withChestContent) {
        CapsuleTemplate template = StructureSaver.getTemplateForReward(helper.getLevel().getServer(), path).getRight();
        assertTrue(helper, template != null && !template.getPalette().isEmpty(), path + " should be read");
        assertTrue(helper, CapsuleTestUtils.deploy(helper, Capsule.newRewardCapsuleItemStack(path, 0, 0, 3, null, null), new BlockPos(4, 0, 4), null), path + " should deploy");
        List<String> problems = sceneProblems(helper, new BlockPos(3, 1, 3), withEntity, withChestContent);
        assertTrue(helper, problems.isEmpty(), path + ": " + problems);
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaStructureDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/vanilla", true);
    }

    /**
     * MCEdit block entities are upgraded from Minecraft 1.12.2. SecurityCraft (in the NeoForge GameTest runtime) mixes
     * into the 1.13 data fixer schema, which then leaves data older than 1.13 as is: the diamonds keep their 1.12 count.
     */
    @GameTest(template = "empty")
    public static void mceditSchematicDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/mcedit", true, !Services.PLATFORM.isModLoaded("securitycraft"));
    }

    /**
     * Sponge v1 has no entities.
     */
    @GameTest(template = "empty")
    public static void spongeV1SchemDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/sponge_v1", false);
    }

    @GameTest(template = "empty")
    public static void spongeV2SchemDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/sponge_v2", true);
    }

    @GameTest(template = "empty")
    public static void spongeV2SchematicDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/sponge_v2_as_schematic", true);
    }

    @GameTest(template = "empty")
    public static void spongeV3SchemDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/sponge_v3", true);
    }

    @GameTest(template = "empty")
    public static void spongeV3SchematicDeploys(GameTestHelper helper) {
        deploysTheScene(helper, "schematics/sponge_v3_as_schematic", true);
    }
}
