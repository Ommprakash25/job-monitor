package com.jobmonitor.masterdata;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Roughly how many emails fit in a sending day. The first email goes out at a random
 * time in the first half hour, then each gap is random between the minimum and maximum,
 * so on average a gap is the midpoint of the two.
 */
public record SendingCapacity(int emailsThatFit, String summary) {

    static final int RANDOM_START_MINUTES = 30;
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    public static SendingCapacity estimate(LocalTime start, LocalTime end, Integer gapMin, Integer gapMax,
                                           Integer dailyLimit) {
        if (start == null || end == null || gapMin == null || gapMax == null
                || !start.isBefore(end) || gapMin < 1 || gapMax < gapMin) {
            return new SendingCapacity(0, "Set the hours and the gap to see how many emails fit in a day.");
        }
        long window = Duration.between(start, end).toMinutes();
        double averageGap = (gapMin + gapMax) / 2.0;
        double averageStartDelay = Math.min(RANDOM_START_MINUTES, window) / 2.0;
        int fit = (int) Math.floor((window - averageStartDelay) / averageGap) + 1;

        String fits = "About " + fit + (fit == 1 ? " email fits" : " emails fit") + " in these hours";
        if (dailyLimit == null || dailyLimit < 1) {
            return new SendingCapacity(fit, fits + ".");
        }
        if (dailyLimit > fit) {
            return new SendingCapacity(fit, fits + ", so a limit of " + dailyLimit + " won't be reached.");
        }
        if (dailyLimit == fit) {
            return new SendingCapacity(fit, fits + ", the same as your limit.");
        }
        long minutes = Math.round(averageStartDelay + (dailyLimit - 1) * averageGap);
        LocalTime reached = start.plusMinutes(minutes);
        return new SendingCapacity(fit, fits + ". A limit of " + dailyLimit
                + " is usually reached around " + reached.format(HOUR_MINUTE) + ".");
    }
}
