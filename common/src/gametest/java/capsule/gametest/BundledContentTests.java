package capsule.gametest;

import capsule.CapsuleMod;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.structure.CapsuleTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class BundledContentTests {

    /**
     * Template paths (without extension) of every structure shipped in data/capsule/initialconfig.
     */
    static List<String> bundledTemplates(GameTestHelper helper) {
        List<String> paths = new ArrayList<>();
        helper.getLevel().getServer().getResourceManager()
                .listResources("initialconfig", rl -> rl.getNamespace().equals(CapsuleMod.MODID) && rl.getPath().endsWith(".nbt"))
                .keySet()
                .forEach(rl -> paths.add(rl.getPath().substring(0, rl.getPath().length() - ".nbt".length())));
        paths.sort(String::compareTo);
        return paths;
    }

    @GameTest(template = "empty17", timeoutTicks = 400)
    public static void everyBundledTemplateDeploys(GameTestHelper helper) {
        List<String> templates = bundledTemplates(helper);
        assertTrue(helper, templates.size() >= 40, "bundled templates should be found, got " + templates.size());
        BlockPos anchor = new BlockPos(8, 0, 8);
        List<String> failures = new ArrayList<>();

        for (String path : templates) {
            CapsuleTemplate template = StructureSaver.getTemplateForReward(helper.getLevel().getServer(), path).getRight();
            if (template == null || template.getPalette().isEmpty() && template.entities.isEmpty()) {
                failures.add(path + " (empty or unreadable)");
                continue;
            }
            int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
            if (size % 2 == 0) size++;
            ItemStack capsule = Capsule.newRewardCapsuleItemStack(path, 0, 0, size, null, null);
            if (!CapsuleTestUtils.deploy(helper, capsule, anchor, null)) {
                failures.add(path + " (deploy failed)");
            }
            CapsuleTestUtils.clear(helper, new BlockPos(0, 1, 0), new BlockPos(16, 16, 16));
        }

        assertTrue(helper, failures.isEmpty(), "templates failing to deploy: " + failures);
        helper.succeed();
    }
}
