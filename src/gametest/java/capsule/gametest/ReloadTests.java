package capsule.gametest;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.template;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class ReloadTests {

    @GameTest(template = "empty", batch = "reload", timeoutTicks = 400)
    public static void reloadRefreshesRewardTemplates(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        String path = Config.rewardTemplatesPath + "/reload_test";
        CapsuleTemplateManager rewards = StructureSaver.getRewardManager(server.getResourceManager());
        CapsuleTemplateManager otherManager = new CapsuleTemplateManager(server.getResourceManager(), new File("."), DataFixers.getDataFixer());

        helper.setBlock(1, 1, 1, Blocks.STONE);
        StructureSaver.duplicateTemplate(StructureSaver.getTemplateNBTData(template(helper, capture(helper, new BlockPos(0, 1, 0), 3))), path, rewards, server);
        helper.setBlock(1, 1, 1, Blocks.STONE);
        helper.setBlock(1, 2, 1, Blocks.STONE);
        StructureSaver.duplicateTemplate(StructureSaver.getTemplateNBTData(template(helper, capture(helper, new BlockPos(0, 1, 0), 3))), path, otherManager, server);
        assertTrue(helper, rewards.getTemplate(ResourceLocation.parse(path)).getPalette().size() == 1, "reward templates are cached");

        CompletableFuture<Void> reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            assertTrue(helper, reload.isDone(), "reloading");
            assertTrue(helper, rewards.getTemplate(ResourceLocation.parse(path)).getPalette().size() == 2, "/reload should refresh reward templates");
        });
    }
}
