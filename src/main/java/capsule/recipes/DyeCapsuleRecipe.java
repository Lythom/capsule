package capsule.recipes;

import capsule.helpers.MinecraftNBT;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import com.google.common.collect.Lists;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.List;

import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class DyeCapsuleRecipe extends CustomRecipe {

    public DyeCapsuleRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.DYE_CAPSULE_SERIALIZER.get();
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput inv, Level worldIn) {
        ItemStack itemstack = ItemStack.EMPTY;
        ArrayList<ItemStack> arraylist = Lists.newArrayList();

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemstack1 = inv.getItem(i);

            if (!itemstack1.isEmpty()) {
                if (itemstack1.getItem() instanceof CapsuleItem) {
                    itemstack = itemstack1;
                } else {
                    if (!(itemstack1.getItem() instanceof DyeItem)) {
                        return false;
                    }

                    arraylist.add(itemstack1);
                }
            }
        }

        return !itemstack.isEmpty() && !arraylist.isEmpty();
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        List<DyeItem> dyes = Lists.newArrayList();
        ItemStack itemstack = ItemStack.EMPTY;

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemstack1 = inv.getItem(i);
            if (!itemstack1.isEmpty()) {
                Item item = itemstack1.getItem();
                if (item instanceof CapsuleItem) {
                    if (!itemstack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemstack = itemstack1.copy();
                } else {
                    if (!(item instanceof DyeItem)) {
                        return ItemStack.EMPTY;
                    }

                    dyes.add((DyeItem) item);
                }
            }
        }

        return !itemstack.isEmpty() && !dyes.isEmpty() ? dyeItem(itemstack, dyes) : ItemStack.EMPTY;
    }

    static ItemStack dyeItem(ItemStack stack, List<DyeItem> dyes) {
        ItemStack itemstack = ItemStack.EMPTY;
        int[] aint = new int[3];
        int i = 0;
        int j = 0;
        CapsuleItem idyeablearmoritem = null;
        Item item = stack.getItem();
        if (item instanceof CapsuleItem) {
            idyeablearmoritem = (CapsuleItem) item;
            itemstack = stack.copy();
            itemstack.setCount(1);
            if (MinecraftNBT.hasColor(stack)) {
                int k = MinecraftNBT.getColor(itemstack);
                float f = (float) (k >> 16 & 255) / 255.0F;
                float f1 = (float) (k >> 8 & 255) / 255.0F;
                float f2 = (float) (k & 255) / 255.0F;
                i = (int) ((float) i + Math.max(f, Math.max(f1, f2)) * 255.0F);
                aint[0] = (int) ((float) aint[0] + f * 255.0F);
                aint[1] = (int) ((float) aint[1] + f1 * 255.0F);
                aint[2] = (int) ((float) aint[2] + f2 * 255.0F);
                ++j;
            }

            for (DyeItem dyeitem : dyes) {
                int texColor = dyeitem.getDyeColor().getTextureDiffuseColor();
                int i2 = (texColor >> 16) & 0xFF;
                int l = (texColor >> 8) & 0xFF;
                int i1 = texColor & 0xFF;
                i += Math.max(i2, Math.max(l, i1));
                aint[0] += i2;
                aint[1] += l;
                aint[2] += i1;
                ++j;
            }
        }

        if (idyeablearmoritem == null) {
            return ItemStack.EMPTY;
        } else {
            int j1 = aint[0] / j;
            int k1 = aint[1] / j;
            int l1 = aint[2] / j;
            float f3 = (float) i / (float) j;
            float f4 = (float) Math.max(j1, Math.max(k1, l1));
            j1 = (int) ((float) j1 * f3 / f4);
            k1 = (int) ((float) k1 * f3 / f4);
            l1 = (int) ((float) l1 * f3 / f4);
            int j2 = (j1 << 8) + k1;
            j2 = (j2 << 8) + l1;
            MinecraftNBT.setColor(itemstack, j2);
            return itemstack;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return CapsuleItems.withState(EMPTY);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, CommonHooks.getCraftingRemainingItem(itemstack));
        }

        return nonnulllist;
    }
}