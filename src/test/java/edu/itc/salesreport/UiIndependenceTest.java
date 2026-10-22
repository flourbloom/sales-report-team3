package edu.itc.salesreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Makes "no UI toolkit in the core" executable. Reads source files only. */
class UiIndependenceTest {
    private static final Path SRC =
            Path.of("src", "main", "java", "edu", "itc", "salesreport");

    /** Packages that must stay toolkit-free (files directly inside each). */
    private static final List<String> CORE =
            List.of("model", "ingest", "engine", "ui", "ui/mvp");

    private static final List<String> FORBIDDEN = List.of(
            "import javafx.", "import java.awt.", "import javax.swing.",
            "import static javafx.", "import static java.awt.",
            "import static javax.swing.");

    @Test
    void corePackagesImportNoUiToolkit() throws IOException {
        var violations = new ArrayList<String>();
        int scanned = 0;

        for (String pkg : CORE) {
            Path dir = SRC.resolve(pkg);
            if (!Files.isDirectory(dir)) continue;   // ui/mvp comes in Task 5

            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java"))
                                      .sorted().toList()) {
                    scanned++;
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i).strip();
                        if (FORBIDDEN.stream().anyMatch(line::startsWith)) {
                            violations.add("%s:%d %s".formatted(file, i + 1, line));
                        }
                    }
                }
            }
        }

        assertTrue(scanned > 0, "no source files found: wrong working directory?");
        assertEquals(List.of(), violations, "UI toolkit import in the core");
    }
}
