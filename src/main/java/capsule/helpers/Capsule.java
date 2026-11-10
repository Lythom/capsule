package capsule.helpers;

import capsule.Config;
import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.CapsuleItems;
import capsule.loot.CapsuleLootEntry;
import capsule.network.CapsuleNetwork;
import capsule.network.CapsuleUndeployNotifToClient;
import capsule.plugins.claims.Claims;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.Util;
import net.minecraft.world.World;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        resentToCapsule(capsule, world, playerIn, playerIn instanceof ServerPlayerEntity ? (ServerPlayerEntity) playerIn : null);
    }

    /**
     * Captures the deployed content back, as actor for claim mods. playerIn, if any, sees the capture.
     */
    public static void resentToCapsule(final ItemStack capsule, final ServerWorld world, @Nullable final PlayerEntity playerIn, @Nullable final ServerPlayerEntity actor) {
        // store again
        RegistryKey<World> dimensionId = CapsuleItem.getDimension(capsule);
        ServerWorld capsuleWorld = world.getServer().getLevel(dimensionId);
        if (capsuleWorld == null) return;

        if (capsule.getTag() == null) return;
        CompoundNBT spawnPos = capsule.getTag().getCompound("spawnPosition");
        BlockPos startPos = new BlockPos(spawnPos.getInt("x"), spawnPos.getInt("y"), spawnPos.getInt("z"));

        int size = CapsuleItem.getSize(capsule);

        // do the transportation, a failed undeploy gives its own feedback
        if (CapsuleItem.isBlueprint(capsule)) {
            if (StructureSaver.undeployBlueprint(capsuleWorld, actor, capsule, startPos, size, CapsuleItem.getExcludedBlocs(capsule))) {
                CapsuleItem.setState(capsule, CapsuleState.BLUEPRINT);
                CapsuleItem.cleanDeploymentTags(capsule);
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size, null); // no cache clean for blueprints
            }
        } else {
            CapsuleTemplate template = StructureSaver.undeploy(capsuleWorld, actor, capsule.getTag().getString("structureName"), startPos, size, CapsuleItem.getExcludedBlocs(capsule), CapsuleItem.getOccupiedSourcePos(capsule));
            boolean storageOK = template != null;
            if (storageOK) {
                CapsuleItem.setState(capsule, CapsuleState.LINKED);
                CapsuleItem.cleanDeploymentTags(capsule);
                CapsuleItem.setCanRotate(capsule, template.canRotate());
                CapsuleItem.setPlacement(capsule, new PlacementSettings());
                if (playerIn != null) notifyUndeploy(playerIn, startPos, size, CapsuleItem.getStructureName(capsule));
            }
        }
    }

    private static void notifyUndeploy(PlayerEntity playerIn, BlockPos startPos, int size, String templateName) {
        BlockPos center = startPos.offset(size / 2, size / 2, size / 2);
        CapsuleNetwork.wrapper.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, playerIn.getCommandSenderWorld().dimension())),
                new CapsuleUndeployNotifToClient(center, playerIn.blockPosition(), size, templateName)
        );
    }

    /**
     * Deploy the capsule at the anchorBlockPos position, as actor for claim mods. update capsule state
     */
    public static boolean deployCapsule(ItemStack capsule, BlockPos anchorBlockPos, @Nullable ServerPlayerEntity actor, int extendLength, ServerWorld world) {
        // specify target to capture

        boolean didSpawn = false;

        if (CapsuleItem.needsTemplateCopy(capsule)) {
            // never deploy the empty template a capsule without its copy points at
            if (!copyLinkedTemplate(capsule, world, actor)) {
                if (capsule.getTag() != null) capsule.getTag().remove("deployAt");
                CapsuleItem.revertStateFromActivated(capsule);
                return false;
            }
            // the copy may have grown the capsule
            extendLength = (CapsuleItem.getSize(capsule) - 1) / 2;
        }

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
        boolean result = StructureSaver.deploy(capsule, world, actor, dest, Config.overridableBlocks, CapsuleItem.getPlacement(capsule));

        if (result) {
            // register the link in the capsule
            if (!CapsuleItem.isReward(capsule)) {
                CapsuleItem.saveSpawnPosition(capsule, dest, world.dimension().location().toString());
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
            BlockPos source = anchor.offset(-extendLength, 1, -extendLength);

            // Save the region in a structure block file
            return captureAtPosition(capsule, Claims.player(playerWorld, thrower), size, playerWorld, source);
        } else {
            CapsuleItem.revertStateFromActivated(capsule);
            // send a chat message to explain failure
            PlayerEntity player = playerWorld.getPlayerByUUID(thrower);
            if (player != null) {
                player.sendMessage(new TranslationTextComponent("capsule.error.noCaptureBase"), Util.NIL_UUID);
            }
        }

        return false;
    }

    /**
     * Captures the content at source into a new template, as actor for claim mods.
     */
    public static boolean captureAtPosition(ItemStack capsule, @Nullable ServerPlayerEntity actor, int size, ServerWorld playerWorld, BlockPos source) {
        String throwerId = actor != null ? actor.getGameProfile().getName() : "CapsuleMod";
        String capsuleID = StructureSaver.getUniqueName(playerWorld, throwerId);
        CapsuleTemplate template = StructureSaver.undeploy(playerWorld, actor, capsuleID, source, size, CapsuleItem.getExcludedBlocs(capsule), null);
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

            Vector3d speed = new Vector3d(
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
        world.sendParticles(ParticleTypes.CLOUD, d0, d1, d2, 8 * (size), 0.5D, 0.25D, 0.5D, 0.01 + 0.05 * size);
    }

    @Nullable
    public static Map<StructureSaver.ItemStackKey, Integer> reloadBlueprint(ItemStack blueprint, ServerWorld world, PlayerEntity player) {
        // list required materials
        Map<StructureSaver.ItemStackKey, Integer> missingMaterials = Blueprint.getMaterialList(blueprint, world, player);
        if (missingMaterials == null) {
            if (player != null) {
                player.sendMessage(new TranslationTextComponent("capsule.error.technicalError"), Util.NIL_UUID);
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
            double flatDistance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

            double diffY = destination.getY() - playerPos.getY() + Math.min(1, flatDistance / 3);
            double yVelocity = (diffY / 10) - (0.5 * 10 * -1 * CapsuleItem.GRAVITY_PER_TICK); // move up then down
            Vector3d currentMotion = ItemEntity.getDeltaMovement();
            ItemEntity.setDeltaMovement(currentMotion.x, Math.max(0.05, yVelocity), currentMotion.z);
        } else {
            float f = 0.5F;
            Vector3d playerInMotion = playerIn.getDeltaMovement();
            ItemEntity.setDeltaMovement(
                    (double) (-MathHelper.sin(playerIn.yRot * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.xRot * CapsuleItem.TO_RAD) * f) + playerInMotion.x,
                    (double) (-MathHelper.sin(playerIn.xRot * CapsuleItem.TO_RAD) * f + 0.1F) + playerInMotion.y,
                    (double) (MathHelper.cos(playerIn.yRot * CapsuleItem.TO_RAD) * MathHelper.cos(playerIn.xRot * CapsuleItem.TO_RAD) * f) + playerInMotion.z
            );
        }
        playerIn.inventory.setItem(playerIn.inventory.selected, ItemStack.EMPTY);
        playerIn.getCommandSenderWorld().playSound(null, ItemEntity.blockPosition(), SoundEvents.ARROW_SHOOT, SoundCategory.BLOCKS, 0.2F, 0.1f);
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
        capsule.addTagElement("color", IntNBT.valueOf(materialColor)); // "color" is for materialColor
        capsule.addTagElement("size", IntNBT.valueOf(size));
        if (upgraded != null) {
            capsule.addTagElement("upgraded", IntNBT.valueOf(upgraded));
        }
        if (overpowered) {
            capsule.addTagElement("overpowered", ByteNBT.valueOf(true));
        }
        if (label != null) {
            capsule.addTagElement("label", StringNBT.valueOf(label));
        }

        return capsule;
    }

    public static void handleItemEntityOnGround(ItemEntity ItemEntity, ItemStack capsule) {
        // stop the capsule where it collided
        ItemEntity.setDeltaMovement(0, 0, 0);

        final int size = CapsuleItem.getSize(capsule);
        final int extendLength = (size - 1) / 2;

        // get destination world available position
        final ServerWorld itemWorld = (ServerWorld) ItemEntity.getCommandSenderWorld();

        if (CapsuleItem.hasStructureLink(capsule)) {

            // DEPLOY
            // is linked, deploy
            BlockPos throwPos = Spacial.findBottomBlock(ItemEntity);
            boolean deployed = deployCapsule(capsule, throwPos, Claims.player(itemWorld, ItemEntity.getThrower()), extendLength, itemWorld);
            if (deployed) {
                itemWorld.playSound(null, ItemEntity.blockPosition(), SoundEvents.ARROW_SHOOT, SoundCategory.BLOCKS, 0.4F, 0.1F);
                showDeployParticules(itemWorld, ItemEntity.blockPosition(), CapsuleItem.getSize(capsule));
            }
            if (deployed && CapsuleItem.isOneUse(capsule)) {
                ItemEntity.remove();
            }

        } else {

            // CAPTURE
            // is not linked, capture
            try {
                BlockPos captureBasePosition = Spacial.findSpecificBlock(ItemEntity, size + 2, BlockCapsuleMarker.class);
                BlockPos anchor = Spacial.getAnchor(captureBasePosition, itemWorld.getBlockState(captureBasePosition), size);

                boolean captured = captureContentIntoCapsule(capsule, anchor, ItemEntity.getThrower(), size, extendLength, itemWorld);
                if (captured) {
                    BlockPos center = anchor.offset(0, size / 2, 0);
                    CapsuleNetwork.wrapper.send(
                            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, itemWorld.dimension())),
                            new CapsuleUndeployNotifToClient(center, ItemEntity.blockPosition(), size, CapsuleItem.getStructureName(capsule))
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

        String destStructureName = StructureSaver.getUniqueName(player.getLevel(), player.getGameProfile().getName() + "-" + srcStructurePath.replace("/", "_"));
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
        srcTemplate.save(srcData);
        StructureSaver.duplicateTemplate(
                srcData,
                destStructureName,
                StructureSaver.getTemplateManager(player.getLevel()),
                player.getServer()
        );
        CapsuleItem.setCanRotate(capsule, srcTemplate.canRotate());
        return capsule;
    }

    /**
     * Gives a linked capsule tagged templateShouldBeCopied its own copy of the config template its structureName points
     * at, as /capsule giveLinked does: a complete copy, inventories included, saved in the world under a new name. The
     * capsule keeps its colors and label, and grows to the template size if it is smaller.
     *
     * @param player who gets the capsule, named in the new template and told when the copy fails; may be null
     * @return false when the source is outside the config folder, missing or empty, or the copy can't be written: the
     * capsule keeps its tag and is marked templateCopyFailed, the error is logged and the player told
     */
    public static boolean copyLinkedTemplate(ItemStack capsule, ServerWorld level, @Nullable PlayerEntity player) {
        String source = CapsuleItem.getStructureName(capsule);
        MinecraftServer server = level.getServer();
        boolean allowed = StructureSaver.isConfigTemplatePath(source);
        CapsuleTemplate template = allowed ? StructureSaver.getConfigTemplate(source, server) : null;
        if (template == null) {
            if (allowed) {
                LOGGER.error("Capsule template {} not copied: template not found or empty, searched {}", source, StructureSaver.searchedConfigLocations(source, server));
            } else {
                LOGGER.error("Capsule template {} not copied: templateShouldBeCopied only copies templates of the config folder (config/ or {})", source, Config.rewardTemplatesPath);
            }
            capsule.getOrCreateTag().putBoolean("templateCopyFailed", true);
            if (player != null) {
                player.sendMessage(new TranslationTextComponent(allowed ? "capsule.error.copySourceNotFound" : "capsule.error.copySourceNotAllowed", source), Util.NIL_UUID);
            }
            return false;
        }

        String owner = player != null ? player.getGameProfile().getName() : "CapsuleMod";
        String destStructureName = StructureSaver.getUniqueName(level, owner + "-" + source.replace("/", "_"));
        CapsuleTemplateManager manager = StructureSaver.getTemplateManager(level);
        if (manager == null || !StructureSaver.duplicateTemplate(StructureSaver.getTemplateNBTData(template), destStructureName, manager, server)) {
            capsule.getOrCreateTag().putBoolean("templateCopyFailed", true);
            StructureSaver.printWriteTemplateError(player, destStructureName);
            return false;
        }

        // a template larger than the capsule would deploy past the checked destination
        CapsuleItem.setSize(capsule, Math.max(CapsuleItem.getSize(capsule), largestDimension(template)));
        CapsuleItem.setStructureName(capsule, destStructureName);
        boolean activated = CapsuleItem.hasState(capsule, CapsuleState.ACTIVATED) || CapsuleItem.hasState(capsule, CapsuleState.EMPTY_ACTIVATED)
                || CapsuleItem.hasState(capsule, CapsuleState.ONE_USE_ACTIVATED);
        boolean oneUse = CapsuleItem.isOneUse(capsule);
        CapsuleItem.setState(capsule, activated ? (oneUse ? CapsuleState.ONE_USE_ACTIVATED : CapsuleState.ACTIVATED) : (oneUse ? CapsuleState.ONE_USE : CapsuleState.LINKED));
        CapsuleItem.setCanRotate(capsule, template.canRotate());
        CompoundNBT tag = capsule.getOrCreateTag();
        tag.remove("templateShouldBeCopied");
        tag.remove("templateCopyFailed");
        return true;
    }

    /**
     * Copies the template of a linked capsule still tagged templateShouldBeCopied (an item that never went through a
     * player inventory, from an autocrafter for instance) before it is used.
     *
     * @return false when the capsule has no template of its own and must not be used
     */
    public static boolean ensureOwnTemplate(ItemStack capsule, ServerWorld level, @Nullable PlayerEntity player) {
        return !CapsuleItem.needsTemplateCopy(capsule) || copyLinkedTemplate(capsule, level, player);
    }

    private static int largestDimension(CapsuleTemplate template) {
        return Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
    }

    public static CapsuleTemplate getRewardTemplateIfExists(String structurePath, MinecraftServer server) {
        CapsuleTemplateManager srcTemplatemanager = StructureSaver.getRewardManager(server.getDataPackRegistries().getResourceManager());
        return srcTemplatemanager.getOrCreateTemplate(new ResourceLocation(structurePath));
    }

    public static boolean clearTemplate(ServerWorld worldserver, String capsuleStructureId) {
        CapsuleTemplateManager templatemanager = StructureSaver.getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return false;
        }
        CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(capsuleStructureId));
        if (template == null) return false;

        List<Template.BlockInfo> blocks = template.getPalette();
        List<Template.EntityInfo> entities = template.entities;

        blocks.clear();
        entities.clear();

        return templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));

    }
}
