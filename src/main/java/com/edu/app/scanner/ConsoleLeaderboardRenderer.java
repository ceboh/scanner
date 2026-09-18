package com.edu.app.scanner;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Redraws an in-place ranked table in the terminal on every poll, so the
 * biggest gapper is always pinned to the top of the screen.
 */
public final class ConsoleLeaderboardRenderer implements LeaderboardRenderer {

    private static final String CLEAR_SCREEN = "\033[H\033[2J";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void render(List<Quote> leaderboard, String sessionLabel, ScannerConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(CLEAR_SCREEN);
        sb.append("GAP-UP SCANNER  |  session: ").append(sessionLabel)
                .append("  |  price $").append(config.minPrice()).append("-$").append(config.maxPrice())
                .append("  |  min gap ").append(config.minGapPercent()).append("%")
                .append("  |  updated ").append(LocalTime.now().format(TIME_FMT)).append('\n');
        sb.append("-".repeat(60)).append('\n');
        sb.append(String.format("%-4s %-8s %10s %10s %12s%n", "#", "SYMBOL", "PRICE", "GAP %", "VOLUME"));
        sb.append("-".repeat(60)).append('\n');

        if (leaderboard.isEmpty()) {
            sb.append("(no gappers matching the current filters right now)\n");
        } else {
            int rank = 1;
            for (Quote q : leaderboard) {
                sb.append(String.format("%-4d %-8s %10.2f %9.2f%% %12d%n",
                        rank++, q.symbol(), q.price(), q.changePercent(), q.volume()));
            }
        }

        System.out.print(sb);
        System.out.flush();
    }

    @Override
    public void renderError(String message) {
        System.out.println("[scanner] error: " + message + " (will retry on next poll)");
    }
}
