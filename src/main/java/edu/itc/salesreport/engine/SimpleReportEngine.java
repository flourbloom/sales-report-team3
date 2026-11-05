package edu.itc.salesreport.engine;

import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.InvalidRowException;
import edu.itc.salesreport.model.MonthlyReport;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.ReportEngine;
import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.model.TransactionParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Sequential engine: reads one branch or all branches of a month. */
public final class SimpleReportEngine implements ReportEngine {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");
    private final TransactionParser parser = new CsvTransactionParser();

    @Override
    public MonthlyReport generate(YearMonth month, Path dataDir)
            throws IOException {
        List<BranchSummary> branches = new ArrayList<>();
        List<SaleTransaction> all = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        for (String b : BRANCHES) {
            List<SaleTransaction> rows = read(month, b, dataDir, anomalies);
            all.addAll(rows);
            branches.add(summarize(b, rows));
        }
        return new MonthlyReport(month, branches, summarize("ALL", all),
                anomalies);
    }

    /** Summary of one branch for one month. */
    public BranchSummary summarizeBranch(YearMonth month, String branch,
            Path dataDir) throws IOException {
        return summarize(branch,
                read(month, branch, dataDir, new ArrayList<>()));
    }

    private List<SaleTransaction> read(YearMonth month, String branch,
            Path dataDir, List<String> anomalies) throws IOException {
        Path dir = dataDir.resolve(month.toString());
        List<SaleTransaction> rows = new ArrayList<>();
        if (!Files.isDirectory(dir)) return rows;
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString()
                    .startsWith(branch + "-")).sorted().toList();
        }
        for (Path f : files) {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) continue;
                try {
                    rows.add(parser.parse(line));
                } catch (InvalidRowException e) {
                    anomalies.add(f.getFileName() + ": " + e.getMessage());
                }
            }
        }
        return rows;
    }

    private static BranchSummary summarize(String branch,
            List<SaleTransaction> rows) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        BigDecimal discounts = BigDecimal.ZERO.setScale(2);
        Set<String> receipts = new HashSet<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        Map<PaymentMethod, BigDecimal> byPayment =
                new EnumMap<>(PaymentMethod.class);
        Map<String, ProductTotal> products = new TreeMap<>();
        for (SaleTransaction t : rows) {
            BigDecimal r = t.revenue();
            revenue = revenue.add(r);
            discounts = discounts.add(t.discount());
            receipts.add(t.receiptNo());
            byCategory.merge(t.category(), r, BigDecimal::add);
            byPayment.merge(t.paymentMethod(), r, BigDecimal::add);
            products.merge(t.sku(), new ProductTotal(t.sku(),
                    t.productName(), r, t.quantity()),
                (a, b) -> new ProductTotal(a.sku(), a.productName(),
                    a.revenue().add(b.revenue()),
                    a.quantity() + b.quantity()));
        }
        List<ProductTotal> top = products.values().stream()
                .sorted(Comparator.comparing(ProductTotal::revenue)
                        .reversed())
                .limit(10).toList();
        return new BranchSummary(branch, revenue, discounts,
                receipts.size(), byCategory, top, byPayment);
    }
}