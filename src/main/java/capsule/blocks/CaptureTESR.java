package capsule.blocks;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Spacial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

public class CaptureTESR extends BlockEntityRenderer<TileEntityCapture> {

    double time = 0;

    public CaptureTESR(BlockEntityRenderDispatcher renderDispatcherIn) {
        super(renderDispatcherIn);
        time = Math.random() * 10000;
    }


    @Override
    public boolean shouldRenderOffScreen(TileEntityCapture te) {
        return true;
    }

    @Override
    public void render(TileEntityCapture tileEntityCapture, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        time += partialTicks;
        int size = tileEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;
        int color = tileEntityCapture.getColor();
        BlockPos offset = Spacial.getAnchor(BlockPos.ZERO, tileEntityCapture.getBlockState(), size);
        AABB boundingBox = Spacial.getBB(offset.getX(), offset.getY(), offset.getZ(), size, extendSize);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.lines());
        CapsulePreviewHandler.renderRecallBox(matrixStackIn, color, boundingBox, ivertexbuilder, time);
    }
}
