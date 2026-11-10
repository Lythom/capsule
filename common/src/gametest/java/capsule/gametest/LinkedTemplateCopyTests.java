package capsule.gametest;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.dispenser.DispenseCapsuleBehavior;
import capsule.helpers.Capsule;
import capsule.helpers.MinecraftNBT;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.structure.CapsuleTemplate;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;
import static capsule.gametest.CapsuleTestUtils.template;

/**
 * Linked capsules tagged templateShouldBeCopied, as modpacks craft them: each crafted capsule gets its own copy of the
 * config template.
 */
public class LinkedTemplateCopyTests {

    private static final int RESULT = 0;

    /**
     * A 3x3x3 config template: a stone in its first corner, a chest holding a diamond in the middle of the floor and
     * oak planks one block higher in the opposite corner. Built at (1, 1, 1), which is left empty.
     */
    private static String configTemplate(GameTestHelper helper, String name) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        helper.setBlock(2, 1, 2, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(2, 1, 2));
        chest.setItem(0, new ItemStack(Items.DIAMOND));
        helper.setBlock(3, 2, 3, Blocks.OAK_PLANKS);
        ItemStack captured = capture(helper, new BlockPos(1, 1, 1), 3);
        String path = Config.rewardTemplatesPath + "/gametest_" + name;
        MinecraftServer server = helper.getLevel().getServer();
        assertTrue(helper, StructureSaver.duplicateTemplate(StructureSaver.getTemplateNBTData(template(helper, captured)), path,
                StructureSaver.getRewardManager(server.getResourceManager()), server), "the config template should be written");
        return path;
    }

    private static void deleteConfigTemplate(GameTestHelper helper, String path) {
        StructureSaver.getRewardManager(helper.getLevel().getServer().getResourceManager()).deleteTemplate(ResourceLocation.parse(path));
    }

    /**
     * The capsule of the modpack example, as KubeJS or /give writes it.
     */
    private static ItemStack taggedCapsule(GameTestHelper helper, String structureName, int size) {
        String item = CapsuleMod.MODID + ":capsule[custom_data={color:6196919,label:\"burner_researcher\",size:" + size + ",state:2,structureName:\""
                + structureName + "\",templateShouldBeCopied:1b},dyed_color={rgb:8072841}]";
        try {
            ItemParser.ItemResult result = new ItemParser(helper.getLevel().registryAccess()).parse(new StringReader(item));
            return new ItemInput(result.item(), result.components()).createItemStack(1, false);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean tagged(ItemStack capsule, String key) {
        return NBTHelper.hasTag(capsule) && NBTHelper.getOrCreateTag(capsule).contains(key);
    }

    private static boolean told(List<Component> chat, String key) {
        return chat.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals(key));
    }

    /**
     * Runs craft with a shapeless recipe turning a furnace into result, removed afterwards.
     */
    private static void withRecipe(GameTestHelper helper, ItemStack result, Runnable craft) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = List.copyOf(recipes.getRecipes());
        RecipeHolder<ShapelessRecipe> recipe = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "test_linked_template_copy"),
                new ShapelessRecipe("", CraftingBookCategory.MISC, result, NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.FURNACE))));
        recipes.replaceRecipes(Stream.concat(original.stream(), Stream.of(recipe)).toList());
        try {
            craft.run();
        } finally {
            recipes.replaceRecipes(original);
        }
    }

    /**
     * A crafting table menu, open for player, with furnaces in its first slot.
     */
    private static CraftingMenu craftingMenu(GameTestHelper helper, ServerPlayer player, int furnaces) {
        CraftingMenu menu = new CraftingMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        player.containerMenu = menu;
        menu.getSlot(1).set(new ItemStack(Items.FURNACE, furnaces));
        return menu;
    }

    /**
     * Crafts the result with a normal click, as many times as there are results, and gives what the cursor took.
     */
    private static List<ItemStack> craft(GameTestHelper helper, ServerPlayer player, ItemStack result, int times) {
        List<ItemStack> crafted = new ArrayList<>();
        withRecipe(helper, result, () -> {
            CraftingMenu menu = craftingMenu(helper, player, times);
            for (int i = 0; i < times; i++) {
                menu.clicked(RESULT, 0, ClickType.PICKUP, player);
                crafted.add(menu.getCarried());
                menu.setCarried(ItemStack.EMPTY);
            }
            player.containerMenu = player.inventoryMenu;
        });
        return crafted;
    }

    private static void assertOwnsACopy(GameTestHelper helper, ItemStack capsule, String source) {
        String name = CapsuleItem.getStructureName(capsule);
        assertTrue(helper, name != null && !name.equals(source), "the capsule should get its own template, got " + name);
        assertTrue(helper, !tagged(capsule, "templateShouldBeCopied") && !tagged(capsule, "templateCopyFailed"), "the copy tags should be removed: " + NBTHelper.getTag(capsule));
        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "the capsule should be linked, got " + CapsuleItem.getState(capsule));
        assertTrue(helper, !CapsuleItem.isBlueprint(capsule) && !CapsuleItem.isReward(capsule), "the capsule should stay a linked capsule");
        CapsuleTemplate copy = StructureSaver.getTemplateForCapsule(helper.getLevel(), name).getRight();
        assertTrue(helper, copy != null && copy.getPalette().size() == 3, "the world template should hold the 3 blocks, got " + (copy == null ? null : copy.getPalette()));
    }

    /**
     * The 3 blocks of the config template, deployed by a size 3 capsule whose content starts at corner.
     */
    private static void assertDeployedAt(GameTestHelper helper, BlockPos corner) {
        helper.assertBlockPresent(Blocks.STONE, corner);
        helper.assertBlockPresent(Blocks.CHEST, corner.offset(1, 0, 1));
        helper.assertContainerContains(corner.offset(1, 0, 1), Items.DIAMOND);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, corner.offset(2, 1, 2));
    }

    private static void assertNothingDeployedAt(GameTestHelper helper, BlockPos corner) {
        BlockPos.betweenClosed(corner, corner.offset(2, 2, 2)).forEach(p -> helper.assertBlockPresent(Blocks.AIR, p));
    }

    @GameTest(template = "empty")
    public static void craftedLinkedCapsuleOwnsACopyOfItsTemplate(GameTestHelper helper) {
        String source = configTemplate(helper, "crafted");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            ItemStack capsule = craft(helper, player, taggedCapsule(helper, source, 1), 1).getFirst();

            assertOwnsACopy(helper, capsule, source);
            String name = CapsuleItem.getStructureName(capsule);
            assertTrue(helper, name.startsWith("c-test-player-"), "the template should be named after the player, got " + name);
            assertTrue(helper, CapsuleItem.getSize(capsule) == 3, "the capsule should grow to the template size, got " + CapsuleItem.getSize(capsule));
            assertTrue(helper, "burner_researcher".equals(NBTHelper.getOrCreateTag(capsule).getString("label")), "the label should be kept");
            assertTrue(helper, CapsuleItem.getMaterialColor(capsule) == 6196919 && MinecraftNBT.getColor(capsule) == 8072841, "the colors should be kept");
            assertTrue(helper, NBTHelper.getOrCreateTag(capsule).contains("canRotate"), "canRotate should be read from the template");

            assertTrue(helper, deploy(helper, capsule, new BlockPos(5, 0, 5), player), "the capsule should deploy");
            assertDeployedAt(helper, new BlockPos(4, 1, 4));
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED), "the capsule should be deployed");
            Capsule.resentToCapsule(capsule, helper.getLevel(), null);
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "the capsule should take its content back");
            assertNothingDeployedAt(helper, new BlockPos(4, 1, 4));
            assertTrue(helper, deploy(helper, capsule, new BlockPos(5, 0, 5), player), "the capsule should deploy again");
            assertDeployedAt(helper, new BlockPos(4, 1, 4));

            CapsuleTemplate config = StructureSaver.getConfigTemplate(source, helper.getLevel().getServer());
            assertTrue(helper, config != null && config.getPalette().size() == 3, "the config template should stay unchanged");
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void twoCraftsGiveTwoTemplates(GameTestHelper helper) {
        String source = configTemplate(helper, "two_crafts");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            List<ItemStack> crafted = craft(helper, player, taggedCapsule(helper, source, 3), 2);
            ItemStack first = crafted.get(0);
            ItemStack second = crafted.get(1);
            assertOwnsACopy(helper, first, source);
            assertOwnsACopy(helper, second, source);
            assertTrue(helper, !CapsuleItem.getStructureName(first).equals(CapsuleItem.getStructureName(second)), "each craft should get its own template");

            // the first capsule takes back its content without its stone
            assertTrue(helper, deploy(helper, first, new BlockPos(2, 0, 2), player), "the first capsule should deploy");
            helper.setBlock(1, 1, 1, Blocks.AIR);
            Capsule.resentToCapsule(first, helper.getLevel(), null);
            assertNothingDeployedAt(helper, new BlockPos(1, 1, 1));

            assertTrue(helper, deploy(helper, second, new BlockPos(6, 0, 6), player), "the second capsule should deploy");
            assertDeployedAt(helper, new BlockPos(5, 1, 5));
            assertTrue(helper, deploy(helper, first, new BlockPos(2, 0, 2), player), "the first capsule should deploy again");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 2, 3));
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    /**
     * The world templates copied from source for the test player.
     */
    private static int copiesOf(GameTestHelper helper, String source) {
        String prefix = "c-test-player-" + source.replace("/", "_") + "-";
        File capsules = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("capsules").toFile();
        String[] copies = capsules.list((dir, name) -> name.startsWith(prefix));
        return copies == null ? 0 : copies.length;
    }

    /**
     * Every crafted capsule in the inventory owns a copy of source, and no other copy was written.
     */
    private static void assertOneCopyPerCapsule(GameTestHelper helper, ServerPlayer player, String source, int crafts) {
        List<ItemStack> crafted = player.getInventory().items.stream().filter(s -> s.getItem() instanceof CapsuleItem).toList();
        assertTrue(helper, crafted.size() == crafts, "expected " + crafts + " capsules, got " + crafted.size());
        crafted.forEach(capsule -> assertOwnsACopy(helper, capsule, source));
        long names = crafted.stream().map(CapsuleItem::getStructureName).distinct().count();
        assertTrue(helper, names == crafts, "each capsule should get its own template, got " + crafted.stream().map(CapsuleItem::getStructureName).toList());
        assertTrue(helper, copiesOf(helper, source) == crafts, "no unused template should be written, got " + copiesOf(helper, source) + " for " + crafts + " capsules");
    }

    @GameTest(template = "empty")
    public static void shiftCraftGivesOneTemplatePerCapsule(GameTestHelper helper) {
        String source = configTemplate(helper, "shift_craft");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            withRecipe(helper, taggedCapsule(helper, source, 3), () -> {
                CraftingMenu menu = craftingMenu(helper, player, 3);
                menu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
                player.containerMenu = player.inventoryMenu;
            });
            assertOneCopyPerCapsule(helper, player, source, 3);
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    /**
     * The inventory grid calls onCraftedBy on a copy of the result only: the crafted capsules get their template on
     * the next player tick.
     */
    @GameTest(template = "empty")
    public static void inventoryGridShiftCraftGivesOneTemplatePerCapsule(GameTestHelper helper) {
        String source = configTemplate(helper, "inventory_shift_craft");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            withRecipe(helper, taggedCapsule(helper, source, 3), () -> {
                player.inventoryMenu.getSlot(1).set(new ItemStack(Items.FURNACE, 2));
                player.inventoryMenu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
            });
            CapsuleItem.onPlayerTick(player);
            assertOneCopyPerCapsule(helper, player, source, 2);
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taggedCapsuleInTheInventoryIsCopiedOnPlayerTick(GameTestHelper helper) {
        String source = configTemplate(helper, "player_tick");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            player.getInventory().setItem(3, taggedCapsule(helper, source, 3));
            CapsuleItem.onPlayerTick(player);
            ItemStack capsule = player.getInventory().getItem(3);

            assertOwnsACopy(helper, capsule, source);
            assertTrue(helper, deploy(helper, capsule, new BlockPos(5, 0, 5), player), "the capsule should deploy");
            assertDeployedAt(helper, new BlockPos(4, 1, 4));
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    /**
     * A tagged capsule that never reached a player, out of a Crafter: the dispenser gives it its template, sized for it.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dispensedTaggedCapsuleGetsItsTemplateFirst(GameTestHelper helper) {
        String source = configTemplate(helper, "dispensed");
        DispenserBlockEntity dispenser = ClaimTests.dispenser(helper, new BlockPos(4, 1, 4));
        ItemStack capsule = taggedCapsule(helper, source, 1);
        try {
            new DispenseCapsuleBehavior().execute(new BlockSource(helper.getLevel(), dispenser.getBlockPos(), dispenser.getBlockState(), dispenser), capsule);

            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED), "the capsule should be deployed, got " + CapsuleItem.getState(capsule));
            assertTrue(helper, CapsuleItem.getStructureName(capsule).startsWith("c-capsulemod-"), "without a player the template is named after the mod, got " + CapsuleItem.getStructureName(capsule));
            assertTrue(helper, !tagged(capsule, "templateShouldBeCopied"), "the copy tag should be removed");
            assertDeployedAt(helper, new BlockPos(3, 2, 3));
        } finally {
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }

    private static void assertNeverDeploysEmpty(GameTestHelper helper, String source, String messageKey, Consumer<List<String>> checkLogs) {
        List<Component> chat = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8), chat);
        try {
            player.getInventory().setItem(0, taggedCapsule(helper, source, 3));
            ItemStack capsule = player.getInventory().getItem(0);
            try (LogCapture logs = LogCapture.open()) {
                CapsuleItem.onPlayerTick(player);
                checkLogs.accept(logs.containing(source));
                int logged = logs.containing(source).size();
                CapsuleItem.onPlayerTick(player);
                assertTrue(helper, logs.containing(source).size() == logged, "a failed copy should not be retried every tick, got " + logs.containing(source));
            }
            assertTrue(helper, told(chat, messageKey), "the player should be told with " + messageKey + ", got " + chat);
            assertTrue(helper, source.equals(CapsuleItem.getStructureName(capsule)), "the capsule should keep its source, got " + CapsuleItem.getStructureName(capsule));
            assertTrue(helper, tagged(capsule, "templateShouldBeCopied") && tagged(capsule, "templateCopyFailed"), "the capsule should still wait for its copy: " + NBTHelper.getTag(capsule));

            chat.clear();
            assertTrue(helper, !deploy(helper, capsule, new BlockPos(5, 0, 5), player), "the deploy should be refused");
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "the capsule should not be deployed, got " + CapsuleItem.getState(capsule));
            assertTrue(helper, told(chat, messageKey), "the refused deploy should be explained, got " + chat);
        } finally {
            CapsuleTestUtils.removePlayer(player);
        }
    }

    @GameTest(template = "empty")
    public static void missingSourceTemplateIsNeverDeployedEmpty(GameTestHelper helper) {
        String source = Config.rewardTemplatesPath + "/gametest_missing_source";
        assertNeverDeploysEmpty(helper, source, "capsule.error.copySourceNotFound", logs ->
                assertTrue(helper, logs.stream().anyMatch(m -> m.contains("gametest_missing_source.nbt")), "the error should name the template and the searched paths, got " + logs));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sourceOutsideTheConfigFolderIsNotCopied(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        ItemStack captured = capture(helper, new BlockPos(1, 1, 1), 1);
        String playerTemplate = CapsuleItem.getStructureName(captured);
        assertNeverDeploysEmpty(helper, playerTemplate, "capsule.error.copySourceNotAllowed", logs ->
                assertTrue(helper, !logs.isEmpty(), "the refused copy should be logged"));
        assertNothingDeployedAt(helper, new BlockPos(4, 1, 4));

        assertTrue(helper, !StructureSaver.isConfigTemplatePath("config/../world/capsules/" + playerTemplate), "parent segments should be refused");
        assertTrue(helper, StructureSaver.isConfigTemplatePath("Config/Capsule/Rewards/Some_Reward"), "config paths are not case sensitive");
        // the player's capsule keeps its content
        assertTrue(helper, deploy(helper, captured, new BlockPos(2, 0, 2), null), "the captured capsule should still deploy");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 2));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintWithTemplateShouldBeCopiedKeepsItsBehavior(GameTestHelper helper) {
        String source = configTemplate(helper, "blueprint");
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(8, 1, 8));
        try {
            ItemStack blueprint = taggedCapsule(helper, source, 3);
            CapsuleItem.setBlueprint(blueprint);
            player.getInventory().setItem(0, blueprint);
            CapsuleItem.onPlayerTick(player);
            blueprint = player.getInventory().getItem(0);

            String name = CapsuleItem.getStructureName(blueprint);
            assertTrue(helper, CapsuleItem.isBlueprint(blueprint), "the blueprint should stay a blueprint");
            assertTrue(helper, name.startsWith(StructureSaver.BLUEPRINT_PREFIX), "the blueprint should get a blueprint template, got " + name);
            assertTrue(helper, !tagged(blueprint, "templateShouldBeCopied") && !tagged(blueprint, "templateCopyFailed"), "the copy tag should be removed: " + NBTHelper.getTag(blueprint));
            CapsuleTemplate copy = template(helper, blueprint);
            assertTrue(helper, copy.getPalette().stream().noneMatch(b -> b.nbt() != null && b.nbt().contains("Items")), "a blueprint never keeps inventories, got " + copy.getPalette());
        } finally {
            CapsuleTestUtils.removePlayer(player);
            deleteConfigTemplate(helper, source);
        }
        helper.succeed();
    }
}
