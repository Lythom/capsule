package capsule.items;

import java.util.List;

import dimension.CapsuleDimension;
import dimension.CapsuleSavedData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import scala.actors.threadpool.Arrays;

public class CapsuleItem extends Item {
	
	public final static int STATE_EMPTY = 0;
	public final static int STATE_ACTIVATED = 1;
	public final static int STATE_LINKED = 2;
	public final static int STATE_DEPLOYED = 3;
	public final static int STATE_BROKEN = 4;
	
	@SuppressWarnings("unchecked")
	public static List<Block> excludedBlocks = Arrays.asList(new Block[]{
			Blocks.air,
			Blocks.bedrock
	});
	
	@SuppressWarnings("unchecked")
	public static List<Block> overridableBlocks = Arrays.asList(new Block[]{
			Blocks.air,
			Blocks.water,
			Blocks.leaves,
			Blocks.leaves2,
			Blocks.tallgrass,
			Blocks.red_flower,
			Blocks.yellow_flower
	});

	public CapsuleItem(String unlocalizedName) {
		super();
		this.setHasSubtypes(true);
		this.setUnlocalizedName(unlocalizedName);
		this.setCreativeTab(CreativeTabs.tabMisc);
		this.setMaxStackSize(1);
		this.setMaxDamage(0);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		switch (stack.getItemDamage()) {
		case CapsuleItem.STATE_ACTIVATED:
			return super.getUnlocalizedName() + ".activated";
		case CapsuleItem.STATE_LINKED:
			return super.getUnlocalizedName() + ".linked";
		case CapsuleItem.STATE_BROKEN:
			return super.getUnlocalizedName() + ".broken";
		default:
			return super.getUnlocalizedName() + ".empty";
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("size")){
			String size = String.valueOf(stack.getTagCompound().getInteger("size"));
			tooltip.add(StatCollector.translateToLocal("capsule.tooltip.size") + " : "+size+"x"+size+"x"+size);
		}
		if(stack.getItemDamage() == CapsuleItem.STATE_BROKEN){
			tooltip.add(EnumChatFormatting.ITALIC.toString() + EnumChatFormatting.RED.toString() + StatCollector.translateToLocal("capsule.tooltip.crafttorepair").trim());
		}
		super.addInformation(stack, playerIn, tooltip, advanced);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		subItems.add(new ItemStack(itemIn, 1, STATE_EMPTY));
		subItems.add(new ItemStack(itemIn, 1, STATE_LINKED));
		subItems.add(new ItemStack(itemIn, 1, STATE_BROKEN));
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {

		ItemStack ret = super.onItemRightClick(itemStackIn, worldIn, playerIn);
		
		System.out.println(String.valueOf(worldIn.isRemote) + " " + itemStackIn.getItemDamage());

		// an activated capsule is thrown farther on right click
		if (itemStackIn.getItemDamage() == STATE_ACTIVATED) {
			throwItem(itemStackIn, playerIn);
			playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;
		}

		// an empty capsule is activated on right click
		else if (itemStackIn.getItemDamage() == STATE_EMPTY || itemStackIn.getItemDamage() == STATE_LINKED) {
			itemStackIn.setItemDamage(STATE_ACTIVATED);

			NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
			timer.setInteger("starttime", playerIn.ticksExisted);
		}
		
		else if (itemStackIn.getItemDamage() == STATE_DEPLOYED && !worldIn.isRemote) {
			// store again
			WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimension.dimensionId);
			WorldServer playerWorld = (WorldServer)playerIn.worldObj;
			
			NBTTagCompound linkPos = itemStackIn.getTagCompound().getCompoundTag("linkPosition");
			BlockPos dest = new BlockPos(linkPos.getInteger("x"),linkPos.getInteger("y"),linkPos.getInteger("z"));
			NBTTagCompound spawnPos = itemStackIn.getTagCompound().getCompoundTag("spawnPosition");
			BlockPos source = new BlockPos(spawnPos.getInteger("x"),spawnPos.getInteger("y"),spawnPos.getInteger("z"));
			
			int size = 1;
			if(itemStackIn.getTagCompound().hasKey("size")){
				size = itemStackIn.getTagCompound().getInteger("size");
			}
			
			// do the transportation
			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					for (int z = 0; z < size; z++) {
						teleportBlock(playerWorld, capsuleWorld, source.add(x,y,z), dest.add(x,y,z));
					}
				}
			}
			
