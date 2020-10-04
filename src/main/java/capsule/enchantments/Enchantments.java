package capsule.enchantments;

import capsule.Config;
import capsule.Main;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

public class Enchantments {

    protected static final Logger LOGGER = LogManager.getLogger(Enchantments.class);

    public static Enchantment recallEnchant = null;
    @SuppressWarnings("rawtypes")
    public static final Predicate hasRecallEnchant = new Predicate() {
        public boolean apply(Entity entityIn) {
            return entityIn instanceof ItemEntity
                    && EnchantmentHelper.getEnchantmentLevel(Enchantments.recallEnchant, ((ItemEntity) entityIn).getItem()) > 0;
        }

        public boolean test(Object obj) {
            return this.apply((Entity) obj);
        }
    };

    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {

        Rarity enchantRarity = Rarity.RARE;
        try {
            enchantRarity = Rarity.valueOf(Config.enchantRarity.get());
        } catch (Exception e) {
            LOGGER.warn("Couldn't find the rarity " + Config.enchantRarity + ". Using RARE instead.");
        }

        EnchantmentType recallEnchantTypeEnumValue = null;
        try {
            recallEnchantTypeEnumValue = EnchantmentType.valueOf(Config.recallEnchantType.get());
        } catch (IllegalArgumentException ignored) {
        }

        Enchantments.recallEnchant = new RecallEnchant(
                new ResourceLocation(Main.MODID, "recall"), // name
                enchantRarity, // weight (chances to appear)
                recallEnchantTypeEnumValue // possible targets
        );

        event.getRegistry().register(Enchantments.recallEnchant);
    }
}
