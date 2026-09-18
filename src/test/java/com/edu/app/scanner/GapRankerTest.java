package com.edu.app.scanner;

import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GapRankerTest {

    private static Quote quote(String symbol, double price, double changePercent, long volume) {
        return new Quote(symbol, price, changePercent, volume, Instant.now());
    }

    private static ScannerConfig config(double minPrice, double maxPrice, double minGap, long minVolume, int topN) {
        return new ScannerConfig("test-key", minPrice, maxPrice, minGap, minVolume, 15, topN);
    }

    @Test
    public void sortsByGapPercentDescending() {
        List<Quote> quotes = List.of(
                quote("AAA", 5.00, 3.0, 100_000),
                quote("BBB", 5.00, 12.5, 100_000),
                quote("CCC", 5.00, 7.2, 100_000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 0, 0, 10));

        assertEquals(List.of("BBB", "CCC", "AAA"), ranked.stream().map(Quote::symbol).toList());
    }

    @Test
    public void excludesStocksOutsidePriceRange() {
        List<Quote> quotes = List.of(
                quote("TOO_CHEAP", 0.50, 20.0, 100_000),
                quote("IN_RANGE", 10.00, 20.0, 100_000),
                quote("TOO_EXPENSIVE", 25.00, 20.0, 100_000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 0, 0, 10));

        assertEquals(List.of("IN_RANGE"), ranked.stream().map(Quote::symbol).toList());
    }

    @Test
    public void excludesGapsBelowMinimumThreshold() {
        List<Quote> quotes = List.of(
                quote("WEAK", 5.00, 1.0, 100_000),
                quote("STRONG", 5.00, 5.0, 100_000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 2.0, 0, 10));

        assertEquals(List.of("STRONG"), ranked.stream().map(Quote::symbol).toList());
    }

    @Test
    public void excludesGapDownsAndFlatMoves() {
        List<Quote> quotes = List.of(
                quote("DOWN", 5.00, -8.0, 100_000),
                quote("FLAT", 5.00, 0.0, 100_000),
                quote("UP", 5.00, 4.0, 100_000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 0.01, 0, 10));

        assertEquals(List.of("UP"), ranked.stream().map(Quote::symbol).toList());
    }

    @Test
    public void excludesLowVolumeWhenMinVolumeSet() {
        List<Quote> quotes = List.of(
                quote("THIN", 5.00, 10.0, 500),
                quote("LIQUID", 5.00, 10.0, 500_000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 0, 50_000, 10));

        assertEquals(List.of("LIQUID"), ranked.stream().map(Quote::symbol).toList());
    }

    @Test
    public void limitsToTopN() {
        List<Quote> quotes = List.of(
                quote("A", 5.00, 10.0, 1000),
                quote("B", 5.00, 9.0, 1000),
                quote("C", 5.00, 8.0, 1000));

        List<Quote> ranked = GapRanker.rank(quotes, config(1, 20, 0, 0, 2));

        assertEquals(2, ranked.size());
        assertTrue(ranked.stream().map(Quote::symbol).toList().containsAll(List.of("A", "B")));
    }
}
