package capsule.structure;

import capsule.CapsuleMod;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.FileUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.datafix.DefaultTypeReferences;
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
    private IResourceManager resourceManager;
    private final Path pathGenerated;

    public CapsuleTemplateManager(IResourceManager resourceManager, File templateFolder, DataFixer fixerIn) {
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
            CapsuleTemplate template = this.loadTemplateFile(p_209204_1_, ".schematics");
            if (template == null) template = this.loadTemplateFile(p_209204_1_, ".nbt");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".schematics");
            if (template == null) template = this.loadTemplateResource(p_209204_1_, ".nbt");
            return template;
        });
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.templates.clear();
    }

    @Nullable
    private CapsuleTemplate loadTemplateResource(ResourceLocation p_209201_1_, String extension) {
        ResourceLocation capsuleTemplateLocation = new ResourceLocation("capsule", p_209201_1_.getPath() + extension);
        try (IResource iresource = this.resourceManager.getResource(capsuleTemplateLocation)) {
            CapsuleTemplate template = this.loadTemplate(iresource.getInputStream(), ".schematics".equals(extension), capsuleTemplateLocation.toString());
            return template;
        } catch (FileNotFoundException var18) {
            return null;
        } catch (Throwable throwable) {
            LOGGER.error("Couldn't load structure {}: {}", capsuleTemplateLocation, throwable.toString());
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
                    CapsuleTemplate template = this.loadTemplate(inputstream, ".schematics".equals(extension), path.toString());
                    return template;
                } catch (FileNotFoundException var18) {
                    return null;
                } catch (IOException ioexception) {
                    LOGGER.error("Couldn't load structure from {}", path, ioexception);
                    return null;
                }
            } catch (ResourceLocationException e) {
                LOGGER.error("Couldn't resolve proper location: {}", locationIn.getPath(), e);
                return null;
            }
        }
    }

    private CapsuleTemplate loadTemplate(InputStream inputStreamIn, Boolean isSchematic, String location) throws IOException {
        if (isSchematic) {
            return readTemplateFromSchematic(inputStreamIn, location);
        }
        CompoundNBT compoundnbt = CompressedStreamTools.readCompressed(inputStreamIn);
        return this.readFromNBT(compoundnbt, location);
    }

    public CapsuleTemplate readFromNBT(CompoundNBT p_227458_1_, String location) {
        if (!p_227458_1_.contains("DataVersion", 99)) {
            p_227458_1_.putInt("DataVersion", 500);
        }

        CapsuleTemplate template = new CapsuleTemplate();
        template.load(NBTUtil.update(this.fixer, DefaultTypeReferences.STRUCTURE, p_227458_1_, p_227458_1_.getInt("DataVersion")), location);
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

                CompoundNBT compoundnbt = template.save(new CompoundNBT());

                try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
                    CompressedStreamTools.writeCompressed(compoundnbt, outputstream);
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
            Path p = this.pathGenerated.resolve(locationIn.getPath() + ext);
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

    public CapsuleTemplate readTemplateFromSchematic(InputStream inputstream, String location) {
        try {
            CompoundNBT schematicNBT = CompressedStreamTools.readCompressed(inputstream);
            CapsuleTemplate template = new CapsuleTemplate();
            // first raw conversion
            template.readSchematic(schematicNBT);
            // second conversion with update of data if needed
            CompoundNBT compoundnbt = template.save(new CompoundNBT());
            return readFromNBT(compoundnbt, location);
        } catch (Throwable var10) {
            return null;
        }
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