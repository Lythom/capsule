package capsule.helpers;

import capsule.loot.LootPathData;
import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Files {

    protected static final Logger LOGGER = LogManager.getLogger(Files.class);
    private static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static JsonObject readJSON(File file) {
        if (file.exists()) {
            try (final InputStream stream = new FileInputStream(file)) {
                JsonObject jsonContent = JsonUtils.fromJson(GSON, new InputStreamReader(stream), JsonObject.class);
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

    public static HashMap<String, JsonObject> populateWhitelistConfig(File capsuleConfigDir) {
        if (!capsuleConfigDir.exists()) {
            capsuleConfigDir.mkdirs();
        }
        HashMap<String, JsonObject> blueprintWhitelist = new HashMap<>();
        if (capsuleConfigDir.exists() && capsuleConfigDir.isDirectory()) {
            File whitelistFile = new File(capsuleConfigDir, "blueprint_whitelist.json");
            if (!whitelistFile.exists()) {
                LOGGER.info("First load: initializing the configs in " + capsuleConfigDir.getPath() + ".");
                Files.populateFolder(capsuleConfigDir, "assets/capsule/config");
            }
            if (whitelistFile.exists()) {
                try (final InputStream stream = new FileInputStream(whitelistFile)) {
                    JsonArray whitelistElements = JsonUtils.fromJson(GSON, new InputStreamReader(stream), JsonArray.class);
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

    public static ArrayList<String> populateStarters(File capsuleConfigDir, String starterTemplatesPath) {
        if (StringUtils.isNullOrEmpty(starterTemplatesPath)) return new ArrayList<>();
        File startersFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), starterTemplatesPath);

        if (!startersFolder.exists()) {
            startersFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the starters in " + starterTemplatesPath + ". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no starter capsule.");
            Files.populateFolder(startersFolder, "assets/capsule/starters");
        }
        ArrayList<String> starterTemplatesList = new ArrayList<>();
        iterateTemplates(startersFolder, templateName -> {
            starterTemplatesList.add(starterTemplatesPath + "/" + templateName);
        });
        return starterTemplatesList;
    }

    public static void populateAndLoadLootList(File capsuleConfigDir, String[] lootTemplatesPaths, Map<String, LootPathData> outLootTemplatesData) {
        // Init the manager for reward Lists
        for (String path : lootTemplatesPaths) {
            LootPathData data = outLootTemplatesData.get(path);

            File templateFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), path);

            if (!templateFolder.exists()) {
                templateFolder.mkdirs();
                // initial with example capsule the first time
                LOGGER.info("First load: initializing the loots in " + path + ". You can change the content of folder with any nbt structure block, schematic, or capsule file. You can remove the folders from capsule.config to remove loots.");
                String assetPath = "assets/capsule/loot/common";
                if (templateFolder.getPath().contains("uncommon")) assetPath = "assets/capsule/loot/uncommon";
                if (templateFolder.getPath().contains("rare")) assetPath = "assets/capsule/loot/rare";
                populateFolder(templateFolder, assetPath);
            }

            data.files = new ArrayList<>();
            iterateTemplates(templateFolder, templateName -> {
                data.files.add(templateName);
            });
        }
    }

    public static ArrayList<String> populatePrefabs(File capsuleConfigDir, String prefabsTemplatesPath) {
        File prefabsFolder = new File(capsuleConfigDir.getParentFile().getParentFile(), prefabsTemplatesPath);

        if (!prefabsFolder.exists()) {
            prefabsFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the prefabs in " + prefabsTemplatesPath + ". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no blueprint prefabs recipes.");
            Files.populateFolder(prefabsFolder, "assets/capsule/prefabs");
        }
        ArrayList<String> prefabsTemplatesList = new ArrayList<>();
        iterateTemplates(prefabsFolder, templateName -> {
            prefabsTemplatesList.add(prefabsTemplatesPath + "/" + templateName);
        });
        return prefabsTemplatesList;
    }

    public static void populateFolder(File templateFolder, String assetPath) {
        try {
            // source path
            String[] resources = getResourceListing(Files.class, assetPath);
            for (String ressource : resources) {
                if (!ressource.isEmpty()) {
                    InputStream sourceTemplate = Files.class.getClassLoader().getResourceAsStream(assetPath + "/" + ressource);
                    if (sourceTemplate == null) {
                        LOGGER.error("asset " + assetPath + "/" + ressource + "couldn't be loaded");
                        break;
                    }
                    Path assetFile = templateFolder.toPath().resolve(ressource.toLowerCase());
                    LOGGER.debug("copying asset " + assetPath + "/" + ressource + " to " + assetFile.toString());
                    try {
                        if (ressource.contains(".")) {
                            // assume file
                            java.nio.file.Files.copy(sourceTemplate, assetFile);
                        } else {
                            // assume directory
                            File assetFileDirectory = assetFile.toFile();
                            if (!assetFileDirectory.exists()) assetFileDirectory.mkdirs();
                            populateFolder(assetFileDirectory, assetPath + "/" + ressource);
                        }
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while copying initial capsule templates, there will be no loots!", e);
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

    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path  Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @author Greg Briggs
     */
    public static String[] getResourceListing(Class<?> clazz, String path) throws
            URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory. Have
             * to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

            LOGGER.debug("Listing files in " + jarPath);

            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    LOGGER.debug("Found in jar " + name);
                    String entry = name.replace(path + "/", "");
                    LOGGER.debug("Keeping " + entry);
                    result.add(entry);
                }
            }
            jar.close();
            return result.toArray(new String[0]);

        } else {

            InputStream inputstream = clazz.getResourceAsStream("/" + path);
            if (inputstream != null) {
                final InputStreamReader isr = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr);

                Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
                String filename = null;
                while ((filename = br.readLine()) != null) {
                    result.add(filename);
                }
                return result.toArray(new String[0]);
            }

        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

}
