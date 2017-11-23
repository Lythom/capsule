package capsule.tabs;

import capsule.items.CapsuleItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static capsule.items.CapsuleItem.STATE_LINKED;

public class CapsuleTabs extends CreativeTabs {

    public CapsuleTabs(int index, String label) {
        super(index, label);
    }

    @SideOnly(Side.CLIENT)
    public ItemStack getTabIconItem() {
        return new ItemStack(CapsuleItems.capsule, 1, STATE_LINKED);
    }

}
