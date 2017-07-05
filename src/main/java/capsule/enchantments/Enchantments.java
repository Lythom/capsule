package capsule.enchantments;

import com.google.common.base.Predicate;

import capsule.Config;
import capsule.Main;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class Enchantments {
	
	public static Enchantment recallEnchant = null;
	
	public static void initEnchantments(){

		Rarity enchantRarity = Rarity.RARE;
		try {
			enchantRarity = Rarity.valueOf(Config.enchantRarity);
		} catch(Exception e) {
			System.err.println("Couldn't find the rarity "+Config.enchantRarity+". Using RARE instead.");
		}
		
		EnumEnchantmentType recallEnchantTypeEnumValue = null;
		try {
			recallEnchantTypeEnumValue = EnumEnchantmentType.valueOf(Config.recallEnchantType);
		} catch (IllegalArgumentException e) {
		}
		
		Enchantments.recallEnchant = new RecallEnchant(
			new ResourceLocation(Main.MODID, "recall"), // name
			enchantRarity, // weight (chances to appear)
			recallEnchantTypeEnumValue // possible targets
		);
		
		GameRegistry.register(Enchantments.recallEnchant);
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
