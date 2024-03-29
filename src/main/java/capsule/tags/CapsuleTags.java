package capsule.tags;

import capsule.CapsuleMod;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public class CapsuleTags {
    public static final  Tags.IOptionalNamedTag<Block> excludedBlocks = capsuleTag("excluded");

    private static Tags.IOptionalNamedTag<Block> capsuleTag(String name) {
        return BlockTags.createOptional(new ResourceLocation(CapsuleMod.MODID, name));
    }
}