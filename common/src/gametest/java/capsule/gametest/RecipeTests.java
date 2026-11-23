package capsule.gametest;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.CapsuleItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class RecipeTests {

    private static ItemStack craft(GameTestHelper helper, String expectedRecipe, int width, int height, ItemStack... items) {
        CraftingInput input = CraftingInput.of(width, height, List.of(items));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        if (recipe == null) helper.fail("no recipe matches for " + expectedRecipe);
        assertTrue(helper, recipe.id().identifier().toString().equals(expectedRecipe), "expected " + expectedRecipe + " but matched " + recipe.id().identifier());
        return recipe.value().assemble(input);
    }

    private static ItemStack linkedCapsule(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        return CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 1);
    }

    /**
     * The GameTest resources fill the ingot tags of other mods (data/c/tags/item/ingots), so every recipe must load.
     */
    @GameTest(template = "empty")
    public static void everyCapsuleRecipeLoadsWithResolvedIngredients(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().recipeAccess();
        Set<String> problems = new LinkedHashSet<>();
        helper.getLevel().getServer().getResourceManager()
                .listResources("recipe", rl -> rl.getNamespace().equals(CapsuleMod.MODID) && rl.getPath().endsWith(".json"))
                .keySet().forEach(file -> {
                    String path = file.getPath();
                    Identifier id = Identifier.fromNamespaceAndPath(CapsuleMod.MODID, path.substring("recipe/".length(), path.length() - ".json".length()));
                    Optional<RecipeHolder<?>> recipe = recipes.byKey(ResourceKey.create(Registries.RECIPE, id));
                    if (recipe.isEmpty()) {
                        problems.add(id + " is not loaded");
                        return;
                    }
                    if (!CapsuleItems.hasNoEmptyTagsIngredient(recipe.get().value())) {
                        problems.add(id + " has an ingredient matching no item");
                    }
                });

        assertTrue(helper, problems.isEmpty(), "recipe problems: " + problems);
        helper.succeed();
    }

    /**
     * The recipe advancements unlock their recipes when the player gets the right item, not any item: an item predicate
     * in a format the codec does not read is dropped without error, and then matches everything.
     */
    @GameTest(template = "empty")
    public static void recipeAdvancementsWaitForTheirItem(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Set<String> problems = new LinkedHashSet<>();
        checkInventoryCriterion(server, problems, "recipes/tools/captureblock", "has_compass", new ItemStack(Items.COMPASS));
        checkInventoryCriterion(server, problems, "recipes/tools/capsule", "has_ender_pearl", new ItemStack(Items.ENDER_PEARL));
        checkInventoryCriterion(server, problems, "recipes/tools/capsule_wood", "has_plankWood", new ItemStack(Items.OAK_PLANKS));

        assertTrue(helper, problems.isEmpty(), "recipe advancement problems: " + problems);
        helper.succeed();
    }

    private static void checkInventoryCriterion(net.minecraft.server.MinecraftServer server, Set<String> problems, String path, String criterion, ItemStack expected) {
        Identifier id = Identifier.fromNamespaceAndPath(CapsuleMod.MODID, path);
        var advancement = server.getAdvancements().get(id);
        if (advancement == null) {
            problems.add(id + " is not loaded");
            return;
        }
        var found = advancement.value().criteria().get(criterion);
        if (found == null || !(found.triggerInstance() instanceof net.minecraft.advancements.triggers.InventoryChangeTrigger.TriggerInstance instance)) {
            problems.add(id + " has no inventory_changed criterion " + criterion);
            return;
        }
        if (instance.items().isEmpty()) {
            problems.add(id + " " + criterion + " has no item predicate");
        } else if (instance.items().stream().anyMatch(p -> !p.test(expected) || p.test(new ItemStack(Items.DIRT)))) {
            problems.add(id + " " + criterion + " does not match exactly " + expected.getItem());
        }
    }

    /**
     * Blueprints are blue: no capsule tier is made of lapis.
     */
    @GameTest(template = "empty")
    public static void noCapsuleTierIsMadeOfLapis(GameTestHelper helper) {
        List<Identifier> lapisTiers = helper.getLevel().recipeAccess().getRecipes().stream()
                .filter(recipe -> recipe.value() instanceof ShapedRecipe shaped && shaped.assemble(CraftingInput.EMPTY).getItem() instanceof CapsuleItem)
                .filter(recipe -> ((ShapedRecipe) recipe.value()).getIngredients().stream().flatMap(Optional::stream).anyMatch(i -> i.test(new ItemStack(Items.LAPIS_LAZULI))))
                .map(recipe -> recipe.id().identifier())
                .toList();
        assertTrue(helper, lapisTiers.isEmpty(), "capsule tiers made of lapis: " + lapisTiers);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dyeRecipeColorsCapsule(GameTestHelper helper) {
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);

        ItemStack result = craft(helper, "capsule:dye", 2, 1, capsule, new ItemStack(Items.DYE.red()));

        assertTrue(helper, CapsuleItem.getBaseColor(result) != CapsuleItem.getBaseColor(capsule), "base color should change");
        assertTrue(helper, CapsuleItem.getSize(result) == 3, "size is kept");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capsuleColorsHaveNoDyedTooltip(GameTestHelper helper) {
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        ItemStack dyed = craft(helper, "capsule:dye", 2, 1, capsule, new ItemStack(Items.DYE.red()));

        for (ItemStack stack : List.of(capsule, dyed)) {
            DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
            assertTrue(helper, color != null && !stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.DYED_COLOR),
                    "the capsule tooltip should not say Dyed: " + color);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradeRecipeGrowsCapsule(GameTestHelper helper) {
        ItemStack result = craft(helper, "capsule:upgrade", 2, 1, CapsuleTestUtils.emptyCapsule(3), new ItemStack(Items.POPPED_CHORUS_FRUIT));

        assertTrue(helper, CapsuleItem.getSize(result) == 5, "size should be 5, got " + CapsuleItem.getSize(result));
        assertTrue(helper, CapsuleItem.getUpgradeLevel(result) == 1, "upgrade level should be 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clearRecipeEmptiesCapsule(GameTestHelper helper) {
        ItemStack result = craft(helper, "capsule:clear", 1, 1, linkedCapsule(helper));

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.EMPTY), "capsule should be empty");
        assertTrue(helper, CapsuleItem.getStructureName(result) == null, "structure link should be removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recoveryRecipeMakesOneUseCopy(GameTestHelper helper) {
        ItemStack linked = linkedCapsule(helper);

        ItemStack result = craft(helper, "capsule:recovery", 2, 1, linked, new ItemStack(Items.GLASS_BOTTLE));

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.ONE_USE) && CapsuleItem.isOneUse(result), "result should be a one use capsule");
        assertTrue(helper, CapsuleItem.getStructureName(result).equals(CapsuleItem.getStructureName(linked)), "recovery points to the same content");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintRecipeCopiesCapsule(GameTestHelper helper) {
        ItemStack linked = linkedCapsule(helper);
        ItemStack e = ItemStack.EMPTY;

        ItemStack result = craft(helper, "capsule:blueprint", 3, 3,
                e, new ItemStack(Items.STONE_BUTTON), e,
                new ItemStack(Items.DYE.blue()), linked, new ItemStack(Items.DYE.blue()),
                e, new ItemStack(Items.PAPER), e);

        assertTrue(helper, CapsuleItem.isBlueprint(result), "result should be a blueprint");
        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.DEPLOYED), "a crafted blueprint is uncharged");
        assertTrue(helper, CapsuleItem.getStructureName(result).equals(CapsuleItem.getStructureName(linked)), "blueprint references the source until crafted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ironCapsuleRecipe(GameTestHelper helper) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack iron = new ItemStack(Items.IRON_INGOT);

        ItemStack result = craft(helper, "capsule:capsule_iron", 3, 3,
                e, new ItemStack(Items.STONE_BUTTON), e,
                iron, new ItemStack(Items.ENDER_PEARL), iron,
                e, iron, e);

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.EMPTY) && CapsuleItem.getSize(result) == 3, "iron capsule should be an empty 3x3x3 capsule");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netheriteCapsuleRecipeGivesAVanilla13Capsule(GameTestHelper helper) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack netherite = new ItemStack(Items.NETHERITE_INGOT);

        ItemStack result = craft(helper, "capsule:capsule_netherite", 3, 3,
                e, new ItemStack(Items.STONE_BUTTON), e,
                netherite, new ItemStack(Items.ENDER_PEARL), netherite,
                e, new ItemStack(Items.IRON_INGOT), e);

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.EMPTY) && CapsuleItem.getSize(result) == 13, "netherite capsule should be an empty 13x13x13 capsule");
        helper.succeed();
    }
}
