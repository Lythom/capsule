package capsule.itemGroups;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static capsule.items.CapsuleItem.CapsuleState.LINKED;

public class CapsuleItemGroups extends CreativeModeTab {

    public CapsuleItemGroups(int index, String label) {
        super(index, label);
    }

    @Override
    public ItemStack makeIcon() {
        ItemStack stack = new ItemStack(CapsuleItems.CAPSULE, 1);
        CapsuleItem.setState(stack, LINKED);
        return stack;
    }

}
