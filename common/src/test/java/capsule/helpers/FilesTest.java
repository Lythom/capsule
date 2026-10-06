package capsule.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesTest {

    @Test
    void templateNamesAreExtensionlessValidResourcePaths(@TempDir Path tmp) throws Exception {
        for (String name : List.of("My House.nbt", "ok_house.nbt", "cabinbt.nbt", "sub/tower.schematic", "sub/hall.schem", "notes.txt")) {
            Path file = tmp.resolve(name);
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file, "");
        }

        List<String> names = new ArrayList<>();
        Files.iterateTemplates(tmp.toFile(), names::add);
        names.sort(String::compareTo);

        assertEquals(List.of("cabinbt", "ok_house", "sub/hall", "sub/tower"), names);
    }
}
