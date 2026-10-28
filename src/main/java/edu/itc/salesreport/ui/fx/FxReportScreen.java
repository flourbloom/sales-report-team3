package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.ui.mvp.ReportPresenter;
import edu.itc.salesreport.ui.mvp.ReportScreen;
import edu.itc.salesreport.ui.mvp.RowItem;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Passive JavaFX view: no model access, only setters and gesture forwarding. */
public final class FxReportScreen implements ReportScreen {
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<RowItem> table = new TableView<>();
    private final BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public FxReportScreen() {
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");

        table.getColumns().setAll(List.of(
                column("Date", RowItem::date),
                column("Receipt", RowItem::receipt),
                column("Product", RowItem::product),
                column("Category", RowItem::category),
                column("Qty", RowItem::quantity),
                column("Discount", RowItem::discount),
                column("Revenue", RowItem::revenue)
        ));

        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");

        HBox topBox = new HBox(10, new Label("Month"), monthField, new Label("Branch"), branchBox);
        SplitPane centerPane = new SplitPane(table, chart);
        VBox bottomBox = new VBox(5, status, error);

        root.setTop(topBox);
        root.setCenter(centerPane);
        root.setBottom(bottomBox);
    }

    public void attach(ReportPresenter presenter) {
        monthField.setOnAction(e -> presenter.monthEntered(monthField.getText()));
        branchBox.setOnAction(e -> presenter.branchChosen(branchBox.getValue()));
    }

    public Parent root() {
        return root;
    }

    @Override
    public void showRows(List<RowItem> rows) {
        table.getItems().setAll(rows);
    }

    @Override
    public void showCategoryRevenue(Map<String, BigDecimal> revenue) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        revenue.forEach((category, rev) ->
                series.getData().add(new XYChart.Data<>(category, rev)));
        chart.getData().setAll(series);
    }

    @Override
    public void showStatus(String text) {
        status.setText(text);
    }

    @Override
    public void showError(String message) {
        error.setText(message);
    }

    private static TableColumn<RowItem, String> column(String title, Function<RowItem, String> value) {
        var col = new TableColumn<RowItem, String>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}
