package com.github.jrybak2312.libinterval;

import com.google.common.collect.ImmutableRangeSet;

import java.time.temporal.Temporal;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * @author Igor Rybak
 * @since 15-Sep-2018
 */
class IntervalUtils {

    static <T extends Comparable<?> & Temporal> Interval<T> intersection(Stream<Interval<T>> intervals) {
        ImmutableRangeSet<T> rangeSet = intervals.map(Interval::rangeSet)
                .map(ImmutableRangeSet::copyOf)
                .reduce(ImmutableRangeSet::intersection)
                .orElseThrow(IllegalArgumentException::new);
        return new IntervalImpl<>(rangeSet);
    }

    static <T extends Comparable<?> & Temporal> Interval<T> union(Stream<Interval<T>> intervals) {
        ImmutableRangeSet<T> rangeSet = intervals.flatMap(i -> i.rangeSet().asRanges().stream())
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::unionOf));

        return new IntervalImpl<>(rangeSet);
    }

    static <T extends Comparable<?> & Temporal> IllegalStateException newInvalidLowerBoundException(Interval<T> interval) {
        return new IllegalStateException("The interval " + interval + " doesn't have lower bound.");
    }

    static <T extends Comparable<?> & Temporal> IllegalStateException newInvalidUpperBoundException(Interval<T> interval) {
        return new IllegalStateException("The interval " + interval + " doesn't have upper bound.");
    }

    private IntervalUtils() {
    }
}
