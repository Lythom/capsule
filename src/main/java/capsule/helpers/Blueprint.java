package capsule.helpers;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.block.*;
import net.minecraft.block.BlockDoublePlant.EnumPlantType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fluids.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Blueprint {
    protected static final Logger LOGGER = LogManager.getLogger(Blueprint.class);

    public static ItemStack getBlockItemCost(Template.BlockInfo blockInfo) {
        final IBlockState state = blockInfo.blockState;
        Block block = state.getBlock();
        try {
            // prevent door to beeing counted twice
            if (block instanceof BlockDoor) {
                if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) {
                    return block.getItem(null, null, state);
                }
                return ItemStack.EMPTY;
            } else if (block instanceof BlockBed) {
                if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                    return new ItemStack(Items.BED, 1, blockInfo.tileentityData.getInteger("color"));
                }
                return ItemStack.EMPTY;
            } else if (block instanceof BlockDoublePlant) {
                final EnumPlantType type = state.getValue(BlockDoublePlant.VARIANT);
                if (type == EnumPlantType.FERN) {
                    return new ItemStack(Blocks.TALLGRASS, 2, BlockTallGrass.EnumType.FERN.getMeta());
                }
                if (type == EnumPlantType.GRASS) {
                    return new ItemStack(Blocks.TALLGRASS, 2, BlockTallGrass.EnumType.GRASS.getMeta());
                }
                return ItemStack.EMPTY;
            } else if (block instanceof BlockDoubleStoneSlab
                    || block instanceof BlockDoubleStoneSlabNew
                    || block instanceof BlockDoubleWoodSlab) {
                ItemStack stack = block.getItem(null, null, state);
                stack.setCount(2);
                return stack;

            } else if (block instanceof BlockLiquid) {
                if (isLiquidSource(state, block)) {
                    ItemStack item = FluidUtil.getFilledBucket(new FluidStack(FluidRegistry.lookupFluidForBlock(block), Fluid.BUCKET_VOLUME));
                    return item.isEmpty() ? null : item; // return null to indicate error
                }
                return ItemStack.EMPTY;
            } else if (block instanceof BlockPistonExtension
                    || block instanceof BlockPistonMoving) {
                return ItemStack.EMPTY;
            }
            return block.getItem(null, null, state);
        } catch (Exception e) {
            // some items requires world to have getItem work, here it produces NullPointerException. fallback to default break state of block.
            return new ItemStack(Item.getItemFromBlock(block), 1, block.damageDropped(state));
        }
    }

    public static boolean isLiquidSource(IBlockState state, Block block) {
        return block instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    @Nullable
    public static Map<StructureSaver.ItemStackKey, Integer> getMaterialList(ItemStack blueprint, WorldServer worldserver, EntityPlayer player) {
        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        CapsuleTemplateManager templatemanager = StructureSaver.getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate blueprintTemplate = templatemanager.get(minecraftserver, new ResourceLocation(CapsuleItem.getStructureName(blueprint)));
        if (blueprintTemplate == null) return null;
        Map<StructureSaver.ItemStackKey, Integer> list = new HashMap<>();

        for (Template.BlockInfo block : blueprintTemplate.blocks) {// Note: tile entities not supported so nbt data is not used here
            ItemStack itemStack = getBlockItemCost(block);
            StructureSaver.ItemStackKey stackKey = new StructureSaver.ItemStackKey(itemStack);
            if (itemStack == null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
                LOGGER.error("Unknown item during blueprint undo for block " + block.blockState.getBlock().getRegistryName());
                return null;
            } else if (!itemStack.isEmpty() && itemStack.getItem() != Items.AIR) {
                Integer currValue = list.get(stackKey);
                if (currValue == null) currValue = 0;
                list.put(stackKey, currValue + itemStack.getCount());
            }
        }
        // Note: entities not supported so no entities check
        return list;
    }
}
