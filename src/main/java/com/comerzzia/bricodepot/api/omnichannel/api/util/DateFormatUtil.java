package com.comerzzia.bricodepot.api.omnichannel.api.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility helpers for dealing with date values that might arrive in different formats
 * from omnichannel services. Jasper templates often receive values typed as Object or
 * String so we need a central place to coerce them into {@link java.util.Date} safely.
 */
public final class DateFormatUtil {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private static final String[] DEFAULT_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    };

    private static final String[] ENGLISH_PATTERNS = {
        "EEE MMM dd HH:mm:ss zzz yyyy"
    };

    private DateFormatUtil() {
        // utility class
    }

    /**
     * Converts the provided value into a {@link Date} instance when possible.
     * Supports {@link Date}, {@link Calendar}, {@link Instant}, {@link LocalDateTime},
     * {@link OffsetDateTime}, {@link ZonedDateTime}, {@link LocalDate}, {@link Number}
     * (treated as epoch milliseconds) and {@link CharSequence} representations using
     * a set of known patterns.
     *
     * @param value value to convert, may be {@code null}
     * @return a {@link Date} instance or {@code null} when the value cannot be converted
     */
    public static Date toDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }

        if (value instanceof Instant) {
            return Date.from((Instant) value);
        }

        if (value instanceof OffsetDateTime) {
            return Date.from(((OffsetDateTime) value).toInstant());
        }

        if (value instanceof ZonedDateTime) {
            return Date.from(((ZonedDateTime) value).toInstant());
        }

        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(DEFAULT_ZONE).toInstant());
        }

        if (value instanceof LocalDate) {
            return Date.from(((LocalDate) value).atStartOfDay(DEFAULT_ZONE).toInstant());
        }

        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }

        if (value instanceof CharSequence) {
            return parseFromString(value.toString().trim());
        }

        return null;
    }

    /**
     * Formats the provided value into a string using the supplied pattern. Returns
     * {@code null} when the value cannot be parsed so Jasper can keep the field blank.
     *
     * @param value   value to format
     * @param pattern formatting pattern compatible with {@link SimpleDateFormat}
     * @return formatted string or {@code null} if the value cannot be converted
     */
    public static String formatDate(Object value, String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        Date date = toDate(value);
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(pattern).format(date);
    }

    private static Date parseFromString(String text) {
        if (text.isEmpty()) {
            return null;
        }

        // Try numeric timestamps first
        try {
            long epochMillis = Long.parseLong(text);
            return new Date(epochMillis);
        } catch (NumberFormatException ignored) {
            // continue with pattern parsing
        }

        for (String pattern : DEFAULT_PATTERNS) {
            Date parsed = tryParseWithPattern(text, pattern, Locale.getDefault());
            if (parsed != null) {
                return parsed;
            }
        }

        for (String pattern : ENGLISH_PATTERNS) {
            Date parsed = tryParseWithPattern(text, pattern, Locale.ENGLISH);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private static Date tryParseWithPattern(String text, String pattern, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
        simpleDateFormat.setLenient(true);
        try {
            return simpleDateFormat.parse(text);
        } catch (ParseException e) {
            return null;
        }
    }
}
