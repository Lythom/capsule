package capsule.gametest;

import capsule.CapsuleMod;
import capsule.loot.CapsuleLootEntry;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class LootTests {

    @GameTest(template = "empty")
    public static void capsuleLootEntryRoundTrips(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootPoolEntryContainer entry = CapsuleLootEntry.builder("config/capsule/loot/common").build();

        JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, entry).getOrThrow();
        LootPoolEntryContainer decoded = LootPoolEntries.CODEC.parse(ops, json).getOrThrow();

        assertTrue(helper, decoded instanceof CapsuleLootEntry, "decoded entry should be a capsule entry: " + json);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lootTablesHoldingCapsulesEncode(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);

        LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void defaultLootTablesHoldTheCapsulePool(GameTestHelper helper) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);

        assertTrue(helper, table.getPool("capsulePool") != null, "simple dungeon should hold the capsule pool");
        helper.succeed();
    }
}
