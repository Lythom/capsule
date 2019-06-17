package capsule.helpers;

import capsule.Config;
import capsule.loot.LootPathData;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.JsonUtils;
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

    public static void populateAndLoadConfigs(MinecraftServer server) {
        File capsuleConfigFolder = new File(server.getDataDirectory(), "config/capsule");

        if (!capsuleConfigFolder.exists()) {
            capsuleConfigFolder.mkdirs();
        }
        Config.blueprintWhitelist = new HashMap<>();
        if (capsuleConfigFolder.exists() && capsuleConfigFolder.isDirectory()) {
            File capsuleConfigFile = new File(capsuleConfigFolder, "blueprint_whitelist.json");
            if (!capsuleConfigFile.exists()) {
                LOGGER.info("First load: initializing the configs in " + capsuleConfigFolder.getPath() + ".");
                Files.populateFolder(capsuleConfigFolder, "assets/capsule/config");
            }
            if (capsuleConfigFile.exists()) {
                final Gson gson = new GsonBuilder().
                        setPrettyPrinting().
                        create();
                try (final InputStream stream = new FileInputStream(capsuleConfigFile)) {
                    JsonArray whitelistElements = JsonUtils.fromJson(gson, new InputStreamReader(stream), JsonArray.class);
                    if (whitelistElements != null)
                        for (JsonElement elem : whitelistElements) {
                            if (elem.isJsonPrimitive()) {
                                Config.blueprintWhitelist.put(elem.getAsString(), null);
                            } else if (elem.isJsonObject()) {
                                JsonObject obj = elem.getAsJsonObject();
                                Config.blueprintWhitelist.put(
                                        obj.get("block").getAsString(),
                                        obj.has("keepNBT") ? obj.get("keepNBT").getAsJsonObject() : null
                                );
                            }
                        }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error(capsuleConfigFile.getPath() + " was expected to be found in config/capsule. Maybe it could not be created.");
            }
        }
    }

    public static void populateAndLoadStarters(MinecraftServer server) {
        String path = Config.starterTemplatesPath;
        File templateFolder = new File(server.getDataDirectory(), path);

        if (path.startsWith("config/") && !templateFolder.exists()) {
            templateFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the starters in " + path + ". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no starter capsule.");
            Files.populateFolder(templateFolder, "assets/capsule/starters");
        }
        Config.starterTemplatesList = new ArrayList<>();
        iterateTemplates(templateFolder, templateName -> {
            Config.starterTemplatesList.add(Config.starterTemplatesPath + "/" + templateName);
        });
    }

    public static void populateAndLodloadLootList(MinecraftServer server) {
        // Init the manager for reward Lists
        for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
            String path = Config.lootTemplatesPaths[i];
            LootPathData data = Config.lootTemplatesData.get(path);

            File templateFolder = new File(server.getDataDirectory(), path);

            if (path.startsWith("config/") && !templateFolder.exists()) {
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
                        java.nio.file.Files.copy(sourceTemplate, assetFile);
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
            File[] fileList = templateFolder.listFiles((p_accept_1_, p_accept_2_) -> p_accept_2_.endsWith(".nbt") || p_accept_2_.endsWith(".schematic"));
            if (fileList != null) {
                for (File templateFile : fileList) {
                    if (templateFile.isFile() && (templateFile.getName().endsWith(".nbt") || templateFile.getName().endsWith(".schematic"))) {
                        onTemplateFound.accept(templateFile.getName().replaceAll(".nbt", "").replaceAll(".schematic", ""));
                    }
                }
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
     * @throws URISyntaxException
     * @throws IOException
     * @author Greg Briggs
     */
    public static String[] getResourceListing(Class<?> clazz, String path) throws URISyntaxException, IOException {
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

        if (dirURL.getProtocol().equals("jar")) {
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
            return result.toArray(new String[result.size()]);

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
                return result.toArray(new String[result.size()]);
            }

        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }
}
