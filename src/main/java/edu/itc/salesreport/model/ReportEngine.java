package edu.itc.salesreport.model;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

public interface ReportEngine {
    MonthlyReport generate(YearMonth month, Path dataDir) throws IOException;
}