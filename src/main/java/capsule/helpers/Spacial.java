package capsule.helpers;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.feature.template.Template;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Spacial {
    public static final float MAX_BLOCKS_PER_TICK_THROW = 1.2f;

    public static BlockPos findBottomBlock(ItemEntity ItemEntity) {
        return findBottomBlock(ItemEntity.posX, ItemEntity.posY, ItemEntity.posZ);
    }

    public static BlockPos findBottomBlock(double x, double y, double z) {

        Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(x, y - 1, z), new BlockPos(x + 1, y + 1, z + 1));
        BlockPos closest = null;
        double closestDistance = 1000;
        for (BlockPos pos : blockPoss) {
            double distance = pos.distanceSqToCenter(x, y, z);
            if (distance < closestDistance) {
                closest = pos;
                closestDistance = distance;
            }
        }

        return closest;
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
    public static BlockPos findSpecificBlock(ItemEntity ItemEntity, int maxRange, Class searchedBlock) {
        if (searchedBlock == null)
            return null;

        double i = ItemEntity.posX;
        double j = ItemEntity.posY;
        double k = ItemEntity.posZ;

        for (int range = 1; range < maxRange; range++) {
            Iterable<BlockPos.MutableBlockPos> blockPoss = BlockPos.getAllInBoxMutable(new BlockPos(i - range, j - range, k - range),
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
        Map<BlockPos, StructureBoundingBox> bbByPos = new HashMap<>();
        blocks.forEach(b -> blocksByPos.put(b.pos, b));

        blocks.forEach(block -> {
            BlockPos destPos = block.pos;
            BlockPos below = block.pos.add(0, -1, 0);
            if (bbByPos.containsKey(below) && blocksByPos.containsKey(below) && blocksByPos.get(below).blockState.getBlock() == block.blockState.getBlock()) {
                // extend the below BB to current
                StructureBoundingBox bb = bbByPos.get(below);
                bb.maxY++;
                bbByPos.put(destPos, bb);
            } else {
                // start a new column
                StructureBoundingBox column = new StructureBoundingBox(block.pos, block.pos);
                bbByPos.put(destPos, column);
            }
        });
        final List<StructureBoundingBox> allBB = bbByPos.values().stream().distinct().collect(Collectors.toList());

        // Merge X
        List<StructureBoundingBox> toRemove = new ArrayList<>();
        allBB.forEach(bb -> {
            if (!toRemove.contains(bb)) {
                StructureBoundingBox matchingBB = findMatchingExpandingX(bb, allBB);
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
                StructureBoundingBox matchingBB = findMatchingExpandingZ(bb, allBB);
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

    private static StructureBoundingBox findMatchingExpandingX(final StructureBoundingBox bb, final List<StructureBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY == bb.minY && candidate.maxY == bb.maxY && candidate.minZ == bb.minZ && candidate.maxZ == bb.maxZ && candidate.minX == bb.maxX + 1)
                .findFirst()
                .orElse(null);
    }

    private static StructureBoundingBox findMatchingExpandingZ(final StructureBoundingBox bb, final List<StructureBoundingBox> allBB) {
        return allBB.stream()
                .filter(candidate -> candidate != bb && candidate.minY == bb.minY && candidate.maxY == bb.maxY && candidate.minX == bb.minX && candidate.maxX == bb.maxX && candidate.minZ == bb.maxZ + 1)
                .findFirst()
                .orElse(null);
    }

    public static boolean isThrowerUnderLiquid(final ItemEntity ItemEntity) {
        UUID thrower = ItemEntity.getThrower();
        if (StringUtils.isNullOrEmpty(thrower)) return false;
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
        double diffX = (dest.getX() + 0.5 - ItemEntity.posX);
        double diffZ = (dest.getZ() + 0.5 - ItemEntity.posZ);

        double distance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

        // velocity will slow down when approaching
        double requiredVelocity = distance / 10;
        double velocity = Math.min(requiredVelocity, MAX_BLOCKS_PER_TICK_THROW);
        double normalizedDiffX = (diffX / distance);
        double normalizedDiffZ = (diffZ / distance);

        // momentum allow to hit side walls
        ItemEntity.motionX = keepMomentum ? 0.9 * ItemEntity.motionX + 0.1 * normalizedDiffX * velocity : normalizedDiffX * velocity;
        ItemEntity.motionZ = keepMomentum ? 0.9 * ItemEntity.motionZ + 0.1 * normalizedDiffZ * velocity : normalizedDiffZ * velocity;
    }
}
