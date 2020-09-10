package capsule.blocks;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;

import static capsule.client.RendererUtils.*;

public class CaptureTESR extends TileEntitySpecialRenderer<TileEntityCapture> {

    public CaptureTESR() {
    }

    public static void drawCaptureZone(double relativeX, double relativeY, double relativeZ, int size, int extendSize,
                                       int color) {
        doPositionPrologue();
        doWirePrologue();
        boolean prevStateEnabled = disableLightmap();

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

        resetLightmap(prevStateEnabled);
        doWireEpilogue();
        doPositionEpilogue();
    }

    @Override
    public void render(TileEntityCapture tileEntityCapture, double relativeX, double relativeY, double relativeZ,
                       float partialTicks, int blockDamageProgress, float alpha) {

        if (tileEntityCapture == null)
            return;

        int size = tileEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;

        int color = tileEntityCapture.getColor();
        CaptureTESR.drawCaptureZone(tileEntityCapture.getPos().getX(), tileEntityCapture.getPos().getY(), tileEntityCapture.getPos().getZ(), size, extendSize, color);

    }


}
