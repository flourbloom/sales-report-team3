package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SimpleReportEngine;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Composition root: the only class that knows every concrete type. */
public class ReportViewerApp extends Application {

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        // adapt YOUR Lab-02 model here; the model code stays unchanged
        var engine = new SimpleReportEngine();
        SummarySource source =
                (month, branch) -> engine.summarizeBranch(month, branch,
                        dataDir);
        // Default to 2026-09-30 reference clock so 2026-10 is in the future 
        String clockParam = getParameters().getNamed().get("clock");
        Clock clock = clockParam != null
                ? Clock.fixed(YearMonth.parse(clockParam).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
                : Clock.fixed(Instant.parse("2026-09-30T00:00:00Z"), ZoneId.systemDefault());
        // Task 2: CallerThreadJobs (UI still freezes, like Lab-02)
        // Task 4: new FxBackgroundJobs(workers)
        var vm = new ReportViewModel(source, new CallerThreadJobs(), clock);
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}