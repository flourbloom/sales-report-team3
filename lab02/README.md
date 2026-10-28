# Lab-02: Model View Presenter

Sales report screen rebuilt as MVP with a passive view, compared with the Lab-01 MVC version. Tag: `lab-02`.

## Files in this lab

| Path                                                                  | Task                                        | Author           |
| --------------------------------------------------------------------- | ------------------------------------------- | ---------------- |
| `src/main/java/edu/itc/salesreport/ui/mvp/RowItem.java`             | Step 1: row record for the view             | Pi Sereyvathanak |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportScreen.java`        | Step 1: passive-view interface              | Pi Sereyvathanak |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java`     | Step 1: presenter and money formatting      | Pi Sereyvathanak |
| `src/main/java/edu/itc/salesreport/ui/fx/FxReportScreen.java`       | Step 2: JavaFX passive view                 | Chea Seavhong    |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportMvpApp.java`         | Step 2: launcher                            | Chea Seavhong    |
| `src/test/java/edu/itc/salesreport/ui/mvp/ReportPresenterTest.java` | Step 3: presenter tests with `FakeScreen` | Pi Sereyvathanak |
| `src/test/java/edu/itc/salesreport/UiIndependenceTest.java`         | Step 3: `ui/mvp` added                    | Pav Ratana       |
| `lab02/mvc-vs-mvp.md`                                               | Step 4: measurements and comparison         | Pi Sereyvathanak |
| `lab02/adr-002-presentation-pattern.md`                             | Step 5: decision for Lab-03                 | Pi Sereyvathanak |
| `lab02/README.md`                                                   | Step 6: this file                           | Pi Sereyvathanak |
| `lab02/screenshots/mvc-viewer.png`, `validation.png`              | Evidence screenshots                        | Kiv Sovannlyda   |

Run the MVP screen: `mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp`
Run the tests: `mvn test`

## Lab-01 inputs reused

| Input                                                                    | Used for                                     | Lab-01 commit / tag |
| ------------------------------------------------------------------------ | -------------------------------------------- | ------------------- |
| `SalesModel`, `SalesEvent`, `SalesModelListener`                   | Model the presenter listens to               | fallback used       |
| `InputRules`, `Validation`                                           | Month and branch validation in the presenter | fallback used       |
| `CsvMonthReader`, `CsvTransactionParser`, `DefaultSalesAggregator` | Loading and summarising data                 | fallback used       |
| `ReportView`, `ReportController`                                     | MVC variant used in the comparison           | fallback used       |
| `lab01/quality-scenarios.md`                                           | Scenario cited in ADR-002                    | fallback used       |

## Change log

| Date       | Change                                                                                                                | Author           |
| ---------- | --------------------------------------------------------------------------------------------------------------------- | ---------------- |
| 2026-10-23 | Added MVP variant (`ui/mvp`, `FxReportScreen`, `ReportMvpApp`), `ReportPresenterTest`, comparison and ADR-002 | Pi Sereyvathanak |
