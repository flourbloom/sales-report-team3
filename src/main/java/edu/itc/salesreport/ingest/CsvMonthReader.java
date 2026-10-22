package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CsvMonthReader {
    private static final Pattern FILE_NAME = Pattern.compile(
            "(PNH|REP|BTB)-\\d{4}-\\d{2}-\\d{2}\\.csv");

    private final TransactionParser parser;

    public CsvMonthReader(TransactionParser parser) {
        this.parser = parser;
    }

    public MonthData read(Path dataDir, YearMonth month) throws IOException {
        Path monthDir = dataDir.resolve(month.toString());

        if (!Files.isDirectory(monthDir)) {
            throw new NoSuchFileException(monthDir.toString());
        }

        var rows = new ArrayList<SaleTransaction>();
        var rejected = new ArrayList<String>();

        try (Stream<Path> files = Files.list(monthDir)) {
            var csvFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> FILE_NAME.matcher(
                            path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()))
                    .toList();

            for (Path file : csvFiles) {
                readFile(file, rows, rejected);
            }
        }

        return new MonthData(month, rows, rejected);
    }

    private void readFile(
            Path file,
            List<SaleTransaction> rows,
            List<String> rejected) throws IOException {

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);

            try {
                rows.add(parser.parse(line));
            } catch (InvalidRowException e) {
                rejected.add("%s:%d %s".formatted(
                        file.getFileName(),
                        index + 1,
                        e.getMessage()));
            }
        }
    }
}