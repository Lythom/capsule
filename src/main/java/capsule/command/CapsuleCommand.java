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
import com.google.common.base.Joiner;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerEntityMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.ServerWorld;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.storage.loot.LootContext;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Lythom
 */
public class CapsuleCommand extends CommandBase {

    public static List<ICommandSender> sentUsageURL = new ArrayList<>();

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

    public static final String[] COMMAND_HELP = new String[]{
            "giveEmpty [size] [overpowered]",
            "giveLinked <rewardName> [playerName]",
            "giveBlueprint <rewardName> [playerName]",
            "exportHeldItem",
            "exportSeenBlock",
            "fromExistingReward <rewardName> [playerName]",
            "fromHeldCapsule [outputName]",
            "fromStructure <structureName> [playerName]",
            "giveRandomLoot [playerName]",
            "reloadLootList",
            "reloadWhitelist",
            "setAuthor <authorName>",
            "setBaseColor <color>",
            "setMaterialColor <color>"
    };

    @Override
    public String getName() {
        return "capsule";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {

        if (!sentUsageURL.contains(sender)) {
            StringTextComponent msg = new StringTextComponent(
                    "see Capsule commands usages at " + TextFormatting.UNDERLINE + "https://github.com/Lythom/capsule/wiki/Commands");
            msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, "https://github.com/Lythom/capsule/wiki/Commands"));
            sender.sendMessage(msg);
            sentUsageURL.add(sender);
        }
        return "Capsule commands list:\n/capsule " + Joiner.on("\n/capsule ").join(COMMAND_HELP);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {

        PlayerEntityMP player = null;
        switch (args.length) {
            case 1:
                return getListOfStringsMatchingLastWord(args, COMMAND_LIST);
            case 2:
                switch (args[0]) {
                    case "giveRandomLoot":
                        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());

                    case "setBaseColor":
                    case "setMaterialColor":
                        return getListOfStringsMatchingLastWord(args, CapsuleLootEntry.COLOR_PALETTE);

                    case "fromStructure":
                        try {
                            player = getCommandSenderAsPlayer(sender);
                            String[] structuresList = (new File(player.getServerWorld().getSaveHandler().getWorldDirectory(), "structures")).list();
                            if (structuresList == null) return new ArrayList<>();
                            return getListOfStringsMatchingLastWord(args, structuresList);
                        } catch (PlayerNotFoundException ignored) {
                        }
                        break;

                    case "fromExistingReward":
                    case "giveLinked":
                    case "giveBlueprint":
                        String[] rewardsList = (new File(Config.rewardTemplatesPath)).list();
                        if (rewardsList == null) return new ArrayList<>();
                        return getListOfStringsMatchingLastWord(args, rewardsList);
                }
            case 3:
                switch (args[0]) {
                    case "fromStructure":
                    case "fromExistingReward":
                    case "giveLinked":
                    case "giveBlueprint":
                        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                }
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }

        PlayerEntityMP player = null;
        if (sender instanceof PlayerEntityMP) {
            player = (PlayerEntityMP) sender;
        }

