package capsule.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
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
                                       int color) {
        doPositionPrologue();
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
    public void render(TileEntityCapture tileEntityCapture, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        int size = tileEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;

        int color = tileEntityCapture.getColor();
        CaptureTESR.drawCaptureZone(tileEntityCapture.getPos().getX(), tileEntityCapture.getPos().getY(), tileEntityCapture.getPos().getZ(), size, extendSize, color);

    }
}
