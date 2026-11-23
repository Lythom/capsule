package capsule.loot;

import capsule.Config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import javax.annotation.Nullable;

public class CapsuleLootTableHook {

    /**
     * The pool the loaders add to a loot table listed in the config, null for other tables.
     */
    @Nullable
    public static LootPool.Builder capsulePool(Identifier lootTable) {
        return Config.lootTablesList.contains(lootTable.toString()) ? capsulePool() : null;
    }

    public static LootPool.Builder capsulePool() {
        // create a capsule loot entry per folder — rebuild each time to pick up config reloads
        LootPool.Builder capsulePoolBuilder = LootPool.lootPool()
                .setBonusRolls(ContextFloatProviders.exactly(0f))
                .setRolls(ContextIntProviders.exactly(1));
        for (Config.LootPathData data : Config.lootTemplatesData.values()) {
            capsulePoolBuilder.add(CapsuleLootEntry.builder(data.path));
        }
        return capsulePoolBuilder;
    }
}
