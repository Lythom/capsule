package capsule.recipes;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;
import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

/**
 * Handle 2 cases :
 * crafting a blueprint from a source capsule, in this case the structureName to create template is taken from the source capsule
 * crafting a prefab, in this case the structureName to create template is taken from the recipe output
 */
public class BlueprintCapsuleRecipe implements ICraftingRecipe {
    public final ShapedRecipe recipe;

    public BlueprintCapsuleRecipe(ShapedRecipe recipe) {
        this.recipe = recipe;
    }

    public ItemStack getRecipeOutput() {
        return recipe.getRecipeOutput();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        return ingredients;
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

    private boolean IsCopyable(ItemStack itemstack) {
        return CapsuleItem.isLinkedStateCapsule(itemstack) || CapsuleItem.isBlueprint(itemstack) || CapsuleItem.isOneUse(itemstack);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory inv, World worldIn) {
        if (!recipe.matches(inv, worldIn)) {
            return false;
        }

        // in case it's not a prefab but a copy template recipe
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            if (itemstack.getItem() instanceof CapsuleItem && !IsCopyable(itemstack)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack getCraftingResult(CraftingInventory inv) {
        ItemStack referenceCapsule = recipe.getRecipeOutput();
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            if (IsCopyable(itemstack)) {
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
            CapsuleItem.setState(blueprintItem, DEPLOYED);
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
        return CapsuleRecipes.BLUEPRINT_CAPSULE_SERIALIZER;
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