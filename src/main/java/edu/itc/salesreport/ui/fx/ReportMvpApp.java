package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.mvp.ReportPresenter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

public class ReportMvpApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SalesModel model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()), new DefaultSalesAggregator());
        FxReportScreen screen = new FxReportScreen();
        ReportPresenter presenter = new ReportPresenter(model, screen, Path.of("data"), Clock.systemDefaultZone());
        screen.attach(presenter);

        stage.setScene(new Scene(screen.root(), 1000, 600));
        stage.setTitle("Angkor Mart · Monthly sales (MVP)");
        stage.show();

        presenter.monthEntered("2026-09");
    }
}
