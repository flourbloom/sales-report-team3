package edu.itc.salesreport.engine;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.SaleTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class DefaultSalesAggregator implements SalesAggregator {
    private record ProductKey(String sku, String name) {}

    private static final class ProductAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        private long quantity;

        void add(SaleTransaction transaction) {
            revenue = revenue.add(transaction.revenue());
            quantity += transaction.quantity();
        }
    }

    @Override
    public BranchSummary summarize(
            String branch,
            List<SaleTransaction> rows) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        BigDecimal discounts = BigDecimal.ZERO.setScale(2);
        Set<String> receipts = new HashSet<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        Map<PaymentMethod, BigDecimal> byPayment =
                new EnumMap<>(PaymentMethod.class);
        Map<ProductKey, ProductAccumulator> products = new HashMap<>();

        for (SaleTransaction transaction : rows) {
            revenue = revenue.add(transaction.revenue());
            discounts = discounts.add(transaction.discount());
            receipts.add(transaction.receiptNo());

            byCategory.merge(
                    transaction.category(),
                    transaction.revenue(),
                    BigDecimal::add);
            byPayment.merge(
                    transaction.paymentMethod(),
                    transaction.revenue(),
                    BigDecimal::add);

            products.computeIfAbsent(
                    new ProductKey(
                            transaction.sku(),
                            transaction.productName()),
                    ignored -> new ProductAccumulator())
                    .add(transaction);
        }

        List<ProductTotal> topProducts = products.entrySet().stream()
                .map(entry -> new ProductTotal(
                        entry.getKey().sku(),
                        entry.getKey().name(),
                        entry.getValue().revenue,
                        entry.getValue().quantity))
                .sorted(Comparator
                        .comparing(ProductTotal::revenue).reversed()
                        .thenComparing(ProductTotal::sku))
                .limit(10)
                .toList();

        return new BranchSummary(
                branch,
                revenue,
                discounts,
                receipts.size(),
                byCategory,
                new ArrayList<>(topProducts),
                byPayment);
    }
}
