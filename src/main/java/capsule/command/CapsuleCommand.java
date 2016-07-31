/**
 * 
 */
package capsule.command;

import java.util.Collections;
import java.util.List;

import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;

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
		return "/capsule fromHeldCapsule|fromStructure|reloadRewardsList";
	}
	
	public String getconvertToRewardUsage() {
		return "/capsule fromHeldCapsule";
	}
	
	public String getfromStructureUsage() {
		return "/capsule fromStructure <structureName> [color]";
	}

	private String getReloadRewardsUsage() {
		return "/capsule reloadRewardsList";
	}
	
	
	
	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args, "convertToReward") : Collections.<String>emptyList();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.command.ICommand#execute(net.minecraft.command.
	 * ICommandSender, java.lang.String[])
	 */
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

		if (args.length < 1)
        {
            throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
        }
		
		// export give command exportHeldItem
		// TODO : create a command to print into the chatbox (+ clipboard ?) the give command to use
		
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
				throw new WrongUsageException(getconvertToRewardUsage(), new Object[0]);
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
		
		
		if ("fromStructure".equalsIgnoreCase(args[0])) {
			
			if (args.length != 2 && args.length != 3) {
				throw new WrongUsageException(getfromStructureUsage(), new Object[0]);
			}
			
			String structureName = args[1];
			int color = args.length > 2 ? Integer.valueOf(args[2]) : 0xCCCCCC;
			
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
					structureCapsule.setTagInfo("color", new NBTTagInt(color));
					structureCapsule.setTagInfo("size", new NBTTagInt(size));
					structureCapsule.getTagCompound().setBoolean("oneUse", true);
					structureCapsule.getTagCompound().setBoolean("isReward", true);
					
					EntityItem entity = new EntityItem(player.worldObj, player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(), structureCapsule);
					entity.setNoPickupDelay();
					entity.onCollideWithPlayer(player);
				} else {
					throw new CommandException("Structure \"%s\" not found ", structureName);
				}					
			}

		}
		
		if ("reloadRewardsList".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getReloadRewardsUsage(), new Object[0]);
			}
			StructureSaver.loadLootList(server);
		}
	}


}
