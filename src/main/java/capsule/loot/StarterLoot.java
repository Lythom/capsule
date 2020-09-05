package capsule.loot;

import capsule.Config;
import capsule.helpers.Capsule;
import net.minecraft.entity.player.PlayerEntityMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class StarterLoot {

    protected static final Logger LOGGER = LogManager.getLogger(StarterLoot.class);

    public static StarterLoot instance = new StarterLoot();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.player.world.isRemote) {
            if (StringUtils.isNullOrEmpty(Config.starterMode) || Config.starterTemplatesList == null || Config.starterTemplatesList.size() <= 0) {
                LOGGER.info("Capsule starters are disabled in capsule.cfg. To enable, set starterMode to 'all' or 'random' and set a directory path with structures for starterTemplatesPath.");
                return;
            }
            PlayerEntityMP player = (PlayerEntityMP) event.player;
            CompoundNBT playerData = player.getEntityData();
            CompoundNBT data;
            if (!playerData.contains("PlayerPersisted")) {
                data = new CompoundNBT();
            } else {
                data = playerData.getCompound("PlayerPersisted");
            }
            LOGGER.info("playerLogin: " + (data.getBoolean("capsule:receivedStarter") ? "already received starters" : "giving starters now"));
            if (!data.getBoolean("capsule:receivedStarter")) {
                if ("all".equals(Config.starterMode.toLowerCase())) {
                    giveAllStarters(player, Config.starterTemplatesList);
                    data.putBoolean("capsule:receivedStarter", true);

                } else if ("random".equals(Config.starterMode)) {
                    giveAllStarters(player, Collections.singletonList(
                            Config.starterTemplatesList.get(
                                    (int) (Math.random() * Config.starterTemplatesList.size())
                            )
                    ));
                    data.putBoolean("capsule:receivedStarter", true);
                }
                playerData.setTag("PlayerPersisted", data);
            }

        }
    }

    public void giveAllStarters(PlayerEntityMP player, List<String> allStartersToGive) {
        for (String templatePath : allStartersToGive) {
            ItemStack starterCapsule = Capsule.createLinkedCapsuleFromReward(templatePath, player);
            int stackIdx = player.inventory.getFirstEmptyStack();
            if (stackIdx < 0 || stackIdx >= player.inventory.getSizeInventory()) {
                Capsule.throwCapsule(starterCapsule, player, player.getPosition());
            } else {
                try {
                    player.inventory.setInventorySlotContents(stackIdx, starterCapsule);
                } catch (Exception e) {
                    Capsule.throwCapsule(starterCapsule, player, player.getPosition());
                }
            }
        }
    }
}
