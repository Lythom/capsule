package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.platform.Services;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class ClearCapsuleRecipe extends CustomRecipe {
    /**
     * Capsules that a craft put back in the grid. Shift-click crafting goes on while the result is a capsule, so it would
     * then clear them; they can be cleared once moved by the player.
     */
    private static final Set<ItemStack> GIVEN_BACK = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static ItemStack givenBack(ItemStack capsule) {
        GIVEN_BACK.add(capsule);
        return capsule;
    }

    public static final ClearCapsuleRecipe INSTANCE = new ClearCapsuleRecipe();
    public static final MapCodec<ClearCapsuleRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearCapsuleRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ClearCapsuleRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeSerializer<ClearCapsuleRecipe> getSerializer() {
        return CapsuleRecipes.CLEAR_CAPSULE_SERIALIZER.get();
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInput inv, Level worldIn) {
        int sourceCapsule = 0;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);

            if (canBeEmptyCapsule(itemstack)) {
                sourceCapsule++;
            } else if (!itemstack.isEmpty()) {
                return false;
            }

        }

        return sourceCapsule == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);

            if (canBeEmptyCapsule(itemstack)) {
                ItemStack copy = itemstack.copy();
                CapsuleItem.clearCapsule(copy);
                return copy;

            }
        }
        return ItemStack.EMPTY;
    }

    public boolean canBeEmptyCapsule(ItemStack itemstack) {
        if (!(itemstack.getItem() instanceof CapsuleItem) || GIVEN_BACK.contains(itemstack)) return false;
        return CapsuleItem.isLinkedStateCapsule(itemstack) || (CapsuleItem.hasState(itemstack, DEPLOYED) && !CapsuleItem.isBlueprint(itemstack));
    }

    public ItemStack getResultItem() {
        return CapsuleItems.withState(EMPTY);
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, Services.PLATFORM.getCraftingRemainingItem(itemstack));
            if (itemstack.getItem() instanceof CapsuleItem && !CapsuleItem.hasState(itemstack, DEPLOYED)) {
                // Copy the capsule and give back a recovery capsule of the previous content
                ItemStack copy = itemstack.copy();
                CapsuleItem.setOneUse(copy);
                nonnulllist.set(i, copy);
            }
        }

        return nonnulllist;
    }
}