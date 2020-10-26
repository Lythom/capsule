package capsule.command;

import capsule.Config;
import capsule.StructureSaver;
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
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.arguments.ColorArgument.color;
import static net.minecraft.command.arguments.EntityArgument.getPlayer;
import static net.minecraft.command.arguments.EntityArgument.player;

/**
 * @author Lythom
 */
public class CapsuleCommand {

    public static List<ServerPlayerEntity> sentUsageURL = new ArrayList<>();

    public static final String[] COMMAND_LIST = new String[]{
            "giveEmpty",
            "giveLinked",
            "giveBlueprint",
            "exportHeldItem",
            "exportSeenBlock",
            "fromExistingReward",
            "fromHeldCapsule",
            "fromStructure",
            "giveRandomLoot",
            "reloadLootList",
            "reloadWhitelist",
            "setAuthor",
            "setBaseColor",
            "setMaterialColor"
    };

    public static String[] getStructuresList(ServerPlayerEntity player) {
        return (new File(player.getServerWorld().getSaveHandler().getWorldDirectory(), "structures")).list();
    }

    public static String[] getRewardsList(ServerPlayerEntity player) {
        return (new File(Config.rewardTemplatesPath.get())).list();
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        final LiteralArgumentBuilder<CommandSource> capsuleCommand = Commands.literal("capsule").requires((player) -> player.hasPermissionLevel(2));

        capsuleCommand
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            int count = 0;
                            if (!sentUsageURL.contains(ctx.getSource().asPlayer())) {
                                StringTextComponent msg = new StringTextComponent(
                                        "see Capsule commands usages at " + TextFormatting.UNDERLINE + "https://github.com/Lythom/capsule/wiki/Commands");
                                msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, "https://github.com/Lythom/capsule/wiki/Commands"));
                                ctx.getSource().sendFeedback(msg, false);
                                sentUsageURL.add(ctx.getSource().asPlayer());
                                count++;
                            }
                            Map<CommandNode<CommandSource>, String> map = dispatcher.getSmartUsage(capsuleCommand.getRedirect(), ctx.getSource());

                            for (String s : map.values()) {
                                ctx.getSource().sendFeedback(new StringTextComponent("/" + s), false);
                            }

                            return map.size() + count;
                        }))
                // giveEmpty [size] [overpowered]
                .then(Commands.literal("giveEmpty")
                        .executes(ctx -> executeGiveEmpty(ctx.getSource().asPlayer(), 3, false))
                        .then(Commands.argument("size", integer(1, CapsuleItem.CAPSULE_MAX_CAPTURE_SIZE))
                                .executes(ctx -> executeGiveEmpty(ctx.getSource().asPlayer(), getInteger(ctx, "size"), getBool(ctx, "overpowered")))
                                .then(Commands.argument("overpowered", BoolArgumentType.bool())
                                        .executes(ctx -> executeGiveEmpty(ctx.getSource().asPlayer(), getInteger(ctx, "size"), getBool(ctx, "overpowered")))
                                )
                        )
                )
                // giveLinked <rewardTemplateName> [playerName]
                .then(Commands.literal("giveLinked")
                        // TODO templates suggestions from folder
                        .then(Commands.argument("rewardTemplateName", string())
                                .executes(ctx -> executeGiveLinked(ctx.getSource().asPlayer(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeGiveLinked(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // giveBlueprint <rewardTemplateName> [playerName]
                .then(Commands.literal("giveBlueprint")
                        .then(Commands.argument("rewardTemplateName", string())
                                .executes(ctx -> executeGiveBlueprint(ctx.getSource().asPlayer(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeGiveBlueprint(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // exportHeldItem
                .then(Commands.literal("exportHeldItem")
                        .executes(ctx -> executeExportHeldItem(ctx.getSource().asPlayer()))
                )
                // exportSeenBlock
                .then(Commands.literal("exportHeldItem")
                        .executes(ctx -> executeExportSeenBlock(ctx.getSource().asPlayer()))
                )
                // fromExistingReward <rewardTemplateName> [playerName]
                .then(Commands.literal("fromExistingReward")
                        .then(Commands.argument("rewardTemplateName", string())
                                .executes(ctx -> executeFromExistingReward(ctx.getSource().asPlayer(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeFromExistingReward(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // fromStructure <structureTemplateName> [playerName]
                .then(Commands.literal("fromStructure")
                        .then(Commands.argument("rewardTemplateName", string())
                                .executes(ctx -> executeFromStructure(ctx.getSource().asPlayer(), getString(ctx, "rewardTemplateName")))
                                .then(Commands.argument("target", player())
                                        .executes(ctx -> executeFromExistingReward(getPlayer(ctx, "target"), getString(ctx, "rewardTemplateName")))
                                )
                        )
                )
                // fromHeldCapsule [outputTemplateName]
                .then(Commands.literal("fromHeldCapsule")
                        .then(Commands.argument("outputTemplateName", string())
                                .executes(ctx -> executeFromHeldCapsule(ctx.getSource().asPlayer(), getString(ctx, "outputTemplateName")))
                        )
                )
                // giveRandomLoot [playerName]
                .then(Commands.literal("giveRandomLoot")
                        .executes(ctx -> executeExportHeldItem(ctx.getSource().asPlayer()))
                        .then(Commands.argument("target", player())
                                .executes(ctx -> executeGiveRandomLoot(getPlayer(ctx, "target")))
                        )
                )
                // reloadLootList
                .then(Commands.literal("reloadLootList")
                        .executes(ctx -> {
                            Files.populateAndLoadLootList(Config.configDir.toFile(), Config.lootTemplatesPaths.get(), Config.lootTemplatesData);
                            return 1;
                        })
                )
                // reloadWhitelist
                .then(Commands.literal("reloadWhitelist")
                        .executes(ctx -> {
                            Files.populateAndLoadLootList(Config.configDir.toFile(), Config.lootTemplatesPaths.get(), Config.lootTemplatesData);
                            Config.starterTemplatesList = Files.populateStarters(Config.configDir.toFile(), Config.starterTemplatesPath.get());
                            Config.blueprintWhitelist = Files.populateWhitelistConfig(Config.configDir.toFile());
                            return 1;
                        })
                )
                // setAuthor <authorName>
                .then(Commands.literal("setAuthor")
                        .then(Commands.argument("authorName", string())
                                .executes(ctx -> executeSetAuthor(ctx.getSource().asPlayer(), getString(ctx, "authorName")))
                        )
                )
                // setBaseColor <color>
                .then(Commands.literal("setBaseColor")
                        .then(Commands.argument("color", color())
                                        .executes(ctx -> executeSetBaseColor(ctx.getSource().asPlayer(), getString(ctx, "color")))
                                // TODO color suggestions and examples
                        )
                )
                // setMaterialColor <color>
                .then(Commands.literal("setMaterialColor")
                        .then(Commands.argument("color", color())
                                        .executes(ctx -> executeSetMaterialColor(ctx.getSource().asPlayer(), getString(ctx, "color")))
                                // TODO color suggestions and examples
                        )
                )
        ;
    }

    private static int executeGiveEmpty(ServerPlayerEntity player, int size, boolean overpowered) {
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

    private static int executeGiveLinked(ServerPlayerEntity player, String rewardTemplateName) {
        String templateName = rewardTemplateName.replaceAll(".nbt", "").replaceAll(".schematic", "");
        if (player != null && !StringUtils.isNullOrEmpty(templateName) && player.getEntityWorld() instanceof ServerWorld) {
            ItemStack capsule = Capsule.createLinkedCapsuleFromReward(Config.getRewardPathFromName(templateName), player);
            if (!capsule.isEmpty()) {
                giveCapsule(capsule, player);
            } else {
                throw new CommandException(new StringTextComponent("Reward Capsule " + rewardTemplateName + " not found "));
            }
            return 1;
        }
        return 0;
    }

    private static int executeGiveBlueprint(ServerPlayerEntity player, String rewardTemplateName) {
        String templateName = rewardTemplateName.replaceAll(".nbt", "").replaceAll(".schematic", "");
        if (player != null && !StringUtils.isNullOrEmpty(templateName) && player.getEntityWorld() instanceof ServerWorld) {

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
                CapsuleItem.setState(capsule, CapsuleItem.STATE_DEPLOYED);
                CapsuleItem.setBlueprint(capsule);

                String destTemplate = StructureSaver.createBlueprintTemplate(
                        Config.getRewardPathFromName(templateName), capsule,
                        player.getServerWorld(),
                        player
                );
                CapsuleItem.setStructureName(capsule, destTemplate);
                giveCapsule(capsule, player);

            } else {
                throw new CommandException(new StringTextComponent("Reward Capsule " + rewardTemplateName + " not found "));
            }
        }
        return 0;
    }

    private static int executeGiveRandomLoot(ServerPlayerEntity player) throws CommandException {
        if (player != null) {
            LootContext.Builder lootcontext$builder = new LootContext.Builder(player.getServerWorld());
            List<ItemStack> loots = new ArrayList<>();
            CapsuleLootTableHook.capsulePool.generate(loots::add, lootcontext$builder.build(LootParameterSets.COMMAND));
            if (loots.size() <= 0) {
                player.sendMessage(new StringTextComponent("No loot this time !"));
            } else {
                for (ItemStack loot : loots) {
                    giveCapsule(loot, player);
                    return 1;
                }
            }
        }
        return 0;
    }

    private static int executeFromExistingReward(ServerPlayerEntity player, String templateName) throws CommandException {
        if (player != null && !StringUtils.isNullOrEmpty(templateName) && player.getEntityWorld() instanceof ServerWorld) {

            String structurePath = Config.getRewardPathFromName(templateName);
            CapsuleTemplateManager templatemanager = StructureSaver.getRewardManager(player.getServer());
            CapsuleTemplate template = templatemanager.getTemplate(new ResourceLocation(structurePath));
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
                throw new CommandException(new StringTextComponent("Reward Capsule \"" + templateName + "\" not found "));
            }
            return 1;
        }
        return 0;
    }

    private static int executeFromStructure(ServerPlayerEntity player, String templateName) throws CommandException {
        if (player != null && !StringUtils.isNullOrEmpty(templateName) && player.getEntityWorld() instanceof ServerWorld) {
            // template
            TemplateManager templatemanager = player.getServerWorld().getStructureTemplateManager();
            Template template = templatemanager.getTemplate(new ResourceLocation(templateName));
            if (template != null) {
                int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                // get source template data
                CompoundNBT data = new CompoundNBT();
                template.writeToNBT(data);

                // create a destination template
                ResourceLocation destinationLocation = new ResourceLocation(Config.rewardTemplatesPath.get() + "/" + templateName);
                CapsuleTemplateManager destManager = StructureSaver.getRewardManager(player.getServer());
                CapsuleTemplate destTemplate = destManager.getTemplate(destinationLocation);
                // write template from source data
                destTemplate.read(data);
                destManager.writeToFile(destinationLocation);

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        destinationLocation.toString(),
                        CapsuleLootEntry.getRandomColor(),
                        CapsuleLootEntry.getRandomColor(),
                        size,
                        templateName,
                        template.getAuthor());
                CapsuleItem.setCanRotate(capsule, destTemplate.canRotate());
                giveCapsule(capsule, player);
                return 1;
            } else {
                throw new CommandException(new StringTextComponent("Structure \"" + templateName + "\" not found "));
            }
        }
        return 0;
    }

    private static int executeFromHeldCapsule(ServerPlayerEntity player, String templateName) throws CommandSyntaxException {
        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                String outputName;
                if (StringUtils.isNullOrEmpty(templateName)) {
                    //noinspection ConstantConditions
                    outputName = heldItem.getTag().getString("label");
                } else {
                    outputName = templateName;
                }
                if (StringUtils.isNullOrEmpty(outputName)) {
                    throw new SimpleCommandExceptionType(new StringTextComponent(
                            "/capsule fromHeldCapsule [outputName]. Please label the held capsule or provide an output name to be used for output template."
                    )).create();
                }

                String destinationTemplateLocation = Config.getRewardPathFromName(outputName.toLowerCase().replace(" ", "_").replace(":", "-"));
                boolean created = StructureSaver.copyFromCapsuleTemplate(
                        heldItem,
                        destinationTemplateLocation,
                        StructureSaver.getRewardManager(player.getServer()),
                        player.getServerWorld(),
                        false,
                        null
                );

                if (!created) {
                    player.sendMessage(new StringTextComponent("Could not duplicate the capsule template. Either the source template don't exist or the destination folder dont exist."));
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

    private static int executeSetMaterialColor(ServerPlayerEntity player, String colorAsInt) throws CommandSyntaxException {
        int color;
        try {
            color = Integer.decode(colorAsInt);
        } catch (NumberFormatException e) {
            throw new SimpleCommandExceptionType(new StringTextComponent("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456")).create();
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setMaterialColor(heldItem, color);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetBaseColor(ServerPlayerEntity player, String colorAsInt) throws CommandSyntaxException {
        int color = 0;
        try {
            color = Integer.decode(colorAsInt);
        } catch (NumberFormatException e) {
            throw new SimpleCommandExceptionType(new StringTextComponent("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456")).create();
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setBaseColor(heldItem, color);
                return 1;
            }
        }
        return 0;
    }

    private static int executeSetAuthor(ServerPlayerEntity player, String authorName) {
        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                if (!StringUtils.isNullOrEmpty(authorName)) {
                    // set a new author
                    //noinspection ConstantConditions
                    heldItem.getTag().putString("author", authorName);
                    Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
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
                    Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
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

    private static int executeExportSeenBlock(ServerPlayerEntity player) {
        if (player != null) {
            if (player.getServer() != null && !player.getServer().isDedicatedServer()) {
                BlockRayTraceResult rtc = Spacial.clientRayTracePreview(player, Minecraft.getInstance().getRenderPartialTicks(), 50);

                if (rtc.getType() == RayTraceResult.Type.BLOCK) {

                    BlockPos position = rtc.getPos();
                    BlockState state = player.getServerWorld().getBlockState(position);
                    TileEntity tileentity = player.getServerWorld().getTileEntity(position);

                    String command = "/give @p " + state.getBlock().getRegistryName().toString() + " 1 ";
                    if (tileentity != null) {
                        command += " {BlockEntityTag:" + tileentity.serializeNBT().toString() + "}";
                    }
                    StringTextComponent msg = new StringTextComponent(command);
                    msg.getStyle()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Copy/Paste from client log (click to open)")));
                    msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                    player.sendMessage(msg);
                    return 1;
                }
            } else {
                player.sendMessage(new StringTextComponent("This command only works on an integrated server, not on an dedicated one"));
            }
        }
        return 0;
    }

    private static int executeExportHeldItem(ServerPlayerEntity player) {
        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!heldItem.isEmpty()) {

                String command = "/give @p " + heldItem.getItem().getRegistryName().toString() + " 1 " + heldItem.getDamage();
                if (heldItem.hasTag()) {
                    //noinspection ConstantConditions
                    command += " " + heldItem.getTag().toString();
                }
                StringTextComponent msg = new StringTextComponent(command);
                msg.getStyle()
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Copy/Paste from client log (click to open)")));
                msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                player.sendMessage(msg);
                return 1;
            }
        }
        return 0;
    }

    private static void giveCapsule(ItemStack capsule, PlayerEntity player) {
        ItemEntity entity = new ItemEntity(player.getEntityWorld(), player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(), capsule);
        entity.setNoPickupDelay();
        entity.onCollideWithPlayer(player);
    }
}
