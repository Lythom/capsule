package capsule;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

import capsule.items.CapsuleItem;
import capsule.loot.LootPathData;
import capsule.structure.CapsulePlacementSettings;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;

public class StructureSaver {

	protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);

	private static CapsuleTemplateManager RewardManager = null;	
	public static Map<WorldServer,CapsuleTemplateManager> CapsulesManagers = new HashMap<>();

	
	public static void loadLootList(MinecraftServer server){
		// Init the manager for reward Lists
		for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
			String path = Config.lootTemplatesPaths[i];
			LootPathData data = Config.lootTemplatesData.get(path);

			File templateFolder = new File(server.getDataDirectory(), path);
			if(templateFolder.exists() && templateFolder.isDirectory()){
				File[] fileList = templateFolder.listFiles(new FilenameFilter()
		        {
		            public boolean accept(File p_accept_1_, String p_accept_2_)
		            {
		                return p_accept_2_.endsWith(".nbt");
		            }
		        });
				data.files = new ArrayList<String>();
				for (File templateFile : fileList) {
					if(templateFile.isFile() && templateFile.getName().endsWith(".nbt"))
						data.files.add(templateFile.getName().replaceAll(".nbt", ""));
				}
			}
		}
	}

	public static CapsuleTemplateManager getRewardManager(MinecraftServer server) {
		if(RewardManager == null){
			RewardManager = new CapsuleTemplateManager(server.getDataDirectory().getPath());
			File rewardDir = new File(Config.rewardTemplatesPath);
			if(!rewardDir.exists()){
				rewardDir.mkdirs();
			}
		}
		return RewardManager;
	}

	public static boolean store(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos position, int size, List<Block> excluded,
			Map<BlockPos, Block> excludedPositions, boolean keepSource) {

		MinecraftServer minecraftserver = worldserver.getMinecraftServer();
		List<Entity> outCapturedEntities = new ArrayList<>();
		
		CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
		CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		List<BlockPos> transferedPositions = template.takeBlocksFromWorldIntoCapsule(worldserver, position, new BlockPos(size, size, size), excludedPositions,
				excluded, outCapturedEntities);
		template.setAuthor(playerID);
		boolean writingOK = templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		if (writingOK && !keepSource) {
			removeTransferedBlockFromWorld(transferedPositions, worldserver);
			for(Entity e : outCapturedEntities){
				e.setDropItemsWhenDead(false);
				e.setDead();
			}
		}

		return writingOK;

	}

	public static CapsuleTemplateManager getTemplateManager(WorldServer worldserver) {
		if(worldserver == null || worldserver.getSaveHandler() == null || worldserver.getSaveHandler().getWorldDirectory() == null) return null;

		if(!CapsulesManagers.containsKey(worldserver)){
			File capsuleDir = new File(worldserver.getSaveHandler().getWorldDirectory(), "structures/capsules");
			capsuleDir.mkdirs();
			CapsulesManagers.put(worldserver, new CapsuleTemplateManager(capsuleDir.toString()));
		}
		return CapsulesManagers.get(worldserver);
	}
	
	/**
	 * Use with caution, delete the blocks at the indicated positions.
	 * @param transferedPositions
	 * @param world
	 */
	public static void removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, WorldServer world) {

		// disable tileDrop during the operation so that broken block are not
		// itemized on the ground.
		boolean flagdoTileDrops = world.getGameRules().getBoolean("doTileDrops");
		world.getGameRules().setOrCreateGameRule("doTileDrops", "false");

		// delete everything that as been saved in the capsule
		for (BlockPos pos : transferedPositions) {
			world.removeTileEntity(pos);
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
			world.notifyNeighborsOfStateChange(pos, Blocks.AIR);
		}

		// revert rule to previous value
		world.getGameRules().setOrCreateGameRule("doTileDrops", String.valueOf(flagdoTileDrops));

	}
	

	public static boolean clearTemplate(WorldServer worldserver, String capsuleStructureId) {
		MinecraftServer minecraftserver = worldserver.getMinecraftServer();
		
		CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
		CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		
		List<Template.BlockInfo> blocks = template.blocks;
		List<Template.EntityInfo> entities = template.entities;
		if(entities == null || blocks == null) return false;
		
		blocks.clear();
		entities.clear();

		return templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		
	}
	
	public static boolean deploy(ItemStack capsule, WorldServer playerWorld, String thrower, BlockPos dest, List<Block> overridableBlocks,
			Map<BlockPos, Block> outOccupiedSpawnPositions, List<String> outEntityBlocking) {

		Pair<CapsuleTemplateManager,CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
		CapsuleTemplate template = templatepair.getRight();

		if (template != null)
        {
			int size = CapsuleItem.getSize(capsule);
        	// check if the destination is valid : no unoverwritable block and no entities in the way.
        	CapsulePlacementSettings placementsettings = (new CapsulePlacementSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false).setChunk((ChunkPos)null).setReplacedBlock((Block)null).setIgnoreStructureBlock(false);
        	boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);
        	if(destValid){
        		template.spawnBlocksAndEntities(playerWorld, dest, placementsettings, outOccupiedSpawnPositions, overridableBlocks);
        		return true;
        	}
        }
		
		return false;
	}

	public static Pair<CapsuleTemplateManager,CapsuleTemplate> getTemplate(ItemStack capsule, WorldServer playerWorld) {
		Pair<CapsuleTemplateManager,CapsuleTemplate> template = null;
		
		boolean isReward = CapsuleItem.isReward(capsule);
		String structureName = CapsuleItem.getStructureName(capsule);
		if(isReward){
			template = getTemplateForReward(playerWorld.getMinecraftServer(), structureName);
		} else {
			template = getTemplateForCapsule(playerWorld, structureName);
		}
		return template;
	}

	public static Pair<CapsuleTemplateManager,CapsuleTemplate> getTemplateForCapsule(WorldServer playerWorld, String structureName) {
		CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
		if(templatemanager == null || Strings.isNullOrEmpty(structureName)) return Pair.of(null,null);
		
		CapsuleTemplate template = templatemanager.func_189942_b(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
		return Pair.of(templatemanager, template);
	}
	
	public static Pair<CapsuleTemplateManager,CapsuleTemplate> getTemplateForReward(MinecraftServer server, String structurePath) {
		CapsuleTemplateManager templatemanager = getRewardManager(server);
		if(templatemanager == null || Strings.isNullOrEmpty(structurePath)) return Pair.of(null,null);
		
		CapsuleTemplate template = templatemanager.func_189942_b(server, new ResourceLocation(structurePath));
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
	 * @param outOccupiedPositions
	 *            Output param, the positions occupied a destination that will
	 *            have to be ignored on
	 * @return List<BlockPos> occupied but not blocking positions
	 */
	public static boolean isDestinationValid(CapsuleTemplate template, CapsulePlacementSettings placementIn, WorldServer destWorld, BlockPos destOriginPos, int size,
			List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

		IBlockState air = Blocks.AIR.getDefaultState();
		
		List<Template.BlockInfo> srcblocks = template.blocks;
		if(srcblocks == null) return false;
		
		Map<BlockPos,Template.BlockInfo> blockInfoByPosition = new HashMap<>();
		for (Template.BlockInfo template$blockinfo : srcblocks)
        {
            BlockPos blockpos = CapsuleTemplate.transformedBlockPos(placementIn, template$blockinfo.pos);
            blockInfoByPosition.put(blockpos, template$blockinfo);
        }
		
		// check the destination is ok for every block of the template
		for (int y = size - 1; y >= 0; y--) {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {
					
					BlockPos srcPos = new BlockPos(x,y,z);
					Template.BlockInfo srcInfo = blockInfoByPosition.get(srcPos);
					IBlockState srcState = air;
					if(srcInfo != null){
						srcState = srcInfo.blockState;
					}
		
					BlockPos destPos = destOriginPos.add(x,y,z);
					IBlockState destState = destWorld.getBlockState(destPos);
		
					boolean destOccupied = (destState != air && !overridable.contains(destState.getBlock()));
					if (destState != air && outOccupiedPositions != null) {
						outOccupiedPositions.put(destPos, destState.getBlock());
					}
					
					boolean srcOccupied = (srcState != air && !overridable.contains(srcState.getBlock()));
					@SuppressWarnings("rawtypes")
					List entities = destWorld.getEntitiesWithinAABB(
							EntityLivingBase.class,
							new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() +1, destPos.getY()+1, destPos.getZ()+1)
					);
		
					// if destination is occupied, and source is neither
					// excluded from transportation, nor can't be overriden by
					// destination, then the merge can't be done.
					if ((entities.size() > 0 && srcOccupied) || (destOccupied && !overridable.contains(srcState.getBlock()))) {
						if(entities.size() > 0 && outEntityBlocking != null){
							for(Object e : entities){
								Entity entity = (Entity)e;
								if(entity != null){
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
	 * @param playerWorld
	 * @param player
	 * @return
	 */
	public static String getUniqueName(WorldServer playerWorld, String player) {
		CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
		String capsuleID = "C-" + player + "-" + csd.getNextCount();
		CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
		
		while(templatemanager.func_189942_b(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
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
