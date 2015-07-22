package capsule;

import items.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CapsuleItems {
	
	public static Item capsule;
	
	public static void createItems(String modid) {
		GameRegistry.registerItem(capsule = new CapsuleItem("capsule"), "capsule");
		ModelBakery.addVariantName(capsule, modid+":capsule_empty", modid+":capsule_activated", modid+":capsule_linked");
    }
	
	public static void registerRenderers(String modid) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItems.capsule, 0, new ModelResourceLocation(modid+":capsule_empty", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItems.capsule, 1, new ModelResourceLocation(modid+":capsule_activated", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(CapsuleItems.capsule, 2, new ModelResourceLocation(modid+":capsule_linked", "inventory"));
	}
	
	public static void registerRecipes() {
		GameRegistry.addRecipe(new ItemStack(CapsuleItems.capsule), new Object[] {"   ", "#P#", " # ", '#', Items.iron_ingot, 'P', Items.ender_pearl});
	}
}
