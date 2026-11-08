package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.engine.SimpleReportEngine;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportViewModelTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T00:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));

    private SimpleReportEngine engine;
    private SummarySource source;
    private ReportViewModel vm;

    @BeforeEach
    void setUp() {
        engine = new SimpleReportEngine();
        source = (month, branch) -> engine.summarizeBranch(month, branch, Path.of("data"));
        vm = new ReportViewModel(source, new CallerThreadJobs(), FIXED_CLOCK);
    }

    @Test
    void testInitialState() {
        assertEquals(12, vm.monthChoices().size());
        assertEquals(YearMonth.of(2026, 9), vm.monthChoices().get(0));
        assertTrue(vm.branchSelectedProperty("PNH").get());
        assertTrue(vm.branchSelectedProperty("REP").get());
        assertTrue(vm.branchSelectedProperty("BTB").get());
        assertEquals("Choose a month.", vm.validationMessageProperty().getValue());
        assertFalse(vm.canGenerateProperty().get());
        assertEquals("USD 0.00", vm.totalRevenueTextProperty().getValue());
    }

    @Test
    void testRow1_emptyMonthWithBranches() {
        vm.monthProperty().set(null);
        vm.branchSelectedProperty("PNH").set(true);
        vm.branchSelectedProperty("REP").set(true);
        vm.branchSelectedProperty("BTB").set(true);

        assertEquals("Choose a month.", vm.validationMessageProperty().getValue());
        assertFalse(vm.canGenerateProperty().get());
    }

    @Test
    void testRow2_futureMonthWithBranches() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        vm.branchSelectedProperty("PNH").set(true);
        vm.branchSelectedProperty("REP").set(true);
        vm.branchSelectedProperty("BTB").set(true);

        assertEquals("Month 2026-10 is in the future.", vm.monthErrorProperty().getValue());
        assertEquals("", vm.branchErrorProperty().getValue());
        assertEquals("Month 2026-10 is in the future.", vm.validationMessageProperty().getValue());
        assertFalse(vm.canGenerateProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void testRow3_validMonthWithNoBranches() {
        vm.monthProperty().set(YearMonth.of(2026, 8));
        vm.branchSelectedProperty("PNH").set(false);
        vm.branchSelectedProperty("REP").set(false);
        vm.branchSelectedProperty("BTB").set(false);

        assertEquals("Select at least one branch.", vm.validationMessageProperty().getValue());
        assertFalse(vm.canGenerateProperty().get());
    }

    @Test
    void testRow4_emptyMonthWithNoBranches() {
        vm.monthProperty().set(null);
        vm.branchSelectedProperty("PNH").set(false);
        vm.branchSelectedProperty("REP").set(false);
        vm.branchSelectedProperty("BTB").set(false);

        assertEquals("Choose a month. Select at least one branch.", vm.validationMessageProperty().getValue());
        assertFalse(vm.canGenerateProperty().get());
    }

    @Test
    void testRow5_validMonthAndBranchEnabled() {
        vm.monthProperty().set(YearMonth.of(2026, 9));
        vm.branchSelectedProperty("PNH").set(true);
        vm.branchSelectedProperty("REP").set(false);
        vm.branchSelectedProperty("BTB").set(false);

        assertEquals("", vm.validationMessageProperty().getValue());
        assertTrue(vm.canGenerateProperty().get());
    }

    @Test
    void testTotalRevenueAndFormatting() {
        BranchSummary s1 = new BranchSummary("PNH", new BigDecimal("1000.50"), BigDecimal.ZERO, 10,
                Map.of(), List.of(), Map.of());
        BranchSummary s2 = new BranchSummary("REP", new BigDecimal("2000.25"), BigDecimal.ZERO, 15,
                Map.of(), List.of(), Map.of());

        SummarySource fakeSource = (m, b) -> "PNH".equals(b) ? s1 : s2;
        var testVm = new ReportViewModel(fakeSource, new CallerThreadJobs(), FIXED_CLOCK);
        testVm.monthProperty().set(YearMonth.of(2026, 9));
        testVm.branchSelectedProperty("BTB").set(false);

        testVm.generateCommand().execute();

        assertEquals(new BigDecimal("3000.75"), testVm.totalRevenueProperty().get());
        assertEquals("USD 3,000.75", testVm.totalRevenueTextProperty().getValue());
    }

    @Test
    void testProductFilterFiltersVisibleProductsCaseInsensitive() {
        ProductTotal p1 = new ProductTotal("SKU-1001", "Jasmine Rice 5kg", new BigDecimal("100.00"), 10);
        ProductTotal p2 = new ProductTotal("SKU-2001", "Angkor Beer 330ml", new BigDecimal("50.00"), 20);
        BranchSummary summary = new BranchSummary("PNH", new BigDecimal("150.00"), BigDecimal.ZERO, 5,
                Map.of(), List.of(p1, p2), Map.of());

        SummarySource fakeSource = (m, b) -> summary;
        var testVm = new ReportViewModel(fakeSource, new CallerThreadJobs(), FIXED_CLOCK);
        testVm.monthProperty().set(YearMonth.of(2026, 9));
        testVm.branchSelectedProperty("REP").set(false);
        testVm.branchSelectedProperty("BTB").set(false);

        testVm.generateCommand().execute();

        assertEquals(2, testVm.visibleProducts().size());

        testVm.productFilterProperty().set("jasmine");
        assertEquals(1, testVm.visibleProducts().size());
        assertEquals("SKU-1001", testVm.visibleProducts().get(0).sku());

        testVm.productFilterProperty().set("SKU-2001");
        assertEquals(1, testVm.visibleProducts().size());
        assertEquals("Angkor Beer 330ml", testVm.visibleProducts().get(0).productName());

        testVm.productFilterProperty().set("unknown");
        assertEquals(0, testVm.visibleProducts().size());

        testVm.productFilterProperty().set("");
        assertEquals(2, testVm.visibleProducts().size());
    }

    @Test
    void testForcedLoadErrorKeepsPreviousDataAndSetsStatusAndLoadError() {
        vm.monthProperty().set(YearMonth.of(2026, 8));
        vm.generateCommand().execute();

        assertEquals(3, vm.summaries().size());
        assertEquals("Loaded 3 branch(es) for 2026-08.", vm.statusProperty().get());

        // Now wrap source to throw an IOException for REP
        AtomicBoolean shouldFail = new AtomicBoolean(true);
        SummarySource failingSource = (month, branch) -> {
            if (shouldFail.get() && "REP".equals(branch)) {
                throw new IOException("REP-2026-08-03.csv: unreadable");
            }
            return engine.summarizeBranch(month, branch, Path.of("data"));
        };

        var failingVm = new ReportViewModel(failingSource, new CallerThreadJobs(), FIXED_CLOCK);
        failingVm.monthProperty().set(YearMonth.of(2026, 8));

        // Initial successful load with failing disabled
        shouldFail.set(false);
        failingVm.generateCommand().execute();
        assertEquals(3, failingVm.summaries().size());

        // Now trigger forced load error
        shouldFail.set(true);
        failingVm.generateCommand().execute();

        // Previous summaries kept!
        assertEquals(3, failingVm.summaries().size());
        assertEquals("REP-2026-08-03.csv: unreadable", failingVm.loadErrorProperty().get());
        assertEquals("Load failed, previous data kept.", failingVm.statusProperty().get());
        assertFalse(failingVm.loadingProperty().get());
    }

    @Test
    void testMonthRuleUsesInjectedClock() {
        Clock customClock = Clock.fixed(
                Instant.parse("2025-05-15T00:00:00Z"),
                ZoneId.of("Asia/Phnom_Penh"));
        var testVm = new ReportViewModel(source, new CallerThreadJobs(), customClock);

        testVm.monthProperty().set(YearMonth.of(2025, 5));
        assertEquals("", testVm.monthErrorProperty().getValue());

        testVm.monthProperty().set(YearMonth.of(2025, 6));
        assertEquals("Month 2025-06 is in the future.", testVm.monthErrorProperty().getValue());
    }
}
