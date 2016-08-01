/**
 * 
 */
package capsule.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import capsule.loot.CapsuleLootTableHook;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.storage.loot.LootContext;

/**
 * @author Lythom
 *
 */
public class CapsuleCommand extends CommandBase {

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
		return 	"/capsule exportHeldItem                // Print the command that can be used to give this specific item.\n"+
				"/capsule fromHeldCapsule               // Create a new Reward Capsule by copying the template from held capsule into config/capsules/rewards.\n"+
				"/capsule fromStructure <structureName> // Create a new Reward Capsule by copying the template from named structure block into config/capsules/rewards.\n"+
				"/capsule giveRandomLoot [player]       // Give the player a random Loot Capsule using the loot generation configuration.\n"+
				"/capsule reloadLootList                // Refresh the lootable capsules from reading configured directories.";
	}
	
	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		switch(args.length){
			case 1:
				return getListOfStringsMatchingLastWord(args, new String[]{"convertToReward", "fromHeldCapsule", "fromStructure", "giveRandomLoot","reloadLootList"});
			case 2:
				switch(args[0]){
				case "giveRandomLoot":
					return getListOfStringsMatchingLastWord(args, server.getAllUsernames());
				}
				break;
		}
		return Collections.<String>emptyList();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.command.ICommand#execute(net.minecraft.command.
	 * ICommandSender, java.lang.String[])
	 */
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

		if (args.length < 1 || "help".equalsIgnoreCase(args[0]))
        {
            throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
        }
		
		// export give command exportHeldItem
		// TODO : create a command to print into the chatbox (+ clipboard ?) the give command to use
		if ("exportHeldItem".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null) {
					
					String command = "/give @p "+heldItem.getItem().getRegistryName().toString()+" 1 "+heldItem.getItemDamage();
					if(heldItem.hasTagCompound()){
						command += " " + heldItem.getTagCompound().toString();
					}
					TextComponentString msg = new TextComponentString(command);
					msg.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Copy/Paste from client log (click to open)")));
					msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

					player.addChatMessage(msg);
					
				}
			}
		}
		
		// set author
		// TODO : create a command to specify the author of the structure /capsule setAuthor <structureName> <authorName>
		
		// set rarity
		// TODO : create a command to specify the rarity of the structure to appear in a dungeon chest /capsule setRarity <structureName> <authorName>
		
		// set color
		// TODO : create a command to specify the base color
		
		// set material color
		// TODO : create a command to specify the material color
		
		// set the held item as reward (or not)
		if ("fromHeldCapsule".equalsIgnoreCase(args[0])) {
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
					
					heldItem.getTagCompound().setBoolean("isReward", true);
					heldItem.getTagCompound().setBoolean("oneUse", true);
					heldItem.setItemDamage(CapsuleItem.STATE_ONE_USE);
				}
			}
		}
		
		
		else if ("fromStructure".equalsIgnoreCase(args[0])) {
			
			if (args.length != 2 ) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			
			String structureName = args[1];
			
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null && structureName != null && player.worldObj instanceof WorldServer) {
				// template
				Template template = StructureSaver.getTemplateForReward(player.worldObj.getMinecraftServer(), structureName);
				if(template != null){
					int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
					if(size%2 == 1) size++;
					
					// TODO : create a new template file under the Config.commandTemplatesPathProp from the /structures template folder
					// TODO : create a new item from the held capsule but linked to the newly created structureBlock file
					// TODO : use the new CapsuleItem.create method instead
					ItemStack structureCapsule = new ItemStack(CapsuleItemsRegistrer.capsule, 1, CapsuleItem.STATE_LINKED);
					structureCapsule.setTagInfo("color", new NBTTagInt(0xCCCCCC));
					structureCapsule.setTagInfo("size", new NBTTagInt(size));
					structureCapsule.getTagCompound().setBoolean("oneUse", true);
					structureCapsule.getTagCompound().setBoolean("isReward", true);
					
					giveCapsule(structureCapsule, player);
				} else {
					throw new CommandException("Structure \"%s\" not found ", structureName);
				}					
			}

		}
		
		// give a random loot capsule
		else if ("giveRandomLoot".equalsIgnoreCase(args[0])) {
			if (args.length != 1 || args.length != 2) {
				EntityPlayerMP player = getCommandSenderAsPlayer(sender);
				if(args.length == 2){
					player = CommandBase.getPlayer(server, sender, args[1]);
				}
				if (player != null) {
					LootContext.Builder lootcontext$builder = new LootContext.Builder(player.getServerWorld());
					List<ItemStack> loots = new ArrayList<>();
					CapsuleLootTableHook.capsulePool.generateLoot(loots, new Random(), lootcontext$builder.build());
					if(loots.size() <= 0){
						player.addChatMessage(new TextComponentString("No loot this time !"));
					} else {
						for(ItemStack loot : loots){
							giveCapsule(loot, player);
						}
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
	}
	
	private void giveCapsule(ItemStack capsule, EntityPlayer player){
		EntityItem entity = new EntityItem(player.worldObj, player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(),  capsule);
		entity.setNoPickupDelay();
		entity.onCollideWithPlayer(player);
	}


}
