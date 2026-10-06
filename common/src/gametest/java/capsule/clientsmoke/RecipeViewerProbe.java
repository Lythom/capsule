package capsule.clientsmoke;

import capsule.CapsuleMod;
import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a recipe viewer shows, asked by the client smoke test. Implementations only load when their viewer is installed.
 */
interface RecipeViewerProbe {
    String name();

    /**
     * True once the viewer has loaded the plugins, which some viewers do in the background.
     */
    boolean ready();

    long craftingRecipes(ItemStack output);

    long capsuleInformationPages();

    void showRecipes(ItemStack output);

    static List<RecipeViewerProbe> installed() {
        List<RecipeViewerProbe> probes = new ArrayList<>();
        if (Services.PLATFORM.isModLoaded("jei")) probes.add(new JeiSmokePlugin());
        if (Services.PLATFORM.isModLoaded("roughlyenoughitems")) probes.add(new ReiProbe());
        if (Services.PLATFORM.isModLoaded("emi")) probes.add(new EmiProbe());
        return probes;
    }

    static boolean isCapsule(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(CapsuleMod.MODID);
    }
}
