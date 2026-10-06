package capsule.gametest;

import capsule.Config;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class BlueprintCraftingTests {

    @GameTest(template = "empty")
    public static void missingPrefabTemplateIsReported(GameTestHelper helper) {
        List<Component> chat = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(1, 1, 1), chat);
        String source = "config/capsule/prefabs/missing_prefab";
        ItemStack blueprint = Capsule.newLinkedCapsuleItemStack(source, 0x3BB3FC, 0xFFFFFF, 3, false, null, 0);
        CapsuleItem.setBlueprint(blueprint);

        try (LogCapture logs = LogCapture.open()) {
            CapsuleItem.duplicateBlueprintTemplate(blueprint, helper.getLevel(), player);
            assertTrue(helper, logs.containing(source).stream().anyMatch(m -> m.contains("missing_prefab.nbt")),
                    "the error should name the template and the searched paths, got " + logs.messages());
        }
        CapsuleTestUtils.removePlayer(player);

        assertTrue(helper, chat.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals("capsule.error.blueprintSourceNotFound")),
                "the player should be told, got " + chat);
        assertTrue(helper, source.equals(CapsuleItem.getStructureName(blueprint)), "no empty blueprint template should be created, got " + CapsuleItem.getStructureName(blueprint));
        helper.succeed();
    }

    private static final int RESULT = 0;

    /**
     * A crafting table menu holding grid (3x3, row by row) for a fresh survival player.
     */
    private static CraftingMenu craftingMenu(GameTestHelper helper, ServerPlayer player, ItemStack... grid) {
        CraftingMenu menu = new CraftingMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        for (int i = 0; i < grid.length; i++) menu.getSlot(i + 1).set(grid[i].copy());
        return menu;
    }

    private static int[] gridCounts(CraftingMenu menu) {
        return IntStream.rangeClosed(1, 9).map(slot -> menu.getSlot(slot).getItem().getCount()).toArray();
    }

    /**
     * The capsules crafted into the player's inventory or hand, which must all be blueprints.
     */
    private static List<ItemStack> blueprints(GameTestHelper helper, ServerPlayer player, CraftingMenu menu) {
        List<ItemStack> found = new ArrayList<>(player.getInventory().items.stream().filter(s -> s.getItem() instanceof CapsuleItem).toList());
        if (!menu.getCarried().isEmpty()) found.add(menu.getCarried());
        assertTrue(helper, found.stream().allMatch(CapsuleItem::isBlueprint), "only blueprints should be crafted, got " + found.stream().map(s -> CapsuleItem.getState(s) + " " + CapsuleItem.getStructureName(s)).toList());
        return found;
    }

    private static ItemStack linkedCapsule(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.STONE);
        return CapsuleTestUtils.capture(helper, pos, 1);
    }

    private static ItemStack of(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    private static void craftBlueprint(GameTestHelper helper, ClickType click, boolean oneUse, int expectedBlueprints, int[] expectedGrid) {
        ItemStack linked = linkedCapsule(helper, new BlockPos(1, 1, 1));
        if (oneUse) CapsuleItem.setOneUse(linked);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        ItemStack e = ItemStack.EMPTY;
        CraftingMenu menu = craftingMenu(helper, player,
                e, of(Items.STONE_BUTTON, 3), e,
                of(Items.BLUE_DYE, 3), linked, of(Items.BLUE_DYE, 3),
                e, of(Items.PAPER, 3), e);

        menu.clicked(RESULT, 0, click, player);

        List<ItemStack> crafted = blueprints(helper, player, menu);
        CapsuleTestUtils.removePlayer(player);
        assertTrue(helper, crafted.size() == expectedBlueprints, "expected " + expectedBlueprints + " blueprints, got " + crafted.size());
        assertTrue(helper, Arrays.equals(gridCounts(menu), expectedGrid), "grid counts " + Arrays.toString(gridCounts(menu)) + " instead of " + Arrays.toString(expectedGrid));
        assertTrue(helper, ItemStack.isSameItemSameComponents(linked, menu.getSlot(5).getItem()), "the source capsule should stay in the grid unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintCraftConsumesItsIngredients(GameTestHelper helper) {
        craftBlueprint(helper, ClickType.PICKUP, false, 1, new int[]{0, 2, 0, 2, 1, 2, 0, 2, 0});
    }

    @GameTest(template = "empty")
    public static void blueprintShiftCraftConsumesItsIngredients(GameTestHelper helper) {
        craftBlueprint(helper, ClickType.QUICK_MOVE, false, 3, new int[]{0, 0, 0, 0, 1, 0, 0, 0, 0});
    }

    @GameTest(template = "empty")
    public static void blueprintCraftFromAOneUseCapsuleConsumesItsIngredients(GameTestHelper helper) {
        craftBlueprint(helper, ClickType.PICKUP, true, 1, new int[]{0, 2, 0, 2, 1, 2, 0, 2, 0});
    }

    @GameTest(template = "empty")
    public static void blueprintShiftCraftFromAOneUseCapsuleConsumesItsIngredients(GameTestHelper helper) {
        craftBlueprint(helper, ClickType.QUICK_MOVE, true, 3, new int[]{0, 0, 0, 0, 1, 0, 0, 0, 0});
    }

    private static void changeBlueprint(GameTestHelper helper, ClickType click) {
        ItemStack source = linkedCapsule(helper, new BlockPos(1, 1, 1));
        ItemStack blueprint = linkedCapsule(helper, new BlockPos(3, 1, 1));
        CapsuleItem.setBlueprint(blueprint);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        CraftingMenu menu = craftingMenu(helper, player, blueprint, source);

        menu.clicked(RESULT, 0, click, player);

        List<ItemStack> crafted = blueprints(helper, player, menu);
        CapsuleTestUtils.removePlayer(player);
        assertTrue(helper, crafted.size() == 1, "expected 1 blueprint, got " + crafted.size());
        assertTrue(helper, Arrays.equals(gridCounts(menu), new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0}), "only the source capsule should stay, got " + Arrays.toString(gridCounts(menu)));
        assertTrue(helper, ItemStack.isSameItemSameComponents(source, menu.getSlot(2).getItem()), "the source capsule should stay in the grid unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintChangeConsumesTheBlueprint(GameTestHelper helper) {
        changeBlueprint(helper, ClickType.PICKUP);
    }

    @GameTest(template = "empty")
    public static void blueprintChangeShiftCraftConsumesTheBlueprint(GameTestHelper helper) {
        changeBlueprint(helper, ClickType.QUICK_MOVE);
    }

    /**
     * Crafts a prefab whose 3x3 recipe has its three template ingredients at prefabSlots (grid indexes): those are given
     * back, the other ingredients are consumed.
     */
    private static void craftPrefab(GameTestHelper helper, ClickType click, Set<Integer> prefabSlots) {
        PrefabsBlueprintCapsuleRecipe recipe = PrefabsBlueprintAggregatorRecipe.instance.recipes.stream()
                .filter(r -> r.recipe.getWidth() == 3 && r.recipe.getHeight() == 3 && prefabSlots.stream().noneMatch(i -> r.recipe.getIngredients().get(i).isEmpty()))
                .findFirst().orElse(null);
        assertTrue(helper, recipe != null, "a prefab recipe with three ingredients should exist");
        List<Ingredient> ingredients = recipe.recipe.getIngredients();
        ItemStack[] grid = ingredients.stream().map(i -> i.isEmpty() ? ItemStack.EMPTY : i.getItems()[0].copyWithCount(2)).toArray(ItemStack[]::new);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 4));
        CraftingMenu menu = craftingMenu(helper, player, grid);

        menu.clicked(RESULT, 0, click, player);

        int crafts = click == ClickType.QUICK_MOVE ? 2 : 1;
        int[] expected = IntStream.range(0, 9).map(i -> grid[i].isEmpty() ? 0 : prefabSlots.contains(i) ? 2 : 2 - crafts).toArray();
        List<ItemStack> crafted = blueprints(helper, player, menu);
        CapsuleTestUtils.removePlayer(player);
        assertTrue(helper, crafted.size() == crafts, recipe.recipe.getResultItem(helper.getLevel().registryAccess()) + ": expected " + crafts + " blueprints, got " + crafted.size());
        assertTrue(helper, Arrays.equals(gridCounts(menu), expected), "grid counts " + Arrays.toString(gridCounts(menu)) + " instead of " + Arrays.toString(expected));
        helper.succeed();
    }

    private static final Set<Integer> DEFAULT_PREFAB_SLOTS = Set.of(0, 2, 4);

    @GameTest(template = "empty")
    public static void prefabCraftGivesItsTemplateIngredientsBack(GameTestHelper helper) {
        craftPrefab(helper, ClickType.PICKUP, DEFAULT_PREFAB_SLOTS);
    }

    @GameTest(template = "empty")
    public static void prefabShiftCraftGivesItsTemplateIngredientsBack(GameTestHelper helper) {
        craftPrefab(helper, ClickType.QUICK_MOVE, DEFAULT_PREFAB_SLOTS);
    }

    /**
     * prefab_blueprint_recipe.json invites pack makers to move the template ingredients 1, 2 and 3.
     */
    private static void withPrefabPattern(GameTestHelper helper, String pattern, Runnable test) {
        Path file = Config.getCapsuleConfigDir().resolve("prefab_blueprint_recipe.json");
        try {
            String original = Files.readString(file);
            try {
                Files.writeString(file, original.replaceFirst("\"pattern\": \\[[^\\]]*\\]", pattern));
                PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(helper.getLevel().getServer().getResourceManager());
                test.run();
            } finally {
                Files.writeString(file, original);
                PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(helper.getLevel().getServer().getResourceManager());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String MOVED_PATTERN = "\"pattern\": [\"b12\", \"l3l\", \" p \"]";

    @GameTest(template = "empty", batch = "prefabPattern")
    public static void prefabCraftWithAMovedPatternGivesItsTemplateIngredientsBack(GameTestHelper helper) {
        withPrefabPattern(helper, MOVED_PATTERN, () -> craftPrefab(helper, ClickType.PICKUP, Set.of(1, 2, 4)));
    }

    @GameTest(template = "empty", batch = "prefabPattern")
    public static void prefabShiftCraftWithAMovedPatternGivesItsTemplateIngredientsBack(GameTestHelper helper) {
        withPrefabPattern(helper, MOVED_PATTERN, () -> craftPrefab(helper, ClickType.QUICK_MOVE, Set.of(1, 2, 4)));
    }
}
