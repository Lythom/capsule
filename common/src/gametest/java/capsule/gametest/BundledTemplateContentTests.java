package capsule.gametest;

import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.InfestedBlock;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class BundledTemplateContentTests {

    @GameTest(template = "empty")
    public static void noBundledTemplateContainsInfestedBlocks(GameTestHelper helper) {
        List<String> infested = BundledContentTests.bundledTemplates(helper).stream()
                .filter(path -> {
                    CapsuleTemplate template = StructureSaver.getTemplateForReward(helper.getLevel().getServer(), path).getRight();
                    return template.getPalette().stream().anyMatch(b -> b.state().getBlock() instanceof InfestedBlock);
                })
                .toList();

        assertTrue(helper, infested.isEmpty(), "templates with infested blocks: " + infested);
        helper.succeed();
    }

    /**
     * Blueprints of the bundled prefabs keep all their blocks, so a default install logs no warning about them.
     */
    @GameTest(template = "empty")
    public static void bundledPrefabBlueprintsLackNoBlock(GameTestHelper helper) {
        List<String> prefabs = BundledContentTests.bundledTemplates(helper).stream()
                .filter(path -> path.startsWith("initialconfig/prefabs/"))
                .toList();
        assertTrue(helper, !prefabs.isEmpty(), "bundled prefabs should be found");
        List<String> lacking = prefabs.stream()
                .flatMap(path -> StructureSaver.getTemplateForReward(helper.getLevel().getServer(), path).getRight().getPalette().stream()
                        .filter(b -> !CapsuleTemplate.isKeptInBlueprint(b))
                        .map(b -> path + " " + BuiltInRegistries.BLOCK.getKey(b.state().getBlock())))
                .distinct()
                .toList();

        assertTrue(helper, lacking.isEmpty(), "blocks the blueprints of bundled prefabs lack: " + lacking);
        helper.succeed();
    }
}
