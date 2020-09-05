package capsule.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class MinecraftNBT {
    /**
     * Return whether the specified armor has a color.
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean hasColor(ItemStack stack) {
        if (!stack.hasTag()) return false;
        if (!stack.getTag().contains("display", 10)) return false;
        return stack.getTag().getCompound("display").contains("color", 3);
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack) {
        CompoundNBT nbttagcompound = stack.getTag();

        if (nbttagcompound != null) {
            CompoundNBT nbttagcompound1 = nbttagcompound.getCompound("display");
            if (nbttagcompound1.contains("color", 3)) {
                return nbttagcompound1.getInt("color");
            }
        }

        return 0xFFFFFF;
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color) {
        CompoundNBT nbttagcompound = stack.getTag();

        if (nbttagcompound == null) {
            nbttagcompound = new CompoundNBT();
            stack.setTag(nbttagcompound);
        }

        CompoundNBT nbttagcompound1 = nbttagcompound.getCompound("display");
        if (!nbttagcompound.contains("display", 10)) {
            nbttagcompound.setTag("display", nbttagcompound1);
        }

        nbttagcompound1.putInt("color", color);
    }
}
