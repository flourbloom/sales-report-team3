package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SimpleReportEngine;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Automates the measurement of load times and UI frame gaps
 * to compare CallerThreadJobs (freezes UI) vs FxBackgroundJobs (responsive UI).
 */
public class LoadBenchmarkApp extends Application {

    private ExecutorService executor;

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed().getOrDefault("data", "data"));
        YearMonth month = YearMonth.parse(getParameters().getNamed().getOrDefault("month", "2026-08"));
        
        System.out.printf("%-14s %3s %8s %17s%n", "mode", "run", "load ms", "max frame gap ms");

        var engine = new SimpleReportEngine();
        SummarySource source = (m, b) -> engine.summarizeBranch(m, b, dataDir);

        executor = Executors.newVirtualThreadPerTaskExecutor();

        // Run the benchmark sequence on a background thread so we don't freeze the app ourselves
        Thread orchestrator = new Thread(() -> {
            try {
                runMode("caller-thread", new CallerThreadJobs(), source, month);
                runMode("fx-task", new FxBackgroundJobs(executor), source, month);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
            }
        });
        orchestrator.setDaemon(true);
        orchestrator.start();
    }

    private void runMode(String mode, BackgroundJobs jobs, SummarySource source, YearMonth month) throws Exception {
        long totalLoadMs = 0;
        double maxGapForMode = 0;

        for (int i = -1; i <= 5; i++) {
            CountDownLatch readyLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(1);
            FrameGapMonitor monitor = new FrameGapMonitor();
            
            ReportViewModel vm = new ReportViewModel(source, jobs, Clock.systemDefaultZone());

            Platform.runLater(() -> {
                vm.monthProperty().set(month);
                for (String b : ReportViewModel.BRANCHES) {
                    vm.branchSelectedProperty(b).set(true);
                }
                
                vm.loadingProperty().addListener((obs, oldVal, newVal) -> {
                    if (oldVal && !newVal) {
                        doneLatch.countDown();
                    }
                });
                
                // Start the monitor slightly before we trigger the load
                monitor.start();
                readyLatch.countDown();
            });
            readyLatch.await();
            
            // Give the monitor a tiny moment to register a baseline frame
            Thread.sleep(50); 

            long start = System.currentTimeMillis();
            
            Platform.runLater(() -> {
                // If this is the caller-thread, this call will block the UI thread right here
                vm.generateCommand().execute(); 
                
                // If it was the caller-thread, the load is already finished by the time we reach this line.
                // We must artificially trigger the latch because 'loading' property might not transition as expected synchronously.
                if (mode.equals("caller-thread")) {
                     doneLatch.countDown();
                }
            });
            
            doneLatch.await();
            
            long elapsed = System.currentTimeMillis() - start;

            Platform.runLater(monitor::stop);
            Thread.sleep(50); 

            double maxGap = monitor.maxGapMillis();

            if (i >= 1) {
                totalLoadMs += elapsed;
                maxGapForMode = Math.max(maxGapForMode, maxGap);
                System.out.printf("%-14s %3d %8d %17.0f%n", mode, i, elapsed, maxGap);
            }
        }

        System.out.printf("%-13s mean load %6d ms, worst frame gap %6.0f ms%n", 
                mode, totalLoadMs / 5, maxGapForMode);
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}