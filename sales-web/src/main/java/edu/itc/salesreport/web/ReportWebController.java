package edu.itc.salesreport.web;

import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ui.InputRules;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.Validation;
import edu.itc.salesreport.ui.Validation.Valid;
import edu.itc.salesreport.ui.Validation.Invalid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@Controller
public class ReportWebController {
    private record Inputs(Validation<YearMonth> month, Validation<String> branch) {}

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final Clock clock;
    private final Path dataDir;

    public ReportWebController(CsvMonthReader reader, SalesAggregator aggregator, Clock clock,
                               @Value("${salesreport.data-dir:data}") String dataDir) {
        this.reader = reader;
        this.aggregator = aggregator;
        this.clock = clock;
        this.dataDir = Path.of(dataDir);
    }

    @GetMapping("/report")
    public String report(@RequestParam(defaultValue = "2026-09") String month,
                         @RequestParam(defaultValue = "ALL") String branch,
                         Model page) {
        page.addAttribute("month", month);
        page.addAttribute("branch", branch);
        page.addAttribute("branches", List.of("ALL", "PNH", "REP", "BTB"));

        var inputs = new Inputs(InputRules.month(month, clock), InputRules.branch(branch));
        if (inputs.month() instanceof Valid<YearMonth> vm && inputs.branch() instanceof Valid<String> vb) {
            show(vm.value(), vb.value(), page);
        } else if (inputs.month() instanceof Invalid<YearMonth> inv) {
            page.addAttribute("error", inv.message());
        } else if (inputs.branch() instanceof Invalid<String> inv) {
            page.addAttribute("error", inv.message());
        }
        return "report";
    }

    private void show(YearMonth month, String branch, Model page) {
        var model = new SalesModel(reader, aggregator);
        try {
            model.load(dataDir, month);
            model.selectBranch(branch);
            page.addAttribute("summary", model.summary());
            page.addAttribute("rejected", model.rejectedRows());
        } catch (NoSuchFileException e) {
            page.addAttribute("error", "No sales data found in " + e.getFile());
        } catch (IOException e) {
            page.addAttribute("error", "Could not read the sales files: " + e.getMessage());
        }
    }
}
