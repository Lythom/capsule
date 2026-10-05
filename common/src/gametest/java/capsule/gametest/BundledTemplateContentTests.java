package capsule.gametest;

import capsule.StructureSaver;
import capsule.structure.CapsuleTemplate;
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
}
