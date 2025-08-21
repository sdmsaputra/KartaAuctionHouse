package com.minekartastudio.kartaauctionhouse.util;

import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    private TimeUtil() {}

    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return "Expired";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 && days == 0) { // Only show minutes if duration is less than a day
            sb.append(minutes).append("m ");
        }
        if (hours == 0 && days == 0) { // Only show seconds if duration is less than an hour
            sb.append(seconds).append("s");
        }

        if (sb.length() == 0) {
            // This case might happen for very long durations where we don't show smaller units
            // e.g., for "1d 1h", we don't show minutes or seconds. Let's refine.
            // A simpler approach:
            if (days > 0) return String.format("%dd %dh", days, hours);
            if (hours > 0) return String.format("%dh %dm", hours, minutes);
            if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
            return String.format("%ds", seconds);
        }

        return sb.toString().trim();
    }
}
