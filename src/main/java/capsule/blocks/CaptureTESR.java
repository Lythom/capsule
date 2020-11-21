package capsule.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;

import static capsule.client.RendererUtils.*;

public class CaptureTESR extends TileEntityRenderer<TileEntityCapture> {


    public CaptureTESR(TileEntityRendererDispatcher renderDispatcherIn) {
        super(renderDispatcherIn);
    }

    public static void drawCaptureZone(double relativeX, double relativeY, double relativeZ, int size, int extendSize,
                                       int color, ActiveRenderInfo view) {
        doPositionPrologue(view);
        doWirePrologue();

        AxisAlignedBB boundingBox = new AxisAlignedBB(
                -extendSize - 0.01 + relativeX,
                1.01 + relativeY,
                -extendSize - 0.01 + relativeZ,
                extendSize + 1.01 + relativeX,
                size + 1.01 + relativeY,
                extendSize + 1.01 + relativeZ);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        setColor(color, 50);
        bufferBuilder.begin(3, DefaultVertexFormats.POSITION);
        drawCapsuleCube(boundingBox, bufferBuilder);
        tessellator.draw();
        setColor(0xFFFFFF, 255);

        doWireEpilogue();
        doPositionEpilogue();
    }


    @Override
    public boolean isGlobalRenderer(TileEntityCapture te) {
        return true;
    }

    @Override
    public void render(TileEntityCapture tileEntityCapture, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        int size = tileEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;
        int color = tileEntityCapture.getColor();
        AxisAlignedBB boundingBox = new AxisAlignedBB(
                -extendSize - 0.01,
                1.01,
                -extendSize - 0.01,
                extendSize + 1.01,
                size + 1.01,
                extendSize + 1.01);

        final int r = (color >> 16) & 0xFF;
        final int g = (color >> 8) & 0xFF;
        final int b = color & 0xFF;

        final float af = 200 / 255f;
        final float rf = r / 255f;
        final float gf = g / 255f;
        final float bf = b / 255f;

        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getLines());
        WorldRenderer.drawBoundingBox(matrixStackIn, ivertexbuilder, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, rf, gf, bf, af, rf, gf, bf);
    }
}
