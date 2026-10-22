# Lab 02, Task 4: refactoring the fat controller

`FatReportController.onLoad()` (legacy) does everything in one click handler: it reads files,
parses text, filters, calculates and writes to a label. This note lists what is wrong with it,
shows what it displays on the broken-row data, and records how the MVC structure from Tasks 1 to 3 fixes it.

## 1. Smells and their consequences

| # | Smell (where in the legacy code) | Consequence |
|---|----------------------------------|-------------|
| 1 | **God handler**: file I/O, parsing, filtering, arithmetic and display in one 20-line method | Nothing can be tested or reused on its own. A web or console view would have to copy all of it. |
| 2 | **Business calculation in the controller**: `unitPrice * quantity - discount` is re-implemented inline | A second copy of the revenue rule next to `SaleTransaction.revenue()`. The copies can drift apart, and the lab deducts 5 points per calculation found in a controller. |
| 3 | **No validation**: the canonical rules (quantity > 0, no negative amount, discount ≤ line amount, known branch and category) are not applied | A row with quantity -1 is silently *subtracted*. Revenue is wrong and nobody is told (see section 2). |
| 4 | **Swallowed exceptions**: `catch (Exception e)` shows only "Error" | A typo in the month, a missing folder, an unreadable file and a bug all look the same. The user cannot fix anything and the developer gets no stack trace. |
| 5 | **Raw text used as a path**: `Path.of("data", monthField.getText())` with no check, and `"data"` hard-coded | Input such as `../x` or a blank field reaches the file system. The data directory cannot be replaced by a temp directory in a test. |
| 6 | **Direct widget access**: reads `monthField` and `branchBox`, writes `status` | The class cannot be constructed without JavaFX, so zero tests are possible without a display. `branchBox.getValue()` can also be `null` and cause a `NullPointerException`. |
| 7 | **Fragile parsing**: `line.split(",")` (drops trailing empty fields), `subList(1, size)` on every file, every file in the folder is read | A short or empty file throws and ends in "Error". Files that are not branch CSVs (for example a notes file) are parsed as data. Order is not deterministic. |
| 8 | **Resource leak**: `Files.list(dir)` is never closed | The directory handle stays open until garbage collection. This matters when the user reloads many times. |
| 9 | **Incomplete figures**: only a revenue total, no rejected count, no receipts, no per-category figures | The status line cannot show "1 rejected", which the viewer must do. |

## 2. What each version shows

Data: `2026-09`, branch ALL, with the broken row
`PNH,2026-09-15,PNH-999999,SKU-1001,Jasmine Rice 5kg,Grocery,-1,6.50,0.00,KHQR`
appended to `PNH-2026-09-15.csv` (line 42).

| Situation | Fat controller | After refactoring |
|-----------|----------------|-------------------|
| 2026-09 ALL, broken row present | `Revenue: 26715.40` (wrong, no warning) | `2026-09: 3600 rows, 1 rejected · revenue USD 26721.90 · receipts 1493` |
| Month `2026/09` | `Error` | `'2026/09' is not a month in the form yyyy-MM` |
| Month blank | `Error` (the folder `data` is read as a file) | `Enter a month as yyyy-MM, e.g. 2026-09` |
| Month `2026-07` (no data) | `Error` | `No sales data found in data/2026-07` (`data\2026-07` on Windows) |
| Month `2099-01` | `Error` | `2099-01 is in the future` |
| Rejected row reported | no | yes: `PNH-2026-09-15.csv:42 quantity must be positive: -1` |
| Lines in the handler `onLoad()` | 20 | 1 |
| Lines in the class file | 43 | 19 |
| Tests possible without JavaFX | 0 | all rules (26 tests, no toolkit started) |

**Why 26715.40?** The broken row contributes `6.50 × (-1) - 0.00 = -6.50`, so
`26721.90 - 6.50 = 26715.40`. The canonical parser rejects the row (quantity must be positive), so
the model sums only valid rows and keeps `26721.90`.

## 3. The refactoring

`onLoad()` now only forwards the raw text, and every decision happens in the toolkit-free core:

```java
public void onLoad() {
    controller.monthEntered(monthField.getText());
}
```

| Concern | Before (inside `onLoad`) | After |
|---------|--------------------------|-------|
| Input validation | none | `InputRules.month` |
| Command | none | `ReportCommand.LoadMonth` |
| File reading and row validation | inline | `CsvMonthReader` + `CsvTransactionParser` |
| Revenue and totals | inline | `SaleTransaction.revenue()`, `DefaultSalesAggregator` |
| Error messages | `"Error"` | `ReportController` through `ErrorDisplay` |
| Display | writes a label directly | `ReportView.modelChanged` |

The branch is no longer read from the combo box in the handler: `ReportController.branchChosen`
and `SalesModel.selectBranch` own it. The old `branchBox` and `status` constructor parameters
were therefore removed, and the constructor now takes the `ReportController`.

## 4. Evidence

- `mvn test` runs without a display: 26 tests (8 parser + 9 input rules + 4 controller + 4 model + 1 architecture).
- `UiIndependenceTest` was seen failing once after adding `import javafx.scene.control.Label;`
  to a class in `ui` (the failure listed the file and line), then the import was removed.
  *(Fill in your own date and commit hash after you try it.)*

## Appendix: the original `FatReportController` (before)

```java
package edu.itc.salesreport.ui.fx.legacy;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Smell on purpose: I/O, parsing, filtering and totals in a handler. */
public class FatReportController {
    private final TextField monthField;
    private final ComboBox<String> branchBox;
    private final Label status;

    public FatReportController(TextField monthField,
            ComboBox<String> branchBox, Label status) {
        this.monthField = monthField;
        this.branchBox = branchBox;
        this.status = status;
    }

    public void onLoad() {
        try {
            var dir = Path.of("data", monthField.getText());
            BigDecimal revenue = BigDecimal.ZERO;
            for (Path f : Files.list(dir).toList()) {
                var lines = Files.readAllLines(f);
                for (String line : lines.subList(1, lines.size())) {
                    String[] c = line.split(",");
                    if (!branchBox.getValue().equals("ALL")
                            && !c[0].equals(branchBox.getValue())) continue;
                    revenue = revenue.add(new BigDecimal(c[7])
                            .multiply(new BigDecimal(c[6]))
                            .subtract(new BigDecimal(c[8])));
                }
            }
            status.setText("Revenue: " + revenue);
        } catch (Exception e) {
            status.setText("Error");
        }
    }
}
```
