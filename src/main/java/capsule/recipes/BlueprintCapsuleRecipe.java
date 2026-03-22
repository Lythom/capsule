package capsule.recipes;

import capsule.helpers.NBTHelper;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

/**
 * Handle 2 cases :
 * crafting a blueprint from a source capsule, in this case the structureName to create template is taken from the source capsule
 * crafting a prefab, in this case the structureName to create template is taken from the recipe output
 */
public class BlueprintCapsuleRecipe extends ShapedRecipe {
    final ShapedRecipePattern pattern;
    final ItemStack result;
    final String group;
    final CraftingBookCategory category;
    final boolean showNotification;

    public BlueprintCapsuleRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result);
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return super.getResultItem(registryAccess);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return super.getIngredients();
    }

    /**
     * The original capsule remains in the crafting grid
     */
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, CommonHooks.getCraftingRemainingItem(itemstack));
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
    public boolean matches(CraftingInput inv, Level worldIn) {
        if (!super.matches(inv, worldIn)) {
            return false;
        }

        // in case it's not a prefab but a copy template recipe
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack.getItem() instanceof CapsuleItem && !IsCopyable(itemstack)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns a copy built from the original capsule.
     */
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        ItemStack referenceCapsule = super.getResultItem(registryAccess);
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
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
                    CapsuleItem.getBaseColor(super.getResultItem(registryAccess)),
                    0xFFFFFF,
                    CapsuleItem.getSize(referenceCapsule),
                    CapsuleItem.isOverpowered(referenceCapsule),
                    referenceCapsule.getTag() != null ? NBTHelper.getOrCreateTag(referenceCapsule).getString("label") : null,
                    0
            );
            CapsuleItem.setBlueprint(blueprintItem);
            // hack to force a tempalte copy if it's not done after craft
            if (blueprintItem.getTag() != null) {
                NBTHelper.updateTag(blueprintItem, tag -> tag.putBoolean("templateShouldBeCopied", true);
            }
            CapsuleItem.setState(blueprintItem, DEPLOYED);
            return blueprintItem;
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return super.canCraftInDimensions(width, height);
    }

//    @Override
//    public ResourceLocation getId() {
//        return recipe.getId();
//    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.BLUEPRINT_CAPSULE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<BlueprintCapsuleRecipe> {
        public static final Codec<BlueprintCapsuleRecipe> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(recipe -> recipe.group),
                                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(recipe -> recipe.category),
                                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                                ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                                ExtraCodecs.strictOptionalField(Codec.BOOL, "show_notification", true).forGetter(recipe -> recipe.showNotification)
                        )
                        .apply(instance, BlueprintCapsuleRecipe::new)
        );

        @Override
        public Codec<BlueprintCapsuleRecipe> codec() {
            return CODEC;
        }

        @Override
        public BlueprintCapsuleRecipe fromNetwork(FriendlyByteBuf pBuffer) {
            String s = pBuffer.readUtf();
            CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern shapedrecipepattern = ShapedRecipePattern.fromNetwork(pBuffer);
            ItemStack itemstack = pBuffer.readItem();
            boolean flag = pBuffer.readBoolean();
            return new BlueprintCapsuleRecipe(s, craftingbookcategory, shapedrecipepattern, itemstack, flag);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, BlueprintCapsuleRecipe pRecipe) {
            pBuffer.writeUtf(pRecipe.group);
            pBuffer.writeEnum(pRecipe.category);
            pRecipe.pattern.toNetwork(pBuffer);
            pBuffer.writeItem(pRecipe.result);
            pBuffer.writeBoolean(pRecipe.showNotification);
        }
    }
}