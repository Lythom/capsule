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
        // a mod inventory
        assertTrue(holdsItems(Blocks.STONE, "{id:\"somemod:item_bus\",inventory:{slots:[{item:{id:\"minecraft:iron_ingot\",Count:5b}}]}}"));
        assertTrue(holdsItems(Blocks.LECTERN, "{id:\"minecraft:lectern\",Book:{id:\"minecraft:writable_book\",Count:1b},Page:0}"));
        assertTrue(holdsItems(Blocks.JUKEBOX, "{id:\"minecraft:jukebox\",RecordItem:{id:\"minecraft:music_disc_cat\",Count:1b}}"));
        // the stack format of 1.20.5 and later
        assertTrue(holdsItems(Blocks.STONE, "{stored:{id:\"minecraft:diamond\",count:3}}"));
        // a non-empty Items list is refused as before, whatever its entries
        assertTrue(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",Items:[{Slot:0b}]}"));
    }

    @Test
    void otherDataIsNotTakenForItems() throws CommandSyntaxException {
        assertFalse(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",Items:[]}"));
        // the fields of the block entity itself
        assertFalse(holdsItems(Blocks.CHEST, "{id:\"minecraft:chest\",Count:3b}"));
        // fluids, entities, empty slots, air and unknown items
        assertFalse(holdsItems(Blocks.STONE, "{tank:{FluidName:\"minecraft:water\",Amount:1000},Bees:[{EntityData:{id:\"minecraft:bee\"}}],"
                + "slots:[{},{item:{id:\"minecraft:air\",Count:1b}},{item:{id:\"unknown:item\",Count:1b}},{item:{id:\"minecraft:diamond\",Count:0b}}]}"));
    }

    @Test
    void itemsInTheNBTTheWhitelistKeepsAreIgnored() throws CommandSyntaxException {
        String filter = "{filter:{id:\"minecraft:diamond\",Count:1b}}";
        assertTrue(holdsItems(Blocks.BELL, filter));
        JsonObject keepNBT = new JsonObject();
        keepNBT.add("filter", JsonNull.INSTANCE);
        boolean listed = Config.blueprintWhitelist.containsKey("minecraft:bell");
        JsonObject previous = Config.blueprintWhitelist.put("minecraft:bell", keepNBT);
        try {
            assertFalse(holdsItems(Blocks.BELL, filter));
            assertTrue(holdsItems(Blocks.BELL, "{filter:{id:\"minecraft:diamond\",Count:1b},inventory:[{id:\"minecraft:diamond\",Count:1b}]}"));
        } finally {
            if (listed) Config.blueprintWhitelist.put("minecraft:bell", previous);
            else Config.blueprintWhitelist.remove("minecraft:bell");
        }
    }
}
