package capsule.gametest;

import capsule.CapsuleMod;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Collection;
import java.util.stream.Stream;

@EventBusSubscriber(modid = CapsuleMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeGameTests {

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(NeoForgeGameTests.class);
    }

    /**
     * The common tests, plus the SecurityCraft ones: SecurityCraft only exists on NeoForge.
     */
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        return CapsuleGameTests.testFunctions(Stream.concat(CapsuleGameTests.TEST_CLASSES.stream(), Stream.of(SecurityCraftTests.class)));
    }
}
