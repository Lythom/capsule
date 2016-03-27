package capsule.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.math.AxisAlignedBB;

public class CaptureTESR extends TileEntitySpecialRenderer<TileEntityCapture> {

	public CaptureTESR() {
	}

	@Override
	public void renderTileEntityAt(TileEntityCapture tileEntityCapture, double relativeX, double relativeY, double relativeZ,
			float partialTicks, int blockDamageProgress) {

		if (tileEntityCapture == null)
			return;

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

		GL11.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();
		GlStateManager.pushMatrix();
		GlStateManager.translate(relativeX, relativeY, relativeZ);

		AxisAlignedBB boundingBox = new AxisAlignedBB(-extendSize - 0.01, 1.01, -extendSize - 0.01,
				extendSize + 1.01, size + 1.01, extendSize + 1.01);
		
		RenderGlobal.drawOutlinedBoundingBox(boundingBox, red, green, blue, 255);

		GlStateManager.popMatrix();
		GlStateManager.enableLighting();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GL11.glLineWidth(1.0F);
	}
	
	

}
