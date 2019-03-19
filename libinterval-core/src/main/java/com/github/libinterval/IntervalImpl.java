package com.github.libinterval;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.github.libinterval.IntervalUtils.newInvalidLowerBoundException;
import static com.github.libinterval.IntervalUtils.newInvalidUpperBoundException;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * @author Igor Rybak
 * @since 15.11.2017
 */
class IntervalImpl<T extends Comparable<?> & Temporal> implements Interval<T> {
    private static Logger logger = LoggerFactory.getLogger(IntervalImpl.class);

    private final ImmutableRangeSet<T> rangeSet;

    @Override
    public Interval<T> difference(Interval<T> interval) {
        logger.debug("Finding difference of " + this + " and " + interval);
        Interval<T> result = findDifference(interval, RangeConverter.defaultInstance());
        logger.debug("Result of a difference of " + this + " and " + interval + " is " + result + ".");
        return result;
    }

    @Override
    public Interval<T> difference(Interval<T> interval, TemporalUnit temporalUnit) {
        logger.debug("Finding difference of " + this + " and " + interval + " with " + temporalUnit + " precision.");
        DiscreteDomain<T> discreteDomain = new CustomComparableTemporalDiscreteDomain<>(temporalUnit);
        Interval<T> result = findDifference(interval, new RangeConverter<>(discreteDomain));
        logger.debug("Result of a difference of " + this + " and " + interval + " is " + result + ".");
        return result;
    }

    private Interval<T> findDifference(Interval<T> interval, RangeConverter<T> rangeConverter) {
        ImmutableRangeSet<T> difference = this.rangeSet.difference(interval.getRangeSet());
        return new IntervalImpl<>(convertToClosed(difference, rangeConverter));
    }

    private ImmutableRangeSet<T> convertToClosed(ImmutableRangeSet<T> difference, RangeConverter<T> rangeConverter) {
        return difference.asRanges().stream()
                .filter(rangeConverter::canBeConvertedToClosed)
                .map(rangeConverter::convertToClosed)
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::copyOf));
    }

    @Override
    public Optional<T> findLowerEndpoint() {
        return getRange().hasLowerBound() ? Optional.of(getRange().lowerEndpoint()) : Optional.empty();
    }

    @Override
    public Optional<T> findUpperEndpoint() {
        return getRange().hasUpperBound() ? Optional.of(getRange().upperEndpoint()) : Optional.empty();
    }

    @Override
    public boolean contains(T t) {
        return rangeSet.contains(t);
    }

    @Override
    public boolean hasLowerBound() {
        return getRange().hasLowerBound();
    }

    @Override
    public boolean hasUpperBound() {
        return getRange().hasUpperBound();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Comparable<?> & Temporal> Stream<R> iterate(TemporalUnit temporalUnit,
                                                                  Function<T, R> lowerEndpointMapper,
                                                                  Function<T, R> upperEndpointMapper) {
        return subIntervalsStream()
                .flatMap(subInterval -> {
                    Interval<R> converted;

                    if (lowerEndpointMapper == null && upperEndpointMapper == null) {
                        converted = (Interval<R>) this;
                    } else {
                        Objects.requireNonNull(lowerEndpointMapper, "lowerEndpointMapper is required");
                        Objects.requireNonNull(lowerEndpointMapper, "upperEndpointMapper is required");
                        converted = subInterval.map(lowerEndpointMapper, upperEndpointMapper);
                    }

                    R lower = converted.findLowerEndpoint()
                            .orElseThrow(() -> newInvalidLowerBoundException(converted));
                    R upper = converted.findUpperEndpoint()
                            .orElseThrow(() -> newInvalidUpperBoundException(converted));

                    UnaryOperator<R> increaseFunction = date -> (R) date.plus(1, temporalUnit);
                    return Stream.iterate(lower, increaseFunction)
                            .limit(temporalUnit.between(lower, upper) + 1);
                })
                .distinct()
                .sorted();
    }

    @Override
    public <R> Stream<R> iterate(BiFunction<T, T, Stream<R>> streamGenerator) {
        return subIntervalsStream()
                .flatMap(subInterval -> {
                    T lower = subInterval.findLowerEndpoint()
                            .orElseThrow(() -> newInvalidLowerBoundException(this));
                    T upper = subInterval.findUpperEndpoint()
                            .orElseThrow(() -> newInvalidUpperBoundException(this));

                    return streamGenerator.apply(lower, upper);
                })
                .sorted();
    }

    @Override
    public <R extends Comparable<?> & Temporal> Interval<R> map(Function<T, R> lowerEndpointMapper,
                                                                Function<T, R> upperEndpointMapper) {
        ImmutableRangeSet<R> rangeSet = getSubIntervals().stream()
                .map(i -> {
                    R start = i.findLowerEndpoint().map(lowerEndpointMapper).orElse(null);
                    R end = i.findUpperEndpoint().map(upperEndpointMapper).orElse(null);
                    return Interval.between(start, end);
                })
                .map(Interval::getRangeSet)
                .map(RangeSet::asRanges)
                .flatMap(Set::stream)
                .collect(collectingAndThen(toSet(), ImmutableRangeSet::unionOf));

        return new IntervalImpl<>(rangeSet);
    }

    @Override
    public Set<Interval<T>> getSubIntervals() {
        return subIntervalsStream()
                .collect(toSet());
    }

    private Stream<IntervalImpl<T>> subIntervalsStream() {
        return rangeSet.asRanges().stream()
                .map(ImmutableRangeSet::of)
                .map(IntervalImpl::new);
    }

    @Override
    public long count(TemporalUnit temporalUnit) {
        return rangeSet.asRanges().stream()
                .mapToLong(range -> temporalUnit.between(range.lowerEndpoint(), range.upperEndpoint()) + 1)
                .sum();
    }

    @Override
    public boolean isPresent() {
        return !rangeSet.isEmpty();
    }

    @Override
    public Optional<Interval<T>> getNotNoneInterval() {
        return isPresent() ? Optional.of(this) : Optional.empty();
    }

    @Override
    public ImmutableRangeSet<T> getRangeSet() {
        return rangeSet;
    }

    private Range<T> getRange() {
        Set<Range<T>> ranges = rangeSet.asRanges();
        if (ranges.size() > 1) {
            throw new IllegalStateException("The interval has more than one sub intervals: " + this + ".");
        }

        return ranges.stream().findFirst().orElseThrow(() -> new IllegalStateException("The interval is empty."));
    }

    @Override
    public String toString() {
        return rangeSet.toString();
    }

    IntervalImpl(ImmutableRangeSet<T> rangeSet) {
        this.rangeSet = rangeSet;
    }
}
