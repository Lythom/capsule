package capsule.blocks;

import capsule.Main;
import capsule.items.CapsuleItem;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class CapsuleBlocksRegistrer {
	
	public static Block blockCapsuleMarker;
	
	public static void createBlocks(String modid) {
		GameRegistry.registerBlock(blockCapsuleMarker = new BlockCapsuleMarker("capsulemarker", Material.ground), "capsulemarker");
		blockCapsuleMarker.setCreativeTab(Main.tabCapsule);
		GameRegistry.registerTileEntity(TileEntityCapture.class, "capsulemarker_te");
    }
	
	public static void registerRenderers(String modid) {

	}
	
	public static void registerRecipes() {
		ItemStack capsulemarker = new ItemStack(CapsuleBlocksRegistrer.blockCapsuleMarker, 1, CapsuleItem.STATE_EMPTY);
		
		// base recipes
		GameRegistry.addRecipe(capsulemarker, new Object[] {"# #", "#T#", "###", '#', Blocks.cobblestone, 'T', Blocks.torch});
		
	}
}
