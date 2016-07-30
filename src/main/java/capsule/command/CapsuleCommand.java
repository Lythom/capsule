/**
 * 
 */
package capsule.command;

import java.util.Collections;
import java.util.List;

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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

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
		return "/capsule convertToReward|fromStructure";
	}
	
	public String getconvertToRewardUsage() {
		return "/capsule convertToReward";
	}
	
	public String getfromStructureUsage() {
		return "/capsule fromStructure <structureName> [color]";
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
		
		
		// set the held item as reward (or not)
		if ("convertToReward".equalsIgnoreCase(args[0])) {
			if (args.length != 1) {
				throw new WrongUsageException(getconvertToRewardUsage(), new Object[0]);
			}
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (player != null) {
				ItemStack heldItem = player.getHeldItemMainhand();
				if (heldItem != null && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTagCompound()) {
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
				TemplateManager tm = player.worldObj.getSaveHandler().getStructureTemplateManager();
				Template template = tm.func_189942_b(server, new ResourceLocation(structureName));
				if(template != null){
					int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
					if(size%2 == 1) size++;
					
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
	}

}
