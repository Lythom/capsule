package capsule.items.recipes;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class RepairCapsuleRecipe implements IRecipe
{
    /** Is the ItemStack that you repair. */
    private final ItemStack input;
	private final int targetMetadata;

    public RepairCapsuleRecipe(ItemStack input, int targetMetadata)
    {
        this.input = input;
		this.targetMetadata = targetMetadata;
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
        }

        return aitemstack;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(InventoryCrafting p_77569_1_, World worldIn)
    {
        for (int i = 0; i < p_77569_1_.getHeight(); ++i)
        {
            for (int j = 0; j < p_77569_1_.getWidth(); ++j)
            {
                ItemStack itemstack = p_77569_1_.getStackInRowAndColumn(j, i);

                if (itemstack != null && itemstack.getItem() == this.input.getItem() && (itemstack.getMetadata() == this.input.getMetadata()))
                {
                    return true;
                }
            }
        }

        return false;
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

                if (itemstack != null)
                {
                	ItemStack copy = itemstack.copy();
                	copy.setItemDamage(this.targetMetadata);
                    return copy;
                }
            }
        }
        return null;
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize()
    {
        return 4;
    }
}