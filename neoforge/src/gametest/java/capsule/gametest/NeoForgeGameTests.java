package capsule.gametest;

import capsule.CapsuleMod;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

@EventBusSubscriber(modid = CapsuleMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeGameTests {

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(NeoForgeGameTests.class);
    }

    /**
     * The common tests, plus the SecurityCraft ones (SecurityCraft is a GameTest dependency of NeoForge only), and the
     * tests of the optional mods that are loaded.
     */
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        Stream<Class<?>> optional = Map.of("waystones", WaystonesTests.class, "sophisticatedstorage", SophisticatedStorageTests.class).entrySet().stream()
                .filter(e -> ModList.get().isLoaded(e.getKey()))
                .map(Map.Entry::getValue);
        return CapsuleGameTests.testFunctions(Stream.of(CapsuleGameTests.TEST_CLASSES.stream(), Stream.of(SecurityCraftTests.class), optional).flatMap(s -> s));
    }
}
