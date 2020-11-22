package capsule.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.AxisAlignedBB;

import static capsule.client.RendererUtils.doPositionEpilogue;
import static capsule.client.RendererUtils.doPositionPrologue;

public class CaptureTESR extends TileEntityRenderer<TileEntityCapture> {

    double time = 0;
    static double incTime = 0;

    public CaptureTESR(TileEntityRendererDispatcher renderDispatcherIn) {
        super(renderDispatcherIn);
        time = Math.random() * 10000;
    }

    public static void drawCaptureZone(double relativeX, double relativeY, double relativeZ, int size, int extendSize,
                                       int color, ActiveRenderInfo view) {
        incTime += 1;
        doPositionPrologue(view);

        AxisAlignedBB boundingBox = getBB(relativeX, relativeY, relativeZ, size, extendSize);

        IRenderTypeBuffer.Impl impl = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        IVertexBuilder ivertexbuilder = impl.getBuffer(RenderType.getLines());

        final float af = 200 / 255f;
        final float rf = ((color >> 16) & 0xFF) / 255f;
        final float gf = ((color >> 8) & 0xFF) / 255f;
        final float bf = (color & 0xFF) / 255f;
        WorldRenderer.drawBoundingBox(ivertexbuilder, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, rf, gf, bf, af);
        for (int i = 0; i < 5; i++) {
            double i1 = getMovingEffectPos(boundingBox, i, incTime, 0.0015f);
            double i2 = getMovingEffectPos(boundingBox, i, incTime, 0.0017f);
            double lowI = Math.min(i1, i2);
            double highI = Math.max(i1, i2);
            WorldRenderer.drawBoundingBox(ivertexbuilder,
                    boundingBox.minX, lowI, boundingBox.minZ,
                    boundingBox.maxX, highI, boundingBox.maxZ,
                    rf, gf, bf, 0.01f + i * 0.05f);
        }

        impl.finish();

        doPositionEpilogue();
    }


    @Override
    public boolean isGlobalRenderer(TileEntityCapture te) {
        return true;
    }

    @Override
    public void render(TileEntityCapture tileEntityCapture, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        time += partialTicks;
        int size = tileEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;
        int color = tileEntityCapture.getColor();
        AxisAlignedBB boundingBox = getBB(0, 0, 0, size, extendSize);

        final float af = 200 / 255f;
        final float rf = ((color >> 16) & 0xFF) / 255f;
        final float gf = ((color >> 8) & 0xFF) / 255f;
        final float bf = (color & 0xFF) / 255f;

        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getLines());
        WorldRenderer.drawBoundingBox(matrixStackIn, ivertexbuilder, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, rf, gf, bf, af, rf, gf, bf);

        for (int i = 0; i < 5; i++) {
            double i1 = getMovingEffectPos(boundingBox, i, time, 0.0015f);
            double i2 = getMovingEffectPos(boundingBox, i, time, 0.0017f);
            double lowI = Math.min(i1, i2);
            double highI = Math.max(i1, i2);
            WorldRenderer.drawBoundingBox(matrixStackIn, ivertexbuilder,
                    boundingBox.minX, lowI, boundingBox.minZ,
                    boundingBox.maxX, highI, boundingBox.maxZ,
                    rf, gf, bf, 0.01f + i * 0.05f, rf, gf, bf);
        }
    }

    private static AxisAlignedBB getBB(double relativeX, double relativeY, double relativeZ, int size, int extendSize) {
        AxisAlignedBB boundingBox = new AxisAlignedBB(
                -extendSize - 0.01 + relativeX,
                1.01 + relativeY,
                -extendSize - 0.01 + relativeZ,
                extendSize + 1.01 + relativeX,
                size + 1.01 + relativeY,
                extendSize + 1.01 + relativeZ);
        return boundingBox;
    }

    private static double getMovingEffectPos(AxisAlignedBB boundingBox, int t, double incTime, float offset) {
        return boundingBox.minY + (Math.cos(t * incTime * offset) * 0.5 + 0.5) * (boundingBox.maxY - boundingBox.minY);
    }
}
