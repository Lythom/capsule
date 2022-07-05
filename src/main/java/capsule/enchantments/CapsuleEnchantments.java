package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

public class CapsuleEnchantments {

    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, CapsuleMod.MODID);
    public static final RegistryObject<Enchantment> RECALL = ENCHANTMENTS.register("recall", CapsuleEnchantments::CreateRecall);

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleEnchantments.class);

    public static final Predicate<Entity> hasRecallEnchant = (Entity entityIn) -> entityIn instanceof ItemEntity && EnchantmentHelper.getItemEnchantmentLevel(CapsuleEnchantments.RECALL.get(), ((ItemEntity) entityIn).getItem()) > 0;

    public static void registerEnchantments(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }

    public static RecallEnchant CreateRecall() {
        Rarity enchantRarity = Rarity.RARE;
        try {
            enchantRarity = Rarity.valueOf(Config.enchantRarity.get());
        } catch (Exception e) {
            LOGGER.warn("Couldn't find the rarity " + Config.enchantRarity.get() + ". Using RARE instead.");
        }

        EnchantmentCategory recallEnchantTypeEnumValue = null;
        try {
            recallEnchantTypeEnumValue = EnchantmentCategory.valueOf(Config.recallEnchantType.get());
        } catch (IllegalArgumentException ignored) {
        }

        return new RecallEnchant(// name
                enchantRarity, // weight (chances to appear)
                recallEnchantTypeEnumValue // possible targets
        );
    }
}
