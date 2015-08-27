package capsule.items.recipes;

import capsule.items.CapsuleItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class RecoveryCapsuleRecipe implements IRecipe
{
    /** Is the ItemStack that you repair. */
    private final ItemStack inputCapsule;
    private final ItemStack inputMaterial;
	private final int targetMetadata;

    public RecoveryCapsuleRecipe(ItemStack inputCapsule, ItemStack inputMaterial, int targetMetadata)
    {
        this.inputCapsule = inputCapsule;
        this.inputMaterial = inputMaterial;
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
            if(aitemstack[i] == null && itemstack != null && itemstack.getItem() instanceof CapsuleItem){
            	aitemstack[i] = itemstack;
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
    	int material = 0;
        for (int i = 0; i < p_77569_1_.getHeight(); ++i)
        {
            for (int j = 0; j < p_77569_1_.getWidth(); ++j)
            {
                ItemStack itemstack = p_77569_1_.getStackInRowAndColumn(j, i);

                if (itemstack != null && itemstack.getItem() == this.inputCapsule.getItem() && (itemstack.getMetadata() == this.inputCapsule.getMetadata()))
                {
                	sourceCapsule++;
                }
                
                if (itemstack != null && itemstack.getItem() == this.inputMaterial.getItem() && (itemstack.getMetadata() == this.inputMaterial.getMetadata()))
                {
                	material++;
                }
            }
        }

        return sourceCapsule == 1 && material == 1;
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

                if (itemstack != null && itemstack.getItem() == this.inputCapsule.getItem() && itemstack.getMetadata() == this.inputCapsule.getMetadata())
                {
                	ItemStack copy = itemstack.copy();
                	CapsuleItem item = (CapsuleItem)copy.getItem();
                	item.setState(copy, this.targetMetadata);
                	if(!copy.hasTagCompound()){
                		copy.setTagCompound(new NBTTagCompound());
                	}
                	copy.getTagCompound().setBoolean("oneUse", true);
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