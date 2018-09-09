package com.github.jrybak2312.siderian;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * Represents interval between two endpoints or in some case interval witch has gap(s).
 *
 * @param <T> - type which implements both Comparable and Temporal. So it supports such types
 *            like {@link LocalDate}, {@link YearMonth}...
 * @author Igor Rybak
 * @since 15.11.2017
 */
public class Interval<T extends Comparable<?> & Temporal> {
    private static Logger logger = LoggerFactory.getLogger(Interval.class);

    private final ImmutableRangeSet<T> rangeSet;

    /**
     * Creates interval between two inclusive endpoints.
     *
     * @param lowerInclusiveEndpoint - lower inclusive endpoint. If null - no lower bound;
     * @param upperInclusiveEndpoint - upper inclusive endpoint. If null - no upper bound;
     */
    public static <T extends Comparable<?> & Temporal> Interval<T> between(T lowerInclusiveEndpoint, T upperInclusiveEndpoint) {
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

        return new Interval<>(ImmutableRangeSet.of(range));
    }

    /**
     * Finds interval of intersection of several intervals.
     *
     * @param intervals - intervals to find intersection.
     * @return intersection of intervals or Optional.empty() if there is no intersection.
     */
    @SafeVarargs
    public static <T extends Comparable<?> & Temporal> Optional<Interval<T>> intersection(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return intersection(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    public static <T extends Comparable<?> & Temporal> Optional<Interval<T>> intersection(Iterable<Interval<T>> intervals) {
        return intersection(Streams.stream(intervals));
    }

    private static <T extends Comparable<?> & Temporal> Optional<Interval<T>> intersection(Stream<Interval<T>> intervals) {
        ImmutableRangeSet<T> rangeSet = intervals.map(Interval::getRangeSet)
                .map(ImmutableRangeSet::copyOf)
                .reduce(ImmutableRangeSet::intersection)
                .orElseThrow(IllegalArgumentException::new);
        return rangeSet.isEmpty() ? Optional.empty() : Optional.of(new Interval<>(rangeSet));
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
    public static <T extends Comparable<?> & Temporal> Interval<T> union(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return union(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    public static <T extends Comparable<?> & Temporal> Interval<T> union(Iterable<Interval<T>> intervals) {
        return union(Streams.stream(intervals));
    }

    public static <T extends Comparable<?> & Temporal, V> Interval<T> union(Function<V, Interval<T>> getIntervalFunction, Iterable<V> intervals) {
        return union(Streams.stream(intervals).map(getIntervalFunction));
    }

    private static <T extends Comparable<?> & Temporal> Interval<T> union(Stream<Interval<T>> intervals) {
        ImmutableRangeSet<T> rangeSet = intervals.flatMap(i -> i.getRangeSet().asRanges().stream())
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::unionOf));

        return new Interval<>(rangeSet);
    }

    public static Interval<LocalDate> fromMonth(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return Interval.between(startDate, endDate);
    }

    public static <T extends Comparable<?> & Temporal> Interval<T> none() {
        return new Interval<>(ImmutableRangeSet.of());
    }

    private Interval(ImmutableRangeSet<T> rangeSet) {
        this.rangeSet = rangeSet;
    }

    /**
     * @return difference of two intervals.
     * e.g the result of difference [[2018-05-01..2018-05-10]] and [[2018-05-03..2018-05-06]]
     * is [[2018-05-01..2018-05-02], [2018-05-07..2018-05-10]]
     */
    public Interval<T> difference(Interval<T> interval) {
        logger.debug("Finding difference of " + this + " and " + interval);
        ImmutableRangeSet<T> difference = this.rangeSet.difference(interval.rangeSet);
        ImmutableRangeSet<T> closedRangeSet = difference.asRanges().stream()
                .filter(range -> {
                    if (range.hasLowerBound() && range.hasUpperBound()) {
                        T closedLowerEndpoint = getClosedLowerEndpoint(range);
                        T closedUpperEndpoint = getClosedUpperEndpoint(range);
                        return ComparableTemporalDiscreteDomain.instance().distance(closedLowerEndpoint, closedUpperEndpoint) > -1;
                    } else {
                        return true;
                    }
                })
                .map(range -> {
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
                })
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::copyOf));

        return new Interval<>(closedRangeSet);
    }

    private T getClosedUpperEndpoint(Range<T> range) {
        ComparableTemporalDiscreteDomain<T> domain = ComparableTemporalDiscreteDomain.instance();
        T upperEnpoint;
        if (range.upperBoundType().equals(BoundType.OPEN)) {
            upperEnpoint = domain.previous(range.upperEndpoint());
        } else {
            upperEnpoint = range.upperEndpoint();
        }
        return upperEnpoint;
    }

    private T getClosedLowerEndpoint(Range<T> range) {
        ComparableTemporalDiscreteDomain<T> domain = ComparableTemporalDiscreteDomain.instance();
        T lowerEnpoint;
        if (range.lowerBoundType().equals(BoundType.OPEN)) {
            lowerEnpoint = domain.next(range.lowerEndpoint());
        } else {
            lowerEnpoint = range.lowerEndpoint();
        }
        return lowerEnpoint;
    }


    public Optional<T> getLowerEndpoint() {
        return getRange().hasLowerBound() ? Optional.of(getRange().lowerEndpoint()) : Optional.empty();
    }

    public Optional<T> getUpperEndpoint() {
        return getRange().hasUpperBound() ? Optional.of(getRange().upperEndpoint()) : Optional.empty();
    }

    public boolean contains(T t) {
        return rangeSet.contains(t);
    }

    public boolean hasLowerBound() {
        return getRange().hasLowerBound();
    }

    public boolean hasUpperBound() {
        return getRange().hasUpperBound();
    }

    public Set<Interval<T>> getSubIntervals() {
        return rangeSet.asRanges().stream()
                .map(ImmutableRangeSet::of)
                .map((Function<ImmutableRangeSet<T>, Interval<T>>) Interval::new)
                .collect(toSet());
    }

    public Interval<YearMonth> toMonthInterval() {
        ImmutableRangeSet<YearMonth> rangeSet = getSubIntervals().stream()
                .map(i -> {
                    YearMonth start = i.getLowerEndpoint().map(YearMonth::from).orElse(null);
                    YearMonth end = i.getUpperEndpoint().map(YearMonth::from).orElse(null);
                    return Interval.between(start, end);
                })
                .map(Interval::getRangeSet)
                .map(RangeSet::asRanges)
                .flatMap(Set::stream)
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::unionOf));

