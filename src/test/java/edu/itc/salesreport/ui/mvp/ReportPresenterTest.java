package edu.itc.salesreport.ui.mvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;

class ReportPresenterTest {

    /** Recording fake: stores whatever the presenter pushes. No JavaFX. */
    static class FakeScreen implements ReportScreen {
        List<RowItem> rows = List.of();
        Map<String, BigDecimal> categories = Map.of();
        String status = "";
        String error = "";

        @Override
        public void showRows(List<RowItem> r) {
            rows = r;
        }

        @Override
        public void showCategoryRevenue(Map<String, BigDecimal> m) {
            categories = m;
        }

        @Override
        public void showStatus(String s) {
            status = s;
        }

        @Override
        public void showError(String m) {
            error = m;
        }
    }

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void septemberPnhShowsFormattedStatusAndRows(@TempDir Path dir) throws IOException {
        TestData.writeSample(dir);
        var model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var screen = new FakeScreen();
        var presenter = new ReportPresenter(model, screen, dir, CLOCK);

        presenter.monthEntered("2026-09");
        presenter.branchChosen("PNH");

        assertEquals("PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00",
                screen.status);
        assertEquals(new RowItem("2026-09-01", "PNH-000001", "Power Bank 10000mAh",
                "Electronics", "1", "18.00"), screen.rows.get(1));
    }

    @Test
    void moneyFormatsWithGroupingAndTwoDecimals() {
        assertEquals("1,234.50", ReportPresenter.money(new BigDecimal("1234.5")));
    }

    private ReportPresenter presenter(Path dir, FakeScreen screen) {
        var model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        return new ReportPresenter(model, screen, dir, CLOCK);
    }

    @Test
    void malformedMonthShowsError(@TempDir Path dir) {
        var screen = new FakeScreen();
        presenter(dir, screen).monthEntered("2026-13");
        assertEquals("'2026-13' is not a month in the form yyyy-MM", screen.error);
    }

    @Test
    void futureMonthShowsError(@TempDir Path dir) {
        var screen = new FakeScreen();
        presenter(dir, screen).monthEntered("2027-01");
        assertEquals("2027-01 is in the future", screen.error);
    }

    @Test
    void monthWithoutDataShowsNoSalesData(@TempDir Path dir) {
        var screen = new FakeScreen();
        presenter(dir, screen).monthEntered("2026-08");
        assertEquals("No sales data for 2026-08", screen.error);
    }

    @Test
    void unknownBranchShowsError(@TempDir Path dir) {
        var screen = new FakeScreen();
        presenter(dir, screen).branchChosen("XYZ");
        assertEquals("Unknown branch 'XYZ': use ALL, PNH, REP or BTB", screen.error);
    }
}