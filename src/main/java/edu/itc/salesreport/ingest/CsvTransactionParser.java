package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.InvalidRowException;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.model.TransactionParser;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Parses one data row of the canonical 10-column branch CSV. */
public final class CsvTransactionParser implements TransactionParser {

    @Override
    public SaleTransaction parse(String line) throws InvalidRowException {
        String[] f = line.split(",", -1);
        if (f.length != 10) {
            throw new InvalidRowException("expected 10 fields: " + line);
        }
        try {
            return new SaleTransaction(f[0], LocalDate.parse(f[1]), f[2],
                    f[3], f[4], f[5], Integer.parseInt(f[6]),
                    new BigDecimal(f[7]), new BigDecimal(f[8]),
                    PaymentMethod.valueOf(f[9]));
        } catch (RuntimeException e) {
            throw new InvalidRowException(e.getMessage() + ": " + line);
        }
    }
}