/**
 * 
 */
package capsule.command;

import capsule.items.CapsuleItem;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

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
	public String getName() {
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
		return "/capsule isReward true|false";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.command.ICommand#execute(net.minecraft.command.
	 * ICommandSender, java.lang.String[])
	 */
	@Override
	public void execute(ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 2) {
			// set the held item as reward (or not)
			if ("isReward".equalsIgnoreCase(args[0])) {
				boolean isReward = Boolean.parseBoolean(args[1]);
				EntityPlayer player = getCommandSenderAsPlayer(sender);
				if (player != null) {
					ItemStack heldItem = player.getHeldItem();
					if (heldItem != null && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTagCompound()) {
						heldItem.getTagCompound().setBoolean("isReward", isReward);
					}
				}

			}
		}

	}

}
