package capsule.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Slots of an inventory a blueprint takes its materials from.
 */
public interface ItemSource {

    int size();

    ItemStack getItem(int slot);

    ItemStack extract(int slot, int count);

    /**
     * Puts the stack back in the slot, what does not fit is lost.
     */
    void insert(int slot, ItemStack stack);
}
