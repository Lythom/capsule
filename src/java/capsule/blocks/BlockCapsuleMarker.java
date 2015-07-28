/**
 * 
 */
package capsule.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 * @author Samuel
 *
 */
public class BlockCapsuleMarker extends Block {

	/**
	 * @param materialIn
	 */
	public BlockCapsuleMarker(String unlocalizedName, Material materialIn) {
		super(materialIn);
		this.setUnlocalizedName(unlocalizedName);
		this.setCreativeTab(CreativeTabs.tabMisc);
		this.setHardness(20);
		this.setResistance(1000);
		this.setHarvestLevel("pickaxe", 0);
	}

}
