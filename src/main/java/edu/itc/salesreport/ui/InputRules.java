package edu.itc.salesreport.ui;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Set;

public final class InputRules {
    private static final Set<String> BRANCHES = Set.of(SalesModel.ALL, "PNH", "REP", "BTB");

    private InputRules() {}

    public static Validation<YearMonth> month(String text, Clock clock) {
        if (text == null || text.isBlank()) {
            return new Validation.Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09");
        }
        try {
            YearMonth ym = YearMonth.parse(text.strip());
            if (ym.isAfter(YearMonth.now(clock))) {
                return new Validation.Invalid<>(ym + " is in the future");
            }
            return new Validation.Valid<>(ym);
        } catch (DateTimeParseException e) {
            return new Validation.Invalid<>("'" + text + "' is not a month in the form yyyy-MM");
        }
    }

    public static Validation<String> branch(String text) {
        if (text == null) return new Validation.Invalid<>("Unknown branch 'null': use ALL, PNH, REP or BTB");
        String b = text.strip().toUpperCase();
        if (!BRANCHES.contains(b)) {
            return new Validation.Invalid<>("Unknown branch '" + b + "': use ALL, PNH, REP or BTB");
        }
        return new Validation.Valid<>(b);
    }
}