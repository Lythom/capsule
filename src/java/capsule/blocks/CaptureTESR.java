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
	
	int iAlpha = 0;
	float getAlpha(){
		return 0.5F;
		//return (float) (0.25 + Math.abs(Math.cos(iAlpha*Math.PI*2/360))*0.5);
	}
	

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double relativeX, double relativeY, double relativeZ, float partialTicks, int blockDamageProgress) {

		if(!(tileEntity instanceof TileEntityCapture)) return;
		
		TileEntityCapture tileEntityCapture = (TileEntityCapture)tileEntity;
		int size = tileEntityCapture.getSize();
		if(size == 0) return;
		int extendSize = (size-1)/2;
		
		int color = tileEntityCapture.getColor();
		Color c = new Color(color);
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		
		iAlpha = (iAlpha + 2)%360;
		
		GL11.glPushMatrix();
	    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
	    
	    GlStateManager.translate(relativeX, relativeY, relativeZ);
		
	    GlStateManager.disableAlpha();
		GlStateManager.enableBlend();

        GlStateManager.color(red, green, blue, getAlpha());
        GL11.glLineWidth(5.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        AxisAlignedBB boundingBox = AxisAlignedBB.fromBounds(-extendSize-0.01, 1.01, -extendSize-0.01, extendSize+1.01, size+1.01, extendSize+1.01);
  
        
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
        
        GL11.glPopAttrib();
        GL11.glPopMatrix();
		
	}

}
