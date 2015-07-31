package capsule;

import java.util.Iterator;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.blocks.TileEntityCapture;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

@Mod(modid = Main.MODID, name = Main.MODNAME, version = Main.VERSION)
public class Main {
	
	public static final String MODID = "capsule";
    public static final String MODNAME = "Capsule";
    public static final String VERSION = "1.0.0";

    @Instance
    public static Main instance = new Main();
        
     
    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
    	CapsuleItemsRegistrer.createItems(Main.MODID);
    	CapsuleBlocksRegistrer.createBlocks(Main.MODID);
        Config.config = new Configuration(e.getSuggestedConfigurationFile());
        Config.config.load();
    }
        
    @EventHandler
    public void init(FMLInitializationEvent e) {
    	CapsuleItemsRegistrer.registerRenderers(Main.MODID);
    	CapsuleBlocksRegistrer.registerRenderers(Main.MODID);
    	FMLCommonHandler.instance().bus().register(this);
    }
        
    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
    	CapsuleItemsRegistrer.registerRecipes();
    	CapsuleBlocksRegistrer.registerRecipes();
    }
    
	@EventHandler
	private void serverAboutToStart( FMLServerAboutToStartEvent evt )
	{
		CapsuleDimensionRegistrer.registerDimension();
	}
	
	@SubscribeEvent
	public void onLivingUpdateEvent(PlayerTickEvent event)
	{

		// do something to player every update tick:
		if (event.player instanceof EntityPlayerSP && event.phase.equals(event.phase.START))
		{
			EntityPlayerSP player = (EntityPlayerSP) event.player;
			ItemStack heldItem = player.getHeldItem();
			if(heldItem != null){
				Item heldItemItem = heldItem.getItem();
	
				if(heldItemItem instanceof CapsuleItem && heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY){
					CapsuleItem capsule = (CapsuleItem)heldItem.getItem();
					if(heldItem.getTagCompound().hasKey("size") && heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY){
						setCaptureTESizeColor(heldItem.getTagCompound().getInteger("size"), capsule.getColorFromItemStack(heldItem, 0), player.worldObj);
					}
				} else {
					setCaptureTESizeColor(0, 0, player.worldObj);
				}
			}
		}
	}
	
	private void setCaptureTESizeColor(int size, int color, World worldIn) {
		for (Iterator<TileEntityCapture> iterator = TileEntityCapture.instances.iterator(); iterator.hasNext();) {
			TileEntityCapture te = (TileEntityCapture) iterator.next();
			if(te.getWorld() == worldIn){
				TileEntityCapture tec = (TileEntityCapture) te;
				tec.getTileData().setInteger("size", size);
				tec.getTileData().setInteger("color", color);
			}
		}
	}
    
}
