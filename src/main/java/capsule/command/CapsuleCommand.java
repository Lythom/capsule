package capsule.command;

import capsule.Config;
import capsule.StructureSaver;
import capsule.enchantments.CapsuleEnchantments;
import capsule.helpers.Capsule;
import capsule.helpers.Files;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.loot.CapsuleLootEntry;
import capsule.loot.CapsuleLootTableHook;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;

/**
 * @author Lythom
 */
public class CapsuleCommand {

    public static List<ServerPlayer> sentUsageURL = new ArrayList<>();

    public static String[] getStructuresList(ServerLevel world) {
        Path capsuleStructuresPath = world.getServer().getWorldPath(new LevelResource("generated/capsules/structures"));
        Path minecraftStructuresPath = world.getServer().getWorldPath(new LevelResource("generated/capsules/structures"));
        return ArrayUtils.addAll(
                capsuleStructuresPath.toFile().list(),
                minecraftStructuresPath.toFile().list()
        );
    }

    public static String[] getRewardsList() {
        return (new File(Config.rewardTemplatesPath)).list();
    }

    private static SuggestionProvider<CommandSourceStack> SUGGEST_REWARD() {
        return (__, builder) -> {
            String[] rewards = getRewardsList();
            if (rewards == null) rewards = new String[0];
            return SharedSuggestionProvider.suggest(rewards, builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATE() {
        return (context, builder) -> {
            String[] tpls = getStructuresList(context.getSource().getLevel());
            if (tpls == null) tpls = new String[0];
            return SharedSuggestionProvider.suggest(tpls, builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> SUGGEST_COLORS() {
        return (context, builder) -> SharedSuggestionProvider.suggest(CapsuleLootEntry.COLOR_PALETTE, builder);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        final LiteralArgumentBuilder<CommandSourceStack> capsuleCommand = Commands.literal("capsule");

        capsuleCommand
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            int count = 0;
                            if (!sentUsageURL.contains(ctx.getSource().getPlayerOrException())) {
                                Component msg = Component.literal(
                                        "see Capsule commands usages at " + ChatFormatting.UNDERLINE + "https://github.com/Lythom/capsule/wiki/Commands");
                                msg.getStyle().withClickEvent(new ClickEvent(Action.OPEN_URL, "https://github.com/Lythom/capsule/wiki/Commands"));
                                ctx.getSource().sendSuccess(msg, false);
                                sentUsageURL.add(ctx.getSource().getPlayerOrException());
                                count++;
                            }
                            Map<CommandNode<CommandSourceStack>, String> map = dispatcher.getSmartUsage(ctx.getRootNode().getChild("capsule"), ctx.getSource());

                            for (String s : map.values()) {
                                ctx.getSource().sendSuccess(Component.literal("/" + s), false);
                            }

                            return map.size() + count;
                        }))
                // giveEmpty [size] [overpowered]
                .then(Commands.literal("giveEmpty")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> executeGiveEmpty(ctx.getSource().getPlayerOrException(), 3, false))
                        .then(Commands.argument("size", integer(1, CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE))
                                .executes(ctx -> executeGiveEmpty(ctx.getSource().getPlayerOrException(), getInteger(ctx, "size"), false))
                                .then(Commands.argument("overpowered", BoolArgumentType.bool())
                                        .executes(ctx -> executeGiveEmpty(ctx.getSource().getPlayerOrException(), getInteger(ctx, "size"), getBool(ctx, "overpowered")))
                                )
                        )
                )
                // giveLinked <rewardTemplateName> [playerName] [withRecall]
                .then(Commands.literal("giveLinked")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("rewardTemplateName", string())
                                .suggests(SUGGEST_REWARD())
                                .executes(ctx -> executeGiveLinked(ctx.getSource().getPlayerOrException(), getString(ctx, "rewardTemplateName"), false))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeGiveLinked(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName"), false))
                                        .then(Commands.argument("withRecall", bool())
                                                .executes(ctx -> executeGiveLinked(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName"), getBool(ctx, "withRecall")))
                                        )
                                )
                        )
                )
                // giveBlueprint <rewardTemplateName> [playerName]
                .then(Commands.literal("giveBlueprint")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("rewardTemplateName", string())
                                .suggests(SUGGEST_REWARD())
                                .executes(ctx -> executeGiveBlueprint(ctx.getSource().getPlayerOrException(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeGiveBlueprint(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // exportHeldItem
                .then(Commands.literal("exportHeldItem")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> executeExportHeldItem(ctx.getSource().getPlayerOrException()))
                )
                // exportSeenBlock
                .then(Commands.literal("exportSeenBlock")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> executeExportSeenBlock(ctx.getSource().getPlayerOrException()))
                )
                // fromExistingReward <rewardTemplateName> [playerName]
                .then(Commands.literal("fromExistingReward")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("rewardTemplateName", string())
                                .suggests(SUGGEST_REWARD())
                                .executes(ctx -> executeFromExistingReward(ctx.getSource().getPlayerOrException(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeFromExistingReward(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // fromStructure <structureTemplateName> [playerName]
                .then(Commands.literal("fromStructure")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("rewardTemplateName", string())
                                .suggests(SUGGEST_TEMPLATE())
                                .executes(ctx -> executeFromStructure(ctx.getSource().getPlayerOrException(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeFromStructure(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // fromHeldCapsule [outputTemplateName]
                .then(Commands.literal("fromHeldCapsule")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("outputTemplateName", string())
                                .executes(ctx -> executeFromHeldCapsule(ctx.getSource().getPlayerOrException(), getString(ctx, "outputTemplateName")))
                        )
                )
                // giveRandomLoot [playerName]
                .then(Commands.literal("giveRandomLoot")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> executeGiveRandomLoot(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", player())
                                .executes(ctx -> executeGiveRandomLoot(getPlayer(ctx, "target")))
                        )
                )
                // reloadLootList
                .then(Commands.literal("reloadLootList")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> {
                            ResourceManager resourceManager = ctx.getSource().getServer().getResourceManager();
                            Files.populateAndLoadLootList(Config.getCapsuleConfigDir().toFile(), Config.lootTemplatesData, resourceManager);
                            return 1;
                        })
                )
                // reloadWhitelist
                .then(Commands.literal("reloadWhitelist")
                        .requires((player) -> player.hasPermission(2))
                        .executes(ctx -> {
                            ResourceManager resourceManager = ctx.getSource().getServer().getResourceManager();
                            Files.populateAndLoadLootList(Config.getCapsuleConfigDir().toFile(), Config.lootTemplatesData, resourceManager);
                            Config.starterTemplatesList = Files.populateStarters(Config.getCapsuleConfigDir().toFile(), Config.starterTemplatesPath, resourceManager);
                            Config.blueprintWhitelist = Files.populateWhitelistConfig(Config.getCapsuleConfigDir().toFile(), resourceManager);
                            return 1;
                        })
                )
                // setAuthor <authorName>
                .then(Commands.literal("setAuthor")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("authorName", string())
                                .executes(ctx -> executeSetAuthor(ctx.getSource().getPlayerOrException(), getString(ctx, "authorName")))
                        )
                )
                // setBaseColor <color>
                .then(Commands.literal("setBaseColor")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("color", string())
                                .suggests(SUGGEST_COLORS())
                                .executes(ctx -> executeSetBaseColor(ctx.getSource().getPlayerOrException(), getString(ctx, "color")))
                        )
                )
                // setMaterialColor <color>
                .then(Commands.literal("setMaterialColor")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("color", string())
                                .suggests(SUGGEST_COLORS())
                                .executes(ctx -> executeSetMaterialColor(ctx.getSource().getPlayerOrException(), getString(ctx, "color")))
                        )
                )
                // setMaterialColor <color>
                .then(Commands.literal("downloadTemplate")
                        .requires((player) -> player.hasPermission(0))
                        .executes(ctx -> executeDownloadTemplate(ctx.getSource().getPlayerOrException()))
                )
                // setYOffset yOffset
                .then(Commands.literal("setYOffset")
                        .requires((player) -> player.hasPermission(2))
                        .then(Commands.argument("yOffset", integer())
                                .executes(ctx -> executeSetYOffset(ctx.getSource().getPlayerOrException(), getInteger(ctx, "yOffset")))
                        )
                )
        ;

        dispatcher.register(capsuleCommand);
    }

    private static int executeDownloadTemplate(ServerPlayer getPlayerOrException) {
        return 0;
    }

    private static int executeGiveEmpty(ServerPlayer player, int size, boolean overpowered) {
        if (player != null) {
            ItemStack capsule = Capsule.newEmptyCapsuleItemStack(
                    0xFFFFFF,
                    0xFFFFFF,
                    size,
                    overpowered,
                    null,
                    null
            );
            giveCapsule(capsule, player);
            return 1;
        }
        return 0;
    }

    private static int executeGiveLinked(ServerPlayer player, String rewardTemplateName, boolean withRecall) {
        String templateName = rewardTemplateName.replaceAll(".nbt", "").replaceAll(".schematic", "");
        if (player != null && !StringUtil.isNullOrEmpty(templateName)) {
            ItemStack capsule = Capsule.createLinkedCapsuleFromReward(Config.getRewardPathFromName(templateName), player);
            if (withRecall) {
                capsule.enchant(CapsuleEnchantments.RECALL.get(), 1);
            }
            if (!capsule.isEmpty()) {
                giveCapsule(capsule, player);
            } else {
                throw new CommandRuntimeException(Component.literal("Reward Capsule " + rewardTemplateName + " not found "));
            }
            return 1;
        }
        return 0;
    }

    private static int executeGiveBlueprint(ServerPlayer player, String rewardTemplateName) {
        String templateName = rewardTemplateName.replaceAll(".nbt", "").replaceAll(".schematic", "");
        if (player != null && !StringUtil.isNullOrEmpty(templateName)) {

            CapsuleTemplate srcTemplate = Capsule.getRewardTemplateIfExists(Config.getRewardPathFromName(templateName), player.getServer());
            if (srcTemplate != null) {
                int size = Math.max(srcTemplate.getSize().getX(), Math.max(srcTemplate.getSize().getY(), srcTemplate.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                ItemStack capsule = Capsule.newEmptyCapsuleItemStack(
                        3949738,
                        0xFFFFFF,
                        size,
                        false,
                        Capsule.labelFromPath(templateName),
                        0
                );
                CapsuleItem.setState(capsule, DEPLOYED);
                CapsuleItem.setBlueprint(capsule);

                String destTemplate = StructureSaver.createBlueprintTemplate(
                        Config.getRewardPathFromName(templateName), capsule,
                        player.getLevel(),
                        player
                );
                CapsuleItem.setStructureName(capsule, destTemplate);
                giveCapsule(capsule, player);

            } else {
                throw new CommandRuntimeException(Component.literal("Reward Capsule " + rewardTemplateName + " not found "));
            }
        }
        return 0;
    }

    private static int executeGiveRandomLoot(ServerPlayer player) throws CommandRuntimeException {
        if (player != null) {
            LootContext.Builder lootcontext$builder = (new LootContext.Builder(player.getLevel())).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.ORIGIN, player.position()).withRandom(player.getRandom());
            List<ItemStack> loots = new ArrayList<>();
            CapsuleLootTableHook.capsulePool.addRandomItems(loots::add, lootcontext$builder.create(LootContextParamSets.COMMAND));
            if (loots.size() <= 0) {
                player.sendSystemMessage(Component.literal("No loot this time !"));
            } else {
                for (ItemStack loot : loots) {
                    giveCapsule(loot, player);
                    return 1;
                }
            }
        }
        return 0;
    }

    private static int executeFromExistingReward(ServerPlayer player, String templateName) throws CommandRuntimeException {
        if (player != null && !StringUtil.isNullOrEmpty(templateName) && player.getLevel() instanceof ServerLevel) {
            String structurePath = Config.getRewardPathFromName(templateName);
            CapsuleTemplateManager templatemanager = StructureSaver.getRewardManager(player.getServer().getResourceManager());
            CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(structurePath));
            if (template != null) {
                int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        structurePath,
                        CapsuleLootEntry.getRandomColor(),
                        CapsuleLootEntry.getRandomColor(),
                        size,
                        Capsule.labelFromPath(templateName),
                        template.getAuthor());
                CapsuleItem.setCanRotate(capsule, template.canRotate());
                giveCapsule(capsule, player);

            } else {
                throw new CommandRuntimeException(Component.literal("Reward Capsule \"" + templateName + "\" not found "));
            }
            return 1;
        }
        return 0;
    }

    private static int executeFromStructure(ServerPlayer player, String templateName) throws CommandRuntimeException {
        if (player != null && !StringUtil.isNullOrEmpty(templateName) && player.getLevel() instanceof ServerLevel) {
            CompoundTag data = new CompoundTag();
            int size = -1;
            String author = null;

            // template
            StructureTemplateManager templatemanager = player.getLevel().getStructureManager();
            String path = templateName.endsWith(".nbt") ? templateName.replace(".nbt", "") : templateName;
            Optional<StructureTemplate> templateO = templatemanager.get(new ResourceLocation(path));
            if (templateO.isPresent()) {
                StructureTemplate template = templateO.get();
                size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                author = template.getAuthor();
                template.save(data);
            } else {
                CapsuleTemplateManager capsuletemplatemanager = StructureSaver.getTemplateManager(player.getLevel().getServer());
                CapsuleTemplate ctemplate = capsuletemplatemanager.getOrCreateTemplate(new ResourceLocation(path));
                size = Math.max(ctemplate.getSize().getX(), Math.max(ctemplate.getSize().getY(), ctemplate.getSize().getZ()));
                author = ctemplate.getAuthor();
                ctemplate.save(data);
            }

            if (size > -1) {
                if (size % 2 == 0)
                    size++;
                // create a destination template
                ResourceLocation destinationLocation = new ResourceLocation(Config.rewardTemplatesPath + "/" + path);
                CapsuleTemplateManager destManager = StructureSaver.getRewardManager(player.getServer().getResourceManager());
                CapsuleTemplate destTemplate = destManager.getOrCreateTemplate(destinationLocation);
                // write template from source data
                destTemplate.load(data, destinationLocation.toString());
                destManager.writeToFile(destinationLocation);

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        destinationLocation.toString(),
                        CapsuleLootEntry.getRandomColor(),
                        CapsuleLootEntry.getRandomColor(),
                        size,
                        path,
                        author);
                CapsuleItem.setCanRotate(capsule, destTemplate.canRotate());
                giveCapsule(capsule, player);
                return 1;
            } else {
                throw new CommandRuntimeException(Component.literal("Structure \"" + path + "\" not found "));
            }
        }
        return 0;
    }

    private static int executeFromHeldCapsule(ServerPlayer player, String templateName) throws CommandSyntaxException {
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                String outputName;
                if (StringUtil.isNullOrEmpty(templateName)) {
                    //noinspection ConstantConditions
                    outputName = heldItem.getTag().getString("label");
                } else {
                    outputName = templateName;
                }
                if (StringUtil.isNullOrEmpty(outputName)) {
                    throw new SimpleCommandExceptionType(Component.literal(
                            "/capsule fromHeldCapsule [outputName]. Please label the held capsule or provide an output name to be used for output template."
                    )).create();
                }

                String destinationTemplateLocation = Config.getRewardPathFromName(outputName.toLowerCase().replace(" ", "_").replace(":", "-"));
                boolean created = StructureSaver.copyFromCapsuleTemplate(
                        heldItem,
                        destinationTemplateLocation,
                        StructureSaver.getRewardManager(player.getServer().getResourceManager()),
                        player.getLevel(),
                        false,
                        null
                );

                if (!created) {
                    player.sendSystemMessage(Component.literal("Could not duplicate the capsule template. Either the source template don't exist or the destination folder dont exist."));
                    return 0;
                }

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        destinationTemplateLocation,
                        CapsuleItem.getBaseColor(heldItem),
                        CapsuleItem.getMaterialColor(heldItem),
                        CapsuleItem.getSize(heldItem),
                        outputName,
                        CapsuleItem.getAuthor(heldItem));
                CapsuleItem.setCanRotate(capsule, CapsuleItem.canRotate(heldItem));
                giveCapsule(capsule, player);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetYOffset(ServerPlayer player, int yOffset) {
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setYOffset(heldItem, yOffset);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetMaterialColor(ServerPlayer player, String colorAsInt) throws CommandSyntaxException {
        int color;
        try {
            color = Integer.decode(colorAsInt);
        } catch (NumberFormatException e) {
            throw new SimpleCommandExceptionType(Component.literal("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456")).create();
        }

        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setMaterialColor(heldItem, color);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetBaseColor(ServerPlayer player, String colorAsInt) throws CommandSyntaxException {
        int color = 0;
        try {
            color = Integer.decode(colorAsInt);
        } catch (NumberFormatException e) {
            throw new SimpleCommandExceptionType(Component.literal("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456")).create();
        }

        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setBaseColor(heldItem, color);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetAuthor(ServerPlayer player, String authorName) {
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                if (!StringUtil.isNullOrEmpty(authorName)) {
                    // set a new author
                    //noinspection ConstantConditions
                    heldItem.getTag().putString("author", authorName);
                    Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getLevel());
                    CapsuleTemplate template = templatepair.getRight();
                    CapsuleTemplateManager templatemanager = templatepair.getLeft();
                    if (template != null && templatemanager != null) {
                        template.setAuthor(authorName);
                        templatemanager.writeToFile(new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
                    }

                } else {
                    // called with one parameter = remove author information
                    //noinspection ConstantConditions
                    heldItem.getTag().remove("author");
                    Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getLevel());
                    CapsuleTemplate template = templatepair.getRight();
                    CapsuleTemplateManager templatemanager = templatepair.getLeft();
                    if (template != null && templatemanager != null) {
                        template.setAuthor("?");
                        templatemanager.writeToFile(new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
                    }
                }
                return 1;
            }
        }
        return 0;
    }

    private static int executeExportSeenBlock(ServerPlayer player) {
        if (player != null) {
            if (player.getServer() != null && !player.getServer().isDedicatedServer()) {
                BlockHitResult rtc = Spacial.clientRayTracePreview(player, Minecraft.getInstance().getFrameTime(), 50);

                if (rtc.getType() == HitResult.Type.BLOCK) {

                    BlockPos position = rtc.getBlockPos();
                    BlockState state = player.getLevel().getBlockState(position);
                    BlockEntity BlockEntity = player.getLevel().getBlockEntity(position);

                    String BlockEntityTag = BlockEntity == null ? "" : "{BlockEntityTag:" + BlockEntity.serializeNBT().toString() + "}";
                    String command = "/give @p " + ForgeRegistries.BLOCKS.getKey(state.getBlock()) + BlockEntityTag + " 1 ";
                    MutableComponent msg = Component.literal(command);
                    player.sendSystemMessage(msg.withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, command))
                    ));
                    return 1;
                }
            } else {
                player.sendSystemMessage(Component.literal("This command only works on an integrated server, not on an dedicated one"));
            }
        }
        return 0;
    }

    private static int executeExportHeldItem(ServerPlayer player) {
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty()) {

                String tag = heldItem.hasTag() ? String.valueOf(heldItem.getTag()) : "";

                String command = "/give @p " + ForgeRegistries.ITEMS.getKey(heldItem.getItem()) + tag + " 1 ";
                MutableComponent msg = Component.literal(command);
                msg.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy/Paste from client log (click to open)")));
                msg.getStyle().withClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                player.sendSystemMessage(msg);
                return 1;
            }
        }
        return 0;
    }

    private static void giveCapsule(ItemStack capsule, Player player) {
        ItemEntity entity = player.drop(capsule, false);
        entity.setNoPickUpDelay();
        entity.setOwner(player.getUUID());
    }
}
