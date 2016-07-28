package capsule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.items.CapsuleItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
			tryPreviewCapture(player, player.getHeldItemMainhand());
		}
	}

	private boolean tryPreviewCapture(EntityPlayerSP player, ItemStack heldItem) {
		// an item is in hand
		if (heldItem != null) {
			Item heldItemItem = heldItem.getItem();
			// it's an empty capsule : show capture zones
			if (heldItemItem instanceof CapsuleItem && (heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY || heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY_ACTIVATED)) {
				CapsuleItem capsule = (CapsuleItem) heldItem.getItem();
				if (heldItem.getTagCompound().hasKey("size")) {
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
			tryPreviewDeploy(mc.thePlayer, mc.thePlayer.getHeldItemMainhand(), mc.objectMouseOver);
		}
	}

	private void tryPreviewDeploy(EntityPlayerSP thePlayer, ItemStack heldItemMainhand, RayTraceResult objectMouseOver) {
		
		BlockPos anchorPos = objectMouseOver.getBlockPos();
		
		if (heldItemMainhand != null && anchorPos != null) {
			if (heldItemMainhand.getItem() instanceof CapsuleItem && heldItemMainhand.getItemDamage() == CapsuleItem.STATE_ACTIVATED) {
				
				// TODO : have this run serverside. Send messages to client with blocks positions
//				WorldServer playerWorldServer = null; // TODO : retrieve
//				TemplateManager templatemanager = StructureSaver.getTemplateManager(playerWorldServer);
//				Template template = templatemanager.func_189942_b(playerWorldServer.getMinecraftServer(), new ResourceLocation(heldItemMainhand.getTagCompound().getString("templateName")));
//				List<Template.BlockInfo> blocksInfos = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "blocks");
//				List<BlockPos> blockspos = new ArrayList<BlockPos>();
//				for (Template.BlockInfo blockInfo: blocksInfos) {
//					blockspos.add(blockInfo.pos);
//				}
//				
//				int extendSize = (getSize(heldItemMainhand) - 1) / 2;
//				CapsuleItem capsuleItem = (CapsuleItem)heldItemMainhand.getItem();
//				int color = capsuleItem.getColorFromItemstack(heldItemMainhand, 0);
//				
//				GlStateManager.pushMatrix();
//				
//				GL11.glLineWidth(1.0F);
//				GlStateManager.enableBlend();
//				GlStateManager.disableLighting();
//				GlStateManager.disableTexture2D();
//				GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
//		        GlStateManager.disableTexture2D();
//		        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
//				
//				GlStateManager.translate(anchorPos.getX() - extendSize, anchorPos.getY() + 1, anchorPos.getZ() - extendSize);
//				
//				for(BlockPos blockpos : blockspos){
//					drawDeployZone(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1, 1, 0x50d894);		
//				}
//				
//				GlStateManager.enableTexture2D();
//				GlStateManager.disableBlend();
//				GlStateManager.enableLighting();
//				
//				GL11.glLineWidth(1.0F);
//				
//				GL11.glPopAttrib();
//				GlStateManager.popMatrix();
				
			}
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
	
	private int getSize(ItemStack capsule){
		int size = 1;
		if (capsule.getTagCompound().hasKey("size")) {
			size = capsule.getTagCompound().getInteger("size");
		}
		return size;
	}
	
	
	private void previewRecall(ItemStack capsule) {

		NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("spawnPosition");
		
		int size = getSize(capsule);
		int extendSize = (size - 1) / 2;
		CapsuleItem capsuleItem = (CapsuleItem)capsule.getItem();
		int color = capsuleItem.getColorFromItemstack(capsule, 0);

		CaptureTESR.drawCaptureZone(
				linkPos.getInteger("x") + extendSize - TileEntityRendererDispatcher.staticPlayerX, 
				linkPos.getInteger("y") - 1 -TileEntityRendererDispatcher.staticPlayerY, 
				linkPos.getInteger("z") + extendSize - TileEntityRendererDispatcher.staticPlayerZ, size,
				extendSize, color);
	}

	private int lastSize = 0;
	private int lastColor = 0;
	private void setCaptureTESizeColor(int size, int color, World worldIn) {
		if(size == lastSize && color == lastColor) return;
		
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
		lastSize = size;
		lastColor = color;
	}
	
	public static void drawDeployZone(double relativeX, double relativeY, double relativeZ, int size, int extendSize,
			int color) {
		Color c = new Color(color);
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		int alpha = 150;

		AxisAlignedBB boundingBox = new AxisAlignedBB(-extendSize - 0.01, 1.01, -extendSize - 0.01,
				extendSize + 1.01, size + 1.01, extendSize + 1.01);
		
		Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();

	}
}
