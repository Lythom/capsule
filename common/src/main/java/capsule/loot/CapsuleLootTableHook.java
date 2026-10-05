package capsule.loot;

import capsule.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import javax.annotation.Nullable;

public class CapsuleLootTableHook {

    /**
     * The pool the loaders add to a loot table listed in the config, null for other tables.
     */
    @Nullable
    public static LootPool.Builder capsulePool(ResourceLocation lootTable) {
        return Config.lootTablesList.contains(lootTable.toString()) ? capsulePool() : null;
    }

    public static LootPool.Builder capsulePool() {
        // create a capsule loot entry per folder — rebuild each time to pick up config reloads
        LootPool.Builder capsulePoolBuilder = LootPool.lootPool()
                .setBonusRolls(ConstantValue.exactly(0))
                .setRolls(ConstantValue.exactly(1));
        for (Config.LootPathData data : Config.lootTemplatesData.values()) {
            capsulePoolBuilder.add(CapsuleLootEntry.builder(data.path));
        }
        return capsulePoolBuilder;
    }
}
