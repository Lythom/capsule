package capsule.loot;

import capsule.CapsuleMod;
import capsule.Config;
import net.minecraft.world.storage.loot.ConstantRange;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapsuleLootTableHook {
    public static LootPool capsulePool = null;

    public CapsuleLootTableHook() {
    }

    @SubscribeEvent
    public static void hookCapsulesOnLootTable(LootTableLoadEvent event) {

        if (!Config.lootTablesList.get().contains(event.getName().toString()))
            return;

        // create a capsule loot entry per folder
        if (capsulePool == null) {
            LootPool.Builder capsulePoolBuilder = LootPool.builder()
                    .bonusRolls(0, 0)
                    .name("capsulePool")
                    .rolls(ConstantRange.of(1));
            for (Config.LootPathData data : Config.lootTemplatesData.values()) {
                capsulePoolBuilder.addEntry(CapsuleLootEntry.builder(data.path));
            }
            capsulePool = capsulePoolBuilder.build();
        }

        // add a new pool containing all weighted entries
        event.getTable().addPool(capsulePool);
    }
}
