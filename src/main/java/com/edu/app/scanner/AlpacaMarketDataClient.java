package com.edu.app.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pulls Alpaca's market-wide "top movers" screener in a single request per
 * poll: {@code GET /v1beta1/screener/stocks/movers}. That endpoint is
 * Alpaca's own server-side ranking of the biggest gainers across the whole
 * market, sorted by percent change, which is exactly the "gap up" signal
 * this scanner needs - no per-symbol requests or client-side ranking of the
 * full market required.
 *
 * <p><b>Known trade-off vs. a full-market snapshot:</b> Alpaca caps this
 * endpoint at its top ~50 market-wide movers. If the very largest movers on
 * a given day happen to be priced outside $1-$20, some in-range gappers
 * further down the true ranking won't appear in the top 50 and so won't be
 * seen here. Raise {@code top} (max 50, Alpaca's own limit) to reduce that
 * risk; there is no way around it with this endpoint short of Alpaca's
 * SIP/premium full-snapshot data.
 *
 * <p>Session state (pre-market/regular/after-hours/closed) is derived from
 * the current US Eastern time using standard hours, as a simple label for
 * the leaderboard header - it does not account for market holidays.
 */
public final class AlpacaMarketDataClient implements MarketDataClient {

    private static final URI MOVERS_URI = URI.create("https://data.alpaca.markets/v1beta1/screener/stocks/movers");
    private static final int TOP = 50; // Alpaca's own maximum for this endpoint
    private static final ZoneId NY = ZoneId.of("America/New_York");

    private final HttpClient httpClient;
    private final String keyId;
    private final String secretKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public AlpacaMarketDataClient(HttpClient httpClient, String keyId, String secretKey) {
        this.httpClient = httpClient;
        this.keyId = keyId;
        this.secretKey = secretKey;
    }

    @Override
    public List<Quote> fetchSnapshot() throws IOException, InterruptedException {
        URI uri = URI.create(MOVERS_URI + "?top=" + TOP);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("APCA-API-KEY-ID", keyId)
                .header("APCA-API-SECRET-KEY", secretKey)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Alpaca movers request failed: HTTP " + response.statusCode()
                    + " - " + truncate(response.body()));
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode gainers = root.path("gainers");
        List<Quote> quotes = new ArrayList<>(gainers.size());
        Instant now = Instant.now();

        for (JsonNode node : gainers) {
            String symbol = node.path("symbol").asText(null);
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            double price = node.path("price").asDouble(0);
            if (price <= 0) {
                continue;
            }
            double changePercent = node.path("percent_change").asDouble(0);
            long volume = node.path("volume").asLong(0);

            quotes.add(new Quote(symbol, price, changePercent, volume, now));
        }

        return quotes;
    }

    @Override
    public String currentSessionLabel() {
        ZonedDateTime nowEt = ZonedDateTime.now(NY);
        if (nowEt.getDayOfWeek() == DayOfWeek.SATURDAY || nowEt.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "CLOSED (weekend)";
        }
        int minuteOfDay = nowEt.getHour() * 60 + nowEt.getMinute();
        if (minuteOfDay < 4 * 60) {
            return "CLOSED";
        } else if (minuteOfDay < 9 * 60 + 30) {
            return "PRE-MARKET";
        } else if (minuteOfDay < 16 * 60) {
            return "REGULAR";
        } else if (minuteOfDay < 20 * 60) {
            return "AFTER-HOURS";
        }
        return "CLOSED";
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "..." : s);
    }
}
