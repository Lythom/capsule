package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
	}
	
	public void init(FMLInitializationEvent event) {
		super.init(event);
		CapsuleItemsRegistrer.registerRenderers(Main.MODID);
		CapsuleBlocksRegistrer.registerRenderers(Main.MODID);
		MinecraftForge.EVENT_BUS.register(new CapsulePreviewHandler());
	}
	
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
	}
}
