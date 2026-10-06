package capsule.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesTest {

    @Test
    void templateNamesAreExtensionlessValidResourcePaths(@TempDir Path tmp) throws Exception {
        for (String name : Arrays.asList("My House.nbt", "ok_house.nbt", "cabinbt.nbt", "sub/tower.schematic", "notes.txt")) {
            Path file = tmp.resolve(name);
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.createFile(file);
        }

        List<String> names = new ArrayList<>();
        Files.iterateTemplates(tmp.toFile(), names::add);
        names.sort(String::compareTo);

        assertEquals(Arrays.asList("cabinbt", "ok_house", "sub/tower"), names);
    }
}
