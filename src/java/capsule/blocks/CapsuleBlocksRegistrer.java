package capsule.blocks;

import capsule.Main;
import capsule.items.CapsuleItem;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CapsuleBlocksRegistrer {
	
	public static Block blockCapsuleMarker;
	
	public static void createBlocks(String modid) {
		GameRegistry.registerBlock(blockCapsuleMarker = new BlockCapsuleMarker("capsulemarker", Material.ground), "capsulemarker");
		blockCapsuleMarker.setCreativeTab(Main.tabCapsule);
		GameRegistry.registerTileEntity(TileEntityCapture.class, "capsulemarker_te");
    }
	
	public static void registerRenderers(String modid) {
		ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
		mesher.register(Item.getItemFromBlock(blockCapsuleMarker), 0, new ModelResourceLocation(modid+":capsulemarker", "inventory"));
		
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCapture.class, new CaptureTESR());
	}
	
	public static void registerRecipes() {
		ItemStack capsulemarker = new ItemStack(CapsuleBlocksRegistrer.blockCapsuleMarker, 1, CapsuleItem.STATE_EMPTY);
		
		// base recipes
		GameRegistry.addRecipe(capsulemarker, new Object[] {"# #", "#T#", "###", '#', Blocks.cobblestone, 'T', Blocks.torch});
		
	}
}
