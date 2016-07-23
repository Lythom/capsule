package capsule.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import capsule.Config;
import capsule.Helpers;
import capsule.Main;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.dimension.CapsuleSavedData;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

@SuppressWarnings("deprecation")
public class CapsuleItem extends Item {

	public final static int STATE_EMPTY = 0;
	public final static int STATE_EMPTY_ACTIVATED = 4;
	public final static int STATE_ACTIVATED = 1;
	public final static int STATE_LINKED = 2;
	public final static int STATE_DEPLOYED = 3;
	public final static int STATE_ONE_USE = 5;
	public final static int STATE_ONE_USE_ACTIVATED = 6;

	private static final int CAPSULE_MAX_CAPTURE_SIZE = 69;

	public CapsuleItem(String unlocalizedName) {
		super();
		this.setHasSubtypes(true);
		this.setUnlocalizedName(unlocalizedName);
		this.setMaxStackSize(1);
		this.setMaxDamage(0);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String name = I18n.translateToLocal("item.capsule.name");

		String state = "";
		switch (stack.getItemDamage()) {
		case CapsuleItem.STATE_ACTIVATED:
		case CapsuleItem.STATE_EMPTY_ACTIVATED:
		case CapsuleItem.STATE_ONE_USE_ACTIVATED:
			state = TextFormatting.DARK_GREEN + I18n.translateToLocal("item.capsule.state_activated") + TextFormatting.RESET;
			break;
		case CapsuleItem.STATE_LINKED:
			state = "";
			break;
		case CapsuleItem.STATE_DEPLOYED:
			state = I18n.translateToLocal("item.capsule.state_deployed");
			break;
		case CapsuleItem.STATE_ONE_USE:
			if (this.isReward(stack)) {
				state = I18n.translateToLocal("item.capsule.state_one_use");
			} else {
				state = I18n.translateToLocal("item.capsule.state_recovery");
			}

			break;
		}

		if (state.length() > 0) {
			state = state + " ";
		}
		String content = this.getLabel(stack);
		if (content.length() > 0) {
			content = content + " ";
		}

		return TextFormatting.RESET + state + content + name;
	}

	public boolean isReward(ItemStack stack) {
		return (stack.hasTagCompound() && stack.getTagCompound().hasKey("isReward") && stack.getTagCompound().getBoolean("isReward") && this.isOneUse(stack));
	}

