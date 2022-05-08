package capsule.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class RendererUtils {
    public static void doPositionPrologue(Camera camera, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180.0F));
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
    }

    public static void doPositionEpilogue(PoseStack poseStack) {
        poseStack.popPose();
    }

    public static void doOverlayPrologue() {
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
    }

    public static void doOverlayEpilogue() {
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void doWirePrologue() {
        RenderSystem.disableCull();
        RenderSystem.disableTexture();
        RenderSystem.lineWidth(3.0F);
    }

    public static void doWireEpilogue() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableTexture();
        RenderSystem.enableCull();

    }

    public static void drawCube(final BlockPos pos, final float sizeOffset, final VertexConsumer buffer) {
        drawCube(pos.getX() - sizeOffset, pos.getY() - sizeOffset, pos.getZ() - sizeOffset, pos.getX() + 1 + sizeOffset, pos.getY() + 1 + sizeOffset, pos.getZ() + 1 + sizeOffset, buffer);
    }

    public static void drawCube(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final VertexConsumer buffer) {
        drawPlaneNegX(minX, minY, maxY, minZ, maxZ, buffer);
        drawPlanePosX(maxX, minY, maxY, minZ, maxZ, buffer);
        drawPlaneNegY(minY, minX, maxX, minZ, maxZ, buffer);
        drawPlanePosY(maxY, minX, maxX, minZ, maxZ, buffer);
        drawPlaneNegZ(minZ, minX, maxX, minY, maxY, buffer);
        drawPlanePosZ(maxZ, minX, maxX, minY, maxY, buffer);
    }

    public static void drawPlaneNegX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final VertexConsumer buffer) {
        buffer.vertex(x, minY, minZ).endVertex();
        buffer.vertex(x, minY, maxZ).endVertex();
        buffer.vertex(x, maxY, maxZ).endVertex();
        buffer.vertex(x, maxY, minZ).endVertex();
    }

    public static void drawPlanePosX(final double x, final double minY, final double maxY, final double minZ, final double maxZ, final VertexConsumer buffer) {
        buffer.vertex(x, minY, minZ).endVertex();
        buffer.vertex(x, maxY, minZ).endVertex();
        buffer.vertex(x, maxY, maxZ).endVertex();
        buffer.vertex(x, minY, maxZ).endVertex();
    }

    public static void drawPlaneNegY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final VertexConsumer buffer) {
        buffer.vertex(minX, y, minZ).endVertex();
        buffer.vertex(maxX, y, minZ).endVertex();
        buffer.vertex(maxX, y, maxZ).endVertex();
        buffer.vertex(minX, y, maxZ).endVertex();
    }

    public static void drawPlanePosY(final double y, final double minX, final double maxX, final double minZ, final double maxZ, final VertexConsumer buffer) {
        buffer.vertex(minX, y, minZ).endVertex();
        buffer.vertex(minX, y, maxZ).endVertex();
        buffer.vertex(maxX, y, maxZ).endVertex();
        buffer.vertex(maxX, y, minZ).endVertex();
    }

    public static void drawPlaneNegZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final VertexConsumer buffer) {
        buffer.vertex(minX, minY, z).endVertex();
        buffer.vertex(minX, maxY, z).endVertex();
        buffer.vertex(maxX, maxY, z).endVertex();
        buffer.vertex(maxX, minY, z).endVertex();
    }

    public static void drawPlanePosZ(final double z, final double minX, final double maxX, final double minY, final double maxY, final VertexConsumer buffer) {
        buffer.vertex(minX, minY, z).endVertex();
        buffer.vertex(maxX, minY, z).endVertex();
        buffer.vertex(maxX, maxY, z).endVertex();
        buffer.vertex(minX, maxY, z).endVertex();
    }

    public static void drawCapsuleCube(AABB boundingBox, BufferBuilder bufferBuilder) {
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

        RenderSystem.setShaderColor(rf, gf, bf, af);
    }
}
