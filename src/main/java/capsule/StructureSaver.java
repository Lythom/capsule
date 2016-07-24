package capsule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
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
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class StructureSaver {

	protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);

	public static boolean store(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos position, int size, List<Block> excluded,
			Map<BlockPos, Block> excludedPositions, boolean keepSource) {

		MinecraftServer minecraftserver = worldserver.getMinecraftServer();
		TemplateManager templatemanager = worldserver.getStructureTemplateManager();
		Template template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		List<BlockPos> transferedPositions = takeBlocksFromWorldIntoCapsule(template, worldserver, position, new BlockPos(size, size, size), excludedPositions,
				excluded);
		template.setAuthor(playerID);
		boolean writingOK = templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
		if (writingOK && !keepSource) {
			removeTransferedBlockFromWorld(transferedPositions, worldserver);
		}

		return writingOK;

	}

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
	 * takes blocks from the world and puts the data them into this template
	 */
	public static List<BlockPos> takeBlocksFromWorldIntoCapsule(Template template, World worldIn, BlockPos startPos, BlockPos endPos,
			Map<BlockPos, Block> sourceIgnorePos, List<Block> excluded) {

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
					transferedBlocks.add(blockpos$mutableblockpos.add(0, 0, 0)); // save
																					// a
																					// copy
				}
			}

			blocks.clear();
			blocks.addAll(list);
			blocks.addAll(list1);
			blocks.addAll(list2);

			takeNonLivingEntitiesFromWorld(template, worldIn, blockpos1, blockpos2.add(1, 1, 1));

			return transferedBlocks;
		}

		return null;
	}

	/**
	 * takes blocks from the world and puts the data them into this template
	 */
	@SuppressWarnings("unchecked")
	public static void takeNonLivingEntitiesFromWorld(Template template, World worldIn, BlockPos startPos, BlockPos endPos) {
		// Hacking template to access entities, since it cannont be extended
		Field entitiesField = null;
		try {
			entitiesField = template.getClass().getDeclaredField("entities");
			entitiesField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e1) {
			e1.printStackTrace();
		}

		List<Template.EntityInfo> entities = null;
		try {
			entities = (List<Template.EntityInfo>) entitiesField.get(template);
		} catch (IllegalArgumentException | IllegalAccessException e1) {
			LOGGER.error("Error while saving capsule into structureBlock Template.", e1);
			return;
		}

		// rewritten vanilla code from Template.takeEntitiesFromWorld
		List<Entity> list = worldIn.<Entity> getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), new Predicate<Entity>() {
			public boolean apply(@Nullable Entity p_apply_1_) {
				return !(p_apply_1_ instanceof EntityLivingBase) || (p_apply_1_ instanceof EntityArmorStand);
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
	}

	public static boolean deploy(WorldServer playerWorld, String thrower, String structureName, BlockPos dest, int size, List<Block> overridableBlocks,
			Map<BlockPos, Block> outOccupiedSpawnPositions, List<String> outEntityBlocking) {

		TemplateManager templatemanager = playerWorld.getStructureTemplateManager();
		Template template = templatemanager.func_189942_b(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
		if (template != null)
        {
        	// check if the destination is valid : no unoverwritable block and no entities in the way.
        	PlacementSettings placementsettings = (new PlacementSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false).setChunk((ChunkPos)null).setReplacedBlock((Block)null).setIgnoreStructureBlock(false);
        	boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);
        	if(destValid){
        		template.addBlocksToWorldChunk(playerWorld, dest, placementsettings);
        		return true;
        	}
        }
		
		return false;
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
					Block destState = destWorld.getBlockState(destPos).getBlock();
		
					boolean destOccupied = (destState != air && !overridable.contains(destState));
					if (destState != air && outOccupiedPositions != null) {
						outOccupiedPositions.put(destPos, destState);
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

}
