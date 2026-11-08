package edu.itc.salesreport.ui;

import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.concurrent.Task;

/** Task 4 skeleton: one javafx.concurrent.Task per job. */
public final class FxBackgroundJobs implements BackgroundJobs {

    private final Executor executor;

    public FxBackgroundJobs(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public <T> JobHandle submit(Work<T> work, JobListener<T> listener) {
        FxTask<T> task = new FxTask<>(work);
        
        // Forward task.progressProperty() to listener.progress(...)
        task.progressProperty().addListener((obs, oldVal, newVal) -> 
                listener.progress(newVal.doubleValue()));
                
        // Route task completion states to the listener
        task.setOnSucceeded(e -> listener.succeeded(task.getValue()));
        task.setOnFailed(e -> listener.failed(task.getException()));
        task.setOnCancelled(e -> listener.cancelled());
        
        executor.execute(task);
        return () -> task.cancel(true);
    }

    private static final class FxTask<T> extends Task<T>
            implements ProgressSink {
        private final Work<T> work;

        FxTask(Work<T> work) {
            this.work = work;
        }

        @Override
        protected T call() throws Exception {
            return work.run(this);
        }

        @Override
        public void update(long done, long total) {
            // Publish progress (thread-safe, coalesced by Task)
            updateProgress(done, total);
        }
    }
}