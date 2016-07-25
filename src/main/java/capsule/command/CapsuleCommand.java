/**
 * 
 */
package capsule.command;

import java.util.Collections;
import java.util.List;

import capsule.items.CapsuleItem;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.command.ICommand#getCommandUsage(net.minecraft.command.
	 * ICommandSender)
	 */
	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/capsule convertToReward";
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
		if (args.length == 1) {
			// set the held item as reward (or not)
			if ("convertToReward".equalsIgnoreCase(args[0])) {
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
		}

	}

}
