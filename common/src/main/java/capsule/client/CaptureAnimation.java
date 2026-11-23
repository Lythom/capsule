package capsule.client;

import capsule.client.render.CapsuleTemplateRenderer;
import capsule.client.render.FakeWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockGetter;
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
 * the client saw when notified, as soon as the server removes them, or a shrinking box when it saw none or too many.
 */
public class CaptureAnimation {
    private static final int DURATION = 16;
    static final int MAX_BLOCKS = 4096;
    /**
     * Larger cubes shrink as a box: reading all their positions would stall the client.
     */
    private static final int MAX_SIZE = 64;
    private static final List<CaptureAnimation> playing = new ArrayList<>();
    /**
     * The shrinking box (1.21.1: 0.87, 0.87, 0.87, 1).
     */
    private static final int BOX_COLOR = ARGB.colorFromFloat(1, 0.87f, 0.87f, 0.87f);
    /**
     * Frames drawn by capture animations, read by the client smoke test.
     */
    public static int renderedFrames = 0;

    private final ClientLevel level;
    private final BlockPos origin;
    private final Vec3 target;
    private final AABB box;
    private final Map<BlockPos, BlockState> standing;
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
        this.standing = standing(level, origin, size);
        removed.templateWorld = new FakeWorld(level);
    }

    /**
     * The blocks of the cube from origin and of the row above it, or none when there are more than MAX_BLOCKS.
     */
    static Map<BlockPos, BlockState> standing(BlockGetter level, BlockPos origin, int size) {
        Map<BlockPos, BlockState> standing = new HashMap<>();
        if (size > MAX_SIZE) return standing;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size, size - 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (standing.size() == MAX_BLOCKS) return new HashMap<>();
            standing.put(pos.immutable(), state);
        }
        return standing;
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
            double x = current.minX + level.getRandom().nextDouble() * current.getXsize();
            double y = current.minY + level.getRandom().nextDouble() * current.getYsize();
            double z = current.minZ + level.getRandom().nextDouble() * current.getZsize();
            level.addParticle(ParticleTypes.END_ROD, x, y, z, (to.x - x) * 0.15, (to.y - y) * 0.15, (to.z - z) * 0.15);
        }
        return ++age < DURATION && level == Minecraft.getInstance().level;
    }

    /**
     * Adds this frame's capture animations to the frame of the previews.
     */
    static void extract(List<CapsulePreviewHandler.PreviewSubmit> frame, Vec3 camera, float partialTick, float lineWidth) {
        for (CaptureAnimation animation : playing) {
            CapsulePreviewHandler.PreviewSubmit submit = animation.extract(camera, Math.min(1, (animation.age + partialTick) / DURATION), lineWidth);
            if (submit != null) frame.add(submit);
        }
    }

    private CapsulePreviewHandler.PreviewSubmit extract(Vec3 camera, float progress, float lineWidth) {
        Vec3 offset = Vec3.atLowerCornerOf(origin).subtract(camera);
        if (!removed.templateWorld.entrySet().isEmpty()) {
            renderedFrames++;
            float scale = scale(progress);
            return (collector, poseStack) -> {
                poseStack.translate(offset);
                poseStack.translate(target.x, target.y, target.z);
                poseStack.scale(scale, scale, scale);
                poseStack.translate(-target.x, -target.y, -target.z);
                removed.submitTemplate(collector, poseStack, 1 - progress * 0.5f);
            };
        } else if (standing.isEmpty()) {
            renderedFrames++;
            AABB box = shrunk(progress).move(offset);
            return (collector, poseStack) -> RendererUtils.submitLineBox(collector, poseStack, box, BOX_COLOR, lineWidth);
        }
        return null;
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
