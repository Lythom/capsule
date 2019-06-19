package capsule.helpers;

import capsule.Config;
import capsule.StructureSaver;
import capsule.StructureSaver.ItemStackKey;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.gson.JsonObject;
import net.minecraft.block.*;
import net.minecraft.block.BlockDoublePlant.EnumPlantType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class Blueprint {
    protected static final Logger LOGGER = LogManager.getLogger(Blueprint.class);

    public static ItemStack getBlockItemCost(Template.BlockInfo blockInfo) {
        final IBlockState state = blockInfo.blockState;
        Block block = state.getBlock();
        NBTTagCompound blockNBT = blockInfo.tileentityData;
        try {
            // prevent door to beeing counted twice
            if (block instanceof BlockDoor) {
                if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) {
                    return block.getItem(null, null, state);
                }
                return ItemStack.EMPTY; // door upper is free, only lower counts.

            } else if (block instanceof BlockBed) {
                if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                    return new ItemStack(Items.BED, 1, blockInfo.tileentityData.getInteger("color"));
                }
                return ItemStack.EMPTY; // Bed foot is free, only head counts.

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

            } else if (block instanceof BlockFarmland) {
                return new ItemStack(Blocks.DIRT);

            } else if (block instanceof BlockLiquid) {
                if (isLiquidSource(state, block)) {
                    ItemStack item = FluidUtil.getFilledBucket(new FluidStack(FluidRegistry.lookupFluidForBlock(block), Fluid.BUCKET_VOLUME));
                    return item.isEmpty() ? null : item; // return null to indicate error
                }
                return ItemStack.EMPTY; //flowing liquid is free

            } else if (block instanceof BlockPistonExtension
                    || block instanceof BlockPistonMoving) {
                return ItemStack.EMPTY; // Piston extension is free
            }
            ItemStack item = block.getItem(null, null, state);
            if (blockNBT != null) {
                if (blockNBT.hasKey("dummy") && blockNBT.getBoolean("dummy"))
                    return ItemStack.EMPTY; // second part of Immersive engineering extended block.
                NBTTagCompound itemNBT = new NBTTagCompound();
                JsonObject allowedNBT = Config.getBlueprintAllowedNBT(block);
                for (String key : blockNBT.getKeySet()) {
                    if (allowedNBT.has(key) && !allowedNBT.get(key).isJsonNull()) {
                        String targetKey = allowedNBT.get(key).getAsString();
                        itemNBT.setTag(targetKey, blockNBT.getTag(key));
                    }
                }
                if (itemNBT.getSize() > 0) {
                    item.setTagCompound(itemNBT);
                }
            }
            return item;
        } catch (Exception e) {
            // some items requires world to have getItem work, here it produces NullPointerException. fallback to default break state of block.
            return new ItemStack(Item.getItemFromBlock(block), 1, block.damageDropped(state));
        }
    }

    public static boolean isLiquidSource(IBlockState state, Block block) {
        return block instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    @Nullable
    public static Map<ItemStackKey, Integer> getMaterialList(ItemStack blueprint, WorldServer
            worldserver, EntityPlayer player) {
        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        CapsuleTemplateManager templatemanager = StructureSaver.getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate blueprintTemplate = templatemanager.get(minecraftserver, new ResourceLocation(CapsuleItem.getStructureName(blueprint)));
        if (blueprintTemplate == null) return null;

        return getMaterialList(blueprintTemplate, player);
    }

    public static Map<ItemStackKey, Integer> getMaterialList(CapsuleTemplate blueprintTemplate, @Nullable EntityPlayer player) {
        Map<ItemStackKey, Integer> list = new HashMap<>();
        for (Template.BlockInfo block : blueprintTemplate.blocks) {// Note: tile entities not supported so nbt data is not used here
            ItemStack itemStack = getBlockItemCost(block);
            ItemStackKey stackKey = new ItemStackKey(itemStack);
            if (itemStack == null) {
                if (player != null) player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
                if (player != null)
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

    public static TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> sortTemplatesByIngredients(ArrayList<String> prefabsTemplatesList, CapsuleTemplateManager tempManager) {
        TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> templatesByIngrendients = new TreeMap<>(Triple::compareTo);
        for (String templateName : prefabsTemplatesList) {
            try {
                ResourceLocation location = new ResourceLocation(templateName);
                tempManager.readTemplate(location);
                CapsuleTemplate template = tempManager.get(null, location);
                if (template != null) {
                    Map<ItemStackKey, Integer> fullList = getMaterialList(template, null);
                    if (fullList != null) {
                        ItemStackKey[] list = fullList.entrySet().stream()
                                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                                .limit(5)
                                .map(Map.Entry::getKey)
                                .toArray(ItemStackKey[]::new);
                        Triple<ItemStackKey, ItemStackKey, ItemStackKey> key = Triple.of(list[0], list.length > 1 ? list[1] : null, list.length > 2 ? list[2] : null);
                        if (templatesByIngrendients.containsKey(key)) {
                            key = Triple.of(list[0], list.length > 2 ? list[2] : null, list.length > 1 ? list[1] : null);
                        }
                        if (templatesByIngrendients.containsKey(key)) {
                            key = Triple.of(list.length > 1 ? list[1] : null, list[0], list.length > 2 ? list[2] : null);
                        }
                        if (templatesByIngrendients.containsKey(key)) {
                            key = Triple.of(list.length > 2 ? list[2] : null, list[0], list.length > 1 ? list[1] : null);
                        }
                        if (templatesByIngrendients.containsKey(key)) {
                            key = Triple.of(list.length > 1 ? list[1] : null, list.length > 2 ? list[2] : null, list[0]);
                        }
                        if (templatesByIngrendients.containsKey(key)) {
                            key = Triple.of(list.length > 2 ? list[2] : null, list.length > 1 ? list[1] : null, list[0]);
                        }
                        templatesByIngrendients.put(key, templateName);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return templatesByIngrendients;
    }

    public static Map<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> reduceIngredientCount(TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> templatesByIngrendients) {
        Map<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> reduced = new HashMap<>();
        templatesByIngrendients.forEach((ingredients, value) -> {
            Triple<ItemStackKey, ItemStackKey, ItemStackKey> withOneIngredient = Triple.of(ingredients.getLeft(), null, null);
            if (!reduced.containsKey(withOneIngredient)) {
                reduced.put(withOneIngredient, value);
            } else if (ingredients.getMiddle() != null) {
                Triple<ItemStackKey, ItemStackKey, ItemStackKey> withTwoIngredient = Triple.of(ingredients.getLeft(), ingredients.getMiddle(), null);
                if (!reduced.containsKey(withTwoIngredient)) {
                    reduced.put(withTwoIngredient, value);
                } else if (ingredients.getRight() != null) {
                    if (!reduced.containsKey(ingredients)) {
                        reduced.put(ingredients, value);
                    } else {
                        LOGGER.warn("Could not create prefab recipe with 2 ingredients or less.");
                    }
                } else {
                    LOGGER.warn("Could not create prefab recipe with 2 ingredients or less.");
                }
            } else {
                LOGGER.warn("Could not create prefab recipe with 1 ingredient.");
            }
        });
        return reduced;
    }

    public static void createDynamicPrefabRecipes(RegistryEvent.Register<IRecipe> event, ArrayList<String> prefabsTemplatesList) {
        JsonObject referenceRecipe = Files.readJSON(new File(Config.configDir, "prefabs/prefab_blueprint_recipe.json"));
        if (referenceRecipe != null) {
            CapsuleTemplateManager tempManager = new CapsuleTemplateManager(Config.configDir.getParentFile().getParentFile().getPath(), FMLCommonHandler.instance().getDataFixer());
            TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> templatesByIngrendients = sortTemplatesByIngredients(prefabsTemplatesList, tempManager);
            Map<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> reduced = reduceIngredientCount(templatesByIngrendients);

            reduced.forEach((ingredients, templateName) -> {
                CapsuleTemplate template = tempManager.get(null, new ResourceLocation(templateName));
                JsonObject jsonRecipe = Files.copy(referenceRecipe);
                if (jsonRecipe != null && template != null) {
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("structureName", templateName);
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("label", Capsule.labelFromPath(templateName));
                    int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("size", size);
                    ShapedOreRecipe templateRecipe = ShapedOreRecipe.factory(new JsonContext("capsule"), jsonRecipe);
                    templateRecipe.getIngredients().set(4, Ingredient.fromStacks(ingredients.getLeft().itemStack));
                    if (ingredients.getMiddle() != null)
                        templateRecipe.getIngredients().set(0, Ingredient.fromStacks(ingredients.getMiddle().itemStack));
                    if (ingredients.getRight() != null)
                        templateRecipe.getIngredients().set(2, Ingredient.fromStacks(ingredients.getRight().itemStack));
                    event.getRegistry().register(templateRecipe.setRegistryName("capsule:" + templateName));
                }
            });
        }
    }
}
