package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import edu.itc.salesreport.ui.ReportController;

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

import java.util.List;
import java.util.function.Function;

public final class ReportView implements SalesModelListener, ErrorDisplay {
    private final SalesModel model;
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<SaleTransaction> table = new TableView<>();
    private final BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public ReportView(SalesModel model) {
        this.model = model;
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        
        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo),
                column("Product", SaleTransaction::productName),
                column("Category", SaleTransaction::category),
                column("Qty", t -> String.valueOf(t.quantity())),
                column("Discount", t -> t.discount().toString()),
                column("Revenue", t -> t.revenue().toString())
        ));
        
        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");
        
        // Layout: HBox top, SplitPane center, VBox bottom
        HBox topBox = new HBox(10, new Label("Month"), monthField, new Label("Branch"), branchBox);
        SplitPane centerPane = new SplitPane(table, chart);
        VBox bottomBox = new VBox(5, status, error);
        
        root.setTop(topBox);
        root.setCenter(centerPane);
        root.setBottom(bottomBox);
        
        model.addListener(this);
    }

    public void bind(ReportController controller) {
        monthField.setOnAction(e -> controller.monthEntered(monthField.getText()));
        branchBox.setOnAction(e -> controller.branchChosen(branchBox.getValue()));
    }

    public Parent root() { return root; }

    @Override
    public void modelChanged(SalesEvent event) {
        branchBox.setValue(model.branch());
        table.getItems().setAll(model.visibleTransactions());
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        model.summary().revenueByCategory().forEach((category, revenue) -> 
            series.getData().add(new XYChart.Data<>(category, revenue))
        );
        chart.getData().setAll(series);
        
        switch (event) {
            case SalesEvent.MonthLoaded m -> 
                status.setText(String.format("%s: %d rows, %d rejected · revenue USD %s · receipts %d", 
                    m.month(), m.rows(), m.rejected(), model.summary().revenue(), model.summary().receipts()));
            case SalesEvent.BranchSelected b -> 
                status.setText(String.format("Branch %s · revenue USD %s · receipts %d", 
                    b.branch(), model.summary().revenue(), model.summary().receipts()));
        }
    }

    @Override public void showError(String message) { error.setText(message); }
    @Override public void clearError() { error.setText(""); }

    private static TableColumn<SaleTransaction, String> column(String title, Function<SaleTransaction, String> value) {
        var col = new TableColumn<SaleTransaction, String>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}