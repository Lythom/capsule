package capsule.loot;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.helpers.Capsule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringUtil;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StarterLoot {

    protected static final Logger LOGGER = LogManager.getLogger(StarterLoot.class);

    public static StarterLoot instance = new StarterLoot();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            if (StringUtil.isNullOrEmpty(Config.starterMode) || Config.starterTemplatesList == null || Config.starterTemplatesList.size() <= 0) {
                LOGGER.info("Capsule starters are disabled in capsule.cfg. To enable, set starterMode to 'all' or 'random' and set a directory path with structures for starterTemplatesPath.");
                return;
            }
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            CompoundTag playerData = player.getPersistentData();
            CompoundTag data;
            if (!playerData.contains("PlayerPersisted")) {
                data = new CompoundTag();
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
                playerData.put("PlayerPersisted", data);
            }

        }
    }

    public static void giveAllStarters(ServerPlayer player, List<String> allStartersToGive) {
        for (String templatePath : allStartersToGive) {
            ItemStack starterCapsule = Capsule.createLinkedCapsuleFromReward(templatePath, player);
            int stackIdx = player.inventory.getFreeSlot();
            if (stackIdx < 0 || stackIdx >= player.inventory.getContainerSize()) {
                Capsule.throwCapsule(starterCapsule, player, player.blockPosition());
            } else {
                try {
                    player.inventory.setItem(stackIdx, starterCapsule);
                } catch (Exception e) {
                    Capsule.throwCapsule(starterCapsule, player, player.blockPosition());
                }
            }
        }
    }
}
