package com.edu.app.scanner;

import java.util.function.Function;

/**
 * Runtime configuration for the scanner, read from environment variables
 * (falling back to system properties, then defaults) so it can be tuned
 * without a rebuild.
 */
public record ScannerConfig(
        String apiKey,
        double minPrice,
        double maxPrice,
        double minGapPercent,
        long minVolume,
        int pollIntervalSeconds,
        int topN) {

    public static ScannerConfig fromEnvironment() {
        String apiKey = readString("POLYGON_API_KEY", null);
        if (apiKey == null || apiKey.isBlank()) {
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

        return new ScannerConfig(apiKey, minPrice, maxPrice, minGapPercent, minVolume, pollIntervalSeconds, topN);
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
