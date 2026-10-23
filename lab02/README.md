# Lab-02: Model View Presenter

Sales report screen rebuilt as MVP with a passive view, compared with the Lab-01 MVC version. Tag: `lab-02`.

## Files in this lab

| Path                                                                | Task                                      | Author |
| ------------------------------------------------------------------- | ----------------------------------------- | ------ |
| `src/main/java/edu/itc/salesreport/ui/mvp/RowItem.java`             | Step 1: row record for the view           | ⟨name⟩ |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportScreen.java`        | Step 1: passive-view interface            | ⟨name⟩ |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java`     | Step 1: presenter and money formatting    | ⟨name⟩ |
| `src/main/java/edu/itc/salesreport/ui/fx/FxReportScreen.java`       | Step 2: JavaFX passive view               | ⟨name⟩ |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportMvpApp.java`         | Step 2: launcher                          | ⟨name⟩ |
| `src/test/java/edu/itc/salesreport/ui/mvp/ReportPresenterTest.java` | Step 3: presenter tests with `FakeScreen` | ⟨name⟩ |
| `src/test/java/edu/itc/salesreport/UiIndependenceTest.java`         | Step 3: `ui/mvp` added                    | ⟨name⟩ |
| `lab02/mvc-vs-mvp.md`                                               | Step 4: measurements and comparison       | ⟨name⟩ |
| `lab02/adr-002-presentation-pattern.md`                             | Step 5: decision for Lab-03               | ⟨name⟩ |
| `lab02/README.md`                                                   | Step 6: this file                         | ⟨name⟩ |
| `lab02/screenshots/mvc-viewer.png`, `validation.png`                | Evidence screenshots                      | ⟨name⟩ |

Run the MVP screen: `mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp`
Run the tests: `mvn test`

## Lab-01 inputs reused

| Input                                                              | Used for                                     | Lab-01 commit / tag       |
| ------------------------------------------------------------------ | -------------------------------------------- | ------------------------- |
| `SalesModel`, `SalesEvent`, `SalesModelListener`                   | Model the presenter listens to               | ⟨hash or `lab-01`⟩        |
| `InputRules`, `Validation`                                         | Month and branch validation in the presenter | ⟨hash or `lab-01`⟩        |
| `CsvMonthReader`, `CsvTransactionParser`, `DefaultSalesAggregator` | Loading and summarising data                 | ⟨hash or `lab-01`⟩        |
| `ReportView`, `ReportController`                                   | MVC variant used in the comparison           | ⟨hash or `lab-01`⟩        |
| `lab01/quality-scenarios.md`                                       | Scenario cited in ADR-002                    | ⟨hash or "fallback used"⟩ |

## Change log

| Date       | Change                                                                                                        | Author  |
| ---------- | ------------------------------------------------------------------------------------------------------------- | ------- |
| 2026-10-23 | Added MVP variant (`ui/mvp`, `FxReportScreen`, `ReportMvpApp`), `ReportPresenterTest`, comparison and ADR-002 | Temam 3 |
