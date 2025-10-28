package com.minekarta.playerauction.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

public final class DurationParser {

    // Updated pattern to support multiple segments like "1d12h"
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");

    private DurationParser() {}

    /**
     * Parses a duration string (e.g., "1d", "12h", "30m") into milliseconds.
     *
     * @param durationStr The string to parse.
     * @return An Optional containing the duration in milliseconds if parsing is successful, otherwise an empty Optional.
     */
    public static Optional<Long> parse(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = DURATION_PATTERN.matcher(durationStr.toLowerCase());
        long totalMillis = 0;
        boolean foundMatch = false;

        while (matcher.find()) {
            foundMatch = true;
            try {
                int value = Integer.parseInt(matcher.group(1));
                char unit = matcher.group(2).charAt(0);
                switch (unit) {
                    case 's' -> totalMillis += TimeUnit.SECONDS.toMillis(value);
                    case 'm' -> totalMillis += TimeUnit.MINUTES.toMillis(value);
                    case 'h' -> totalMillis += TimeUnit.HOURS.toMillis(value);
                    case 'd' -> totalMillis += TimeUnit.DAYS.toMillis(value);
                    default -> { return Optional.empty(); } // Should not be reached due to regex
                }
            } catch (NumberFormatException e) {
                // Value too large to parse
                return Optional.empty();
            }
        }

        return foundMatch ? Optional.of(totalMillis) : Optional.empty();
    }
}
