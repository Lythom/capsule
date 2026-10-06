package capsule.client;

import capsule.client.render.CapsuleTemplateRenderer;
import capsule.client.render.FakeWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The captured blocks shrinking into the capsule. The template is not on the client: the animation shows the blocks
 * the client saw when notified, as soon as the server removes them, or a shrinking box when it saw none.
 */
public class CaptureAnimation {
    private static final int DURATION = 16;
    private static final int MAX_BLOCKS = 4096;
    private static final List<CaptureAnimation> playing = new ArrayList<>();
    /**
     * Frames drawn by capture animations, read by the client smoke test.
     */
    public static int renderedFrames = 0;

    private final ClientLevel level;
    private final BlockPos origin;
    private final Vec3 target;
    private final AABB box;
    private final Map<BlockPos, BlockState> standing = new HashMap<>();
    private final CapsuleTemplateRenderer removed = new CapsuleTemplateRenderer();
    private int age = 0;

    /**
     * @param center center of the captured cube; the cube above a capture base starts one row higher, so the row above
     *               the cube is watched too (only the blocks that disappear are animated)
     * @param target where the blocks go: the capsule or the player
     */
    public static void start(ClientLevel level, BlockPos center, BlockPos target, int size) {
        if (ClientConfig.captureAnimation && level != null) playing.add(new CaptureAnimation(level, center, target, size));
    }

    private CaptureAnimation(ClientLevel level, BlockPos center, BlockPos target, int size) {
        this.level = level;
        this.origin = center.offset(-size / 2, -size / 2, -size / 2);
        this.target = Vec3.atCenterOf(target.subtract(origin));
        this.box = new AABB(0, 0, 0, size, size, size);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size, size - 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) standing.put(pos.immutable(), state);
        }
        if (standing.size() > MAX_BLOCKS) standing.clear();
        removed.templateWorld = new FakeWorld(level);
    }

    public static void tick() {
        playing.removeIf(animation -> !animation.tickAndContinue());
    }

    private boolean tickAndContinue() {
        Iterator<Map.Entry<BlockPos, BlockState>> it = standing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, BlockState> block = it.next();
            if (level.getBlockState(block.getKey()) != block.getValue()) {
                removed.templateWorld.setBlock(block.getKey().subtract(origin), block.getValue(), 0);
                it.remove();
            }
        }
        AABB current = shrunk(age / (float) DURATION).move(origin);
        Vec3 to = target.add(Vec3.atLowerCornerOf(origin));
        for (int i = 0; i < 3; i++) {
            double x = current.minX + level.random.nextDouble() * current.getXsize();
            double y = current.minY + level.random.nextDouble() * current.getYsize();
            double z = current.minZ + level.random.nextDouble() * current.getZsize();
            level.addParticle(ParticleTypes.END_ROD, x, y, z, (to.x - x) * 0.15, (to.y - y) * 0.15, (to.z - z) * 0.15);
        }
        return ++age < DURATION && level == Minecraft.getInstance().level;
    }

    public static void render(PoseStack poseStack, Vec3 camera, float partialTick) {
        for (CaptureAnimation animation : playing) animation.draw(poseStack, camera, Math.min(1, (animation.age + partialTick) / DURATION));
    }

    private void draw(PoseStack poseStack, Vec3 camera, float progress) {
        poseStack.pushPose();
        poseStack.translate(origin.getX() - camera.x, origin.getY() - camera.y, origin.getZ() - camera.z);
        if (!removed.templateWorld.entrySet().isEmpty()) {
            float scale = scale(progress);
            poseStack.translate(target.x, target.y, target.z);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-target.x, -target.y, -target.z);
            removed.renderTemplate(poseStack, 1 - progress * 0.5f);
            renderedFrames++;
        } else if (standing.isEmpty()) {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), shrunk(progress), 0.87f, 0.87f, 0.87f, 1);
            buffers.endBatch(RenderType.lines());
            renderedFrames++;
        }
        poseStack.popPose();
    }

    /**
     * Accelerates toward the target, like sucked in.
     */
    private static float scale(float progress) {
        return 1 - progress * progress;
    }

    private AABB shrunk(float progress) {
        float scale = scale(progress);
        return new AABB(target.add(box.getMinPosition().subtract(target).scale(scale)), target.add(box.getMaxPosition().subtract(target).scale(scale)));
    }
}
