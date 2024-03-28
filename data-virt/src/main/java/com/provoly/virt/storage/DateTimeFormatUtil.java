package com.provoly.virt.storage;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * DateTimeFormatter compatible with elastic (version 8.4) date format.<br>
 * see
 * https://github.com/elastic/elasticsearch-java/blob/8.4/java-client/src/main/java/co/elastic/clients/util/DateTimeUtil.java
 */
public class DateTimeFormatUtil {

    private static final DateTimeFormatter TIME_ZONE_FORMATTER_NO_COLON = new DateTimeFormatterBuilder()
            .appendOffset("+HHmm", "Z")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter STRICT_YEAR_MONTH_DAY_FORMATTER = new DateTimeFormatterBuilder().appendValue(
            ChronoField.YEAR,
            4,
            4,
            SignStyle.EXCEEDS_PAD)
            .optionalStart()
            .appendLiteral("-")
            .appendValue(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE)
            .optionalEnd()
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    public static final DateTimeFormatter STRICT_DATE_OPTIONAL_TIME_FORMATTER = new DateTimeFormatterBuilder().append(
            STRICT_YEAR_MONTH_DAY_FORMATTER)
            .optionalStart()
            .appendLiteral('T')
            .optionalStart()
            .appendValue(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendLiteral(',')
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
            .optionalEnd()
            .optionalEnd()
            .optionalEnd()
            .optionalStart()
            .appendZoneOrOffsetId()
            .optionalEnd()
            .optionalStart()
            .append(TIME_ZONE_FORMATTER_NO_COLON)
            .optionalEnd()
            .optionalEnd()
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
}