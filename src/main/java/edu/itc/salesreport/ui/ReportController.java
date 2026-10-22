package edu.itc.salesreport.ui;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;

public final class ReportController {
    private final SalesModel model;
    private final Path dataDir;
    private final Clock clock;
    private final ErrorDisplay errors;

    public ReportController(SalesModel model, Path dataDir, Clock clock, ErrorDisplay errors) {
        this.model = model;
        this.dataDir = dataDir;
        this.clock = clock;
        this.errors = errors;
    }

    public void monthEntered(String text) {
        switch (InputRules.month(text, clock)) {
            case Validation.Valid(var month) -> execute(new ReportCommand.LoadMonth(month));
            case Validation.Invalid(var message) -> errors.showError(message);
        }
    }

    public void branchChosen(String text) {
        switch (InputRules.branch(text)) {
            case Validation.Valid(var branch) -> execute(new ReportCommand.SelectBranch(branch));
            case Validation.Invalid(var message) -> errors.showError(message);
        }
    }

    void execute(ReportCommand command) {
        try {
            switch (command) {
                case ReportCommand.LoadMonth(var month) -> model.load(dataDir, month);
                case ReportCommand.SelectBranch(var branch) -> model.selectBranch(branch);
            }
            errors.clearError();
        } catch (NoSuchFileException e) {
            errors.showError("No sales data found in " + e.getFile());
        } catch (IOException e) {
            errors.showError("Could not read the sales files: " + e.getMessage());
        }
    }
}