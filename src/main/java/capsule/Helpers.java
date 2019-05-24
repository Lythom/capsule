package capsule;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Helpers {

    protected static final Logger LOGGER = LogManager.getLogger(Helpers.class);

    public static BlockPos findBottomBlock(EntityItem entityItem) {
        return findBottomBlock(entityItem.posX, entityItem.posY, entityItem.posZ);
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

    public static RayTraceResult clientRayTracePreview(EntityPlayer thePlayer, float partialTicks, int size) {
        int blockReachDistance = 18 + size;
        Vec3d vec3d = thePlayer.getPositionEyes(partialTicks);
        Vec3d vec3d1 = thePlayer.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.addVector(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        boolean stopOnLiquid = !isImmergedInLiquid(thePlayer);
        RayTraceResult rtc = thePlayer.getEntityWorld().rayTraceBlocks(vec3d, vec3d2, stopOnLiquid);
        return rtc;
    }

    @SuppressWarnings("rawtypes")
    public static BlockPos findSpecificBlock(EntityItem entityItem, int maxRange, Class searchedBlock) {
        if (searchedBlock == null)
            return null;

        double i = entityItem.posX;
        double j = entityItem.posY;
        double k = entityItem.posZ;

        for (int range = 1; range < maxRange; range++) {
            Iterable<MutableBlockPos> blockPoss = BlockPos.getAllInBoxMutable(new BlockPos(i - range, j - range, k - range),
                    new BlockPos(i + range, j + range, k + range));
            for (BlockPos pos : blockPoss) {
                Block block = entityItem.getEntityWorld().isBlockLoaded(pos) ? entityItem.getEntityWorld().getBlockState(pos).getBlock() : null;
                if (block != null && block.getClass().equals(searchedBlock)) {
                    return new BlockPos(pos.getX(), pos.getY(), pos.getZ()); // return a copy
                }
            }
        }

        return null;
    }

    /*
     * Color stuff
     */

    /**
     * Return whether the specified armor has a color.
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean hasColor(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        if (!stack.getTagCompound().hasKey("display", 10)) return false;
        return stack.getTagCompound().getCompoundTag("display").hasKey("color", 3);
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");
            if (nbttagcompound1.hasKey("color", 3)) {
                return nbttagcompound1.getInteger("color");
            }
        }

        return 0xFFFFFF;
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound == null) {
            nbttagcompound = new NBTTagCompound();
            stack.setTagCompound(nbttagcompound);
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");
        if (!nbttagcompound.hasKey("display", 10)) {
            nbttagcompound.setTag("display", nbttagcompound1);
        }

        nbttagcompound1.setInteger("color", color);
    }

    public static Block[] deserializeBlockArray(String[] blockIds) {
        ArrayList<Block> states = new ArrayList<>();
        ArrayList<String> notfound = new ArrayList<>();

        for (String blockId : blockIds) {
            Block b = Block.getBlockFromName(blockId);
            if (b != null) {
                states.add(b);
            } else {
                notfound.add(blockId);
            }
        }
        if (notfound.size() > 0) {
            LOGGER.warn(String.format(
                    "Blocks not found from config name : %s. Those blocks won't be considered in the overridable or excluded blocks list when capturing with capsule.",
                    String.join(", ", notfound.toArray(new CharSequence[notfound.size()]))
            ));
        }
        Block[] output = new Block[states.size()];
        return states.toArray(output);
    }

    public static String[] serializeBlockArray(Block[] states) {

        String[] blocksNames = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            blocksNames[i] = Block.REGISTRY.getNameForObject(states[i]).toString();
        }
        return blocksNames;

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

}
