package capsule.gametest.mixin;

import capsule.gametest.FabricGameTests;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the capsule test instances in the datapack registries before they are frozen, at the same two points as
 * Fabric API's own RegistryDataLoaderMixin (fabric-gametest-api-v1), which only knows tests declared by annotations:
 * the load from resources that includes the test instance registry, then the lambda that freezes its result.
 */
@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @Unique
    private static final AtomicBoolean CAPSULE_LOADING_TESTS = new AtomicBoolean(false);

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private static void capsule$loadFromResources(ResourceManager resourceManager, List<HolderLookup.RegistryLookup<?>> registries, List<RegistryDataLoader.RegistryData<?>> entries, Executor executor, CallbackInfoReturnable<CompletableFuture<RegistryAccess.Frozen>> cir) {
        CAPSULE_LOADING_TESTS.set(entries.stream().anyMatch(entry -> entry.key() == Registries.TEST_INSTANCE));
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "lambda$load$2(Ljava/util/List;Ljava/util/Map;Ljava/lang/Void;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At("HEAD"))
    private static void capsule$beforeFreeze(List<RegistryLoadTask<?>> loadTasks, Map<ResourceKey<?>, Exception> loadingErrors, Void ignored, CallbackInfoReturnable<RegistryAccess.Frozen> cir) {
        if (!CAPSULE_LOADING_TESTS.getAndSet(false)) return;
        Registry<TestEnvironmentDefinition<?>> environments = null;
        Registry<GameTestInstance> instances = null;
        for (RegistryLoadTask<?> task : loadTasks) {
            Registry<?> registry = ((RegistryLoadTaskAccessor) task).capsule$registry();
            if (registry.key() == Registries.TEST_ENVIRONMENT) environments = (Registry<TestEnvironmentDefinition<?>>) registry;
            if (registry.key() == Registries.TEST_INSTANCE) instances = (Registry<GameTestInstance>) registry;
        }
        if (environments != null && instances != null) FabricGameTests.registerInstances(environments, instances);
    }
}
