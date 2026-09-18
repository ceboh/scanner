package com.edu.app.scanner;

import java.util.Comparator;
import java.util.List;

/**
 * Pure filtering/ranking logic, kept separate from I/O so it can be unit
 * tested without hitting the network. Keeps only stocks priced between
 * {@code minPrice} and {@code maxPrice} that are gapping up by at least
 * {@code minGapPercent}, sorted with the biggest percentage move first.
 */
public final class GapRanker {

    private GapRanker() {
    }

    public static List<Quote> rank(List<Quote> quotes, ScannerConfig config) {
        return quotes.stream()
                .filter(q -> q.price() >= config.minPrice() && q.price() <= config.maxPrice())
                .filter(q -> q.changePercent() >= config.minGapPercent())
                .filter(q -> q.volume() >= config.minVolume())
                .sorted(Comparator.comparingDouble(Quote::changePercent).reversed())
                .limit(config.topN())
                .toList();
    }
}
