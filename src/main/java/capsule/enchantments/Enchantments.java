package capsule.enchantments;

import com.google.common.base.Predicate;

import capsule.Config;
import net.minecraft.enchantment.Enchantment;
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
		enchantId.comment = "Id used to register the Enchantment \"Recall\".\n This enchantment allow an dropped item to come back into the thrower inventory (if not full) when after it collided with something.";
		
		Property enchantWeight = Config.config.get("Balancing", "recallEnchantWeight", 1);
		enchantWeight.comment = "The higher, the better chance to get this enchant on an item. Default 1. Max vanilla: 10.";
		
		Property recallEnchantType = Config.config.get("Balancing", "recallEnchantType", "ALL");
		recallEnchantType.comment = "Possible targets for the enchantment. By default : ALL.\nPossible values are ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, null.\nIf null or empty, Capsules will be the only items to be able to get this Enchantment.";
		
		Enchantments.recallEnchant = new RecallEnchant(
			enchantId.getInt(), // id
			new ResourceLocation("recall"), // name
			enchantWeight.getInt(), // weight (chances to appear)
			Enum.valueOf(EnumEnchantmentType.class, recallEnchantType.getString()) // possible targets
		);
	}
	
	
	@SuppressWarnings("rawtypes")
	public static final Predicate hasRecallEnchant = new Predicate() {
		public boolean apply(Entity entityIn) {
			return entityIn instanceof EntityItem
					&& EnchantmentHelper.getEnchantmentLevel(Enchantments.recallEnchant.effectId, ((EntityItem) entityIn).getEntityItem()) > 0;
		}

		public boolean apply(Object obj) {
			return this.apply((Entity) obj);
		}
	};
}
