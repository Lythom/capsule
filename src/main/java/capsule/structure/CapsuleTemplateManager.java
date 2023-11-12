package capsule.structure;

import capsule.CapsuleMod;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Initiated from mc original net.minecraft.world.gen.feature.template.TemplateManager, but using CapsuleTemplate instead and custom jar source folder.
 * Added support to load schematic file as Template.
 */
public class CapsuleTemplateManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<ResourceLocation, CapsuleTemplate> templates = Maps.newHashMap();
    private final DataFixer fixer;
    private ResourceManager resourceManager;
    private final Path pathGenerated;

    public CapsuleTemplateManager(ResourceManager resourceManager, File templateFolder, DataFixer fixerIn) {
        this.resourceManager = resourceManager;
        this.fixer = fixerIn;
        this.pathGenerated = templateFolder.toPath().normalize();
    }

    public CapsuleTemplate getOrCreateTemplate(ResourceLocation templateLocation) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation(CapsuleMod.MODID, templateLocation.getPath());
        CapsuleTemplate template = this.getTemplate(capsuleTemplateLocation);
        if (template == null) {
            template = new CapsuleTemplate();
            this.templates.put(capsuleTemplateLocation, template);
        }

        return template;
    }

    @Nullable
    public CapsuleTemplate getTemplate(ResourceLocation templateLocation) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation(CapsuleMod.MODID, templateLocation.getPath());
        return this.templates.computeIfAbsent(capsuleTemplateLocation, (p_209204_1_) -> {
            CapsuleTemplate template = this.loadTemplateFile(p_209204_1_, ".schematic");
            if (template == null) template = this.loadTemplateFile(p_209204_1_, ".nbt");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".schematic");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".nbt");
            return template;
        });
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.templates.clear();
    }

    @Nullable
    private CapsuleTemplate loadTemplateResource(ResourceLocation p_209201_1_, String extension) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation("capsule", p_209201_1_.getPath() + extension);
        try (Resource iresource = this.resourceManager.getResource(capsuleTemplateLocation)) {
            CapsuleTemplate template = this.loadTemplate(iresource.getInputStream(), ".schematic".equals(extension), capsuleTemplateLocation.toString());
            return template;
        } catch (FileNotFoundException var18) {
            return null;
        } catch (Exception ex) {
            LOGGER.error("Couldn't load structure {}", capsuleTemplateLocation, ex);
            return null;
        }
    }

    @Nullable
    private CapsuleTemplate loadTemplateFile(ResourceLocation locationIn, String extension) {
        if (!this.pathGenerated.toAbsolutePath().toFile().isDirectory()) {
            return null;
        } else {
            try {
                Path path = this.resolvePath(locationIn, extension);

                try (InputStream inputstream = new FileInputStream(path.toFile())) {
                    CapsuleTemplate template = this.loadTemplate(inputstream, ".schematic".equals(extension), path.toString());
                    return template;
                } catch (FileNotFoundException var18) {
                    return null;
                } catch (IOException ioexception) {
                    LOGGER.error("Couldn't load structure from {}", path, ioexception);
                    return null;
                } catch (Exception exception) {
                    LOGGER.error("Error while loading {}", path, exception);
                    return null;
                }
            } catch (ResourceLocationException e) {
                LOGGER.error("Couldn't resolve proper location: {}", locationIn.getPath(), e);
                return null;
            }
        }
    }

    private CapsuleTemplate loadTemplate(InputStream inputStreamIn, Boolean isSchematic, String location) throws Exception {
        if (isSchematic) {
            return readTemplateFromSchematic(inputStreamIn, location);
        }
        CompoundTag compoundnbt = NbtIo.readCompressed(inputStreamIn);
        return this.readFromNBT(compoundnbt, location);
    }

    public CapsuleTemplate readFromNBT(CompoundTag p_227458_1_, String location) {
        if (!p_227458_1_.contains("DataVersion", 99)) {
            p_227458_1_.putInt("DataVersion", 500);
        }

        CapsuleTemplate template = new CapsuleTemplate();
        template.load(NbtUtils.update(this.fixer, DataFixTypes.STRUCTURE, p_227458_1_, p_227458_1_.getInt("DataVersion")), location);
        return template;
    }

    public boolean writeToFile(ResourceLocation templateName) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation(CapsuleMod.MODID, templateName.getPath());
        CapsuleTemplate template = this.templates.get(capsuleTemplateLocation);
        if (template == null) {
            return false;
        } else {
            Path path = this.resolvePath(capsuleTemplateLocation, ".nbt");
            Path path1 = path.getParent();
            if (path1 == null) {
                return false;
            } else {
                try {
                    Files.createDirectories(Files.exists(path1) ? path1.toRealPath() : path1);
                } catch (IOException var19) {
                    LOGGER.error("Failed to create parent directory: {}", path1);
                    return false;
                }

                CompoundTag compoundnbt = template.save(new CompoundTag());

                try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
                    NbtIo.writeCompressed(compoundnbt, outputstream);
                    return true;
                } catch (Throwable var21) {
                    return false;
                }
            }
        }
    }

    private Path resolvePath(ResourceLocation locationIn, String extIn) {
        if (locationIn.getPath().contains("//")) {
            throw new ResourceLocationException("Invalid resource path: " + locationIn);
        } else {
            String ext = locationIn.getPath().endsWith(extIn) ? "" : extIn;
            Path p = this.pathGenerated.toAbsolutePath().resolve(locationIn.getPath() + ext).normalize();
            if (FileUtil.isPathNormalized(p) && FileUtil.isPathPortable(p)) {
                return p;
            } else {
                throw new ResourceLocationException("Invalid resource path: " + p);
            }
        }
    }

    public void remove(ResourceLocation templateLocation) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation(CapsuleMod.MODID, templateLocation.getPath());
        this.templates.remove(capsuleTemplateLocation);
    }

    public CapsuleTemplate readTemplateFromSchematic(InputStream inputstream, String location) throws Exception {
            CompoundTag schematicNBT = NbtIo.readCompressed(inputstream);
            CapsuleTemplate template = new CapsuleTemplate();
            // first raw conversion
            template.readSchematic(schematicNBT);
            // second conversion with update of data if needed
            CompoundTag compoundnbt = template.save(new CompoundTag());
            return readFromNBT(compoundnbt, location);
    }

    public boolean deleteTemplate(ResourceLocation templateLocation) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation(CapsuleMod.MODID, templateLocation.getPath());
        if (this.templates.containsKey(capsuleTemplateLocation)) {
            File file = this.resolvePath(capsuleTemplateLocation, ".nbt").toFile();

            boolean deleted = file.delete();
            if (deleted) {
                remove(capsuleTemplateLocation);
            }
            return deleted;
        }
        return false;
    }
}
