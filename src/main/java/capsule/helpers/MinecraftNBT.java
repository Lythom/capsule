package capsule.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class MinecraftNBT {
    /**
     * Return whether the specified armor has a color.
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean hasColor(ItemStack stack) {
        CompoundNBT compoundnbt = stack.getTagElement("display");
        return compoundnbt != null && compoundnbt.contains("color", 99);
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack) {
        CompoundNBT compoundnbt = stack.getTagElement("display");
        return compoundnbt != null && compoundnbt.contains("color", 99) ? compoundnbt.getInt("color") : 0xFFFFFF;
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
    }
}
