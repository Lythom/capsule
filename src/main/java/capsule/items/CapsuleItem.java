package capsule.items;

import capsule.CommonProxy;
import capsule.Config;
import capsule.Main;
import capsule.StructureSaver;
import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Capsule;
import capsule.helpers.MinecraftNBT;
import capsule.helpers.Spacial;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleLeftClickQueryToServer;
import capsule.network.CapsuleThrowQueryToServer;
import capsule.recipes.BlueprintCapsuleRecipeFactory.BlueprintCapsuleRecipe;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
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
    public static final int CAPSULE_MAX_CAPTURE_SIZE = 255;
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleItem.class);// = 180 / PI
    public static final float TO_RAD = 0.017453292F;
    public static final float GRAVITY_PER_TICK = 0.04f;

    public static long lastRotationTime = 0;


    /**
     * Capsule Mod main item. Used to store region data to be deployed and undeployed.
     * <p>
     * Damage values :
     * see STATE_<state> constants.
     * <p>
     * NBTData reference:
     * int color                                                  // material color
     * tag display : {int color}                                  // base color
     * int size                                                   // odd number, size of the square side the capsule can hold
     * string label                                               // User customizable label
     * byte overpowered                                           // If the capsule can capture powerfull blocks
     * bool onUse                                                 // if the content of the template must be kept when capsule is deployed.
     * bool isReward                                              // if the template is located in the configured reward folder
     * string author                                              // Name of the player who created the structure. Set using commands.
     * string structureName                                       // name of the template file name without the .nbt extension.
     * // Lookup paths are /<worldsave>/structures/capsule for non-rewards, and structureName must contains the full path for rewards and loots
     * string prevStructureName                                   // Used to remove older unused blueprint templates
     * tag activetimer : {int starttime}                          // used to time the moment when the capsule must deactivate
     * tag spawnPosition : {int x, int y, int z, int dim    }     // location where the capsule is currently deployed
     * tag occupiedSpawnPositions : [{int blockId, long pos},…]   // remember what position not the recapture is block didn't change
     * long deployAt                                              // when thrown with preview, position to deploy the capsule to match preview
     * int upgraded                                               // How many upgrades the capsule has
     * tag sourceInventory : {int x, int y, int z, int dim    }   // [Blueprints] location of the linked inventory
     * string mirror                                              // [Blueprints] current mirror mode
     * string rotation                                            // [Blueprints] current rotation mode
     * arr ench:[0:{lvl:1s,id:101s}]
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
        return !stack.isEmpty() && stack.getItem() instanceof CapsuleItem && stack.hasTagCompound() && stack.getTagCompound().hasKey("sourceInventory");
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

    public static boolean isInstantAndUndeployed(ItemStack capsule) {
        return capsule.getItemDamage() == STATE_BLUEPRINT || (getSize(capsule) == 1 && capsule.getItemDamage() != STATE_DEPLOYED);
    }

    public static boolean hasStructureLink(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("structureName");
    }

    public static boolean isLinkedStateCapsule(ItemStack itemstack) {
        return (!itemstack.isEmpty() && itemstack.getItem() instanceof CapsuleItem && CapsuleItem.STATE_LINKED == itemstack.getItemDamage());
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
            size++;
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
            size++;
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
        if (!StringUtils.isNullOrEmpty(author)) capsule.getTagCompound().setString("author", author);
    }


    public static int getBaseColor(ItemStack capsule) {
        return MinecraftNBT.getColor(capsule);
    }

    public static void setBaseColor(ItemStack capsule, int color) {
        MinecraftNBT.setColor(capsule, color);
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

    public static void setState(ItemStack stack, int state) {
        stack.setItemDamage(state);
    }

    public static boolean isOverpowered(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("overpowered") && stack.getTagCompound().getByte("overpowered") == (byte) 1;
    }

    private static boolean isActivated(ItemStack capsule) {
        return capsule.getItemDamage() == STATE_ACTIVATED || capsule.getItemDamage() == STATE_EMPTY_ACTIVATED
                || capsule.getItemDamage() == STATE_ONE_USE_ACTIVATED;
    }

    public static void setCanRotate(ItemStack capsule, boolean canRotate) {
        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }
        capsule.getTagCompound().setBoolean("canRotate", canRotate);
    }

    public static boolean canRotate(ItemStack capsule) {
        return isBlueprint(capsule) || capsule.getItemDamage() != STATE_DEPLOYED && capsule.hasTagCompound() && capsule.getTagCompound().hasKey("canRotate") && capsule.getTagCompound().getBoolean("canRotate");
    }

    public static void revertStateFromActivated(ItemStack capsule) {
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
                if (isBlueprint(stack)) {
                    name = I18n.translateToLocal("item.capsule.state_blueprint");
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
                name = I18n.translateToLocal("item.capsule.state_blueprint");
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
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack capsule, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {

        String author = getAuthor(capsule);
        if (author != null) {
            tooltip.add(TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + I18n.translateToLocal("capsule.tooltip.author") + " " + author + TextFormatting.RESET);
        }

        if (capsule.getItemDamage() == STATE_ONE_USE) {
            tooltip.add(I18n.translateToLocal("capsule.tooltip.one_use").trim());
        }

        if (isOverpowered(capsule)) {
            tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("capsule.tooltip.overpowered") + TextFormatting.RESET);
        }

        int size = getSize(capsule);
        int upgradeLevel = getUpgradeLevel(capsule);
        String sizeTxt = size + "×" + size + "×" + size;
        if (upgradeLevel > 0) {
            sizeTxt += " (" + upgradeLevel + "/" + Config.upgradeLimit + " " + I18n.translateToLocal("capsule.tooltip.upgraded") + ")";
        }
        if (isInstantAndUndeployed(capsule) || isBlueprint(capsule)) {
            sizeTxt += " (" + I18n.translateToLocal("capsule.tooltip.instant").trim() + ")";
        }
        tooltip.add(I18n.translateToLocal("capsule.tooltip.size") + ": " + sizeTxt);


        if (isBlueprint(capsule)) {
            if (capsule.getItemDamage() == STATE_DEPLOYED) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.blueprintUseUncharged", TextFormatting.WHITE);
            } else {
                tooltipAddMultiline(tooltip, "capsule.tooltip.canRotate", TextFormatting.WHITE);
                tooltipAddMultiline(tooltip, "capsule.tooltip.blueprintUseCharged", TextFormatting.WHITE);

            }
        } else {
            if (canRotate(capsule)) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.canRotate", TextFormatting.WHITE);
            } else if (capsule.hasTagCompound() && capsule.getTagCompound().hasKey("canRotate")) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.cannotRotate", TextFormatting.DARK_GRAY);
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
            PlacementSettings p = getPlacement(capsule);
            tooltip.add(TextFormatting.GOLD + "⌯ Symmetry: " + Capsule.getMirrorLabel(p));
            tooltip.add(TextFormatting.GOLD + "⟳ Rotation: " + Capsule.getRotationLabel(p));
        }
    }

    public void tooltipAddMultiline(List<String> tooltip, String key) {
        tooltipAddMultiline(tooltip, key, null);
    }

    public void tooltipAddMultiline(List<String> tooltip, String key, TextFormatting formatting) {
        for (String s : I18n.translateToLocal(key).trim().split("\\\\n")) {
            tooltip.add(formatting == null ? s : formatting + s);
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
            for (Pair<ItemStack, BlueprintCapsuleRecipe> blueprintCapsule : CapsuleItems.blueprintCapsules) {
                subItems.add(blueprintCapsule.getKey());
            }
            if (CapsuleItems.unlabelledCapsule != null) subItems.add(CapsuleItems.unlabelledCapsule.getKey());
            if (CapsuleItems.recoveryCapsule != null) subItems.add(CapsuleItems.recoveryCapsule.getKey());
        }
    }


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
        if (event.getWorld().isRemote && stack.getItem() instanceof CapsuleItem && (CapsuleItem.isBlueprint(stack) || CapsuleItem.canRotate(stack))) {
            CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleLeftClickQueryToServer());
            askPreviewIfNeeded(stack);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.isCanceled()) {
            ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
            if (stack.getItem() instanceof CapsuleItem && CapsuleItem.canRotate(stack)) {
                event.setCanceled(true);
                if (event.getWorld().isRemote) {
                    if (lastRotationTime + 60 < Minecraft.getSystemTime()) {
                        lastRotationTime = Minecraft.getSystemTime();
                        // prevent action to be triggered on server + on client
                        CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleLeftClickQueryToServer());
                        askPreviewIfNeeded(stack);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void heldItemChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof EntityPlayer && event.getSlot().equals(EntityEquipmentSlot.MAINHAND) && isBlueprint(event.getTo())) {
            askPreviewIfNeeded(event.getTo());
        }
    }

    @SideOnly(Side.CLIENT)
    public void askPreviewIfNeeded(ItemStack stack) {
        if (!CapsulePreviewHandler.currentPreview.containsKey(getStructureName(stack))) {
            // try to get the preview from server
            CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleContentPreviewQueryToServer(getStructureName(stack)));
        }
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
            // a capsule is activated on right click, except instant that are deployed immediatly
            if (!isInstantAndUndeployed(capsule)) {
                activateCapsule(capsule, worldIn, playerIn);
            }
        } else if (worldIn.isRemote) {
            // client side, if is going to get activated, ask for server preview
            if (!isInstantAndUndeployed(capsule)
                    && (capsule.getItemDamage() == STATE_LINKED || capsule.getItemDamage() == STATE_ONE_USE)) {
                RayTraceResult rtr = hasStructureLink(capsule) ? Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule)) : null;
                BlockPos dest = rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().add(rtr.sideHit.getDirectionVec()) : null;
                if (dest != null) {
                    CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleContentPreviewQueryToServer(capsule.getTagCompound().getString("structureName")));
                }
            }

            // client side, is deployable, ask for the server a throw at position
            if (isInstantAndUndeployed(capsule)) {
                RayTraceResult rtr = Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule));
                BlockPos dest = null;
                if (rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK) {
                    if (capsule.getItemDamage() == STATE_EMPTY) {
                        dest = rtr.getBlockPos();
                    } else {
                        dest = rtr.getBlockPos().add(rtr.sideHit.getDirectionVec());
                    }
                }
                if (dest != null) {
                    CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleThrowQueryToServer(dest, true));
                }
            } else if (isActivated(capsule)) {
                RayTraceResult rtr = hasStructureLink(capsule) ? Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule)) : null;
                BlockPos dest = rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().add(rtr.sideHit.getDirectionVec()) : null;
                CommonProxy.simpleNetworkWrapper.sendToServer(new CapsuleThrowQueryToServer(dest, false));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, capsule);
    }

    public void activateCapsule(ItemStack capsule, World worldIn, EntityPlayer playerIn) {
        if (capsule.getItemDamage() == STATE_EMPTY) {
            setState(capsule, STATE_EMPTY_ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        } else if (capsule.getItemDamage() == STATE_LINKED) {
            setState(capsule, STATE_ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        } else if (capsule.getItemDamage() == STATE_ONE_USE) {
            setState(capsule, STATE_ONE_USE_ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        }
        // an open capsule undeploy content on right click
        else if (capsule.getItemDamage() == STATE_DEPLOYED) {
            try {
                Capsule.resentToCapsule(capsule, playerIn);
                worldIn.playSound(null, playerIn.getPosition(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
            } catch (Exception e) {
                LOGGER.error("Couldn't resend the content into the capsule", e);
            }
        }
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
            // special case that can happen in case of crash
            if (isActivated(stack) && !timer.hasKey("starttime")) {
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

        final ItemStack capsule = entityItem.getItem();
        if (capsule == null) return false;

        // Deploying capsule content on collision with a block
        if (!entityItem.getEntityWorld().isRemote
                && entityItem.ticksExisted > 2 // avoid immediate collision
                && isActivated(capsule)
                && (entityItem.collided || Spacial.entityItemShouldAndCollideLiquid(entityItem))
        ) {
            Capsule.handleEntityItemOnGround(entityItem, capsule);
        }

        // throwing the capsule toward the right place
        if (!entityItem.getEntityWorld().isRemote
                && isActivated(capsule)
                && capsule.hasTagCompound()
                && capsule.getTagCompound().hasKey("deployAt")
                && !entityItem.collided && !Spacial.entityItemShouldAndCollideLiquid(entityItem)) {
            Spacial.moveEntityItemToDeployPos(entityItem, capsule, true);
        }

        return false;
    }

    @Override
    public void onCreated(ItemStack capsule, World worldIn, EntityPlayer playerIn) {
        if (capsule.getItem() instanceof CapsuleItem && isBlueprint(capsule)) {
            String srcStructurePath = CapsuleItem.getStructureName(capsule);
            if (srcStructurePath != null && !worldIn.isRemote && !srcStructurePath.startsWith(StructureSaver.BLUEPRINT_PREFIX)) {
                String templateName = StructureSaver.createBlueprintTemplate(srcStructurePath, capsule, (WorldServer) worldIn, playerIn);
                // anyway we write the structure name
                // we dont want to have the same link as the original capsule
                CapsuleItem.setStructureName(capsule, templateName);
            }
        }
    }

    public static Map<BlockPos, Block> getOccupiedSourcePos(ItemStack capsule) {
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

    public static void setOccupiedSourcePos(ItemStack capsule, Map<BlockPos, Block> occupiedSpawnPositions) {
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

    // TODO: whitelist some blocks for blueprints
    // TODO: ignore flowing liquids
    // TODO: Add blueprint specific crafts (chick farm, starting base)

    public static void cleanDeploymentTags(ItemStack capsule) {
        capsule.getTagCompound().removeTag("spawnPosition");
        capsule.getTagCompound().removeTag("occupiedSpawnPositions"); // don't need anymore those data
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
            color = MinecraftNBT.getColor(stack);

        } else if (renderPass == 1) {
            if (isBlueprint(stack) && stack.getItemDamage() == STATE_DEPLOYED) {
                color = 0x7CC4EA; // trick for blueprint to reuse the "deployed" item model and get okish label color
            } else {
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("color")) {
                    color = stack.getTagCompound().getInteger("color");
                }
            }
        } else if (renderPass == 2) {
            if (isBlueprint(stack)) {
                color = 0x3BB3FC;
            } else {
                color = 0xFFFFFF;
            }
        }
        return color;
    }

    /**
     * Set the MinecraftNBT tag "key" to be a BlockPos coordinates.
     *
     * @param capsule capsule stack
     * @param dest    position to save as nbt into the capsule stack
     * @param dimID   dimension where the position is.
     */
    public static void saveSpawnPosition(ItemStack capsule, BlockPos dest, int dimID) {
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
     * Set the MinecraftNBT tag "sourceInventory" to be a BlockPos coordinates.
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

    @Nullable
    public static BlockPos getSourceInventoryLocation(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            NBTTagCompound sourceInventory = capsule.getTagCompound().getCompoundTag("sourceInventory");
            return new BlockPos(sourceInventory.getInteger("x"), sourceInventory.getInteger("y"), sourceInventory.getInteger("z"));
        }
        return null;
    }

    @Nullable
    public static Integer getSourceInventoryDimension(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            return capsule.getTagCompound().getCompoundTag("sourceInventory").getInteger("dim");
        }
        return null;
    }

    @Nullable
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
        return new PlacementSettings();
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
