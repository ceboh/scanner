# aib — Gap-Up Stock Scanner

Continuously scans the US stock market — pre-market and regular trading
hours alike — and keeps a live, ranked list of $1–$20 stocks that are
gapping up, biggest percentage move always on top.

## How it works

Supports two data providers, selected with `MARKET_DATA_PROVIDER`
(default: `alpaca`):

- **Alpaca** (default) — each poll cycle calls Alpaca's
  `GET /v1beta1/screener/stocks/movers`, which is Alpaca's own server-side
  ranking of the market's biggest gainers by percent change. This is a
  single bulk request; Alpaca does the market-wide ranking for you.

  **Trade-off:** this endpoint returns only Alpaca's top ~50 movers
  market-wide. If the very largest movers on a given day are priced outside
  $1–$20, some in-range gappers further down the true ranking won't appear
  in that top 50. There's no way around this short of Alpaca's paid
  full-snapshot data. Extended-hours (pre-market/after-hours) coverage also
  depends on your Alpaca market data subscription/feed (IEX vs SIP).

- **Polygon** — `GET /v2/snapshot/locale/us/markets/stocks/tickers`, a full
  snapshot of every US ticker in one request. Polygon's `todaysChangePerc`
  is computed against the previous session's close, so it doubles as the
  "gap" figure in pre-market, regular hours, or after-hours alike. No
  top-N cap, but real-time extended-hours data needs a paid Polygon plan.

Each cycle, regardless of provider:
1. Fetches the snapshot from the selected provider.
2. Filters to tickers priced between `SCAN_MIN_PRICE` and `SCAN_MAX_PRICE`
   (defaults: $1–$20).
3. Drops anything gapping up less than `SCAN_MIN_GAP_PERCENT` (default 2%)
   or below `SCAN_MIN_VOLUME` (default: no volume filter).
4. Sorts by percentage gap descending and redraws the top `SCAN_TOP_N`
   (default 20) in the terminal.

A failed poll (network hiccup, rate limit) is logged and skipped — the
scanner keeps running rather than exiting.

## Setup (Alpaca — default)

1. Get API keys at https://app.alpaca.markets/paper/dashboard/overview.
   Paper trading keys work fine — this only reads market data, it never
   places trades.

2. Export them:

   ```bash
   export ALPACA_API_KEY_ID=your_key_id
   export ALPACA_API_SECRET_KEY=your_secret_key
   ```

3. Build and run:

   ```bash
   mvn clean package
   java -jar target/aib-1.0-shaded.jar
   ```

Stop with `Ctrl+C`.

## Setup (Polygon — alternative)

```bash
export MARKET_DATA_PROVIDER=polygon
export POLYGON_API_KEY=your_key_here
mvn clean package
java -jar target/aib-1.0-shaded.jar
```

Get a free key at https://polygon.io/dashboard/api-keys. The free tier
serves delayed data; live pre-market/after-hours snapshots require a paid
plan (e.g. "Stocks Starter" or higher).

## Configuration

All optional, set as environment variables:

| Variable                     | Default   | Meaning                                            |
|-------------------------------|-----------|-----------------------------------------------------|
| `MARKET_DATA_PROVIDER`        | `alpaca`  | `alpaca` or `polygon`                                |
| `ALPACA_API_KEY_ID`           | —         | required if provider is `alpaca`                     |
| `ALPACA_API_SECRET_KEY`       | —         | required if provider is `alpaca`                     |
| `POLYGON_API_KEY`             | —         | required if provider is `polygon`                    |
| `SCAN_MIN_PRICE`              | `1.0`     | minimum stock price                                  |
| `SCAN_MAX_PRICE`              | `20.0`    | maximum stock price                                  |
| `SCAN_MIN_GAP_PERCENT`        | `2.0`     | minimum gap-up % to be listed                        |
| `SCAN_MIN_VOLUME`             | `0`       | minimum volume to be listed                          |
| `SCAN_POLL_INTERVAL_SECONDS`  | `15`      | seconds between scans                                |
| `SCAN_TOP_N`                  | `20`      | how many rows to display                             |

## Code layout

- `com.edu.app.scanner.Quote` — one ticker's price/gap%/volume reading.
- `com.edu.app.scanner.MarketDataClient` — data source interface (swap in a
  different provider by implementing this).
- `com.edu.app.scanner.AlpacaMarketDataClient` — the default Alpaca
  implementation.
- `com.edu.app.scanner.PolygonMarketDataClient` — the Polygon alternative.
- `com.edu.app.scanner.GapRanker` — pure filter/sort logic (unit tested,
  no network).
- `com.edu.app.scanner.GapScanner` — the continuous polling loop.
- `com.edu.app.scanner.ConsoleLeaderboardRenderer` — redraws the ranked
  table in the terminal each cycle.
- `com.edu.app.App` — wires it all together.
