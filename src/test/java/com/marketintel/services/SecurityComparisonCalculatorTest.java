package com.marketintel.services;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityComparisonResult;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SecurityComparisonCalculatorTest {

    private final SecurityComparisonCalculator calculator =
            new SecurityComparisonCalculator();

    @Test
    void calculatesPositiveDifference() {

        MarketQuote first =
                quote(
                        "AAA",
                        "100",
                        "97.50",
                        "2.50"
                );

        MarketQuote second =
                quote(
                        "BBB",
                        "100",
                        "101",
                        "-1.00"
                );

        SecurityComparisonResult result =
                calculator.compare(
                        first,
                        second
                );

        assertEquals(
                new BigDecimal(
                        "3.50"
                ),
                result
                        .getPercentChangeDifference()
        );

        assertSame(
                first,
                result.getFirstQuote()
        );

        assertSame(
                second,
                result.getSecondQuote()
        );
    }

    @Test
    void calculatesNegativeDifference() {

        MarketQuote first =
                quote(
                        "AAA",
                        "100",
                        "101",
                        "-1.00"
                );

        MarketQuote second =
                quote(
                        "BBB",
                        "100",
                        "98",
                        "2.00"
                );

        SecurityComparisonResult result =
                calculator.compare(
                        first,
                        second
                );

        assertEquals(
                new BigDecimal(
                        "-3.00"
                ),
                result
                        .getPercentChangeDifference()
        );
    }

    @Test
    void calculatesZeroDifference() {

        MarketQuote first =
                quote(
                        "AAA",
                        "100",
                        "99",
                        "1.25"
                );

        MarketQuote second =
                quote(
                        "BBB",
                        "200",
                        "198",
                        "1.25"
                );

        SecurityComparisonResult result =
                calculator.compare(
                        first,
                        second
                );

        assertEquals(
                0,
                result
                        .getPercentChangeDifference()
                        .compareTo(
                                BigDecimal.ZERO
                        )
        );
    }

    private MarketQuote quote(
            String symbol,
            String price,
            String previousClose,
            String change) {

        return new MarketQuote(
                symbol,
                new BigDecimal(
                        price
                ),
                new BigDecimal(
                        previousClose
                ),
                new BigDecimal(
                        change
                )
        );
    }
}