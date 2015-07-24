package capsule.items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import scala.actors.threadpool.Arrays;

public class CapsuleItem extends Item {
	
	public final static int STATE_EMPTY = 0;
	public final static int STATE_ACTIVATED = 1;
	public final static int STATE_LINKED = 2;
	public final static int STATE_BROKEN = 3;
	
	@SuppressWarnings("rawtypes")
	public static List excludedBlocks = Arrays.asList(new Block[]{
			Blocks.air,
			Blocks.dirt,
			Blocks.gravel,
			Blocks.grass,
			Blocks.tallgrass,
			Blocks.bedrock,
			Blocks.stone
	});
	
	@SuppressWarnings("rawtypes")
	public static List overridableBlocks = Arrays.asList(new Block[]{
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
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("size")){
			String size = String.valueOf(stack.getTagCompound().getInteger("size"));
			int color = stack.getTagCompound().getInteger("color");
			tooltip.add("Size : "+size+"x"+size+"x"+size);
			tooltip.add("Color : "+Integer.toHexString(color));
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

		if (itemStackIn.getItemDamage() == 1) {
			throwItem(itemStackIn, playerIn);
			playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;
		}

		if (itemStackIn.getItemDamage() == 0) {
			itemStackIn.setItemDamage(1);

			NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
			timer.setInteger("starttime", playerIn.ticksExisted);
		}

		return ret;
	}
	
	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		int color = 0xFFFFFF;
		if(renderPass == 0){
			if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("color")){
				color = stack.getTagCompound().getInteger("color");
			}
		} else if(renderPass == 1) {
			color = this.getColor(stack);
		}
		return color;
	}

	/**
	 * Throw an item and return the new EntityItem created
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

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

		NBTTagCompound timer = stack.getSubCompound("activetimer", true);
		int tickDuration = 60; // 3 sec at 20 ticks/sec;
		if (stack.getItemDamage() == 1 && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + tickDuration) {
			stack.setItemDamage(0);
		}
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		super.onEntityItemUpdate(entityItem);
		
		if (entityItem.isCollided && entityItem.getEntityItem().getItemDamage() == 2 && entityItem.getEntityWorld() != null) {
			BlockPos baseBlockPos = new BlockPos(entityItem.posX, entityItem.posY, entityItem.posZ);
			
			
		}
		
		if (entityItem.isCollided && entityItem.getEntityItem().getItemDamage() == 1 && entityItem.getEntityWorld() != null) {
			BlockPos blockPos = this.findClosestBlock(entityItem, excludedBlocks);
			// put the structure in item NBT
			// it's a Map of relative BlockPos => BlockState
			
			// remove the structure from the world
			
			entityItem.getEntityItem().setItemDamage(2);
			return true;
		}
		
		return false;
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
