package capsule.gametest;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class BlueprintCraftingTests {

    @GameTest(template = "empty")
    public static void missingPrefabTemplateIsReported(GameTestHelper helper) {
        List<Component> chat = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(1, 1, 1), chat);
        String source = "config/capsule/prefabs/missing_prefab";
        ItemStack blueprint = Capsule.newLinkedCapsuleItemStack(source, 0x3BB3FC, 0xFFFFFF, 3, false, null, 0);
        CapsuleItem.setBlueprint(blueprint);

        try (LogCapture logs = LogCapture.open()) {
            CapsuleItem.duplicateBlueprintTemplate(blueprint, helper.getLevel(), player);
            assertTrue(helper, logs.containing(source).stream().anyMatch(m -> m.contains("missing_prefab.nbt")),
                    "the error should name the template and the searched paths, got " + logs.messages());
        }
        CapsuleTestUtils.removePlayer(player);

        assertTrue(helper, chat.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals("capsule.error.blueprintSourceNotFound")),
                "the player should be told, got " + chat);
        assertTrue(helper, source.equals(CapsuleItem.getStructureName(blueprint)), "no empty blueprint template should be created, got " + CapsuleItem.getStructureName(blueprint));
        helper.succeed();
    }
}
