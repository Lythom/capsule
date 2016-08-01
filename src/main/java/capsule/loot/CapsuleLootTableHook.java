package capsule.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import capsule.Config;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CapsuleLootTableHook {

	public CapsuleLootTableHook() {

	}

	public static List<String> allowedTables = Arrays.asList(new String[] {
			LootTableList.CHESTS_ABANDONED_MINESHAFT.toString(),
			LootTableList.CHESTS_DESERT_PYRAMID.toString(),
			LootTableList.CHESTS_END_CITY_TREASURE.toString(),
			LootTableList.CHESTS_IGLOO_CHEST.toString(),
			LootTableList.CHESTS_JUNGLE_TEMPLE.toString(),
			LootTableList.CHESTS_SIMPLE_DUNGEON.toString(),
			LootTableList.CHESTS_STRONGHOLD_CORRIDOR.toString(),
			LootTableList.CHESTS_STRONGHOLD_CROSSING.toString(),
			LootTableList.CHESTS_STRONGHOLD_LIBRARY.toString(),
			LootTableList.CHESTS_VILLAGE_BLACKSMITH.toString(),
			LootTableList.GAMEPLAY_FISHING_TREASURE.toString()
	});
	public static LootPool capsulePool = null;

	@SubscribeEvent
	public void hookCapsulesOnLootTable(LootTableLoadEvent event) {

		if (!allowedTables.contains(event.getName().toString()))
			return;

		// create a capsule loot entry per folder
		if(capsulePool == null){
			List<CapsuleLootEntry> entries = new ArrayList<>();
			for (String path : Config.lootTemplatesPaths) {
				int weight = findConfiguredWeight(path, 3);
				entries.add(new CapsuleLootEntry(path, weight, 0, new LootCondition[0], "capsule:capsuleLootsEntry" + path.replace("/", "_")));
			}
			
			capsulePool = new LootPool(
					entries.toArray(new LootEntry[0]), 	// the loot is taken from a Capsule managed entry list
					new LootCondition[0], 				// no particular condition, always loot one capsule
					new RandomValueRange(1.0F, 1.0F), 	// spawn one capsule using that pool
					new RandomValueRange(0.0F, 0.0F), 	// no extra capsules
					"capsulePool");
		}
		
		// add a new pool containing all weighted entries
		event.getTable().addPool(capsulePool);

	}

	public int findConfiguredWeight(String path, int defaultWeight) {
		int weight = defaultWeight;
		if (Config.lootTemplatesData.containsKey(path)) {
			weight = Config.lootTemplatesData.get(path).weigth;
		}
		return weight;
	}
}
