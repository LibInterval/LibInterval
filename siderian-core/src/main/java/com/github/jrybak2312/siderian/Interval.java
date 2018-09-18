package com.github.jrybak2312.siderian;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.jrybak2312.siderian.TemporalConverters.convertLowerEndpoint;
import static com.github.jrybak2312.siderian.TemporalConverters.convertUpperEndpoint;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Represents interval between two endpoints or in some case interval witch has gap(s).
 *
 * @param <T> - type which implements both Comparable and Temporal. So it supports such types
 *            like {@link LocalDate}, {@link YearMonth}...
 * @author Igor Rybak
 * @since 15-Sep-2018
 */
public interface Interval<T extends Comparable<?> & Temporal> {

    /**
     * Creates interval between two inclusive endpoints.
     *
     * @param lowerInclusiveEndpoint - lower inclusive endpoint. If null - no lower bound;
     * @param upperInclusiveEndpoint - upper inclusive endpoint. If null - no upper bound;
     */
    static <T extends Comparable<?> & Temporal> Interval<T> between(T lowerInclusiveEndpoint, T upperInclusiveEndpoint) {
        Range<T> range;
        if (lowerInclusiveEndpoint != null && upperInclusiveEndpoint != null) {
            range = Range.closed(lowerInclusiveEndpoint, upperInclusiveEndpoint);
        } else if (lowerInclusiveEndpoint != null) {
            range = Range.atLeast(lowerInclusiveEndpoint);
        } else if (upperInclusiveEndpoint != null) {
            range = Range.atMost(upperInclusiveEndpoint);
        } else {
            range = Range.all();
        }

        return new IntervalImpl<>(ImmutableRangeSet.of(range));
    }

    /**
     * Finds interval of intersection of several intervals.
     *
     * @param intervals - intervals to find intersection.
     * @return intersection of intervals or Optional.empty() if there is no intersection.
     */
    @SafeVarargs
    static <T extends Comparable<?> & Temporal> Interval<T> intersectionOf(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return IntervalUtils.intersection(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    static <T extends Comparable<?> & Temporal> Interval<T> intersectionOf(Iterable<Interval<T>> intervals) {
        return IntervalUtils.intersection(Streams.stream(intervals));
    }

    /**
     * Creates union of several intervals. E.g:
     * interval1 = [2018-03-15..2018-03-25],
     * interval2 = [2018-04-04..2018-04-14].
     * So in this case result is interval [2018-03-15..2018-04-14] with exclusive gap [2018-03-25..2018-04-04].
     *
     * @param intervals - intervals to create union;
     */
    @SafeVarargs
    static <T extends Comparable<?> & Temporal> Interval<T> unionOf(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return IntervalUtils.union(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    static <T extends Comparable<?> & Temporal> Interval<T> unionOf(Iterable<Interval<T>> intervals) {
        return IntervalUtils.union(Streams.stream(intervals));
    }

    static <T extends Comparable<?> & Temporal, V> Interval<T> unionOf(Function<V, Interval<T>> getIntervalFunction, Iterable<V> intervals) {
        return IntervalUtils.union(Streams.stream(intervals).map(getIntervalFunction));
    }

    static Interval<LocalDate> daysIntervalFromMonth(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return Interval.between(startDate, endDate);
    }

    static <T extends Comparable<?> & Temporal> Interval<T> all() {
        ImmutableRangeSet<T> all = ImmutableRangeSet.of(Range.all());
        return new IntervalImpl<>(all);
    }

    static <T extends Comparable<?> & Temporal> Interval<T> none() {
        return new IntervalImpl<>(ImmutableRangeSet.of());
    }

    /**
     * @return difference of two intervals.
     * e.g the result of difference [[2018-05-01..2018-05-10]] and [[2018-05-03..2018-05-06]]
     * is [[2018-05-01..2018-05-02], [2018-05-07..2018-05-10]]
     */
    Interval<T> difference(Interval<T> interval);

    Interval<T> difference(Interval<T> interval, TemporalUnit temporalUnit);

    Optional<T> lowerEndpoint();

    Optional<T> upperEndpoint();

    boolean contains(T t);

    boolean hasLowerBound();

    boolean hasUpperBound();

    Set<Interval<T>> subIntervals();

    default Interval<YearMonth> toMonthsInterval() {
        return map(t -> convertLowerEndpoint(t, YearMonth.class), t -> convertUpperEndpoint(t, YearMonth.class));
    }

    default Interval<LocalDate> toDaysInterval() {
        return map(t -> convertLowerEndpoint(t, LocalDate.class), t -> convertUpperEndpoint(t, LocalDate.class));
    }

    default Interval<LocalDateTime> toTimeInterval() {
        return map(t -> convertLowerEndpoint(t, LocalDateTime.class), t -> convertUpperEndpoint(t, LocalDateTime.class));
    }

    default <R extends Comparable<?> & Temporal> Interval<R> map(Function<T, R> mapper) {
        return map(mapper, mapper);
    }

    <R extends Comparable<?> & Temporal> Interval<R> map(Function<T, R> lowerEndpointMapper,
                                                         Function<T, R> upperEndpointMapper);

    default Stream<YearMonth> months() {
        return iterate(MONTHS, YearMonth::from);
    }

    default Stream<LocalDate> days() {
        return iterate(DAYS, LocalDate::from);
    }

    default <R extends Comparable<?> & Temporal> Stream<R> iterate(TemporalUnit temporalUnit,
                                                                   Function<T, R> mapper) {
        return iterate(temporalUnit, mapper, mapper);
    }

    <R extends Comparable<?> & Temporal> Stream<R> iterate(TemporalUnit temporalUnit,
                                                           Function<T, R> lowerEndpointMapper,
                                                           Function<T, R> upperEndpointMapper);

    default long countDays() {
        return count(DAYS);
    }

    long count(TemporalUnit temporalUnit);

    boolean isPresent();

    Optional<Interval<T>> notNoneInterval();

    ImmutableRangeSet<T> rangeSet();
}
