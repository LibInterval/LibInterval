package com.github.libinterval;

import com.google.common.collect.DiscreteDomain;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

/**
 *  @author Igor Rybak
 *  @since 11-Sep-2018
 */
public class CustomComparableTemporalDiscreteDomain<T extends Comparable<?> & Temporal> extends DiscreteDomain<T> {

    private final TemporalUnit unit;

    CustomComparableTemporalDiscreteDomain(TemporalUnit unit) {
        this.unit = unit;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T next(T value) {
        return (T) value.plus(1, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T previous(T value) {
        return (T) value.minus(1, unit);
    }

    @Override
    public long distance(T start, T end) {
        return unit.between(start, end);
    }
}
