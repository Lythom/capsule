package capsule;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Items held by the block entities of a deployed blueprint, which an undeploy would destroy.
 */
class BlueprintItemsTest {

    /**
     * Already done by NeoForge's test launcher, not by Fabric's.
     */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static boolean holdsItems(Block block, String snbt) throws CommandSyntaxException {
        return StructureSaver.holdsItems(new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, block.defaultBlockState(), TagParser.parseTag(snbt)));
    }

    @Test
    void itemsAreFoundUnderAnyKey() throws CommandSyntaxException {
        // a Modular Machinery Reborn item bus
        assertTrue(holdsItems(Blocks.STONE, "{id:\"modular_machinery_reborn:item_input_bus\",inventory:{slots:[{item:{id:\"minecraft:iron_ingot\",count:5}}]}}"));
        assertTrue(holdsItems(Blocks.LECTERN, "{id:\"minecraft:lectern\",Book:{id:\"minecraft:writable_book\",count:1},Page:0}"));
        // the stack format before 1.20.5
        assertTrue(holdsItems(Blocks.STONE, "{stored:{id:\"minecraft:diamond\",Count:3b}}"));
        // a non-empty Items list is refused as before, whatever its entries
        assertTrue(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",Items:[{Slot:0b}]}"));
    }

    @Test
    void otherDataIsNotTakenForItems() throws CommandSyntaxException {
        assertFalse(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",Items:[]}"));
        // the fields of the block entity itself
        assertFalse(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",count:3}"));
        assertFalse(holdsItems(Blocks.DECORATED_POT, "{id:\"minecraft:decorated_pot\",sherds:[\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\",\"minecraft:brick\"]}"));
        // fluids, entities, empty slots, air and unknown items
        assertFalse(holdsItems(Blocks.STONE, "{tank:{id:\"minecraft:water\",amount:1000},bees:[{entity_data:{id:\"minecraft:bee\"}}],"
                + "slots:[{},{item:{id:\"minecraft:air\",count:1}},{item:{id:\"unknown:item\",count:1}},{item:{id:\"minecraft:diamond\",count:0}}]}"));
    }

    @Test
    void itemsInTheNBTTheWhitelistKeepsAreIgnored() throws CommandSyntaxException {
        String filter = "{filter:{id:\"minecraft:diamond\",count:1}}";
        assertTrue(holdsItems(Blocks.BELL, filter));
        JsonObject keepNBT = new JsonObject();
        keepNBT.add("filter", JsonNull.INSTANCE);
        boolean listed = Config.blueprintWhitelist.containsKey("minecraft:bell");
        JsonObject previous = Config.blueprintWhitelist.put("minecraft:bell", keepNBT);
        try {
            assertFalse(holdsItems(Blocks.BELL, filter));
            assertTrue(holdsItems(Blocks.BELL, "{filter:{id:\"minecraft:diamond\",count:1},inventory:[{id:\"minecraft:diamond\",count:1}]}"));
        } finally {
            if (listed) Config.blueprintWhitelist.put("minecraft:bell", previous);
            else Config.blueprintWhitelist.remove("minecraft:bell");
        }
    }
}
