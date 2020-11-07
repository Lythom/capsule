package capsule.itemGroups;

import capsule.items.CapsuleItems;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import static capsule.items.CapsuleItem.STATE_LINKED;

public class CapsuleItemGroups extends ItemGroup {

    public CapsuleItemGroups(int index, String label) {
        super(index, label);
    }

    @Override
    public ItemStack createIcon() {
        ItemStack stack = new ItemStack(CapsuleItems.CAPSULE, 1);
        stack.setDamage(STATE_LINKED);
        return stack;
    }

}