			itemStackIn.setItemDamage(STATE_LINKED);
			itemStackIn.getTagCompound().removeTag("spawnPosition");
		}

		return ret;
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

		// disactivate capsule after some time
		NBTTagCompound timer = stack.getSubCompound("activetimer", true);
		int tickDuration = 60; // 3 sec at 20 ticks/sec;
		if (stack.getItemDamage() == 1 && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + tickDuration) {
			stack.setItemDamage(0);
		}
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		super.onEntityItemUpdate(entityItem);
		
		ItemStack capsule = entityItem.getEntityItem();
		
		// Deploying capsule content on collision with a block
		if (!entityItem.worldObj.isRemote && entityItem.isCollided && capsule.getItemDamage() == STATE_ACTIVATED && entityItem.getEntityWorld() != null) {

			// total side size
			int size = 1;
			if(capsule.getTagCompound().hasKey("size")){
				size = capsule.getTagCompound().getInteger("size");
			}
			int exdendLength = (size-1)/2;
			
			// get destination world available position
			WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimension.dimensionId);
			WorldServer playerWorld = (WorldServer)entityItem.worldObj;
			
			// DEPLOY
			// is linked, deploy
			if(capsule.getTagCompound().hasKey("linkPosition")){
				
				// specify target to capture
				BlockPos dest = this.findClosestBlock(entityItem, excludedBlocks).add(-exdendLength, 1, -exdendLength);
				NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("linkPosition");
				BlockPos source = new BlockPos(linkPos.getInteger("x"),linkPos.getInteger("y"),linkPos.getInteger("z"));
				
				// do the transportation
				for (int x = 0; x < size; x++) {
					for (int y = 0; y < size; y++) {
						for (int z = 0; z < size; z++) {
							teleportBlock(capsuleWorld, playerWorld, source.add(x,y,z), dest.add(x,y,z));
						}
					}
				}
				
				// register the link in the capsule
				capsule.setItemDamage(STATE_DEPLOYED);
				savePosition("spawnPosition", capsule, dest);
				
				return true;
				
			// CAPTURE
			// is not linked, capture
			} else {
				
				// get available space data
				CapsuleSavedData capsulePlacer = getCapsulePlacer(capsuleWorld);
				
				// specify target to capture
				BlockPos blockPos = this.findClosestBlock(entityItem, excludedBlocks); // TO REPLACE with a specific crafted block
				BlockPos source = blockPos.add(-exdendLength, -exdendLength, -exdendLength);
				
				// get free room to store
				BlockPos dest = capsulePlacer.reserveNextAvailablePositionForSize(size);
				
				// do the transportation
				for (int x = 0; x < size; x++) {
					for (int y = 0; y < size; y++) {
						for (int z = 0; z < size; z++) {
							teleportBlock(playerWorld, capsuleWorld, source.add(x,y,z), dest.add(x,y,z));
						}
					}
				}
				
				// register the link in the capsule
				capsule.setItemDamage(STATE_LINKED);
				savePosition("linkPosition", capsule, dest);
				return true;
			}
		}
		
		return false;
	}
	

	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		int color = 0xFFFFFF;

		// material color
		if(renderPass == 0){
			if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("color")){
				color = stack.getTagCompound().getInteger("color");
			}
			
		// label color
		} else if(renderPass == 1) {
			color = this.getColor(stack);
		}
		return color;
	}

	/**
	 * Throw an item and return the new EntityItem created. Simulated a drop with stronger throw.
	 * 
	 * @param itemStackIn
	 * @param playerIn
	 * @return
	 */
	private EntityItem throwItem(ItemStack itemStackIn, EntityPlayer playerIn) {
		double d0 = playerIn.posY - 0.30000001192092896D + (double) playerIn.getEyeHeight();
		EntityItem entityitem = new EntityItem(playerIn.worldObj, playerIn.posX, d0, playerIn.posZ, itemStackIn);
		entityitem.setPickupDelay(40);
		entityitem.setThrower(playerIn.getName());
		float f = 0.5F;
		entityitem.motionX = (double) (-MathHelper.sin(playerIn.rotationYaw / 180.0F * (float) Math.PI)
				* MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
		entityitem.motionZ = (double) (MathHelper.cos(playerIn.rotationYaw / 180.0F * (float) Math.PI)
				* MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
		entityitem.motionY = (double) (-MathHelper.sin(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f + 0.1F);

		playerIn.joinEntityItemWithWorld(entityitem);

		return entityitem;
	}

	private void savePosition(String key, ItemStack capsule, BlockPos dest) {
		NBTTagCompound pos = new NBTTagCompound();
		pos.setInteger("x", dest.getX());
		pos.setInteger("y", dest.getY());
		pos.setInteger("z", dest.getZ());
		capsule.getTagCompound().setTag(key,pos);
	}

	private void teleportBlock(WorldServer sourceWorld, WorldServer capsuleWorld, BlockPos srcPos, BlockPos destPos) {
		TileEntity srcTE = sourceWorld.getTileEntity(srcPos);
		if(srcTE != null){
			// store the current block
			capsuleWorld.setTileEntity(destPos, srcTE);
			// remove from the world the stored block
			sourceWorld.removeTileEntity(srcPos);

		} else {
			IBlockState srcState = sourceWorld.getBlockState(srcPos);
			// store the current block
			capsuleWorld.setBlockState(destPos, srcState);
			// remove from the world the stored block
			sourceWorld.setBlockState(srcPos, Blocks.air.getDefaultState());
		}
		
		capsuleWorld.markBlockForUpdate(destPos);
		sourceWorld.markBlockForUpdate(srcPos);
	}

	private CapsuleSavedData getCapsulePlacer(WorldServer capsuleWorld) {
		CapsuleSavedData capsulePlacer = (CapsuleSavedData)capsuleWorld.loadItemData(CapsuleSavedData.class, "capsulePositions");
		if(capsulePlacer == null){
			capsulePlacer = new CapsuleSavedData("capsule");
			capsuleWorld.setItemData("capsulePositions", capsulePlacer);
		}
		return capsulePlacer;
	}
	
	private BlockPos findClosestBlock(EntityItem entityItem, List<Block> excludedBlocks) {
		if(entityItem.getEntityWorld() == null) return null;
		
		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

        Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i - 1, j - 1, k - 1), new BlockPos(i + 1, j + 1, k + 1));
        BlockPos closest = null;
        double closestDistance = 1000;
        for( BlockPos pos : blockPoss) {
        	Block block = entityItem.worldObj.getBlockState(pos).getBlock();
        	double distance = pos.distanceSqToCenter(i, j, k);
        	if (!excludedBlocks.contains(block) &&  distance < closestDistance) {
        		closest = pos;
            	closestDistance = distance;
            }
        }
        
		return closest;
	}
	
	private BlockPos findBottomBlock(EntityItem entityItem, List<Block> excludedBlocks) {
		if(entityItem.getEntityWorld() == null) return null;
		
		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

        Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i, j - 1, k), new BlockPos(i + 1, j + 1, k + 1));
        BlockPos closest = null;
        double closestDistance = 1000;
        for( BlockPos pos : blockPoss) {
        	Block block = entityItem.worldObj.getBlockState(new BlockPos(i, j - 1, k)).getBlock();
        	double distance = pos.distanceSqToCenter(i, j, k);
        	if (!excludedBlocks.contains(block) &&  distance < closestDistance) {
        		closest = pos;
            	closestDistance = distance;
            }
        }
        
		return closest;
	}
	
	
	/*
	 * Color stuff
	 */
	
	/**
     * Return whether the specified armor has a color.
     */
    public boolean hasColor(ItemStack stack)
    {
        return (!stack.hasTagCompound() ? false : (!stack.getTagCompound().hasKey("display", 10) ? false : stack.getTagCompound().getCompoundTag("display").hasKey("color", 3)));
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public int getColor(ItemStack stack)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null)
        {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

            if (nbttagcompound1 != null && nbttagcompound1.hasKey("color", 3))
            {
                return nbttagcompound1.getInteger("color");
            }
        }

        return 0xFFFFFF;
    }

    /**
     * Remove the color from the specified ItemStack.
     */
    public void removeColor(ItemStack stack)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null)
        {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

            if (nbttagcompound1.hasKey("color"))
            {
                nbttagcompound1.removeTag("color");
            }
        }
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public void setColor(ItemStack stack, int color)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound == null)
        {
            nbttagcompound = new NBTTagCompound();
            stack.setTagCompound(nbttagcompound);
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

        if (!nbttagcompound.hasKey("display", 10))
        {
            nbttagcompound.setTag("display", nbttagcompound1);
        }

        nbttagcompound1.setInteger("color", color);
    }
}
