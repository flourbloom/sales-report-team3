package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record BranchSummary(String branch, BigDecimal revenue,
        BigDecimal discounts, long receipts,
        Map<String, BigDecimal> revenueByCategory,
        List<ProductTotal> topProducts,
        Map<PaymentMethod, BigDecimal> revenueByPayment) {

    /** Revenue per receipt, scale 2; zero when there are no receipts. */
    public BigDecimal averageBasket() {
        return receipts == 0 ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(receipts), 2,
                        RoundingMode.HALF_UP);
    }
}