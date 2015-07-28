package capsule.items;

import capsule.Helpers;
import capsule.dimension.CapsuleDimensionRegistrer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class CreativeTP extends Item {
	

	public CreativeTP(String unlocalizedName) {
		super();
		this.setUnlocalizedName(unlocalizedName);
		this.setCreativeTab(CreativeTabs.tabMisc);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName() + ".creative_tp";
	}
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side,
			float hitX, float hitY, float hitZ) {
		
		if(!worldIn.isRemote){
			Helpers.teleportBlock((WorldServer)worldIn, (WorldServer)worldIn, pos, pos.add(0, 1, 0));
		}
		
		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {

		EntityPlayerMP playerMP = null;
		if(playerIn instanceof EntityPlayerMP) {
			
			playerMP = (EntityPlayerMP)playerIn;
			if(playerMP.dimension == 0){
				NBTTagCompound overworldPos = itemStackIn.getSubCompound("overworldPos", true);
				overworldPos.setInteger("x", playerIn.getPosition().getX());
				overworldPos.setInteger("y", playerIn.getPosition().getY());
				overworldPos.setInteger("z", playerIn.getPosition().getZ());
				playerMP.mcServer.getConfigurationManager().transferPlayerToDimension(playerMP, CapsuleDimensionRegistrer.dimensionId, new CTPTeleporter((WorldServer)worldIn,-1, 1, -1));
			} else {
				NBTTagCompound overworldPos = itemStackIn.getSubCompound("overworldPos", true);
				playerMP.mcServer.getConfigurationManager().transferPlayerToDimension(playerMP, 0, new CTPTeleporter((WorldServer)worldIn,overworldPos.getInteger("x"), overworldPos.getInteger("y"), overworldPos.getInteger("z")));
			}
			
		}

		return itemStackIn;
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
	
	static class CTPTeleporter extends Teleporter
	{

		private double x;
		private double z;
		private double y;

		public CTPTeleporter(WorldServer worldIn, double x, double y, double z) {
			super(worldIn);
			this.x = x;
			this.y = y;
			this.z = z;
		}


		@Override
		public void placeInPortal(Entity entityIn, float rotationYaw)
		{
			entityIn.setLocationAndAngles( this.x, this.y, this.z, entityIn.getRotationYawHead(), 0.0F );
		}

		@Override
		public boolean placeInExistingPortal( Entity par1Entity, float par8 )
		{
			return false;
		}

		@Override
		public boolean makePortal( Entity par1Entity )
		{
			return false;
		}

		@Override
		public void removeStalePortalLocations( long par1 )
		{

		}
	}

}
