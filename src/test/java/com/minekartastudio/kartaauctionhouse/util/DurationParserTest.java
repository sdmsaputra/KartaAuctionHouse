package com.minekartastudio.kartaauctionhouse.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

class DurationParserTest {

    @Test
    void testSingleUnits() {
        assertEquals(Optional.of(TimeUnit.SECONDS.toMillis(1)), DurationParser.parse("1s"));
        assertEquals(Optional.of(TimeUnit.MINUTES.toMillis(30)), DurationParser.parse("30m"));
        assertEquals(Optional.of(TimeUnit.HOURS.toMillis(12)), DurationParser.parse("12h"));
        assertEquals(Optional.of(TimeUnit.DAYS.toMillis(3)), DurationParser.parse("3d"));
    }

    @Test
    void testCombinedUnits() {
        long expected = TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(12);
        assertEquals(Optional.of(expected), DurationParser.parse("1d12h"));
    }

    @Test
    void testUppercaseAndWhitespace() {
        assertEquals(Optional.of(TimeUnit.HOURS.toMillis(1)), DurationParser.parse("1H"));
        assertEquals(Optional.of(TimeUnit.HOURS.toMillis(1)), DurationParser.parse(" 1h "));
    }

    @Test
    void testInvalidFormats() {
        assertTrue(DurationParser.parse("").isEmpty());
        assertTrue(DurationParser.parse("abc").isEmpty());
        assertTrue(DurationParser.parse("1").isEmpty());
        assertTrue(DurationParser.parse("s1").isEmpty());
        assertTrue(DurationParser.parse("1y").isEmpty());
        assertTrue(DurationParser.parse(null).isEmpty());
    }

    @Test
    void testZeroValue() {
        assertEquals(Optional.of(0L), DurationParser.parse("0s"));
        assertEquals(Optional.of(0L), DurationParser.parse("0d"));
    }
}
