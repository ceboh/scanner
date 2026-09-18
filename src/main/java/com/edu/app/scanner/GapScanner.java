package com.edu.app.scanner;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Continuously polls a {@link MarketDataClient} on a fixed schedule and
 * renders the ranked gap-up leaderboard after each poll. A failed poll
 * (network hiccup, rate limit, bad response) is logged and skipped rather
 * than stopping the scanner, so it keeps running unattended.
 */
public final class GapScanner {

    private final MarketDataClient client;
    private final ScannerConfig config;
    private final LeaderboardRenderer renderer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "gap-scanner-poll");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<List<Quote>> lastLeaderboard = new AtomicReference<>(List.of());

    public GapScanner(MarketDataClient client, ScannerConfig config, LeaderboardRenderer renderer) {
        this.client = client;
        this.config = config;
        this.renderer = renderer;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::pollOnce, 0, config.pollIntervalSeconds(), TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /** Exposed for tests and for any future programmatic consumer of the current ranking. */
    public List<Quote> lastLeaderboard() {
        return lastLeaderboard.get();
    }

    private void pollOnce() {
        try {
            List<Quote> snapshot = client.fetchSnapshot();
            List<Quote> ranked = GapRanker.rank(snapshot, config);
            lastLeaderboard.set(ranked);
            renderer.render(ranked, client.currentSessionLabel(), config);
        } catch (Exception e) {
            renderer.renderError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