        return new Interval<>(rangeSet);
    }

    public Stream<YearMonth> months() {
        return getSubIntervals().stream()
                .flatMap(subInterval -> {
                    YearMonth start = subInterval.getConvertedLowerEndpoint(YearMonth::from);
                    YearMonth end = subInterval.getConvertedUpperEndpoint(YearMonth::from);

                    return Stream.iterate(start, date -> date.plusMonths(1))
                            .limit(MONTHS.between(start, end) + 1);
                })
                .distinct()
                .sorted();
    }

    public Stream<LocalDate> days() {
        return getSubIntervals().stream()
                .flatMap(subInterval -> {
                    LocalDate start = subInterval.getConvertedLowerEndpoint(LocalDate::from);
                    LocalDate end = subInterval.getConvertedUpperEndpoint(LocalDate::from);

                    return Stream.iterate(start, date -> date.plusDays(1))
                            .limit(DAYS.between(start, end) + 1);
                })
                .distinct()
                .sorted();
    }

    public long getDaysCount() {
        return rangeSet.asRanges().stream()
                .mapToLong(range -> DAYS.between(range.lowerEndpoint(), range.upperEndpoint()) + 1)
                .sum();
    }

    ImmutableRangeSet<T> getRangeSet() {
        return rangeSet;
    }

    private Range<T> getRange() {
        Set<Range<T>> ranges = rangeSet.asRanges();
        if (ranges.size() > 1) {
            throw new IllegalStateException("The interval has more than one sub intervals: " + this + ".");
        }

        return ranges.stream().findFirst().orElseThrow(() -> new IllegalStateException("The interval is empty."));
    }

    private <R> R getConvertedLowerEndpoint(Function<T, R> converter) {
        return this.getLowerEndpoint()
                .map(converter)
                .orElseThrow(this::newInvalidLowerBoundException);
    }

    private <R> R getConvertedUpperEndpoint(Function<T, R> converter) {
        return this.getUpperEndpoint()
                .map(converter)
                .orElseThrow(this::newInvalidUpperBoundException);
    }

    private IllegalStateException newInvalidLowerBoundException() {
        return new IllegalStateException("The interval " + this + " doesn't have lower bound.");
    }

    private IllegalStateException newInvalidUpperBoundException() {
        return new IllegalStateException("The interval " + this + " doesn't have upper bound.");
    }

    @Override
    public String toString() {
        return rangeSet.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval<?> interval = (Interval<?>) o;
        return Objects.equals(rangeSet, interval.rangeSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeSet);
    }

}
