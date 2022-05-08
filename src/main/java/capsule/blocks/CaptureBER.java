package capsule.blocks;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Spacial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class CaptureBER implements BlockEntityRenderer<BlockEntityCapture> {

    double time = 0;

    public CaptureBER(BlockEntityRendererProvider.Context ctx) {
        time = Math.random() * 10000;
    }


    @Override
    public boolean shouldRenderOffScreen(BlockEntityCapture te) {
        return true;
    }

    @Override
    public void render(BlockEntityCapture BlockEntityCapture, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        time += partialTicks;
        int size = BlockEntityCapture.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;
        int color = BlockEntityCapture.getColor();
        BlockPos offset = Spacial.getAnchor(BlockPos.ZERO, BlockEntityCapture.getBlockState(), size);
        AABB boundingBox = Spacial.getBB(offset.getX(), offset.getY(), offset.getZ(), size, extendSize);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.lines());
        CapsulePreviewHandler.renderRecallBox(matrixStackIn, color, boundingBox, ivertexbuilder, time);
    }
}
