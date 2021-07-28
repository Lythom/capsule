package capsule.helpers;

import capsule.Config;
import com.google.gson.*;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Files {

    protected static final Logger LOGGER = LogManager.getLogger(Files.class);
    private static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static JsonObject readJSON(File file) {
        if (file.exists()) {
            try (final InputStream stream = new FileInputStream(file)) {
                JsonObject jsonContent = JSONUtils.fromJson(GSON, new InputStreamReader(stream), JsonObject.class);
                return jsonContent;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static JsonObject copy(JsonObject original) {
        try {
            return GSON.fromJson(GSON.toJson(original, JsonObject.class), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static HashMap<String, JsonObject> populateWhitelistConfig(File capsuleConfigDir, IResourceManager ressourceManager) {
        if (!capsuleConfigDir.exists()) {
            capsuleConfigDir.mkdirs();
        }
        HashMap<String, JsonObject> blueprintWhitelist = new HashMap<>();
        if (capsuleConfigDir.exists() && capsuleConfigDir.isDirectory()) {
            File whitelistFile = new File(capsuleConfigDir, "blueprint_whitelist.json");
            if (!whitelistFile.exists()) {
                LOGGER.info("First load: initializing the configs in " + capsuleConfigDir.getPath() + ".");
                Files.populateFolder(capsuleConfigDir, "initialconfig/root", ressourceManager);
            }
            if (whitelistFile.exists()) {
                try (final InputStream stream = new FileInputStream(whitelistFile)) {
                    JsonArray whitelistElements = JSONUtils.fromJson(GSON, new InputStreamReader(stream), JsonArray.class);
                    if (whitelistElements != null)
                        for (JsonElement elem : whitelistElements) {
                            if (elem.isJsonPrimitive()) {
                                blueprintWhitelist.put(elem.getAsString(), null);
                            } else if (elem.isJsonObject()) {
                                JsonObject obj = elem.getAsJsonObject();
                                blueprintWhitelist.put(
                                        obj.get("block").getAsString(),
                                        obj.has("keepNBT") ? obj.get("keepNBT").getAsJsonObject() : null
                                );
                            }
                        }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error(whitelistFile.getPath() + " was expected to be found in config/capsule. Maybe it could not be created.");
            }
        }
        return blueprintWhitelist;
    }

    public static ArrayList<String> populateStarters(File capsuleConfigDir, String starterTemplatesPath, IResourceManager ressourceManager) {
        if (StringUtils.isNullOrEmpty(starterTemplatesPath)) return new ArrayList<>();
        File startersFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), starterTemplatesPath);

        if (!startersFolder.exists()) {
            startersFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the starters in " + starterTemplatesPath + ". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no starter capsule.");
            Files.populateFolder(startersFolder, "initialconfig/starters", ressourceManager);
        }
        ArrayList<String> starterTemplatesList = new ArrayList<>();
        iterateTemplates(startersFolder, templateName -> starterTemplatesList.add(starterTemplatesPath + "/" + templateName));
        return starterTemplatesList;
    }

    public static void populateAndLoadLootList(File capsuleConfigDir, Map<String, Config.LootPathData> lootTemplatesData, IResourceManager ressourceManager) {
        // Init the manager for reward Lists
        for (Config.LootPathData data : lootTemplatesData.values()) {
            File templateFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), data.path);

            if (!templateFolder.exists()) {
                templateFolder.mkdirs();
                // initial with example capsule the first time
                LOGGER.info("First load: initializing the loots in " + data.path + ". You can change the content of folder with any nbt structure block, schematic, or capsule file. You can remove the folders from capsule.config to remove loots.");
                String assetPath = null;
                if (templateFolder.getPath().contains(File.separatorChar + "uncommon")) assetPath = "initialconfig/loot/uncommon";
                if (templateFolder.getPath().contains(File.separatorChar + "rare")) assetPath = "initialconfig/loot/rare";
                if (templateFolder.getPath().contains(File.separatorChar + "common")) assetPath = "initialconfig/loot/common";
                if (assetPath != null) populateFolder(templateFolder, assetPath, ressourceManager);
            }

            data.files = new ArrayList<>();
            iterateTemplates(templateFolder, templateName -> data.files.add(templateName));
        }
    }

    public static ArrayList<String> populatePrefabs(File capsuleConfigDir, String prefabsTemplatesPath, IResourceManager ressourceManager) {
        File prefabsFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), prefabsTemplatesPath);

        if (!prefabsFolder.exists()) {
            prefabsFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the prefabs in " + prefabsTemplatesPath + ". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no blueprint prefabs recipes.");
            Files.populateFolder(prefabsFolder, "initialconfig/prefabs", ressourceManager);
        }
        ArrayList<String> prefabsTemplatesList = new ArrayList<>();
        iterateTemplates(prefabsFolder, templateName -> prefabsTemplatesList.add(prefabsTemplatesPath + "/" + templateName));
        LOGGER.info(prefabsTemplatesList.size() + " prefab capsules loaded.");
        return prefabsTemplatesList;
    }

    public static void populateFolder(File templateFolder, String assetPath, IResourceManager ressourceManager) {
        try {
            for (ResourceLocation ressourceLoc : ressourceManager.listResources(assetPath, s -> s.endsWith(".nbt") || s.endsWith(".json") || s.endsWith(".schematics"))) {
                IResource ressource = ressourceManager.getResource(ressourceLoc);
                // source path
                InputStream sourceTemplate = ressource.getInputStream();
                String sourcePath = ressourceLoc.getPath();
                String fileName = sourcePath.replace(assetPath + "/", "");
                Path assetFile = templateFolder.toPath().resolve(fileName);
                LOGGER.debug("copying asset " + assetPath + "/" + fileName + " to " + assetFile.toString());
                try {
                    File assetAsFile = assetFile.toFile();
                    if (assetAsFile.isDirectory()) {
                        if (!assetAsFile.exists()) assetAsFile.mkdirs();
                        populateFolder(assetAsFile, assetPath + "/" + fileName, ressourceManager);
                    } else {
                        File parentFolder = assetFile.getParent().toFile();
                        if (!parentFolder.exists()) parentFolder.mkdirs();
                        java.nio.file.Files.copy(sourceTemplate, assetFile);
                    }
                } catch (Exception e) {
                    LOGGER.error(e);
                }

            }
        } catch (IOException e) {
            LOGGER.error("Error while copying initial capsule templates, there will be no loots, prefabs or starters!", e);
        }
    }

    public static void iterateTemplates(File templateFolder, Consumer<String> onTemplateFound) {
        if (templateFolder.exists() && templateFolder.isDirectory()) {
            Iterator<Path> iterator = null;
            try {
                iterator = java.nio.file.Files.walk(templateFolder.toPath()).iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    File file = path.toFile();
                    if (file.isFile() && (file.getName().endsWith(".nbt") || file.getName().endsWith(".schematic"))) {
                        Path relative = templateFolder.toPath().relativize(path);
                        onTemplateFound.accept(relative.toString().replaceAll("\\\\", "/").replaceAll(".nbt", "").replaceAll(".schematic", ""));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
