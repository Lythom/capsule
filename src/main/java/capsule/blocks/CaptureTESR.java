package capsule.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class CaptureTESR extends TileEntitySpecialRenderer {

	public CaptureTESR() {
	}

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double relativeX, double relativeY, double relativeZ,
			float partialTicks, int blockDamageProgress) {

		if (!(tileEntity instanceof TileEntityCapture))
			return;

		TileEntityCapture tileEntityCapture = (TileEntityCapture) tileEntity;
		int size = tileEntityCapture.getSize();
		if (size == 0)
			return;
		int extendSize = (size - 1) / 2;

		int color = tileEntityCapture.getColor();
		CaptureTESR.drawCaptureZone(relativeX, relativeY, relativeZ, size, extendSize, color);

	}

	public static void drawCaptureZone(double relativeX, double relativeY, double relativeZ, int size, int extendSize,
			int color) {
		Color c = new Color(color);
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		int alpha = c.getAlpha();

		GL11.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();
		GlStateManager.pushMatrix();
		GlStateManager.translate(relativeX, relativeY, relativeZ);

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		AxisAlignedBB boundingBox = AxisAlignedBB.fromBounds(-extendSize - 0.01, 1.01, -extendSize - 0.01,
				extendSize + 1.01, size + 1.01, extendSize + 1.01);
		
		RenderGlobal.drawOutlinedBoundingBox(boundingBox, red, green, blue, 255);

		GlStateManager.popMatrix();
		GlStateManager.enableLighting();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GL11.glLineWidth(1.0F);
	}
	
	

}
