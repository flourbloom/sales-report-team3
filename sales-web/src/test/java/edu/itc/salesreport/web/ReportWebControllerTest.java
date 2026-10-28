package edu.itc.salesreport.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWebControllerTest {

    @Nested
    @WebMvcTest(controllers = ReportWebController.class, properties = "salesreport.data-dir=target/test-data")
    class MockDataTests {

        @Autowired
        private MockMvcTester mvc;

        @BeforeAll
        static void setUpTestData() throws IOException {
            Path dir = Files.createDirectories(Path.of("target/test-data/2026-09"));
            Files.writeString(dir.resolve("PNH-2026-09-01.csv"), """
                branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
                PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR
                """);
            Files.writeString(dir.resolve("REP-2026-09-01.csv"), """
                branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
                REP,2026-09-01,REP-000001,SKU-4003,Sunscreen SPF50,Personal Care,3,7.50,1.50,CASH
                """);
        }

        @Test
        void rendersSummaryOfOneBranch() {
            assertThat(mvc.get().uri("/report?month=2026-09&branch=PNH"))
                    .hasStatusOk()
                    .hasViewName("report")
                    .bodyText().contains("Revenue USD 13.00", "Jasmine Rice 5kg");
        }

        @Test
        void invalidMonthRendersErrorMessage() {
            assertThat(mvc.get().uri("/report?month=2026-13"))
                    .hasStatusOk()
                    .hasViewName("report")
                    .bodyText().contains("'2026-13' is not a month in the form yyyy-MM");
        }

        @Test
        void invalidBranchRendersErrorMessage() {
            assertThat(mvc.get().uri("/report?month=2026-09&branch=XYZ"))
                    .hasStatusOk()
                    .hasViewName("report")
                    .bodyText().contains("Unknown branch 'XYZ': use ALL, PNH, REP or BTB");
        }
    }

    @Nested
    @WebMvcTest(controllers = ReportWebController.class, properties = "salesreport.data-dir=../data")
    class RealDataTests {

        @Autowired
        private MockMvcTester mvc;

        @Test
        void pnhSeptemberMatchesDesktopFigures() {
            assertThat(mvc.get().uri("/report?month=2026-09&branch=PNH"))
                    .hasStatusOk()
                    .hasViewName("report")
                    .bodyText().contains("<h1>Angkor Mart · 2026-09 · PNH</h1>")
                    .contains("Revenue USD 8455.90 · receipts 489 · basket USD 17.29 · rejected rows 0")
                    .contains("<td>Electronics</td><td>3404.50</td>")
                    .contains("<li>Power Bank 10000mAh: USD 2604.70</li>")
                    .contains("<li>Sunscreen SPF50: USD 1064.00</li>");
        }

        @Test
        void allSeptemberMatchesDesktopFigures() {
            assertThat(mvc.get().uri("/report?month=2026-09&branch=ALL"))
                    .hasStatusOk()
                    .hasViewName("report")
                    .bodyText().contains("<h1>Angkor Mart · 2026-09 · ALL</h1>")
                    .contains("Revenue USD 26721.90 · receipts 1493 · basket USD 17.90 · rejected rows 0")
                    .contains("<td>Electronics</td><td>11173.40</td>");
        }
    }
}
