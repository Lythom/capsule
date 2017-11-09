package capsule.items;

import capsule.*;
import capsule.blocks.BlockCapsuleMarker;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleThrowQueryToServer;
import joptsimple.internal.Strings;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings({"deprecation", "ConstantConditions"})
public class CapsuleItem extends Item {

    public static final int ACTIVE_DURATION_IN_TICKS = 60; // 3 sec at 20 ticks/sec
    public final static int STATE_EMPTY = 0;
    public final static int STATE_EMPTY_ACTIVATED = 4;
    public final static int STATE_ACTIVATED = 1;
    public final static int STATE_LINKED = 2;
    public final static int STATE_DEPLOYED = 3;
    public final static int STATE_ONE_USE = 5;
    public final static int STATE_ONE_USE_ACTIVATED = 6;
    public static final int CAPSULE_MAX_CAPTURE_SIZE = 31; // max size of the StructureBlocks Templates
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleItem.class);

    /**
     * Capsule Mod main item. Used to store region data to be deployed and undeployed.
     * <p>
     * Damage values :
     * see STATE_<state> constants.
     * <p>
     * NBTData reference:
     * * int color 													// material color
     * * tag display : {int color} 									// base color
     * * int size													// odd number, size of the square side the capsule can hold
     * * string label												// User customizable label
     * * byte overpowered											// If the capsule can capture powerfull blocks
     * * bool isReward												// if the content of the template must be kept when capsule is deployed.
     * * string structureName										// name of the template file name without the .nbt extension.
     * // Lookup paths are /<worldsave>/structures/capsules for non-rewards, and structureName must contains the full path for rewards and loots
     * * tag activetimer : {int starttime} 							// used to time the moment when the capsule must deactivate
     * * tag occupiedSpawnPositions : [{int blockId, long pos},…]   // remember what position not the recapture is block didn't change
     * * long deployAt												// when thrown with preview, position to deploy the capsule to match preview
     * * arr ench:[0:{lvl:1s,id:101s}]
     *
     * @param unlocalizedName registry name
     */
    public CapsuleItem(String unlocalizedName) {
        super();
        this.setHasSubtypes(true);
        this.setUnlocalizedName(unlocalizedName);
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
    }

    public static ItemStack createEmptyCapsule(int baseColor, int materialColor, int size, boolean overpowered, @Nullable String label, @Nullable Integer upgraded) {
        ItemStack capsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, STATE_EMPTY);
        Helpers.setColor(capsule, baseColor); // standard dye is for baseColor
        capsule.setTagInfo("color", new NBTTagInt(materialColor)); // "color" is for materialColor
        capsule.setTagInfo("size", new NBTTagInt(size));
        if (upgraded != null) {
            capsule.setTagInfo("upgraded", new NBTTagInt(upgraded));
        }
        if (overpowered) {
            capsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
        }
        if (label != null) {
            capsule.setTagInfo("label", new NBTTagString(label));
        }

        return capsule;
    }

    public static ItemStack createRewardCapsule(String structureName, int baseColor, int materialColor, int size, @Nullable String label, @Nullable String author) {
        ItemStack capsule = createEmptyCapsule(baseColor, materialColor, size, false, label, null);
        setIsReward(capsule);
        setOneUse(capsule);
        setStructureName(capsule, structureName);
        setAuthor(capsule, author);

        return capsule;
    }

    public static boolean isOneUse(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("oneUse") && stack.getTagCompound().getBoolean("oneUse");
    }

    public static void setOneUse(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.setItemDamage(STATE_ONE_USE);
        capsule.getTagCompound().setBoolean("oneUse", true);
    }

    public static boolean isReward(ItemStack stack) {
        return stack != null && (stack.hasTagCompound() && stack.getTagCompound().hasKey("isReward") && stack.getTagCompound().getBoolean("isReward") && isOneUse(stack));
    }

    public static void setIsReward(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }

        capsule.getTagCompound().setBoolean("isReward", true);
        setOneUse(capsule);
    }

    public static boolean isLinked(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("structureName");
    }

    public static String getLabel(ItemStack stack) {

        if (stack == null)
            return "";

        if (!isLinked(stack)) {
            return I18n.translateToLocal("item.capsule.content_empty");
        } else if (stack.hasTagCompound() && stack.getTagCompound().hasKey("label") && !"".equals(stack.getTagCompound().getString("label"))) {
            return "“" + TextFormatting.ITALIC + stack.getTagCompound().getString("label") + TextFormatting.RESET + "”";
        }
        return I18n.translateToLocal("item.capsule.content_unlabeled");
    }

    public static void setLabel(ItemStack capsule, String label) {
        if (capsule != null && !capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setString("label", label);
    }

    /**
     * The capsule capture size.
     */
    public static int getSize(ItemStack itemStackIn) {
        int size = 1;
        if (itemStackIn != null && itemStackIn.hasTagCompound() && itemStackIn.getTagCompound().hasKey("size")) {
            size = itemStackIn.getTagCompound().getInteger("size");
        }
        if (size > CAPSULE_MAX_CAPTURE_SIZE) {
            size = CAPSULE_MAX_CAPTURE_SIZE;
            itemStackIn.getTagCompound().setInteger("size", size);
            LOGGER.error("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
        } else if (size % 2 == 0) {
            size--;
            itemStackIn.getTagCompound().setInteger("size", size);
            LOGGER.error("Capsule size must be an odd number to achieve consistency on deployment. Resized to : " + size);
        }

        return size;
    }

    public static void setSize(ItemStack capsule, int size) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        if (size > CAPSULE_MAX_CAPTURE_SIZE) {
            size = CAPSULE_MAX_CAPTURE_SIZE;
            LOGGER.warn("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
        } else if (size % 2 == 0) {
            size--;
            LOGGER.warn("Capsule size must be an odd number to achieve consistency on deployment. Resized to : " + size);
        }
        capsule.getTagCompound().setInteger("size", size);
    }


    public static String getStructureName(ItemStack capsule) {
        String name = null;
        if (capsule != null && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("structureName")) {
            name = capsule.getTagCompound().getString("structureName");
        }
        return name;
    }

    public static void setStructureName(ItemStack capsule, String structureName) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setString("structureName", structureName);
    }

    public static String getAuthor(ItemStack capsule) {
        String name = null;
        if (capsule != null && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("author")) {
            name = capsule.getTagCompound().getString("author");
        }
        return name;
    }

    public static void setAuthor(ItemStack capsule, String author) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        if (!Strings.isNullOrEmpty(author)) capsule.getTagCompound().setString("author", author);
    }


    public static int getBaseColor(ItemStack capsule) {
        return Helpers.getColor(capsule);
    }

    public static void setBaseColor(ItemStack capsule, int color) {
        Helpers.setColor(capsule, color);
    }

    public static int getMaterialColor(ItemStack capsule) {
        int color = 0;
        if (capsule != null && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("color")) {
            color = capsule.getTagCompound().getInteger("color");
        }
        return color;
    }

    public static void setMaterialColor(ItemStack capsule, int color) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setInteger("color", color);
    }

    public static int getUpgradeLevel(ItemStack stack) {
        int upgradeLevel = 0;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("upgraded")) {
            upgradeLevel = stack.getTagCompound().getInteger("upgraded");
        }
        return upgradeLevel;
    }

    public static void setUpgradeLevel(ItemStack capsule, int upgrades) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setInteger("upgraded", upgrades);
    }

    public static Integer getDimension(ItemStack capsule) {
        Integer dim = null;
        if (capsule != null && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("spawnPosition")) {
            dim = capsule.getTagCompound().getCompoundTag("spawnPosition").getInteger("dim");
        }
        return dim;
    }

    public static void moveEntityItemToDeployPos(final EntityItem entityItem, final ItemStack capsule) {
        if (!capsule.hasTagCompound()) return;
        BlockPos dest = BlockPos.fromLong(capsule.getTagCompound().getLong("deployAt"));
        BlockPos centerDest = dest;
        double diffX = (centerDest.getX() + 0.5 - entityItem.posX);
        double diffZ = (centerDest.getZ() + 0.5 - entityItem.posZ);
        entityItem.motionX = diffX / (Math.abs(diffX) > 1 ? 10 : 3);
        entityItem.motionZ = diffZ / (Math.abs(diffZ) > 1 ? 10 : 3);
    }

    public static void setState(ItemStack stack, int state) {

        stack.setItemDamage(state);
    }

    /**
     * Throw an item and return the new EntityItem created. Simulated a drop
     * with stronger throw.
     */
    public static EntityItem throwCapsule(ItemStack itemStackIn, EntityPlayer playerIn, BlockPos destination) {
        double d0 = playerIn.posY - 0.30000001192092896D + (double) playerIn.getEyeHeight();
        EntityItem entityitem = new EntityItem(playerIn.worldObj, playerIn.posX, d0, playerIn.posZ, itemStackIn);
        entityitem.setPickupDelay(20);// cannot be picked up before deployment goes Yay or Nay
        entityitem.setThrower(playerIn.getName());

        if (destination != null) {

            itemStackIn.getTagCompound().setLong("deployAt", destination.toLong());

            moveEntityItemToDeployPos(entityitem, itemStackIn);

            double distance = playerIn.getPosition().distanceSq(new BlockPos(destination.getX(), playerIn.posY, destination.getZ()));
            double pitch = (-MathHelper.sin(playerIn.rotationPitch / 180.0F * (float) Math.PI));
            if (pitch < 0) pitch = 0.08;
            entityitem.motionY = pitch * 1.1 + (Math.min(distance / (18 * 18), 0.8)) + 0.1;

        } else {
            float f = 0.5F;
            entityitem.motionX = (double) (-MathHelper.sin(playerIn.rotationYaw / 180.0F * (float) Math.PI)
                    * MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
            entityitem.motionZ = (double) (MathHelper.cos(playerIn.rotationYaw / 180.0F * (float) Math.PI)
                    * MathHelper.cos(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f);
            entityitem.motionY = (double) (-MathHelper.sin(playerIn.rotationPitch / 180.0F * (float) Math.PI) * f + 0.1F);
        }

        playerIn.dropItemAndGetStack(entityitem);
        playerIn.inventory.mainInventory[playerIn.inventory.currentItem] = null;

        return entityitem;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = I18n.translateToLocal("item.capsule.name");

        String state = "";
        switch (stack.getItemDamage()) {
            case STATE_ACTIVATED:
            case STATE_EMPTY_ACTIVATED:
            case STATE_ONE_USE_ACTIVATED:
                state = TextFormatting.DARK_GREEN + I18n.translateToLocal("item.capsule.state_activated") + TextFormatting.RESET;
                break;
            case STATE_LINKED:
                state = "";
                break;
            case STATE_DEPLOYED:
                state = I18n.translateToLocal("item.capsule.state_deployed");
                break;
            case STATE_ONE_USE:
                if (isReward(stack)) {
                    state = I18n.translateToLocal("item.capsule.state_one_use");
                } else {
                    state = I18n.translateToLocal("item.capsule.state_recovery");
                }

                break;
        }

        if (state.length() > 0) {
            state = state + " ";
        }
        String content = getLabel(stack);
        if (content.length() > 0) {
            content = content + " ";
        }

        return TextFormatting.RESET + state + content + name;
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
        return isOverpowered(stack);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {

        if (isOverpowered(stack)) {
            tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("capsule.tooltip.overpowered") + TextFormatting.RESET);
        }

        int size = getSize(stack);
        String author = getAuthor(stack);
        if (author != null) {
            tooltip.add(TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + I18n.translateToLocal("capsule.tooltip.author") + " " + author + TextFormatting.RESET);
        }

        int upgradeLevel = getUpgradeLevel(stack);
        String sizeTxt = String.valueOf(size) + "×" + String.valueOf(size) + "×" + String.valueOf(size);
        if (upgradeLevel > 0) {
            sizeTxt += " (" + upgradeLevel + "/" + Config.upgradeLimit + " " + I18n.translateToLocal("capsule.tooltip.upgraded") + ")";
        }
        tooltip.add(I18n.translateToLocal("capsule.tooltip.size") + ": " + sizeTxt);

        if (stack.getItemDamage() == STATE_ONE_USE) {
            tooltip.add(I18n.translateToLocal("capsule.tooltip.one_use").trim());
        }


    }

    public boolean isOverpowered(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("overpowered") && stack.getTagCompound().getByte("overpowered") == (byte) 1;
    }

    /**
     * Register items in the creative tab
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {

        ItemStack ironCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, STATE_EMPTY);
        ironCapsule.setTagInfo("color", new NBTTagInt(0xCCCCCC));
        ironCapsule.setTagInfo("size", new NBTTagInt(Config.ironCapsuleSize));

        ItemStack goldCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, STATE_EMPTY);
        goldCapsule.setTagInfo("color", new NBTTagInt(0xFFD700));
        goldCapsule.setTagInfo("size", new NBTTagInt(Config.goldCapsuleSize));

        ItemStack diamondCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, STATE_EMPTY);
        diamondCapsule.setTagInfo("color", new NBTTagInt(0x00FFF2));
        diamondCapsule.setTagInfo("size", new NBTTagInt(Config.diamondCapsuleSize));

        ItemStack opCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, STATE_EMPTY);
        opCapsule.setTagInfo("color", new NBTTagInt(0xFFFFFF));
        opCapsule.setTagInfo("size", new NBTTagInt(Config.opCapsuleSize));
        opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));

        ItemStack unlabelledCapsule = ironCapsule.copy();
        unlabelledCapsule.setItemDamage(STATE_LINKED);
        unlabelledCapsule.getTagCompound().setString("structureName", "(C-CreativeLinkedCapsule)");

        ItemStack recoveryCapsule = ironCapsule.copy();
        CapsuleItem.setOneUse(recoveryCapsule);
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

        if (hand == EnumHand.OFF_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, itemStackIn);
        }

        if (playerIn.isSneaking() && (itemStackIn.getItemDamage() == STATE_LINKED || itemStackIn.getItemDamage() == STATE_DEPLOYED || itemStackIn.getItemDamage() == STATE_ONE_USE)) {
            Main.proxy.openGuiScreen(playerIn);
        } else if (!worldIn.isRemote) {

            // an empty or a linked capsule is activated on right click
            if (itemStackIn.getItemDamage() == STATE_EMPTY || itemStackIn.getItemDamage() == STATE_LINKED
                    || itemStackIn.getItemDamage() == STATE_ONE_USE) {
                if (itemStackIn.getItemDamage() == STATE_EMPTY) {
                    setState(itemStackIn, STATE_EMPTY_ACTIVATED);
                }
                if (itemStackIn.getItemDamage() == STATE_LINKED) {
                    setState(itemStackIn, STATE_ACTIVATED);
                }
                if (itemStackIn.getItemDamage() == STATE_ONE_USE) {
                    setState(itemStackIn, STATE_ONE_USE_ACTIVATED);
                }

                NBTTagCompound timer = itemStackIn.getSubCompound("activetimer", true);
                timer.setInteger("starttime", playerIn.ticksExisted);
            }

            // an opened capsule revoke deployed content on right click
            else if (itemStackIn.getItemDamage() == STATE_DEPLOYED) {

                try {
                    resentToCapsule(itemStackIn, playerIn);
                } catch (Exception e) {
                    LOGGER.error("Couldn't resend the content into the capsule", e);
                }

            }
        }

        if (worldIn.isRemote) {
            // client side, if is going to get activated, ask for server preview
            if (itemStackIn.hasTagCompound()
                    && (itemStackIn.getItemDamage() == STATE_LINKED || itemStackIn.getItemDamage() == STATE_ONE_USE)) {
                BlockPos anchor = Minecraft.getMinecraft().objectMouseOver.getBlockPos();
                if (anchor != null) {
                    CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleContentPreviewQueryToServer(itemStackIn.getTagCompound().getString("structureName")));
                }
            }

            // client side, is activated, ask for the server a throw at position
            if (isActivated(itemStackIn)) {
                RayTraceResult rtr = isLinked(itemStackIn) ? Helpers.clientRayTracePreview(playerIn, 0) : null;
                BlockPos dest = rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().add(rtr.sideHit.getDirectionVec()) : null;
                CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleThrowQueryToServer(dest));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, itemStackIn);
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
            NBTTagCompound timer = stack.getSubCompound("activetimer", false);

            if (this.isActivated(stack) && timer != null && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + ACTIVE_DURATION_IN_TICKS) {
                revertStateFromActivated(stack);
            }
        }
    }

    /**
     * Detect a collision and act accordingly (deploy or capture or break)
     */
    @Override
    public boolean onEntityItemUpdate(final EntityItem entityItem) {
        super.onEntityItemUpdate(entityItem);

        final ItemStack capsule = entityItem.getEntityItem();
        if (capsule == null) return false;

        // Deploying capsule content on collision with a block
        if (!entityItem.worldObj.isRemote
                && entityItem.isCollided
                && entityItem.ticksExisted > 2 // avoid immediate collision
                && this.isActivated(capsule)) {

            final int size = getSize(capsule);
            final int extendLength = (size - 1) / 2;

            // get destination world available position
            final WorldServer itemWorld = (WorldServer) entityItem.worldObj;

            if (isLinked(capsule)) {

                // DEPLOY
                // is linked, deploy
                boolean deployed = deployCapsule(entityItem, capsule, extendLength, itemWorld);
                if (deployed && isOneUse(capsule)) {
                    entityItem.setDead();
                }

            } else {

                // CAPTURE
                // is not linked, capture
                try {
                    captureContentIntoCapsule(entityItem, capsule, size, extendLength, itemWorld);
                } catch (Exception e) {
                    LOGGER.error("Couldn't capture the content into the capsule", e);
                }
            }
        }

        // throwing the capsule toward the right place
        if (!entityItem.worldObj.isRemote
                && !entityItem.isCollided
                && this.isActivated(capsule)
                && capsule.hasTagCompound()
                && capsule.getTagCompound().hasKey("deployAt")) {
            moveEntityItemToDeployPos(entityItem, capsule);

        }

        return false;
    }

    /**
     * Capture the content around the capsule entityItem, update capsule state.
     */
    private boolean captureContentIntoCapsule(EntityItem entityItem, ItemStack capsule, int size, int extendLength, WorldServer playerWorld) {

        // specify target to capture
        BlockPos marker = Helpers.findSpecificBlock(entityItem, size + 2, BlockCapsuleMarker.class);
        if (marker != null) {
            BlockPos source = marker.add(-extendLength, 1, -extendLength);

            // Save the region in a structure block file
            String player = "CapsuleMod";
            if (entityItem.getThrower() != null) {
                player = entityItem.getThrower();
            }
            String capsuleID = StructureSaver.getUniqueName(playerWorld, player);
            boolean storageOK = StructureSaver.store(playerWorld, entityItem.getThrower(), capsuleID, source, size, getExcludedBlocs(capsule), null);

            if (storageOK) {
                // register the link in the capsule
                setState(capsule, STATE_LINKED);
                CapsuleItem.setStructureName(capsule, capsuleID);
                return true;
            } else {
                LOGGER.error("Error occured during capture.");
                // send a chat message to explain failure
                if (entityItem.getThrower() != null){
                    EntityPlayer playerEntity = playerWorld.getPlayerEntityByName(entityItem.getThrower());
                    if (playerEntity != null) playerEntity.addChatMessage(new TextComponentTranslation("capsule.error.technicalError"));
                }
            }
        } else {
            revertStateFromActivated(capsule);
            // send a chat message to explain failure
            EntityPlayer player = playerWorld.getPlayerEntityByName(entityItem.getThrower());
            if (player != null) {
                player.addChatMessage(new TextComponentTranslation("capsule.error.noCaptureBase"));
            }
        }

        return false;
    }

    private Map<BlockPos, Block> getOccupiedSourcePos(ItemStack capsule) {
        Map<BlockPos, Block> occupiedSources = new HashMap<>();
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
        Map<BlockPos, Block> occupiedSources = new HashMap<>();
        NBTTagList entries = new NBTTagList();
        if (occupiedSpawnPositions != null) {
            for (Entry<BlockPos, Block> entry : occupiedSpawnPositions.entrySet()) {
                NBTTagCompound nbtEntry = new NBTTagCompound();
                nbtEntry.setLong("pos", entry.getKey().toLong());
                nbtEntry.setInteger("blockId", Block.getIdFromBlock(entry.getValue()));
                entries.appendTag(nbtEntry);
            }
        }
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setTag("occupiedSpawnPositions", entries);
        return occupiedSources;
    }

    private void revertStateFromActivated(ItemStack capsule) {
        if (isOneUse(capsule)) {
            setState(capsule, STATE_ONE_USE);
        } else if (isLinked(capsule)) {
            setState(capsule, STATE_LINKED);
        } else {
            setState(capsule, STATE_EMPTY);
        }
        if (capsule.hasTagCompound()) {
            capsule.getTagCompound().removeTag("activetimer");
        }
    }

    /**
     * Deploy the capsule at the entityItem position. update capsule state
     */
    private boolean deployCapsule(EntityItem entityItem, ItemStack capsule, int extendLength, WorldServer playerWorld) {
        // specify target to capture

        boolean didSpawn = false;

        BlockPos dest = null;
        if (capsule.hasTagCompound() && capsule.getTagCompound().hasKey("deployAt")) {
            BlockPos centerDest = BlockPos.fromLong(capsule.getTagCompound().getLong("deployAt"));
            dest = centerDest.add(-extendLength, 0, -extendLength);
            capsule.getTagCompound().removeTag("deployAt");
        } else {
            BlockPos bottomBlockPos = Helpers.findBottomBlock(entityItem);
            dest = bottomBlockPos.add(-extendLength, 1, -extendLength);
        }
        String structureName = capsule.getTagCompound().getString("structureName");

        // do the transportation
        Map<BlockPos, Block> occupiedSpawnPositions = new HashMap<>();
        List<String> outEntityBlocking = new ArrayList<>();

        boolean result = StructureSaver.deploy(capsule, playerWorld, entityItem.getThrower(), dest, Config.overridableBlocks, occupiedSpawnPositions, outEntityBlocking);

        if (result) {

            this.setOccupiedSourcePos(capsule, occupiedSpawnPositions);

            // register the link in the capsule
            if (!isReward(capsule)) {

                setState(capsule, STATE_DEPLOYED);
                savePosition("spawnPosition", capsule, dest, entityItem.getEntityWorld().provider.getDimension());
                // remove the content from the structure block to prevent dupe using recovery capsules
                StructureSaver.clearTemplate(playerWorld, structureName);
            }

            didSpawn = true;

        } else {
            // could not deploy, either entity or block preventing merge
            revertStateFromActivated(capsule);
        }

        return didSpawn;
    }

    private void resentToCapsule(final ItemStack itemStackIn, final EntityPlayer playerIn) {
        // store again
        Integer dimensionId = getDimension(itemStackIn);
        MinecraftServer server = playerIn.getServer();
        final WorldServer capsuleWorld = dimensionId != null ? server.worldServerForDimension(dimensionId) : (WorldServer) playerIn.getEntityWorld();

        NBTTagCompound spawnPos = itemStackIn.getTagCompound().getCompoundTag("spawnPosition");
        BlockPos source = new BlockPos(spawnPos.getInteger("x"), spawnPos.getInteger("y"), spawnPos.getInteger("z"));

        int size = getSize(itemStackIn);

        // do the transportation
        boolean storageOK = StructureSaver.store(capsuleWorld, playerIn.getName(), itemStackIn.getTagCompound().getString("structureName"), source, size, getExcludedBlocs(itemStackIn), getOccupiedSourcePos(itemStackIn));

        if (storageOK) {
            setState(itemStackIn, STATE_LINKED);
            itemStackIn.getTagCompound().removeTag("spawnPosition");
            itemStackIn.getTagCompound().removeTag("occupiedSpawnPositions"); // don't need anymore those data
        } else {
            LOGGER.error("Error occured during undeploy of capsule.");
            playerIn.addChatMessage(new TextComponentTranslation("capsule.error.technicalError"));
        }
    }

    public List<Block> getExcludedBlocs(ItemStack stack) {
        List<Block> excludedBlocks = Config.excludedBlocks;
        if (isOverpowered(stack)) {
            excludedBlocks = Config.opExcludedBlocks;
        }
        return excludedBlocks;
    }

    /**
     * renderPass 0 => The material color renderPass 1 => The label color
     */
    public int getColorFromItemstack(ItemStack stack, int renderPass) {
        int color = 0xFFFFFF;

        // material color on label surronding
        if (renderPass == 0) {
            color = Helpers.getColor(stack);

        } else if (renderPass == 1) {
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("color")) {
                color = stack.getTagCompound().getInteger("color");
            }

        } else if (renderPass == 2) {
            color = 0xFFFFFF;
        }
        return color;
    }

    /**
     * Set the NBT tag "key" to be a BlockPos coordinates.
     *
     * @param key     nbt tag key name
     * @param capsule capsule stack
     * @param dest    position to save as nbt into the capsule stack
     * @param dimID   dimension where the position is.
     */
    private void savePosition(String key, ItemStack capsule, BlockPos dest, int dimID) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound pos = new NBTTagCompound();
        pos.setInteger("x", dest.getX());
        pos.setInteger("y", dest.getY());
        pos.setInteger("z", dest.getZ());
        pos.setInteger("dim", dimID);
        capsule.getTagCompound().setTag(key, pos);
    }

    public void clearCapsule(ItemStack capsule) {
        setState(capsule, STATE_EMPTY);
        if (!capsule.hasTagCompound()) return;
        capsule.getTagCompound().removeTag("structureName");
    }


}
