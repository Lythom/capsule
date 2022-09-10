package capsule.helpers;

import capsule.blocks.BlockCapsuleMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
                .min(Comparator.comparingDouble((BlockPos pos) -> pos.distToLowCornerSqr(x, y, z))).orElse(null);
    }

    public static boolean isImmergedInLiquid(Entity entity) {
        if (entity == null) return false;
        return !entity.isFree(0, 1.5, 0);
    }

    public static BlockHitResult clientRayTracePreview(Player thePlayer, float partialTicks, int size) {
        int blockReachDistance = 18 + size;
        Vec3 vec3d = thePlayer.getEyePosition(partialTicks);
        Vec3 vec3d1 = thePlayer.getViewVector(partialTicks);
        Vec3 vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        boolean stopOnLiquid = !isImmergedInLiquid(thePlayer);
        return thePlayer.getCommandSenderWorld().clip(
                new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, stopOnLiquid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, thePlayer)
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
                captureBasePosition.getY() + (double) direction.getStepY() + (direction.getStepY() < 0 ? -size : -1),
                captureBasePosition.getZ() + (double) direction.getStepZ() * (0.5 + size * 0.5)
        );
    }

    public static List<AABB> mergeVoxels(List<StructureTemplate.StructureBlockInfo> blocks) {

        if (blocks.size() > 8000) {
            // too big, use a simplified view of max size
            return new ArrayList<>();
        }

        Map<BlockPos, StructureTemplate.StructureBlockInfo> blocksByPos = new HashMap<>();
        Map<BlockPos, BoundingBox> bbByPos = new HashMap<>();
        blocks.forEach(b -> blocksByPos.put(b.pos, b));

        blocks.forEach(block -> {
            BlockPos destPos = block.pos;
            BlockPos below = block.pos.offset(0, -1, 0);
            if (bbByPos.containsKey(below) && blocksByPos.containsKey(below) && blocksByPos.get(below).state.getBlock() == block.state.getBlock()) {
                // extend the below BB to current
                BoundingBox bb = bbByPos.get(below);
                bbByPos.put(destPos, new BoundingBox(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY() + 1, bb.maxZ()));
            } else {
                // start a new column
                BoundingBox column = new BoundingBox(block.pos);
                bbByPos.put(destPos, column);
            }
        });
        final List<BoundingBox> allBB = bbByPos.values().stream().distinct().collect(Collectors.toList());

        // Merge X
        List<BoundingBox> toRemove = new ArrayList<>();
        allBB.forEach(bb -> {
            if (!toRemove.contains(bb)) {
                BoundingBox matchingBB = findMatchingExpandingX(bb, allBB);
                while (matchingBB != null) {
                    toRemove.add(matchingBB);
                    bb.encapsulate(matchingBB);
                    matchingBB = findMatchingExpandingX(bb, allBB);
                }
            }
        });
        allBB.removeAll(toRemove);
        toRemove.clear();

        // Merge Z
        allBB.forEach(bb -> {
            if (!toRemove.contains(bb)) {
                BoundingBox matchingBB = findMatchingExpandingZ(bb, allBB);
                while (matchingBB != null) {
                    toRemove.add(matchingBB);
                    bb.encapsulate(matchingBB);
                    matchingBB = findMatchingExpandingZ(bb, allBB);
                }
            }
        });
        allBB.removeAll(toRemove);

        return allBB.stream().map(bb -> new AABB(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX(), bb.maxY(), bb.maxZ()
        )).collect(Collectors.toList());
    }

    private static BoundingBox findMatchingExpandingX(final BoundingBox bb, final List<BoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY() == bb.minY() && candidate.maxY() == bb.maxY() && candidate.minZ() == bb.minZ() && candidate.maxZ() == bb.maxZ() && candidate.minX() == bb.maxX() + 1)
                .findFirst()
                .orElse(null);
    }

    private static BoundingBox findMatchingExpandingZ(final BoundingBox bb, final List<BoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY() == bb.minY() && candidate.maxY() == bb.maxY() && candidate.minX() == bb.minX() && candidate.maxX() == bb.maxX() && candidate.minZ() == bb.maxZ() + 1)
                .findFirst()
                .orElse(null);
    }

    public static boolean isThrowerUnderLiquid(final ItemEntity ItemEntity) {
        UUID thrower = ItemEntity.getThrower();
        if (thrower == null) return false;
        Player player = ItemEntity.getCommandSenderWorld().getPlayerByUUID(thrower);
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

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        // velocity will slow down when approaching
        double requiredVelocity = distance / 10;
        double velocity = Math.min(requiredVelocity, MAX_BLOCKS_PER_TICK_THROW);
        double normalizedDiffX = (diffX / distance);
        double normalizedDiffZ = (diffZ / distance);

        // momentum allow to hit side walls
        Vec3 motion = ItemEntity.getDeltaMovement();
        ItemEntity.setDeltaMovement(
                keepMomentum ? 0.9 * motion.x + 0.1 * normalizedDiffX * velocity : normalizedDiffX * velocity,
                motion.y,
                keepMomentum ? 0.9 * motion.z + 0.1 * normalizedDiffZ * velocity : normalizedDiffZ * velocity
        );
    }

    public static AABB getBB(double relativeX, double relativeY, double relativeZ, int size, int extendSize) {
        AABB boundingBox = new AABB(
                -extendSize - 0.01 + relativeX,
                1.01 + relativeY,
                -extendSize - 0.01 + relativeZ,
                extendSize + 1.01 + relativeX,
                size + 1.01 + relativeY,
                extendSize + 1.01 + relativeZ);
        return boundingBox;
    }
}
