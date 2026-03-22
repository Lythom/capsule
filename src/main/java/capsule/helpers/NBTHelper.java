package capsule.helpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Helper class to bridge the old ItemStack NBT API (1.20.4) with the new DataComponents system (1.21+).
 * Uses CustomData component to store arbitrary NBT data on ItemStacks.
 */
public class NBTHelper {

    /**
     * Equivalent to the old stack.hasTag()
     */
    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    /**
     * Equivalent to the old stack.getTag() - returns a COPY of the tag.
     * Modifications to the returned tag will NOT be reflected on the stack.
     * Use updateTag() for modifications.
     */
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : null;
    }

    /**
     * Equivalent to the old stack.getOrCreateTag() - returns a copy.
     * Modifications to the returned tag will NOT be reflected on the stack.
     * Use updateTag() for modifications.
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /**
     * Equivalent to the old stack.setTag(tag)
     */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Update the CustomData tag in-place. The consumer receives a mutable CompoundTag.
     * Equivalent to: tag = stack.getOrCreateTag(); consumer.accept(tag); // auto-saved
     */
    public static void updateTag(ItemStack stack, Consumer<CompoundTag> modifier) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, modifier);
    }

    /**
     * Helper to add a sub-element to the tag.
     * Equivalent to the old stack.addTagElement(key, tag)
     */
    public static void addTagElement(ItemStack stack, String key, net.minecraft.nbt.Tag value) {
        updateTag(stack, tag -> tag.put(key, value));
    }
}
