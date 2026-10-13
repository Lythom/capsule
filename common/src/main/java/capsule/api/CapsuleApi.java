package capsule.api;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.structure.CapsuleTemplate;
import capsule.structure.Deployments;
import capsule.structure.Deployments.Deployment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Captures, deploys and undeploys capsules from code, and lists where reusable capsules are deployed. Content is
 * positioned by its anchor: the block under the center of its bottom face, where a Capture Base facing up stands. The
 * actor is the player that claim mods check, or null for none. Players get no chat feedback.
 */
public final class CapsuleApi {

    private CapsuleApi() {
    }

    /**
     * Captures the cube of the capsule size above anchor into an empty capsule, which becomes linked.
     */
    public static boolean capture(ItemStack emptyCapsule, ServerLevel level, BlockPos anchor, @Nullable ServerPlayer actor) {
        if (!CapsuleItem.hasState(emptyCapsule, CapsuleState.EMPTY)) return false;
        int size = CapsuleItem.getSize(emptyCapsule);
        int extendLength = (size - 1) / 2;
        return Capsule.captureAtPosition(emptyCapsule, actor, size, level, anchor.offset(-extendLength, 1, -extendLength));
    }

    /**
     * Deploys a linked, one-use or blueprint capsule above anchor. A reusable capsule becomes deployed, a one-use capsule
     * is consumed. Fails when a block or an entity is in the way, when the actor may not build there, or when the
     * content cannot rotate and rotation is not NONE.
     */
    public static boolean deploy(ItemStack capsule, ServerLevel level, BlockPos anchor, Rotation rotation, @Nullable ServerPlayer actor) {
        boolean deployable = CapsuleItem.hasState(capsule, CapsuleState.LINKED)
                || CapsuleItem.hasState(capsule, CapsuleState.ONE_USE)
                || CapsuleItem.hasState(capsule, CapsuleState.BLUEPRINT);
        if (!deployable) return false;
        if (rotation != Rotation.NONE && !CapsuleItem.canRotate(capsule)) return false;
        CapsuleItem.setPlacement(capsule, new StructurePlaceSettings().setRotation(rotation));
        NBTHelper.updateTag(capsule, tag -> tag.remove("deployAt"));
        int extendLength = (CapsuleItem.getSize(capsule) - 1) / 2;
        boolean deployed = Capsule.deployCapsule(capsule, anchor, actor, extendLength, level);
        if (deployed && CapsuleItem.isOneUse(capsule)) capsule.shrink(1);
        return deployed;
    }

    /**
     * Undeploys a deployed capsule back into it, wherever its content is.
     */
    public static boolean undeploy(ItemStack capsule, ServerLevel level, @Nullable ServerPlayer actor) {
        if (!CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED)) return false;
        Capsule.resentToCapsule(capsule, level, null, actor);
        return !CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED);
    }

    /**
     * The reusable capsule deployments of the level that intersect box, blueprints excepted.
     */
    public static List<Deployment> deploymentsIntersecting(ServerLevel level, BoundingBox box) {
        return Deployments.get(level.getServer()).intersecting(level.dimension(), box);
    }

    /**
     * The positions where a deployment placed its content: its box, less the blocks that were there before the deploy.
     */
    public static Predicate<BlockPos> deployedContent(Deployment deployment, MinecraftServer server) {
        CapsuleTemplate template = StructureSaver.getTemplateManager(server).getOrCreateTemplate(ResourceLocation.parse(deployment.structureName()));
        Map<BlockPos, Block> occupied = template == null || template.occupiedPositions == null ? Map.of() : template.occupiedPositions;
        BoundingBox box = deployment.box();
        return pos -> box.isInside(pos) && !occupied.containsKey(pos);
    }
}
