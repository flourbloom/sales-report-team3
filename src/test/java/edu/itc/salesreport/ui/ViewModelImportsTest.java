package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ViewModelImportsTest {
    // task5
    // @Test
    // void testViewModelImportsOnlyBeansAndCollections() throws IOException {
    // Path vmFile =
    // Path.of("src/main/java/edu/itc/salesreport/ui/ReportViewModel.java");
    // List<String> lines = Files.readAllLines(vmFile);

    // long sceneImports = lines.stream()
    // .filter(l -> l.startsWith("import javafx.scene"))
    // .count();
    // assertEquals(0, sceneImports, "ReportViewModel must not import
    // javafx.scene");

    // long stageImports = lines.stream()
    // .filter(l -> l.startsWith("import javafx.stage"))
    // .count();
    // assertEquals(0, stageImports, "ReportViewModel must not import
    // javafx.stage");
    // }

    // @Test
    // void testNoYearMonthNowWithoutClockInUi() throws IOException {
    // Path uiDir = Path.of("src/main/java/edu/itc/salesreport/ui");
    // try (var stream = Files.walk(uiDir)) {
    // var javaFiles = stream.filter(p -> p.toString().endsWith(".java") &&
    // !p.toString().contains("mvc")).toList();
    // for (Path file : javaFiles) {
    // List<String> lines = Files.readAllLines(file);
    // for (int i = 0; i < lines.size(); i++) {
    // String line = lines.get(i);
    // assertFalse(line.contains("YearMonth.now()") &&
    // !line.contains("YearMonth.now(clock)"),
    // "File " + file.getFileName() + ":" + (i + 1) + " must not use YearMonth.now()
    // without clock");
    // }
    // }
    // }
    // }
}