	public void setIsReward(ItemStack stack, boolean isReward) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setBoolean("isReward", isReward);
	}

	public String getLabel(ItemStack stack) {

		if (stack == null)
			return "";
		if (!this.isLinked(stack)) {
			return I18n.translateToLocal("item.capsule.content_empty");
		} else if (stack.hasTagCompound() && stack.getTagCompound().hasKey("label") && !"".equals(stack.getTagCompound().getString("label"))) {
			return "“" + TextFormatting.ITALIC + stack.getTagCompound().getString("label") + TextFormatting.RESET + "”";
		}
		return I18n.translateToLocal("item.capsule.content_unlabeled");
	}

	private boolean isLinked(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey("structureName");
	}

	public void setLabel(ItemStack stack, String label) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("label", label);
	}

	@Override
	public int getItemEnchantability() {
		return 5;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {
		return getItemEnchantability();
	}

	@Override
	public boolean isItemTool(ItemStack stack) {
		return true;
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		int size = getSize(stack);
		tooltip.add(I18n.translateToLocal("capsule.tooltip.size") + " : " + size + "x" + size + "x" + size);
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("upgraded")) {
			int upgradeLevel = stack.getTagCompound().getInteger("upgraded");
			tooltip.add(I18n.translateToLocal("capsule.tooltip.upgraded") + " : " + String.valueOf(upgradeLevel)
					+ (upgradeLevel >= Config.config.get("Balancing", "capsuleUpgradesLimit", 10).getInt()
							? " (" + I18n.translateToLocal("capsule.tooltip.maxedout") + ")" : ""));

		}
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("overpowered") && stack.getTagCompound().getByte("overpowered") == (byte)1) {
			tooltip.add(I18n.translateToLocal("capsule.tooltip.overpowered"));

		}
		if (stack.getItemDamage() == CapsuleItem.STATE_ONE_USE) {
			I18n.translateToLocal("capsule.tooltip.one_use").trim();
		}
		super.addInformation(stack, playerIn, tooltip, advanced);
	}

	/**
	 * Register items in the creative tab
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {

		ItemStack ironCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		ironCapsule.setTagInfo("color", new NBTTagInt(0xCCCCCC));
		ironCapsule.setTagInfo("size", new NBTTagInt(Config.config.get("Balancing", "ironCapsuleSize", "1").getInt()));

		ItemStack goldCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		goldCapsule.setTagInfo("color", new NBTTagInt(0xFFD700));
		goldCapsule.setTagInfo("size", new NBTTagInt(Config.config.get("Balancing", "goldCapsuleSize", "3").getInt()));

		ItemStack diamondCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		diamondCapsule.setTagInfo("color", new NBTTagInt(0x00FFF2));
		diamondCapsule.setTagInfo("size", new NBTTagInt(Config.config.get("Balancing", "diamondCapsuleSize", "5").getInt()));

		ItemStack opCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_EMPTY);
		opCapsule.setTagInfo("color", new NBTTagInt(0xFFFFFF));
		opCapsule.setTagInfo("size", new NBTTagInt(Config.config.get("Balancing", "opCapsuleSize", "1").getInt()));
		opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
		
		ItemStack unlabelledCapsule = ironCapsule.copy();
		unlabelledCapsule.setItemDamage(STATE_LINKED);
		unlabelledCapsule.getTagCompound().setString("structureName", "(C-CreativeLinkedCapsule)");
		
		ItemStack recoveryCapsule = ironCapsule.copy();
		recoveryCapsule.setItemDamage(STATE_ONE_USE);
		recoveryCapsule.getTagCompound().setBoolean("oneUse", true);
		unlabelledCapsule.getTagCompound().setString("structureName", "(C-CreativeOneUseCapsule)");

		subItems.add(ironCapsule);
		subItems.add(goldCapsule);
		subItems.add(diamondCapsule);
		subItems.add(opCapsule);
		
		subItems.add(unlabelledCapsule);
		subItems.add(recoveryCapsule);


	}

	/**
	 * Activate or power throw on right click.
	 */
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
		
		if(hand == EnumHand.OFF_HAND){
			return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemStackIn);
		}

		if (playerIn.isSneaking() && (itemStackIn.getItemDamage() == STATE_LINKED || itemStackIn.getItemDamage() == STATE_DEPLOYED)) {
			Main.proxy.openGuiScreen(playerIn);
		}

		else if (!worldIn.isRemote) {

			// an activated capsule is thrown farther on right click
			if (isActivated(itemStackIn)) {
				throwItem(itemStackIn, playerIn);
				playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;
			}

			// an empty or a linked capsule is activated on right click
			else if (itemStackIn.getItemDamage() == STATE_EMPTY || itemStackIn.getItemDamage() == STATE_LINKED
					|| itemStackIn.getItemDamage() == STATE_ONE_USE) {
				if (itemStackIn.getItemDamage() == STATE_EMPTY) {
					this.setState(itemStackIn, STATE_EMPTY_ACTIVATED);
				}
				if (itemStackIn.getItemDamage() == STATE_LINKED) {
					this.setState(itemStackIn, STATE_ACTIVATED);
				}
				if (itemStackIn.getItemDamage() == STATE_ONE_USE) {
					this.setState(itemStackIn, STATE_ONE_USE_ACTIVATED);
				}

				NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
				timer.setInteger("starttime", playerIn.ticksExisted);
			}

			// an opened capsule revoke deployed content on right click
			else if (itemStackIn.getItemDamage() == STATE_DEPLOYED && !worldIn.isRemote) {
				resentToCapsule(itemStackIn, playerIn);
			}
		}

		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStackIn);
	}

	private boolean isActivated(ItemStack itemStackIn) {
		return itemStackIn.getItemDamage() == STATE_ACTIVATED || itemStackIn.getItemDamage() == STATE_EMPTY_ACTIVATED
				|| itemStackIn.getItemDamage() == STATE_ONE_USE_ACTIVATED;
	}

	/**
	 * Manage the "activated" state of the capsule.
	 */
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

		if (!worldIn.isRemote) {

			// disable capsule after some time
			NBTTagCompound timer = stack.getSubCompound("activetimer", true);
			int tickDuration = 60; // 3 sec at 20 ticks/sec;
			if (this.isActivated(stack) && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + tickDuration) {

				revertStateFromActivated(stack);
			}
		}
	}

	/**
	 * Detect a collision and act accordingly (deploy or capture or break)
	 */
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		super.onEntityItemUpdate(entityItem);

		ItemStack capsule = entityItem.getEntityItem();

		// Deploying capsule content on collision with a block
		if (!entityItem.worldObj.isRemote && entityItem.isCollided && this.isActivated(capsule) && entityItem.getEntityWorld() != null) {

			int size = getSize(capsule);
			int exdendLength = (size - 1) / 2;

			// get destination world available position
			WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimensionRegistrer.dimensionId);
			WorldServer playerWorld = (WorldServer) entityItem.worldObj;

			if (isLinked(capsule)) {

				// DEPLOY
				// is linked, deploy
				boolean deployed = deployCapsule(entityItem, capsule, size, exdendLength, capsuleWorld, playerWorld);
				if (deployed && this.isOneUse(capsule)) {
					entityItem.setDead();
				}
				return true;

			} else {

				// CAPTURE
				// is not linked, capture
				captureContentIntoCapsule(entityItem, capsule, size, exdendLength, capsuleWorld, playerWorld);
				return true;
			}
		}

		return false;
	}

	/**
	 * Capture the content around the capsule entityItem, update capsule state.
	 * 
	 * @param entityItem
	 * @param capsule
	 * @param size
	 * @param exdendLength
	 * @param capsuleWorld
	 * @param playerWorld
	 */
	private boolean captureContentIntoCapsule(EntityItem entityItem, ItemStack capsule, int size, int exdendLength, WorldServer capsuleWorld,
			WorldServer playerWorld) {

		boolean didCapture = false;

		// specify target to capture
		BlockPos marker = Helpers.findSpecificBlock(entityItem, size + 2, BlockCapsuleMarker.class);
		if (marker != null) {
			BlockPos source = marker.add(-exdendLength, 1, -exdendLength);

			// Save the region in a structure block file
			String player = "CapsuleMod";
			if(entityItem.getThrower() != null){
				player = entityItem.getThrower();
			}
			CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
			// TODO : create folder structures/capsule if it does not exists
			String capsuleID = "capsule/C-" + player + "-" + csd.getNextCount();
			StructureSaver.store(playerWorld, entityItem.getThrower(), capsuleID, source, size, getExcludedBlocs(capsule), null);
			// Helpers.swapRegions(playerWorld, capsuleWorld, source, dest, size, Config.overridableBlocks, getExcludedBlocs(capsule), false, null, null, null);

			// register the link in the capsule
			this.setState(capsule, STATE_LINKED);
			capsule.getTagCompound().setString("structureName", capsuleID);
			didCapture = true;

		} else {

			revertStateFromActivated(capsule);
			if (entityItem == null || playerWorld == null) {
				return false;
			}

			// send a chat message to explain failure
			EntityPlayer player = playerWorld.getPlayerEntityByName(entityItem.getThrower());
			if (player != null) {
				player.addChatMessage(new TextComponentTranslation("capsule.error.noCaptureBase"));
			}
		}

		return didCapture;
	}

	private Map<BlockPos, Block> getOccupiedSourcePos(ItemStack capsule) {
		Map<BlockPos, Block> occupiedSources = new HashMap<BlockPos, Block>();
		if (capsule.hasTagCompound() && capsule.getTagCompound().hasKey("occupiedSpawnPositions")) {
			NBTTagList list = capsule.getTagCompound().getTagList("occupiedSpawnPositions", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound entry = list.getCompoundTagAt(i);
				occupiedSources.put(BlockPos.fromLong(entry.getLong("pos")), Block.getBlockById(entry.getInteger("blockId")));
			}
		}
		return occupiedSources;
	}

	private Map<BlockPos, Block> setOccupiedSourcePos(ItemStack capsule, Map<BlockPos, Block> occupiedSpawnPositions) {
		Map<BlockPos, Block> occupiedSources = new HashMap<BlockPos, Block>();
		NBTTagList entries = new NBTTagList();
		for (Entry<BlockPos, Block> entry : occupiedSpawnPositions.entrySet()) {
			NBTTagCompound nbtEntry = new NBTTagCompound();
			nbtEntry.setLong("pos", entry.getKey().toLong());
			nbtEntry.setInteger("blockId", Block.getIdFromBlock(entry.getValue()));
			entries.appendTag(nbtEntry);
		}
		if (!capsule.hasTagCompound()) {
			capsule.setTagCompound(new NBTTagCompound());
		}
		capsule.getTagCompound().setTag("occupiedSpawnPositions", entries);
		return occupiedSources;
	}

	private void revertStateFromActivated(ItemStack capsule) {
		if (this.isOneUse(capsule)) {
			this.setState(capsule, STATE_ONE_USE);
		} else if (this.isLinked(capsule)) {
			this.setState(capsule, STATE_LINKED);
		} else {
			this.setState(capsule, STATE_EMPTY);
		}
	}

	private boolean isOneUse(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey("oneUse") && stack.getTagCompound().getBoolean("oneUse");
	}

	/**
	 * Deploy the capsule at the entityItem position. update capsule state
	 * 
	 * @param entityItem
	 * @param capsule
	 * @param size
	 * @param exdendLength
	 * @param capsuleWorld
	 * @param playerWorld
	 */
	private boolean deployCapsule(EntityItem entityItem, ItemStack capsule, int size, int exdendLength, WorldServer capsuleWorld, WorldServer playerWorld) {
		// specify target to capture
//		BlockPos bottomBlockPos = Helpers.findBottomBlock(entityItem, Config.excludedBlocks);
//		boolean didSpawn = false;
//
//		if (bottomBlockPos != null) {
//			BlockPos dest = bottomBlockPos.add(-exdendLength, 1, -exdendLength);
//			NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("linkPosition");
//			BlockPos source = new BlockPos(linkPos.getInteger("x"), linkPos.getInteger("y"), linkPos.getInteger("z"));
//
//			// do the transportation
//			Map<BlockPos, Block> occupiedSpawnPositions = new HashMap<BlockPos, Block>();
//			List<String> outEntityBlocking = new ArrayList<String>();
//			boolean result = Helpers.swapRegions(capsuleWorld, playerWorld, source, dest, size, Config.overridableBlocks, getExcludedBlocs(capsule),
//					this.isReward(capsule), null, occupiedSpawnPositions, outEntityBlocking);
//			this.setOccupiedSourcePos(capsule, occupiedSpawnPositions);
//
//			if (result) {
//				// register the link in the capsule
//				this.setState(capsule, STATE_DEPLOYED);
//				savePosition("spawnPosition", capsule, dest);
//				didSpawn = true;
//
//			} else {
//				revertStateFromActivated(capsule);
//				if (entityItem == null || playerWorld == null) {
//					return false;
//				}
//				// send a chat message to explain failure
//				EntityPlayer player = playerWorld.getPlayerEntityByName(entityItem.getThrower());
//				if (player != null) {
//					if (outEntityBlocking.size() > 0) {
//						player.addChatMessage(
//								new TextComponentTranslation("capsule.error.cantMergeWithDestinationEntity",
//										StringUtils.join(outEntityBlocking, ", ")));
//					} else {
//						player.addChatMessage(new TextComponentTranslation("capsule.error.cantMergeWithDestination"));
//					}
//
//				}
//			}
//		}
//
//		return didSpawn;
		return false;
	}

	private void resentToCapsule(ItemStack itemStackIn, EntityPlayer playerIn) {
		// store again
//		WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimensionRegistrer.dimensionId);
//		if (capsuleWorld == null) {
//			System.err.println("Can't get Capsule World from DimensionManager");
//			return;
//		}
//		WorldServer playerWorld = (WorldServer) playerIn.worldObj;
//
//		NBTTagCompound spawnPos = itemStackIn.getTagCompound().getCompoundTag("spawnPosition");
//		BlockPos source = new BlockPos(spawnPos.getInteger("x"), spawnPos.getInteger("y"), spawnPos.getInteger("z"));
//
//		int size = getSize(itemStackIn);
//
//		// do the transportation
//		Helpers.swapRegions(playerWorld, capsuleWorld, source, null, size, Config.overridableBlocks, getExcludedBlocs(itemStackIn), false,
//				this.getOccupiedSourcePos(itemStackIn), null, null);
//
//		this.setState(itemStackIn, STATE_LINKED);
//		itemStackIn.getTagCompound().removeTag("spawnPosition");
	}

	public void setState(ItemStack stack, int state) {
		stack.setItemDamage(state);
	}
	
	public List<Block> getExcludedBlocs(ItemStack stack) {
		List<Block> excludedBlocks = Config.excludedBlocks;
		if(stack.hasTagCompound() && stack.getTagCompound().hasKey("overpowered") &&  stack.getTagCompound().getByte("overpowered") == ((byte)1)){
			excludedBlocks = Config.opExcludedBlocks;
		}
		return excludedBlocks;
	}

	/**
	 * The capsule capture size.
	 * 
	 * @param itemStackIn
	 * @return
	 */
	private int getSize(ItemStack itemStackIn) {
		int size = 1;
		if (itemStackIn.hasTagCompound() && itemStackIn.getTagCompound().hasKey("size")) {
			size = itemStackIn.getTagCompound().getInteger("size");
		}
		if (size > CAPSULE_MAX_CAPTURE_SIZE) {
			size = CAPSULE_MAX_CAPTURE_SIZE;
			itemStackIn.getTagCompound().setInteger("size", size);
			System.err.println("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
		} else if (size % 2 == 0) {
			size--;
			itemStackIn.getTagCompound().setInteger("size", size);
			System.err.println("Capsule size must be an odd number to achieve consistency on deployment. Resized to : " + size);
		}

		return size;
	}

	/**
	 * renderPass 0 => The material color renderPass 1 => The label color
	 */
	public int getColorFromItemstack(ItemStack stack, int renderPass) {
		int color = 0xFFFFFF;

		// material color
		if (renderPass == 0) {
			if (stack.hasTagCompound() && stack.getTagCompound().hasKey("color")) {
				color = stack.getTagCompound().getInteger("color");
			}

			// label color
		} else if (renderPass == 1) {
			if (this.isLinked(stack)) {
				color = Helpers.getColor(stack);
			} else {
				return -1;
			}

		}
		return color;
	}

	/**
	 * Throw an item and return the new EntityItem created. Simulated a drop
	 * with stronger throw.
	 * 
	 * @param itemStackIn
	 * @param playerIn
	 * @return
	 */
	private EntityItem throwItem(ItemStack itemStackIn, EntityPlayer playerIn) {
		double d0 = playerIn.posY - 0.30000001192092896D + (double) playerIn.getEyeHeight();
		EntityItem entityitem = new EntityItem(playerIn.worldObj, playerIn.posX, d0, playerIn.posZ, itemStackIn);
		entityitem.setPickupDelay(10);
		entityitem.setThrower(playerIn.getName());
		float f = 0.5F;
		entityitem.motionX = (double) (-MathHelper.sin(playerIn.rotationYaw / 180.0F * (float) Math.PI)
				* MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
		entityitem.motionZ = (double) (MathHelper.cos(playerIn.rotationYaw / 180.0F * (float) Math.PI)
				* MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
		entityitem.motionY = (double) (-MathHelper.sin(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f + 0.1F);

		playerIn.dropItemAndGetStack(entityitem);

		return entityitem;
	}

	/**
	 * Set the NBT tag "key" to be a BlockPos coordinates.
	 * 
	 * @param key
	 * @param capsule
	 * @param dest
	 */
	private void savePosition(String key, ItemStack capsule, BlockPos dest) {
		NBTTagCompound pos = new NBTTagCompound();
		pos.setInteger("x", dest.getX());
		pos.setInteger("y", dest.getY());
		pos.setInteger("z", dest.getZ());
		capsule.getTagCompound().setTag(key, pos);
	}

	public void clearCapsule(ItemStack capsule) {
		this.setState(capsule, CapsuleItem.STATE_EMPTY);
		capsule.getTagCompound().removeTag("structureName");
	}
	
	/**
	 * Get the Capsule saving tool that can allocate a new Capsule zone in the
	 * capsule dimension.
	 * 
	 * @param capsuleWorld
	 * @return
	 */
	private CapsuleSavedData getCapsuleSavedData(WorldServer capsuleWorld) {
		CapsuleSavedData capsuleSavedData = (CapsuleSavedData) capsuleWorld.loadItemData(CapsuleSavedData.class, "capsuleData");
		if (capsuleSavedData == null) {
			capsuleSavedData = new CapsuleSavedData("capsuleData");
			capsuleWorld.setItemData("capsuleData", capsuleSavedData);
			capsuleSavedData.setDirty(true);
		}
		return capsuleSavedData;
	}

}
