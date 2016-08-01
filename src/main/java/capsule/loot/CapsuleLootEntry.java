/**
 * 
 */
package capsule.loot;

import java.util.Collection;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import capsule.Config;
import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;

/**
 * @author Lythom
 *
 */		
public class CapsuleLootEntry extends LootEntry {

	private String templatesPath = null;
	private static Random random = new Random();

	/**
	 * 
	 * @param templatesPath
	 * @param weightIn
	 * @param qualityIn
	 * @param conditionsIn
	 * @param entryName
	 */
	protected CapsuleLootEntry(String path, int weightIn, int qualityIn, LootCondition[] conditionsIn, String entryName) {
		super(weightIn, qualityIn, conditionsIn, entryName);
		this.templatesPath  = path;
	}

	/**
	 * Add all eligible capsules to the list to be picked from.
	 */
	@Override
	public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
		if(this.templatesPath == null) return;
		
		if (LootConditionManager.testAllConditions(this.conditions, rand, context) && Config.lootTemplatesData.containsKey(this.templatesPath))
        {
			
			Pair<String,Template> templatePair = getRandomTemplate(context);

			if(templatePair != null){
				Template template = templatePair.getRight();
				String templatePath = templatePair.getLeft();
				int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
				String[] path = templatePath.split("/");
				if(path.length == 0) return;
				
				ItemStack capsule = CapsuleItem.createRewardCapsule(templatePath, random.nextInt(0xFFFFFF), random.nextInt(0xFFFFFF), size, false, path[path.length-1], null);
	            stacks.add(capsule);
			}
			
        }

	}

	public Pair<String,Template> getRandomTemplate(LootContext context) {
		LootPathData lpd = Config.lootTemplatesData.get(this.templatesPath);
		if(lpd == null) return null;
		
		int initRand = random.nextInt(lpd.files.size());
		
		for (int i = 0; i < lpd.files.size(); i++) {
			int ri = (initRand + i) % lpd.files.size();
			String structureName = lpd.files.get(ri);
			Template template = StructureSaver.getTemplateForReward(context.getWorld().getMinecraftServer(), this.templatesPath + "/" + structureName);
			if(template != null) return Pair.of(this.templatesPath + "/" + structureName, template);
		}
		return null;
	}

	@Override
	protected void serialize(JsonObject json, JsonSerializationContext context) {
		
	}

}
