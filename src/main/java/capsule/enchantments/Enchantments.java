package capsule.enchantments;

import com.google.common.base.Predicate;

import capsule.Config;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Property;

public class Enchantments {
	
	public static Enchantment recallEnchant = null;
	
	public static void initEnchantments(){
		Property enchantId = Config.config.get("Compatibility", "recallEnchantId", 101);
		enchantId.setComment("Id used to register the Enchantment \"Recall\".\n This enchantment allow a dropped item to come back into the thrower inventory (if not full) when it collided something.");
		
		Property enchantRarityConfig = Config.config.get("Balancing", "recallEnchantRarity", "RARE");
		enchantRarityConfig.setComment("Rarity of the enchantmant. Possible values : COMMON, UNCOMMON, RARE, VERY_RARE. Default: RARE.");
		Rarity enchantRarity = Rarity.RARE;
		try {
			enchantRarity = Rarity.valueOf(enchantRarityConfig.getString());
		} catch(Exception e) {
			System.err.println("Couldn't find the rarity "+enchantRarityConfig.getString()+". Using RARE instead.");
		}
				
		
		Property recallEnchantType = Config.config.get("Balancing", "recallEnchantType", "ALL");
		recallEnchantType.setComment("Possible targets for the enchantment. By default : ALL.\nPossible values are ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, null.\nIf null or empty, Capsules will be the only items to be able to get this Enchantment.");
		
		Config.config.save();
		
		Enchantments.recallEnchant = new RecallEnchant(
			new ResourceLocation("recall"), // name
			enchantRarity, // weight (chances to appear)
			Enum.valueOf(EnumEnchantmentType.class, recallEnchantType.getString()) // possible targets
		);
	}
	
	
	@SuppressWarnings("rawtypes")
	public static final Predicate hasRecallEnchant = new Predicate() {
		public boolean apply(Entity entityIn) {
			return entityIn instanceof EntityItem
					&& EnchantmentHelper.getEnchantmentLevel(Enchantments.recallEnchant, ((EntityItem) entityIn).getEntityItem()) > 0;
		}

		public boolean apply(Object obj) {
			return this.apply((Entity) obj);
		}
	};
}
