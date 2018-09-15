package com.github.jrybak2312.siderian;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import java.time.temporal.Temporal;

import static com.google.common.collect.BoundType.OPEN;

/**
 *  @author Igor Rybak
 *  @since 11-Sep-2018
 */
class RangeConverter<T extends Comparable<?> & Temporal> {
    private final static RangeConverter<?> INSTANCE = new RangeConverter<>(DefaultComparableTemporalDiscreteDomain.instance());

    private final DiscreteDomain<T> domain;

    @SuppressWarnings("unchecked")
    static <T extends Comparable<?> & Temporal> RangeConverter<T> defaultInstance() {
        return (RangeConverter<T>) INSTANCE;
    }

    RangeConverter(DiscreteDomain<T> discreteDomain) {
        this.domain = discreteDomain;
    }

    boolean canBeConvertedToClosed(Range<T> range) {
        if (range.hasLowerBound() && range.hasUpperBound()) {
            T closedLowerEndpoint = getClosedLowerEndpoint(range);
            T closedUpperEndpoint = getClosedUpperEndpoint(range);
            return domain.distance(closedLowerEndpoint, closedUpperEndpoint) > -1;
        } else {
            return true;
        }
    }

    Range<T> convertToClosed(Range<T> range) {
        Range<T> result;
        if (range.hasLowerBound() && range.hasUpperBound()) {
            result = Range.closed(getClosedLowerEndpoint(range), getClosedUpperEndpoint(range));
        } else if (range.hasLowerBound()) {
            result = Range.atLeast(getClosedLowerEndpoint(range));
        } else if (range.hasUpperBound()) {
            result = Range.atMost(getClosedUpperEndpoint(range));
        } else {
            result = Range.all();
        }
        return result;
    }

    private T getClosedUpperEndpoint(Range<T> range) {
        T upperEndpoint;
        if (range.upperBoundType().equals(OPEN)) {
            upperEndpoint = domain.previous(range.upperEndpoint());
        } else {
            upperEndpoint = range.upperEndpoint();
        }
        return upperEndpoint;
    }

    private T getClosedLowerEndpoint(Range<T> range) {
        T lowerEndpoint;
        if (range.lowerBoundType().equals(OPEN)) {
            lowerEndpoint = domain.next(range.lowerEndpoint());
        } else {
            lowerEndpoint = range.lowerEndpoint();
        }
        return lowerEndpoint;
    }
}
