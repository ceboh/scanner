package com.edu.app.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pulls a full US-market snapshot from Polygon.io in a single request per
 * poll (rather than one request per symbol), which is what makes continuous
 * whole-market scanning practical on a free/low-tier API key.
 *
 * <p>Polygon's {@code todaysChangePerc} is computed against the previous
 * session's close, so it doubles as the "gap" figure whether the market is
 * in pre-market, regular hours or after-hours.
 */
public final class PolygonMarketDataClient implements MarketDataClient {

    private static final URI SNAPSHOT_URI_TEMPLATE =
            URI.create("https://api.polygon.io/v2/snapshot/locale/us/markets/stocks/tickers");
    private static final URI MARKET_STATUS_URI =
            URI.create("https://api.polygon.io/v1/marketstatus/now");

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final AtomicReference<String> sessionLabel = new AtomicReference<>("UNKNOWN");

    public PolygonMarketDataClient(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    @Override
    public List<Quote> fetchSnapshot() throws IOException, InterruptedException {
        refreshSessionLabel();

        URI uri = URI.create(SNAPSHOT_URI_TEMPLATE + "?apiKey=" + apiKey);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Polygon snapshot request failed: HTTP " + response.statusCode()
                    + " - " + truncate(response.body()));
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode tickers = root.path("tickers");
        List<Quote> quotes = new ArrayList<>(tickers.size());
        Instant now = Instant.now();

        for (JsonNode node : tickers) {
            String symbol = node.path("ticker").asText(null);
            if (symbol == null || symbol.isBlank()) {
                continue;
            }

            double price = firstPositive(
                    node.path("min").path("c").asDouble(0),
                    node.path("day").path("c").asDouble(0),
                    node.path("prevDay").path("c").asDouble(0));
            if (price <= 0) {
                continue;
            }

            double changePercent = node.path("todaysChangePerc").asDouble(0);
            long volume = Math.max(node.path("day").path("v").asLong(0), node.path("min").path("av").asLong(0));

            quotes.add(new Quote(symbol, price, changePercent, volume, now));
        }

        return quotes;
    }

    @Override
    public String currentSessionLabel() {
        return sessionLabel.get();
    }

    private void refreshSessionLabel() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(MARKET_STATUS_URI + "?apiKey=" + apiKey))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                String market = root.path("market").asText("unknown");
                sessionLabel.set(market.toUpperCase());
            }
        } catch (Exception e) {
            // Non-fatal: keep the previous label if the status call fails.
        }
    }

    private static double firstPositive(double... values) {
        for (double v : values) {
            if (v > 0) {
                return v;
            }
        }
        return 0;
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "..." : s);
    }
}
