package com.github.jrybak2312.libinterval;

import com.google.common.collect.DiscreteDomain;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

/**
 * @author Igor Rybak
 * @since 09-May-2018
 */
class DefaultComparableTemporalDiscreteDomain<T extends Comparable<?> & Temporal> extends DiscreteDomain<T> {
    private final static DefaultComparableTemporalDiscreteDomain INSTANCE = new DefaultComparableTemporalDiscreteDomain();

    private DefaultComparableTemporalDiscreteDomain() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<?> & Temporal> DefaultComparableTemporalDiscreteDomain<T> instance() {
        return (DefaultComparableTemporalDiscreteDomain<T>) INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T next(T value) {
        return (T) value.plus(1, getUnit(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T previous(T value) {
        return (T) value.minus(1, getUnit(value));
    }

    @Override
    public long distance(T start, T end) {
        return getUnit(start).between(start, end);
    }

    private TemporalUnit getUnit(T value) {
        return Optional.ofNullable(value.query(TemporalQueries.precision()))
                .orElseThrow(() -> new UnsupportedOperationException(getMessage(value.getClass())));
    }

    private String getMessage(Class<?> type) {
        return "No default temporal unit for " + type.getName() + ". " +
                "Use Interval.difference(Interval<T>, java.time.temporal.TemporalUnit) to provide temporal unit.";
    }
}
