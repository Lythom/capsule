package capsule.clientsmoke;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.CapsuleBlocks;
import capsule.client.CaptureAnimation;
import capsule.client.CapsulePreviewHandler;
import capsule.client.ClientConfig;
import capsule.enchantments.CapsuleEnchantments;
import capsule.helpers.Capsule;
import capsule.helpers.MinecraftNBT;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.items.CapsuleItems;
import capsule.plugins.RecipeViewerContent;
import capsule.structure.CapsuleTemplate;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Dev only client smoke test, enabled with -Dcapsule.clientsmoke=true: creates a flat creative world, plays a capsule
 * scenario with screenshots in screenshots/capsule-smoke, writes report.txt next to them and quits. The game exits
 * with status 1 if a check failed.
 */
public class ClientSmokeTest {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final boolean ENABLED = Boolean.getBoolean("capsule.clientsmoke");
    private static final String LEVEL_NAME = "capsule-smoke";
    private static final int CAPTURE_SIZE = 5;
    private static final int CHEST_DIAMONDS = 5;
    /**
     * Pixels of the missing texture tolerated in a screenshot, far less than one item slot.
     */
    private static final int MAX_MISSING_TEXTURE_PIXELS = 64;
    private static final int SLOT_BACKGROUND = 0x8B;

    private static ClientSmokeTest instance;

    private final Scenario scenario = new Scenario();
    private final LogWatcher log = LogWatcher.install();
    private volatile int ground;
    private volatile String capturedStructure;
    private volatile String blueprintStructure;
    private volatile int deployedChestDiamonds = -1;
    private volatile int blueprintBlocks = -1;
    private final List<String> moddedBlocks = new ArrayList<>();
    private volatile int deployedModdedBlocks = -1;

    /**
     * Called by the loader entrypoint at client initialization, so that the log is watched from the model loading on.
     */
    public static void init() {
        if (ENABLED) instance = new ClientSmokeTest();
    }

    public static void onClientTick() {
        if (instance == null) return;
        // toasts and chat would hide parts of the screenshots; the chat is in the log anyway
        mc().getToasts().clear();
        mc().gui.getChat().clearMessages(false);
        instance.scenario.tick();
    }

    private ClientSmokeTest() {
        scenario.await("title screen", 20 * 600, () -> mc().getOverlay() == null
                        && (mc().screen instanceof TitleScreen || mc().screen instanceof AccessibilityOnboardingScreen))
                .run("create a flat creative world", this::createWorld)
                .await("world loaded", 20 * 300, () -> mc().level != null && mc().player != null && mc().screen == null)
                .async("prepare the capture area", 100, () -> onServer(this::prepareCaptureArea))
                .sleep(80);

        captureScenario();
        inventoryScenario();
        deployScenario();
        blueprintScenario();
        previewSurroundingsScenario();
        moddedBlocksScenario();

        scenario.finallyRun("check the log", () -> {
                    synchronized (log.problems) {
                        scenario.check("no capsule warning, missing model/texture or exception in the log", log.problems.isEmpty(), String.join("\n  ", log.problems));
                    }
                })
                .finallyRun("write the report", this::writeReport)
                .finallyRun("leave the world", () -> {
                    if (mc().level != null) {
                        mc().level.disconnect();
                        mc().disconnect(new GenericMessageScreen(Component.literal("Saving")));
                    }
                })
                .finallyAwait("world saved", 20 * 60, () -> mc().getSingleplayerServer() == null)
                .finallyRun("quit", () -> {
                    if (scenario.failed()) System.exit(1);
                    mc().stop();
                });
    }

