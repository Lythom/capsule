package capsule.tags;

import capsule.CapsuleMod;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

public class CapsuleTags {
    public static final Tag<Block> excludedBlocks = capsuleTag("excluded");

    private static Tag<Block> capsuleTag(String name) {
        return new BlockTags.Wrapper(new ResourceLocation(CapsuleMod.MODID, name));
    }
}