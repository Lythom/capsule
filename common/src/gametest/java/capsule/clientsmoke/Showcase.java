package capsule.clientsmoke;

import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.CapsuleBlocks;
import capsule.client.CaptureAnimation;
import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Blueprint;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.CapsuleItems;
import capsule.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static capsule.clientsmoke.ClientSmokeTest.mc;
import static capsule.clientsmoke.ClientSmokeTest.mainHandIs;

/**
 * Dev only, with -Dcapsule.showcase=true on top of -Dcapsule.clientsmoke=true (scripts/showcase.sh): instead of the smoke
 * scenario, stages one scene per feature of the 9.1 changelog, records each with ffmpeg (x11grab of DISPLAY) into
 * screenshots/capsule-showcase/NN-scene.mkv and takes PNG stills, for the illustrated changelog of the wiki.
 * -Dcapsule.showcase.scenes=capture,preview,... plays only these scenes (capture always runs: the others use its house).
 */
class Showcase {
    private static final Logger LOGGER = LogManager.getLogger();
    static final boolean ENABLED = Boolean.getBoolean("capsule.showcase");
    private static final Set<String> SCENES = Arrays.stream(System.getProperty("capsule.showcase.scenes", "").split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    private static final int SIZE = 5;
    /**
     * Ticks to let the chunks around a scene load and render after a teleport.
     */
    private static final int SETTLE = 100;
    private static final UUID STRANGER = UUID.nameUUIDFromBytes("OfflinePlayer:Alex".getBytes());

    private final ClientSmokeTest test;
    private final Scenario scenario;
    private volatile int ground;
    private volatile ItemStack house = ItemStack.EMPTY;
    private volatile BlockPos chest;
    private volatile ItemStack blueprint = ItemStack.EMPTY;
    private Process ffmpeg;
    private String recording;

    Showcase(ClientSmokeTest test, Scenario scenario) {
        this.test = test;
        this.scenario = scenario;
    }

    private static boolean scene(String name) {
        return SCENES.isEmpty() || SCENES.contains(name);
    }

    /**
     * Before the world: the title screen and Mod Menu's list (Fabric).
     */
    void titleScreens() {
        scenario.run("clear the output", () -> {
            try {
                FileUtils.deleteDirectory(ClientSmokeTest.outputDir().toFile());
                Files.createDirectories(ClientSmokeTest.outputDir());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        scenario.run("skip the accessibility onboarding", () -> {
            if (mc().screen instanceof AccessibilityOnboardingScreen) {
                mc().options.onboardAccessibility = false;
                mc().setScreen(new TitleScreen());
            }
        });
        if (scene("title")) {
            scenario.sleep(40).run("screenshot", () -> test.screenshot("08-title-screen"));
        }
        if (scene("modmenu") && Services.PLATFORM.isModLoaded("modmenu")) {
            scenario.run("open Mod Menu", () -> {
                try {
                    Screen mods = (Screen) Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance(mc().screen);
                    mc().setScreen(mods);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }).sleep(20).run("search capsule", () -> "capsule".chars().forEach(c -> mc().screen.charTyped((char) c, 0)))
                    .sleep(20)
                    .run("select Capsule", () -> {
                        // the first entry of the list, left of the screen
                        double x = mc().screen.width / 4.0, y = 48 + 18;
                        mc().screen.mouseClicked(x, y, 0);
                        mc().screen.mouseReleased(x, y, 0);
                    })
                    .sleep(20)
                    .run("screenshot", () -> test.screenshot("08-mod-menu"))
                    .run("back to the title screen", () -> mc().setScreen(new TitleScreen()))
                    .sleep(20);
        }
    }

    void scenes() {
        scenario.async("prepare the showcase", 100, () -> test.onServer(this::prepareWorld));
        RecipeViewerProbe.installed().forEach(viewer -> scenario.await(viewer.name() + " loaded", 1200, viewer::ready)
                .run("show only capsules in " + viewer.name(), () -> viewer.search("capsule")));
        captureScene();
        if (scene("preview")) previewScene();
        if (scene("deploy")) deployScene();
        if (scene("loyalty")) loyaltyScene();
        if (scene("enchanting")) enchantingScene();
        if (scene("claim") && Services.PLATFORM.isModLoaded("flan")) claimScene();
        if (scene("blueprint")) blueprintScene();
        // last: the house stays deployed in the lava, which empties its template
        if (scene("lava")) lavaScene();
        if (scene("viewer")) RecipeViewerProbe.installed().forEach(this::recipeViewerScene);
    }

    private void prepareWorld(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.getInventory().clearContent();
    }

    // ---------------------------------------------------------------- scenes

    /**
     * 1. An empty capsule thrown at a capture base: the house is sucked into the capsule.
     */
    private void captureScene() {
        BlockPos[] marker = {null};
        scenario.async("set the capture scene", 200, () -> test.onServer(p -> {
                    marker[0] = new BlockPos(0, ground, 4);
                    buildHouseOnCaptureBase(p.serverLevel(), marker[0]);
                    p.getInventory().setItem(0, Capsule.newEmptyCapsuleItemStack(DyeColor.LIGHT_BLUE.getTextureDiffuseColor(), 0xFFD700, SIZE, false, null, 0));
                    ClientSmokeTest.flyTo(p, 0.5, ground + 3, -4.5, 25);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("record", () -> startRecording("01-capture"))
                .sleep(20)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.EMPTY_ACTIVATED))
                .sleep(25)
                .run("reset the animation counter", () -> CaptureAnimation.renderedFrames = 0)
                .run("throw", test::rightClick)
                .await("capture animation", 200, () -> CaptureAnimation.renderedFrames > 0)
                .sleep(5)
                .run("screenshot", () -> test.screenshot("01-capture-animation"))
                .await("captured", 200, test.serverCondition(p -> p.level().getBlockState(marker[0].above()).isAir()
                        && ClientSmokeTest.capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.LINKED)) != null))
                .sleep(40)
                .run("stop", this::stopRecording)
                .async("pick up the linked capsule", 100, () -> test.onServer(p -> {
                    ClientSmokeTest.pickUp(p, 0);
                    house = p.getInventory().getItem(0).copy();
                    blueprint = houseBlueprint(p);
                }))
                .await("linked capsule in hand", 40, () -> mainHandIs(CapsuleState.LINKED));
    }

    /**
     * 2. The translucent preview, rotated (left click) and mirrored (sneak + left click).
     */
    private void previewScene() {
        scenario.async("set the preview scene", 200, () -> test.onServer(p -> {
                    Config.previewDisplayDuration = 20 * 60;
                    p.getInventory().setItem(0, house.copy());
                    decoratePreviewSpot(p.serverLevel(), new BlockPos(96, ground, 0));
                    ClientSmokeTest.flyTo(p, 96.5, ground + 3, -8.5, 30);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("record", () -> startRecording("02-preview-rotate-mirror"))
                .sleep(15)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .await("full preview received", 100, () -> CapsulePreviewHandler.cachedFullPreview.containsKey(CapsuleItem.getStructureName(house)))
                .sleep(25)
                .run("screenshot", () -> test.screenshot("02-translucent-preview"));
        for (int i = 0; i < 4; i++) {
            scenario.run("rotate", test::leftClick).sleep(22);
        }
        for (int i = 0; i < 3; i++) {
            scenario.async("sneak", 100, () -> test.onServer(p -> p.setShiftKeyDown(true)))
                    .run("mirror", test::leftClick)
                    .sleep(4)
                    .async("stand up", 100, () -> test.onServer(p -> p.setShiftKeyDown(false)))
                    .sleep(18);
        }
        scenario.run("stop", this::stopRecording);
    }

    /**
     * 3. The house thrown, deployed, then undeployed.
     */
    private void deployScene() {
        scenario.async("set the deploy scene", 200, () -> test.onServer(p -> {
                    Config.previewDisplayDuration = 20 * 60;
                    p.getInventory().setItem(0, house.copy());
                    ClientSmokeTest.flyTo(p, 96.5, ground + 3, -40.5, 30);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("record", () -> startRecording("03-deploy-undeploy"))
                .sleep(15)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .sleep(25)
                .run("throw", test::rightClick)
                .await("deployed", 200, test.serverCondition(p -> ClientSmokeTest.capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)) != null))
                .sleep(5)
                .run("screenshot", () -> test.screenshot("03-deployed"))
                .sleep(40)
                .async("pick up the deployed capsule", 100, () -> test.onServer(p -> ClientSmokeTest.pickUp(p, 0)))
                .await("deployed capsule in hand", 40, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(25)
                .run("undeploy", test::rightClick)
                .await("undeployed", 60, () -> mainHandIs(CapsuleState.LINKED))
                .sleep(45)
                .run("stop", this::stopRecording)
                .run("default preview duration", () -> Config.previewDisplayDuration = 120);
    }

    /**
     * 4. A capsule with Loyalty deploys and comes back to the hand, then undeploys.
     */
    private void loyaltyScene() {
        scenario.async("set the loyalty scene", 200, () -> test.onServer(p -> {
                    p.getInventory().clearContent();
                    ItemStack loyal = house.copy();
                    CapsuleItem.setLabel(loyal, "Loyal House");
                    loyal.enchant(p.serverLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), 1);
                    p.getInventory().setItem(0, loyal);
                    ClientSmokeTest.flyTo(p, 192.5, ground + 3, -8.5, 30);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("record", () -> startRecording("04-loyalty"))
                .sleep(20)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .sleep(25)
                .run("throw", test::rightClick)
                .await("back in hand", 200, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(50)
                .run("undeploy", test::rightClick)
                .await("undeployed", 60, () -> mainHandIs(CapsuleState.LINKED))
                .sleep(40)
                .run("stop", this::stopRecording);
    }

    /**
     * 4b. The enchanting table offers Loyalty for a capsule.
     */
    private void enchantingScene() {
        scenario.async("open an enchanting table with a capsule", 200, () -> test.onServer(p -> {
                    ServerLevel level = p.serverLevel();
                    BlockPos table = new BlockPos(186, ground, -8);
                    level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
                    for (BlockPos pos : BlockPos.betweenClosed(table.offset(-2, 0, -2), table.offset(2, 1, 2))) {
                        int dx = pos.getX() - table.getX(), dz = pos.getZ() - table.getZ();
                        if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && dx != 2) level.setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
                    }
                    ItemStack capsule = CapsuleItems.capsuleList.keySet().stream().skip(1).findFirst().orElseThrow().copy();
                    MenuProvider provider = new SimpleMenuProvider((id, inventory, player) -> new EnchantmentMenu(id, inventory, ContainerLevelAccess.create(level, table)),
                            Component.translatable("container.enchant"));
                    p.openMenu(provider);
                    if (p.containerMenu instanceof EnchantmentMenu menu) {
                        menu.getSlot(0).set(capsule);
                        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
                    }
                }))
                .await("enchanting screen", 100, () -> mc().screen instanceof EnchantmentScreen)
                .sleep(10)
                .run("hover the third offer", () -> mc().setScreen(new HoverEnchantmentScreen((EnchantmentMenu) mc().player.containerMenu)))
                .sleep(20)
                .run("screenshot", () -> test.screenshot("04b-enchanting-table-loyalty"))
                .run("close", () -> mc().player.closeContainer())
                .sleep(10);
    }

    /**
     * 5. Capsules are fire and lava proof: one thrown into lava deploys there and stays.
     */
    private void lavaScene() {
        scenario.async("set the lava scene", 200, () -> test.onServer(p -> {
                    ServerLevel level = p.serverLevel();
                    BlockPos center = new BlockPos(288, ground, 6);
                    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, -1, 6))) {
                        int dx = Math.abs(pos.getX() - center.getX()), dz = Math.abs(pos.getZ() - center.getZ());
                        boolean rim = dx == 6 || dz == 6 || (dx == 5 && dz == 5);
                        if (rim) {
                            level.setBlock(pos, ((dx + dz) % 3 == 0 ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState(), 2 | 16);
                        } else {
                            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2 | 16);
                            level.setBlock(pos.below(), Blocks.LAVA.defaultBlockState(), 2 | 16);
                        }
                    }
                    p.getInventory().clearContent();
                    p.getInventory().setItem(0, house.copy());
                    ClientSmokeTest.flyTo(p, 288.5, ground + 3, -7.5, 25);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("record", () -> startRecording("05-lava"))
                .sleep(20)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .sleep(25)
                .run("throw", test::rightClick)
                .await("deployed", 200, test.serverCondition(p -> ClientSmokeTest.capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)) != null))
                .sleep(60)
                .run("screenshot", () -> test.screenshot("05-lava-deployed"))
                .run("stop", this::stopRecording);
    }

    /**
     * 7. A capsule thrown into the Flan claim of another player is refused, with the chat message.
     */
    private void claimScene() {
        Object[] claim = {null};
        scenario.async("set the claim scene", 200, () -> test.onServer(p -> {
                    ServerLevel level = p.serverLevel();
                    BlockPos center = new BlockPos(384, ground, 6);
                    BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
                    for (int dx = -2; dx <= 2; dx++) {
                        level.setBlockAndUpdate(center.offset(dx, 0, 3), fence);
                    }
                    level.setBlockAndUpdate(center.offset(-2, 0, 2), Blocks.HAY_BLOCK.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(2, 0, 2), Blocks.COMPOSTER.defaultBlockState());
                    // outside the house footprint, which a block would block before the claim check
                    level.setBlockAndUpdate(center.offset(-1, 0, 2), Blocks.POPPY.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(0, 0, 2), Blocks.DANDELION.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(1, 0, 2), Blocks.CORNFLOWER.defaultBlockState());
                    claim[0] = ShowcaseFlan.claim(p, center.offset(-4, -1, -6), center.offset(4, -1, 5), STRANGER);
                    p.getInventory().clearContent();
                    p.getInventory().setItem(0, house.copy());
                    ClientSmokeTest.flyTo(p, 384.5, ground + 3, -4.5, 30);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .async("show the claim", 100, () -> test.onServer(p -> ShowcaseFlan.display(p, claim[0])))
                .sleep(20)
                .run("record", () -> startRecording("07-claim-refused"))
                .sleep(20)
                .run("activate", test::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .sleep(25)
                .run("throw", test::rightClick)
                .sleep(80)
                .run("screenshot", () -> test.screenshot("07-claim-refused"))
                .sleep(20)
                .run("stop", this::stopRecording);
    }

    /**
     * 9. A blueprint of the house linked to a chest: deployed, recharged from the chest (left click), deployed again.
     */
    private void blueprintScene() {
        scenario.async("set the blueprint scene", 200, () -> test.onServer(p -> {
                    ServerLevel level = p.serverLevel();
                    p.getInventory().clearContent();
                    ItemStack blueprint = this.blueprint.copy();
                    chest = new BlockPos(477, ground, -5);
                    level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.EAST));
                    if (level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                        int slot = 0;
                        var materials = Blueprint.getMaterialList(blueprint, level, p);
                        if (materials != null) {
                            for (var entry : materials.entrySet()) {
                                ItemStack stack = entry.getKey().itemStack.copy();
                                stack.setCount(Math.min(stack.getMaxStackSize(), entry.getValue() * 3));
                                be.setItem(slot++, stack);
                            }
                        }
                    }
                    CapsuleItem.saveSourceInventory(blueprint, chest, level.dimension());
                    p.getInventory().setItem(0, blueprint);
                    // survival, where blueprints take their materials, flying to keep the camera still
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().mayfly = true;
                    ClientSmokeTest.flyTo(p, 480.5, ground + 4, -8.5, 32);
                }))
                .run("select it", () -> ClientSmokeTest.select(0))
                .sleep(SETTLE)
                .run("look at the chest", () -> mc().player.setYRot(60))
                .async("open the chest", 100, () -> test.onServer(p -> p.openMenu((ChestBlockEntity) p.level().getBlockEntity(chest))))
                .sleep(20)
                .run("screenshot", () -> test.screenshot("09-linked-chest-before"))
                .run("close", () -> mc().player.closeContainer())
                .run("look at the first spot", () -> mc().player.setYRot(18))
                .sleep(20)
                .run("record", () -> startRecording("09-blueprint-linked-chest"))
                .sleep(20)
                .run("deploy", test::rightClick)
                .await("deployed", 60, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(25)
                .run("recharge from the chest", test::leftClick)
                .await("recharged", 60, () -> mainHandIs(CapsuleState.BLUEPRINT))
                .sleep(15);
        turn(-36, 16);
        scenario.sleep(10)
                .run("deploy again", test::rightClick)
                .await("deployed again", 60, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(35)
                .run("stop", this::stopRecording)
                .run("look at the chest", () -> mc().player.setYRot(60))
                .async("open the chest", 100, () -> test.onServer(p -> p.openMenu((ChestBlockEntity) p.level().getBlockEntity(chest))))
                .sleep(20)
                .run("screenshot", () -> test.screenshot("09-linked-chest-after"))
                .run("close", () -> mc().player.closeContainer())
                .async("creative mode", 100, () -> test.onServer(p -> {
                    p.setGameMode(GameType.CREATIVE);
                    p.getInventory().clearContent();
                }))
                .sleep(10);
    }

    /**
     * 6. The capsule tiers in the recipe viewer, with the tooltip of the netherite capsule, and its recipe.
     */
    private void recipeViewerScene(RecipeViewerProbe viewer) {
        String name = viewer.name().toLowerCase(Locale.ROOT);
        ItemStack[] netherite = {ItemStack.EMPTY};
        scenario.await(viewer.name() + " loaded", 1200, viewer::ready)
                .async("give the netherite capsule", 100, () -> test.onServer(p -> {
                    netherite[0] = CapsuleItems.capsuleList.keySet().stream()
                            .filter(s -> CapsuleItem.getSize(s) == 13 && (CapsuleItem.getMaterialColor(s) & 0xFFFFFF) == 0x443E40)
                            .findFirst().orElse(CapsuleItems.capsuleList.lastKey()).copy();
                    p.getInventory().clearContent();
                    p.getInventory().setItem(0, netherite[0]);
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                }))
                .await("survival mode on the client", 40, () -> !mc().gameMode.hasInfiniteItems())
                .run("search capsule", () -> viewer.search("capsule"))
                .run("open the inventory on the netherite capsule", () -> mc().setScreen(new ClientSmokeTest.HoverInventoryScreen(0)))
                .sleep(30)
                .run("screenshot", () -> test.screenshot("06-" + name + "-capsule-tiers"))
                .run("show the recipe", () -> viewer.showRecipes(netherite[0]))
                .sleep(30)
                .run("screenshot", () -> test.screenshot("06-" + name + "-netherite-recipe"))
                .run("close", () -> mc().setScreen(null))
                .async("creative mode", 100, () -> test.onServer(p -> p.setGameMode(GameType.CREATIVE)));
    }

    // ---------------------------------------------------------------- helpers

    /**
     * A charged blueprint of the captured house, made while its template is full (a deploy empties it).
     */
    private ItemStack houseBlueprint(ServerPlayer player) {
        ItemStack blueprint = Capsule.newEmptyCapsuleItemStack(0x3C44AA, 0xFFFFFF, SIZE, false, "House blueprint", 0);
        CapsuleItem.setState(blueprint, CapsuleState.DEPLOYED);
        CapsuleItem.setBlueprint(blueprint);
        CapsuleItem.setStructureName(blueprint, StructureSaver.createBlueprintTemplate(CapsuleItem.getStructureName(house), blueprint, player.serverLevel(), player));
        Capsule.reloadBlueprint(blueprint, player.serverLevel(), player);
        return blueprint;
    }

    /**
     * A 5×5×5 hut with a chimney on a capture base, on a foundation, with flowers around.
     */
    private void buildHouseOnCaptureBase(ServerLevel level, BlockPos marker) {
        for (BlockPos pos : BlockPos.betweenClosed(marker.offset(-3, 0, -3), marker.offset(3, 0, 3))) {
            level.setBlock(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2 | 16);
        }
        level.setBlockAndUpdate(marker, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        for (BlockPos pos : BlockPos.betweenClosed(marker.offset(-2, 1, -2), marker.offset(2, 4, 2))) {
            int dx = pos.getX() - marker.getX(), dy = pos.getY() - marker.getY(), dz = pos.getZ() - marker.getZ();
            boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            BlockState state = dy == 1 ? Blocks.STONE_BRICKS.defaultBlockState()
                    : dy == 4 ? Blocks.SPRUCE_SLAB.defaultBlockState()
                    : corner ? Blocks.OAK_LOG.defaultBlockState()
                    : wall && (dx == 0 || dz == 0) ? Blocks.GLASS_PANE.defaultBlockState()
                    : wall ? Blocks.OAK_PLANKS.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            level.setBlock(pos, state, 3 | 16);
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(marker.offset(0, 2, -2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        level.setBlock(marker.offset(0, 3, -2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
        // a chimney on the back right corner, smoking
        level.setBlock(marker.offset(-2, 4, 2), Blocks.BRICKS.defaultBlockState(), 2 | 16);
        level.setBlock(marker.offset(-2, 5, 2), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false), 2 | 16);
        level.setBlock(marker.offset(1, 5, -1), Blocks.LANTERN.defaultBlockState(), 2 | 16);
        level.setBlock(marker.offset(1, 2, 1), Blocks.POTTED_POPPY.defaultBlockState(), 2 | 16);
        level.setBlock(marker.offset(-1, 2, 1), Blocks.CHEST.defaultBlockState(), 2 | 16);
        List<BlockPos> flowers = List.of(marker.offset(-4, 0, -2), marker.offset(-4, 0, 1), marker.offset(4, 0, -3), marker.offset(4, 0, 0),
                marker.offset(-3, 0, -5), marker.offset(3, 0, -5), marker.offset(5, 0, 2));
        BlockState[] kinds = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()};
        for (int i = 0; i < flowers.size(); i++) level.setBlockAndUpdate(flowers.get(i), kinds[i % kinds.length]);
    }

    /**
     * A tree, a fence and a pumpkin patch where the preview shows, to see them through it.
     */
    private static void decoratePreviewSpot(ServerLevel level, BlockPos center) {
        for (int dx = -4; dx <= 4; dx++) level.setBlockAndUpdate(center.offset(dx, 0, 3), Blocks.OAK_FENCE.defaultBlockState());
        BlockPos tree = center.offset(-2, 0, 4);
        for (int dy = 0; dy < 5; dy++) level.setBlock(tree.above(dy), Blocks.BIRCH_LOG.defaultBlockState(), 2 | 16);
        for (BlockPos pos : BlockPos.betweenClosed(tree.offset(-2, 3, -2), tree.offset(2, 6, 2))) {
            int d = Math.abs(pos.getX() - tree.getX()) + Math.abs(pos.getZ() - tree.getZ()) + Math.max(0, pos.getY() - tree.getY() - 4) * 2;
            if (level.getBlockState(pos).isAir() && d <= 3) level.setBlock(pos, Blocks.BIRCH_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2 | 16);
        }
        level.setBlockAndUpdate(center.offset(1, 0, 0), Blocks.PUMPKIN.defaultBlockState());
        level.setBlockAndUpdate(center.offset(-1, 0, 1), Blocks.MELON.defaultBlockState());
        level.setBlockAndUpdate(center.offset(2, 0, -1), Blocks.ROSE_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        level.setBlockAndUpdate(center.offset(2, 1, -1), Blocks.ROSE_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * Turns the camera by yaw degrees over ticks, eased.
     */
    private void turn(float yaw, int ticks) {
        float[] start = {Float.NaN};
        int[] tick = {0};
        scenario.await("turn " + yaw, ticks + 2, () -> {
            if (Float.isNaN(start[0])) start[0] = mc().player.getYRot();
            float t = (float) ++tick[0] / ticks;
            mc().player.setYRot(start[0] + yaw * t * t * (3 - 2 * t));
            return tick[0] >= ticks;
        });
    }

    private void startRecording(String name) {
        stopRecording();
        // the scene's chat only (welcome messages of other mods...)
        mc().gui.getChat().clearMessages(false);
        var window = mc().getWindow();
        String display = System.getenv().getOrDefault("DISPLAY", ":0");
        ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "x11grab", "-draw_mouse", "0",
                "-framerate", "20", "-video_size", window.getWidth() + "x" + window.getHeight(), "-i", display + "+" + window.getX() + "," + window.getY(),
                "-c:v", "libx264", "-preset", "ultrafast", "-crf", "8", "-pix_fmt", "yuv444p",
                ClientSmokeTest.outputDir().resolve(name + ".mkv").toString());
        builder.redirectErrorStream(true).redirectOutput(ClientSmokeTest.outputDir().resolve(name + ".ffmpeg.log").toFile());
        try {
            ffmpeg = builder.start();
            recording = name;
            LOGGER.info("Showcase recording {} started", name);
        } catch (IOException e) {
            scenario.check("record " + name, false, e.toString());
        }
    }

    void stopRecording() {
        if (ffmpeg == null) return;
        try (OutputStream in = ffmpeg.getOutputStream()) {
            in.write('q');
            in.flush();
        } catch (IOException ignored) {
            // ffmpeg already gone
        }
        try {
            if (!ffmpeg.waitFor(20, TimeUnit.SECONDS)) ffmpeg.destroyForcibly();
        } catch (InterruptedException e) {
            ffmpeg.destroyForcibly();
        }
        LOGGER.info("Showcase recording {} stopped", recording);
        ffmpeg = null;
    }

    /**
     * The enchanting table screen with the mouse over its third offer, showing its clue.
     */
    private static class HoverEnchantmentScreen extends EnchantmentScreen {
        HoverEnchantmentScreen(EnchantmentMenu menu) {
            super(menu, mc().player.getInventory(), Component.translatable("container.enchant"));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, leftPos + 110, topPos + 14 + 19 * 2 + 9, partialTick);
        }
    }
}
