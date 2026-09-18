package com.edu.app.scanner;

import java.io.IOException;
import java.util.List;

/**
 * Source of market-wide price snapshots. Implementations are expected to
 * return one snapshot per US-listed ticker per call, covering whichever
 * session is currently active (pre-market, regular hours or after-hours).
 */
public interface MarketDataClient {

    List<Quote> fetchSnapshot() throws IOException, InterruptedException;

    /** Human-readable label for the market session the last snapshot came from. */
    String currentSessionLabel();
}
