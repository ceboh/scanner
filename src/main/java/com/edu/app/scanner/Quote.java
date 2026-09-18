package com.edu.app.scanner;

import java.time.Instant;

/**
 * A single snapshot reading for one ticker: last price, percent change versus
 * the previous session's close (Polygon computes this the same way whether
 * the market is in pre-market, regular hours or after-hours), and volume.
 */
public record Quote(String symbol, double price, double changePercent, long volume, Instant updatedAt) {
}
