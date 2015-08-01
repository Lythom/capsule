package capsule.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
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

		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GL11.glLineWidth(3.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.pushMatrix();
		GlStateManager.translate(relativeX, relativeY, relativeZ);
		GlStateManager.color(red,green,blue, 0.5F);
		

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		AxisAlignedBB boundingBox = AxisAlignedBB.fromBounds(-extendSize - 0.01, 1.01, -extendSize - 0.01,
				extendSize + 1.01, size + 1.01, extendSize + 1.01);

		worldrenderer.startDrawing(GL11.GL_LINE_LOOP);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		tessellator.draw();

		worldrenderer.startDrawing(GL11.GL_LINE_LOOP);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		tessellator.draw();

		worldrenderer.startDrawing(GL11.GL_LINE_LOOP);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		worldrenderer.addVertex(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		tessellator.draw();

		GlStateManager.popMatrix();
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GL11.glLineWidth(1.0F);
	}

}
