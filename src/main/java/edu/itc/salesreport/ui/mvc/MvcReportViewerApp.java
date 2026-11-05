package edu.itc.salesreport.ui.mvc;

import edu.itc.salesreport.engine.SimpleReportEngine;
import edu.itc.salesreport.model.BranchSummary;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** Fallback Lab-02 viewer (MVC): the controller reads and writes controls. */
public class MvcReportViewerApp extends Application {

    @Override
    public void start(Stage stage) {
        ComboBox<YearMonth> month = new ComboBox<>();
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) month.getItems().add(now.minusMonths(i));
        List<CheckBox> branches = new ArrayList<>();
        for (String b : SimpleReportEngine.BRANCHES) {
            CheckBox box = new CheckBox(b);
            box.setSelected(true);
            branches.add(box);
        }
        Button generate = new Button("Generate");
        Label status = new Label();
        TableView<BranchSummary> table = new TableView<>();
        TableColumn<BranchSummary, String> branch = new TableColumn<>("Branch");
        branch.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().branch()));
        TableColumn<BranchSummary, String> revenue =
                new TableColumn<>("Revenue");
        revenue.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().revenue().toPlainString()));
        table.getColumns().add(branch);
        table.getColumns().add(revenue);

        var controller = new ReportController(new SimpleReportEngine(),
                Path.of(getParameters().getNamed()
                        .getOrDefault("data", "data")),
                month, branches, status, table);
        generate.setOnAction(e -> controller.onGenerate());

        HBox top = new HBox(8, month, generate, status);
        top.getChildren().addAll(1, branches);
        stage.setScene(new Scene(new BorderPane(table, top, null, null, null),
                800, 500));
        stage.setTitle("Angkor Mart · Monthly report (MVC)");
        stage.show();
    }

    /** Controller: validation, loading and display mixed with controls. */
    static final class ReportController {
        private final SimpleReportEngine engine;
        private final Path dataDir;
        private final ComboBox<YearMonth> month;
        private final List<CheckBox> branches;
        private final Label status;
        private final TableView<BranchSummary> table;

        ReportController(SimpleReportEngine engine, Path dataDir,
                ComboBox<YearMonth> month, List<CheckBox> branches,
                Label status, TableView<BranchSummary> table) {
            this.engine = engine;
            this.dataDir = dataDir;
            this.month = month;
            this.branches = branches;
            this.status = status;
            this.table = table;
        }

        void onGenerate() {
            YearMonth m = month.getValue();
            if (m == null) {
                status.setText("Choose a month.");
                return;
            }
            List<BranchSummary> rows = new ArrayList<>();
            try {
                for (CheckBox box : branches) {
                    if (box.isSelected()) {
                        // runs on the FX thread: the window freezes
                        rows.add(engine.summarizeBranch(m, box.getText(),
                                dataDir));
                    }
                }
            } catch (IOException e) {
                status.setText("Load failed: " + e.getMessage());
                return;
            }
            table.getItems().setAll(rows);
            status.setText("Loaded " + rows.size() + " branch(es).");
        }
    }
}