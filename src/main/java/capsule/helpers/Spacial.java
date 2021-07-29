package capsule.helpers;

import capsule.blocks.BlockCapsuleMarker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.feature.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class Spacial {
    public static final float MAX_BLOCKS_PER_TICK_THROW = 1.2f;
    protected static final Logger LOGGER = LogManager.getLogger(Spacial.class);

    public static BlockPos findBottomBlock(ItemEntity ItemEntity) {
        return findBottomBlock(ItemEntity.getX(), ItemEntity.getY(), ItemEntity.getZ());
    }

    public static BlockPos findBottomBlock(double x, double y, double z) {
        return BlockPos.betweenClosedStream(new BlockPos(x, y - 1, z), new BlockPos(x + 1, y + 1, z + 1))
                .min(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(x, y, z, true))).orElse(null);
    }

    public static boolean isImmergedInLiquid(Entity entity) {
        if (entity == null) return false;
        return !entity.isFree(0, 1.5, 0);
    }

    public static BlockRayTraceResult clientRayTracePreview(PlayerEntity thePlayer, float partialTicks, int size) {
        int blockReachDistance = 18 + size;
        Vector3d vec3d = thePlayer.getEyePosition(partialTicks);
        Vector3d vec3d1 = thePlayer.getViewVector(partialTicks);
        Vector3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        boolean stopOnLiquid = !isImmergedInLiquid(thePlayer);
        return thePlayer.getCommandSenderWorld().clip(
                new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.OUTLINE, stopOnLiquid ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE, thePlayer)
        );
    }

    @Nullable
    public static BlockPos findSpecificBlock(ItemEntity ItemEntity, int maxRange, Class<? extends Block> searchedBlock) {
        if (searchedBlock == null)
            return null;

        double i = ItemEntity.getX();
        double j = ItemEntity.getY();
        double k = ItemEntity.getZ();

        for (int range = 1; range < maxRange; range++) {
            Iterable<BlockPos> blockPoss = BlockPos.betweenClosed(new BlockPos(i - range, j - range, k - range),
                    new BlockPos(i + range, j + range, k + range));
            for (BlockPos pos : blockPoss) {
                Block block = ItemEntity.getCommandSenderWorld().hasChunkAt(pos) ? ItemEntity.getCommandSenderWorld().getBlockState(pos).getBlock() : null;
                if (block != null && block.getClass().equals(searchedBlock)) {
                    return new BlockPos(pos.getX(), pos.getY(), pos.getZ()); // return a copy
                }
            }
        }

        return null;
    }

    public static BlockPos getAnchor(BlockPos captureBasePosition, BlockState captureBaseState, int size) {
        Direction direction = null;
        try {
            direction = captureBaseState.getValue(BlockCapsuleMarker.FACING);
        } catch (Exception e) {
            LOGGER.error("Could not get BlockCapsuleMarker.FACING in blockstate at " + captureBasePosition);
            return captureBasePosition;
        }
        return new BlockPos(
                captureBasePosition.getX() + (double) direction.getStepX() * (0.5 + size * 0.5),
                captureBasePosition.getY() + (double) direction.getStepY() + (direction.getStepY() < 0 ? -size :  - 1),
                captureBasePosition.getZ() + (double) direction.getStepZ() * (0.5 + size * 0.5)
        );
    }

    public static List<AxisAlignedBB> mergeVoxels(List<Template.BlockInfo> blocks) {

        Map<BlockPos, Template.BlockInfo> blocksByPos = new HashMap<>();
        Map<BlockPos, MutableBoundingBox> bbByPos = new HashMap<>();
        blocks.forEach(b -> blocksByPos.put(b.pos, b));

        blocks.forEach(block -> {
            BlockPos destPos = block.pos;
            BlockPos below = block.pos.offset(0, -1, 0);
            if (bbByPos.containsKey(below) && blocksByPos.containsKey(below) && blocksByPos.get(below).state.getBlock() == block.state.getBlock()) {
                // extend the below BB to current
                MutableBoundingBox bb = bbByPos.get(below);
                bb.y1++;
                bbByPos.put(destPos, bb);
            } else {
                // start a new column
                MutableBoundingBox column = new MutableBoundingBox(block.pos, block.pos);
                bbByPos.put(destPos, column);
            }
        });
        final List<MutableBoundingBox> allBB = bbByPos.values().stream().distinct().collect(Collectors.toList());

        // Merge X
        List<MutableBoundingBox> toRemove = new ArrayList<>();
        allBB.forEach(bb -> {
            if (!toRemove.contains(bb)) {
                MutableBoundingBox matchingBB = findMatchingExpandingX(bb, allBB);
                while (matchingBB != null) {
                    toRemove.add(matchingBB);
                    bb.expand(matchingBB);
                    matchingBB = findMatchingExpandingX(bb, allBB);
                }
            }
        });
        allBB.removeAll(toRemove);
        toRemove.clear();

        // Merge Z
        allBB.forEach(bb -> {
            if (!toRemove.contains(bb)) {
                MutableBoundingBox matchingBB = findMatchingExpandingZ(bb, allBB);
                while (matchingBB != null) {
                    toRemove.add(matchingBB);
                    bb.expand(matchingBB);
                    matchingBB = findMatchingExpandingZ(bb, allBB);
                }
            }
        });
        allBB.removeAll(toRemove);

        return allBB.stream().map(bb -> new AxisAlignedBB(
                new BlockPos(bb.x0, bb.y0, bb.z0),
                new BlockPos(bb.x1, bb.y1, bb.z1)
        )).collect(Collectors.toList());
    }

    private static MutableBoundingBox findMatchingExpandingX(final MutableBoundingBox bb, final List<MutableBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.y0 == bb.y0 && candidate.y1 == bb.y1 && candidate.z0 == bb.z0 && candidate.z1 == bb.z1 && candidate.x0 == bb.x1 + 1)
                .findFirst()
                .orElse(null);
    }

    private static MutableBoundingBox findMatchingExpandingZ(final MutableBoundingBox bb, final List<MutableBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.y0 == bb.y0 && candidate.y1 == bb.y1 && candidate.x0 == bb.x0 && candidate.x1 == bb.x1 && candidate.z0 == bb.z1 + 1)
                .findFirst()
                .orElse(null);
    }

    public static boolean isThrowerUnderLiquid(final ItemEntity ItemEntity) {
        UUID thrower = ItemEntity.getThrower();
        if (thrower == null) return false;
        PlayerEntity player = ItemEntity.getCommandSenderWorld().getPlayerByUUID(thrower);
        boolean underLiquid = isImmergedInLiquid(player);
        return underLiquid;
    }

    public static boolean isEntityCollidingLiquid(final ItemEntity ItemEntity) {
        return !ItemEntity.isFree(0, -0.1, 0);
    }

    public static boolean ItemEntityShouldAndCollideLiquid(final ItemEntity ItemEntity) {
        boolean throwerInLiquid = isThrowerUnderLiquid(ItemEntity);
        boolean entityInLiquid = isEntityCollidingLiquid(ItemEntity);
        return !throwerInLiquid && entityInLiquid;
    }

    public static void moveItemEntityToDeployPos(final ItemEntity ItemEntity, final ItemStack capsule, boolean keepMomentum) {
        if (capsule.getTag() == null) return;
        BlockPos dest = BlockPos.of(capsule.getTag().getLong("deployAt"));
        // +0.5 to aim the center of the block
        double diffX = (dest.getX() + 0.5 - ItemEntity.getX());
        double diffZ = (dest.getZ() + 0.5 - ItemEntity.getZ());

        double distance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

        // velocity will slow down when approaching
        double requiredVelocity = distance / 10;
        double velocity = Math.min(requiredVelocity, MAX_BLOCKS_PER_TICK_THROW);
        double normalizedDiffX = (diffX / distance);
        double normalizedDiffZ = (diffZ / distance);

        // momentum allow to hit side walls
        Vector3d motion = ItemEntity.getDeltaMovement();
        ItemEntity.setDeltaMovement(
                keepMomentum ? 0.9 * motion.x + 0.1 * normalizedDiffX * velocity : normalizedDiffX * velocity,
                motion.y,
                keepMomentum ? 0.9 * motion.z + 0.1 * normalizedDiffZ * velocity : normalizedDiffZ * velocity
        );
    }

    public static AxisAlignedBB getBB(double relativeX, double relativeY, double relativeZ, int size, int extendSize) {
        AxisAlignedBB boundingBox = new AxisAlignedBB(
                -extendSize - 0.01 + relativeX,
                1.01 + relativeY,
                -extendSize - 0.01 + relativeZ,
                extendSize + 1.01 + relativeX,
                size + 1.01 + relativeY,
                extendSize + 1.01 + relativeZ);
        return boundingBox;
    }
}
