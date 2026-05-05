package com.musinsa.freepoint.common.time;

import java.time.LocalDateTime;

public class TestTimeProvider implements TimeProvider {

    public static final LocalDateTime DEFAULT_BASE_TIME = LocalDateTime.of(2026, 6, 1, 12, 0);

    private LocalDateTime now;

    public TestTimeProvider() {
        this(DEFAULT_BASE_TIME);
    }

    public TestTimeProvider(LocalDateTime initial) {
        this.now = initial;
    }

    @Override
    public LocalDateTime now() {
        return now;
    }

    public void setNow(LocalDateTime now) {
        this.now = now;
    }

    public void advanceDays(long days) {
        this.now = this.now.plusDays(days);
    }
}
