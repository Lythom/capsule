package capsule.recipes;

import capsule.CommonProxy;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import static capsule.items.CapsuleItem.STATE_DEPLOYED;

public class BlueprintCapsuleRecipe implements ICraftingRecipe {
    public final ShapedRecipe recipe;

    public BlueprintCapsuleRecipe(ShapedRecipe recipe) {
        this.recipe = recipe;
    }

    public ItemStack getRecipeOutput() {
        return recipe.getRecipeOutput();
    }

    /**
     * The original capsule remains in the crafting grid
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem) {
                nonnulllist.set(i, itemstack.copy());
            }
        }

        return nonnulllist;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory inv, World worldIn) {
        return recipe.matches(inv, worldIn);
    }

    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack getCraftingResult(CraftingInventory invC) {
        ItemStack referenceCapsule = recipe.getRecipeOutput();
        for (int i = 0; i < invC.getSizeInventory(); ++i) {
            ItemStack itemstack = invC.getStackInSlot(i);
            if (CapsuleItem.isLinkedStateCapsule(itemstack) || CapsuleItem.isReward(itemstack)) {
                // This blueprint will take the source structure name by copying it here
                // a new dedicated template is created later.
                // @see CapsuleItem.onCreated
                referenceCapsule = itemstack;
            }

        }
        try {
            ItemStack blueprintItem = Capsule.newLinkedCapsuleItemStack(
                    CapsuleItem.getStructureName(referenceCapsule),
                    CapsuleItem.getBaseColor(recipe.getRecipeOutput()),
                    0xFFFFFF,
                    CapsuleItem.getSize(referenceCapsule),
                    CapsuleItem.isOverpowered(referenceCapsule),
                    referenceCapsule.getTag() != null ? referenceCapsule.getTag().getString("label") : null,
                    0
            );
            CapsuleItem.setBlueprint(blueprintItem);
            // hack to force a tempalte copy if it's not done after craft
            if (blueprintItem.getTag() != null) {
                blueprintItem.getTag().putBoolean("templateShouldBeCopied", true);
            }
            CapsuleItem.setState(blueprintItem, STATE_DEPLOYED);
            return blueprintItem;
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canFit(int width, int height) {
        return recipe.canFit(width, height);
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return CommonProxy.BLUEPRINT_CAPSULE_SERIALIZER;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlueprintCapsuleRecipe> {

        @Override
        public BlueprintCapsuleRecipe read(ResourceLocation recipeId, JsonObject json) {
            return new BlueprintCapsuleRecipe(ShapedRecipe.Serializer.CRAFTING_SHAPED.read(recipeId, json));
        }

        @Override
        public BlueprintCapsuleRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new BlueprintCapsuleRecipe(ShapedRecipe.Serializer.CRAFTING_SHAPED.read(recipeId, buffer));
        }

        @Override
        public void write(PacketBuffer buffer, BlueprintCapsuleRecipe recipe) {
            ShapedRecipe.Serializer.CRAFTING_SHAPED.write(buffer, recipe.recipe);
        }
    }
}