package capsule.items.recipes;

import capsule.items.CapsuleItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class ClearCapsuleRecipe implements IRecipe
{

	public ClearCapsuleRecipe() {
	}

    public ItemStack getRecipeOutput()
    {
        return null;
    }

    public ItemStack[] getRemainingItems(InventoryCrafting p_179532_1_)
    {
        ItemStack[] aitemstack = new ItemStack[p_179532_1_.getSizeInventory()];

        for (int i = 0; i < aitemstack.length; ++i)
        {
            ItemStack itemstack = p_179532_1_.getStackInSlot(i);
            aitemstack[i] = net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack);
            if(aitemstack[i] == null && itemstack != null && itemstack.getItem() instanceof CapsuleItem){
            	// Copy the capsule and give back a recovery capsule of the previous content
            	ItemStack copy = itemstack.copy();
            	CapsuleItem item = (CapsuleItem)copy.getItem();
				if (item != null) {
                	CapsuleItem.setOneUse(copy);
					aitemstack[i] = copy;
				}
            }
        }

        return aitemstack;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(InventoryCrafting p_77569_1_, World worldIn)
    {
    	int sourceCapsule = 0;
        for (int i = 0; i < p_77569_1_.getHeight(); ++i)
        {
            for (int j = 0; j < p_77569_1_.getWidth(); ++j)
            {
                ItemStack itemstack = p_77569_1_.getStackInRowAndColumn(j, i);

                if (isLinkedCapsule(itemstack))
                {
                	sourceCapsule++;
                } 
                else if (itemstack != null) {
                	return false;
                }
            }
        }

        return sourceCapsule == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting invC)
    {
    	for (int i = 0; i < invC.getHeight(); ++i)
        {
            for (int j = 0; j < invC.getWidth(); ++j)
            {
                ItemStack itemstack = invC.getStackInRowAndColumn(j, i);

                if (isLinkedCapsule(itemstack))
                {
                	ItemStack copy = itemstack.copy();
                	CapsuleItem item = (CapsuleItem)copy.getItem();
                	if (item != null) {
    					item.clearCapsule(copy);
    				}
                    return copy;
                }
            }
        }
        return null;
    }
    
    private boolean isLinkedCapsule(ItemStack itemstack) {
    	return (itemstack != null && itemstack.getItem() instanceof CapsuleItem && CapsuleItem.STATE_LINKED == itemstack.getMetadata());
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize()
    {
        return 4;
    }
}