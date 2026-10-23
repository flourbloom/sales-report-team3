# ADR-002: Presentation pattern for the sales report (MVC vs MVP)

- **Status:** Accepted
- **Date:** 2026-10-23
- **Deciders:** Team 3 (⟨names⟩)

## Context

Lab-01 left us with an MVC screen: `ReportView` listens to `SalesModel`, reads it directly, and formats the status line and money values itself inside JavaFX code. In Lab-02 we rebuilt the same screen as MVP with a passive view (`ReportScreen`, `ReportPresenter`, `FxReportScreen`) and compared both (see `mvc-vs-mvp.md`).

## Decision

We keep **MVP with a passive view** for Lab-03.

## Rationale

- **Testability:** the presenter produces every display string (`"PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00"`, `money(1234.5)` = `1,234.50`) and `ReportPresenterTest` checks them with a `FakeScreen`, with no JavaFX. In MVC the same text lives in `ReportView` and cannot be tested without the toolkit. This meets the scenario directly.
- **Separation:** `FxReportScreen` holds no model reference and makes no decisions, so `UiIndependenceTest` can guard `ui/mvp` against JavaFX imports.
- **Reuse:** a second screen (web or Swing) only has to implement the four `ReportScreen` methods; formatting and validation handling are reused.

## Consequences

- **Positive:** display logic is tested in milliseconds; the JavaFX class is thin and rarely needs to change.
- **Negative:** more files and more lines; adding a column (for example Discount) touches `RowItem`, the presenter, the screen and the test, versus one file in MVC.
- **Mitigation:** keep `RowItem` small and keep formatting in one place (`money`), so each new column stays a mechanical change.
- **Revisit if:** the UI stays a single JavaFX screen and the extra files slow the team down more than the tests help.

## Alternatives considered

- **MVC (`ReportView` + `ReportController`):** fewer files and quicker column changes, but display text is formatted inside the view and is untestable without the toolkit, and the view depends on the model.
