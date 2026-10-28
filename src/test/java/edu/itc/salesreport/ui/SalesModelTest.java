package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SalesModelTest {
    @TempDir
    Path dataDir;

    private final List<SalesEvent> events = new ArrayList<>();
    private SalesModel model;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(dataDir);
        model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
    }

    @Test
    void loadFiresMonthLoadedAndComputesChainRevenue() throws IOException {
        model.addListener(events::add);
        model.load(dataDir, YearMonth.of(2026, 9));

        assertEquals(
                List.of(new SalesEvent.MonthLoaded(YearMonth.of(2026, 9), 3, 1)),
                events);
        assertEquals(new BigDecimal("52.00"), model.summary().revenue());
    }

    @Test
    void selectingPnhTwiceFiresOnceAndGivesRevenueAndBasket() throws IOException {
        model.load(dataDir, YearMonth.of(2026, 9));
        model.addListener(events::add);

        model.selectBranch("PNH");
        model.selectBranch("PNH");

        assertEquals(List.of(new SalesEvent.BranchSelected("PNH")), events);
        assertEquals(new BigDecimal("31.00"), model.summary().revenue());
        assertEquals(new BigDecimal("31.00"), model.summary().averageBasket());
    }

    @Test
    void missingMonthThrowsNoSuchFileExceptionFiresNothingAndKeepsExistingRows() throws IOException {
        model.load(dataDir, YearMonth.of(2026, 9));
        model.addListener(events::add);

        assertThrows(NoSuchFileException.class, () -> model.load(dataDir, YearMonth.of(2026, 7)));

        assertTrue(events.isEmpty());
        assertEquals(3, model.visibleTransactions().size());
    }

    @Test
    void removedListenerIsNotCalled() throws IOException {
        SalesModelListener listener = events::add;
        model.addListener(listener);
        model.removeListener(listener);

        model.load(dataDir, YearMonth.of(2026, 9));
        model.selectBranch("PNH");

        assertTrue(events.isEmpty());
    }
}
