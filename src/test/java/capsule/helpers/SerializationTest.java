package capsule.helpers;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest {

    @Test
    void invalidIdsAreIgnoredInsteadOfCrashing() {
        List<Block> blocks = Serialization.deserializeBlockList(List.of("Bad Id", "minecraft:stone", "Minecraft:Spawner"));

        assertEquals(List.of(Blocks.STONE), blocks);
    }

    @Test
    void tagsAreReadWithOrWithoutHash() {
        List<TagKey<Block>> tags = Serialization.deserializeBlockTags(List.of("minecraft:beds", "#minecraft:logs", "minecraft:stone", "Bad Id", "ic2:"));

        assertEquals(List.of(BlockTags.BEDS, BlockTags.LOGS), tags);
    }

    @Test
    void tagsAreNotResolvedToAir() {
        List<Block> blocks = Serialization.deserializeBlockList(List.of("minecraft:beds", "#minecraft:beds", "minecraft:stone"));

        assertTrue(blocks.contains(Blocks.STONE));
        assertFalse(blocks.contains(Blocks.AIR));
    }
}