        if ("giveEmpty".equalsIgnoreCase(args[0])) {
            executeGiveEmpty(args, player);
        } else if ("giveLinked".equalsIgnoreCase(args[0])) {
            executeGiveLinked(server, sender, args, player);
        } else if ("giveBlueprint".equalsIgnoreCase(args[0])) {
            executeGiveBlueprint(server, sender, args, player);
        } else if ("exportHeldItem".equalsIgnoreCase(args[0])) {
            executeExportHeldItem(sender, args, player);
        } else if ("exportSeenBlock".equalsIgnoreCase(args[0])) {
            executeExportSeenBlock(server, sender, args, player);
        } else if ("setAuthor".equalsIgnoreCase(args[0])) {
            executeSetAuthor(server, sender, args, player);
        } else if ("setBaseColor".equalsIgnoreCase(args[0])) {
            executeSetBaseColor(sender, args, player);
        } else if ("setMaterialColor".equalsIgnoreCase(args[0])) {
            executeSetMaterialColor(sender, args, player);
        } else if ("fromHeldCapsule".equalsIgnoreCase(args[0])) {
            executeFromHeldCapsule(server, sender, args, player);
        } else if ("fromStructure".equalsIgnoreCase(args[0])) {
            executeFromStructure(server, sender, args, player);
        } else if ("fromExistingReward".equalsIgnoreCase(args[0])) {
            executeFromExistingReward(server, sender, args, player);
        } else if ("giveRandomLoot".equalsIgnoreCase(args[0])) {
            executeGiveRandomLoot(server, sender, args, player);
        } else if ("reloadLootList".equalsIgnoreCase(args[0])) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            Files.populateAndLoadLootList(Config.configDir, Config.lootTemplatesPaths, Config.lootTemplatesData);
        } else if ("reloadWhitelist".equalsIgnoreCase(args[0])) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            Files.populateAndLoadLootList(Config.configDir, Config.lootTemplatesPaths, Config.lootTemplatesData);
            Config.starterTemplatesList = Files.populateStarters(Config.configDir, Config.starterTemplatesPath);
            Config.blueprintWhitelist = Files.populateWhitelistConfig(Config.configDir);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void executeGiveEmpty(String[] args, PlayerEntityMP player) {
        if (player != null) {
            ItemStack capsule = Capsule.newEmptyCapsuleItemStack(
                    0xFFFFFF,
                    0xFFFFFF,
                    (args.length >= 2 ? Integer.decode(args[1]) : 3),
                    (args.length >= 3 && Boolean.valueOf(args[2])),
                    null,
                    null
            );
            giveCapsule(capsule, player);
        }
    }

    private void executeGiveLinked(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP p) throws CommandException {
        StructureAndPlayerArgs structureAndPlayerArgs = new StructureAndPlayerArgs().invoke(server, sender, args, p);
        PlayerEntityMP player = structureAndPlayerArgs.getTargetedPlayer();
        String srcStructureName = structureAndPlayerArgs.getStructureName();

        if (player != null && !StringUtils.isNullOrEmpty(srcStructureName) && player.getEntityWorld() instanceof ServerWorld) {
            ItemStack capsule = Capsule.createLinkedCapsuleFromReward(Config.getRewardPathFromName(srcStructureName), player);
            if (!capsule.isEmpty()) {
                giveCapsule(capsule, player);
            } else {
                throw new CommandException("Reward Capsule \"%s\" not found ", srcStructureName);
            }
        }
    }

    private void executeGiveBlueprint(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP p) throws CommandException {
        StructureAndPlayerArgs structureAndPlayerArgs = new StructureAndPlayerArgs().invoke(server, sender, args, p);
        PlayerEntityMP player = structureAndPlayerArgs.getTargetedPlayer();
        String srcStructureName = structureAndPlayerArgs.getStructureName();

        if (player != null && !StringUtils.isNullOrEmpty(srcStructureName) && player.getEntityWorld() instanceof ServerWorld) {

            CapsuleTemplate srcTemplate = Capsule.getRewardTemplateIfExists(Config.getRewardPathFromName(srcStructureName), server);
            if (srcTemplate != null) {
                int size = Math.max(srcTemplate.getSize().getX(), Math.max(srcTemplate.getSize().getY(), srcTemplate.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                ItemStack capsule = Capsule.newEmptyCapsuleItemStack(
                        3949738,
                        0xFFFFFF,
                        size,
                        false,
                        Capsule.labelFromPath(srcStructureName),
                        0
                );
                CapsuleItem.setState(capsule, CapsuleItem.STATE_DEPLOYED);
                CapsuleItem.setBlueprint(capsule);

                String destTemplate = StructureSaver.createBlueprintTemplate(
                        Config.getRewardPathFromName(srcStructureName), capsule,
                        player.getServerWorld(),
                        player
                );
                CapsuleItem.setStructureName(capsule, destTemplate);
                giveCapsule(capsule, player);

            } else {
                throw new CommandException("Reward Capsule \"%s\" not found ", srcStructureName);
            }
        }
    }

    private void executeGiveRandomLoot(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP player) throws CommandException {
        if (args.length != 1 && args.length != 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        if (args.length == 2) {
            player = CommandBase.getPlayer(server, sender, args[1]);
        }
        if (player != null) {
            LootContext.Builder lootcontext$builder = new LootContext.Builder(player.getServerWorld());
            List<ItemStack> loots = new ArrayList<>();
            CapsuleLootTableHook.capsulePool.generateLoot(loots, new Random(), lootcontext$builder.build());
            if (loots.size() <= 0) {
                player.sendMessage(new StringTextComponent("No loot this time !"));
            } else {
                for (ItemStack loot : loots) {
                    giveCapsule(loot, player);
                }
            }
        }
    }

    private void executeFromExistingReward(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP p) throws CommandException {
        StructureAndPlayerArgs structureAndPlayerArgs = new StructureAndPlayerArgs().invoke(server, sender, args, p);
        PlayerEntityMP player = structureAndPlayerArgs.getTargetedPlayer();
        String structureName = structureAndPlayerArgs.getStructureName();

        if (player != null && !StringUtils.isNullOrEmpty(structureName) && player.getEntityWorld() instanceof ServerWorld) {

            String structurePath = Config.getRewardPathFromName(structureName);
            CapsuleTemplateManager templatemanager = StructureSaver.getRewardManager(server);
            CapsuleTemplate template = templatemanager.get(server, new ResourceLocation(structurePath));
            if (template != null) {
                int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        structurePath,
                        CapsuleLootEntry.getRandomColor(),
                        CapsuleLootEntry.getRandomColor(),
                        size,
                        Capsule.labelFromPath(structureName),
                        template.getAuthor());
                CapsuleItem.setCanRotate(capsule, template.canRotate());
                giveCapsule(capsule, player);

            } else {
                throw new CommandException("Reward Capsule \"%s\" not found ", structureName);
            }
        }
    }

    private void executeFromStructure(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP p) throws CommandException {
        StructureAndPlayerArgs structureAndPlayerArgs = new StructureAndPlayerArgs().invoke(server, sender, args, p);
        PlayerEntityMP player = structureAndPlayerArgs.getTargetedPlayer();
        String srcStructureName = structureAndPlayerArgs.getStructureName();

        if (player != null && !StringUtils.isNullOrEmpty(srcStructureName) && player.getEntityWorld() instanceof ServerWorld) {
            // template
            TemplateManager templatemanager = player.getServerWorld().getStructureTemplateManager();
            Template template = templatemanager.get(server, new ResourceLocation(srcStructureName));
            if (template != null) {
                int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                if (size % 2 == 0)
                    size++;

                // get source template data
                CompoundNBT data = new CompoundNBT();
                template.writeToNBT(data);

                // create a destination template
                ResourceLocation destinationLocation = new ResourceLocation(Config.rewardTemplatesPath + "/" + srcStructureName);
                CapsuleTemplateManager destManager = StructureSaver.getRewardManager(server);
                CapsuleTemplate destTemplate = destManager.getTemplate(server, destinationLocation);
                // write template from source data
                destTemplate.read(data);
                destManager.writeTemplate(server, destinationLocation);

                ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                        destinationLocation.toString(),
                        CapsuleLootEntry.getRandomColor(),
                        CapsuleLootEntry.getRandomColor(),
                        size,
                        srcStructureName,
                        template.getAuthor());
                CapsuleItem.setCanRotate(capsule, destTemplate.canRotate());
                giveCapsule(capsule, player);

            } else {
                throw new CommandException("Structure \"%s\" not found ", srcStructureName);
            }
        }
    }

    private void executeFromHeldCapsule(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 1 && args.length != 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                String outputName;
                if (args.length == 1) {
                    //noinspection ConstantConditions
                    outputName = heldItem.getTag().getString("label");
                } else {
                    outputName = args[1];
                }
                if (StringUtils.isNullOrEmpty(outputName)) {
                    throw new WrongUsageException(
                            "/capsule fromHeldCapsule [outputName]. Please label the held capsule or provide an output name to be used for output template.");
                }

                String destinationTemplateLocation = Config.getRewardPathFromName(outputName.toLowerCase().replace(" ", "_").replace(":", "-"));
                boolean created = StructureSaver.copyFromCapsuleTemplate(
                        heldItem,
                        destinationTemplateLocation,
                        StructureSaver.getRewardManager(server),
                        player.getServerWorld(),
                        false,
                        null
                );

                if (!created) {
                    player.sendMessage(new StringTextComponent("Could not duplicate the capsule template. Either the source template don't exist or the destination folder dont exist."));
                    return;
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

            }
        }
    }

    private void executeSetMaterialColor(ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        int color = 0;
        try {
            color = Integer.decode(args[1]);
        } catch (NumberFormatException e) {
            throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setMaterialColor(heldItem, color);
            }
        }
    }

    private void executeSetBaseColor(ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        int color = 0;
        try {
            color = Integer.decode(args[1]);
        } catch (NumberFormatException e) {
            throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof CapsuleItem) {
                CapsuleItem.setBaseColor(heldItem, color);
            }
        }
    }

    private void executeSetAuthor(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 1 && args.length != 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTag()) {

                if (args.length == 2) {
                    // set a new author
                    String author = args[1];
                    //noinspection ConstantConditions
                    heldItem.getTag().putString("author", args[1]);
                    Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
                    CapsuleTemplate template = templatepair.getRight();
                    CapsuleTemplateManager templatemanager = templatepair.getLeft();
                    if (template != null && templatemanager != null) {
                        template.setAuthor(author);
                        templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
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
                        templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
                    }
                }

            }
        }
    }

    private void executeExportSeenBlock(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (player != null) {
            if (!server.isDedicatedServer()) {
                BlockRayTraceResult rtc = Spacial.clientRayTracePreview(player, Minecraft.getMinecraft().getRenderPartialTicks(), 50);

                if (rtc != null && rtc.typeOfHit == RayTraceResult.Type.BLOCK) {

                    BlockPos position = rtc.getBlockPos();
                    BlockState state = player.getServerWorld().getBlockState(position);
                    TileEntity tileentity = player.getServerWorld().getTileEntity(position);

                    String command = "/give @p " + state.getBlock().getRegistryName().toString() + " 1 " + state.getBlock().getMetaFromState(state);
                    if (tileentity != null) {
                        command += " {BlockEntityTag:" + tileentity.serializeNBT().toString() + "}";
                    }
                    StringTextComponent msg = new StringTextComponent(command);
                    msg.getStyle()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Copy/Paste from client log (click to open)")));
                    msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                    player.sendMessage(msg);

                }
            } else {
                player.sendMessage(new StringTextComponent("This command only works on an integrated server, not on an dedicated one"));
            }
        }
    }

    private void executeExportHeldItem(ICommandSender sender, String[] args, PlayerEntityMP player) throws WrongUsageException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
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

            }
        }
    }

    private void giveCapsule(ItemStack capsule, PlayerEntity player) {
        ItemEntity entity = new ItemEntity(player.getEntityWorld(), player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(), capsule);
        entity.setNoPickupDelay();
        entity.onCollideWithPlayer(player);
    }

    /**
     * Extract a target player from command args or command executor.
     * Target player must be the last argument.
     */
    private class StructureAndPlayerArgs {
        private PlayerEntityMP targetedPlayer;
        private String structureName;

        public PlayerEntityMP getTargetedPlayer() {
            return targetedPlayer;
        }

        public String getStructureName() {
            return structureName;
        }

        public StructureAndPlayerArgs invoke(MinecraftServer server, ICommandSender sender, String[] args, PlayerEntityMP senderPlayer) throws CommandException {
            if (args.length == 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            targetedPlayer = senderPlayer;
            int finalArgsCount = 0;
            if (args.length > 2) {
                PlayerEntityMP p = null;
                try {
                    p = getPlayer(server, sender, args[args.length - 1]);
                } catch (Exception ignored) {
                }
                if (p != null) {
                    targetedPlayer = p;
                    finalArgsCount = 1;
                }
            }
            StringBuilder structureNameB = new StringBuilder();
            for (int i = 1; i < args.length - finalArgsCount; i++) {
                structureNameB.append(args[i]);
                if (i < args.length - finalArgsCount - 1) structureNameB.append(" ");
            }

            structureName = structureNameB.toString().replaceAll(".nbt", "").replaceAll(".schematic", "");
            return this;
        }
    }
}
