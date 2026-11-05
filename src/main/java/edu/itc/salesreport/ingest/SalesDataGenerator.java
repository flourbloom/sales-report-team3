package edu.itc.salesreport.ingest;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Seeded generator of canonical branch CSV files (small sample version). */
public final class SalesDataGenerator {

    private record Product(String sku, String name, String category,
            String price) { }

    private static final List<Product> PRODUCTS = List.of(
        new Product("SKU-1001", "Jasmine Rice 5kg", "Grocery", "6.50"),
        new Product("SKU-1002", "Fish Sauce 700ml", "Grocery", "1.80"),
        new Product("SKU-2001", "Angkor Beer 330ml", "Beverages", "0.90"),
        new Product("SKU-2002", "Iced Coffee 250ml", "Beverages", "1.20"),
        new Product("SKU-3001", "Laundry Powder 1kg", "Household", "3.40"),
        new Product("SKU-4001", "Shampoo 400ml", "Personal Care", "4.10"),
        new Product("SKU-5001", "USB-C Cable 1m", "Electronics", "5.00"),
        new Product("SKU-6001", "Notebook A5", "Stationery", "0.75"));
    private static final String[] BRANCHES = { "PNH", "REP", "BTB" };
    private static final String[] PAYMENTS = { "CASH", "CARD", "KHQR" };
    private static final String HEADER = "branch,date,receipt_no,sku,"
        + "product_name,category,quantity,unit_price,discount,payment_method";

    /** Arguments: month rowsPerDay seed dataDir, e.g. 2026-08 5000 42 data */
    public static void main(String[] args) throws IOException {
        new SalesDataGenerator().generate(YearMonth.parse(args[0]),
                Integer.parseInt(args[1]), Long.parseLong(args[2]),
                Path.of(args[3]));
    }

    public void generate(YearMonth month, int rowsPerDay, long seed,
            Path dataDir) throws IOException {
        Random rnd = new Random(seed);
        Path dir = dataDir.resolve(month.toString());
        Files.createDirectories(dir);
        for (String branch : BRANCHES) {
            int receipt = 0;
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                LocalDate date = month.atDay(d);
                Path file = dir.resolve(branch + "-" + date + ".csv");
                try (var out = new PrintWriter(Files.newBufferedWriter(
                        file, StandardCharsets.UTF_8))) {
                    out.println(HEADER);
                    for (int r = 0; r < rowsPerDay; r++) {
                        if (r % 3 == 0) receipt++;
                        Product p = PRODUCTS.get(rnd.nextInt(PRODUCTS.size()));
                        int qty = 1 + rnd.nextInt(4);
                        String discount =
                                rnd.nextInt(10) == 0 ? "0.50" : "0.00";
                        out.println(String.format(Locale.ROOT,
                            "%s,%s,%s-%06d,%s,%s,%s,%d,%s,%s,%s", branch, date,
                            branch, receipt, p.sku(), p.name(), p.category(),
                            qty, p.price(), discount,
                            PAYMENTS[rnd.nextInt(PAYMENTS.length)]));
                    }
                }
            }
        }
    }
}