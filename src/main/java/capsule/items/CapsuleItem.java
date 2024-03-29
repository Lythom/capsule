package capsule.items;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.MinecraftNBT;
import capsule.helpers.Spacial;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleLeftClickQueryToServer;
import capsule.network.CapsuleNetwork;
import capsule.network.CapsuleThrowQueryToServer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static capsule.items.CapsuleItem.CapsuleState.*;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings({"ConstantConditions"})
public class CapsuleItem extends Item {

    public static final int ACTIVE_DURATION_IN_TICKS = 60; // 3 sec at 20 ticks/sec

    public enum CapsuleState {
        EMPTY(0),
        EMPTY_ACTIVATED(4),
        ACTIVATED(1),
        LINKED(2),
        DEPLOYED(3),
        ONE_USE(5),
        ONE_USE_ACTIVATED(6),
        BLUEPRINT(7);

        private final int value;
        private static final Map<Integer, CapsuleState> map = new HashMap<>();

        CapsuleState(int value) {
            this.value = value;
        }

        static {
            for (CapsuleState state : CapsuleState.values()) {
                map.put(state.value, state);
            }
        }

        public static CapsuleState valueOf(int state) {
            return map.get(state);
        }

        public int getValue() {
            return value;
        }
    }

    public static final int CAPSULE_MAX_CAPTURE_SIZE = 255;
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleItem.class);// = 180 / PI
    public static final float TO_RAD = 0.017453292F;
    public static final float GRAVITY_PER_TICK = 0.04f;

    public static long lastRotationTime = 0;


    /**
     * Capsule Mod main item. Used to store region data to be deployed and undeployed.
     * NBTData reference:
     * int state                                                  // see CapsuleState enum values : EMPTY(0), EMPTY_ACTIVATED(4), ACTIVATED(1), LINKED(2), DEPLOYED(3), ONE_USE(5), ONE_USE_ACTIVATED(6), BLUEPRINT(7);
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
     * long deployAt                                              // when thrown with preview, position to deploy the capsule to match preview
     * int upgraded                                               // How many upgrades the capsule has
     * tag sourceInventory : {int x, int y, int z, int dim    }   // [Blueprints] location of the linked inventory
     * string mirror                                              // [Blueprints] current mirror mode
     * string rotation                                            // [Blueprints] current rotation mode
     * arr ench:[0:{lvl:1s,id:101s}]
     */
    public CapsuleItem() {
        super((new Item.Properties().tab(CapsuleMod.tabCapsule))
                .stacksTo(1)
                .durability(0)
                .setNoRepair());
    }


    public static boolean isOneUse(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("oneUse") && stack.getTag().getBoolean("oneUse");
    }

    public static void setOneUse(ItemStack capsule) {
        CapsuleItem.setState(capsule, CapsuleState.ONE_USE);
        capsule.getOrCreateTag().putBoolean("oneUse", true);
    }

    public static boolean isBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CapsuleItem && stack.hasTag() && stack.getTag().contains("sourceInventory");
    }

    public static void setBlueprint(ItemStack capsule) {
        saveSourceInventory(capsule, null, null);
    }

    public static boolean isReward(ItemStack stack) {
        return !stack.isEmpty() && (stack.hasTag() && stack.getTag().contains("isReward") && stack.getTag().getBoolean("isReward") && isOneUse(stack));
    }

    public static void setIsReward(ItemStack capsule) {
        capsule.getOrCreateTag().putBoolean("isReward", true);
        setOneUse(capsule);
    }

    public static boolean isInstantAndUndeployed(ItemStack capsule) {
        return CapsuleItem.hasState(capsule, BLUEPRINT) || (getSize(capsule) == 1 && !CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED));
    }

    public static boolean hasStructureLink(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("structureName");
    }

    public static boolean isLinkedStateCapsule(ItemStack itemstack) {
        return (!itemstack.isEmpty() && itemstack.getItem() instanceof CapsuleItem && CapsuleItem.hasState(itemstack, LINKED));
    }

    public static String getLabel(ItemStack stack) {

        if (stack.isEmpty())
            return "";

        if (!hasStructureLink(stack) && !CapsuleItem.hasState(stack, CapsuleState.LINKED)) {
            return I18n.get("items.capsule.content_empty");
        } else if (stack.hasTag() && stack.getTag().contains("label") && !"".equals(stack.getTag().getString("label"))) {
            return "«" + TextFormatting.ITALIC + stack.getTag().getString("label") + TextFormatting.RESET + "»";
        }
        return I18n.get("items.capsule.content_unlabeled");
    }

    public static void setLabel(ItemStack capsule, String label) {
        capsule.getOrCreateTag().putString("label", label);
    }

    /**
     * The capsule capture size.
     */
    public static int getSize(ItemStack capsule) {
        int size = 1;
        if (!capsule.isEmpty() && capsule.hasTag() && capsule.getTag().contains("size")) {
            size = capsule.getTag().getInt("size");
        }
        if (size > CAPSULE_MAX_CAPTURE_SIZE) {
            size = CAPSULE_MAX_CAPTURE_SIZE;
            capsule.getTag().putInt("size", size);
            LOGGER.error("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
        } else if (size % 2 == 0) {
            size++;
            capsule.getTag().putInt("size", size);
            LOGGER.error("Capsule size must be an odd number to achieve consistency on deployment. Resized to : " + size);
        }

        return size;
    }

    public static void setSize(ItemStack capsule, int size) {
        if (size > CAPSULE_MAX_CAPTURE_SIZE) {
            size = CAPSULE_MAX_CAPTURE_SIZE;
            LOGGER.warn("Capsule sizes are capped to " + CAPSULE_MAX_CAPTURE_SIZE + ". Resized to : " + size);
        } else if (size % 2 == 0) {
            size++;
            LOGGER.warn("Capsule size must be an odd number to achieve consistency on deployment. Resized to : " + size);
        }
        capsule.getOrCreateTag().putInt("size", size);
    }


    public static String getStructureName(ItemStack capsule) {
        String name = null;
        if (capsule != null && capsule.hasTag() && capsule.getTag().contains("structureName")) {
            name = capsule.getTag().getString("structureName");
        }
        return name;
    }

    public static void setStructureName(ItemStack capsule, String structureName) {
        if (!capsule.hasTag()) {
            capsule.setTag(new CompoundNBT());
        }
        capsule.getTag().putString("structureName", structureName);
    }

    public static String getAuthor(ItemStack capsule) {
        String name = null;
        if (capsule != null && capsule.hasTag() && capsule.getTag().contains("author")) {
            name = capsule.getTag().getString("author");
        }
        return name;
    }

    public static void setAuthor(ItemStack capsule, String author) {
        if (!capsule.hasTag()) {
            capsule.setTag(new CompoundNBT());
        }
        if (!StringUtils.isNullOrEmpty(author)) capsule.getTag().putString("author", author);
    }


    public static int getBaseColor(ItemStack capsule) {
        return MinecraftNBT.getColor(capsule);
    }

    public static void setBaseColor(ItemStack capsule, int color) {
        MinecraftNBT.setColor(capsule, color);
    }

    public static int getMaterialColor(ItemStack capsule) {
        int color = 0;
        if (capsule != null && capsule.hasTag() && capsule.getTag().contains("color")) {
            color = capsule.getTag().getInt("color");
        }
        return color;
    }

    public static void setMaterialColor(ItemStack capsule, int color) {
        if (!capsule.hasTag()) {
            capsule.setTag(new CompoundNBT());
        }
        capsule.getTag().putInt("color", color);
    }

    public static int getUpgradeLevel(ItemStack stack) {
        int upgradeLevel = 0;
        if (stack.hasTag() && stack.getTag().contains("upgraded")) {
            upgradeLevel = stack.getTag().getInt("upgraded");
        }
        return upgradeLevel;
    }

    public static void setUpgradeLevel(ItemStack capsule, int upgrades) {
        if (!capsule.hasTag()) {
            capsule.setTag(new CompoundNBT());
        }
        capsule.getTag().putInt("upgraded", upgrades);
    }

    public static RegistryKey<World> getDimension(ItemStack capsule) {
        RegistryKey<World> dim = null;
        if (capsule != null && capsule.hasTag() && capsule.getTag().contains("spawnPosition")) {
            dim = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(
                    capsule.getTag().getCompound("spawnPosition").getString("dim")
            ));
        }
        return dim;
    }

    public static void setState(ItemStack stack, CapsuleState state) {
        stack.getOrCreateTag().putInt("state", state.getValue());
    }

    public static boolean isOverpowered(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("overpowered") && stack.getTag().getByte("overpowered") == (byte) 1;
    }

    private static boolean isActivated(ItemStack capsule) {
        return CapsuleItem.hasState(capsule, CapsuleState.ACTIVATED) || CapsuleItem.hasState(capsule, CapsuleState.EMPTY_ACTIVATED)
                || CapsuleItem.hasState(capsule, CapsuleState.ONE_USE_ACTIVATED);
    }

    public static void setCanRotate(ItemStack capsule, boolean canRotate) {
        if (!capsule.hasTag()) {
            capsule.setTag(new CompoundNBT());
        }
        capsule.getTag().putBoolean("canRotate", canRotate);
    }

    public static boolean canRotate(ItemStack capsule) {
        return isBlueprint(capsule) || !CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED) && capsule.hasTag() && capsule.getTag().contains("canRotate") && capsule.getTag().getBoolean("canRotate");
    }

    public static void revertStateFromActivated(ItemStack capsule) {
        if (isBlueprint(capsule)) {
            setState(capsule, BLUEPRINT);
        } else if (isOneUse(capsule)) {
            setState(capsule, CapsuleState.ONE_USE);
        } else if (hasStructureLink(capsule)) {
            setState(capsule, CapsuleState.LINKED);
        } else {
            setState(capsule, CapsuleState.EMPTY);
        }
        if (capsule.hasTag()) {
            capsule.getTag().remove("activetimer");
        }
    }

    public static CapsuleState getState(ItemStack stack) {
        if (!stack.hasTag()) return CapsuleState.valueOf(0);
        if (stack.getTag().contains("state")) return CapsuleState.valueOf(stack.getTag().getInt("state"));
        // compatibility fallback
        return CapsuleState.valueOf(stack.getTag().getInt("Damage"));
    }

    public static boolean hasState(ItemStack stack, CapsuleState state) {
        return stack.hasTag() && stack.getTag().getInt("state") == state.getValue();
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ITextComponent getName(ItemStack stack) {
        String name = I18n.get("items.capsule.name");

        String state = "";
        switch (CapsuleItem.getState(stack)) {
            case ACTIVATED:
            case EMPTY_ACTIVATED:
            case ONE_USE_ACTIVATED:
                state = TextFormatting.DARK_GREEN + I18n.get("items.capsule.state_activated") + TextFormatting.RESET;
                break;
            case LINKED:
                state = "";
                break;
            case DEPLOYED:
                if (isBlueprint(stack)) {
                    name = I18n.get("items.capsule.state_blueprint");
                } else {
                    state = I18n.get("items.capsule.state_deployed");
                }
                break;
            case ONE_USE:
                if (isReward(stack)) {
                    state = I18n.get("items.capsule.state_one_use");
                } else {
                    state = I18n.get("items.capsule.state_recovery");
                }
                break;
            case BLUEPRINT:
                name = I18n.get("items.capsule.state_blueprint");
                break;
        }

        if (state.length() > 0) {
            state = state + " ";
        }
        String content = getLabel(stack);
        if (content.length() > 0) {
            content = content + " ";
        }

        return new StringTextComponent(TextFormatting.RESET + state + content + name);
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return getEnchantmentValue();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return !CapsuleItem.hasState(stack, CapsuleState.ONE_USE);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isOverpowered(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack capsule, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        String author = getAuthor(capsule);
        if (author != null) {
            tooltip.add(new StringTextComponent(TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + I18n.get("capsule.tooltip.author") + " " + author + TextFormatting.RESET));
        }

        if (CapsuleItem.hasState(capsule, CapsuleState.ONE_USE)) {
            tooltip.add(new StringTextComponent(I18n.get("capsule.tooltip.one_use").trim()));
        }

        if (isOverpowered(capsule)) {
            tooltip.add(new StringTextComponent(TextFormatting.DARK_PURPLE + I18n.get("capsule.tooltip.overpowered") + TextFormatting.RESET));
        }

        int size = getSize(capsule);
        int upgradeLevel = getUpgradeLevel(capsule);
        String sizeTxt = size + "×" + size + "×" + size;
        if (upgradeLevel > 0) {
            sizeTxt += " (" + upgradeLevel + "/" + Config.upgradeLimit + " " + I18n.get("capsule.tooltip.upgraded") + ")";
        }
        if (isInstantAndUndeployed(capsule) || isBlueprint(capsule)) {
            sizeTxt += " (" + I18n.get("capsule.tooltip.instant").trim() + ")";
        }
        tooltip.add(new StringTextComponent(I18n.get("capsule.tooltip.size") + ": " + sizeTxt));


        if (isBlueprint(capsule)) {
            if (CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED)) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.blueprintUseUncharged", TextFormatting.WHITE);
            } else {
                tooltipAddMultiline(tooltip, "capsule.tooltip.canRotate", TextFormatting.WHITE);
                tooltipAddMultiline(tooltip, "capsule.tooltip.blueprintUseCharged", TextFormatting.WHITE);

            }
        } else {
            if (canRotate(capsule)) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.canRotate", TextFormatting.WHITE);
            } else if (capsule.hasTag() && capsule.getTag().contains("canRotate")) {
                tooltipAddMultiline(tooltip, "capsule.tooltip.cannotRotate", TextFormatting.DARK_GRAY);
            }
        }
        if (flagIn == ITooltipFlag.TooltipFlags.ADVANCED) {
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "structureName: " + getStructureName(capsule)));
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "oneUse: " + isOneUse(capsule)));
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "isReward: " + isReward(capsule)));
            if (isBlueprint(capsule)) {
                tooltip.add(new StringTextComponent(TextFormatting.GOLD + "sourceInventory: " + getSourceInventoryLocation(capsule) + " in dimension " + getSourceInventoryDimension(capsule)));
            }
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "color (material): " + Integer.toHexString(getMaterialColor(capsule))));
            PlacementSettings p = getPlacement(capsule);
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "⌯ Symmetry: " + Capsule.getMirrorLabel(p)));
            tooltip.add(new StringTextComponent(TextFormatting.GOLD + "⟳ Rotation: " + Capsule.getRotationLabel(p)));
        }
    }

    public void tooltipAddMultiline(List<ITextComponent> tooltip, String key, TextFormatting formatting) {
        for (String s : I18n.get(key).trim().split("\\\\n")) {
            tooltip.add(new StringTextComponent(formatting == null ? s : formatting + s));
        }
    }

    /**
     * Register items in the creative tab
     */
    @Override
    public void fillItemCategory(ItemGroup tab, NonNullList<ItemStack> subItems) {
        if (this.allowdedIn(tab)) {
            // Add capsuleList items, loaded from json files
            subItems.addAll(CapsuleItems.capsuleList.keySet());
            subItems.addAll(CapsuleItems.opCapsuleList.keySet());
            if (CapsuleItems.unlabelledCapsule != null) subItems.add(CapsuleItems.unlabelledCapsule.getKey());
            if (CapsuleItems.deployedCapsule != null) subItems.add(CapsuleItems.deployedCapsule.getKey());
            if (CapsuleItems.recoveryCapsule != null) subItems.add(CapsuleItems.recoveryCapsule.getKey());
            if (CapsuleItems.blueprintChangedCapsule != null)
                subItems.add(CapsuleItems.blueprintChangedCapsule.getKey());
            for (Pair<ItemStack, ICraftingRecipe> blueprintCapsule : CapsuleItems.blueprintCapsules) {
                subItems.add(blueprintCapsule.getKey());
            }
            for (Pair<ItemStack, ICraftingRecipe> blueprintCapsule : CapsuleItems.blueprintPrefabs) {
                subItems.add(blueprintCapsule.getKey());
            }
        }
    }


    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemStack stack = event.getPlayer().getMainHandItem();
        if (event.getWorld().isClientSide && stack.getItem() instanceof CapsuleItem && (CapsuleItem.isBlueprint(stack) || CapsuleItem.canRotate(stack) && !CapsuleItem.hasState(stack, EMPTY))) {
            CapsuleNetwork.wrapper.sendToServer(new CapsuleLeftClickQueryToServer());
            askPreviewIfNeeded(stack);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.isCanceled()) {
            ItemStack stack = event.getPlayer().getMainHandItem();
            if (stack.getItem() instanceof CapsuleItem) {
                event.setCanceled(true);
                if (event.getWorld().isClientSide) {
                    if (CapsuleItem.canRotate(stack)) {
                        if (lastRotationTime + 60 < Util.getMillis()) {
                            lastRotationTime = Util.getMillis();
                            // prevent action to be triggered on server + on client
                            CapsuleNetwork.wrapper.sendToServer(new CapsuleLeftClickQueryToServer());
                            askPreviewIfNeeded(stack);
                        }
                    } else if (!CapsuleItem.hasState(stack, CapsuleState.DEPLOYED)) {
                        event.getPlayer().sendMessage(new TranslationTextComponent("capsule.tooltip.cannotRotate"), Util.NIL_UUID);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void heldItemChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof PlayerEntity && event.getSlot().equals(EquipmentSlotType.MAINHAND) && isInstantAndUndeployed(event.getTo())) {
            askPreviewIfNeeded(event.getTo());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void askPreviewIfNeeded(ItemStack stack) {
        if (!capsule.client.CapsulePreviewHandler.currentPreview.containsKey(getStructureName(stack))) {
            if (Minecraft.getInstance().getConnection() != null)
                // try to get the preview from server
                CapsuleNetwork.wrapper.sendToServer(new CapsuleContentPreviewQueryToServer(getStructureName(stack)));
        }
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        Hand hand = context.getHand();
        if (context.getHand() == Hand.OFF_HAND) {
            return ActionResultType.PASS;
        }
        PlayerEntity player = context.getPlayer();
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        ItemStack capsule = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isBlueprint(capsule)) {
            TileEntity te = world.getBlockEntity(pos);
            if (te != null && te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).isPresent()) {
                if (hasSourceInventory(capsule) && pos.equals(getSourceInventoryLocation(capsule)) && getSourceInventoryDimension(capsule).equals(world.dimension())) {
                    // remove if it was the same
                    saveSourceInventory(capsule, null, null);
                } else {
                    // new inventory
                    saveSourceInventory(capsule, pos, world.dimension());
                }
                return ActionResultType.SUCCESS;
            }
        }

        return super.onItemUseFirst(stack, context);
    }

    /**
     * Activate or power throw on right click.
     */
    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack capsule = playerIn.getItemInHand(handIn);

        if (handIn == Hand.OFF_HAND) {
            return new ActionResult<>(ActionResultType.FAIL, capsule);
        }

        if (playerIn.isShiftKeyDown() && (CapsuleItem.hasState(capsule, CapsuleState.LINKED) || CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED) || CapsuleItem.hasState(capsule, CapsuleState.ONE_USE) || CapsuleItem.hasState(capsule, BLUEPRINT))) {
            CapsuleMod.openGuiScreenCommon.accept(playerIn);

        } else if (!worldIn.isClientSide) {
            // a capsule is activated on right click, except instant that are deployed immediatly
            if (!isInstantAndUndeployed(capsule)) {
                activateCapsule(capsule, (ServerWorld) worldIn, playerIn);
            }
        } else if (worldIn.isClientSide) {
            // client side, if is going to get activated, ask for server preview
            if (!isInstantAndUndeployed(capsule)
                    && (CapsuleItem.hasState(capsule, CapsuleState.LINKED) || CapsuleItem.hasState(capsule, CapsuleState.ONE_USE))) {
                BlockRayTraceResult rtr = hasStructureLink(capsule) ? Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule)) : null;
                BlockPos dest = rtr != null && rtr.getType() == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().offset(rtr.getDirection().getNormal()) : null;
                if (dest != null) {
                    CapsuleNetwork.wrapper.sendToServer(new CapsuleContentPreviewQueryToServer(capsule.getTag().getString("structureName")));
                }
            }

            // client side, is deployable, ask for the server a throw at position
            if (isInstantAndUndeployed(capsule)) {
                BlockRayTraceResult rtr = Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule));
                BlockPos dest = null;
                if (rtr != null && rtr.getType() == RayTraceResult.Type.BLOCK) {
                    if (CapsuleItem.hasState(capsule, CapsuleState.EMPTY)) {
                        dest = rtr.getBlockPos();
                    } else {
                        dest = rtr.getBlockPos().offset(rtr.getDirection().getNormal());
                    }
                }
                if (dest != null) {
                    CapsuleNetwork.wrapper.sendToServer(new CapsuleThrowQueryToServer(dest, true));
                }
            } else if (isActivated(capsule)) {
                BlockRayTraceResult rtr = hasStructureLink(capsule) ? Spacial.clientRayTracePreview(playerIn, 0, getSize(capsule)) : null;
                BlockPos dest = rtr != null && rtr.getType() == RayTraceResult.Type.BLOCK ? rtr.getBlockPos().offset(rtr.getDirection().getNormal()) : null;
                CapsuleNetwork.wrapper.sendToServer(new CapsuleThrowQueryToServer(dest, false));
            }
        }

        return new ActionResult<>(ActionResultType.SUCCESS, capsule);
    }

    public void activateCapsule(ItemStack capsule, ServerWorld worldIn, PlayerEntity playerIn) {
        if (CapsuleItem.hasState(capsule, CapsuleState.EMPTY)) {
            setState(capsule, CapsuleState.EMPTY_ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        } else if (CapsuleItem.hasState(capsule, CapsuleState.LINKED)) {
            setState(capsule, CapsuleState.ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        } else if (CapsuleItem.hasState(capsule, CapsuleState.ONE_USE)) {
            setState(capsule, CapsuleState.ONE_USE_ACTIVATED);
            startTimer(worldIn, playerIn, capsule);
        }
        // an open capsule undeploy content on right click
        else if (CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED) && CapsuleItem.getDimension(capsule) != null) {
            try {
                Capsule.resentToCapsule(capsule, worldIn, playerIn);
                worldIn.playSound(null, playerIn.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
            } catch (Exception e) {
                LOGGER.error("Couldn't resend the content into the capsule", e);
            }
        }
    }

    private void startTimer(World worldIn, PlayerEntity playerIn, ItemStack capsule) {
        CompoundNBT timer = capsule.getOrCreateTagElement("activetimer");
        timer.putInt("starttime", playerIn.tickCount);
        worldIn.playSound(null, playerIn.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.2F, 0.9F);
    }


    /**
     * Manage the "activated" state of the capsule.
     */
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);

        if (!worldIn.isClientSide) {

            // disable capsule after some time
            CompoundNBT timer = stack.getTagElement("activetimer");

            if (timer != null && isActivated(stack) && timer.contains("starttime") && entityIn.tickCount >= timer.getInt("starttime") + ACTIVE_DURATION_IN_TICKS) {
                revertStateFromActivated(stack);
                worldIn.playSound(null, entityIn.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.2F, 0.4F);
            }
            // special case that can happen in case of crash
            if (isActivated(stack) && !timer.contains("starttime")) {
                revertStateFromActivated(stack);
            }
        }
    }

    /**
     * Detect a collision and act accordingly (deploy or capture or break)
     */
    @Override
    public boolean onEntityItemUpdate(ItemStack capsule, ItemEntity entity) {
        super.onEntityItemUpdate(capsule, entity);
        if (capsule == null) return false;

        // Deploying capsule content on collision with a block
        if (!entity.getCommandSenderWorld().isClientSide
                && entity.tickCount > 2 // avoid immediate collision
                && isActivated(capsule)
                && (entity.verticalCollision || entity.horizontalCollision || Spacial.ItemEntityShouldAndCollideLiquid(entity))
        ) {
            Capsule.handleItemEntityOnGround(entity, capsule);
        }

        // throwing the capsule toward the right place
        if (!entity.getCommandSenderWorld().isClientSide
                && isActivated(capsule)
                && capsule.hasTag() && capsule.getTag().contains("deployAt")
                && !entity.verticalCollision || entity.horizontalCollision && !Spacial.ItemEntityShouldAndCollideLiquid(entity)) {
            Spacial.moveItemEntityToDeployPos(entity, capsule, true);
        }

        return false;
    }

    /**
     * Hack: constantly check for player inventory to create a template if not exists.
     */
    @SubscribeEvent
    public static void onTickPlayerEvent(TickEvent.PlayerTickEvent event) {
        if (!event.player.level.isClientSide) {
            for (int i = 0; i < event.player.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = event.player.inventory.getItem(i);
                if (itemstack.hasTag() && itemstack.getTag().contains("templateShouldBeCopied")) {
                    duplicateBlueprintTemplate(itemstack, event.player.level, event.player);
                }
            }
        }
    }

    @Override
    public void onCraftedBy(ItemStack capsule, World worldIn, PlayerEntity playerIn) {
        duplicateBlueprintTemplate(capsule, worldIn, playerIn);
    }

    public static void duplicateBlueprintTemplate(ItemStack capsule, World worldIn, PlayerEntity playerIn) {
        if (!worldIn.isClientSide && capsule.getItem() instanceof CapsuleItem && isBlueprint(capsule)) {
            String srcStructurePath = CapsuleItem.getStructureName(capsule);
            if (srcStructurePath != null) {
                String templateName = StructureSaver.createBlueprintTemplate(srcStructurePath, capsule, (ServerWorld) worldIn, playerIn);
                // anyway we write the structure name
                // we dont want to have the same link as the original capsule
                CapsuleItem.setStructureName(capsule, templateName);
                if (capsule.getTag() != null) {
                    capsule.getTag().remove("templateShouldBeCopied");
                }
            }
        }
    }

    public static Map<BlockPos, Block> getOccupiedSourcePos(ItemStack capsule) {
        Map<BlockPos, Block> occupiedSources = null;
        if (capsule.hasTag() && capsule.getTag().contains("occupiedSpawnPositions")) {
            occupiedSources = new HashMap<>();
            ListNBT list = capsule.getTag().getList("occupiedSpawnPositions", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundNBT entry = list.getCompound(i);
                occupiedSources.put(BlockPos.of(entry.getLong("pos")), Block.stateById(entry.getInt("blockId")).getBlock());
            }
        }
        return occupiedSources;
    }

    public static void cleanDeploymentTags(ItemStack capsule) {
        capsule.getTag().remove("spawnPosition");
        capsule.getTag().remove("occupiedSpawnPositions"); // don't need anymore those data
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
            if (isBlueprint(stack) && CapsuleItem.hasState(stack, CapsuleState.DEPLOYED)) {
                color = 0x7CC4EA; // trick for blueprint to reuse the "deployed" item model and get okish label color
            } else {
                if (stack.hasTag() && stack.getTag().contains("color")) {
                    color = stack.getTag().getInt("color");
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
    public static void saveSpawnPosition(ItemStack capsule, BlockPos dest, String dimID) {
        CompoundNBT pos = new CompoundNBT();
        pos.putInt("x", dest.getX());
        pos.putInt("y", dest.getY());
        pos.putInt("z", dest.getZ());
        pos.putString("dim", dimID);
        capsule.getOrCreateTag().put("spawnPosition", pos);
    }

    /**
     * Set the MinecraftNBT tag "sourceInventory" to be a BlockPos coordinates.
     *
     * @param capsule capsule stack
     * @param dest    position to save as nbt into the capsule stack
     * @param dimID   dimension where the position is.
     */
    public static void saveSourceInventory(ItemStack capsule, BlockPos dest, RegistryKey<World> dimID) {
        CompoundNBT pos = new CompoundNBT();
        if (dest != null) {
            pos.putInt("x", dest.getX());
            pos.putInt("y", dest.getY());
            pos.putInt("z", dest.getZ());
            pos.putString("dim", dimID.location().toString());
        }
        capsule.getOrCreateTag().put("sourceInventory", pos);
    }

    public static boolean hasSourceInventory(ItemStack capsule) {
        return capsule.hasTag() && capsule.getTag().contains("sourceInventory") && capsule.getTag().getCompound("sourceInventory").contains("x");
    }

    @Nullable
    public static BlockPos getSourceInventoryLocation(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            CompoundNBT sourceInventory = capsule.getTag().getCompound("sourceInventory");
            return new BlockPos(sourceInventory.getInt("x"), sourceInventory.getInt("y"), sourceInventory.getInt("z"));
        }
        return null;
    }

    @Nullable
    public static RegistryKey<World> getSourceInventoryDimension(ItemStack capsule) {
        if (hasSourceInventory(capsule)) {
            return RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(
                    capsule.getTag().getCompound("sourceInventory").getString("dim")
            ));
        }
        return null;
    }

    @Nullable
    public static IItemHandler getSourceInventory(ItemStack blueprint, ServerWorld w) {
        BlockPos location = CapsuleItem.getSourceInventoryLocation(blueprint);
        RegistryKey<World> dimension = CapsuleItem.getSourceInventoryDimension(blueprint);
        if (location == null || dimension == null) return null;
        ServerWorld inventoryWorld = w.getServer().getLevel(dimension);

        TileEntity te = inventoryWorld.getBlockEntity(location);
        if (te != null) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
        }
        return null;
    }

    public static void setPlacement(ItemStack blueprint, PlacementSettings placementSettings) {
        if (!blueprint.hasTag()) {
            blueprint.setTag(new CompoundNBT());
        }
        blueprint.getTag().putString("rotation", placementSettings == null ? Rotation.NONE.name() : placementSettings.getRotation().name());
        blueprint.getTag().putString("mirror", placementSettings == null ? Mirror.NONE.name() : placementSettings.getMirror().name());
    }

    public static PlacementSettings getPlacement(ItemStack capsule) {
        if (hasPlacement(capsule)) {
            PlacementSettings placementSettings = new PlacementSettings()
                    .setMirror(Mirror.valueOf(capsule.getTag().getString("mirror")))
                    .setRotation(Rotation.valueOf(capsule.getTag().getString("rotation")))
                    .setIgnoreEntities(false)
                    .setChunkPos(null);
            return placementSettings;
        }
        return new PlacementSettings();
    }

    public static boolean hasPlacement(ItemStack blueprint) {
        if (!blueprint.hasTag()) {
            blueprint.setTag(new CompoundNBT());
        }
        return blueprint.getTag().contains("mirror") && blueprint.getTag().contains("rotation");
    }

    public static void clearCapsule(ItemStack capsule) {
        setState(capsule, CapsuleState.EMPTY);
        if (!capsule.hasTag()) return;
        capsule.getTag().remove("structureName");
        capsule.getTag().remove("sourceInventory");
        capsule.getTag().remove("canRotate");
    }
}
