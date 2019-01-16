package com.github.jrybak2312.libinterval;

import com.google.common.collect.ImmutableMap;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;
import java.util.Map;
import java.util.Optional;

/**
 * @author Igor Rybak
 * @since 16-Sep-2018
 */
class TemporalConverters {

    private final static Map<Class<?>, TemporalConverter<?>> converters = ImmutableMap.<Class<?>, TemporalConverter<?>>builder()
            .put(YearMonth.class, new YearMonthTemporalConverter())
            .put(LocalDate.class, new LocalDateTemporalConverter())
            .put(LocalDateTime.class, new LocalDateTimeTemporalConverter())
            .build();

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<?> & Temporal, R> R convertLowerEndpoint(T lowerEndpoint, Class<R> type) {
        return (R) Optional.ofNullable(converters.get(type))
                .map(converter -> converter.convertToLowerEndpoint(lowerEndpoint))
                .orElseThrow(() -> new IllegalArgumentException("Type " + type + " is not supported."));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<?> & Temporal, R> R convertUpperEndpoint(T upperEndpoint, Class<R> type) {
        return (R) Optional.ofNullable(converters.get(type))
                .map(converter -> converter.convertToUpperEndpoint(upperEndpoint))
                .orElseThrow(() -> new IllegalArgumentException("Type " + type + " is not supported."));
    }

    private interface TemporalConverter<V> {
        V convertToLowerEndpoint(Temporal temporal);

        V convertToUpperEndpoint(Temporal temporal);
    }

    private static class LocalDateTemporalConverter implements TemporalConverter<LocalDate> {

        @Override
        public LocalDate convertToLowerEndpoint(Temporal temporal) {
            LocalDate result;
            if (temporal instanceof Year) {
                result = ((Year) temporal).atDay(1);
            } else if (temporal instanceof YearMonth) {
                result = ((YearMonth) temporal).atDay(1);
            } else {
                result = tryToConvertToLocalDate(temporal);
            }
            return result;
        }

        @Override
        public LocalDate convertToUpperEndpoint(Temporal temporal) {
            LocalDate result = temporal.query(TemporalQueries.localDate());
            if (result == null) {
                if (temporal instanceof Year) {
                    result = atEndOfYear((Year) temporal);
                } else if (temporal instanceof YearMonth) {
                    result = ((YearMonth) temporal).atEndOfMonth();
                } else {
                    result = tryToConvertToLocalDate(temporal);
                }
            }
            return result;
        }

        private LocalDate tryToConvertToLocalDate(Temporal temporal) {
            try {
                return LocalDate.from(temporal);
            } catch (DateTimeException e) {
                throw newUnsupportedTypeException(temporal, LocalDate.class, e);
            }
        }
    }

    private static class YearMonthTemporalConverter implements TemporalConverter<YearMonth> {
        @Override
        public YearMonth convertToLowerEndpoint(Temporal temporal) {
            YearMonth result;
            if (temporal instanceof Year) {
                return ((Year) temporal).atMonth(1);
            } else {
                result = tryToConvertToYearMonth(temporal);
            }

            return result;
        }

        @Override
        public YearMonth convertToUpperEndpoint(Temporal temporal) {
            YearMonth result;
            if (temporal instanceof Year) {
                return ((Year) temporal).atMonth(12);
            } else {
                result = tryToConvertToYearMonth(temporal);
            }

            return result;
        }

        private YearMonth tryToConvertToYearMonth(Temporal temporal) {
            try {
                return YearMonth.from(temporal);
            } catch (DateTimeException e) {
                throw newUnsupportedTypeException(temporal, YearMonth.class, e);
            }
        }
    }

    private static class LocalDateTimeTemporalConverter implements TemporalConverter<LocalDateTime> {
        @Override
        public LocalDateTime convertToLowerEndpoint(Temporal temporal) {
            LocalDateTime result;

            if (temporal instanceof Year) {
                result = ((Year) temporal).atDay(1).atStartOfDay();
            } else if (temporal instanceof YearMonth) {
                result = ((YearMonth) temporal).atDay(1).atStartOfDay();
            } else if (temporal instanceof LocalDate) {
                result = ((LocalDate) temporal).atStartOfDay();
            } else {
                result = tryToConvertToLocalDateTime(temporal);
            }
            return result;
        }

        @Override
        public LocalDateTime convertToUpperEndpoint(Temporal temporal) {
            LocalDateTime result;

            if (temporal instanceof Year) {
                result = atEndOfYear((Year) temporal).atTime(LocalTime.MAX);
            } else if (temporal instanceof YearMonth) {
                result = ((YearMonth) temporal).atEndOfMonth().atTime(LocalTime.MAX);
            } else if (temporal instanceof LocalDate) {
                result = ((LocalDate) temporal).atTime(LocalTime.MAX);
            } else {
                result = tryToConvertToLocalDateTime(temporal);
            }

            return result;
        }

        private LocalDateTime tryToConvertToLocalDateTime(Temporal temporal) {
            try {
                return LocalDateTime.from(temporal);
            } catch (DateTimeException e) {
                throw newUnsupportedTypeException(temporal, LocalDateTime.class, e);
            }
        }
    }

    private static LocalDate atEndOfYear(Year temporal) {
        return temporal.atMonth(Month.DECEMBER).atEndOfMonth();
    }

    private static UnsupportedOperationException newUnsupportedTypeException(Temporal temporal, Class<?> type, Throwable cause) {
        throw new UnsupportedOperationException(temporal.getClass() + " is not supported to convert to " + type, cause);
    }

    private TemporalConverters() {
    }
}
