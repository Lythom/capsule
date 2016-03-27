package capsule;

import java.util.Iterator;

import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.items.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class CapsulePreviewHandler {
	public CapsulePreviewHandler() {
	}
	
	/**
	 * set captureBlock data (clientside only ) when capsule is in hand.
	 * @param event
	 */
	@SubscribeEvent
	public void onLivingUpdateEvent(PlayerTickEvent event) {

		// do something to player every update tick:
		if (event.player instanceof EntityPlayerSP && event.phase.equals(Phase.START)) {
			EntityPlayerSP player = (EntityPlayerSP) event.player;
			
			boolean mainHandPreview = tryPreviewCapture(player, player.getHeldItemMainhand());
			if (!mainHandPreview) {
				tryPreviewCapture(player, player.getHeldItemOffhand());
			}
		}
	}

	private boolean tryPreviewCapture(EntityPlayerSP player, ItemStack heldItem) {
		// an item is in hand
		if (heldItem != null) {
			Item heldItemItem = heldItem.getItem();
			// it's an empty capsule : show capture zones
			if (heldItemItem instanceof CapsuleItem && heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY) {
				CapsuleItem capsule = (CapsuleItem) heldItem.getItem();
				if (heldItem.getTagCompound().hasKey("size") && heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY) {
					setCaptureTESizeColor(heldItem.getTagCompound().getInteger("size"), capsule.getColorFromItemstack(heldItem, 0), player.worldObj);
					return true;
				}
			
			} else {
				setCaptureTESizeColor(0, 0, player.worldObj);
			}
		} else {
			setCaptureTESizeColor(0, 0, player.worldObj);
		}
		
		return false;
	}
	
	/**
	 * Render recall preview when deployed capsule in hand
	 * @param event
	 */
	@SubscribeEvent
	public void onWorldRenderLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.thePlayer != null) {
			tryPreviewRecall(mc.thePlayer.getHeldItemMainhand());
			tryPreviewRecall(mc.thePlayer.getHeldItemOffhand());
		}
	}

	private void tryPreviewRecall(ItemStack heldItem) {
		// an item is in hand
		if (heldItem != null) {
			Item heldItemItem = heldItem.getItem();
			// it's an empty capsule : show capture zones
			if (heldItemItem instanceof CapsuleItem 
					&& heldItem.getItemDamage() == CapsuleItem.STATE_DEPLOYED 
					&& heldItem.getTagCompound().hasKey("spawnPosition")) {
				previewRecall(heldItem);
			}
		}
	}
	
	private void previewRecall(ItemStack capsule) {

		NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("spawnPosition");
		int size = 1;
		if (capsule.getTagCompound().hasKey("size")) {
			size = capsule.getTagCompound().getInteger("size");
		}
		int extendSize = (size - 1) / 2;
		IItemColor capsuleItem = (IItemColor)capsule.getItem();
		int color = capsuleItem.getColorFromItemstack(capsule, 0);

		CaptureTESR.drawCaptureZone(
				linkPos.getInteger("x") + extendSize - TileEntityRendererDispatcher.staticPlayerX, 
				linkPos.getInteger("y") - 1 -TileEntityRendererDispatcher.staticPlayerY, 
				linkPos.getInteger("z") + extendSize - TileEntityRendererDispatcher.staticPlayerZ, size,
				extendSize, color);
	}

	private void setCaptureTESizeColor(int size, int color, World worldIn) {
		// change NBT of all existing TileEntityCapture in the world to make them display the preview zone
		// remember it's client side only
		for (Iterator<TileEntityCapture> iterator = TileEntityCapture.instances.iterator(); iterator.hasNext();) {
			TileEntityCapture te = (TileEntityCapture) iterator.next();
			if (te.getWorld() == worldIn) {
				TileEntityCapture tec = (TileEntityCapture) te;
				tec.getTileData().setInteger("size", size);
				tec.getTileData().setInteger("color", color);
				worldIn.markBlockRangeForRenderUpdate(te.getPos(), te.getPos());
			}
		}
	}
}
