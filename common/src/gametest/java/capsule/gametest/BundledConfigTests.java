package capsule.gametest;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.helpers.Blueprint;
import capsule.recipes.PrefabsBlueprintAggregatorRecipe;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.datafix.DataFixers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * The config files Capsule ships in data/capsule/initialconfig, read with the code that reads them from config/ (D7 of
 * the 26.2 port: no migration of older configs, so what Capsule bundles must be right), and what a broken or missing
 * prefab_blueprint_recipe.json does. Their own batch: they rewrite config/capsule/prefab_blueprint_recipe.json and
 * rebuild the prefab recipes, which the other tests use.
 */
public class BundledConfigTests {

    private static final String BATCH = "bundledConfig";

    private static Path prefabRecipeFile() {
        return Config.getCapsuleConfigDir().resolve(Blueprint.PREFAB_RECIPE_FILE);
    }

    private static String bundled(MinecraftServer server, String assetPath) {
        Resource resource = server.getResourceManager().getResource(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, assetPath)).orElseThrow();
        try (InputStream stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe> populatePrefabRecipes(GameTestHelper helper) {
        PrefabsBlueprintAggregatorRecipe.instance.populateRecipes(helper.getLevel().getServer().getResourceManager(), helper.getLevel().registryAccess());
        return List.copyOf(PrefabsBlueprintAggregatorRecipe.instance.recipes);
    }

    /**
     * Runs test with config/capsule/prefab_blueprint_recipe.json as it is, then puts the file and the prefab recipes
     * back as they were.
     */
    private static void keepingPrefabRecipeFile(GameTestHelper helper, Runnable test) {
        Path file = prefabRecipeFile();
        try {
            String original = Files.readString(file);
            try {
                test.run();
            } finally {
                Files.writeString(file, original);
                populatePrefabRecipes(helper);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Every file of data/capsule/initialconfig: the two JSON files of config/capsule, the starters, the loot templates
     * and the prefabs, read without a warning or an error.
     */
    @CapsuleGameTest(template = "empty", batch = BATCH)
    public static void everyBundledConfigFileLoadsWithoutWarning(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> problems = new ArrayList<>();

        // blueprint_whitelist.json, read as Config reads config/capsule/blueprint_whitelist.json
        try (LogCapture logs = LogCapture.open()) {
            Path dir = Files.createTempDirectory("capsule-bundled-config");
            Files.writeString(dir.resolve("blueprint_whitelist.json"), bundled(server, "initialconfig/root/blueprint_whitelist.json"));
            Map<String, ?> whitelist = capsule.helpers.Files.populateWhitelistConfig(dir.toFile(), server.getResourceManager());
            JsonArray entries = JsonParser.parseString(bundled(server, "initialconfig/root/blueprint_whitelist.json")).getAsJsonArray();
            if (whitelist.size() != entries.size()) problems.add("blueprint_whitelist.json: " + whitelist.size() + " blocks read of " + entries.size() + " entries");
            logs.messages().forEach(m -> problems.add("blueprint_whitelist.json: " + m));
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted((a, b) -> b.compareTo(a)).map(Path::toFile).forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // prefab_blueprint_recipe.json, read and turned into the prefab recipes as when the server starts
        keepingPrefabRecipeFile(helper, () -> {
            try (LogCapture logs = LogCapture.open()) {
                Files.writeString(prefabRecipeFile(), bundled(server, "initialconfig/root/" + Blueprint.PREFAB_RECIPE_FILE));
                List<?> recipes = populatePrefabRecipes(helper);
                if (recipes.size() != Config.prefabsTemplatesList.size()) {
                    problems.add(Blueprint.PREFAB_RECIPE_FILE + ": " + recipes.size() + " prefab recipes for the " + Config.prefabsTemplatesList.size() + " prefabs " + Config.prefabsTemplatesList);
                }
                logs.messages().forEach(m -> problems.add(Blueprint.PREFAB_RECIPE_FILE + ": " + m));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // the starters, loot templates and prefabs copied to config/ on the first load, read from there by a fresh
        // template manager (the shared ones may have them cached)
        Map<String, List<String>> templates = new HashMap<>();
        templates.put("starters", Config.starterTemplatesList);
        templates.put("prefabs", Config.prefabsTemplatesList);
        Config.lootTemplatesData.values().forEach(data -> templates.put("loot " + data.path, data.files.stream().map(name -> data.path + "/" + name).toList()));
        CapsuleTemplateManager manager = new CapsuleTemplateManager(server.getResourceManager(), new File("."), DataFixers.getDataFixer());
        int read = 0;
        for (Map.Entry<String, List<String>> kind : templates.entrySet()) {
            if (kind.getValue().isEmpty()) problems.add(kind.getKey() + ": none in config");
            for (String path : kind.getValue()) {
                try (LogCapture logs = LogCapture.open()) {
                    CapsuleTemplate template = manager.getTemplate(Identifier.parse(path));
                    if (template == null || template.getPalette().isEmpty() && template.entities.isEmpty()) problems.add(path + ": empty or unreadable");
                    logs.messages().forEach(m -> problems.add(path + ": " + m));
                }
                read++;
            }
        }
        long bundledTemplates = BundledContentTests.bundledTemplates(helper).size();
        assertTrue(helper, read == bundledTemplates, "every bundled template should be in config, read " + read + " of " + bundledTemplates + ": " + templates);
        assertTrue(helper, problems.isEmpty(), "bundled config files with problems: " + problems);
        helper.succeed();
    }

    /**
     * A prefab_blueprint_recipe.json that does not parse, or in the format of Capsule 9 (1.21.1), is not converted
     * (D7): one error names the file, what failed and how to restore the default, there is no prefab recipe, and nothing
     * crashes.
     */
    @CapsuleGameTest(template = "empty", batch = BATCH)
    public static void brokenPrefabRecipeFileIsReported(GameTestHelper helper) {
        String capsule9 = bundled(helper.getLevel().getServer(), "initialconfig/root/" + Blueprint.PREFAB_RECIPE_FILE)
                .replace("\"minecraft:dyed_color\": 2963801", "\"minecraft:dyed_color\": {\"rgb\": 2963801, \"show_in_tooltip\": false}")
                .replace("\"b\": \"minecraft:stone_button\"", "\"b\": {\"item\": \"minecraft:stone_button\"}")
                .replace("\"p\": \"minecraft:paper\"", "\"p\": {\"item\": \"minecraft:paper\"}")
                .replace("\"l\": \"minecraft:blue_dye\"", "\"l\": {\"item\": \"minecraft:blue_dye\"}");
        assertTrue(helper, capsule9.contains("\"item\": \"minecraft:paper\"") && capsule9.contains("show_in_tooltip"), "the Capsule 9 file should be made from the bundled one");
        Map<String, String> brokenFiles = Map.of(
                "invalid json", "{ \"type\": \"capsule:blueprint_prefabs\", \"pattern\": [",
                "Capsule 9 format", capsule9);
        keepingPrefabRecipeFile(helper, () -> brokenFiles.forEach((kind, content) -> {
            try (LogCapture logs = LogCapture.open()) {
                Files.writeString(prefabRecipeFile(), content);
                List<?> recipes = populatePrefabRecipes(helper);
                assertTrue(helper, recipes.isEmpty(), kind + ": no prefab recipe should be made, got " + recipes.size());
                List<String> errors = logs.containing(Blueprint.PREFAB_RECIPE_FILE);
                assertTrue(helper, errors.size() == 1 && logs.messages().size() == 1, kind + ": one error should be logged, got " + logs.messages());
                String error = errors.get(0);
                assertTrue(helper, error.contains(prefabRecipeFile().toFile().getPath()) && error.contains("cannot be used, it ") && error.contains("Delete this file to restore the default"),
                        kind + ": the error should name the file, what failed and how to restore the default, got " + error);
                assertTrue(helper, Files.readString(prefabRecipeFile()).equals(content), kind + ": the file should be left as it is");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
        assertTrue(helper, !populatePrefabRecipes(helper).isEmpty(), "the prefab recipes should be back with the restored file");
        helper.succeed();
    }

    /**
     * A deleted prefab_blueprint_recipe.json is recreated from the bundled default, and the prefab recipes made from it.
     */
    @CapsuleGameTest(template = "empty", batch = BATCH)
    public static void missingPrefabRecipeFileIsRecreatedFromTheDefault(GameTestHelper helper) {
        keepingPrefabRecipeFile(helper, () -> {
            try (LogCapture logs = LogCapture.open()) {
                Files.delete(prefabRecipeFile());
                List<?> recipes = populatePrefabRecipes(helper);
                assertTrue(helper, Files.exists(prefabRecipeFile()), "the file should be recreated");
                String expected = bundled(helper.getLevel().getServer(), "initialconfig/root/" + Blueprint.PREFAB_RECIPE_FILE);
                assertTrue(helper, Files.readString(prefabRecipeFile()).equals(expected), "the file should be the bundled default");
                assertTrue(helper, recipes.size() == Config.prefabsTemplatesList.size(), "every prefab should get its recipe, got " + recipes.size());
                assertTrue(helper, logs.messages().isEmpty(), "nothing should be logged above info, got " + logs.messages());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        helper.succeed();
    }
}
