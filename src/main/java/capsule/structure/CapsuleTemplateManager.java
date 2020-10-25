package capsule.structure;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FileUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.datafix.DefaultTypeReferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Initiated from mc original net.minecraft.world.gen.feature.template.TemplateManager, but using CapsuleTemplate instead and custom jar source folder.
 * Added support to load schematic file as Template.
 */
public class CapsuleTemplateManager implements IResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<ResourceLocation, CapsuleTemplate> templates = Maps.newHashMap();
    private final DataFixer fixer;
    private final MinecraftServer minecraftServer;
    private final Path pathGenerated;

    public CapsuleTemplateManager(MinecraftServer server, File templateFolder, DataFixer fixerIn) {
        this.minecraftServer = server;
        this.fixer = fixerIn;
        this.pathGenerated = templateFolder.toPath().resolve("generated").normalize();
        server.getResourceManager().addReloadListener(this);
    }

    public CapsuleTemplate getTemplateDefaulted(ResourceLocation p_200220_1_) {
        CapsuleTemplate template = this.getTemplate(p_200220_1_);
        if (template == null) {
            template = new CapsuleTemplate();
            this.templates.put(p_200220_1_, template);
        }

        return template;
    }

    @Nullable
    public CapsuleTemplate getTemplate(ResourceLocation p_200219_1_) {
        return this.templates.computeIfAbsent(p_200219_1_, (p_209204_1_) -> {
            CapsuleTemplate template = this.loadTemplateFile(p_209204_1_, ".schematics");
            if (template == null) template = this.loadTemplateFile(p_209204_1_, ".nbt");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".schematics");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".nbt");
            return template;
        });
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.templates.clear();
    }

    @Nullable
    private CapsuleTemplate loadTemplateResource(ResourceLocation p_209201_1_, String extension) {
        ResourceLocation resourcelocation = new ResourceLocation(p_209201_1_.getNamespace(), "structures/" + p_209201_1_.getPath() + extension);

        try (IResource iresource = this.minecraftServer.getResourceManager().getResource(resourcelocation)) {
            CapsuleTemplate template = this.loadTemplate(iresource.getInputStream(), ".schematics".equals(extension));
            return template;
        } catch (FileNotFoundException var18) {
            return null;
        } catch (Throwable throwable) {
            LOGGER.error("Couldn't load structure {}: {}", p_209201_1_, throwable.toString());
            return null;
        }
    }

    @Nullable
    private CapsuleTemplate loadTemplateFile(ResourceLocation locationIn, String extension) {
        if (!this.pathGenerated.toFile().isDirectory()) {
            return null;
        } else {
            Path path = this.resolvePath(locationIn, extension);

            try (InputStream inputstream = new FileInputStream(path.toFile())) {
                CapsuleTemplate template = this.loadTemplate(inputstream, ".schematics".equals(extension));
                return template;
            } catch (FileNotFoundException var18) {
                return null;
            } catch (IOException ioexception) {
                LOGGER.error("Couldn't load structure from {}", path, ioexception);
                return null;
            }
        }
    }

    private CapsuleTemplate loadTemplate(InputStream inputStreamIn, Boolean isSchematic) throws IOException {
        if (isSchematic) {
            return readTemplateFromSchematic(inputStreamIn);
        }
        CompoundNBT compoundnbt = CompressedStreamTools.readCompressed(inputStreamIn);
        return this.readFromNBT(compoundnbt);
    }

    public CapsuleTemplate readFromNBT(CompoundNBT p_227458_1_) {
        if (!p_227458_1_.contains("DataVersion", 99)) {
            p_227458_1_.putInt("DataVersion", 500);
        }

        CapsuleTemplate template = new CapsuleTemplate();
        template.read(NBTUtil.update(this.fixer, DefaultTypeReferences.STRUCTURE, p_227458_1_, p_227458_1_.getInt("DataVersion")));
        return template;
    }

    public boolean writeToFile(ResourceLocation templateName) {
        CapsuleTemplate template = this.templates.get(templateName);
        if (template == null) {
            return false;
        } else {
            Path path = this.resolvePath(templateName, ".nbt");
            Path path1 = path.getParent();
            if (path1 == null) {
                return false;
            } else {
                try {
                    Files.createDirectories(Files.exists(path1) ? path1.toRealPath() : path1);
                } catch (IOException var19) {
                    LOGGER.error("Failed to create parent directory: {}", (Object) path1);
                    return false;
                }

                CompoundNBT compoundnbt = template.writeToNBT(new CompoundNBT());

                try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
                    CompressedStreamTools.writeCompressed(compoundnbt, outputstream);
                    return true;
                } catch (Throwable var21) {
                    return false;
                }
            }
        }
    }

    public Path resolvePathStructures(ResourceLocation locationIn, String extIn) {
        try {
            Path path = this.pathGenerated.resolve(locationIn.getNamespace());
            Path path1 = path.resolve("structures");
            return FileUtil.func_214993_b(path1, locationIn.getPath(), extIn);
        } catch (InvalidPathException invalidpathexception) {
            throw new ResourceLocationException("Invalid resource path: " + locationIn, invalidpathexception);
        }
    }

    private Path resolvePath(ResourceLocation locationIn, String extIn) {
        if (locationIn.getPath().contains("//")) {
            throw new ResourceLocationException("Invalid resource path: " + locationIn);
        } else {
            Path path = this.resolvePathStructures(locationIn, extIn);
            if (path.startsWith(this.pathGenerated) && FileUtil.func_214995_a(path) && FileUtil.func_214994_b(path)) {
                return path;
            } else {
                throw new ResourceLocationException("Invalid resource path: " + path);
            }
        }
    }

    public void remove(ResourceLocation templatePath) {
        this.templates.remove(templatePath);
    }

    public CapsuleTemplate readTemplateFromSchematic(InputStream inputstream) {
        try {
            CompoundNBT schematicNBT = CompressedStreamTools.readCompressed(inputstream);
            CapsuleTemplate template = new CapsuleTemplate();
            // first raw conversion
            template.readSchematic(schematicNBT);
            // second conversion with update of data if needed
            CompoundNBT compoundnbt = template.writeToNBT(new CompoundNBT());
            return readFromNBT(compoundnbt);
        } catch (Throwable var10) {
            return null;
        }
    }

    public boolean deleteTemplate(@Nullable MinecraftServer server, ResourceLocation id) {
        if (server != null && this.templates.containsKey(id)) {
            File file = this.resolvePath(id, ".nbt").toFile();

            boolean deleted = file.delete();
            if (deleted) {
                remove(id);
            }
            return deleted;
        }
        return false;
    }
}