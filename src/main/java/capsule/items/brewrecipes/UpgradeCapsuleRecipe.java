package capsule.items.brewrecipes;

import capsule.Config;
import capsule.items.CapsuleItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class UpgradeCapsuleRecipe implements IBrewingRecipe {
	
	@Override
	public boolean isInput(ItemStack input) {
		
		return input.getItem() instanceof CapsuleItem && CapsuleItem.STATE_EMPTY == input.getItemDamage() && CapsuleItem.getUpgradeLevel(input) < Config.upgradeLimit  && CapsuleItem.getSize(input) < CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
	}

	@Override
	public boolean isIngredient(ItemStack ingredient) {
		return Items.CHORUS_FRUIT_POPPED.equals(ingredient.getItem());
	}

	@Override
	public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
		
		if(isIngredient(ingredient) && isInput(input)){
			
			ItemStack copy = input.copy();
			int newSize = CapsuleItem.getSize(input) + 2;
			int newUpgraded = CapsuleItem.getUpgradeLevel(input) + 1;
			
			if(newSize > CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE) newSize = CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
			if(newUpgraded > Config.upgradeLimit) newUpgraded = Config.upgradeLimit;
			
			CapsuleItem.setSize(copy, newSize);
			CapsuleItem.setUpgradeLevel(copy, newUpgraded);
			
			return copy;
		}
		
		return null;

	}
}