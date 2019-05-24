package capsule.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MinecraftNBT {
    /**
     * Return whether the specified armor has a color.
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean hasColor(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        if (!stack.getTagCompound().hasKey("display", 10)) return false;
        return stack.getTagCompound().getCompoundTag("display").hasKey("color", 3);
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");
            if (nbttagcompound1.hasKey("color", 3)) {
                return nbttagcompound1.getInteger("color");
            }
        }

        return 0xFFFFFF;
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound == null) {
            nbttagcompound = new NBTTagCompound();
            stack.setTagCompound(nbttagcompound);
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");
        if (!nbttagcompound.hasKey("display", 10)) {
            nbttagcompound.setTag("display", nbttagcompound1);
        }

        nbttagcompound1.setInteger("color", color);
    }
}
