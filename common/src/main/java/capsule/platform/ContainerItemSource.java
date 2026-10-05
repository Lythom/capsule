package capsule.platform;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public record ContainerItemSource(Container container) implements ItemSource {

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    @Override
    public ItemStack extract(int slot, int count) {
        return container.removeItem(slot, count);
    }

    @Override
    public void insert(int slot, ItemStack stack) {
        ItemStack current = container.getItem(slot);
        if (current.isEmpty()) {
            container.setItem(slot, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(Math.min(stack.getCount(), container.getMaxStackSize(current) - current.getCount()));
        }
        container.setChanged();
    }
}
