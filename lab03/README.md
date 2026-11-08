
# Lab-03: Model View ViewModel

Sales report screen rebuilt as MVVM: `ReportViewModel` with properties, validation and commands, a bound JavaFX view, background loading with progress and cancel, and toolkit-free tests. Branch: `lab-03`. Pull request: 

## Files in this lab

| Path                                                                                     | Purpose                                             | Task   | Author          |
| ---------------------------------------------------------------------------------------- | --------------------------------------------------- | ------ | --------------- |
| `src/main/java/edu/itc/salesreport/ui/Command.java`, `DelegateCommand.java`          | Action plus "can run now" flag                      | 1      | Seavhong        |
| `src/main/java/edu/itc/salesreport/ui/SummarySource.java`, `BackgroundJobs.java`     | Ports: model and threading as seen by the ViewModel | 1      | Seavhong        |
| `src/main/java/edu/itc/salesreport/ui/ReportViewModel.java`                            | State, validation, commands, loading                | 1 to 4 | Seavhong & Lyda |
| `src/main/java/edu/itc/salesreport/ui/ReportView.java`, `ReportViewerApp.java`       | Bound view and composition root                     | 2      | Seavhong        |
| `src/main/java/edu/itc/salesreport/ui/CallerThreadJobs.java`                           | Same-thread jobs (frozen-UI baseline)               | 2      | Seavhong        |
| `src/main/java/edu/itc/salesreport/ui/FxBackgroundJobs.java`                           | `javafx.concurrent.Task` jobs                     | 4      | Lyda            |
| `src/main/java/edu/itc/salesreport/ui/FrameGapMonitor.java`, `LoadBenchmarkApp.java` | Responsiveness measurement                          | 4      | Lyda            |
| `src/test/java/edu/itc/salesreport/ui/ManualJobs.java`                                 | Test double: jobs run when the test says so         | 5      | Ratana          |
| `src/test/java/edu/itc/salesreport/ui/ReportViewModelTest.java`                        | 15 toolkit-free ViewModel tests                     | 5      | Ratana          |
| `src/test/java/edu/itc/salesreport/ui/ViewModelImportsTest.java`                       | Guard: ViewModel imports javafx.base only           | 5      | Ratana          |
| `lab03/viewmodel-design.md`                                                            | Inventory, members, class and state diagrams        | 1, 3   | Seavhong        |
| `lab03/measurements.md`                                                                | Load time and frame gap, both modes                 | 4      | Lyda            |
| `lab03/mvc-vs-mvvm.md`                                                                 | Comparison with numbers and conclusion              | 5      | Ratana          |
| `lab03/README.md`                                                                      | This file                                           | 5      | Ratana          |
| `lab03/screenshots/viewer.png`                                                         | MVVM viewer next to the MVC viewer                  | 2      | Seavhong        |

Run the viewer: `mvn javafx:run`
Run the benchmark: `mvn javafx:run -Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp "-Djavafx.args=--data=data --month=2026-08"`
Run the tests and the coverage report: `mvn verify`

## Lab-02 inputs reused

| Input                                                                                       | Used for                                                | Lab-02 commit / tag                   |
| ------------------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------- |
| `model` records, `CsvTransactionParser`, `SimpleReportEngine`, `SalesDataGenerator` | Loading one branch for one month; measurement data      | fallback starter A                    |
| `MvcReportViewerApp` with `ReportController`                                            | Controller inventory and the MVC side of the comparison | fallback starter B                    |
| `data/2026-08` (5,000 rows per branch and day, seed 42)                                   | Task 4 measurements                                     | generated with `SalesDataGenerator` |

## Change log

| Date       | Change                                                                                                                                                                | Author            | Model package changed?                                                                                 |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------ |
| 2026-11-09 | Added `ManualJobs`, rewrote `ReportViewModelTest` (15 tests, no files, no real clock), enabled `ViewModelImportsTest`, wrote `mvc-vs-mvvm.md` and this README | <author></author> | No (check: `git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model` prints nothing) |
