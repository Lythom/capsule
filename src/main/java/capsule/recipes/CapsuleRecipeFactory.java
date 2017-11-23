package capsule.recipes;

import capsule.Main;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nonnull;

public class CapsuleRecipeFactory implements IRecipeFactory {

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);

        CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
        primer.width = recipe.getRecipeWidth();
        primer.height = recipe.getRecipeHeight();
        primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
        primer.input = recipe.getIngredients();

        return new CapsuleRecipe(new ResourceLocation(Main.MODID, "capsule"), recipe.getRecipeOutput(), primer);
    }

    public static class CapsuleRecipe extends ShapedOreRecipe {
        public CapsuleRecipe(ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
            super(group, result, primer);
        }

        @Override
        @Nonnull
        public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
            ItemStack newOutput = this.output.copy();

            ItemStack itemstack = ItemStack.EMPTY;

            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);

                if (!stack.isEmpty()) {
                    if (stack.getItem() instanceof CapsuleItem) {
                        itemstack = stack;
                    }
                }
            }

            if (itemstack.hasTagCompound()) {
                newOutput.setTagCompound(itemstack.getTagCompound().copy());
            }

            return newOutput;
        }
    }
}