    private void captureScenario() {
        scenario.run("screenshot", () -> screenshot("01-capture-base"))
                .async("give an empty capsule", 100, () -> onServer(p -> p.getInventory().setItem(0,
                        Capsule.newEmptyCapsuleItemStack(DyeColor.LIGHT_BLUE.getTextureDiffuseColor(), 0xFFD700, CAPTURE_SIZE, false, null, 0))))
                .run("select it", () -> select(0))
                .sleep(20)
                .run("screenshot", () -> screenshot("02-capture-base-with-empty-capsule"))
                .run("activate", this::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.EMPTY_ACTIVATED))
                .run("throw", this::rightClick)
                .sleep(3)
                .run("screenshot", () -> screenshot("03-capsule-thrown"))
                .await("capture animation", 100, () -> CaptureAnimation.renderedFrames > 0)
                .sleep(6)
                .run("screenshot", () -> screenshot("04a-capture-animation"))
                .sleep(4)
                .run("screenshot", () -> screenshot("04b-capture-animation"))
                .run("animate a capture the client did not see", () -> CaptureAnimation.start(mc().level, markerPos().above(3), markerPos(), CAPTURE_SIZE))
                .sleep(5)
                .run("screenshot", () -> screenshot("04c-capture-animation-unseen"))
                .run("check", () -> scenario.check("the capture animation plays with captureAnimation on", CaptureAnimation.renderedFrames > 0,
                        CaptureAnimation.renderedFrames + " frames rendered"))
                .await("captured", 100, serverCondition(p -> p.level().getBlockState(markerPos().above()).isAir()
                        && capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.LINKED)) != null))
                .sleep(20)
                .run("screenshot", () -> screenshot("04-captured"))
                .async("pick up the linked capsule", 100, () -> onServer(p -> pickUp(p, 0)))
                .await("linked capsule in hand", 40, () -> mainHandIs(CapsuleState.LINKED))
                .run("remember its template", () -> capturedStructure = CapsuleItem.getStructureName(mc().player.getMainHandItem()));
    }

    private void inventoryScenario() {
        scenario.async("give capsules of every kind", 200, () -> onServer(this::giveDisplayCapsules))
                .sleep(20)
                .run("screenshot", () -> screenshot("05-hotbar"))
                .run("check the item models", this::checkItemModelStates)
                .async("survival mode", 100, () -> onServer(p -> p.setGameMode(GameType.SURVIVAL)))
                .await("survival mode on the client", 40, () -> !mc().gameMode.hasInfiniteItems())
                .run("open the inventory on the linked capsule", () -> mc().setScreen(new HoverInventoryScreen(0)))
                .sleep(20)
                .run("screenshot", () -> screenshot("06-inventory-linked-tooltip", ((HoverInventoryScreen) mc().screen).capsuleSlots()))
                .run("hover the blueprint", () -> mc().setScreen(new HoverInventoryScreen(7)))
                .sleep(20)
                .run("screenshot", () -> screenshot("07-inventory-blueprint-tooltip", ((HoverInventoryScreen) mc().screen).capsuleSlots()))
                .run("close", () -> mc().setScreen(null))
                .async("creative mode", 100, () -> onServer(p -> p.setGameMode(GameType.CREATIVE)))
                .await("creative mode on the client", 40, () -> mc().gameMode.hasInfiniteItems())
                .run("open the creative inventory", () -> mc().setScreen(new CreativeModeInventoryScreen(mc().player, mc().player.connection.enabledFeatures(), false)))
                .sleep(10)
                .run("select the search tab", () -> pressKey(mc().options.keyChat))
                .sleep(5)
                .run("search capsule", () -> {
                    pressKey(mc().options.keyAttack);
                    "capsule".chars().forEach(c -> mc().screen.charTyped((char) c, 0));
                })
                .sleep(20)
                .run("screenshot", () -> screenshot("08-creative-search"))
                .run("close", () -> mc().setScreen(null));
        List<RecipeViewerProbe> viewers = RecipeViewerProbe.installed();
        if (viewers.isEmpty()) scenario.run("recipe viewers", () -> scenario.results.add("SKIP recipe viewers: none installed"));
        viewers.forEach(this::recipeViewerScenario);
    }

    private void recipeViewerScenario(RecipeViewerProbe viewer) {
        String name = viewer.name();
        scenario.await(name + " loaded", 1200, viewer::ready)
                .run(name, () -> {
                    ItemStack capsule = CapsuleItems.capsuleList.firstKey();
                    long recipes = viewer.craftingRecipes(capsule);
                    long pages = viewer.capsuleInformationPages();
                    scenario.check(name + " shows the capsule recipes", recipes > 0 && pages > 0,
                            recipes + " crafting recipes for " + capsule.getHoverName().getString() + ", " + pages + " capsule information pages");
                    List<String> tiersWithoutRecipe = CapsuleItems.capsuleList.keySet().stream()
                            .filter(tier -> viewer.craftingRecipes(tier) == 0)
                            .map(tier -> CapsuleItem.getSize(tier) + "/" + Integer.toHexString(CapsuleItem.getMaterialColor(tier)))
                            .toList();
                    scenario.check(name + " shows a recipe for every capsule tier", tiersWithoutRecipe.isEmpty(),
                            CapsuleItems.capsuleList.size() + " tiers, without recipe: " + tiersWithoutRecipe);
                    List<RecipeHolder<CraftingRecipe>> special = RecipeViewerContent.craftingRecipes();
                    List<String> specialWithoutRecipe = special.stream()
                            .filter(recipe -> viewer.craftingRecipes(recipe.value().getResultItem(mc().level.registryAccess())) == 0)
                            .map(recipe -> recipe.id().getPath())
                            .toList();
                    long prefabs = special.stream().filter(recipe -> recipe.id().getPath().startsWith("/prefab/")).count();
                    scenario.check(name + " shows the upgrade, clear, recovery, blueprint and prefab recipes", specialWithoutRecipe.isEmpty(),
                            special.size() + " recipes including " + prefabs + " prefabs, without recipe: " + specialWithoutRecipe);
                    viewer.showRecipes(capsule);
                })
                .sleep(20)
                .run("screenshot", () -> {
                    if (mc().screen != null) screenshot("09-" + name.toLowerCase(Locale.ROOT) + "-capsule-recipes");
                })
                .run("close", () -> mc().setScreen(null));
    }

    private void deployScenario() {
        scenario.async("go to the deploy area", 100, () -> onServer(p -> flyTo(p, 16.5, ground + 3, -8.5, 30)))
                .run("select the linked capsule", () -> select(0))
                .sleep(60)
                .run("third person view", () -> {
                    mc().player.setXRot(0);
                    mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                })
                .sleep(10)
                .run("screenshot", () -> screenshot("10-third-person-holding-capsule"))
                .run("first person view", () -> {
                    mc().player.setXRot(30);
                    mc().options.setCameraType(CameraType.FIRST_PERSON);
                })
                .sleep(10)
                .run("activate", this::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .await("full preview received", 100, () -> CapsulePreviewHandler.cachedFullPreview.containsKey(capturedStructure))
                .sleep(10)
                .run("screenshot", () -> screenshot("11-deploy-preview"))
                .run("rotate", this::leftClick)
                .await("rotated", 40, () -> CapsuleItem.getPlacement(mc().player.getMainHandItem()).getRotation() != Rotation.NONE)
                .sleep(10)
                .run("screenshot", () -> screenshot("12-deploy-preview-rotated"))
                .run("throw", this::rightClick)
                .sleep(4)
                .run("screenshot", () -> screenshot("13-capsule-thrown"))
                .await("deployed", 200, serverCondition(p -> capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)) != null))
                .async("count the deployed chest content", 100, () -> onServer(this::countDeployedChestDiamonds))
                .run("check", () -> scenario.check("the deployed chest keeps its content", deployedChestDiamonds == CHEST_DIAMONDS,
                        deployedChestDiamonds + " diamonds, expected " + CHEST_DIAMONDS))
                .sleep(20)
                .run("screenshot", () -> screenshot("14-deployed"))
                .run("aim at the deployed house with the oak hut reward", () -> {
                    select(5);
                    mc().player.setXRot(25);
                })
                .sleep(5)
                .run("activate", this::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ONE_USE_ACTIVATED))
                .sleep(20)
                .run("screenshot", () -> screenshot("14b-preview-over-deployed"))
                .run("back to the deployed capsule slot", () -> {
                    select(0);
                    mc().player.setXRot(30);
                })
                .async("pick up the deployed capsule", 100, () -> onServer(p -> pickUp(p, 0)))
                .await("deployed capsule in hand", 40, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(20)
                .run("screenshot", () -> screenshot("15-deployed-capsule-in-hand"))
                .run("turn the capture animation off", () -> {
                    ClientConfig.captureAnimation = false;
                    CaptureAnimation.renderedFrames = 0;
                })
                .run("undeploy", this::rightClick)
                .await("undeployed", 60, () -> mainHandIs(CapsuleState.LINKED))
                .sleep(20)
                .run("screenshot", () -> screenshot("16-undeployed"))
                .run("check", () -> {
                    scenario.check("no capture animation with captureAnimation off", CaptureAnimation.renderedFrames == 0,
                            CaptureAnimation.renderedFrames + " frames rendered");
                    ClientConfig.captureAnimation = true;
                });
    }

    private void blueprintScenario() {
        scenario.async("go to the blueprint area", 100, () -> onServer(p -> flyTo(p, 32.5, ground + 4, -12.5, 30)))
                .run("select the blueprint", () -> select(7))
                .sleep(60)
                .await("blueprint preview received", 100, () -> CapsulePreviewHandler.currentFullPreview.containsKey(blueprintStructure))
                .async("count the blueprint blocks", 100, () -> onServer(p -> blueprintBlocks = StructureSaver.getTemplate(p.getInventory().getItem(7), p.serverLevel()).getRight().getPalette().size()))
                .run("check", () -> {
                    int previewed = CapsulePreviewHandler.currentFullPreview.get(blueprintStructure).getPalette().size();
                    scenario.check("the preview shows the blueprint", previewed == blueprintBlocks, previewed + " blocks previewed, " + blueprintBlocks + " in the blueprint");
                })
                .sleep(10)
                .run("screenshot", () -> screenshot("17-blueprint-preview"))
                .run("deploy", this::rightClick)
                .await("blueprint deployed", 60, () -> mainHandIs(CapsuleState.DEPLOYED))
                .sleep(20)
                .run("screenshot", () -> screenshot("18-blueprint-deployed"));
    }

    /**
     * The captured house previewed on a pool, between glass blocks and against a grass bump, in first then third person.
     */
    private void previewSurroundingsScenario() {
        scenario.async("build the surroundings of a deploy spot", 100, () -> onServer(this::prepareSurroundings))
                .run("select the linked capsule", () -> select(0))
                .sleep(60)
                .run("activate", this::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .await("full preview received", 100, () -> CapsulePreviewHandler.cachedFullPreview.containsKey(capturedStructure))
                .sleep(10)
                .run("screenshot", () -> screenshot("19-preview-water-glass-terrain"))
                .run("third person view", () -> mc().options.setCameraType(CameraType.THIRD_PERSON_BACK))
                .sleep(10)
                .run("screenshot", () -> screenshot("20-preview-water-glass-terrain-third-person"))
                .run("first person view", () -> mc().options.setCameraType(CameraType.FIRST_PERSON));
    }

    /**
     * Blocks of the mods from issues (#76 farmland, #81 Mob Grinding Utils, #94 Integrated Dynamics, #117 Ad Astra) that
     * are installed, plus vanilla farmland and crops: captured, previewed and deployed.
     */
    private void moddedBlocksScenario() {
        scenario.async("place the modded blocks on a capture base", 100, () -> onServer(this::prepareModdedBlocks))
                .run("select the capture base, which shows no preview", () -> select(8))
                .sleep(40)
                .run("screenshot", () -> screenshot("21-modded-blocks"))
                .async("capture them", 100, () -> onServer(this::captureModdedBlocks))
                .run("select the capsule", () -> select(2))
                .sleep(40)
                .run("activate", this::rightClick)
                .await("activated", 40, () -> mainHandIs(CapsuleState.ACTIVATED))
                .await("full preview received", 100, () -> CapsulePreviewHandler.cachedFullPreview.containsKey(CapsuleItem.getStructureName(mc().player.getMainHandItem())))
                .sleep(10)
                .run("screenshot", () -> screenshot("22-modded-blocks-preview"))
                .run("throw", this::rightClick)
                .await("deployed", 200, serverCondition(p -> capsuleEntity(p, s -> CapsuleItem.hasState(s, CapsuleState.DEPLOYED)) != null))
                .async("count the deployed blocks", 100, () -> onServer(p -> deployedModdedBlocks = deployedBlocks(p)))
                .run("check", () -> scenario.check("the modded blocks deploy", deployedModdedBlocks == moddedBlocks.size(),
                        deployedModdedBlocks + " blocks deployed, " + moddedBlocks.size() + " captured: " + String.join(" ", moddedBlocks)))
                .sleep(20)
                .run("screenshot", () -> screenshot("23-modded-blocks-deployed"));
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private void createWorld() {
        Minecraft mc = mc();
        mc.options.onboardAccessibility = false;
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.renderDistance().set(5);
        mc.options.simulationDistance().set(5);
        mc.options.guiScale().set(2);
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
        mc.options.save();
        mc.resizeDisplay();
        try {
            FileUtils.deleteDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(LEVEL_NAME).toFile());
            FileUtils.deleteDirectory(outputDir().toFile());
            Files.createDirectories(outputDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(LEVEL_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(LEVEL_NAME, settings, new WorldOptions(0, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                new TitleScreen());
    }

    /**
     * A capture base under a 5×5×5 house in front of the player, and an empty one.
     */
    private void prepareCaptureArea(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.getInventory().clearContent();
        BlockPos marker = markerPos();
        BlockState markerState = CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP);
        level.setBlockAndUpdate(marker, markerState);
        // a second capture base, out of reach of the thrown capsule, with nothing above to see its top
        level.setBlockAndUpdate(new BlockPos(-6, ground, 10), markerState);
        for (BlockPos pos : BlockPos.betweenClosed(marker.offset(-2, 1, -2), marker.offset(2, 4, 2))) {
            int dx = pos.getX() - marker.getX(), dy = pos.getY() - marker.getY(), dz = pos.getZ() - marker.getZ();
            boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            BlockState state = dy == 1 ? Blocks.STONE_BRICKS.defaultBlockState()
                    : dy == 4 ? Blocks.OAK_SLAB.defaultBlockState()
                    : corner ? Blocks.OAK_LOG.defaultBlockState()
                    : wall && dx == 0 ? Blocks.GLASS.defaultBlockState()
                    : wall ? Blocks.OAK_PLANKS.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            level.setBlock(pos, state, 2 | 16);
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(marker.offset(0, 2, -2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        level.setBlock(marker.offset(0, 3, -2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
        level.setBlock(marker.offset(1, 2, 1), Blocks.POTTED_POPPY.defaultBlockState(), 2 | 16);
        level.setBlock(marker.offset(0, 5, 0), Blocks.LANTERN.defaultBlockState(), 2 | 16);
        BlockPos chest = marker.offset(-1, 2, 1);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2 | 16);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity be) be.setItem(0, new ItemStack(Items.DIAMOND, CHEST_DIAMONDS));
        flyTo(player, 0.5, ground + 3, -4.5, 25);
    }

    private void prepareModdedBlocks(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos marker = moddedBlocksMarkerPos();
        level.setBlockAndUpdate(marker, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        BlockPos floor = marker.above();
        for (int dx = -2; dx <= 2; dx++) {
            place(level, floor.offset(dx, 0, -2), "ad_astra:steel_cable");
            place(level, floor.offset(dx, 0, 0), dx < 2 ? "integrateddynamics:cable" : "integrateddynamics:variablestore");
        }
        for (int dx = -2; dx <= 0; dx++) place(level, floor.offset(dx, 0, -1), "ad_astra:desh_fluid_pipe");
        place(level, floor.offset(2, 0, -1), "ad_astra:oxygen_loader");
        String[][] fields = {{"minecraft:farmland", "minecraft:wheat"}, {"minecraft:farmland", "farmersdelight:cabbages"},
                {"farmersdelight:rich_soil_farmland", "farmersdelight:budding_tomatoes"}, {"farmersdelight:rich_soil_farmland", "minecraft:carrots"}};
        for (int i = 0; i < fields.length; i++) {
            if (place(level, floor.offset(i - 2, 0, 1), fields[i][0])) place(level, floor.offset(i - 2, 1, 1), fields[i][1]);
        }
        place(level, floor.offset(2, 0, 1), "farmersdelight:cooking_pot");
        place(level, floor.offset(-2, 0, 2), "mob_grinding_utils:dreadful_dirt");
        place(level, floor.offset(-1, 0, 2), "mob_grinding_utils:delightful_dirt");
        place(level, floor.offset(1, 0, 2), "farmersdelight:cutting_board");
        place(level, floor.offset(2, 0, 2), "ad_astra:steel_block");
        attachPart(player, floor.offset(-1, 0, 0), "integratedtunnels:part_interface_item");
        attachPart(player, floor.offset(1, 0, 0), "integratedtunnels:part_exporter_item");
        moddedBlocks.clear();
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 4, 2))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) moddedBlocks.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        }
        flyTo(player, marker.getX() + 0.5, ground + 3, marker.getZ() - 7.5, 30);
    }

    private BlockPos moddedBlocksMarkerPos() {
        return new BlockPos(-24, ground, 24);
    }

    /**
     * Places the block if its mod is installed.
     */
    private static boolean place(ServerLevel level, BlockPos pos, String id) {
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id));
        block.ifPresent(b -> level.setBlockAndUpdate(pos, b.defaultBlockState()));
        return block.isPresent();
    }

    /**
     * Uses a part item on the top of a cable, as a player attaching it.
     */
    private static void attachPart(ServerPlayer player, BlockPos cable, String id) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).ifPresent(item -> {
            ItemStack held = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(cable).add(0, 0.5, 0), Direction.UP, cable, false)));
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
        });
    }

    private void captureModdedBlocks(ServerPlayer player) {
        ItemStack capsule = Capsule.newEmptyCapsuleItemStack(0x8B4513, 0xFFD700, CAPTURE_SIZE, false, "Modded blocks", 0);
        Capsule.captureAtPosition(capsule, player, CAPTURE_SIZE, player.serverLevel(), moddedBlocksMarkerPos().offset(-2, 1, -2));
        player.getInventory().setItem(2, capsule);
        flyTo(player, -23.5, ground + 3, -7.5, 30);
    }

    /**
     * Non air blocks in the area of the capsule lying around.
     */
    private static int deployedBlocks(ServerPlayer player) {
        CompoundTag spawn = NBTHelper.getOrCreateTag(capsuleEntity(player, s -> true).getItem()).getCompound("spawnPosition");
        BlockPos start = new BlockPos(spawn.getInt("x"), spawn.getInt("y"), spawn.getInt("z"));
        return (int) BlockPos.betweenClosedStream(start, start.offset(CAPTURE_SIZE - 1, CAPTURE_SIZE - 1, CAPTURE_SIZE - 1))
                .filter(pos -> !player.serverLevel().getBlockState(pos).isAir())
                .count();
    }

    /**
     * A deploy spot on a pool, glass in front and behind, a grass bump overlapping its side, and the player looking at it.
     */
    private void prepareSurroundings(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spot = new BlockPos(48, ground, -24);
        for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-4, -1, -4), spot.offset(1, -1, 3))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2 | 16);
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                level.setBlock(spot.offset(dx, dy, 4), (dx & 1) == 0 ? Blocks.GLASS.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2 | 16);
            }
        }
        for (int dy = 0; dy <= 1; dy++) {
            level.setBlock(spot.offset(-2, dy, -3), Blocks.GLASS.defaultBlockState(), 2 | 16);
            level.setBlock(spot.offset(-1, dy, -3), Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), 2 | 16);
        }
        for (BlockPos pos : BlockPos.betweenClosed(spot.offset(2, 0, -1), spot.offset(5, 0, 1))) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
        }
        for (BlockPos pos : BlockPos.betweenClosed(spot.offset(3, 1, -1), spot.offset(5, 1, 0))) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
        }
        // looking down at 30°, the eye 4.6 blocks above the ground aims 8 blocks ahead
        flyTo(player, spot.getX() + 0.5, ground + 3, spot.getZ() - 7.5, 30);
    }

    /**
     * A view from above, flying so that the camera stays where it is put.
     */
    private static void flyTo(ServerPlayer player, double x, double y, double z, float pitch) {
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.connection.teleport(x, y, z, 0, pitch);
    }

    private BlockPos markerPos() {
        return new BlockPos(0, ground, 4);
    }

    /**
     * Hotbar: linked (captured), wood, dyed iron, OP, deployed, one-use reward, recovery, blueprint, capture base.
     * Inventory: the other materials, the 16 dye colors, an uncharged blueprint, an upgraded and an enchanted capsule.
     */
    private void checkItemModelStates() {
        ResourceLocation stateProperty = ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "state");
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = mc().player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof CapsuleItem)) continue;
            CapsuleState state = CapsuleItem.getState(stack);
            ItemPropertyFunction property = ItemProperties.getProperty(stack, stateProperty);
            float value = property == null ? -1 : property.call(stack, mc().level, mc().player, 0);
            scenario.check("hotbar slot " + slot + " uses the " + state + " model", value == state.getValue(), "capsule:state is " + value);
        }
    }

    private void giveDisplayCapsules(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack linked = inventory.getItem(0);
        List<ItemStack> materials = CapsuleItems.capsuleList.keySet().stream().map(ItemStack::copy).toList();
        ItemStack iron = materials.get(1);

        ItemStack deployed = linked.copy();
        CapsuleItem.setState(deployed, CapsuleState.DEPLOYED);
        MinecraftNBT.setColor(deployed, DyeColor.YELLOW.getTextureDiffuseColor());
        ItemStack recovery = linked.copy();
        CapsuleItem.setOneUse(recovery);
        ItemStack reward = Capsule.newRewardCapsuleItemStack(Config.starterTemplatesPath + "/_stater_oak_hut",
                DyeColor.GREEN.getTextureDiffuseColor(), 0xB8945F, 3, "Oak hut", "Lythom");
        ItemStack blueprint = blueprint(player, Config.prefabsTemplatesPath + "/castle_wall");
        blueprintStructure = CapsuleItem.getStructureName(blueprint);

        List<ItemStack> hotbar = List.of(linked, materials.get(0), dyed(iron, DyeColor.RED), CapsuleItems.opCapsuleList.firstKey().copy(),
                deployed, reward, recovery, blueprint, new ItemStack(CapsuleBlocks.CAPSULE_MARKER_ITEM.get()));
        List<ItemStack> main = new ArrayList<>(materials.subList(2, materials.size()));
        for (DyeColor color : DyeColor.values()) main.add(dyed(iron, color));
        ItemStack uncharged = blueprint.copy();
        CapsuleItem.setState(uncharged, CapsuleState.DEPLOYED);
        main.add(uncharged);
        main.add(CapsuleItems.getUpgradedCapsule(iron, 3));
        ItemStack recall = iron.copy();
        recall.enchant(player.serverLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(CapsuleEnchantments.RECALL), 1);
        main.add(recall);

        for (int i = 0; i < hotbar.size(); i++) inventory.setItem(i, hotbar.get(i));
        for (int i = 0; i < main.size(); i++) inventory.setItem(9 + i, main.get(i));
    }

    private static ItemStack dyed(ItemStack capsule, DyeColor color) {
        ItemStack copy = capsule.copy();
        MinecraftNBT.setColor(copy, color.getTextureDiffuseColor() & 0xFFFFFF);
        return copy;
    }

    /**
     * A charged blueprint of a prefab, as given by /capsule giveBlueprint.
     */
    private static ItemStack blueprint(ServerPlayer player, String prefabPath) {
        CapsuleTemplate source = Capsule.getRewardTemplateIfExists(prefabPath, player.getServer());
        int size = Math.max(source.getSize().getX(), Math.max(source.getSize().getY(), source.getSize().getZ())) | 1;
        ItemStack blueprint = Capsule.newEmptyCapsuleItemStack(0x3C44AA, 0xFFFFFF, size, false, "Castle wall", 0);
        CapsuleItem.setState(blueprint, CapsuleState.DEPLOYED);
        CapsuleItem.setBlueprint(blueprint);
        CapsuleItem.setStructureName(blueprint, StructureSaver.createBlueprintTemplate(prefabPath, blueprint, player.serverLevel(), player));
        Capsule.reloadBlueprint(blueprint, player.serverLevel(), player);
        return blueprint;
    }

    /**
     * Puts the thrown capsule lying around in the given hotbar slot.
     */
    private static void pickUp(ServerPlayer player, int slot) {
        ItemEntity entity = capsuleEntity(player, s -> true);
        player.getInventory().setItem(slot, entity.getItem().copy());
        entity.discard();
    }

    private void countDeployedChestDiamonds(ServerPlayer player) {
        deployedChestDiamonds = Optional.ofNullable(capsuleEntity(player, s -> true))
                .map(e -> NBTHelper.getOrCreateTag(e.getItem()).getCompound("spawnPosition"))
                .map(spawn -> BlockPos.betweenClosedStream(
                                new BlockPos(spawn.getInt("x"), spawn.getInt("y"), spawn.getInt("z")),
                                new BlockPos(spawn.getInt("x"), spawn.getInt("y"), spawn.getInt("z")).offset(CAPTURE_SIZE, CAPTURE_SIZE, CAPTURE_SIZE))
                        .map(pos -> player.serverLevel().getBlockEntity(pos))
                        .filter(be -> be instanceof ChestBlockEntity)
                        .mapToInt(be -> ((ChestBlockEntity) be).countItem(Items.DIAMOND))
                        .sum())
                .orElse(-1);
    }

    private CompletableFuture<Void> onServer(Consumer<ServerPlayer> action) {
        IntegratedServer server = mc().getSingleplayerServer();
        UUID id = mc().player.getUUID();
        return server.submit(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    /**
     * A condition checked on the server thread, polled from the client ticks.
     */
    private BooleanSupplier serverCondition(Predicate<ServerPlayer> condition) {
        List<CompletableFuture<Boolean>> pending = new ArrayList<>(1);
        return () -> {
            if (pending.isEmpty()) {
                IntegratedServer server = mc().getSingleplayerServer();
                UUID id = mc().player.getUUID();
                pending.add(server.submit(() -> condition.test(server.getPlayerList().getPlayer(id))));
            }
            if (!pending.getFirst().isDone()) return false;
            return pending.removeFirst().join();
        };
    }

    /**
     * A capsule lying around. Its stack is only up to date on the server: changes of the stack of an item entity are not
     * synchronized.
     */
    private static ItemEntity capsuleEntity(ServerPlayer player, Predicate<ItemStack> predicate) {
        return player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(32),
                e -> e.getItem().getItem() instanceof CapsuleItem && predicate.test(e.getItem())).stream().findFirst().orElse(null);
    }

    private static boolean mainHandIs(CapsuleState state) {
        return CapsuleItem.hasState(mc().player.getMainHandItem(), state);
    }

    private static void select(int slot) {
        mc().player.getInventory().selected = slot;
    }

    private void rightClick() {
        KeyMapping.click(mc().options.keyUse.getDefaultKey());
    }

    private void leftClick() {
        KeyMapping.click(mc().options.keyAttack.getDefaultKey());
    }

    private static void pressKey(KeyMapping key) {
        mc().screen.keyPressed(key.getDefaultKey().getValue(), 0, 0);
    }

    private static Path outputDir() {
        return mc().gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("capsule-smoke");
    }

    private void screenshot(String name) {
        screenshot(name, List.of());
    }

    /**
     * Saves the last rendered frame, and checks that it shows no missing texture and that the given slots (GUI
     * coordinates of 16×16 item slots on the inventory background) are not empty.
     */
    private void screenshot(String name, List<int[]> itemSlots) {
        try (NativeImage image = Screenshot.takeScreenshot(mc().getMainRenderTarget())) {
            int missing = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    if (isMissingTextureMagenta(image.getPixelRGBA(x, y))) missing++;
                }
            }
            scenario.check(name + " has no missing texture", missing <= MAX_MISSING_TEXTURE_PIXELS, missing + " missing texture pixels");
            double scale = mc().getWindow().getGuiScale();
            int emptySlots = 0;
            for (int[] slot : itemSlots) {
                int differing = 0, total = 0;
                for (int x = (int) (slot[0] * scale); x < (int) ((slot[0] + 16) * scale); x++) {
                    for (int y = (int) (slot[1] * scale); y < (int) ((slot[1] + 16) * scale); y++) {
                        int c = image.getPixelRGBA(x, y);
                        total++;
                        if (Math.abs((c & 0xFF) - SLOT_BACKGROUND) > 12 || Math.abs((c >> 8 & 0xFF) - SLOT_BACKGROUND) > 12 || Math.abs((c >> 16 & 0xFF) - SLOT_BACKGROUND) > 12) differing++;
                    }
                }
                if (differing < total / 8) emptySlots++;
            }
            if (!itemSlots.isEmpty()) {
                scenario.check(name + " shows every item", emptySlots == 0, emptySlots + " of " + itemSlots.size() + " slots look empty");
            }
            image.writeToFile(outputDir().resolve(name + ".png"));
        } catch (IOException e) {
            scenario.check(name + " saved", false, e.toString());
        }
    }

    /**
     * The magenta of the missing texture, possibly darkened by shading.
     */
    private static boolean isMissingTextureMagenta(int abgr) {
        int r = abgr & 0xFF, g = abgr >> 8 & 0xFF, b = abgr >> 16 & 0xFF;
        return r > 100 && b > 100 && g < 30 && Math.abs(r - b) < 20;
    }

    private void writeReport() {
        List<String> lines = new ArrayList<>(scenario.results);
        lines.add("RESULT " + (scenario.failed() ? "FAIL" : "PASS"));
        try {
            Files.createDirectories(outputDir());
            Files.write(outputDir().resolve("report.txt"), lines);
        } catch (IOException e) {
            LOGGER.error("Could not write the client smoke report", e);
        }
        LOGGER.info("Client smoke test:\n{}", String.join("\n", lines));
    }

    /**
     * The survival inventory with the mouse over a hotbar slot, showing its tooltip.
     */
    private static class HoverInventoryScreen extends InventoryScreen {
        private final int hoveredHotbarSlot;

        HoverInventoryScreen(int hoveredHotbarSlot) {
            super(mc().player);
            this.hoveredHotbarSlot = hoveredHotbarSlot;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Slot slot = menu.slots.stream().filter(s -> s.container instanceof Inventory && s.getContainerSlot() == hoveredHotbarSlot).findFirst().orElseThrow();
            super.render(guiGraphics, leftPos + slot.x + 8, topPos + slot.y + 8, partialTick);
        }

        List<int[]> capsuleSlots() {
            return menu.slots.stream()
                    .filter(s -> s.getItem().getItem() instanceof CapsuleItem)
                    .map(s -> new int[]{leftPos + s.x, topPos + s.y})
                    .toList();
        }
    }
}
