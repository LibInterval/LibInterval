package com.github.jrybak2312.siderian;

import com.google.common.collect.DiscreteDomain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

/**
 * @author Igor Rybak
 * @since 09-May-2018
 */
@SuppressWarnings("unchecked")
class ComparableTemporalDiscreteDomain<T extends Comparable<?> & Temporal> extends DiscreteDomain<T> {
    private static ComparableTemporalDiscreteDomain INSTANCE = new ComparableTemporalDiscreteDomain();

    public static <T extends Comparable<?> & Temporal> ComparableTemporalDiscreteDomain<T> instance() {
        return (ComparableTemporalDiscreteDomain<T>) INSTANCE;
    }

    @Override
    public T next(T value) {
        if (value instanceof LocalDate) {
            return (T) ((LocalDate) value).plusDays(1);
        }

        if (value instanceof YearMonth) {
            return (T) ((YearMonth) value).plusMonths(1);
        }

        throw new UnsupportedOperationException(getMessage(value.getClass()));
    }

    @Override
    public T previous(T value) {
        if (value instanceof LocalDate) {
            return (T) ((LocalDate) value).minusDays(1);
        }

        if (value instanceof YearMonth) {
            return (T) ((YearMonth) value).minusMonths(1);
        }

        throw new UnsupportedOperationException(getMessage(value.getClass()));
    }

    @Override
    public long distance(T start, T end) {
        if (start instanceof LocalDate) {
            return ChronoUnit.DAYS.between(start, end);
        }

        if (start instanceof YearMonth) {
            return ChronoUnit.MONTHS.between(start, end);
        }

        throw new UnsupportedOperationException(getMessage(start.getClass()));
    }

    private String getMessage(Class<?> type) {
        return "The type " + type.getName() + " is not supported.";
    }
}
