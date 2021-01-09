package capsule.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class RendererUtils {
    public static void doPositionPrologue(ActiveRenderInfo info) {
        RenderSystem.pushMatrix();
        RenderSystem.rotatef(info.getPitch(), 1.0F, 0.0F, 0.0F);
        RenderSystem.rotatef(info.getYaw() + 180, 0.0F, 1.0F, 0.0F);
        RenderSystem.translated(-info.getProjectedView().x, -info.getProjectedView().y, -info.getProjectedView().z);
    }

    public static void doPositionEpilogue() {
        RenderSystem.popMatrix();
    }

    public static void doOverlayPrologue() {
        RenderSystem.disableLighting();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
    }

    public static void doOverlayEpilogue() {
        RenderSystem.enableLighting();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void doWirePrologue() {
        RenderSystem.disableCull();
        RenderSystem.disableLighting();
        RenderSystem.disableTexture();
        RenderSystem.lineWidth(3.0F);
    }

    public static void doWireEpilogue() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableTexture();
        RenderSystem.enableLighting();
        RenderSystem.enableCull();

    }

    public static void drawCube(final BlockPos pos, final float sizeOffset, final BufferBuilder buffer) {
        drawCube(pos.getX() - sizeOffset, pos.getY() - sizeOffset, pos.getZ() - sizeOffset, pos.getX() + 1 + sizeOffset, pos.getY() + 1 + sizeOffset, pos.getZ() + 1 + sizeOffset, buffer);
    }

    public static void drawCube(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final BufferBuilder buffer) {
        drawPlaneNegX(minX, minY, maxY, minZ, maxZ, buffer);
        drawPlanePosX(maxX, minY, maxY, minZ, maxZ, buffer);
        drawPlaneNegY(minY, minX, maxX, minZ, maxZ, buffer);
        drawPlanePosY(maxY, minX, maxX, minZ, maxZ, buffer);
        drawPlaneNegZ(minZ, minX, maxX, minY, maxY, buffer);
        drawPlanePosZ(maxZ, minX, maxX, minY, maxY, buffer);
    }

    public static void drawPlaneNegX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).endVertex();
        buffer.pos(x, minY, maxZ).endVertex();
        buffer.pos(x, maxY, maxZ).endVertex();
        buffer.pos(x, maxY, minZ).endVertex();
    }

    public static void drawPlanePosX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(x, minY, minZ).endVertex();
        buffer.pos(x, maxY, minZ).endVertex();
        buffer.pos(x, maxY, maxZ).endVertex();
        buffer.pos(x, minY, maxZ).endVertex();
    }

    public static void drawPlaneNegY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).endVertex();
        buffer.pos(maxX, y, minZ).endVertex();
        buffer.pos(maxX, y, maxZ).endVertex();
        buffer.pos(minX, y, maxZ).endVertex();
    }

    public static void drawPlanePosY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final BufferBuilder buffer) {
        buffer.pos(minX, y, minZ).endVertex();
        buffer.pos(minX, y, maxZ).endVertex();
        buffer.pos(maxX, y, maxZ).endVertex();
        buffer.pos(maxX, y, minZ).endVertex();
    }

    public static void drawPlaneNegZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).endVertex();
        buffer.pos(minX, maxY, z).endVertex();
        buffer.pos(maxX, maxY, z).endVertex();
        buffer.pos(maxX, minY, z).endVertex();
    }

    public static void drawPlanePosZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final BufferBuilder buffer) {
        buffer.pos(minX, minY, z).endVertex();
        buffer.pos(maxX, minY, z).endVertex();
        buffer.pos(maxX, maxY, z).endVertex();
        buffer.pos(minX, maxY, z).endVertex();
    }

    public static void drawCapsuleCube(AxisAlignedBB boundingBox, BufferBuilder bufferBuilder) {
        drawPlaneNegX(boundingBox.minX, boundingBox.minY, boundingBox.maxY, boundingBox.minZ, boundingBox.maxZ, bufferBuilder);
        drawPlanePosX(boundingBox.maxX, boundingBox.minY, boundingBox.maxY, boundingBox.minZ, boundingBox.maxZ, bufferBuilder);
        drawPlaneNegY(boundingBox.minY, boundingBox.minX, boundingBox.maxX, boundingBox.minZ, boundingBox.maxZ, bufferBuilder);
        drawPlanePosY(boundingBox.maxY, boundingBox.minX, boundingBox.maxX, boundingBox.minZ, boundingBox.maxZ, bufferBuilder);
        drawPlaneNegZ(boundingBox.minZ, boundingBox.minX, boundingBox.maxX, boundingBox.minY, boundingBox.maxY, bufferBuilder);
        drawPlanePosZ(boundingBox.maxZ, boundingBox.minX, boundingBox.maxX, boundingBox.minY, boundingBox.maxY, bufferBuilder);
    }

    public static void setColor(final int rgb, final int alpha) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;

        final float af = alpha / 255f;
        final float rf = r / 255f;
        final float gf = g / 255f;
        final float bf = b / 255f;

        RenderSystem.color4f(rf, gf, bf, af);
    }

    /**
     * // from  https://discord.com/channels/313125603924639766/454376090362970122/668151688733130774
     * final IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getBufferBuilders().getEntityVertexConsumers();
     *         for (BlockInfo blockInfo : blockAccess.getBlueprint().getBlockInfoAsList())
     *         {
     *             matrixStack.push();
     *             matrixStack.translate(x-viewPosition.getX(), y-viewPosition.getY(), z-viewPosition.getZ());
     *             final Matrix4f model = matrixStack.peek().getModel();
     *             BlockState state = blockInfo.getState();
     *
     *             final BlockPos blockPos = blockInfo.getPos();
     *             matrixStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
     *
     *             blockrendererdispatcher.renderBlock(state,
     *               blockPos,
     *               blockAccess,
     *               matrixStack,
     *               buffer.getBuffer(RenderTypeLookup.getEntityBlockLayer(state)),
     *               true,
     *               random);
     *             matrixStack.translate(-blockPos.getX(), -blockPos.getY(), -blockPos.getZ());
     *             matrixStack.pop();
     *
     *             matrixStack.push();
     *             final IFluidState fluidState = state.getFluidState();
     *             if (!fluidState.isEmpty())
     *             {
     *                 FluidRenderer.render(model, blockAccess, blockPos, buffer.getBuffer(RenderTypeLookup.getFluidLayer(fluidState)), fluidState);
     *             }
     *             matrixStack.pop();
     *         }
     */
}
