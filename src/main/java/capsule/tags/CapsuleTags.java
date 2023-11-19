package capsule.tags;

import capsule.CapsuleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class CapsuleTags {
    public static final TagKey<Block> excludedBlocks = capsuleTag("excluded");
    public static final TagKey<Block> overridable = capsuleTag("overridable");

    private static TagKey<Block> capsuleTag(String name) {
        return BlockTags.create(new ResourceLocation(CapsuleMod.MODID, name));
    }
}