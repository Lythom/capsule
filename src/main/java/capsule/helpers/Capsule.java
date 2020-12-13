package capsule.helpers;

import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.CapsuleItems;
import capsule.loot.CapsuleLootEntry;
import capsule.network.CapsuleNetwork;
import capsule.network.CapsuleUndeployNotifToClient;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

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

    public static void resentToCapsule(final ItemStack capsule, final ServerWorld world, @Nullable final PlayerEntity playerIn) {
        // store again
        Integer dimensionId = CapsuleItem.getDimension(capsule);
        DimensionType dimType = DimensionType.getById(dimensionId);
        if (dimType == null) return;

        if (capsule.getTag() == null) return;
        CompoundNBT spawnPos = capsule.getTag().getCompound("spawnPosition");
        BlockPos startPos = new BlockPos(spawnPos.getInt("x"), spawnPos.getInt("y"), spawnPos.getInt("z"));

        int size = CapsuleItem.getSize(capsule);

        // do the transportation
        if (CapsuleItem.isBlueprint(capsule)) {
            boolean blueprintMatch = StructureSaver.undeployBlueprint(world, playerIn == null ? null : playerIn.getUniqueID(), capsule, startPos, size, CapsuleItem.getExcludedBlocs(capsule));
            if (blueprintMatch) {
                CapsuleItem.setState(capsule, CapsuleState.BLUEPRINT);
                CapsuleItem.cleanDeploymentTags(capsule);
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size);
            } else if (playerIn != null) {
                playerIn.sendMessage(new TranslationTextComponent("capsule.error.blueprintDontMatch"));
            }
        } else {
            CapsuleTemplate template = StructureSaver.undeploy(world, playerIn == null ? null : playerIn.getUniqueID(), capsule.getTag().getString("structureName"), startPos, size, CapsuleItem.getExcludedBlocs(capsule), CapsuleItem.getOccupiedSourcePos(capsule));
            boolean storageOK = template != null;
            if (storageOK) {
                CapsuleItem.setState(capsule, CapsuleState.LINKED);
                CapsuleItem.cleanDeploymentTags(capsule);
                CapsuleItem.setCanRotate(capsule, template.canRotate());
                CapsuleItem.setPlacement(capsule, new PlacementSettings());
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size);
            } else {
                LOGGER.error("Error occured during undeploy of capsule.");
                if (playerIn != null) playerIn.sendMessage(new TranslationTextComponent("capsule.error.technicalError"));
            }
        }
    }

    private static void notifyUndeploy(PlayerEntity playerIn, BlockPos startPos, int size) {
        BlockPos center = startPos.add(size / 2, size / 2, size / 2);
        CapsuleNetwork.wrapper.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, playerIn.dimension)),
                new CapsuleUndeployNotifToClient(center, playerIn.getPosition(), size)
        );
    }

    /**
     * Deploy the capsule at the anchorBlockPos position. update capsule state
     */
    public static boolean deployCapsule(ItemStack capsule, BlockPos anchorBlockPos, UUID thrower, int extendLength, ServerWorld world) {
        // specify target to capture

        boolean didSpawn = false;

        BlockPos dest;
        if (capsule.getTag() != null && capsule.getTag().contains("deployAt")) {
            BlockPos centerDest = BlockPos.fromLong(capsule.getTag().getLong("deployAt"));
            dest = centerDest.add(-extendLength, 0, -extendLength);
            capsule.getTag().remove("deployAt");
        } else {
            dest = anchorBlockPos.add(-extendLength, 1, -extendLength);
        }
        String structureName = capsule.getTag().getString("structureName");

        // do the transportation
        List<UUID> outEntityBlocking = new ArrayList<>();

        boolean result = StructureSaver.deploy(capsule, world, thrower, dest, Config.overridableBlocks, CapsuleItem.getPlacement(capsule));

        if (result) {
            // register the link in the capsule
            if (!CapsuleItem.isReward(capsule)) {
                CapsuleItem.saveSpawnPosition(capsule, dest, world.getDimension().getType().getId());
                CapsuleItem.setState(capsule, CapsuleState.DEPLOYED);
                if (!CapsuleItem.isBlueprint(capsule)) {
                    // remove the content from the structure block to prevent dupe using recovery capsule
                    clearTemplate(world, structureName);
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
     * Capture the content around the capsule ItemEntity, update capsule state.
     */
    public static boolean captureContentIntoCapsule(ItemStack capsule, BlockPos anchor, UUID thrower, int size, int extendLength, ServerWorld playerWorld) {

        // if there is an anchor, it's an initial capture, else an undeploy
        if (anchor != null) {
            BlockPos source = anchor.add(-extendLength, 1, -extendLength);

            // Save the region in a structure block file
            return captureAtPosition(capsule, thrower, size, playerWorld, source);
        } else {
            CapsuleItem.revertStateFromActivated(capsule);
            // send a chat message to explain failure
            PlayerEntity player = playerWorld.getPlayerByUuid(thrower);
            if (player != null) {
                player.sendMessage(new TranslationTextComponent("capsule.error.noCaptureBase"));
            }
        }

        return false;
    }

    public static boolean captureAtPosition(ItemStack capsule, UUID thrower, int size, ServerWorld playerWorld, BlockPos source) {
        String throwerId = "CapsuleMod";
        PlayerEntity player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerByUuid(thrower);
            throwerId = player.getGameProfile().getName();
        }
        String capsuleID = StructureSaver.getUniqueName(playerWorld, throwerId);
        CapsuleTemplate template = StructureSaver.undeploy(playerWorld, thrower, capsuleID, source, size, CapsuleItem.getExcludedBlocs(capsule), null);
        boolean storageOK = template != null;
        if (storageOK) {
            // register the link in the capsule
            CapsuleItem.setState(capsule, CapsuleState.LINKED);
            CapsuleItem.setStructureName(capsule, capsuleID);
            CapsuleItem.setCanRotate(capsule, template.canRotate());
            CapsuleItem.setPlacement(capsule, new PlacementSettings());
            return true;
        } else {
            // could not capture, StructureSaver.undeploy handles the feedback already
            CapsuleItem.revertStateFromActivated(capsule);
        }
        return false;
    }

    public static void showUndeployParticules(ClientWorld world, BlockPos posFrom, BlockPos posTo, int size) {
        for (int i = 0; i < 8 * size; i++) {
            double x = (double) posFrom.getX() + 0.5D + Math.random() * size - size * 0.5;
            double y = (double) posFrom.getY() + Math.random() * size - size * 0.5;
            double z = (double) posFrom.getZ() + 0.5D + Math.random() * size - size * 0.5;

            Vec3d speed = new Vec3d(
                    posTo.getX() - x,
                    posTo.getY() - y,
                    posTo.getZ() - z
            );
            double speedFactor = 0.1f + speed.length() * 0.04f;
            speed = speed.normalize();
            world.addParticle(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    speed.x * speedFactor,
                    speed.y * speedFactor,
                    speed.z * speedFactor
            );
        }
    }

    public static void showDeployParticules(ServerWorld world, BlockPos blockpos, int size) {
        double d0 = (double) ((float) blockpos.getX()) + 0.5D;
        double d1 = (double) ((float) blockpos.getY()) + 0.5D;
        double d2 = (double) ((float) blockpos.getZ()) + 0.5D;
        world.spawnParticle(ParticleTypes.CLOUD, d0, d1, d2, 8 * (size), 0.5D, 0.25D, 0.5D, 0.01 + 0.05 * size);
    }

    @Nullable
    public static Map<StructureSaver.ItemStackKey, Integer> reloadBlueprint(ItemStack blueprint, ServerWorld world, PlayerEntity player) {
        // list required materials
        Map<StructureSaver.ItemStackKey, Integer> missingMaterials = Blueprint.getMaterialList(blueprint, world, player);
        if (missingMaterials == null) {
            if (player != null) {
                player.sendMessage(new TranslationTextComponent("capsule.error.technicalError"));
            }
            return null;
        }
        // try to provision the materials from linked inventory or player inventory
        IItemHandler inv = CapsuleItem.getSourceInventory(blueprint, world);
        IItemHandler inv2 = player == null ? null : player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
        Map<Integer, Integer> inv1SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv);
        Map<Integer, Integer> inv2SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv2);

        // if there is enough items, remove the provision items from inventories and recharge the capsule
        if (missingMaterials.size() == 0) {
            if (inv != null) inv1SlotQuantityProvisions.forEach((slot, qty) -> extractItemOrFluid(inv, slot, qty));
            if (inv2 != null) inv2SlotQuantityProvisions.forEach((slot, qty) -> extractItemOrFluid(inv2, slot, qty));
            CapsuleItem.setState(blueprint, CapsuleState.BLUEPRINT);
            CapsuleItem.cleanDeploymentTags(blueprint);
        } else if (player != null && player.isCreative()) {
            CapsuleItem.setState(blueprint, CapsuleState.BLUEPRINT);
            CapsuleItem.cleanDeploymentTags(blueprint);
            missingMaterials.clear();
        }

        return missingMaterials;
    }

    public static void extractItemOrFluid(IItemHandler inv, Integer slot, Integer qty) {
        ItemStack item = inv.extractItem(slot, qty, false);
        ItemStack container = net.minecraftforge.common.ForgeHooks.getContainerItem(item);
        inv.insertItem(slot, container, false);
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
     * Throw an item and return the new ItemEntity created. Simulated a drop
     * with stronger throw.
     */
    public static ItemEntity throwCapsule(ItemStack capsule, PlayerEntity playerIn, BlockPos destination) {
        // startPosition from EntityThrowable
        double startPosition = playerIn.getPosY() - 0.3D + (double) playerIn.getEyeHeight();
        ItemEntity ItemEntity = new ItemEntity(playerIn.getEntityWorld(), playerIn.getPosX(), startPosition, playerIn.getPosZ(), capsule);
        ItemEntity.setPickupDelay(20);// cannot be picked up before deployment
        ItemEntity.setThrowerId(playerIn.getUniqueID());
        ItemEntity.setNoDespawn();

        if (destination != null && capsule.getTag() != null) {
            capsule.getTag().putLong("deployAt", destination.toLong());

            Spacial.moveItemEntityToDeployPos(ItemEntity, capsule, false);
            BlockPos playerPos = playerIn.getPosition();
            // +0.5 to aim the center of the block
            double diffX = (destination.getX() + 0.5 - playerPos.getX());
            double diffZ = (destination.getZ() + 0.5 - playerPos.getZ());
            double flatDistance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

            double diffY = destination.getY() - playerPos.getY() + Math.min(1, flatDistance / 3);
            double yVelocity = (diffY / 10) - (0.5 * 10 * -1 * CapsuleItem.GRAVITY_PER_TICK); // move up then down
            Vec3d currentMotion = ItemEntity.getMotion();
            ItemEntity.setMotion(currentMotion.x, Math.max(0.05, yVelocity), currentMotion.z);
        } else {
            float f = 0.5F;
            Vec3d playerInMotion = playerIn.getMotion();
            ItemEntity.setMotion(
                    (double) (-MathHelper.sin(playerIn.rotationYaw * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f) + playerInMotion.x,
                    (double) (-MathHelper.sin(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f + 0.1F) + playerInMotion.y,
                    (double) (MathHelper.cos(playerIn.rotationYaw * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.rotationPitch * CapsuleItem.TO_RAD) * f) + playerInMotion.z
            );
        }
        playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, ItemStack.EMPTY);
        playerIn.getEntityWorld().playSound(null, ItemEntity.getPosition(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.2F, 0.1f);
        playerIn.getEntityWorld().addEntity(ItemEntity);
        return ItemEntity;
    }

    public static ItemStack newRewardCapsuleItemStack(String structureName, int baseColor, int materialColor, int size, @Nullable String label, @Nullable String author) {
        ItemStack capsule = newEmptyCapsuleItemStack(baseColor, materialColor, size, false, label, null);
        CapsuleItem.setIsReward(capsule);
        CapsuleItem.setStructureName(capsule, structureName);
        CapsuleItem.setAuthor(capsule, author);

        return capsule;
    }

    public static ItemStack newLinkedCapsuleItemStack(String structureName, int baseColor, int materialColor, int size, boolean overpowered, @Nullable String label, @Nullable Integer upgraded) {
        ItemStack capsule = newEmptyCapsuleItemStack(baseColor, materialColor, size, overpowered, label, upgraded);
        CapsuleItem.setStructureName(capsule, structureName);
        CapsuleItem.setState(capsule, CapsuleState.LINKED);
        return capsule;
    }

    public static ItemStack newEmptyCapsuleItemStack(int baseColor, int materialColor, int size, boolean overpowered, @Nullable String label, @Nullable Integer upgraded) {
        ItemStack capsule = CapsuleItems.withState(CapsuleState.EMPTY);
        MinecraftNBT.setColor(capsule, baseColor); // standard dye is for baseColor
        capsule.setTagInfo("color", IntNBT.valueOf(materialColor)); // "color" is for materialColor
        capsule.setTagInfo("size", IntNBT.valueOf(size));
        if (upgraded != null) {
            capsule.setTagInfo("upgraded", IntNBT.valueOf(upgraded));
        }
        if (overpowered) {
            capsule.setTagInfo("overpowered", ByteNBT.valueOf(true));
        }
        if (label != null) {
            capsule.setTagInfo("label", StringNBT.valueOf(label));
        }

        return capsule;
    }

    public static void handleItemEntityOnGround(ItemEntity ItemEntity, ItemStack capsule) {
        // stop the capsule where it collided
        ItemEntity.setMotion(0, 0, 0);

        final int size = CapsuleItem.getSize(capsule);
        final int extendLength = (size - 1) / 2;

        // get destination world available position
        final ServerWorld itemWorld = (ServerWorld) ItemEntity.getEntityWorld();

        if (CapsuleItem.hasStructureLink(capsule)) {

            // DEPLOY
            // is linked, deploy
            BlockPos throwPos = Spacial.findBottomBlock(ItemEntity);
            boolean deployed = deployCapsule(capsule, throwPos, ItemEntity.getThrowerId(), extendLength, itemWorld);
            if (deployed) {
                itemWorld.playSound(null, ItemEntity.getPosition(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.4F, 0.1F);
                showDeployParticules(itemWorld, ItemEntity.getPosition(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                ItemEntity.remove();
            }

        } else {

            // CAPTURE
            // is not linked, capture
            try {
                BlockPos anchor = Spacial.findSpecificBlock(ItemEntity, size + 2, BlockCapsuleMarker.class);
                boolean captured = captureContentIntoCapsule(capsule, anchor, ItemEntity.getThrowerId(), size, extendLength, itemWorld);
                if (captured) {
                    BlockPos center = anchor.add(0, size / 2, 0);
                    CapsuleNetwork.wrapper.send(
                            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, ItemEntity.dimension)),
                            new CapsuleUndeployNotifToClient(center, ItemEntity.getPosition(), size)
                    );
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't capture the content into the capsule", e);
            }
        }
    }

    public static String labelFromPath(String path) {
        if (path.contains("/")) {
            return WordUtils.capitalize(path.substring(path.lastIndexOf("/") + 1).replace("_", " "));
        } else {
            return WordUtils.capitalize(path.replace("_", " "));
        }
    }

    public static ItemStack createLinkedCapsuleFromReward(String srcStructurePath, ServerPlayerEntity player) {
        if (player == null) return ItemStack.EMPTY;

        CapsuleTemplate srcTemplate = getRewardTemplateIfExists(srcStructurePath, player.getServer());
        if (srcTemplate == null) return ItemStack.EMPTY;

        int size = Math.max(srcTemplate.getSize().getX(), Math.max(srcTemplate.getSize().getY(), srcTemplate.getSize().getZ()));
        if (size % 2 == 0)
            size++;

        String destStructureName = StructureSaver.getUniqueName(player.getServerWorld(), player.getGameProfile().getName() + "-" + srcStructurePath.replace("/", "_"));
        ItemStack capsule = Capsule.newLinkedCapsuleItemStack(
                destStructureName,
                CapsuleLootEntry.getRandomColor(),
                CapsuleLootEntry.getRandomColor(),
                size,
                false,
                labelFromPath(srcStructurePath),
                0
        );

        CompoundNBT srcData = new CompoundNBT();
        srcTemplate.writeToNBT(srcData);
        StructureSaver.duplicateTemplate(
                srcData,
                destStructureName,
                StructureSaver.getTemplateManager(player.getServerWorld()),
                player.getServer()
        );
        return capsule;
    }

    public static CapsuleTemplate getRewardTemplateIfExists(String structurePath, MinecraftServer server) {
        CapsuleTemplateManager srcTemplatemanager = StructureSaver.getRewardManager(server);
        return srcTemplatemanager.getTemplateDefaulted(new ResourceLocation(structurePath));
    }

    public static boolean clearTemplate(ServerWorld worldserver, String capsuleStructureId) {
        CapsuleTemplateManager templatemanager = StructureSaver.getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return false;
        }
        CapsuleTemplate template = templatemanager.getTemplateDefaulted(new ResourceLocation(capsuleStructureId));
        if (template == null) return false;

        List<Template.BlockInfo> blocks = template.getBlocks();
        List<Template.EntityInfo> entities = template.entities;

        blocks.clear();
        entities.clear();

        return templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));

    }
}
