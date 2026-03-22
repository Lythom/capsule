package capsule.helpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class MinecraftNBT {
    /**
     * Return whether the specified armor has a color.
     */
    public static boolean hasColor(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR);
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack) {
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        return dyedColor != null ? dyedColor.rgb() : 0xFFFFFF;
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
    }
}
