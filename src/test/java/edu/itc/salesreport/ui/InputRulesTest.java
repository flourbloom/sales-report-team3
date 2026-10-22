package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Input rules are pure: a fixed clock makes them independent of today's date. */
class InputRulesTest {
    // 2026-09-27 10:00 in Phnom Penh, so "now" is the month 2026-09
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));

    @Test
    void validMonthIsTrimmed() {
        assertEquals(new Validation.Valid<>(YearMonth.of(2026, 9)),
                InputRules.month("  2026-09 ", CLOCK));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" is invalid")
    @ValueSource(strings = {"", " ", "2026/09", "2026-9", "Sept", "2026-10"})
    void invalidMonthIsRejected(String text) {
        assertInstanceOf(Validation.Invalid.class, InputRules.month(text, CLOCK));
    }

    @Test
    void futureMonthHasExactMessage() {
        assertEquals(new Validation.Invalid<YearMonth>("2026-10 is in the future"),
                InputRules.month("2026-10", CLOCK));
    }

    @Test
    void branchIsTrimmedAndUpperCased() {
        assertEquals(new Validation.Valid<>("REP"), InputRules.branch(" rep "));
        assertEquals(new Validation.Valid<>("ALL"), InputRules.branch("all"));
        assertEquals(
                new Validation.Invalid<String>(
                        "Unknown branch 'XYZ': use ALL, PNH, REP or BTB"),
                InputRules.branch("xyz"));
    }
}
