package capsule;

import capsule.items.CapsuleItem;
import capsule.loot.LootPathData;
import capsule.structure.CapsulePlacementSettings;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.common.base.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class StructureSaver {

    protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);
    public static Map<WorldServer, CapsuleTemplateManager> CapsulesManagers = new HashMap<>();
    private static CapsuleTemplateManager RewardManager = null;

    public static void loadLootList(MinecraftServer server) {
        // Init the manager for reward Lists
        for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
            String path = Config.lootTemplatesPaths[i];
            LootPathData data = Config.lootTemplatesData.get(path);

            File templateFolder = new File(server.getDataDirectory(), path);

            if (path.startsWith("config/") && !templateFolder.exists()) {
                templateFolder.mkdirs();
            }

            if (templateFolder.exists() && templateFolder.isDirectory()) {
                File[] fileList = templateFolder.listFiles((p_accept_1_, p_accept_2_) -> p_accept_2_.endsWith(".nbt"));
                data.files = new ArrayList<>();
                if (fileList != null) {
                    for (File templateFile : fileList) {
                        if (templateFile.isFile() && templateFile.getName().endsWith(".nbt"))
                            data.files.add(templateFile.getName().replaceAll(".nbt", ""));
                    }
                }
            } else {

                // another try reading from jar files
                try {
                    LOGGER.debug("Listing files at " + "/" + path);

                    String[] fileNames = getResourceListing(StructureSaver.class, path);

                    data.files = new ArrayList<>();
                    LOGGER.debug("Found " + fileNames.length + " files.");
                    for (String file : fileNames) {
                        LOGGER.debug("Found " + file);
                        if (file.endsWith(".nbt"))
                            data.files.add(file.replaceAll(".nbt", ""));
                    }

                } catch (Exception e) {
                    LOGGER.error("Error while listing files in the jar", e);
                }

            }
        }
    }


    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path  Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     * @author Greg Briggs
     */
    public static String[] getResourceListing(Class<?> clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory. Have
			 * to assume the same jar as clazz.
			 */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        else if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

            LOGGER.debug("Listing files in " + jarPath);

            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.replace(path + "/", "");
                    result.add(entry);
                }
            }
            jar.close();
            return result.toArray(new String[result.size()]);

        } else {

            InputStream inputstream = clazz.getResourceAsStream("/" + path);
            if (inputstream != null) {
                final InputStreamReader isr = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr);

                Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
                String filename = null;
                while ((filename = br.readLine()) != null) {
                    result.add(filename);
                }
                return result.toArray(new String[result.size()]);
            }

        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    public static CapsuleTemplateManager getRewardManager(MinecraftServer server) {
        if (RewardManager == null) {
            RewardManager = new CapsuleTemplateManager(server.getDataDirectory().getPath());
            File rewardDir = new File(Config.rewardTemplatesPath);
            if (!rewardDir.exists()) {
                rewardDir.mkdirs();
            }
        }
        return RewardManager;
    }

    public static boolean store(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                Map<BlockPos, Block> excludedPositions) {

        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        List<Entity> outCapturedEntities = new ArrayList<>();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
        List<BlockPos> transferedPositions = template.takeBlocksFromWorldIntoCapsule(worldserver, startPos, new BlockPos(size, size, size), excludedPositions,
                excluded, outCapturedEntities);
        EntityPlayer player = null;
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
                templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
            }
        } else {
            if (player != null) {
                player.addChatMessage(new TextComponentTranslation("capsule.error.technicalError"));
            }
        }

        return writingOK;

    }

    public static CapsuleTemplateManager getTemplateManager(WorldServer worldserver) {
        if (worldserver == null) return null;

        if (!CapsulesManagers.containsKey(worldserver)) {
            File capsuleDir = new File(worldserver.getSaveHandler().getWorldDirectory(), "structures/capsules");
            capsuleDir.mkdirs();
            CapsulesManagers.put(worldserver, new CapsuleTemplateManager(capsuleDir.toString()));
        }
        return CapsulesManagers.get(worldserver);
    }

    /**
     * Use with caution, delete the blocks at the indicated positions.
     *
     * @param transferedPositions
     * @param world
     * @return list of blocks that could not be removed
     */
    public static List<BlockPos> removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, WorldServer world, EntityPlayer player) {

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
                LOGGER.error("Block crashed during Capsule capture phase : couldn't be removed. Will be ignored.", e);
                if (player != null) {
                    player.addChatMessage(new TextComponentTranslation("capsule.error.technicalError"));
                }
                try {
                    world.setBlockState(pos, b);
                } catch (Exception ignored) {}
                if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                couldNotBeRemoved.add(pos);
            }
        }

        // revert rule to previous value even in case of crash
        world.restoringBlockSnapshots = false;
        world.getGameRules().setOrCreateGameRule("doTileDrops", String.valueOf(flagdoTileDrops));

        return couldNotBeRemoved;
    }


    public static boolean clearTemplate(WorldServer worldserver, String capsuleStructureId) {
        MinecraftServer minecraftserver = worldserver.getMinecraftServer();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

        List<Template.BlockInfo> blocks = template.blocks;
        List<Template.EntityInfo> entities = template.entities;

        blocks.clear();
        entities.clear();

        return templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

    }

    public static boolean deploy(ItemStack capsule, WorldServer playerWorld, String thrower, BlockPos dest, List<Block> overridableBlocks,
                                 Map<BlockPos, Block> outOccupiedSpawnPositions, List<String> outEntityBlocking) {


        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
        CapsuleTemplate template = templatepair.getRight();

        EntityPlayer player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerEntityByName(thrower);
        }

        if (template != null) {
            int size = CapsuleItem.getSize(capsule);
            // check if the destination is valid : no unoverwritable block and no entities in the way.
            CapsulePlacementSettings placementsettings = (new CapsulePlacementSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false).setChunk(null).setReplacedBlock(null).setIgnoreStructureBlock(false);
            boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);

            if (destValid) {
                List<BlockPos> spawnedBlocks = new ArrayList<>();
                List<Entity> spawnedEntities = new ArrayList<>();
                try {
                    // check if the player can place a block
                    if (player != null && !playerCanPlace(playerWorld, dest, template, player, placementsettings)){
                        player.addChatMessage(new TextComponentTranslation("capsule.error.notAllowed"));
                        return false;
                    }

                    template.spawnBlocksAndEntities(playerWorld, dest, placementsettings, spawnedBlocks, spawnedEntities);
                    return true;
                } catch (Exception err) {
                    LOGGER.error("Couldn't deploy the capsule", err);
                    if (player != null) {
                        player.addChatMessage(new TextComponentTranslation("capsule.error.technicalError"));
                    }

                    // rollback
                    removeTransferedBlockFromWorld(spawnedBlocks, playerWorld, player);
                    for (Entity e : spawnedEntities) {
                        e.setDropItemsWhenDead(false);
                        e.setDead();
                    }
                }
            } else {
                // send a chat message to explain failure
                if (player != null) {
                    if (outOccupiedSpawnPositions.size() == 0) {
                        player.addChatMessage(
                                new TextComponentTranslation("capsule.error.cantMergeWithDestinationEntity",
                                        StringUtils.join(outEntityBlocking, ", ")));
                    } else {
                        player.addChatMessage(new TextComponentTranslation("capsule.error.cantMergeWithDestination"));
                    }
                }
            }
        } else {
            // send a chat message to explain failure
            if (player != null) {
                player.addChatMessage(new TextComponentTranslation("capsule.error.capsuleContentNotFound", CapsuleItem.getStructureName(capsule)));
            }
        }


        return false;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     * @return
     */
    private static boolean playerCanPlace(WorldServer worldserver, BlockPos dest, CapsuleTemplate template, EntityPlayer player, CapsulePlacementSettings placementsettings) {
        if (player != null) {
            List<BlockPos> expectedOut = template.calculateDeployPositions(worldserver, dest, placementsettings);
            for (BlockPos blockPos : expectedOut) {
                BlockSnapshot blocksnapshot = new BlockSnapshot(worldserver, blockPos, Blocks.AIR.getDefaultState());
                BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(blocksnapshot, Blocks.DIRT.getDefaultState(), player, EnumHand.MAIN_HAND);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanRemove(WorldServer worldserver, BlockPos blockPos, EntityPlayer player) {
        if (player != null) {
            BlockSnapshot blocksnapshot = new BlockSnapshot(worldserver, blockPos, Blocks.AIR.getDefaultState());
            BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(blocksnapshot, Blocks.DIRT.getDefaultState(), player, EnumHand.MAIN_HAND);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        return true;
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplate(ItemStack capsule, WorldServer playerWorld) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> template = null;

        boolean isReward = CapsuleItem.isReward(capsule);
        String structureName = CapsuleItem.getStructureName(capsule);
        if (isReward) {
            template = getTemplateForReward(playerWorld.getMinecraftServer(), structureName);
        } else {
            template = getTemplateForCapsule(playerWorld, structureName);
        }
        return template;
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForCapsule(WorldServer playerWorld, String structureName) {
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null || Strings.isNullOrEmpty(structureName)) return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
        return Pair.of(templatemanager, template);
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForReward(MinecraftServer server, String structurePath) {
        CapsuleTemplateManager templatemanager = getRewardManager(server);
        if (templatemanager == null || Strings.isNullOrEmpty(structurePath)) return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(server, new ResourceLocation(structurePath));
        return Pair.of(templatemanager, template);
    }

    /**
     * Check whether a merge can be done at the destination
     *
     * @param template
     * @param destWorld
     * @param destOriginPos
     * @param size
     * @param overridable
     * @param outOccupiedPositions Output param, the positions occupied a destination that will
     *                             have to be ignored on
     * @return List<BlockPos> occupied but not blocking positions
     */
    public static boolean isDestinationValid(CapsuleTemplate template, CapsulePlacementSettings placementIn, WorldServer destWorld, BlockPos destOriginPos, int size,
                                             List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

        IBlockState air = Blocks.AIR.getDefaultState();

        List<Template.BlockInfo> srcblocks = template.blocks;
        if (srcblocks == null) return false;

        Map<BlockPos, Template.BlockInfo> blockInfoByPosition = new HashMap<>();
        for (Template.BlockInfo template$blockinfo : srcblocks) {
            BlockPos blockpos = CapsuleTemplate.transformedBlockPos(placementIn, template$blockinfo.pos);
            blockInfoByPosition.put(blockpos, template$blockinfo);
        }

        // check the destination is ok for every block of the template
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {

                    BlockPos srcPos = new BlockPos(x, y, z);
                    Template.BlockInfo srcInfo = blockInfoByPosition.get(srcPos);
                    IBlockState srcState = air;
                    if (srcInfo != null) {
                        srcState = srcInfo.blockState;
                    }

                    BlockPos destPos = destOriginPos.add(x, y, z);
                    IBlockState destState = destWorld.getBlockState(destPos);

                    boolean destOccupied = (destState != air && !overridable.contains(destState.getBlock()));
                    if (destState != air && outOccupiedPositions != null) {
                        outOccupiedPositions.put(destPos, destState.getBlock());
                    }

                    boolean srcOccupied = (srcState != air && !overridable.contains(srcState.getBlock()));
                    @SuppressWarnings("rawtypes")
                    List entities = destWorld.getEntitiesWithinAABB(
                            EntityLivingBase.class,
                            new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() + 1, destPos.getY() + 1, destPos.getZ() + 1)
                    );

                    // if destination is occupied, and source is neither
                    // excluded from transportation, nor can't be overriden by
                    // destination, then the merge can't be done.
                    if ((entities.size() > 0 && srcOccupied) || (destOccupied && !overridable.contains(srcState.getBlock()))) {
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
     *
     * @param playerWorld
     * @param player
     * @return
     */
    public static String getUniqueName(WorldServer playerWorld, String player) {
        CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
        String capsuleID = "C-" + player + "-" + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);

        while (templatemanager.get(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
            capsuleID = "C-" + player + "-" + csd.getNextCount();
        }

        return capsuleID;
    }

    /**
     * Get the Capsule saving tool that remembers last capsule id.
     *
     * @param capsuleWorld
     * @return
     */
    public static CapsuleSavedData getCapsuleSavedData(WorldServer capsuleWorld) {
        CapsuleSavedData capsuleSavedData = (CapsuleSavedData) capsuleWorld.loadItemData(CapsuleSavedData.class, "capsuleData");
        if (capsuleSavedData == null) {
            capsuleSavedData = new CapsuleSavedData("capsuleData");
            capsuleWorld.setItemData("capsuleData", capsuleSavedData);
            capsuleSavedData.setDirty(true);
        }
        return capsuleSavedData;
    }


}
