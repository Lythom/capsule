package capsule;

import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StructureSaver {

    protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);
    public static final String BLUEPRINT_PREFIX = "B-";
    public static Map<String, CapsuleTemplateManager> CapsulesManagers = new HashMap<>();
    private static CapsuleTemplateManager RewardManager = null;
    private static List<String> outExcluded = new ArrayList<>();

    public static CapsuleTemplateManager getRewardManager(MinecraftServer server) {
        if (RewardManager == null) {
            RewardManager = new CapsuleTemplateManager(server.getDataDirectory().getPath(), net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer());
            File rewardDir = new File(Config.rewardTemplatesPath);
            if (!rewardDir.exists()) {
                rewardDir.mkdirs();
            }
        }
        return RewardManager;
    }

    public static CapsuleTemplate undeploy(ServerWorld worldserver, String playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                           Map<BlockPos, Block> legacyItemOccupied) {

        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        if (minecraftserver == null) {
            LOGGER.error("worldserver.getMinecraftServer() returned null");
            return null;
        }
        List<Entity> outCapturedEntities = new ArrayList<>();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
        Map<BlockPos, Block> occupiedPositions = template.occupiedPositions;
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = template.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, outCapturedEntities);
        template.removeOccupiedPositions();
        PlayerEntity player = null;
        if (playerID != null) {
            template.setAuthor(playerID);
            player = worldserver.getPlayerEntityByName(playerID);
        }
        boolean writingOK = templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
        if (writingOK) {
            List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
            for (Entity e : outCapturedEntities) {
                e.setDropItemsWhenDead(false);
                e.setDead();
            }
            // check if some remove failed, exclude those blocks from the template.
            if (couldNotBeRemoved != null) {
                template.removeBlocks(couldNotBeRemoved, startPos);
            }
            templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

        } else {
            printWriteTemplateError(player, capsuleStructureId);
        }

        return template;

    }

    public static boolean undeployBlueprint(ServerWorld worldserver, String playerID, ItemStack blueprintItemStack, BlockPos startPos, int size, List<Block> excluded) {

        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        if (minecraftserver == null) return false;

        Pair<CapsuleTemplateManager, CapsuleTemplate> blueprint = StructureSaver.getTemplate(blueprintItemStack, worldserver);
        CapsuleTemplate blueprintTemplate = blueprint.getRight();
        if (blueprintTemplate == null) return false;

        CapsuleTemplate tempTemplate = new CapsuleTemplate();
        Map<BlockPos, Block> occupiedPositions = blueprintTemplate.occupiedPositions;
        Map<BlockPos, Block> legacyItemOccupied = CapsuleItem.getOccupiedSourcePos(blueprintItemStack);
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = tempTemplate.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, null);
        List<Template.BlockInfo> worldBlocks = tempTemplate.blocks.stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());
        List<Template.BlockInfo> blueprintBLocks = blueprintTemplate.blocks.stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());

        PlayerEntity player = null;
        if (playerID != null) {
            player = worldserver.getPlayerEntityByName(playerID);
        }
        // compare the 2 lists, assume they are sorted the same since the same script is used to build them.
        if (blueprintBLocks.size() != worldBlocks.size())
            return false;

        List<String> tempTemplateSorted = worldBlocks.stream()
                .map(StructureSaver::serializeComparable)
                .sorted()
                .collect(Collectors.toList());
        List<String> blueprintTemplateSorted = blueprintBLocks.stream()
                .map(StructureSaver::serializeComparable)
                .sorted()
                .collect(Collectors.toList());
        boolean blueprintMatch = IntStream.range(0, tempTemplateSorted.size())
                .allMatch(i -> tempTemplateSorted.get(i).equals(blueprintTemplateSorted.get(i)));

        blueprintMatch = blueprintMatch && worldBlocks.stream().allMatch(b -> b.tileentityData == null || !b.tileentityData.contains("Items") || b.tileentityData.getTagList("Items", Constants.NBT.TAG_COMPOUND).hasNoTags());

        if (blueprintMatch) {
            blueprintTemplate.removeOccupiedPositions();
            String capsuleStructureId = CapsuleItem.getStructureName(blueprintItemStack);
            boolean written = blueprint.getLeft().writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
            if (written) {
                List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
                // check if some remove failed, it should never happen but keep it in case to prevent exploits
                if (couldNotBeRemoved != null) {
                    return false;
                }
            } else {
                printWriteTemplateError(player, capsuleStructureId);
            }
        }

        return blueprintMatch;
    }

    public static String serializeComparable(Template.BlockInfo b) {
        return b.blockState.getBlock().getUnlocalizedName()
                + "@"
                + b.blockState.getBlock().damageDropped(b.blockState)
                + (b.tileentityData == null ? "" : nbtStringNotEmpty(filterIdentityNBT(b)));
    }

    public static CompoundNBT filterIdentityNBT(Template.BlockInfo b) {
        CompoundNBT nbt = b.tileentityData.copy();
        List<String> converted = Config.getBlueprintIdentityNBT(b.blockState.getBlock());
        nbt.getKeySet().removeIf(key -> converted == null || !converted.contains(key));
        return nbt;
    }

    public static String nbtStringNotEmpty(CompoundNBT nbt) {
        if (nbt.hasNoTags()) return "";
        return nbt.toString();
    }

    public static boolean isFlowingLiquid(Template.BlockInfo b) {
        return b.blockState.getBlock() instanceof BlockLiquid && b.blockState.getValue(BlockLiquid.LEVEL) != 0;
    }


    @Nullable
    public static CapsuleTemplateManager getTemplateManager(ServerWorld worldserver) {
        if (worldserver == null) return null;
        File directory = worldserver.getSaveHandler().getWorldDirectory();
        String directoryPath = directory.getPath();

        if (!CapsulesManagers.containsKey(directoryPath)) {
            File capsuleDir = new File(directory, "structures/capsule");
            capsuleDir.mkdirs();
            CapsulesManagers.put(directoryPath, new CapsuleTemplateManager(capsuleDir.toString(), net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer()));
        }
        return CapsulesManagers.get(directoryPath);
    }

    /**
     * Use with caution, delete the blocks at the indicated positions.
     *
     * @return list of blocks that could not be removed
     */
    public static List<BlockPos> removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, ServerWorld
            world, PlayerEntity player) {

        List<BlockPos> couldNotBeRemoved = null;

        // disable tileDrop during the operation so that broken block are not
        // itemized on the ground.
        boolean flagdoTileDrops = world.getGameRules().getBoolean("doTileDrops");
        world.getGameRules().setOrCreateGameRule("doTileDrops", "false");
        world.restoringBlockSnapshots = true;

        // delete everything that as been saved in the capsule

        for (BlockPos pos : transferedPositions) {
            IBlockState b = world.getBlockState(pos);
            try {
                // uses same mechanic for TileEntity than net.minecraft.world.gen.structure.template.Template
                if (playerCanRemove(world, pos, player)) {
                    world.setBlockToAir(pos);
                } else {
                    if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                    couldNotBeRemoved.add(pos);
                }
            } catch (Exception e) {
                printDeployError(player, e, "Block crashed during Capsule capture phase : couldn't be removed. Will be ignored.");
                try {
                    world.setBlockState(pos, b);
                } catch (Exception ignored) {
                }
                if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                couldNotBeRemoved.add(pos);
            }
        }

        // revert rule to previous value even in case of crash
        world.restoringBlockSnapshots = false;
        world.getGameRules().setOrCreateGameRule("doTileDrops", String.valueOf(flagdoTileDrops));

        return couldNotBeRemoved;
    }


    public static boolean deploy(ItemStack capsule, ServerWorld playerWorld, String thrower, BlockPos
            dest, List<Block> overridableBlocks, List<String> outEntityBlocking, PlacementSettings placementsettings) {

        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
        CapsuleTemplate template = templatepair.getRight();

        if (template == null) return false;

        PlayerEntity player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerEntityByName(thrower);
        }

        Map<BlockPos, Block> outOccupiedSpawnPositions = new HashMap<>();
        int size = CapsuleItem.getSize(capsule);
        // check if the destination is valid : no unoverwritable block and no entities in the way.
        boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);
        if (!destValid) {
            printDeployFailure(outEntityBlocking, player);
            return false;
        }

        // check if the player can place a block
        if (player != null && !playerCanPlace(playerWorld, dest, template, player, placementsettings)) {
            player.sendMessage(new TextComponentTranslation("capsule.error.notAllowed"));
            return false;
        }

        final Map<BlockPos, Block> occupiedPositions = outOccupiedSpawnPositions;
        List<BlockPos> spawnedBlocks = new ArrayList<>();
        List<Entity> spawnedEntities = new ArrayList<>();

        CapsuleTemplateManager templateManager = templatepair.getLeft();
        String capsuleStructureId = CapsuleItem.getStructureName(capsule);
        template.saveOccupiedPositions(occupiedPositions);
        if (!templateManager.writeTemplate(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleStructureId))) {
            printWriteTemplateError(player, capsuleStructureId);
            return false;
        }

        try {
            template.spawnBlocksAndEntities(playerWorld, dest, placementsettings, occupiedPositions, overridableBlocks, spawnedBlocks, spawnedEntities);
            placePlayerOnTop(playerWorld, dest, size);

            return true;
        } catch (Exception err) {
            printDeployError(player, err, "Couldn't deploy the capsule");

            // rollback
            removeTransferedBlockFromWorld(spawnedBlocks, playerWorld, player);
            template.removeOccupiedPositions();
            if (!templateManager.writeTemplate(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleStructureId))) {
                printWriteTemplateError(player, capsuleStructureId);
            }
            for (Entity e : spawnedEntities) {
                e.setDropItemsWhenDead(false);
                e.setDead();
            }
            return false;
        }
    }

    public static void placePlayerOnTop(ServerWorld playerWorld, BlockPos dest, int size) {
        // Players don't block deployment, instead they are pushed up if they would suffocate
        List<EntityLivingBase> players = playerWorld.getEntitiesWithinAABB(
                EntityLivingBase.class,
                new AxisAlignedBB(dest.getX(), dest.getY(), dest.getZ(), dest.getX() + size, dest.getY() + size, dest.getZ() + size),
                entity -> (entity instanceof PlayerEntity)
        );
        for (EntityLivingBase p : players) {
            for (int y = 0; y < size; y++) {
                if (playerWorld.collidesWithAnyBlock(p.getEntityBoundingBox())) {
                    p.setPositionAndUpdate(p.posX, p.posY + 1, p.posZ);
                }
            }
        }
    }

    public static void printDeployFailure(List<String> outEntityBlocking, PlayerEntity player) {
        // send a chat message to explain failure
        if (player != null) {
            if (outEntityBlocking.size() > 0) {
                player.sendMessage(
                        new TextComponentTranslation("capsule.error.cantMergeWithDestinationEntity",
                                StringUtils.join(outEntityBlocking, ", ")));
            } else {
                player.sendMessage(new TextComponentTranslation("capsule.error.cantMergeWithDestination"));
            }
        }
    }

    public static void printDeployError(PlayerEntity player, Exception err, String s) {
        LOGGER.error(s, err);
        if (player != null) {
            player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
        }
    }

    public static void printWriteTemplateError(PlayerEntity player, String capsuleStructureId) {
        LOGGER.error("Couldn't write template " + capsuleStructureId);
        if (player != null) {
            player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
        }
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanPlace(ServerWorld worldserver, BlockPos dest, CapsuleTemplate
            template, PlayerEntity player, PlacementSettings placementsettings) {
        if (player != null) {
            List<BlockPos> expectedOut = template.calculateDeployPositions(worldserver, dest, placementsettings);
            for (BlockPos blockPos : expectedOut) {
                if (blockPos.getY() >= worldserver.getHeight() || !isPlaceEventAllowed(worldserver, blockPos, player))
                    return false;
            }
        }
        return true;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanRemove(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        if (player != null) {
            return isPlaceEventAllowed(worldserver, blockPos, player);
        }
        return true;
    }

    private static boolean isPlaceEventAllowed(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        BlockSnapshot blocksnapshot = new BlockSnapshot(worldserver, blockPos, Blocks.AIR.getDefaultState());
        BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(blocksnapshot, Blocks.DIRT.getDefaultState(), player, Hand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplate(ItemStack capsule, ServerWorld
            playerWorld) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> template = null;

        boolean isReward = CapsuleItem.isReward(capsule);
        String structureName = CapsuleItem.getStructureName(capsule);
        if (isReward || structureName.startsWith("config/") && CapsuleItem.isBlueprint(capsule)) {
            template = getTemplateForReward(playerWorld.getMinecraftServer(), structureName);
        } else {
            template = getTemplateForCapsule(playerWorld, structureName);
        }
        return template;
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForCapsule(ServerWorld
                                                                                              playerWorld, String structureName) {
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structureName))
            return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
        return Pair.of(templatemanager, template);
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForReward(MinecraftServer server, String
            structurePath) {
        CapsuleTemplateManager templatemanager = getRewardManager(server);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(server, new ResourceLocation(structurePath));
        return Pair.of(templatemanager, template);
    }

    /**
     * Check whether a merge can be done at the destination
     *
     * @param outOccupiedPositions Output param, the positions occupied a destination that will
     *                             have to be ignored on
     * @return List<BlockPos> occupied but not blocking positions
     */
    public static boolean isDestinationValid(CapsuleTemplate template, PlacementSettings placementIn, ServerWorld
            destWorld, BlockPos destOriginPos, int size,
                                             List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

        IBlockState air = Blocks.AIR.getDefaultState();

        List<Template.BlockInfo> srcblocks = template.blocks;

        Map<BlockPos, Template.BlockInfo> blockInfoByPosition = new HashMap<>();
        for (Template.BlockInfo template$blockinfo : srcblocks) {
            BlockPos blockpos = CapsuleTemplate.transformedBlockPos(placementIn, template$blockinfo.pos).add(destOriginPos).add(CapsuleTemplate.recenterRotation((size - 1) / 2, placementIn));
            blockInfoByPosition.put(blockpos, template$blockinfo);
        }

        // check the destination is ok for every block of the template
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {

                    BlockPos destPos = destOriginPos.add(x, y, z);
                    Template.BlockInfo srcInfo = blockInfoByPosition.get(destPos);
                    IBlockState templateBlockState = air;
                    if (srcInfo != null) {
                        templateBlockState = srcInfo.blockState;
                    }

                    if (!destWorld.isBlockLoaded(destPos)) return false;
                    IBlockState worldDestState = destWorld.getBlockState(destPos);

                    boolean worldDestOccupied = (worldDestState != air && !overridable.contains(worldDestState.getBlock()));
                    if (worldDestState != air && outOccupiedPositions != null) {
                        outOccupiedPositions.put(destPos, worldDestState.getBlock());
                    }

                    boolean srcOccupied = (templateBlockState != air && !overridable.contains(templateBlockState.getBlock()));

                    List<EntityLivingBase> entities = destWorld.getEntitiesWithinAABB(
                            EntityLivingBase.class,
                            new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() + 1, destPos.getY() + 1, destPos.getZ() + 1),
                            entity -> !(entity instanceof PlayerEntity)
                    );


                    // if destination is occupied, and source is neither
                    // excluded from transportation, nor can't be overriden by
                    // destination, then the merge can't be done.
                    if ((entities.size() > 0 && srcOccupied) || (worldDestOccupied && !overridable.contains(templateBlockState.getBlock()))) {
                        if (entities.size() > 0 && outEntityBlocking != null) {
                            for (Object e : entities) {
                                Entity entity = (Entity) e;
                                if (entity != null) {
                                    outEntityBlocking.add(entity.getName());
                                }
                            }

                        }
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getUniqueName(ServerWorld playerWorld, String player) {
        CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
        String capsuleID = "C-" + player + "-" + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "CExcetion-" + player + "-" + csd.getNextCount();
        }
        while (templatemanager.get(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
            capsuleID = "C-" + player + "-" + csd.getNextCount();
        }

        return capsuleID;
    }

    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getBlueprintUniqueName(ServerWorld world) {
        CapsuleSavedData csd = getCapsuleSavedData(world);
        String capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(world);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "BException-" + csd.getNextCount();
        }
        while (templatemanager.get(world.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
            capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        }

        return capsuleID;
    }

    public static boolean copyFromCapsuleTemplate(ItemStack capsule, String destinationStructureName, CapsuleTemplateManager
            destManager, ServerWorld worldServer, boolean onlyBlocks, List<String> outExcluded) {
        CompoundNBT srcTemplateData = getTemplateNBTData(capsule, worldServer);
        if (srcTemplateData == null) return false; // capsule template not found
        return duplicateTemplate(srcTemplateData, destinationStructureName, destManager, worldServer.getMinecraftServer(), onlyBlocks, outExcluded);

    }

    public static boolean duplicateTemplate(CompoundNBT templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server) {
        return duplicateTemplate(templateData, destinationStructureName, destManager, server, false, null);
    }

    public static boolean duplicateTemplate(CompoundNBT templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server, boolean onlyWhitelisted, List<String> outExcluded) {
        // create a destination template
        ResourceLocation destinationLocation = new ResourceLocation(destinationStructureName);
        CapsuleTemplate destTemplate = destManager.getTemplate(server, destinationLocation);
        // populate template from source data
        destTemplate.read(templateData);
        // empty occupied position, it makes no sense for a new template to copy those situational data
        destTemplate.occupiedPositions = null;
        // remove all tile entities
        if (onlyWhitelisted) {
            List<Template.BlockInfo> newBlockList = destTemplate.blocks.stream()
                    .filter(b -> {
                        ResourceLocation registryName = b.blockState.getBlock().getRegistryName();
                        boolean included = b.tileentityData == null
                                || registryName != null && Config.blueprintWhitelist.keySet().contains(registryName.toString());
                        if (!included && outExcluded != null) outExcluded.add(b.blockState.toString());
                        return included;
                    })
                    .map(b -> {
                        if (b.tileentityData == null) return b;
                        // remove all unlisted nbt data to prevent dupe or cheating
                        CompoundNBT nbt = null;
                        JsonObject allowedNBT = Config.getBlueprintAllowedNBT(b.blockState.getBlock());
                        if (allowedNBT != null) {
                            nbt = b.tileentityData.copy();
                            nbt.getKeySet().removeIf(key -> !allowedNBT.has(key));
                        } else {
                            nbt = new CompoundNBT();
                        }
                        return new Template.BlockInfo(
                                b.pos,
                                b.blockState,
                                nbt
                        );
                    }).collect(Collectors.toList());
            destTemplate.blocks.clear();
            destTemplate.blocks.addAll(newBlockList);
            // remove all entities
            destTemplate.entities.clear();
        }
        // write the new template
        return destManager.writeTemplate(server, destinationLocation);
    }

    /**
     * Extract the template NBTData from a capsule
     */
    public static CompoundNBT getTemplateNBTData(ItemStack capsule, ServerWorld worldServer) {
        return getTemplateNBTData(StructureSaver.getTemplate(capsule, worldServer).getRight());
    }

    public static CompoundNBT getTemplateNBTData(String path, ServerWorld worldServer) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> sourcetemplatepair;
        if (path.startsWith(Config.rewardTemplatesPath) || path.startsWith("config/")) {
            sourcetemplatepair = StructureSaver.getTemplateForReward(worldServer.getMinecraftServer(), path);
        } else {
            sourcetemplatepair = StructureSaver.getTemplateForCapsule(worldServer, path);
        }
        return getTemplateNBTData(sourcetemplatepair.getRight());
    }

    public static CompoundNBT getTemplateNBTData(CapsuleTemplate template) {
        if (template == null) return null;
        CompoundNBT data = new CompoundNBT();
        template.writeToNBT(data);
        return data;
    }

    /**
     * Get the Capsule saving tool that remembers last capsule id.
     */
    public static CapsuleSavedData getCapsuleSavedData(ServerWorld capsuleWorld) {
        CapsuleSavedData capsuleSavedData = (CapsuleSavedData) capsuleWorld.loadData(CapsuleSavedData.class, "capsuleData");
        if (capsuleSavedData == null) {
            capsuleSavedData = new CapsuleSavedData("capsuleData");
            capsuleWorld.setData("capsuleData", capsuleSavedData);
            capsuleSavedData.setDirty(true);
        }
        return capsuleSavedData;
    }

    @Nullable
    public static String createBlueprintTemplate(String srcStructurePath, ItemStack destCapsule, ServerWorld worldServer, PlayerEntity
            playerIn) {
        if (worldServer == null) {
            LOGGER.error("worldServer is null");
            return null;
        }

        String destStructureName = getBlueprintUniqueName(worldServer) + "-" + srcStructurePath.replace("/", "_");

        CapsuleTemplateManager templateManager = getTemplateManager(worldServer);
        outExcluded.clear();
        boolean created = templateManager != null && duplicateTemplate(
                getTemplateNBTData(srcStructurePath, worldServer),
                destStructureName,
                templateManager,
                worldServer.getMinecraftServer(),
                true,
                outExcluded
        );

        // try to cleanup previous template to save disk space on the long run
        if (destCapsule.getTag() != null && destCapsule.getTag().contains("prevStructureName")) {
            if (templateManager != null)
                templateManager.deleteTemplate(worldServer.getMinecraftServer(), new ResourceLocation(destCapsule.getTag().getString("prevStructureName")));
        }

        if (!created && playerIn != null) {
            playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintCreationError"));
        }
        if (outExcluded.size() > 0 && playerIn != null) {
            playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintExcluded", "\n* " + String.join("\n* ", outExcluded)));
        }
        return destStructureName;
    }

    public static class ItemStackKey implements Comparable<ItemStackKey> {
        public ItemStack itemStack;

        public ItemStackKey(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        public boolean equals(Object someOther) {
            if (!(someOther instanceof ItemStackKey)) return false;
            final ItemStack otherStack = ((ItemStackKey) someOther).itemStack;
            return otherStack.isItemEqual(this.itemStack) && (!otherStack.hasTag() && !this.itemStack.hasTag() || otherStack.getTag().equals(this.itemStack.getTag()));
        }

        public int hashCode() {
            int val = itemStack.getItem().hashCode() * 29 + itemStack.getDamage();
            return val;
        }


        @Override
        public int compareTo(ItemStackKey o) {
            if (o == null || o.itemStack == null) return 1;
            return this.equals(o) ? 0 : serializeItemStack(this.itemStack).compareTo(serializeItemStack(o.itemStack));
        }

        public static String serializeItemStack(ItemStack itemstack) {
            return itemstack.getItem().getUnlocalizedName()
                    + "@"
                    + itemstack.getDamage();
        }
    }

}
