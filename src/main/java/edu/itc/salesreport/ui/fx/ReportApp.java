package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.SalesModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

public class ReportApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SalesModel model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()), new DefaultSalesAggregator());
        ReportView view = new ReportView(model);
        
        ReportController controller = new ReportController(model, Path.of("data"), Clock.systemDefaultZone(), view);
        view.bind(controller);
        
        stage.setScene(new Scene(view.root(), 1000, 600));
        stage.setTitle("Angkor Mart · Monthly sales (MVC)");
        stage.show();
        
        controller.monthEntered("2026-09");
    }
}