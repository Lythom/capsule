package capsule.enchantments;

import capsule.items.CapsuleItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class RecallEnchant extends Enchantment {

	protected RecallEnchant(int enchID, ResourceLocation enchName, int enchWeight, EnumEnchantmentType enchType) {
		super(enchID, enchName, enchWeight, enchType);
		this.setName("recall");
	}
	
	@Override
	public boolean canApply(ItemStack stack) {
		return canApplyAtEnchantingTable(stack);
	}
	
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return stack.getItem() instanceof CapsuleItem || this.type != null && super.canApplyAtEnchantingTable(stack);
	}
	
	

}
