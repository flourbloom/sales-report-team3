package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SimpleReportEngine;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Composition root: the only class that knows every concrete type. */
public class ReportViewerApp extends Application {

    private ExecutorService executor;

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        
        var engine = new SimpleReportEngine();
        SummarySource source =
                (month, branch) -> engine.summarizeBranch(month, branch, dataDir);
        
        // Task 4 (Step 3): Use FxBackgroundJobs with Virtual Threads
        executor = Executors.newVirtualThreadPerTaskExecutor();
        var jobs = new FxBackgroundJobs(executor);
        
        var vm = new ReportViewModel(source, jobs, Clock.systemDefaultZone());
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}