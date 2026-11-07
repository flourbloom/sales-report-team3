package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/** Task 1 skeleton: state, commands and ports. javafx.base only. */
public final class ReportViewModel {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private final SummarySource source;
    private final BackgroundJobs jobs;
    private final Clock clock;

    // ---- input state: two-way bound by the view
    private final ObjectProperty<YearMonth> month =
            new SimpleObjectProperty<>(this, "month");
    private final Map<String, BooleanProperty> branchSelected =
            new LinkedHashMap<>();
    private final StringProperty productFilter =
            new SimpleStringProperty(this, "productFilter", "");

    // ---- output state: one-way bound by the view
    private final ObservableList<YearMonth> monthChoices =
            FXCollections.observableArrayList();
    private final ObservableList<BranchSummary> summaries =
            FXCollections.observableArrayList();
    private final ObservableList<ProductTotal> products =
            FXCollections.observableArrayList();
    private final FilteredList<ProductTotal> visibleProducts =
            new FilteredList<>(products);
    private final ReadOnlyBooleanWrapper loading =
            new ReadOnlyBooleanWrapper(this, "loading", false);
    private final ReadOnlyDoubleWrapper progress =
            new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper(this, "status", "Choose a month.");
    private final ReadOnlyStringWrapper loadError =
            new ReadOnlyStringWrapper(this, "loadError", "");

    // ---- computed state (Task 3 replaces the placeholders)
    private final StringBinding monthError;
    private final StringBinding branchError;
    private final StringBinding validationMessage;
    private final BooleanBinding canGenerate;
    private final ObjectBinding<BigDecimal> totalRevenue;
    private final StringBinding totalRevenueText;

    private final Command generateCommand;
    private final Command cancelCommand;
    private BackgroundJobs.JobHandle running;

    public ReportViewModel(SummarySource source, BackgroundJobs jobs,
                           Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.jobs = Objects.requireNonNull(jobs);
        this.clock = Objects.requireNonNull(clock);
        YearMonth now = YearMonth.now(clock);
        for (int i = 0; i < 12; i++) {
            monthChoices.add(now.minusMonths(i));
        }
        for (String b : BRANCHES) {
            branchSelected.put(b, new SimpleBooleanProperty(this, b, true));
        }
        // TODO Task 3: real rules (future month, empty branch selection)
        monthError = Bindings.createStringBinding(() -> "", month);
        branchError = Bindings.createStringBinding(() -> "");
        validationMessage = Bindings.createStringBinding(() -> "");
        canGenerate = month.isNotNull().and(loading.not());

        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setGroupingUsed(true);

        totalRevenue = Bindings.createObjectBinding(() -> {
            BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            for (BranchSummary s : summaries) {
                if (s != null && s.revenue() != null) {
                    sum = sum.add(s.revenue());
                }
            }
            return sum;
        }, summaries);

        totalRevenueText = Bindings.createStringBinding(
                () -> "USD " + currencyFormat.format(totalRevenue.get()),
                totalRevenue);

        visibleProducts.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            String filter = productFilter.get();
            if (filter == null || filter.isBlank()) {
                return p -> true;
            }
            String term = filter.strip().toLowerCase(Locale.ROOT);
            return p -> (p.productName() != null && p.productName().toLowerCase(Locale.ROOT).contains(term))
                    || (p.sku() != null && p.sku().toLowerCase(Locale.ROOT).contains(term));
        }, productFilter));

        generateCommand = new DelegateCommand(this::generate, canGenerate);
        cancelCommand = new DelegateCommand(this::cancel,
                loading.getReadOnlyProperty());
    }

    public List<String> selectedBranches() {
        return branchSelected.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void generate() {
        YearMonth m = month.get();
        List<String> branches = selectedBranches();
        if (m == null || branches.isEmpty()) {
            return;
        }
        loading.set(true);
        loadError.set("");
        status.set("Loading...");
        progress.set(0);

        record LoadResult(List<BranchSummary> summaries, List<ProductTotal> products) {}

        running = jobs.submit(sink -> {
            List<BranchSummary> loadedSummaries = new ArrayList<>();
            Map<String, ProductTotal> mergedProducts = new TreeMap<>();
            long total = branches.size();
            long count = 0;
            for (String b : branches) {
                if (sink.isCancelled()) {
                    break;
                }
                BranchSummary s = source.load(m, b);
                loadedSummaries.add(s);
                if (s.topProducts() != null) {
                    for (ProductTotal pt : s.topProducts()) {
                        mergedProducts.merge(pt.sku(), pt, (p1, p2) -> new ProductTotal(
                                p1.sku(),
                                p1.productName(),
                                p1.revenue().add(p2.revenue()),
                                p1.quantity() + p2.quantity()));
                    }
                }
                count++;
                sink.update(count, total);
            }
            List<ProductTotal> top = mergedProducts.values().stream()
                    .sorted(Comparator.comparing(ProductTotal::revenue).reversed())
                    .limit(10)
                    .toList();
            return new LoadResult(loadedSummaries, top);
        }, new BackgroundJobs.JobListener<LoadResult>() {
            @Override
            public void progress(double fraction) {
                progress.set(fraction);
            }

            @Override
            public void succeeded(LoadResult value) {
                summaries.setAll(value.summaries());
                products.setAll(value.products());
                status.set("Loaded %d branch(es) for %s.".formatted(value.summaries().size(), m));
                loading.set(false);
            }

            @Override
            public void failed(Throwable error) {
                loadError.set("Load failed: " + error.getMessage());
                status.set("Load failed: " + error.getMessage());
                loading.set(false);
            }

            @Override
            public void cancelled() {
                status.set("Cancelled.");
                loading.set(false);
            }
        });
    }

    private void cancel() {
        if (running != null) {
            running.cancel();
        }
    }

    // ---- accessors: read-only types for everything the view must not set
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public BooleanProperty branchSelectedProperty(String branch) {
        return Objects.requireNonNull(branchSelected.get(branch), branch);
    }
    public StringProperty productFilterProperty() { return productFilter; }
    public ObservableList<YearMonth> monthChoices() {
        return FXCollections.unmodifiableObservableList(monthChoices);
    }
    public ObservableList<BranchSummary> summaries() {
        return FXCollections.unmodifiableObservableList(summaries);
    }
    public ObservableList<ProductTotal> visibleProducts() {
        return visibleProducts;
    }
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty loadErrorProperty() {
        return loadError.getReadOnlyProperty();
    }
    public StringExpression monthErrorProperty() { return monthError; }
    public StringExpression branchErrorProperty() { return branchError; }
    public StringExpression validationMessageProperty() {
        return validationMessage;
    }
    public ObservableBooleanValue canGenerateProperty() { return canGenerate; }
    public ObservableObjectValue<BigDecimal> totalRevenueProperty() {
        return totalRevenue;
    }
    public StringExpression totalRevenueTextProperty() {
        return totalRevenueText;
    }

    
    public Command generateCommand() { return generateCommand; }
    public Command cancelCommand() { return cancelCommand; }
}