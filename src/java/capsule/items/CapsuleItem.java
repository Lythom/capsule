package capsule.items;

import java.util.Iterator;
import java.util.List;

import capsule.Helpers;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.TileEntityCapture;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.dimension.CapsuleSavedData;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
	public String getItemStackDisplayName(ItemStack stack) {
		String name = StatCollector.translateToLocal("item.capsule.name");
		
		String state = "";
		switch (stack.getItemDamage()) {
			case CapsuleItem.STATE_ACTIVATED:
				state = StatCollector.translateToLocal("item.capsule.state_activated");
				break;
			case CapsuleItem.STATE_LINKED:
				state = StatCollector.translateToLocal("item.capsule.state_linked");
				break;
			case CapsuleItem.STATE_BROKEN:
				state = StatCollector.translateToLocal("item.capsule.state_broken");
				break;
			case CapsuleItem.STATE_DEPLOYED:
				state = StatCollector.translateToLocal("item.capsule.state_deployed");
				break;
		}
		
		if(state.length() > 0){
			state = state + " ";
		}
		String content = this.getLabel(stack);
		if(content.length() > 0){
			content = content + " ";
		}

		return state + content + name;
	}
	
	public String getLabel(ItemStack stack){
		
		if(stack == null) return "";
		if(!this.isLinked(stack)){
			return StatCollector.translateToLocal("item.capsule.content_empty");
		}
		else if(stack.hasTagCompound() && stack.getTagCompound().hasKey("Label")){
			return stack.getTagCompound().getString("Label");
		}
		return StatCollector.translateToLocal("item.capsule.content_unlabeled");
	}
	
	private boolean isLinked(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey("linkPosition");
	}

	public void setLabel(ItemStack stack, String label) {
		if(stack.hasTagCompound()){
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("Label", label);
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

		if(!worldIn.isRemote){
	
			// an activated capsule is thrown farther on right click
			if (itemStackIn.getItemDamage() == STATE_ACTIVATED) {
				throwItem(itemStackIn, playerIn);
				playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;
			}
	
			// an empty or a linked capsule is activated on right click
			else if (itemStackIn.getItemDamage() == STATE_EMPTY || itemStackIn.getItemDamage() == STATE_LINKED) {
				itemStackIn.setItemDamage(STATE_ACTIVATED);
	
				NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
				timer.setInteger("starttime", playerIn.ticksExisted);
			}
			
			// an opened capsule revoke deployed content on right click
			else if (itemStackIn.getItemDamage() == STATE_DEPLOYED && !worldIn.isRemote) {
				resentToCapsule(itemStackIn, playerIn);
			}
		}

		return ret;
	}


	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

		// disactivate capsule after some time
		NBTTagCompound timer = stack.getSubCompound("activetimer", true);
		int tickDuration = 60; // 3 sec at 20 ticks/sec;
		if (stack.getItemDamage() == STATE_ACTIVATED && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + tickDuration) {
			
			if(this.isLinked(stack)){
				stack.setItemDamage(STATE_LINKED);
			} else {
				stack.setItemDamage(STATE_EMPTY);
			}
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
			WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimensionRegistrer.dimensionId);
			WorldServer playerWorld = (WorldServer)entityItem.worldObj;
			
			// DEPLOY
			// is linked, deploy
			if(capsule.getTagCompound().hasKey("linkPosition")){
				
				deployCapsule(entityItem, capsule, size, exdendLength, capsuleWorld, playerWorld);
				return true;
				
			// CAPTURE
			// is not linked, capture
			} else {
				
				captureContentIntoCapsule(entityItem, capsule, size, exdendLength, capsuleWorld, playerWorld);
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Capture the content around the capsule entityItem, update capsule state.
	 * @param entityItem
	 * @param capsule
	 * @param size
	 * @param exdendLength
	 * @param capsuleWorld
	 * @param playerWorld
	 */
	private void captureContentIntoCapsule(EntityItem entityItem, ItemStack capsule, int size, int exdendLength,
			WorldServer capsuleWorld, WorldServer playerWorld) {
		// get available space data
		CapsuleSavedData capsulePlacer = getCapsulePlacer(capsuleWorld);
		
		// specify target to capture
		BlockPos marker = Helpers.findSpecificBlock(entityItem, size, BlockCapsuleMarker.class);
		if(marker != null){
			BlockPos source = marker.add(-exdendLength, 1, -exdendLength);
			
			// get free room to store
			BlockPos dest = capsulePlacer.reserveNextAvailablePositionForSize(size);
			
			// do the transportation
			for (int y = size-1; y > 0; y--) {
				for (int x = 0; x < size; x++) {
					for (int z = 0; z < size; z++) {
						Helpers.teleportBlock(playerWorld, capsuleWorld, source.add(x,y,z), dest.add(x,y,z));
					}
				}
			}
			
			// register the link in the capsule
			capsule.setItemDamage(STATE_LINKED);
			savePosition("linkPosition", capsule, dest);
		} else {
			capsule.setItemDamage(STATE_BROKEN);
		}
	}

	/**
	 * Deploy the capsule at the entityItem position. update capsule state
	 * @param entityItem
	 * @param capsule
	 * @param size
	 * @param exdendLength
	 * @param capsuleWorld
	 * @param playerWorld
	 */
	private void deployCapsule(EntityItem entityItem, ItemStack capsule, int size, int exdendLength,
			WorldServer capsuleWorld, WorldServer playerWorld) {
		// specify target to capture
		BlockPos dest = Helpers.findBottomBlock(entityItem, excludedBlocks).add(-exdendLength, 1, -exdendLength);
		NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("linkPosition");
		BlockPos source = new BlockPos(linkPos.getInteger("x"),linkPos.getInteger("y"),linkPos.getInteger("z"));
		
		// do the transportation
		for (int y = size-1; y > 0; y--) {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {
					Helpers.teleportBlock(capsuleWorld, playerWorld, source.add(x,y,z), dest.add(x,y,z));
				}
			}
		}
		
		// register the link in the capsule
		capsule.setItemDamage(STATE_DEPLOYED);
		savePosition("spawnPosition", capsule, dest);
	}
	

	private void resentToCapsule(ItemStack itemStackIn, EntityPlayer playerIn) {
		// store again
		WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimensionRegistrer.dimensionId);
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
					Helpers.teleportBlock(playerWorld, capsuleWorld, source.add(x,y,z), dest.add(x,y,z));
				}
			}
		}
		
		itemStackIn.setItemDamage(STATE_LINKED);
		itemStackIn.getTagCompound().removeTag("spawnPosition");
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
			color = Helpers.getColor(stack);
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

	private CapsuleSavedData getCapsulePlacer(WorldServer capsuleWorld) {
		CapsuleSavedData capsulePlacer = (CapsuleSavedData)capsuleWorld.loadItemData(CapsuleSavedData.class, "capsulePositions");
		if(capsulePlacer == null){
			capsulePlacer = new CapsuleSavedData("capsule");
			capsuleWorld.setItemData("capsulePositions", capsulePlacer);
		}
		return capsulePlacer;
	}

}
