package capsule.tabs;

import capsule.items.CapsuleItemsRegistrer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class CapsuleTabs extends CreativeTabs {

	 public CapsuleTabs(int index, String label) {
		super(index, label);
	}

    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {

        return CapsuleItemsRegistrer.capsule;
    }

}
