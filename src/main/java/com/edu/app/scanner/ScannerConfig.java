package com.edu.app.scanner;

import java.util.function.Function;

/**
 * Runtime configuration for the scanner, read from environment variables
 * (falling back to system properties, then defaults) so it can be tuned
 * without a rebuild.
 */
public record ScannerConfig(
        DataProvider provider,
        String polygonApiKey,
        String alpacaKeyId,
        String alpacaSecretKey,
        double minPrice,
        double maxPrice,
        double minGapPercent,
        long minVolume,
        int pollIntervalSeconds,
        int topN) {

    public enum DataProvider {
        ALPACA, POLYGON
    }

    public static ScannerConfig fromEnvironment() {
        DataProvider provider = readProvider();

        String polygonApiKey = readString("POLYGON_API_KEY", null);
        String alpacaKeyId = readString("ALPACA_API_KEY_ID", null);
        String alpacaSecretKey = readString("ALPACA_API_SECRET_KEY", null);

        if (provider == DataProvider.ALPACA && (isBlank(alpacaKeyId) || isBlank(alpacaSecretKey))) {
            throw new IllegalStateException(
                    "ALPACA_API_KEY_ID and ALPACA_API_SECRET_KEY are not both set. Get keys at "
                            + "https://app.alpaca.markets/paper/dashboard/overview (paper keys work fine for "
                            + "market data) and export them, e.g. "
                            + "`export ALPACA_API_KEY_ID=your_key_id` and "
                            + "`export ALPACA_API_SECRET_KEY=your_secret_key`.");
        }
        if (provider == DataProvider.POLYGON && isBlank(polygonApiKey)) {
            throw new IllegalStateException(
                    "POLYGON_API_KEY is not set. Get a free key at https://polygon.io/dashboard/api-keys "
                            + "and export it, e.g. `export POLYGON_API_KEY=your_key_here`. "
                            + "Note: real-time pre-market/after-hours data requires a paid Polygon plan; "
                            + "the free tier serves delayed data.");
        }

        double minPrice = readDouble("SCAN_MIN_PRICE", 1.0);
        double maxPrice = readDouble("SCAN_MAX_PRICE", 20.0);
        double minGapPercent = readDouble("SCAN_MIN_GAP_PERCENT", 2.0);
        long minVolume = readLong("SCAN_MIN_VOLUME", 0L);
        int pollIntervalSeconds = readInt("SCAN_POLL_INTERVAL_SECONDS", 15);
        int topN = readInt("SCAN_TOP_N", 20);

        if (minPrice <= 0 || maxPrice <= 0 || minPrice > maxPrice) {
            throw new IllegalStateException("SCAN_MIN_PRICE/SCAN_MAX_PRICE must satisfy 0 < min <= max");
        }
        if (pollIntervalSeconds < 1) {
            throw new IllegalStateException("SCAN_POLL_INTERVAL_SECONDS must be >= 1");
        }
        if (topN < 1) {
            throw new IllegalStateException("SCAN_TOP_N must be >= 1");
        }

        return new ScannerConfig(provider, polygonApiKey, alpacaKeyId, alpacaSecretKey,
                minPrice, maxPrice, minGapPercent, minVolume, pollIntervalSeconds, topN);
    }

    private static DataProvider readProvider() {
        String raw = readString("MARKET_DATA_PROVIDER", "alpaca");
        try {
            return DataProvider.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Invalid MARKET_DATA_PROVIDER: " + raw + " (expected 'alpaca' or 'polygon')", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String readString(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static double readDouble(String key, double fallback) {
        return readAs(key, fallback, Double::parseDouble);
    }

    private static long readLong(String key, long fallback) {
        return readAs(key, fallback, Long::parseLong);
    }

    private static int readInt(String key, int fallback) {
        return readAs(key, fallback, Integer::parseInt);
    }

    private static <T> T readAs(String key, T fallback, Function<String, T> parser) {
        String raw = readString(key, null);
        if (raw == null) {
            return fallback;
        }
        try {
            return parser.apply(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid value for " + key + ": " + raw, e);
        }
    }
}
