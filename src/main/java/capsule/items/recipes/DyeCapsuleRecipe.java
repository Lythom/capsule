package capsule.items.recipes;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import capsule.Helpers;
import capsule.items.CapsuleItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class DyeCapsuleRecipe implements IRecipe
{

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean matches(InventoryCrafting p_77569_1_, World worldIn)
    {
        ItemStack itemstack = null;
		ArrayList arraylist = Lists.newArrayList();

        for (int i = 0; i < p_77569_1_.getSizeInventory(); ++i)
        {
            ItemStack itemstack1 = p_77569_1_.getStackInSlot(i);

            if (itemstack1 != null)
            {
                if (itemstack1.getItem() instanceof CapsuleItem)
                {
                    itemstack = itemstack1;
                }
                else
                {
                    if (itemstack1.getItem() != Items.DYE)
                    {
                        return false;
                    }

                    arraylist.add(itemstack1);
                }
            }
        }

        return itemstack != null && !arraylist.isEmpty();
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting invCrafting)
    {
        ItemStack itemstack = null;
        int[] aint = new int[3];
        int i = 0;
        int j = 0;
        CapsuleItem itemarmor = null;
        int k;
        int l;
        float f;
        float f1;
        int l1;

        for (k = 0; k < invCrafting.getSizeInventory(); ++k)
        {
            ItemStack itemstack1 = invCrafting.getStackInSlot(k);

            if (itemstack1 != null)
            {
                if (itemstack1.getItem() instanceof CapsuleItem)
                {
                    itemarmor = (CapsuleItem)itemstack1.getItem();
                    itemstack = itemstack1.copy();

                    if (Helpers.hasColor(itemstack1))
                    {
                        l = Helpers.getColor(itemstack);
                        f = (float)(l >> 16 & 255) / 255.0F;
                        f1 = (float)(l >> 8 & 255) / 255.0F;
                        float f2 = (float)(l & 255) / 255.0F;
                        i = (int)((float)i + Math.max(f, Math.max(f1, f2)) * 255.0F);
                        aint[0] = (int)((float)aint[0] + f * 255.0F);
                        aint[1] = (int)((float)aint[1] + f1 * 255.0F);
                        aint[2] = (int)((float)aint[2] + f2 * 255.0F);
                        ++j;
                    }
                }
                else
                {
                    if (itemstack1.getItem() != Items.DYE)
                    {
                        return null;
                    }

                    float[] afloat = EntitySheep.getDyeRgb(EnumDyeColor.byDyeDamage(itemstack1.getMetadata()));
                    int j1 = (int)(afloat[0] * 255.0F);
                    int k1 = (int)(afloat[1] * 255.0F);
                    l1 = (int)(afloat[2] * 255.0F);
                    i += Math.max(j1, Math.max(k1, l1));
                    aint[0] += j1;
                    aint[1] += k1;
                    aint[2] += l1;
                    ++j;
                }
            }
        }

        if (itemarmor == null)
        {
            return null;
        }
        else
        {
            k = aint[0] / j;
            int i1 = aint[1] / j;
            l = aint[2] / j;
            f = (float)i / (float)j;
            f1 = (float)Math.max(k, Math.max(i1, l));
            k = (int)((float)k * f / f1);
            i1 = (int)((float)i1 * f / f1);
            l = (int)((float)l * f / f1);
            l1 = (k << 8) + i1;
            l1 = (l1 << 8) + l;
            Helpers.setColor(itemstack, l1);
            return itemstack;
        }
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize()
    {
        return 10;
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
}