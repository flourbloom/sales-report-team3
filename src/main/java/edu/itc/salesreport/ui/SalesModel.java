// src/main/java/edu/itc/salesreport/ui/SalesModel.java
package edu.itc.salesreport.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.MonthData;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.SaleTransaction;

// imports: engine.SalesAggregator, ingest.CsvMonthReader, ingest.MonthData,
// model.*, java.io.IOException, java.nio.file.Path, java.time.YearMonth,
// java.util.*, java.util.concurrent.CopyOnWriteArrayList  (no javafx!)

/** MVC model of the report viewer. Confined to one thread (the UI thread). */
public final class SalesModel {
    public static final String ALL = "ALL";

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final List<SalesModelListener> listeners = new CopyOnWriteArrayList<>();
    private MonthData data;            // null until the first successful load
    private String branch = ALL;

    public SalesModel(CsvMonthReader reader, SalesAggregator aggregator) {
        this.reader = reader;
        this.aggregator = aggregator;
    }

    public void addListener(SalesModelListener l) { listeners.add(l); }
    public void removeListener(SalesModelListener l) { listeners.remove(l); }

    /** Replaces the data; on failure the old state is kept. */
    public void load(Path dataDir, YearMonth month) throws IOException {
        
        MonthData loaded = reader.read(dataDir, month);
        data = loaded;

        fire(new SalesEvent.MonthLoaded(
            month, 
            loaded.rows().size(), 
            loaded.rejected().size()));
    }

    public void selectBranch(String newBranch) {
        if(branch.equals(newBranch)){
            return;
        }

        branch = newBranch;
        fire(new SalesEvent.BranchSelected(branch));
    }

    public String branch() { return branch; }

    public Optional<YearMonth> month() {
        return Optional.ofNullable(data).map(MonthData::month);
    }
    public List<SaleTransaction> visibleTransactions() {
        if (data == null) {
            return List.of();
        }

        if (ALL.equals(branch)) {
            return data.rows();
        }

        return data.rows().stream()
                .filter(row -> row.branch().equals(branch))
                .toList();
    }

    public BranchSummary summary() {
        return aggregator.summarize(branch, visibleTransactions());
    }

    public List<String> rejectedRows() {
        return data == null ? List.of() : data.rejected();
    }

    private void fire(SalesEvent event) {
        for (var l : listeners) l.modelChanged(event);
    }
}