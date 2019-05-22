package capsule.items;

import capsule.*;
import capsule.blocks.BlockCapsuleMarker;
import capsule.client.CapsulePreviewHandler;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleLeftClickQueryToServer;
import capsule.network.CapsuleThrowQueryToServer;
import capsule.structure.CapsuleTemplateManager;
import joptsimple.internal.Strings;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
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
    public final static int STATE_BLUEPRINT = 7;
    public final static int STATE_BLUEPRINT_ACTIVATED = 8;
    public static final int CAPSULE_MAX_CAPTURE_SIZE = 255;
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleItem.class);// = 180 / PI
    public static final float TO_RAD = 0.017453292F;
    public static final float MAX_BLOCKS_PER_TICK_THROW = 1.2f;
    public static final float GRAVITY_PER_TICK = 0.04f;
    public static final PlacementSettings DEFAULT_PLACEMENT = new PlacementSettings();


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
     * // Lookup paths are /<worldsave>/structures/capsule for non-rewards, and structureName must contains the full path for rewards and loots
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
        ItemStack capsule = new ItemStack(CapsuleItems.capsule, 1, STATE_EMPTY);
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
        setStructureName(capsule, structureName);
        setAuthor(capsule, author);

        return capsule;
    }

    public static boolean isOneUse(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("oneUse") && stack.getTagCompound().getBoolean("oneUse");
    }

    public static void setOneUse(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.setItemDamage(STATE_ONE_USE);
        capsule.getTagCompound().setBoolean("oneUse", true);
    }

    public static boolean isBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("sourceInventory");
    }

    public static void setBlueprint(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        saveSourceInventory(capsule, null, 0);
    }

    public static boolean isReward(ItemStack stack) {
        return !stack.isEmpty() && (stack.hasTagCompound() && stack.getTagCompound().hasKey("isReward") && stack.getTagCompound().getBoolean("isReward") && isOneUse(stack));
    }

    public static void setIsReward(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }

        capsule.getTagCompound().setBoolean("isReward", true);
        setOneUse(capsule);
    }

    public static boolean hasStructureLink(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("structureName");
    }

    public static boolean isLinkedStateCapsule(ItemStack itemstack) {
        return (!itemstack.isEmpty() && itemstack.getItem() instanceof CapsuleItem && CapsuleItem.STATE_LINKED == itemstack.getMetadata());
    }

    public static String getLabel(ItemStack stack) {

        if (stack.isEmpty())
            return "";

        if (!hasStructureLink(stack) && stack.getItemDamage() != STATE_LINKED) {
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
    public static int getSize(ItemStack capsule) {
        int size = 1;
        if (!capsule.isEmpty() && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("size")) {
            size = capsule.getTagCompound().getInteger("size");
        }
        if (size > CAPSULE_MAX_CAPTURE_SIZE) {
            size = CAPSULE_MAX_CAPTURE_SIZE;
            capsule.getTagCompound().setInteger("size", size);
            LOGGER.error("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
        } else if (size % 2 == 0) {
            size--;
            capsule.getTagCompound().setInteger("size", size);
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

    public static void moveEntityItemToDeployPos(final EntityItem entityItem, final ItemStack capsule, boolean keepMomentum) {
        if (!capsule.hasTagCompound()) return;
        BlockPos dest = BlockPos.fromLong(capsule.getTagCompound().getLong("deployAt"));
        // +0.5 to aim the center of the block
        double diffX = (dest.getX() + 0.5 - entityItem.posX);
        double diffZ = (dest.getZ() + 0.5 - entityItem.posZ);

        double distance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

        // velocity will slow down when approaching
        double requiredVelocity = distance / 10;
        double velocity = Math.min(requiredVelocity, MAX_BLOCKS_PER_TICK_THROW);
        double normalizedDiffX = (diffX / distance);
        double normalizedDiffZ = (diffZ / distance);

        // momentum allow to hit side walls
        entityItem.motionX = keepMomentum ? 0.9 * entityItem.motionX + 0.1 * normalizedDiffX * velocity : normalizedDiffX * velocity;
        entityItem.motionZ = keepMomentum ? 0.9 * entityItem.motionZ + 0.1 * normalizedDiffZ * velocity : normalizedDiffZ * velocity;
    }

    public static void setState(ItemStack stack, int state) {

        stack.setItemDamage(state);
    }

    public static boolean isOverpowered(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("overpowered") && stack.getTagCompound().getByte("overpowered") == (byte) 1;
    }

    private static boolean isActivated(ItemStack capsule) {
        return capsule.getItemDamage() == STATE_ACTIVATED || capsule.getItemDamage() == STATE_EMPTY_ACTIVATED
                || capsule.getItemDamage() == STATE_ONE_USE_ACTIVATED || capsule.getItemDamage() == STATE_BLUEPRINT_ACTIVATED;
    }

    private static void revertStateFromActivated(ItemStack capsule) {
        if (isBlueprint(capsule)) {
            setState(capsule, STATE_BLUEPRINT);
        } else if (isOneUse(capsule)) {
            setState(capsule, STATE_ONE_USE);
        } else if (hasStructureLink(capsule)) {
            setState(capsule, STATE_LINKED);
        } else {
            setState(capsule, STATE_EMPTY);
        }
        if (capsule.hasTagCompound()) {
            capsule.getTagCompound().removeTag("activetimer");
        }
    }

    public static boolean isThrowerUnderLiquid(final EntityItem entityItem) {
        String thrower = entityItem.getThrower();
        if (thrower == null) return false;
        EntityPlayer player = entityItem.getEntityWorld().getPlayerEntityByName(thrower);
        boolean underLiquid = Helpers.isImmergedInLiquid(player);
        return underLiquid;
    }

    public static boolean isEntityCollidingLiquid(final EntityItem entityItem) {
        return !entityItem.isOffsetPositionInLiquid(0, -0.1, 0);
    }

    public static boolean entityItemShouldAndCollideLiquid(final EntityItem entityItem) {
        boolean throwerInLiquid = isThrowerUnderLiquid(entityItem);
        boolean entityInLiquid = isEntityCollidingLiquid(entityItem);
        return !throwerInLiquid && entityInLiquid;
    }

    /**
     * Throw an item and return the new EntityItem created. Simulated a drop
     * with stronger throw.
     */
    public static EntityItem throwCapsule(ItemStack capsule, EntityPlayer playerIn, BlockPos destination) {
        // startPosition from EntityThrowable
        double startPosition = playerIn.posY - 0.3D + (double) playerIn.getEyeHeight();
        EntityItem entityitem = new EntityItem(playerIn.getEntityWorld(), playerIn.posX, startPosition, playerIn.posZ, capsule);
        entityitem.setPickupDelay(20);// cannot be picked up before deployment
        entityitem.setThrower(playerIn.getName());
        entityitem.setNoDespawn();

        if (destination != null) {

            capsule.getTagCompound().setLong("deployAt", destination.toLong());

            moveEntityItemToDeployPos(entityitem, capsule, false);
            BlockPos playerPos = playerIn.getPosition();
            // +0.5 to aim the center of the block
            double diffX = (destination.getX() + 0.5 - playerPos.getX());
            double diffZ = (destination.getZ() + 0.5 - playerPos.getZ());
            double flatDistance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);

            double diffY = destination.getY() - playerPos.getY() + Math.min(1, flatDistance / 3);
            double yVelocity = (diffY / 10) - (0.5 * 10 * -1 * GRAVITY_PER_TICK); // move up then down
            entityitem.motionY = Math.max(0.05, yVelocity);

        } else {
            float f = 0.5F;

            entityitem.motionX = (double) (-MathHelper.sin(playerIn.rotationYaw * TO_RAD) * MathHelper.cos(playerIn.rotationPitch * TO_RAD) * f) + playerIn.motionX;
            entityitem.motionZ = (double) (MathHelper.cos(playerIn.rotationYaw * TO_RAD) * MathHelper.cos(playerIn.rotationPitch * TO_RAD) * f) + playerIn.motionZ;
            entityitem.motionY = (double) (-MathHelper.sin(playerIn.rotationPitch * TO_RAD) * f + 0.1F) + playerIn.motionY;
        }

        playerIn.dropItemAndGetStack(entityitem);
        playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, ItemStack.EMPTY);
        playerIn.getEntityWorld().playSound(null, entityitem.getPosition(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.2F, 0.1f);

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
            case STATE_BLUEPRINT_ACTIVATED:
                state = TextFormatting.DARK_GREEN + I18n.translateToLocal("item.capsule.state_activated") + TextFormatting.RESET;
                break;
            case STATE_LINKED:
                state = "";
                break;
            case STATE_DEPLOYED:
                if (isBlueprint(stack)) {
                    state = I18n.translateToLocal("item.capsule.state_uncharged");
                } else {
                    state = I18n.translateToLocal("item.capsule.state_deployed");
                }
                break;
            case STATE_ONE_USE:
                if (isReward(stack)) {
                    state = I18n.translateToLocal("item.capsule.state_one_use");
                } else {
                    state = I18n.translateToLocal("item.capsule.state_recovery");
                }
                break;
            case STATE_BLUEPRINT:
                state = I18n.translateToLocal("item.capsule.state_blueprint");
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
    public boolean isEnchantable(ItemStack stack) {
        return stack.getItemDamage() != STATE_ONE_USE;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isOverpowered(stack);
    }


    @Override
    public void addInformation(ItemStack capsule, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

        if (capsule.getItemDamage() == STATE_ONE_USE) {
            tooltip.add(I18n.translateToLocal("capsule.tooltip.one_use").trim());
        }

        if (isOverpowered(capsule)) {
            tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("capsule.tooltip.overpowered") + TextFormatting.RESET);
        }

        int size = getSize(capsule);
        String author = getAuthor(capsule);
        if (author != null) {
            tooltip.add(TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + I18n.translateToLocal("capsule.tooltip.author") + " " + author + TextFormatting.RESET);
        }

        int upgradeLevel = getUpgradeLevel(capsule);
        String sizeTxt = size + "×" + size + "×" + size;
        if (upgradeLevel > 0) {
            sizeTxt += " (" + upgradeLevel + "/" + Config.upgradeLimit + " " + I18n.translateToLocal("capsule.tooltip.upgraded") + ")";
        }
        tooltip.add(I18n.translateToLocal("capsule.tooltip.size") + ": " + sizeTxt);


        if (isBlueprint(capsule)) {
            if (capsule.getItemDamage() == STATE_DEPLOYED) {
                tooltip.add(TextFormatting.WHITE + I18n.translateToLocal("capsule.tooltip.upgraded"));
            } else {
                tooltip.add(TextFormatting.WHITE + "* " + "Right click: to deploy");
            }
        }
        if (flagIn == ITooltipFlag.TooltipFlags.ADVANCED) {
            tooltip.add(TextFormatting.GOLD + "structureName: " + getStructureName(capsule));
            tooltip.add(TextFormatting.GOLD + "oneUse: " + isOneUse(capsule));
            tooltip.add(TextFormatting.GOLD + "isReward: " + isReward(capsule));
            if (isBlueprint(capsule)) {
                tooltip.add(TextFormatting.GOLD + "sourceInventory: " + getSourceInventoryLocation(capsule) + " in dimension " + getSourceInventoryDimension(capsule));
            }
            tooltip.add(TextFormatting.GOLD + "color (material): " + Integer.toHexString(getMaterialColor(capsule)));
        }
    }

    /**
     * Register items in the creative tab
     */
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            // Add capsuleList items, loaded from json files
            subItems.addAll(CapsuleItems.capsuleList.keySet());
            subItems.addAll(CapsuleItems.opCapsuleList.keySet());
            if (CapsuleItems.unlabelledCapsule != null) subItems.add(CapsuleItems.unlabelledCapsule.getKey());
            if (CapsuleItems.recoveryCapsule != null) subItems.add(CapsuleItems.recoveryCapsule.getKey());
            if (CapsuleItems.blueprintCapsule != null) subItems.add(CapsuleItems.blueprintCapsule.getKey());
        }
    }


    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
        if (stack.getItem() instanceof CapsuleItem && CapsuleItem.isBlueprint(stack)) {
            CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleLeftClickQueryToServer());
            if (!CapsulePreviewHandler.currentPreview.containsKey(getStructureName(stack))) {
                // try to get the preview from server
                CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleContentPreviewQueryToServer(getStructureName(stack)));
            }
        }
    }

    public static Map<StructureSaver.ItemStackKey, Integer> reloadBlueprint(ItemStack blueprint, WorldServer world, EntityLivingBase entity) {
        // list required materials
        Map<StructureSaver.ItemStackKey, Integer> missingMaterials = StructureSaver.getMaterialList(blueprint, world);
        // try to provision the materials from linked inventory or player inventory
        IItemHandler inv = CapsuleItem.getSourceInventory(blueprint, world);
        IItemHandler inv2 = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        Map<Integer, Integer> inv1SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv);
        Map<Integer, Integer> inv2SlotQuantityProvisions = recordSlotQuantityProvisions(missingMaterials, inv2);

        // if there is enough items, remove the provision items from inventories and recharge the capsule
        if (missingMaterials.size() == 0) {
            inv1SlotQuantityProvisions.forEach((slot, qty) -> {
                inv.extractItem(slot, qty, false);
            });
            inv2SlotQuantityProvisions.forEach((slot, qty) -> {
                inv2.extractItem(slot, qty, false);
            });
            CapsuleItem.setState(blueprint, STATE_BLUEPRINT);
            blueprint.getTagCompound().removeTag("spawnPosition");
            blueprint.getTagCompound().removeTag("occupiedSpawnPositions");
        }

        return missingMaterials;
    }

    /**
     * Tell which quantities should be extracted from which slot to ay the price.
     */
    private static Map<Integer, Integer> recordSlotQuantityProvisions(Map<StructureSaver.ItemStackKey, Integer> missingMaterials, final IItemHandler inv) {
        Map<Integer, Integer> invSlotQuantityProvisions = new HashMap<>();
        if (inv != null) {
            int size = inv.getSlots();
            for (int invSlot = 0; invSlot < size; invSlot++) {
                ItemStack invStack = inv.getStackInSlot(invSlot);
                StructureSaver.ItemStackKey stackKey = new StructureSaver.ItemStackKey(invStack);
                Integer missing = missingMaterials.get(stackKey);
                if (missing != null && missing > 0 && invStack.getCount() > 0) {
                    if (invStack.getCount() >= missing) {
                        missingMaterials.remove(stackKey);
                        invSlotQuantityProvisions.put(invSlot, missing);
                    } else {
                        missingMaterials.put(stackKey, missing - invStack.getCount());
                        invSlotQuantityProvisions.put(invSlot, invStack.getCount());
                    }
                }
            }
        }
        return invSlotQuantityProvisions;
    }


    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {

        if (hand == EnumHand.OFF_HAND) {
            return EnumActionResult.PASS;
        }
        ItemStack capsule = player.getHeldItem(hand);
        if (player.isSneaking() && isBlueprint(capsule)) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                if (hasSourceInventory(capsule) && pos.equals(getSourceInventoryLocation(capsule)) && getSourceInventoryDimension(capsule).equals(world.provider.getDimension())) {
                    // remove if it was the same
                    saveSourceInventory(capsule, null, 0);
                } else {
                    // new inventory
                    saveSourceInventory(capsule, pos, world.provider.getDimension());
                }
                return EnumActionResult.SUCCESS;
            }
        }

        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    /**
     * Activate or power throw on right click.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack capsule = playerIn.getHeldItem(handIn);

        if (handIn == EnumHand.OFF_HAND) {
            return new ActionResult<>(EnumActionResult.FAIL, capsule);
        }

        if (playerIn.isSneaking() && (capsule.getItemDamage() == STATE_LINKED || capsule.getItemDamage() == STATE_DEPLOYED || capsule.getItemDamage() == STATE_ONE_USE || capsule.getItemDamage() == STATE_BLUEPRINT)) {
            Main.proxy.openGuiScreen(playerIn);

        } else if (!worldIn.isRemote) {

            // an empty or a linked capsule is activated on right click
            if (capsule.getItemDamage() == STATE_EMPTY) {
                setState(capsule, STATE_EMPTY_ACTIVATED);
                startTimer(worldIn, playerIn, capsule);
            } else if (capsule.getItemDamage() == STATE_LINKED) {
                setState(capsule, STATE_ACTIVATED);
                startTimer(worldIn, playerIn, capsule);
            } else if (capsule.getItemDamage() == STATE_ONE_USE) {
                setState(capsule, STATE_ONE_USE_ACTIVATED);
                startTimer(worldIn, playerIn, capsule);
            } else if (capsule.getItemDamage() == STATE_BLUEPRINT) {
                setState(capsule, STATE_BLUEPRINT_ACTIVATED);
                startTimer(worldIn, playerIn, capsule);
            }

            // an open capsule undeploy content on right click
            else if (capsule.getItemDamage() == STATE_DEPLOYED) {

                try {
                    resentToCapsule(capsule, playerIn);
                    worldIn.playSound(null, playerIn.getPosition(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
                } catch (Exception e) {
                    LOGGER.error("Couldn't resend the content into the capsule", e);
                }

            }
        }

        if (worldIn.isRemote) {
            // client side, if is going to get activated, ask for server preview
            if (capsule.hasTagCompound()
                    && (capsule.getItemDamage() == STATE_LINKED || capsule.getItemDamage() == STATE_ONE_USE || capsule.getItemDamage() == STATE_BLUEPRINT)) {
                RayTraceResult rtr = hasStructureLink(capsule) ? Helpers.clientRayTracePreview(playerIn, 0) : null;
                BlockPos dest = rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().add(rtr.sideHit.getDirectionVec()) : null;
                if (dest != null) {
                    CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleContentPreviewQueryToServer(capsule.getTagCompound().getString("structureName")));
                }
            }

            // client side, is activated, ask for the server a throw at position
            if (isActivated(capsule)) {
                RayTraceResult rtr = hasStructureLink(capsule) ? Helpers.clientRayTracePreview(playerIn, 0) : null;
                BlockPos dest = rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().add(rtr.sideHit.getDirectionVec()) : null;
                CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleThrowQueryToServer(dest));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, capsule);
    }

    private void startTimer(World worldIn, EntityPlayer playerIn, ItemStack capsule) {
        NBTTagCompound timer = capsule.getOrCreateSubCompound("activetimer");
        timer.setInteger("starttime", playerIn.ticksExisted);
        worldIn.playSound(null, playerIn.getPosition(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.2F, 0.9F);
    }


    /**
     * Manage the "activated" state of the capsule.
     */
    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

        if (!worldIn.isRemote) {

            // disable capsule after some time
            NBTTagCompound timer = stack.getSubCompound("activetimer");

            if (timer != null && isActivated(stack) && timer.hasKey("starttime") && entityIn.ticksExisted >= timer.getInteger("starttime") + ACTIVE_DURATION_IN_TICKS) {
                revertStateFromActivated(stack);
                worldIn.playSound(null, entityIn.getPosition(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
            }
        }
    }

    /**
     * Detect a collision and act accordingly (deploy or capture or break)
     */
    @Override
    public boolean onEntityItemUpdate(final EntityItem entityItem) {
        super.onEntityItemUpdate(entityItem);

        final ItemStack capsule = entityItem.getItem();
        if (capsule == null) return false;

        // Deploying capsule content on collision with a block
        if (!entityItem.getEntityWorld().isRemote
                && entityItem.ticksExisted > 2 // avoid immediate collision
                && isActivated(capsule)
                && (entityItem.collided || entityItemShouldAndCollideLiquid(entityItem))
        ) {

            // stop the capsule where it collided
            entityItem.motionX = 0;
            entityItem.motionZ = 0;
            entityItem.motionY = 0;

            final int size = getSize(capsule);
            final int extendLength = (size - 1) / 2;

            // get destination world available position
            final WorldServer itemWorld = (WorldServer) entityItem.getEntityWorld();

            if (hasStructureLink(capsule)) {

                // DEPLOY
                // is linked, deploy
                boolean deployed = deployCapsule(entityItem, capsule, extendLength, itemWorld);
                if (deployed) {
                    itemWorld.playSound(null, entityItem.getPosition(), SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.BLOCKS, 0.4F, 0.1F);
                    showDeployParticules(itemWorld, entityItem.getPosition(), size);
                }
                if (deployed && isOneUse(capsule)) {
                    entityItem.setDead();
                }

            } else {

                // CAPTURE
                // is not linked, capture
                try {
                    boolean captured = captureContentIntoCapsule(entityItem, capsule, size, extendLength, itemWorld);
                    if (captured) {
                        showUndeployParticules(itemWorld, entityItem.getPosition(), size);
                    }
                } catch (Exception e) {
                    LOGGER.error("Couldn't capture the content into the capsule", e);
                }
            }

        }

        // throwing the capsule toward the right place
        if (!entityItem.getEntityWorld().isRemote
                && isActivated(capsule)
                && capsule.hasTagCompound()
                && capsule.getTagCompound().hasKey("deployAt")
                && !entityItem.collided && !entityItemShouldAndCollideLiquid(entityItem)) {
            moveEntityItemToDeployPos(entityItem, capsule, true);
        }

        return false;
    }

    @Override
    public void onCreated(ItemStack capsule, World worldIn, EntityPlayer playerIn) {
        String sourceStructureName = CapsuleItem.getStructureName(capsule);
        if (!worldIn.isRemote && !sourceStructureName.startsWith(StructureSaver.BLUEPRINT_PREFIX)) {
            WorldServer worldServer = (WorldServer) worldIn;
            String destStructureName = StructureSaver.getBlueprintUniqueName(worldServer) + "-" + sourceStructureName.replace("/", "_");
            ItemStack source = new ItemStack(CapsuleItems.capsule, 1, STATE_LINKED);
            CapsuleItem.setStructureName(source, sourceStructureName);
            if (sourceStructureName.startsWith(Config.rewardTemplatesPath)) CapsuleItem.setIsReward(source);
            boolean created = StructureSaver.copyFromCapsuleTemplate(
                    worldServer,
                    source,
                    StructureSaver.getTemplateManager(worldServer),
                    destStructureName
            );
            // anyway we write the structure name
            // we dont want to have the same link as the original capsule
            CapsuleItem.setStructureName(capsule, destStructureName);

            // try to cleaup previous template to save disk space on the long run
            if (capsule.getTagCompound().hasKey("prevStructureName")) {
                CapsuleTemplateManager tm = StructureSaver.getTemplateManager(worldServer);
                if (tm != null)
                    tm.deleteTemplate(worldServer.getMinecraftServer(), new ResourceLocation(capsule.getTagCompound().getString("prevStructureName")));
            }

            if (!created && playerIn != null) {
                playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintCreationError"));
            }
        }
    }

    private void showDeployParticules(WorldServer world, BlockPos blockpos, int size) {
        double d0 = (double) ((float) blockpos.getX()) + 0.5D;
        double d1 = (double) ((float) blockpos.getY()) + 0.5D;
        double d2 = (double) ((float) blockpos.getZ()) + 0.5D;
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d0, d1, d2, 16 * size, 0.5D, 0.25D, 0.5D, 0.1 + 0.01 * size);
    }

    private void showUndeployParticules(WorldServer world, BlockPos blockpos, int size) {
        double d0 = (double) ((float) blockpos.getX()) + 0.5D;
        double d1 = (double) ((float) blockpos.getY()) - size * 0.10;
        double d2 = (double) ((float) blockpos.getZ()) + 0.5D;

        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d0, d1, d2, 8 * size, 0.5D, 0.25D, 0.5D, 0.01 * size);
    }

    /**
     * Capture the content around the capsule entityItem, update capsule state.
     */
    private static boolean captureContentIntoCapsule(EntityItem entityItem, ItemStack capsule, int size, int extendLength, WorldServer playerWorld) {

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
                // could not capture, StructureSaver.store handles the feedback already
                revertStateFromActivated(capsule);
            }
        } else {
            revertStateFromActivated(capsule);
            // send a chat message to explain failure
            EntityPlayer player = playerWorld.getPlayerEntityByName(entityItem.getThrower());
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.noCaptureBase"));
            }
        }

        return false;
    }

    private static Map<BlockPos, Block> getOccupiedSourcePos(ItemStack capsule) {
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

    private static void setOccupiedSourcePos(ItemStack capsule, Map<BlockPos, Block> occupiedSpawnPositions) {
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
    }


    /**
     * Deploy the capsule at the entityItem position. update capsule state
     */
    private static boolean deployCapsule(EntityItem entityItem, ItemStack capsule, int extendLength, WorldServer playerWorld) {
        // specify target to capture

        boolean didSpawn = false;

        BlockPos dest;
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

        if (isBlueprint(capsule)) {
            // TODO: allow rotation and mirror
            // TODO: ADD HUD dispay
            // TODO: Add starting capsule base for players
            // TODO: Add blueprint specific crafts (chick farm, starting base)
        }
        boolean result = StructureSaver.deploy(capsule, playerWorld, entityItem.getThrower(), dest, Config.overridableBlocks, occupiedSpawnPositions, outEntityBlocking, getPlacement(capsule));

        if (result) {

            setOccupiedSourcePos(capsule, occupiedSpawnPositions);

            // register the link in the capsule
            if (!isReward(capsule)) {
                saveSpawnPosition(capsule, dest, entityItem.getEntityWorld().provider.getDimension());
                setState(capsule, STATE_DEPLOYED);
                if (!isBlueprint(capsule)) {
                    // remove the content from the structure block to prevent dupe using recovery capsule
                    StructureSaver.clearTemplate(playerWorld, structureName);
                }
            }

            didSpawn = true;

        } else {
            // could not deploy, either entity or block preventing merge
            revertStateFromActivated(capsule);
        }

        return didSpawn;
    }

    private void resentToCapsule(final ItemStack capsule, final EntityPlayer playerIn) {
        // store again
        Integer dimensionId = getDimension(capsule);
        MinecraftServer server = playerIn.getServer();
        final WorldServer world = dimensionId != null ? server.getWorld(dimensionId) : (WorldServer) playerIn.getEntityWorld();

        NBTTagCompound spawnPos = capsule.getTagCompound().getCompoundTag("spawnPosition");
        BlockPos startPos = new BlockPos(spawnPos.getInteger("x"), spawnPos.getInteger("y"), spawnPos.getInteger("z"));

        int size = getSize(capsule);

        // do the transportation
        if (isBlueprint(capsule)) {
            boolean blueprintMatch = StructureSaver.undeployBlueprint(world, playerIn.getName(), capsule.getTagCompound().getString("structureName"), startPos, size, getExcludedBlocs(capsule), getOccupiedSourcePos(capsule));
            if (blueprintMatch) {
                setState(capsule, STATE_BLUEPRINT);
                capsule.getTagCompound().removeTag("spawnPosition");
                capsule.getTagCompound().removeTag("occupiedSpawnPositions"); // don't need anymore those data
                showUndeployParticules(world, startPos.add(size / 2, size / 2, size / 2), size);
            } else {
                playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintDontMatch"));
            }
        } else {
            boolean storageOK = StructureSaver.store(world, playerIn.getName(), capsule.getTagCompound().getString("structureName"), startPos, size, getExcludedBlocs(capsule), getOccupiedSourcePos(capsule));
            if (storageOK) {
                setState(capsule, STATE_LINKED);
                capsule.getTagCompound().removeTag("spawnPosition");
                capsule.getTagCompound().removeTag("occupiedSpawnPositions"); // don't need anymore those data
                showUndeployParticules(world, startPos.add(size / 2, size / 2, size / 2), size);
            } else {
                LOGGER.error("Error occured during undeploy of capsule.");
                playerIn.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
            }
        }
    }

    public static List<Block> getExcludedBlocs(ItemStack stack) {
        List<Block> excludedBlocks = Config.excludedBlocks;
        if (isOverpowered(stack)) {
            excludedBlocks = Config.opExcludedBlocks;
        }
        return excludedBlocks;
    }

    /**
     * renderPass 0 => The material color renderPass 1 => The label color
     */
    public static int getColorFromItemstack(ItemStack stack, int renderPass) {
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
     * @param capsule capsule stack
     * @param dest    position to save as nbt into the capsule stack
     * @param dimID   dimension where the position is.
     */
    private static void saveSpawnPosition(ItemStack capsule, BlockPos dest, int dimID) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound pos = new NBTTagCompound();
        pos.setInteger("x", dest.getX());
        pos.setInteger("y", dest.getY());
        pos.setInteger("z", dest.getZ());
        pos.setInteger("dim", dimID);
        capsule.getTagCompound().setTag("spawnPosition", pos);
    }

    /**
     * Set the NBT tag "sourceInventory" to be a BlockPos coordinates.
     *
     * @param capsule capsule stack
     * @param dest    position to save as nbt into the capsule stack
     * @param dimID   dimension where the position is.
     */
    public static void saveSourceInventory(ItemStack capsule, BlockPos dest, int dimID) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound pos = new NBTTagCompound();
        if (dest != null) {
            pos.setInteger("x", dest.getX());
            pos.setInteger("y", dest.getY());
            pos.setInteger("z", dest.getZ());
            pos.setInteger("dim", dimID);
        }
        capsule.getTagCompound().setTag("sourceInventory", pos);
    }

    public static boolean hasSourceInventory(ItemStack capsule) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        return capsule.getTagCompound().hasKey("sourceInventory") && capsule.getTagCompound().getCompoundTag("sourceInventory").hasKey("x");
    }

    public static BlockPos getSourceInventoryLocation(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            NBTTagCompound sourceInventory = capsule.getTagCompound().getCompoundTag("sourceInventory");
            return new BlockPos(sourceInventory.getInteger("x"), sourceInventory.getInteger("y"), sourceInventory.getInteger("z"));
        }
        return null;
    }

    public static Integer getSourceInventoryDimension(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            return capsule.getTagCompound().getCompoundTag("sourceInventory").getInteger("dim");
        }
        return null;
    }

    public static IItemHandler getSourceInventory(ItemStack blueprint, WorldServer w) {
        BlockPos location = CapsuleItem.getSourceInventoryLocation(blueprint);
        Integer dimension = CapsuleItem.getSourceInventoryDimension(blueprint);
        if (location == null || dimension == null) return null;
        WorldServer world = w.getMinecraftServer().getWorld(dimension);

        TileEntity te = world.getTileEntity(location);
        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }
        return null;
    }

    public static void setPlacement(ItemStack blueprint, PlacementSettings placementSettings) {
        if (!blueprint.hasTagCompound()) {
            blueprint.setTagCompound(new NBTTagCompound());
        }
        blueprint.getTagCompound().setString("rotation", placementSettings == null ? Rotation.NONE.name() : placementSettings.getRotation().name());
        blueprint.getTagCompound().setString("mirror", placementSettings == null ? Mirror.NONE.name() : placementSettings.getMirror().name());
    }

    public static PlacementSettings getPlacement(ItemStack capsule) {
        if (hasPlacement(capsule)) {
            PlacementSettings placementSettings = new PlacementSettings()
                    .setMirror(Mirror.valueOf(capsule.getTagCompound().getString("mirror")))
                    .setRotation(Rotation.valueOf(capsule.getTagCompound().getString("rotation")))
                    .setIgnoreEntities(false)
                    .setChunk(null)
                    .setReplacedBlock(null)
                    .setIgnoreStructureBlock(false);
            return placementSettings;
        }
        return DEFAULT_PLACEMENT;
    }

    public static boolean hasPlacement(ItemStack blueprint) {
        if (!blueprint.hasTagCompound()) {
            blueprint.setTagCompound(new NBTTagCompound());
        }
        return blueprint.getTagCompound().hasKey("mirror") && blueprint.getTagCompound().hasKey("rotation");
    }

    public static void clearCapsule(ItemStack capsule) {
        setState(capsule, STATE_EMPTY);
        if (!capsule.hasTagCompound()) return;
        capsule.getTagCompound().removeTag("structureName");
        capsule.getTagCompound().removeTag("sourceInventory");
    }


}
