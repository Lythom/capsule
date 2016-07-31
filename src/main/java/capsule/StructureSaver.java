package capsule;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import capsule.loot.LootPathData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.BlockRotationProcessor;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class StructureSaver {

	protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);

	private static TemplateManager RewardManager = null;	
	public static Map<WorldServer,TemplateManager> CapsulesManagers = new HashMap<>();

	
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
				for (File templateFile : fileList) {
					data.files.add(templateFile.getName().replaceAll(".nbt", ""));
				}
			}
		}
	}

	public static TemplateManager getRewardManager(MinecraftServer server) {
		if(RewardManager == null){
			RewardManager = new TemplateManager(server.getDataDirectory().getPath());
		}
		return RewardManager;
	}

	public static boolean store(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos position, int size, List<Block> excluded,
			Map<BlockPos, Block> excludedPositions, boolean keepSource) {

		MinecraftServer minecraftserver = worldserver.getMinecraftServer();
		List<Entity> outCapturedEntities = new ArrayList<>();
		
		TemplateManager templatemanager = getTemplateManager(worldserver);
		Template template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		List<BlockPos> transferedPositions = takeBlocksFromWorldIntoCapsule(template, worldserver, position, new BlockPos(size, size, size), excludedPositions,
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

	public static TemplateManager getTemplateManager(WorldServer worldserver) {
		if(worldserver == null || worldserver.getSaveHandler() == null || worldserver.getSaveHandler().getWorldDirectory() == null) return null;

		if(!CapsulesManagers.containsKey(worldserver)){
			File capsuleDir = new File(worldserver.getSaveHandler().getWorldDirectory(), "structures/capsules");
			capsuleDir.mkdirs();
			CapsulesManagers.put(worldserver, new TemplateManager(capsuleDir.toString()));
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
		
		TemplateManager templatemanager = getTemplateManager(worldserver);
		Template template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		
		List<Template.BlockInfo> blocks = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "blocks");
		List<Template.EntityInfo> entities = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "entities");
		if(entities == null || blocks == null) return false;
		
		blocks.clear();
		entities.clear();

		return templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		
	}

	/**
	 * Reflexion id mappings : [0] private final java.util.List
	 * net.minecraft.world.gen.structure.template.Template.blocks [1] private
	 * final java.util.List
	 * net.minecraft.world.gen.structure.template.Template.entities [2] private
	 * net.minecraft.util.math.BlockPos
	 * net.minecraft.world.gen.structure.template.Template.size [3] private
	 * java.lang.String
	 * net.minecraft.world.gen.structure.template.Template.author
	 */

	/**
	 * Rewritten from Template.takeBlocksFromWorld
	 * takes blocks from the world and puts the data them into this template
	 */
	public static List<BlockPos> takeBlocksFromWorldIntoCapsule(Template template, World worldIn, BlockPos startPos, BlockPos endPos,
			Map<BlockPos, Block> sourceIgnorePos, List<Block> excluded, List<Entity> outCapturedEntities) {

		// As it can't be extended, hacking template to use it as a saving
		// mechanics with reflexion
		List<Template.BlockInfo> blocks = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "blocks");

		List<BlockPos> transferedBlocks = new ArrayList<BlockPos>();

		// rewritten vanilla code from Template.takeBlocksFromWorld

		if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1) {
			BlockPos blockpos = startPos.add(endPos).add(-1, -1, -1);
			List<Template.BlockInfo> list = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list1 = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list2 = Lists.<Template.BlockInfo> newArrayList();
			BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()),
					Math.min(startPos.getZ(), blockpos.getZ()));
			BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()),
					Math.max(startPos.getZ(), blockpos.getZ()));

			// template.size = endPos;
			ObfuscationReflectionHelper.setPrivateValue(Template.class, template, endPos, "size");

			for (BlockPos.MutableBlockPos blockpos$mutableblockpos : BlockPos.getAllInBoxMutable(blockpos1, blockpos2)) {
				BlockPos blockpos3 = blockpos$mutableblockpos.subtract(blockpos1);
				IBlockState iblockstate = worldIn.getBlockState(blockpos$mutableblockpos);
				Block iblock = iblockstate.getBlock();

				if (!excluded.contains(iblock) // excluded blocks are not
												// captured at all
						&& (sourceIgnorePos == null // exclude sourceBlock that
													// were already presents.
													// Capture if it was
													// changed.
								|| !(sourceIgnorePos.keySet().contains(blockpos$mutableblockpos)
										&& sourceIgnorePos.get(blockpos$mutableblockpos).equals(iblock)))) {
					TileEntity tileentity = worldIn.getTileEntity(blockpos$mutableblockpos);

					if (tileentity != null) {
						NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());
						nbttagcompound.removeTag("x");
						nbttagcompound.removeTag("y");
						nbttagcompound.removeTag("z");
						list1.add(new Template.BlockInfo(blockpos3, iblockstate, nbttagcompound));
					} else if (!iblockstate.isFullBlock() && !iblockstate.isFullCube()) {
						list2.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound) null));
					} else {
						list.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound) null));
					}
					transferedBlocks.add(new BlockPos(blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ())); // save
																					// a
																					// copy
				}
			}

			blocks.clear();
			blocks.addAll(list);
			blocks.addAll(list1);
			blocks.addAll(list2);

			List<Entity> capturedEntities = takeNonLivingEntitiesFromWorld(template, worldIn, blockpos1, blockpos2.add(1, 1, 1));
			if(outCapturedEntities != null && capturedEntities != null){
				outCapturedEntities.addAll(capturedEntities);
			}

			return transferedBlocks;
		}

		return null;
	}

	/**
	 * takes blocks from the world and puts the data them into this template
	 */
	public static List<Entity> takeNonLivingEntitiesFromWorld(Template template, World worldIn, BlockPos startPos, BlockPos endPos) {
		// Hacking template to access entities, since it cannont be extended
		List<Template.EntityInfo> entities = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "entities");
		if(entities == null) return null;
		
		// rewritten vanilla code from Template.takeEntitiesFromWorld
		List<Entity> list = worldIn.<Entity> getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), new Predicate<Entity>() {
			public boolean apply(@Nullable Entity p_apply_1_) {
				return !(p_apply_1_ instanceof EntityItem) && (!(p_apply_1_ instanceof EntityLivingBase) || (p_apply_1_ instanceof EntityArmorStand));
			}
		});
		entities.clear();

		for (Entity entity : list) {
			Vec3d vec3d = new Vec3d(entity.posX - (double) startPos.getX(), entity.posY - (double) startPos.getY(), entity.posZ - (double) startPos.getZ());
			NBTTagCompound nbttagcompound = new NBTTagCompound();
			entity.writeToNBTOptional(nbttagcompound);
			BlockPos blockpos;

			if (entity instanceof EntityPainting) {
				blockpos = ((EntityPainting) entity).getHangingPosition().subtract(startPos);
			} else {
				blockpos = new BlockPos(vec3d);
			}

			entities.add(new Template.EntityInfo(vec3d, blockpos, nbttagcompound));
		}
		
		return list;
	}

	public static boolean deploy(WorldServer playerWorld, boolean isReward, String thrower, String structureName, BlockPos dest, int size, List<Block> overridableBlocks,
			Map<BlockPos, Block> outOccupiedSpawnPositions, List<String> outEntityBlocking) {

		Template template = null;
		if(isReward){
			template = getTemplateForReward(playerWorld.getMinecraftServer(), structureName);
		} else {
			template = getTemplateForCapsule(playerWorld, structureName);
		}

		if (template != null)
        {
        	// check if the destination is valid : no unoverwritable block and no entities in the way.
        	PlacementSettings placementsettings = (new PlacementSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false).setChunk((ChunkPos)null).setReplacedBlock((Block)null).setIgnoreStructureBlock(false);
        	boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);
        	if(destValid){
        		spawnBlocksAndEntities(template, playerWorld, dest, placementsettings, outOccupiedSpawnPositions, overridableBlocks);
        		return true;
        	}
        }
		
		return false;
	}

	public static Template getTemplateForCapsule(WorldServer playerWorld, String structureName) {
		TemplateManager templatemanager = getTemplateManager(playerWorld);
		if(templatemanager == null) return null;
		
		Template template = templatemanager.func_189942_b(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
		return template;
	}
	
	public static Template getTemplateForReward(MinecraftServer server, String structureName) {
		return getRewardManager(server).func_189942_b(server, new ResourceLocation(structureName));
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
	public static boolean isDestinationValid(Template template, PlacementSettings placementIn, WorldServer destWorld, BlockPos destOriginPos, int size,
			List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

		IBlockState air = Blocks.AIR.getDefaultState();
		
		List<Template.BlockInfo> srcblocks = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "blocks");
		if(srcblocks == null) return false;
		
		Map<BlockPos,Template.BlockInfo> blockInfoByPosition = new HashMap<>();
		for (Template.BlockInfo template$blockinfo : srcblocks)
        {
            BlockPos blockpos = Template.transformedBlockPos(placementIn, template$blockinfo.pos);
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
	 * Rewritten from Template.func_189960_a
	 * @param p_189960_1_
	 * @param p_189960_2_
	 * @param p_189960_3_
	 * @param p_189960_4_
	 * @param p_189960_5_
	 */
	public static void spawnBlocksAndEntities(Template template, World p_189960_1_, BlockPos p_189960_2_, PlacementSettings p_189960_4_, Map<BlockPos,Block> occupiedPositions, List<Block> overridableBlocks)
    {
		// As it can't be extended, hacking template to use it as a saving mechanics with reflexion		
		List<Template.BlockInfo> blocks = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "blocks");
		BlockPos size = ObfuscationReflectionHelper.getPrivateValue(Template.class, template, "size");
		ITemplateProcessor p_189960_3_ = new BlockRotationProcessor(p_189960_2_, p_189960_4_);
		int p_189960_5_ = 2;
		
		if(blocks == null || size == null || p_189960_3_== null) return; 
		

        if (!blocks.isEmpty() && size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1)
        {
            Block block = p_189960_4_.getReplacedBlock();
            StructureBoundingBox structureboundingbox = p_189960_4_.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : blocks)
            {
                BlockPos blockpos = Template.transformedBlockPos(p_189960_4_, template$blockinfo.pos).add(p_189960_2_);
                Template.BlockInfo template$blockinfo1 = p_189960_3_ != null ? p_189960_3_.func_189943_a(p_189960_1_, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null)
                {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if (
                    		(block == null || block != block1) && 
                    		(!p_189960_4_.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) && 
                    		(structureboundingbox == null || structureboundingbox.isVecInside(blockpos)) &&
                    		// add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                    		(!occupiedPositions.containsKey(blockpos) || overridableBlocks.contains(occupiedPositions.get(blockpos))) 
                    )
                    {
                        IBlockState iblockstate = template$blockinfo1.blockState.withMirror(p_189960_4_.getMirror());
                        IBlockState iblockstate1 = iblockstate.withRotation(p_189960_4_.getRotation());

                        if (template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity != null)
                            {
                                if (tileentity instanceof IInventory)
                                {
                                    ((IInventory)tileentity).clear();
                                }

                                p_189960_1_.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (p_189960_1_.setBlockState(blockpos, iblockstate1, p_189960_5_) && template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity2 = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity2 != null)
                            {
                                template$blockinfo1.tileentityData.setInteger("x", blockpos.getX());
                                template$blockinfo1.tileentityData.setInteger("y", blockpos.getY());
                                template$blockinfo1.tileentityData.setInteger("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                tileentity2.func_189668_a(p_189960_4_.getMirror());
                                tileentity2.func_189667_a(p_189960_4_.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : blocks)
            {
                if (block == null || block != template$blockinfo2.blockState.getBlock())
                {
                    BlockPos blockpos1 = Template.transformedBlockPos(p_189960_4_, template$blockinfo2.pos).add(p_189960_2_);

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1))
                    {
                        p_189960_1_.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock());

                        if (template$blockinfo2.tileentityData != null)
                        {
                            TileEntity tileentity1 = p_189960_1_.getTileEntity(blockpos1);

                            if (tileentity1 != null)
                            {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }

            if (!p_189960_4_.getIgnoreEntities())
            {
				Method addEntitiesToWorld = ReflectionHelper.findMethod(
						Template.class, template, 
						new String[] { "func_186263_a", "addEntitiesToWorld" },
						World.class, BlockPos.class, Mirror.class, Rotation.class, StructureBoundingBox.class
				);
				try {
					addEntitiesToWorld.invoke(template, p_189960_1_, p_189960_2_, p_189960_4_.getMirror(), p_189960_4_.getRotation(), structureboundingbox);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					LOGGER.error("Error while invoking method addEntitiesToWorld during capsule spawning.", e);
				}
            }
        }
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
		TemplateManager templatemanager = getTemplateManager(playerWorld);
		
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
