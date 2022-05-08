package capsule.enchantments;

import capsule.CapsuleMod;
import capsule.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.RegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

public class Enchantments {

    protected static final Logger LOGGER = LogManager.getLogger(Enchantments.class);

    public static Enchantment recallEnchant = null;
    public static final Predicate<Entity> hasRecallEnchant = (Entity entityIn) -> entityIn instanceof ItemEntity && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.recallEnchant, ((ItemEntity) entityIn).getItem()) > 0;

    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {

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

        Enchantments.recallEnchant = new RecallEnchant(
                new ResourceLocation(CapsuleMod.MODID, "recall"), // name
                enchantRarity, // weight (chances to appear)
                recallEnchantTypeEnumValue // possible targets
        );

        event.getRegistry().register(Enchantments.recallEnchant);
    }
}
