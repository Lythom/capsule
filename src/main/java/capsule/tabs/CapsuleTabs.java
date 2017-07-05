package capsule.tabs;

import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CapsuleTabs extends CreativeTabs {

    public CapsuleTabs(int index, String label) {
        super(index, label);
    }

    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {

        return CapsuleItemsRegistrer.capsule;
    }

}
