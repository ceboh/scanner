package com.edu.app;

import com.edu.app.scanner.ConsoleLeaderboardRenderer;
import com.edu.app.scanner.GapScanner;
import com.edu.app.scanner.MarketDataClient;
import com.edu.app.scanner.PolygonMarketDataClient;
import com.edu.app.scanner.ScannerConfig;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Entry point: runs a continuous gap-up scanner that watches the whole US
 * market and keeps a ranked leaderboard of $1-$20 stocks gapping up, in
 * pre-market and during regular trading hours alike.
 */
public final class App {

    public static void main(String[] args) {
        ScannerConfig config;
        try {
            config = ScannerConfig.fromEnvironment();
        } catch (IllegalStateException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        MarketDataClient client = new PolygonMarketDataClient(httpClient, config.apiKey());
        GapScanner scanner = new GapScanner(client, config, new ConsoleLeaderboardRenderer());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scanner.stop();
            System.out.println("\nScanner stopped.");
        }));

        System.out.println("Starting gap-up scanner (polling every " + config.pollIntervalSeconds() + "s)... "
                + "press Ctrl+C to stop.");
        scanner.start();
    }
}
