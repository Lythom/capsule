package capsule.loot;

import capsule.Config;
import capsule.helpers.Capsule;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class StarterLoot {

    public static StarterLoot instance = new StarterLoot();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.player.world.isRemote) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            NBTTagCompound playerData = player.getEntityData();
            NBTTagCompound data;
            if (!playerData.hasKey("PlayerPersisted")) {
                data = new NBTTagCompound();
            } else {
                data = playerData.getCompoundTag("PlayerPersisted");
            }

            if (!data.getBoolean("capsule:receivedStarter")) {
                for (String templatePath : Config.starterTemplatesList) {
                    ItemStack starterCapsule = Capsule.createLinkedCapsuleFromReward(templatePath, player);
                    int stackIdx = player.inventory.getFirstEmptyStack();
                    player.inventory.setInventorySlotContents(stackIdx, starterCapsule);
                }

                data.setBoolean("capsule:receivedStarter", true);
                playerData.setTag("PlayerPersisted", data);
            }

        }
    }
}
