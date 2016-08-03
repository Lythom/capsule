package capsule.items.recipes;

import capsule.Config;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class UpgradeCapsuleRecipe implements IRecipe {
	/** Is the ItemStack that you repair. */
	private final Item upgradeItem;

	public UpgradeCapsuleRecipe(Item upgradeItem) {
		this.upgradeItem = upgradeItem;
	}

	public ItemStack getRecipeOutput() {
		return null;
	}

	public ItemStack[] getRemainingItems(InventoryCrafting inv) {
		ItemStack[] aitemstack = new ItemStack[inv.getSizeInventory()];

		for (int i = 0; i < aitemstack.length; ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			aitemstack[i] = net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack);
		}

		return aitemstack;
	}

	/**
	 * Used to check if a recipe matches current crafting inventory
	 */
	public boolean matches(InventoryCrafting craftingGrid, World worldIn) {

		ItemStack sourceCapsule = null;
    	int material = 0;
        for (int i = 0; i < craftingGrid.getHeight(); ++i)
        {
            for (int j = 0; j < craftingGrid.getWidth(); ++j)
            {
                ItemStack itemstack = craftingGrid.getStackInRowAndColumn(j, i);

                if (itemstack != null && itemstack.getItem() == CapsuleItemsRegistrer.capsule && itemstack.getItemDamage() == CapsuleItem.STATE_EMPTY && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit)
                {
                	sourceCapsule = itemstack;
                } 
                
                else if (itemstack != null && itemstack.getItem() == upgradeItem)
                {
                	material++;
                }
                
                else if (itemstack != null) {
                	return false;
                }
            }
        }

        return sourceCapsule != null && material > 0 && CapsuleItem.getUpgradeLevel(sourceCapsule) + material <= Config.upgradeLimit;
	}

	/**
	 * Returns an Item that is the result of this recipe
	 */
	public ItemStack getCraftingResult(InventoryCrafting craftingGrid) {

		ItemStack input = null;
    	int material = 0;
        for (int i = 0; i < craftingGrid.getHeight(); ++i)
        {
            for (int j = 0; j < craftingGrid.getWidth(); ++j)
            {
                ItemStack itemstack = craftingGrid.getStackInRowAndColumn(j, i);

                if (itemstack != null && itemstack.getItem() == CapsuleItemsRegistrer.capsule && itemstack.getItemDamage() == CapsuleItem.STATE_EMPTY && CapsuleItem.getUpgradeLevel(itemstack) < Config.upgradeLimit)
                {
                	input = itemstack;
                } 
                
                else if (itemstack != null && itemstack.getItem() == upgradeItem)
                {
                	material++;
                }
                
                else if (itemstack != null) {
                	return null;
                }
            }
        }
        
        if(input == null) return null;

		ItemStack copy = input.copy();
		int newSize = CapsuleItem.getSize(input) + material * 2;
		int newUpgraded = CapsuleItem.getUpgradeLevel(input) + material;
		
		if(newSize > CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE) newSize = CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE;
		if(newUpgraded > Config.upgradeLimit) newUpgraded = Config.upgradeLimit;
		
		CapsuleItem.setSize(copy, newSize);
		CapsuleItem.setUpgradeLevel(copy, newUpgraded);

		return copy;
	}

	/**
	 * Returns the size of the recipe area
	 */
	public int getRecipeSize() {
		return 10;
	}
}