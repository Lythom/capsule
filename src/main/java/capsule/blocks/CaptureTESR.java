package capsule.blocks;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Spacial;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class CaptureTESR extends TileEntityRenderer<TileEntityCapture> {

    double time = 0;

    public CaptureTESR(TileEntityRendererDispatcher renderDispatcherIn) {
        super(renderDispatcherIn);
        time = Math.random() * 10000;
    }


    @Override
    public boolean shouldRenderOffScreen(TileEntityCapture te) {
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
        BlockPos offset = Spacial.getAnchor(BlockPos.ZERO, tileEntityCapture.getBlockState(), size);
        AxisAlignedBB boundingBox = Spacial.getBB(offset.getX(), offset.getY(), offset.getZ(), size, extendSize);
        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.lines());
        CapsulePreviewHandler.renderRecallBox(matrixStackIn, color, boundingBox, ivertexbuilder, time);
    }
}
