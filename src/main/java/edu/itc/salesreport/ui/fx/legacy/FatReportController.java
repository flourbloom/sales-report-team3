package edu.itc.salesreport.ui.fx.legacy;

import edu.itc.salesreport.ui.ReportController;
import javafx.scene.control.TextField;

/** Refactored: the handler only forwards raw text; the rest lives in the core. */
public class FatReportController {
    private final TextField monthField;
    private final ReportController controller;

    public FatReportController(TextField monthField, ReportController controller) {
        this.monthField = monthField;
        this.controller = controller;
    }

    public void onLoad() {
        controller.monthEntered(monthField.getText());
    }
}
