package capsule.structure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapsuleTemplateManagerTest {

    private static CapsuleTemplateManager manager(Path folder) {
        return new CapsuleTemplateManager(ResourceManager.Empty.INSTANCE, folder.toFile(), DataFixers.getDataFixer());
    }

    @Test
    void templatesAreWrittenUnderAnInstallPathWithReservedWindowsNames(@TempDir Path tmp) throws Exception {
        // Flatpak ATLauncher instances live under .../com.atlauncher.ATLauncher/..., "com" being a reserved name on Windows
        Path folder = Files.createDirectories(tmp.resolve("com.atlauncher.ATLauncher").resolve("aux").resolve("capsules"));
        ResourceLocation name = ResourceLocation.parse("c-player-1");

        CapsuleTemplateManager m = manager(folder);
        m.getOrCreateTemplate(name);
        assertTrue(m.writeToFile(name));

        assertTrue(Files.exists(folder.resolve("c-player-1.nbt")));
        assertNotNull(manager(folder).getTemplate(name));
    }

    @Test
    void pathsEscapingTheTemplateFolderAreRefusedWithoutException(@TempDir Path tmp) {
        CapsuleTemplateManager m = manager(tmp);
        ResourceLocation escaping = ResourceLocation.parse("../escaped");
        m.getOrCreateTemplate(escaping);

        assertFalse(m.writeToFile(escaping));
        assertFalse(m.deleteTemplate(escaping));
        assertFalse(Files.exists(tmp.resolveSibling("escaped.nbt")));
    }

    @Test
    void reservedNamesInTheTemplatePathAreRefusedWithoutException(@TempDir Path tmp) {
        CapsuleTemplateManager m = manager(tmp);
        ResourceLocation reserved = ResourceLocation.parse("con/template");
        m.getOrCreateTemplate(reserved);

        assertFalse(m.writeToFile(reserved));
    }
}
