package capsule.helpers;

import capsule.Config;
import capsule.StructureSaver;
import capsule.StructureSaver.ItemStackKey;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.gson.JsonObject;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Blueprint {
    protected static final Logger LOGGER = LogManager.getLogger(Blueprint.class);

    public static ItemStack getBlockItemCost(Template.BlockInfo blockInfo) {
        final BlockState state = blockInfo.state;
        Block block = state.getBlock();
        CompoundNBT blockNBT = blockInfo.nbt;
        try {
            // prevent door to beeing counted twice
            if (block instanceof DoorBlock) {
                if (state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    return new ItemStack(block.asItem(), 1);
                }
                return ItemStack.EMPTY; // door upper is free, only lower counts.

            } else if (block instanceof BedBlock) {
                if (state.get(BedBlock.PART) == BedPart.HEAD) {
                    return new ItemStack(block.asItem(), 1);
                }
                return ItemStack.EMPTY; // Bed foot is free, only head counts.

            } else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return new ItemStack(block.asItem(), 2);

            } else if (block instanceof FarmlandBlock) {
                return new ItemStack(Blocks.DIRT);

            } else if (block instanceof FlowingFluidBlock) {
                FlowingFluidBlock fblock = (FlowingFluidBlock) block;
                if (isLiquidSource(state, fblock)) {
                    ItemStack item = FluidUtil.getFilledBucket(new FluidStack(fblock.getFluid(), FluidAttributes.BUCKET_VOLUME));
                    return item.isEmpty() ? null : item; // return null to indicate error
                }
                return ItemStack.EMPTY; //flowing liquid is free

            } else if (block instanceof PistonHeadBlock
                    || block instanceof MovingPistonBlock) {
                return ItemStack.EMPTY; // Piston extension is free
            }
            ItemStack item = new ItemStack(block.asItem(), 1);
            if (blockNBT != null) {
//                if (blockNBT.contains("dummy") && blockNBT.getBoolean("dummy"))
//                    return ItemStack.EMPTY; // second part of Immersive engineering extended block.
                CompoundNBT itemNBT = new CompoundNBT();
                JsonObject allowedNBT = Config.getBlueprintAllowedNBT(block);
                for (String key : blockNBT.keySet()) {
                    if (allowedNBT.has(key) && !allowedNBT.get(key).isJsonNull()) {
                        String targetKey = allowedNBT.get(key).getAsString();
                        itemNBT.put(targetKey, blockNBT.get(key));
                    }
                }
                if (itemNBT.size() > 0) {
                    item.setTag(itemNBT);
                }
            }
            return item;
        } catch (Exception e) {
            // some items requires world to have getItem work, here it produces NullPointerException. fallback to default break state of block.
            return new ItemStack(Item.getItemFromBlock(block), 1);
        }
    }

    public static boolean isLiquidSource(BlockState state, FlowingFluidBlock block) {
        return block.getFluidState(state).isSource();
    }

    @Nullable
    public static Map<ItemStackKey, Integer> getMaterialList(ItemStack blueprint, ServerWorld
            worldserver, PlayerEntity player) {
        CapsuleTemplate blueprintTemplate = StructureSaver.getTemplate(blueprint, worldserver).getRight();
        if (blueprintTemplate == null) return null;

        return getMaterialList(blueprintTemplate, player);
    }

    public static Map<ItemStackKey, Integer> getMaterialList(CapsuleTemplate blueprintTemplate, @Nullable PlayerEntity player) {
        Map<ItemStackKey, Integer> list = new HashMap<>();
        for (Template.BlockInfo block : blueprintTemplate.getBlocks()) {// Note: tile entities not supported so nbt data is not used here
            ItemStack itemStack = getBlockItemCost(block);
            ItemStackKey stackKey = new ItemStackKey(itemStack);
            if (itemStack == null) {
                if (player != null) player.sendMessage(new TranslationTextComponent("capsule.error.technicalError"));
                if (player != null)
                    LOGGER.error("Unknown item during blueprint undo for block " + block.state.getBlock().getRegistryName());
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

    public static TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> sortTemplatesByIngredients(List<String> prefabsTemplatesList, CapsuleTemplateManager tempManager) {
        TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> templatesByIngrendients = new TreeMap<>(Triple::compareTo);
        for (String templateName : prefabsTemplatesList) {
            try {
                CapsuleTemplate template = tempManager.getTemplateDefaulted(new ResourceLocation(templateName));
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

    public static void createDynamicPrefabRecipes(MinecraftServer server, List<String> prefabsTemplatesList, TriConsumer<ResourceLocation, JsonObject, Triple<ItemStackKey, ItemStackKey, ItemStackKey>> parseTemplate) {
        JsonObject referenceRecipe = Files.readJSON(new File(Config.getCapsuleConfigDir().toString(), "prefab_blueprint_recipe.json"));
        if (referenceRecipe != null) {
            // declarations extract to improve readability
            List<String> enabledPrefabsTemplatesList;
            TreeMap<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> templatesByIngrendients;
            Map<Triple<ItemStackKey, ItemStackKey, ItemStackKey>, String> reduced;
            // get the minimum amount of ingredient without conflicts for each recipe
            CapsuleTemplateManager tempManager = new CapsuleTemplateManager(server, Config.getCapsuleConfigDir().toFile().getParentFile().getParentFile(), DataFixesManager.getDataFixer());
            enabledPrefabsTemplatesList = getModEnabledTemplates(prefabsTemplatesList);
            templatesByIngrendients = sortTemplatesByIngredients(enabledPrefabsTemplatesList, tempManager);
            reduced = reduceIngredientCount(templatesByIngrendients);

            reduced.forEach((ingredients, templateName) -> {
                CapsuleTemplate template = tempManager.getTemplateDefaulted(new ResourceLocation(templateName));
                JsonObject jsonRecipe = Files.copy(referenceRecipe);
                if (jsonRecipe != null && template != null) {
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("structureName", templateName);
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("label", Capsule.labelFromPath(templateName));
                    int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                    jsonRecipe.getAsJsonObject("result").getAsJsonObject("nbt").addProperty("size", size);
                    parseTemplate.accept(new ResourceLocation(templateName), jsonRecipe, ingredients);
                }
            });
        }
    }

    public static List<String> getModEnabledTemplates(List<String> prefabsTemplatesList) {
        return prefabsTemplatesList.stream().filter(templatePath -> {
            String[] path = templatePath.replaceAll(Config.prefabsTemplatesPath + "/", "").split("/");
            return path.length == 1 || ModList.get().isLoaded(path[0]);
        }).collect(Collectors.toList());
    }
}
