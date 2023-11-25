package capsule.helpers;

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
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Capsule {

    protected static final Logger LOGGER = LogManager.getLogger(Capsule.class);

    static public String getMirrorLabel(StructurePlaceSettings placement) {
        return switch (placement.getMirror()) {
            case FRONT_BACK -> ChatFormatting.STRIKETHROUGH + "[ ]";
            case LEFT_RIGHT -> "[I]";
            default -> "[ ]";
        };
    }

    static public String getRotationLabel(StructurePlaceSettings placement) {
        return switch (placement.getRotation()) {
            case CLOCKWISE_90 -> "90";
            case CLOCKWISE_180 -> "180";
            case COUNTERCLOCKWISE_90 -> "270";
            default -> "0";
        };
    }

    public static void resentToCapsule(final ItemStack capsule, final ServerLevel world, @Nullable final Player playerIn) {
        // store again
        ResourceKey<Level> dimensionId = CapsuleItem.getDimension(capsule);
        ServerLevel capsuleWorld = world.getServer().getLevel(dimensionId);
        if (capsuleWorld == null) return;

        if (capsule.getTag() == null) return;
        CompoundTag spawnPos = capsule.getTag().getCompound("spawnPosition");
        BlockPos startPos = new BlockPos(spawnPos.getInt("x"), spawnPos.getInt("y"), spawnPos.getInt("z"));

        int size = CapsuleItem.getSize(capsule);

        // do the transportation
        if (CapsuleItem.isBlueprint(capsule)) {
            boolean blueprintMatch = StructureSaver.undeployBlueprint(capsuleWorld, playerIn == null ? null : playerIn.getUUID(), capsule, startPos, size, CapsuleItem.getExcludedBlocs(capsule));
            if (blueprintMatch) {
                CapsuleItem.setState(capsule, CapsuleState.BLUEPRINT);
                CapsuleItem.cleanDeploymentTags(capsule);
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size, null); // no cache clean for blueprints
            } else if (playerIn != null) {
                playerIn.sendSystemMessage(Component.translatable("capsule.error.blueprintDontMatch"));
            }
        } else {
            CapsuleTemplate template = StructureSaver.undeploy(capsuleWorld, playerIn == null ? null : playerIn.getUUID(), capsule.getTag().getString("structureName"), startPos, size, CapsuleItem.getExcludedBlocs(capsule), CapsuleItem.getOccupiedSourcePos(capsule));
            boolean storageOK = template != null;
            if (storageOK) {
                CapsuleItem.setState(capsule, CapsuleState.LINKED);
                CapsuleItem.cleanDeploymentTags(capsule);
                CapsuleItem.setCanRotate(capsule, template.canRotate());
                CapsuleItem.setPlacement(capsule, new StructurePlaceSettings());
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size, CapsuleItem.getStructureName(capsule));
            } else {
                LOGGER.error("Error occured during undeploy of capsule.");
                if (playerIn != null)
                    playerIn.sendSystemMessage(Component.translatable("capsule.error.technicalError"));
            }
        }
    }

    private static void notifyUndeploy(Player playerIn, BlockPos startPos, int size, String templateName) {
        BlockPos center = startPos.offset(size / 2, size / 2, size / 2);
        CapsuleNetwork.wrapper.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, playerIn.getCommandSenderWorld().dimension())),
                new CapsuleUndeployNotifToClient(center, playerIn.blockPosition(), size, templateName)
        );
    }

    /**
     * Deploy the capsule at the anchorBlockPos position. update capsule state
     */
    public static boolean deployCapsule(ItemStack capsule, BlockPos anchorBlockPos, UUID thrower, int extendLength, ServerLevel world) {
        // specify target to capture

        boolean didSpawn = false;

        BlockPos dest;
        if (capsule.getTag() != null && capsule.getTag().contains("deployAt")) {
            BlockPos centerDest = BlockPos.of(capsule.getTag().getLong("deployAt"));
            dest = centerDest.offset(-extendLength, 0, -extendLength);
            capsule.getTag().remove("deployAt");
        } else {
            dest = anchorBlockPos.offset(-extendLength, 1, -extendLength);
        }
        String structureName = capsule.getTag().getString("structureName");

        // do the transportation
        boolean result = StructureSaver.deploy(capsule, world, thrower, dest, CapsuleItem.getPlacement(capsule));

        if (result) {
            // register the link in the capsule
            if (!CapsuleItem.isReward(capsule)) {
                CapsuleItem.saveSpawnPosition(capsule, dest, world.dimension().location().toString());
                CapsuleItem.setState(capsule, CapsuleState.DEPLOYED);
                if (!CapsuleItem.isBlueprint(capsule)) {
                    // remove the content from the structure block to prevent dupe using recovery capsule
                    clearTemplate(structureName, world.getServer());
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
    public static boolean captureContentIntoCapsule(ItemStack capsule, BlockPos anchor, UUID thrower, int size, int extendLength, ServerLevel playerWorld) {

        // if there is an anchor, it's an initial capture, else an undeploy
        if (anchor != null) {
            BlockPos source = anchor.offset(-extendLength, 1, -extendLength);

            // Save the region in a structure block file
            return captureAtPosition(capsule, thrower, size, playerWorld, source);
        } else {
            CapsuleItem.revertStateFromActivated(capsule);
            // send a chat message to explain failure
            Player player = playerWorld.getPlayerByUUID(thrower);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("capsule.error.noCaptureBase"));
            }
        }

        return false;
    }

    public static boolean captureAtPosition(ItemStack capsule, UUID thrower, int size, ServerLevel playerWorld, BlockPos source) {
        String throwerId = "CapsuleMod";
        Player player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerByUUID(thrower);
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
            CapsuleItem.setPlacement(capsule, new StructurePlaceSettings());
            return true;
        } else {
            // could not capture, StructureSaver.undeploy handles the feedback already
            CapsuleItem.revertStateFromActivated(capsule);
        }
        return false;
    }

    public static void showUndeployParticules(ClientLevel world, BlockPos posFrom, BlockPos posTo, int size) {
        for (int i = 0; i < 8 * size; i++) {
            double x = (double) posFrom.getX() + 0.5D + Math.random() * size - size * 0.5;
            double y = (double) posFrom.getY() + Math.random() * size - size * 0.5;
            double z = (double) posFrom.getZ() + 0.5D + Math.random() * size - size * 0.5;

            Vec3 speed = new Vec3(
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

    public static void showDeployParticules(ServerLevel world, BlockPos blockpos, int size) {
        double d0 = (double) ((float) blockpos.getX()) + 0.5D;
        double d1 = (double) ((float) blockpos.getY()) + 0.5D;
        double d2 = (double) ((float) blockpos.getZ()) + 0.5D;
        world.sendParticles(ParticleTypes.CLOUD, d0, d1, d2, 8 * (size), 0.5D, 0.25D, 0.5D, 0.01 + 0.05 * size);
    }

    @Nullable
    public static Map<StructureSaver.ItemStackKey, Integer> reloadBlueprint(ItemStack blueprint, ServerLevel world, Player player) {
        // list required materials
        Map<StructureSaver.ItemStackKey, Integer> missingMaterials = Blueprint.getMaterialList(blueprint, world, player);
        if (missingMaterials == null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("capsule.error.technicalError"));
            }
            return null;
        }
        // try to provision the materials from linked inventory or player inventory
        IItemHandler inv = CapsuleItem.getSourceInventory(blueprint, world);
        IItemHandler inv2 = player == null ? null : player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
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
        ItemStack container = net.minecraftforge.common.ForgeHooks.getCraftingRemainingItem(item);
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
    public static ItemEntity throwCapsule(ItemStack capsule, Player playerIn, BlockPos destination) {
        // startPosition from EntityThrowable
        double startPosition = playerIn.getY() - 0.3D + (double) playerIn.getEyeHeight();
        ItemEntity ItemEntity = new ItemEntity(playerIn.getCommandSenderWorld(), playerIn.getX(), startPosition, playerIn.getZ(), capsule);
        ItemEntity.setPickUpDelay(20);// cannot be picked up before deployment
        ItemEntity.setThrower(playerIn.getUUID());
        ItemEntity.setExtendedLifetime();

        if (destination != null && capsule.getTag() != null) {
            capsule.getTag().putLong("deployAt", destination.asLong());

            Spacial.moveItemEntityToDeployPos(ItemEntity, capsule, false);
            BlockPos playerPos = playerIn.blockPosition();
            // +0.5 to aim the center of the block
            double diffX = (destination.getX() + 0.5 - playerPos.getX());
            double diffZ = (destination.getZ() + 0.5 - playerPos.getZ());
            double flatDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);

            double diffY = destination.getY() - playerPos.getY() + Math.min(1, flatDistance / 3);
            double yVelocity = (diffY / 10) - (0.5 * 10 * -1 * CapsuleItem.GRAVITY_PER_TICK); // move up then down
            Vec3 currentMotion = ItemEntity.getDeltaMovement();
            ItemEntity.setDeltaMovement(currentMotion.x, Math.max(0.05, yVelocity), currentMotion.z);
        } else {
            float f = 0.5F;
            Vec3 playerInMotion = playerIn.getDeltaMovement();
            ItemEntity.setDeltaMovement(
                    (double) (-Mth.sin(playerIn.getYRot() * CapsuleItem.TO_RAD) * Mth.cos(playerIn.getXRot() * CapsuleItem.TO_RAD) * f) + playerInMotion.x,
                    (double) (-Mth.sin(playerIn.getXRot() * CapsuleItem.TO_RAD) * f + 0.1F) + playerInMotion.y,
                    (double) (Mth.cos(playerIn.getYRot() * CapsuleItem.TO_RAD) * Mth.cos(playerIn.getXRot() * CapsuleItem.TO_RAD) * f) + playerInMotion.z
            );
        }
        playerIn.getInventory().setItem(playerIn.getInventory().selected, ItemStack.EMPTY);
        playerIn.getCommandSenderWorld().playSound(null, ItemEntity.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.2F, 0.1f);
        playerIn.getCommandSenderWorld().addFreshEntity(ItemEntity);
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
        capsule.addTagElement("color", IntTag.valueOf(materialColor)); // "color" is for materialColor
        capsule.addTagElement("size", IntTag.valueOf(size));
        if (upgraded != null) {
            capsule.addTagElement("upgraded", IntTag.valueOf(upgraded));
        }
        if (overpowered) {
            capsule.addTagElement("overpowered", ByteTag.valueOf(true));
        }
        if (label != null) {
            capsule.addTagElement("label", StringTag.valueOf(label));
        }

        return capsule;
    }

    public static void handleItemEntityOnGround(ItemEntity itemEntity, ItemStack capsule) {
        // stop the capsule where it collided
        itemEntity.setDeltaMovement(0, 0, 0);

        final int size = CapsuleItem.getSize(capsule);
        final int extendLength = (size - 1) / 2;

        // get destination world available position
        final ServerLevel itemWorld = (ServerLevel) itemEntity.getCommandSenderWorld();

        if (CapsuleItem.hasStructureLink(capsule)) {

            // DEPLOY
            // is linked, deploy
            BlockPos throwPos = Spacial.findBottomBlock(itemEntity);
            boolean deployed = deployCapsule(capsule, throwPos, itemEntity.thrower, extendLength, itemWorld);
            if (deployed) {
                itemWorld.playSound(null, itemEntity.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.4F, 0.1F);
                showDeployParticules(itemWorld, itemEntity.blockPosition(), size);
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                itemEntity.remove(Entity.RemovalReason.DISCARDED);
            }

        } else {

            // CAPTURE
            // is not linked, capture
            try {
                BlockPos captureBasePosition = Spacial.findSpecificBlock(itemEntity, size + 2, BlockCapsuleMarker.class);
                BlockPos anchor = Spacial.getAnchor(captureBasePosition, itemWorld.getBlockState(captureBasePosition), size);

                boolean captured = captureContentIntoCapsule(capsule, anchor, itemEntity.thrower, size, extendLength, itemWorld);
                if (captured) {
                    BlockPos center = anchor.offset(0, size / 2, 0);
                    CapsuleNetwork.wrapper.send(
                            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, itemWorld.dimension())),
                            new CapsuleUndeployNotifToClient(center, itemEntity.blockPosition(), size, CapsuleItem.getStructureName(capsule))
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

    public static ItemStack createLinkedCapsuleFromReward(String srcStructurePath, ServerPlayer player) {
        if (player == null) return ItemStack.EMPTY;

        CapsuleTemplate srcTemplate = getRewardTemplateIfExists(srcStructurePath, player.getServer());
        if (srcTemplate == null) return ItemStack.EMPTY;

        int size = Math.max(srcTemplate.getSize().getX(), Math.max(srcTemplate.getSize().getY(), srcTemplate.getSize().getZ()));
        if (size % 2 == 0)
            size++;

        String destStructureName = StructureSaver.getUniqueName(player.serverLevel(), player.getGameProfile().getName() + "-" + srcStructurePath.replace("/", "_"));
        ItemStack capsule = Capsule.newLinkedCapsuleItemStack(
                destStructureName,
                CapsuleLootEntry.getRandomColor(),
                CapsuleLootEntry.getRandomColor(),
                size,
                false,
                labelFromPath(srcStructurePath),
                0
        );

        CompoundTag srcData = new CompoundTag();
        srcTemplate.save(srcData);
        StructureSaver.duplicateTemplate(
                srcData,
                destStructureName,
                StructureSaver.getTemplateManager(player.serverLevel().getServer()),
                player.getServer()
        );
        CapsuleItem.setCanRotate(capsule, srcTemplate.canRotate());
        return capsule;
    }

    public static CapsuleTemplate getRewardTemplateIfExists(String structurePath, MinecraftServer server) {
        CapsuleTemplateManager srcTemplatemanager = StructureSaver.getRewardManager(server.getResourceManager());
        return srcTemplatemanager.getOrCreateTemplate(new ResourceLocation(structurePath));
    }

    public static boolean clearTemplate(String capsuleStructureId, MinecraftServer server) {
        CapsuleTemplateManager templatemanager = StructureSaver.getTemplateManager(server);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return false;
        }
        CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(capsuleStructureId));
        if (template == null) return false;

        List<StructureTemplate.StructureBlockInfo> blocks = template.getPalette();
        List<StructureTemplate.StructureEntityInfo> entities = template.entities;

        blocks.clear();
        entities.clear();

        return templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));

    }
}
