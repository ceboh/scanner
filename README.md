# aib — Gap-Up Stock Scanner

Continuously scans the whole US stock market — pre-market and regular
trading hours alike — and keeps a live, ranked list of $1–$20 stocks that
are gapping up, biggest percentage move always on top.

## How it works

Each poll cycle makes a single bulk request to Polygon.io's full-market
snapshot endpoint (one request covers every US ticker, rather than one
request per symbol), which keeps the scan practical on a normal API key
and avoids per-symbol rate limits. Polygon's `todaysChangePerc` is computed
against the previous session's close, so the same figure works as the "gap"
percentage whether the market is currently in pre-market, regular hours, or
after-hours.

Each cycle:
1. Fetches the market-wide snapshot.
2. Filters to tickers priced between `SCAN_MIN_PRICE` and `SCAN_MAX_PRICE`
   (defaults: $1–$20).
3. Drops anything gapping up less than `SCAN_MIN_GAP_PERCENT` (default 2%)
   or below `SCAN_MIN_VOLUME` (default: no volume filter).
4. Sorts by percentage gap descending and redraws the top `SCAN_TOP_N`
   (default 20) in the terminal.

A failed poll (network hiccup, rate limit) is logged and skipped — the
scanner keeps running rather than exiting.

## Setup

1. Get a free API key at https://polygon.io/dashboard/api-keys.

   **Note:** the free tier serves end-of-day/delayed data. Live pre-market
   and after-hours snapshots require a paid Polygon plan (e.g. "Stocks
   Starter" or higher) that includes extended-hours data.

2. Export it:

   ```bash
   export POLYGON_API_KEY=your_key_here
   ```

3. Build and run:

   ```bash
   mvn clean package
   java -jar target/aib-1.0-shaded.jar
   ```

Stop with `Ctrl+C`.

## Configuration

All optional, set as environment variables:

| Variable                    | Default | Meaning                                   |
|------------------------------|---------|--------------------------------------------|
| `POLYGON_API_KEY`            | —       | required, your Polygon.io API key          |
| `SCAN_MIN_PRICE`             | `1.0`   | minimum stock price                        |
| `SCAN_MAX_PRICE`             | `20.0`  | maximum stock price                        |
| `SCAN_MIN_GAP_PERCENT`       | `2.0`   | minimum gap-up % to be listed              |
| `SCAN_MIN_VOLUME`            | `0`     | minimum volume to be listed                |
| `SCAN_POLL_INTERVAL_SECONDS` | `15`    | seconds between scans                      |
| `SCAN_TOP_N`                 | `20`    | how many rows to display                   |

## Code layout

- `com.edu.app.scanner.Quote` — one ticker's price/gap%/volume reading.
- `com.edu.app.scanner.MarketDataClient` — data source interface (swap in a
  different provider by implementing this).
- `com.edu.app.scanner.PolygonMarketDataClient` — the default Polygon.io
  implementation.
- `com.edu.app.scanner.GapRanker` — pure filter/sort logic (unit tested,
  no network).
- `com.edu.app.scanner.GapScanner` — the continuous polling loop.
- `com.edu.app.scanner.ConsoleLeaderboardRenderer` — redraws the ranked
  table in the terminal each cycle.
- `com.edu.app.App` — wires it all together.
