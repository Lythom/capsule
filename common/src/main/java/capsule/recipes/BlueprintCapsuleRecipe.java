package capsule.recipes;

import capsule.helpers.NBTHelper;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.platform.Services;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

/**
 * Handle 2 cases :
 * crafting a blueprint from a source capsule, in this case the structureName to create template is taken from the source capsule
 * crafting a prefab, in this case the structureName to create template is taken from the recipe output
 */
public class BlueprintCapsuleRecipe extends ShapedRecipe {
    /**
     * The fields of vanilla's shaped recipe (group, category, show_notification, pattern, key, result).
     */
    public static final MapCodec<BlueprintCapsuleRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
            ).apply(instance, BlueprintCapsuleRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintCapsuleRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            BlueprintCapsuleRecipe::new);
    public static final RecipeSerializer<BlueprintCapsuleRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    final ShapedRecipePattern pattern;
    final ItemStackTemplate result;

    public BlueprintCapsuleRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    /**
     * The capsule this recipe shows, as written in its json.
     */
    public ItemStack getResultItem() {
        return result.create();
    }

    /**
     * The original capsule remains in the crafting grid
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, Services.PLATFORM.getCraftingRemainingItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem) {
                nonnulllist.set(i, ClearCapsuleRecipe.givenBack(itemstack.copy()));
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
    @Override
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
    @Override
    public ItemStack assemble(CraftingInput inv) {
        ItemStack referenceCapsule = getResultItem();
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
                    CapsuleItem.getBaseColor(getResultItem()),
                    0xFFFFFF,
                    CapsuleItem.getSize(referenceCapsule),
                    CapsuleItem.isOverpowered(referenceCapsule),
                    NBTHelper.hasTag(referenceCapsule) ? NBTHelper.getOrCreateTag(referenceCapsule).getStringOr("label", "") : null,
                    0
            );
            CapsuleItem.setBlueprint(blueprintItem);
            // hack to force a tempalte copy if it's not done after craft
            if (NBTHelper.hasTag(blueprintItem)) {
                NBTHelper.updateTag(blueprintItem, tag -> tag.putBoolean("templateShouldBeCopied", true));
            }
            CapsuleItem.setState(blueprintItem, DEPLOYED);
            return blueprintItem;
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    /**
     * ShapedRecipe declares its own serializer type; the recipe is written and read by SERIALIZER, registered as
     * capsule:blueprint_capsule.
     */
    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) CapsuleRecipes.BLUEPRINT_CAPSULE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
