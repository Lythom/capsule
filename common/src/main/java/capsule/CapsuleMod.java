package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.itemGroups.CapsuleCreativeTabs;
import capsule.items.CapsuleItems;
import capsule.loot.CapsuleLootEntry;
import capsule.plugins.claims.Claims;
import capsule.recipes.CapsuleRecipes;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.structure.CapsuleTemplateManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;

/**
 * Loader independent entry points, called by the loader modules.
 */
public class CapsuleMod {
    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleMod.class);

    public static final String MODID = "capsule";

    /**
     * Opens the label editor, set by the client.
     */
    public static Consumer<Player> openGuiScreenCommon = player -> {
    };
    public static MinecraftServer server = null;

    /**
     * Registers the blocks, items, creative tab, recipe serializers and loot entry type.
     */
    public static void init() {
        CapsuleBlocks.init();
        CapsuleItems.init();
        CapsuleCreativeTabs.init();
        CapsuleRecipes.init();
        CapsuleLootEntry.init();
    }

    public static PreparableReloadListener reloadListener() {
        return new StructureSaverReloadListener();
    }

    public static void serverStarting(MinecraftServer startingServer) {
        server = startingServer;
        Config.populateConfigFolders(server);
        Claims.loadAdapters();
        if (PrefabsBlueprintAggregatorRecipe.instance != null)
            PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(CapsuleMod.server.getResourceManager());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        CapsuleCommand.register(dispatcher);
    }

    public static void serverStopped() {
        server = null;
    }
}

class StructureSaverReloadListener extends SimplePreparableReloadListener<Void> {
    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        StructureSaver.getRewardManager(pResourceManager).onResourceManagerReload(pResourceManager);
        for (CapsuleTemplateManager ctm : StructureSaver.CapsulesManagers.values()) {
            ctm.onResourceManagerReload(pResourceManager);
        }
        return (Void) null;
    }

    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {

    }
}
