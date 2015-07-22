package items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class CapsuleItem extends Item {

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
		case 1:
			return super.getUnlocalizedName() + ".activated";
		case 2:
			return super.getUnlocalizedName() + ".linked";
		case 0:
		default:
			return super.getUnlocalizedName() + ".empty";
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		subItems.add(new ItemStack(itemIn, 1, 0));
		subItems.add(new ItemStack(itemIn, 1, 1));
		subItems.add(new ItemStack(itemIn, 1, 2));
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {

		ItemStack ret = super.onItemRightClick(itemStackIn, worldIn, playerIn);
		
		System.out.println(String.valueOf(worldIn.isRemote) + " " + itemStackIn.getItemDamage());

		if (itemStackIn.getItemDamage() == 1) {
			EntityItem entityItem = throwItem(itemStackIn, playerIn);
			playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;
		}

		if (itemStackIn.getItemDamage() == 0) {
			itemStackIn.setItemDamage(1);

			NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
			timer.setInteger("starttime", playerIn.ticksExisted);
		}

		return ret;
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
		
		if (entityItem.isCollided && entityItem.getEntityItem().getItemDamage() == 1 && entityItem.getEntityWorld() != null) {
			BlockPos blockPos = this.findClosestBlock(entityItem);
			entityItem.getEntityWorld().setBlockState(blockPos, Blocks.obsidian.getDefaultState());
			entityItem.getEntityItem().setItemDamage(2);
			return true;
		}
		
		return false;
	}
	
	private BlockPos findClosestBlock(EntityItem entityItem) {
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
        	if (block.getMaterial() != Material.air && distance < closestDistance) {
        		closest = pos;
            	closestDistance = distance;
            }
        }
        
		return closest;
	}
}
