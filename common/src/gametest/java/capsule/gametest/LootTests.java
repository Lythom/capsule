package capsule.gametest;

import capsule.loot.CapsuleLootEntry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class LootTests {

    @CapsuleGameTest(template = "empty")
    public static void capsuleLootEntryRoundTrips(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootPoolEntryContainer entry = CapsuleLootEntry.builder("config/capsule/loot/common").build();

        JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, entry).getOrThrow();
        LootPoolEntryContainer decoded = LootPoolEntries.CODEC.parse(ops, json).getOrThrow();

        assertTrue(helper, decoded instanceof CapsuleLootEntry, "decoded entry should be a capsule entry: " + json);
        helper.succeed();
    }

    @CapsuleGameTest(template = "empty")
    public static void lootTablesHoldingCapsulesEncode(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);

        LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow();
        helper.succeed();
    }

    @CapsuleGameTest(template = "empty")
    public static void defaultLootTablesHoldTheCapsulePool(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);

        JsonObject json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
        boolean hasCapsuleEntry = json.getAsJsonArray("pools").asList().stream()
                .flatMap(pool -> pool.getAsJsonObject().getAsJsonArray("entries").asList().stream())
                .anyMatch(entry -> "capsule:capsule".equals(entry.getAsJsonObject().get("type").getAsString()));
        assertTrue(helper, hasCapsuleEntry, "simple dungeon should hold the capsule pool");
        helper.succeed();
    }

    /**
     * The capture base drops itself only when it survives an explosion, as vanilla blocks. Since 26.3 a pool has one
     * "condition" object instead of a "conditions" list, and the old key is ignored without a warning.
     */
    @CapsuleGameTest(template = "empty")
    public static void captureBaseDropNeedsToSurviveTheExplosion(GameTestHelper helper) {
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("capsule", "blocks/capsulemarker")));

        JsonObject json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
        JsonObject pool = json.getAsJsonArray("pools").get(0).getAsJsonObject();
        assertTrue(helper, pool.has("condition") && pool.get("condition").toString().contains("minecraft:survives_explosion"),
                "the capture base pool should have the survives_explosion condition: " + json);
        helper.succeed();
    }
}
