package edu.itc.salesreport.ui;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;

/** Plain JUnit: no JavaFX toolkit, no window, no robot, no sleep, no files. */
class ReportViewModelTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);

    ManualJobs jobs = new ManualJobs();
    /** "month:branch" for every call the (fake) worker made to the source. */
    List<String> loaded = new ArrayList<>();
    /** Value of vm.progress seen by the worker before each branch is loaded. */
    List<Double> progressSeen = new ArrayList<>();
    boolean failNext;
    ReportViewModel vm;

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> {
            if (failNext) {
                throw new IOException(branch + "-2026-08-03.csv: unreadable");
            }
            loaded.add(month + ":" + branch);
            progressSeen.add(vm.progressProperty().get());
            return summary(branch, "200.00");
        };
        vm = new ReportViewModel(source, jobs, SEP_27);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    /** Valid input, one generate, and the queued job run to completion. */
    private void loadAugust() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
    }

    // ---- validation (3)

    @Test
    void cannotGenerateBeforeAMonthIsChosen() {
        assertEquals("Choose a month.", vm.validationMessageProperty().get());
        assertFalse(vm.canGenerateProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void futureMonthIsRejected() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void emptyBranchSelectionIsRejected() {
        vm.monthProperty().set(AUG);
        for (String b : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(b).set(false);
        }
        assertEquals("Select at least one branch.",
                vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());

        vm.monthProperty().set(null);
        assertEquals("Choose a month. Select at least one branch.",
                vm.validationMessageProperty().get());
    }

    @Test
    void monthRuleUsesTheInjectedClock() {
        vm.monthProperty().set(YearMonth.of(2026, 9));
        assertEquals("", vm.monthErrorProperty().get());
        vm.monthProperty().set(YearMonth.of(2026, 10));
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertEquals(YearMonth.of(2026, 9), vm.monthChoices().get(0));
        assertEquals(12, vm.monthChoices().size());
    }

    // ---- command enablement (2)

    @Test
    void generateStartsLoadingAndDisablesItself() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        assertEquals("Loading...", vm.statusProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        assertTrue(vm.cancelCommand().isExecutable());
        assertEquals(1, jobs.pendingCount());
    }

    @Test
    void generateIsIgnoredWhileInvalidOrAlreadyLoading() {
        vm.generateCommand().execute();               // no month yet
        assertEquals(0, jobs.pendingCount());

        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        vm.generateCommand().execute();               // double click
        assertEquals(1, jobs.pendingCount());

        assertTrue(vm.cancelCommand().isExecutable());
    }

    @Test
    void cancelIsOnlyEnabledWhileALoadRuns() {
        assertFalse(vm.cancelCommand().isExecutable());
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.cancelCommand().isExecutable());
        jobs.runNext();
        assertFalse(vm.cancelCommand().isExecutable());
    }

    // ---- success, totals, progress

    @Test
    void successfulLoadFillsSummariesAndTotal() {
        loadAugust();
        assertEquals(3, vm.summaries().size());
        assertEquals(new BigDecimal("600.00"), vm.totalRevenueProperty().get());
        assertEquals("USD 600.00", vm.totalRevenueTextProperty().get());
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
        assertEquals("", vm.loadErrorProperty().get());
        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void totalIsFormattedWithThousandsSeparator() {
        SummarySource big = (m, b) -> summary(b, "4115.20");
        var bigVm = new ReportViewModel(big, jobs, SEP_27);
        bigVm.monthProperty().set(AUG);
        bigVm.generateCommand().execute();
        jobs.runNext();
        assertEquals("USD 12,345.60", bigVm.totalRevenueTextProperty().get());
    }

    @Test
    void progressFollowsTheBranches() {
        loadAugust();
        // before branch 1, 2 and 3 the worker saw 0, 1/3 and 2/3
        assertEquals(3, progressSeen.size());
        assertEquals(0.0, progressSeen.get(0), 1e-9);
        assertEquals(1.0 / 3, progressSeen.get(1), 1e-9);
        assertEquals(2.0 / 3, progressSeen.get(2), 1e-9);
        assertEquals(1.0, vm.progressProperty().get(), 1e-9);
    }

    // ---- failure and cancellation keep the previous data

    @Test
    void failureKeepsPreviousDataAndShowsTheError() {
        loadAugust();
        assertEquals(3, vm.summaries().size());

        failNext = true;
        vm.generateCommand().execute();
        jobs.runNext();

        assertEquals(3, vm.summaries().size());
        assertEquals("PNH-2026-08-03.csv: unreadable",
                vm.loadErrorProperty().get());
        assertEquals("Load failed, previous data kept.",
                vm.statusProperty().get());
        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void newLoadClearsThePreviousError() {
        loadAugust();
        failNext = true;
        vm.generateCommand().execute();
        jobs.runNext();
        assertFalse(vm.loadErrorProperty().get().isEmpty());

        failNext = false;
        vm.generateCommand().execute();
        assertEquals("", vm.loadErrorProperty().get());
        jobs.runNext();
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
    }

    @Test
    void cancelReenablesGenerateAndKeepsOldData() {
        loadAugust();

        vm.generateCommand().execute();
        vm.cancelCommand().execute();

        assertEquals(0, jobs.pendingCount());
        assertEquals(3, vm.summaries().size());
        assertEquals("Cancelled, previous data kept.",
                vm.statusProperty().get());
        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
        assertFalse(vm.cancelCommand().isExecutable());
    }

    // ---- snapshot and product filter

    @Test
    void workerUsesASnapshotOfTheInputs() {
        vm.monthProperty().set(AUG);
        vm.branchSelectedProperty("REP").set(false);
        vm.generateCommand().execute();

        // the user keeps editing while the job is queued
        vm.monthProperty().set(YearMonth.of(2026, 7));
        vm.branchSelectedProperty("REP").set(true);
        vm.branchSelectedProperty("BTB").set(false);

        jobs.runNext();

        assertEquals(List.of("2026-08:PNH", "2026-08:BTB"), loaded);
        assertEquals("Loaded 2 branch(es) for 2026-08.",
                vm.statusProperty().get());
    }

    @Test
    void productFilterIsCaseInsensitiveOnNameAndSku() {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal("100.00"), 10);
        var beer = new ProductTotal("SKU-2001", "Angkor Beer 330ml",
                new BigDecimal("50.00"), 20);
        var one = new BranchSummary("PNH", new BigDecimal("150.00"),
                BigDecimal.ZERO, 5, Map.of(), List.of(rice, beer), Map.of());
        var filterVm = new ReportViewModel((m, b) -> one, jobs, SEP_27);
        filterVm.monthProperty().set(AUG);
        filterVm.branchSelectedProperty("REP").set(false);
        filterVm.branchSelectedProperty("BTB").set(false);
        filterVm.generateCommand().execute();
        jobs.runNext();

        assertEquals(2, filterVm.visibleProducts().size());

        filterVm.productFilterProperty().set("JASMINE");
        assertEquals(1, filterVm.visibleProducts().size());
        assertEquals("SKU-1001", filterVm.visibleProducts().get(0).sku());

        filterVm.productFilterProperty().set("sku-2001");
        assertEquals(1, filterVm.visibleProducts().size());
        assertEquals("Angkor Beer 330ml",
                filterVm.visibleProducts().get(0).productName());

        filterVm.productFilterProperty().set("unknown");
        assertEquals(0, filterVm.visibleProducts().size());

        filterVm.productFilterProperty().set("");
        assertEquals(2, filterVm.visibleProducts().size());
    }
}