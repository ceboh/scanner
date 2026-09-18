package com.edu.app.scanner;

import java.util.List;

public interface LeaderboardRenderer {

    void render(List<Quote> leaderboard, String sessionLabel, ScannerConfig config);

    void renderError(String message);
}
