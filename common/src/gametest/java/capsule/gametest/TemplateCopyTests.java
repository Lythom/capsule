package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;
import static capsule.gametest.CapsuleTestUtils.template;

public class TemplateCopyTests {

    @GameTest(template = "empty")
    public static void templateCopiesDoNotShareBlockEntityData(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1));
        chest.setItem(0, new ItemStack(Items.DIAMOND));
        ItemStack source = capture(helper, new BlockPos(1, 1, 1), 1);
        CapsuleTemplate sourceTemplate = template(helper, source);
        String copyName = CapsuleItem.getStructureName(source) + "-copy";

        StructureSaver.duplicateTemplate(StructureSaver.getTemplateNBTData(sourceTemplate), copyName, StructureSaver.getTemplateManager(helper.getLevel().getServer()), helper.getLevel().getServer());
        ItemStack copy = Capsule.newLinkedCapsuleItemStack(copyName, 0, 0, 1, false, null, 0);
        CompoundTag copyNbt = template(helper, copy).getPalette().getFirst().nbt();
        assertTrue(helper, deploy(helper, copy, new BlockPos(4, 0, 4), null), "copy should deploy");

        CompoundTag sourceNbt = sourceTemplate.getPalette().getFirst().nbt();
        assertTrue(helper, sourceNbt != copyNbt, "source and copy share the same block entity tag");
        assertTrue(helper, !sourceNbt.contains("x"), "deploying the copy modified the source template: " + sourceNbt);
        helper.assertContainerContains(new BlockPos(4, 1, 4), Items.DIAMOND);
        helper.succeed();
    }
}
