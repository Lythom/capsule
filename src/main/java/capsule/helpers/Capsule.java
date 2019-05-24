package capsule.helpers;

import capsule.CommonProxy;
import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.network.CapsuleUndeployNotifToClient;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Capsule {

    protected static final Logger LOGGER = LogManager.getLogger(Capsule.class);

    static public String getMirrorLabel(PlacementSettings placement) {
        switch (placement.getMirror()) {
            case FRONT_BACK:
                return TextFormatting.STRIKETHROUGH + "[ ]";
            case LEFT_RIGHT:
                return "[I]";
        }
        return "[ ]";
    }

    static public String getRotationLabel(PlacementSettings placement) {
        switch (placement.getRotation()) {
            case CLOCKWISE_90:
                return "90";
            case CLOCKWISE_180:
                return "180";
            case COUNTERCLOCKWISE_90:
                return "270";
        }
        return "0";
    }

    public static void resentToCapsule(final ItemStack capsule, final EntityPlayer playerIn) {
        // store again
        Integer dimensionId = CapsuleItem.getDimension(capsule);
        MinecraftServer server = playerIn.getServer();
        if (server == null) return;
        final WorldServer world = dimensionId != null ? server.getWorld(dimensionId) : (WorldServer) playerIn.getEntityWorld();

        if (capsule.getTagCompound() == null) return;
        NBTTagCompound spawnPos = capsule.getTagCompound().getCompoundTag("spawnPosition");
        BlockPos startPos = new BlockPos(spawnPos.getInteger("x"), spawnPos.getInteger("y"), spawnPos.getInteger("z"));

        int size = CapsuleItem.getSize(capsule);

        // do the transportation
        if (CapsuleItem.isBlueprint(capsule)) {
            boolean blueprintMatch = StructureSaver.undeployBlueprint(world, playerIn.getName(), capsule.getTagCompound().getString("structureName"), startPos, size, CapsuleItem.getExcludedBlocs(capsule), CapsuleItem.getOccupiedSourcePos(capsule));
            if (blueprintMatch) {
                CapsuleItem.setState(capsule, CapsuleItem.STATE_BLUEPRINT);
                CapsuleItem.cleanDeploymentTags(capsule);
                notifyUndeploy(playerIn, startPos, size);
            } else {
                playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintDontMatch"));
            }
        } else {
            boolean storageOK = StructureSaver.store(world, playerIn.getName(), capsule.getTagCompound().getString("structureName"), startPos, size, CapsuleItem.getExcludedBlocs(capsule), CapsuleItem.getOccupiedSourcePos(capsule));
            if (storageOK) {
                CapsuleItem.setState(capsule, CapsuleItem.STATE_LINKED);
                CapsuleItem.cleanDeploymentTags(capsule);
                notifyUndeploy(playerIn, startPos, size);
            } else {
                LOGGER.error("Error occured during undeploy of capsule.");
                playerIn.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
            }
        }
    }

    private static void notifyUndeploy(EntityPlayer playerIn, BlockPos startPos, int size) {
        BlockPos center = startPos.add(size / 2, size / 2, size / 2);
        CommonProxy.simpleNetworkWrapper.sendToAllAround(
                new CapsuleUndeployNotifToClient(center, playerIn.getPosition(), size),
                new NetworkRegistry.TargetPoint(playerIn.dimension, center.getX(), center.getY(), center.getZ(), 200 + size)
        );
    }

    /**
     * Deploy the capsule at the anchorBlockPos position. update capsule state
     */
    public static boolean deployCapsule(ItemStack capsule, BlockPos anchorBlockPos, String thrower, int extendLength, WorldServer world) {
        // specify target to capture

        boolean didSpawn = false;

        BlockPos dest;
        if (capsule.getTagCompound() != null && capsule.getTagCompound().hasKey("deployAt")) {
            BlockPos centerDest = BlockPos.fromLong(capsule.getTagCompound().getLong("deployAt"));
            dest = centerDest.add(-extendLength, 0, -extendLength);
            capsule.getTagCompound().removeTag("deployAt");
        } else {
            dest = anchorBlockPos.add(-extendLength, 1, -extendLength);
        }
        String structureName = capsule.getTagCompound().getString("structureName");

        // do the transportation
        Map<BlockPos, Block> occupiedSpawnPositions = new HashMap<>();
        List<String> outEntityBlocking = new ArrayList<>();

        boolean result = StructureSaver.deploy(capsule, world, thrower, dest, Config.overridableBlocks, occupiedSpawnPositions, outEntityBlocking, CapsuleItem.getPlacement(capsule));

        if (result) {

            CapsuleItem.setOccupiedSourcePos(capsule, occupiedSpawnPositions);

            // register the link in the capsule
            if (!CapsuleItem.isReward(capsule)) {
                CapsuleItem.saveSpawnPosition(capsule, dest, world.provider.getDimension());
                CapsuleItem.setState(capsule, CapsuleItem.STATE_DEPLOYED);
                if (!CapsuleItem.isBlueprint(capsule)) {
                    // remove the content from the structure block to prevent dupe using recovery capsule
                    StructureSaver.clearTemplate(world, structureName);
                }
            }

            didSpawn = true;

        } else {
            // could not deploy, either entity or block preventing merge
            CapsuleItem.revertStateFromActivated(capsule);
        }

        return didSpawn;
    }

    /**
     * Capture the content around the capsule entityItem, update capsule state.
     */
    public static boolean captureContentIntoCapsule(ItemStack capsule, BlockPos anchor, String thrower, int size, int extendLength, WorldServer playerWorld) {

        // if there is an anchor, it's an initial capture, else an undeploy
        if (anchor != null) {
            BlockPos source = anchor.add(-extendLength, 1, -extendLength);

            // Save the region in a structure block file
            return captureAtPosition(capsule, thrower, size, playerWorld, source);
        } else {
            CapsuleItem.revertStateFromActivated(capsule);
            // send a chat message to explain failure
            EntityPlayer player = playerWorld.getPlayerEntityByName(thrower);
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.noCaptureBase"));
            }
        }

        return false;
    }

    public static boolean captureAtPosition(ItemStack capsule, String thrower, int size, WorldServer playerWorld, BlockPos source) {
        String player = "CapsuleMod";
        if (thrower != null) {
            player = thrower;
        }
        String capsuleID = StructureSaver.getUniqueName(playerWorld, player);
        boolean storageOK = StructureSaver.store(playerWorld, player, capsuleID, source, size, CapsuleItem.getExcludedBlocs(capsule), null);

        if (storageOK) {
            // register the link in the capsule
            CapsuleItem.setState(capsule, CapsuleItem.STATE_LINKED);
            CapsuleItem.setStructureName(capsule, capsuleID);
            return true;
        } else {
            // could not capture, StructureSaver.store handles the feedback already
            CapsuleItem.revertStateFromActivated(capsule);
        }
        return false;
    }

    public static void showUndeployParticules(WorldClient world, BlockPos posFrom, BlockPos posTo, int size) {
        for (int i = 0; i < 8 * size; i++) {
            double x = (double) posFrom.getX() + 0.5D + Math.random() * size - size * 0.5;
            double y = (double) posFrom.getY() + Math.random() * size - size * 0.5;
            double z = (double) posFrom.getZ() + 0.5D + Math.random() * size - size * 0.5;

            Vec3d speed = new Vec3d(
                    posTo.getX() - x,
                    posTo.getY() - y,
                    posTo.getZ() - z
            );
            double speedFactor = 0.1f + speed.lengthVector() * 0.04f;
            speed = speed.normalize();
            world.spawnParticle(
                    EnumParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    speed.x * speedFactor,
                    speed.y * speedFactor,
                    speed.z * speedFactor
            );
        }
    }

    public static void showDeployParticules(WorldServer world, BlockPos blockpos, int size) {
        double d0 = (double) ((float) blockpos.getX()) + 0.5D;
        double d1 = (double) ((float) blockpos.getY()) + 0.5D;
        double d2 = (double) ((float) blockpos.getZ()) + 0.5D;
        world.spawnParticle(EnumParticleTypes.CLOUD, d0, d1, d2, 8 * (size), 0.5D, 0.25D, 0.5D, 0.01 + 0.05 * size);
    }

    @Nullable
    public static Map<StructureSaver.ItemStackKey, Integer> reloadBlueprint(ItemStack blueprint, WorldServer world, EntityPlayer player) {
        // list required materials
        Map<StructureSaver.ItemStackKey, Integer> missingMaterials = StructureSaver.getMaterialList(blueprint, world);
        if (missingMaterials == null) {
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
            }
            return null;
        }
        // try to provision the materials from linked inventory or player inventory
        IItemHandler inv = CapsuleItem.getSourceInventory(blueprint, world);
        IItemHandler inv2 = player == null ? null : player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        Map<Integer, Integer> inv1SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv);
        Map<Integer, Integer> inv2SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv2);

        // if there is enough items, remove the provision items from inventories and recharge the capsule
        if (missingMaterials.size() == 0) {
            if (inv != null) inv1SlotQuantityProvisions.forEach((slot, qty) -> inv.extractItem(slot, qty, false));
            if (inv2 != null) inv2SlotQuantityProvisions.forEach((slot, qty) -> inv2.extractItem(slot, qty, false));
            CapsuleItem.setState(blueprint, CapsuleItem.STATE_BLUEPRINT);
            CapsuleItem.cleanDeploymentTags(blueprint);
        }

        return missingMaterials;
    }

    /**
     * Tell which quantities should be extracted from which slot to ay the price.
     */
    private static Map<Integer, Integer> recordSlotQuantityProvisions(Map<StructureSaver.ItemStackKey, Integer> missingMaterials, final IItemHandler inv) {
        Map<Integer, Integer> invSlotQuantityProvisions = new HashMap<>();
        if (inv != null) {
            int size = inv.getSlots();
            for (int invSlot = 0; invSlot < size; invSlot++) {
                ItemStack invStack = inv.getStackInSlot(invSlot);
                StructureSaver.ItemStackKey stackKey = new StructureSaver.ItemStackKey(invStack);
                Integer missing = missingMaterials.get(stackKey);
                if (missing != null && missing > 0 && invStack.getCount() > 0) {
                    if (invStack.getCount() >= missing) {
                        missingMaterials.remove(stackKey);
                        invSlotQuantityProvisions.put(invSlot, missing);
                    } else {
                        missingMaterials.put(stackKey, missing - invStack.getCount());
                        invSlotQuantityProvisions.put(invSlot, invStack.getCount());
                    }
                }
            }
        }
        return invSlotQuantityProvisions;
    }

    /**
     * Throw an item and return the new EntityItem created. Simulated a drop
     * with stronger throw.
     */
    public static EntityItem throwCapsule(ItemStack capsule, EntityPlayer playerIn, BlockPos destination) {
        // startPosition from EntityThrowable
        double startPosition = playerIn.posY - 0.3D + (double) playerIn.getEyeHeight();
        EntityItem entityitem = new EntityItem(playerIn.getEntityWorld(), playerIn.posX, startPosition, playerIn.posZ, capsule);
        entityitem.setPickupDelay(20);// cannot be picked up before deployment
        entityitem.setThrower(playerIn.getName());
        entityitem.setNoDespawn();

        if (destination != null && capsule.getTagCompound() != null) {
            capsule.getTagCompound().setLong("deployAt", destination.toLong());

            Spacial.moveEntityItemToDeployPos(entityitem, capsule, false);
            BlockPos playerPos = playerIn.getPosition();
            // +0.5 to aim the center of the block
            double diffX = (destination.getX() + 0.5 - playerPos.getX());
            double diffZ = (destination.getZ() + 0.5 - playerPos.getZ());
            double flatDistance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

            double diffY = destination.getY() - playerPos.getY() + Math.min(1, flatDistance / 3);
            double yVelocity = (diffY / 10) - (0.5 * 10 * -1 * CapsuleItem.GRAVITY_PER_TICK); // move up then down
            entityitem.motionY = Math.max(0.05, yVelocity);

        } else {
            float f = 0.5F;

            entityitem.motionX = (double) (-MathHelper.sin(playerIn.rotationYaw * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f) + playerIn.motionX;
            entityitem.motionZ = (double) (MathHelper.cos(playerIn.rotationYaw * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f) + playerIn.motionZ;
            entityitem.motionY = (double) (-MathHelper.sin(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f + 0.1F) + playerIn.motionY;
        }

        playerIn.dropItemAndGetStack(entityitem);
        playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, ItemStack.EMPTY);
        playerIn.getEntityWorld().playSound(null, entityitem.getPosition(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.2F, 0.1f);

        return entityitem;
    }

    public static ItemStack createRewardCapsule(String structureName, int baseColor, int materialColor, int size, @Nullable String label, @Nullable String author) {
        ItemStack capsule = createEmptyCapsule(baseColor, materialColor, size, false, label, null);
        CapsuleItem.setIsReward(capsule);
        CapsuleItem.setStructureName(capsule, structureName);
        CapsuleItem.setAuthor(capsule, author);

        return capsule;
    }

    public static ItemStack createEmptyCapsule(int baseColor, int materialColor, int size, boolean overpowered, @Nullable String label, @Nullable Integer upgraded) {
        ItemStack capsule = new ItemStack(CapsuleItems.capsule, 1, CapsuleItem.STATE_EMPTY);
        MinecraftNBT.setColor(capsule, baseColor); // standard dye is for baseColor
        capsule.setTagInfo("color", new NBTTagInt(materialColor)); // "color" is for materialColor
        capsule.setTagInfo("size", new NBTTagInt(size));
        if (upgraded != null) {
            capsule.setTagInfo("upgraded", new NBTTagInt(upgraded));
        }
        if (overpowered) {
            capsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
        }
        if (label != null) {
            capsule.setTagInfo("label", new NBTTagString(label));
        }

        return capsule;
    }

    public static void handleEntityItemOnGround(EntityItem entityItem, ItemStack capsule) {
        // stop the capsule where it collided
        entityItem.motionX = 0;
        entityItem.motionZ = 0;
        entityItem.motionY = 0;

        final int size = CapsuleItem.getSize(capsule);
        final int extendLength = (size - 1) / 2;

        // get destination world available position
        final WorldServer itemWorld = (WorldServer) entityItem.getEntityWorld();

        if (CapsuleItem.hasStructureLink(capsule)) {

            // DEPLOY
            // is linked, deploy
            BlockPos throwPos = Spacial.findBottomBlock(entityItem);
            boolean deployed = deployCapsule(capsule, throwPos, entityItem.getThrower(), extendLength, itemWorld);
            if (deployed) {
                itemWorld.playSound(null, entityItem.getPosition(), SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.BLOCKS, 0.4F, 0.1F);
                showDeployParticules(itemWorld, entityItem.getPosition(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                entityItem.setDead();
            }

        } else {

            // CAPTURE
            // is not linked, capture
            try {
                BlockPos anchor = Spacial.findSpecificBlock(entityItem, size + 2, BlockCapsuleMarker.class);
                boolean captured = captureContentIntoCapsule(capsule, anchor, entityItem.getThrower(), size, extendLength, itemWorld);
                if (captured) {
                    BlockPos center = anchor.add(0, size / 2, 0);
                    CommonProxy.simpleNetworkWrapper.sendToAllAround(
                            new CapsuleUndeployNotifToClient(center, entityItem.getPosition(), size),
                            new NetworkRegistry.TargetPoint(entityItem.dimension, center.getX(), center.getY(), center.getZ(), 200 + size)
                    );
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't capture the content into the capsule", e);
            }
        }
    }
}
