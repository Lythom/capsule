package capsule.helpers;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.feature.template.Template;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class Spacial {
    public static final float MAX_BLOCKS_PER_TICK_THROW = 1.2f;

    public static BlockPos findBottomBlock(ItemEntity ItemEntity) {
        return findBottomBlock(ItemEntity.getPosX(), ItemEntity.getPosY(), ItemEntity.getPosZ());
    }

    public static BlockPos findBottomBlock(double x, double y, double z) {
        return BlockPos.getAllInBox(new BlockPos(x, y - 1, z), new BlockPos(x + 1, y + 1, z + 1))
                .min(Comparator.comparingDouble((BlockPos pos) -> pos.distanceSq(x, y, z, true))).orElse(null);
    }

    public static boolean isImmergedInLiquid(Entity entity) {
        if (entity == null) return false;
        return !entity.isOffsetPositionInLiquid(0, 1.5, 0);
    }

    public static BlockRayTraceResult clientRayTracePreview(PlayerEntity thePlayer, float partialTicks, int size) {
        int blockReachDistance = 18 + size;
        Vec3d vec3d = thePlayer.getEyePosition(partialTicks);
        Vec3d vec3d1 = thePlayer.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        boolean stopOnLiquid = !isImmergedInLiquid(thePlayer);
        return thePlayer.getEntityWorld().rayTraceBlocks(
                new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.COLLIDER, stopOnLiquid ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE, thePlayer)
        );
    }

    @Nullable
    public static BlockPos findSpecificBlock(ItemEntity ItemEntity, int maxRange, Class<? extends Block> searchedBlock) {
        if (searchedBlock == null)
            return null;

        double i = ItemEntity.getPosX();
        double j = ItemEntity.getPosY();
        double k = ItemEntity.getPosZ();

        for (int range = 1; range < maxRange; range++) {
            Iterable<BlockPos> blockPoss = BlockPos.getAllInBoxMutable(new BlockPos(i - range, j - range, k - range),
                    new BlockPos(i + range, j + range, k + range));
            for (BlockPos pos : blockPoss) {
                Block block = ItemEntity.getEntityWorld().isBlockLoaded(pos) ? ItemEntity.getEntityWorld().getBlockState(pos).getBlock() : null;
                if (block != null && block.getClass().equals(searchedBlock)) {
                    return new BlockPos(pos.getX(), pos.getY(), pos.getZ()); // return a copy
                }
            }
        }

        return null;
    }

    public static List<AxisAlignedBB> mergeVoxels(List<Template.BlockInfo> blocks) {

        Map<BlockPos, Template.BlockInfo> blocksByPos = new HashMap<>();
        Map<BlockPos, MutableBoundingBox> bbByPos = new HashMap<>();
        blocks.forEach(b -> blocksByPos.put(b.pos, b));

        blocks.forEach(block -> {
            BlockPos destPos = block.pos;
            BlockPos below = block.pos.add(0, -1, 0);
            if (bbByPos.containsKey(below) && blocksByPos.containsKey(below) && blocksByPos.get(below).state.getBlock() == block.state.getBlock()) {
                // extend the below BB to current
                MutableBoundingBox bb = bbByPos.get(below);
                bb.maxY++;
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
                    bb.expandTo(matchingBB);
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
                    bb.expandTo(matchingBB);
                    matchingBB = findMatchingExpandingZ(bb, allBB);
                }
            }
        });
        allBB.removeAll(toRemove);

        return allBB.stream().map(bb -> new AxisAlignedBB(
                new BlockPos(bb.minX, bb.minY, bb.minZ),
                new BlockPos(bb.maxX, bb.maxY, bb.maxZ)
        )).collect(Collectors.toList());
    }

    private static MutableBoundingBox findMatchingExpandingX(final MutableBoundingBox bb, final List<MutableBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY == bb.minY && candidate.maxY == bb.maxY && candidate.minZ == bb.minZ && candidate.maxZ == bb.maxZ && candidate.minX == bb.maxX + 1)
                .findFirst()
                .orElse(null);
    }

    private static MutableBoundingBox findMatchingExpandingZ(final MutableBoundingBox bb, final List<MutableBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY == bb.minY && candidate.maxY == bb.maxY && candidate.minX == bb.minX && candidate.maxX == bb.maxX && candidate.minZ == bb.maxZ + 1)
                .findFirst()
                .orElse(null);
    }

    public static boolean isThrowerUnderLiquid(final ItemEntity ItemEntity) {
        UUID thrower = ItemEntity.getThrowerId();
        if (thrower == null) return false;
        PlayerEntity player = ItemEntity.getEntityWorld().getPlayerByUuid(thrower);
        boolean underLiquid = isImmergedInLiquid(player);
        return underLiquid;
    }

    public static boolean isEntityCollidingLiquid(final ItemEntity ItemEntity) {
        return !ItemEntity.isOffsetPositionInLiquid(0, -0.1, 0);
    }

    public static boolean ItemEntityShouldAndCollideLiquid(final ItemEntity ItemEntity) {
        boolean throwerInLiquid = isThrowerUnderLiquid(ItemEntity);
        boolean entityInLiquid = isEntityCollidingLiquid(ItemEntity);
        return !throwerInLiquid && entityInLiquid;
    }

    public static void moveItemEntityToDeployPos(final ItemEntity ItemEntity, final ItemStack capsule, boolean keepMomentum) {
        if (capsule.getTag() == null) return;
        BlockPos dest = BlockPos.fromLong(capsule.getTag().getLong("deployAt"));
        // +0.5 to aim the center of the block
        double diffX = (dest.getX() + 0.5 - ItemEntity.getPosX());
        double diffZ = (dest.getZ() + 0.5 - ItemEntity.getPosZ());

        double distance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

        // velocity will slow down when approaching
        double requiredVelocity = distance / 10;
        double velocity = Math.min(requiredVelocity, MAX_BLOCKS_PER_TICK_THROW);
        double normalizedDiffX = (diffX / distance);
        double normalizedDiffZ = (diffZ / distance);

        // momentum allow to hit side walls
        Vec3d motion = ItemEntity.getMotion();
        ItemEntity.setMotion(
                keepMomentum ? 0.9 * motion.x + 0.1 * normalizedDiffX * velocity : normalizedDiffX * velocity,
                motion.y,
                keepMomentum ? 0.9 * motion.z + 0.1 * normalizedDiffZ * velocity : normalizedDiffZ * velocity
        );
    }
}
