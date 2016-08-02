/**
 * 
 */
package capsule.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.loot.CapsuleLootTableHook;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.loot.LootContext;

/**
 * @author Lythom
 *
 */
public class CapsuleCommand extends CommandBase {

	public static String[] COMMAND_LIST = new String[] {
			"convertToReward", "exportHeldItem", "fromHeldCapsule", "fromStructure", "giveRandomLoot", "reloadLootList", "setAuthor", "setBaseColor",
			"setMaterialColor"
	};
	
	public static String[] COLOR_EXEMPLES = new String[] {
			"0xCCCCCC","0x549b57","0xe08822","0x5e8eb7","0x6c6c6c","0xbd5757","0x99c33d","0x4a4cba","0x7b2e89","0x95d5e7","0xffffff"
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.command.ICommand#getName()
	 */
	@Override
	public String getCommandName() {
		return "capsule";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.command.ICommand#getCommandUsage(net.minecraft.command.
	 * ICommandSender)
	 */
	@Override
	public String getCommandUsage(ICommandSender sender) {

		TextComponentString msg = new TextComponentString(
				"see Capsule commands usages at " + TextFormatting.UNDERLINE + "https://bitbucket.org/Lythom/mccapsule/wiki/Commands");
		msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, "https://bitbucket.org/Lythom/mccapsule/wiki/Commands"));
		sender.addChatMessage(msg);
		return "/capsule <" + String.join("|", COMMAND_LIST) + ">";
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		switch (args.length) {
		case 1:
			return getListOfStringsMatchingLastWord(args, COMMAND_LIST);
		case 2:
			switch (args[0]) {
				case "giveRandomLoot":
					return getListOfStringsMatchingLastWord(args, server.getAllUsernames());
				
				case "setBaseColor":
					return getListOfStringsMatchingLastWord(args, COLOR_EXEMPLES);
				
				case "setMaterialColor":
					return getListOfStringsMatchingLastWord(args, COLOR_EXEMPLES);
			}
		}
		return Collections.<String> emptyList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.command.ICommand#execute(net.minecraft.command.
	 * ICommandSender, java.lang.String[])
	 */
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

		if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}

		// export give command exportHeldItem
		else if ("exportHeldItem".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null) {

					String command = "/give @p " + heldItem.getItem().getRegistryName().toString() + " 1 " + heldItem.getItemDamage();
					if (heldItem.hasTagCompound()) {
						command += " " + heldItem.getTagCompound().toString();
					}
					TextComponentString msg = new TextComponentString(command);
					msg.getStyle()
							.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Copy/Paste from client log (click to open)")));
					msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

					player.addChatMessage(msg);

				}
			}
		}

		// set author
		else if ("setAuthor".equalsIgnoreCase(args[0])) {
			if (args.length != 1 && args.length != 2) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}

			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null && heldItem.getItem() instanceof CapsuleItem) {

					if (args.length == 2) {
						// set a new author
						String author = args[1];
						heldItem.getTagCompound().setString("author", args[1]);
						Pair<TemplateManager, Template> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
						Template template = templatepair.getRight();
						TemplateManager templatemanager = templatepair.getLeft();
						if (template != null && templatemanager != null) {
							template.setAuthor(author);
							templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
						}

					} else {
						// called with one parameter = remove author information
						heldItem.getTagCompound().removeTag("author");
						Pair<TemplateManager, Template> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
						Template template = templatepair.getRight();
						TemplateManager templatemanager = templatepair.getLeft();
						if (template != null && templatemanager != null) {
							template.setAuthor("?");
							templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
						}
					}

				}
			}

		}

		// set color
		else if ("setBaseColor".equalsIgnoreCase(args[0])) {
			if (args.length != 2) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}

			int color = 0;
			try {
				color = Integer.decode(args[1]);
			} catch (NumberFormatException e) {
				throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
			}

			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null && heldItem.getItem() instanceof CapsuleItem) {
					CapsuleItem.setBaseColor(heldItem, color);
				}
			}
		}

		else if ("setMaterialColor".equalsIgnoreCase(args[0])) {
			if (args.length != 2) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}

			int color = 0;
			try {
				color = Integer.decode(args[1]);
			} catch (NumberFormatException e) {
				throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
			}

			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null && heldItem.getItem() instanceof CapsuleItem) {
					CapsuleItem.setMaterialColor(heldItem, color);
				}
			}
		}
		// set the held item as reward (or not)
		else if ("fromHeldCapsule".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTagCompound()) {

					// TODO : create a new template file under the Config.commandTemplatesPathProp from the template linked to the capsule
					// TODO : create a new item from the held capsule but linked to the newly created structureBlock file
					// TODO : use the new CapsuleItem.create method instead

				}
			}
		}

		else if ("fromStructure".equalsIgnoreCase(args[0])) {

			if (args.length != 2) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}

			String structureName = args[1];

			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null && structureName != null && player.worldObj instanceof WorldServer) {
				// template
				Template template = StructureSaver.getTemplateForReward(player.worldObj.getMinecraftServer(), structureName).getRight();
				if (template != null) {
					int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
					if (size % 2 == 1)
						size++;

					// TODO : create a new template file under the Config.commandTemplatesPathProp from the /structures template folder
					// TODO : create a new item from the held capsule but linked to the newly created structureBlock file
					// TODO : use the new CapsuleItem.create method instead

					//giveCapsule(structureCapsule, player);
				} else {
					throw new CommandException("Structure \"%s\" not found ", structureName);
				}
			}

		}

		// give a random loot capsule
		else if ("giveRandomLoot".equalsIgnoreCase(args[0])) {
			if (args.length != 1 && args.length != 2) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			if (args.length == 2) {
				player = CommandBase.getPlayer(server, sender, args[1]);
			}
			if (player != null) {
				LootContext.Builder lootcontext$builder = new LootContext.Builder(player.getServerWorld());
				List<ItemStack> loots = new ArrayList<>();
				CapsuleLootTableHook.capsulePool.generateLoot(loots, new Random(), lootcontext$builder.build());
				if (loots.size() <= 0) {
					player.addChatMessage(new TextComponentString("No loot this time !"));
				} else {
					for (ItemStack loot : loots) {
						giveCapsule(loot, player);
					}
				}
			}
		}

		else if ("reloadLootList".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			StructureSaver.loadLootList(server);
		}

		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	private void giveCapsule(ItemStack capsule, EntityPlayer player) {
		EntityItem entity = new EntityItem(player.worldObj, player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(), capsule);
		entity.setNoPickupDelay();
		entity.onCollideWithPlayer(player);
	}

}
