package capsule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {

    @Test
    void lootTableIdsGeneratedByEarlier121VersionsAreUnderstood() {
        assertEquals("minecraft:chests/simple_dungeon", Config.lootTableId("ResourceKey[minecraft:loot_table / minecraft:chests/simple_dungeon]"));
        assertEquals("minecraft:chests/simple_dungeon", Config.lootTableId("minecraft:chests/simple_dungeon"));
        assertEquals("minecraft:gameplay/fishing/treasure", Config.lootTableId("gameplay/fishing/treasure"));
    }
}
