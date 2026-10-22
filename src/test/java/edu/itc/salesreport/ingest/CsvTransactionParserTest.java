package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure parser tests: no files, no UI toolkit. */
class CsvTransactionParserTest {
    private final CsvTransactionParser parser = new CsvTransactionParser();

    @Test
    void canonicalRowGivesRevenue13AndKhqr() throws InvalidRowException {
        SaleTransaction t = parser.parse(
                "PNH,2026-09-01,PNH-000123,SKU-1001,Jasmine Rice 5kg,"
                + "Grocery,2,6.50,0.00,KHQR");

        assertEquals("PNH", t.branch());
        assertEquals(LocalDate.of(2026, 9, 1), t.date());
        assertEquals(new BigDecimal("13.00"), t.revenue());
        assertEquals(PaymentMethod.KHQR, t.paymentMethod());
    }

    @ParameterizedTest(name = "[{index}] rejected: {0}")
    @ValueSource(strings = {
        // unknown branch
        "XXX,2026-09-01,XXX-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR",
        // bad date (month 13)
        "PNH,2026-13-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR",
        // quantity 0
        "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,0,6.50,0.00,KHQR",
        // discount above quantity x unit price (7.00 > 6.50)
        "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,1,6.50,7.00,KHQR",
        // unknown category
        "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Toys,2,6.50,0.00,KHQR",
        // unknown payment method
        "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,BITCOIN",
        // missing fields (3 instead of 10)
        "PNH,2026-09-01,PNH-000001"
    })
    void invalidRowIsRejected(String line) {
        assertThrows(InvalidRowException.class, () -> parser.parse(line));
    }
}
