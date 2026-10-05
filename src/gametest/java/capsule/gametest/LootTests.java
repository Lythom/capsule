package capsule.gametest;

import capsule.CapsuleMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class LootTests {

    @GameTest(template = "empty")
    public static void defaultLootTablesHoldTheCapsulePool(GameTestHelper helper) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);

        assertTrue(helper, table.getPool("capsulePool") != null, "simple dungeon should hold the capsule pool");
        helper.succeed();
    }
}
