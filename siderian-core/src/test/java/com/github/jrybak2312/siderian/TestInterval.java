package com.github.jrybak2312.siderian;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static com.github.jrybak2312.siderian.Interval.between;
import static com.github.jrybak2312.siderian.Interval.intersection;
import static com.github.jrybak2312.siderian.Interval.union;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Igor Rybak
 * @since 21-Mar-2018
 */
@RunWith(JUnit4.class)
public class TestInterval {
    private LocalDate baseDate;

    @Before
    public void setUp() {
        baseDate = LocalDate.of(2020, 1, 1);
    }

    //__________________________________intersection________________________________________________

    @Test
    public void testGetIntersectionOf3Intervals() {
        Interval<LocalDate> intersection = intersection(
                between(baseDate.plusDays(1), baseDate.plusDays(90)),
                between(baseDate.plusDays(60), baseDate.plusDays(100)),
                between(baseDate.plusDays(50), baseDate.plusDays(95))).get();

        assertEquals("[[2020-03-01..2020-03-31]]", intersection.toString());
    }

    @Test
    public void testGetNoIntersection() {
        Optional<Interval<LocalDate>> intersection = intersection(
                between(baseDate.plusDays(1), baseDate.plusDays(20)),
                between(baseDate.plusDays(30), baseDate.plusDays(50)),
                between(baseDate.plusDays(40), baseDate.plusDays(60)));

        assertFalse(intersection.isPresent());
    }

    @Test
    public void testIntersectionWithUnboundedInterval() {
        Interval<LocalDate> intersection = intersection(
                between(baseDate.plusDays(1), baseDate.plusDays(30)),
                between(baseDate.plusDays(25), null)).get();

        assertEquals("[[2020-01-26..2020-01-31]]", intersection.toString());
    }

    @Test
    public void testIntersectionOf2UnboundedIntervals() {
        Interval<? extends Comparable<?>> intersection = intersection(
                between(null, null),
                between(null, null)).get();

        assertEquals("[(-∞..+∞)]", intersection.toString());
    }

    @Test
    public void testOneDayIntersection() {
        Interval<LocalDate> intersection = intersection(
                between(null, baseDate.plusDays(30)),
                between(baseDate.plusDays(30), null)).get();

        assertEquals("[[2020-01-31..2020-01-31]]", intersection.toString());
    }

    @Test
    public void testIntersectionWithIntervalWitchHasGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(20);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> union = union(between(l1, u1), between(l2, u2));

        LocalDate l3 = baseDate.plusDays(5);
        LocalDate u3 = baseDate.plusDays(25);

        Interval<LocalDate> interval = intersection(union, between(l3, u3)).get();

        assertEquals("[[2020-01-06..2020-01-11], [2020-01-21..2020-01-26]]", interval.toString());
    }

    //___________________________________union___________________________________

    @Test
    public void testUnionWithTheResultOfIntervalWhichHasGap() {
        LocalDate l1 = baseDate;
        LocalDate u1 = l1.plusDays(10);

        LocalDate l2 = l1.plusDays(20);
        LocalDate u2 = l1.plusDays(30);
        Interval<LocalDate> result = union(between(l1, u1), between(l2, u2));

        assertEquals("[[2020-01-01..2020-01-11], [2020-01-21..2020-01-31]]", result.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testShouldThrowExceptionIfIntervalHasGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(20);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> result = union(between(l1, u1), between(l2, u2));
        result.hasLowerBound();
    }

    @Test
    public void testUnionWithTheResultOfIntervalWitchDoesNotHaveGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(8);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> result = union(between(l1, u1), between(l2, u2));
        assertEquals("[[2020-01-01..2020-01-31]]", result.toString());
    }

    //__________________________________difference_________________________________________

    @Test
    public void testDifference() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> i1 = between(l1, u1);

        LocalDate l2 = LocalDate.of(2018, 5, 2);
        LocalDate u2 = LocalDate.of(2018, 5, 4);
        Interval<LocalDate> i2 = between(l2, u2);

        LocalDate l3 = LocalDate.of(2018, 5, 6);
        LocalDate u3 = LocalDate.of(2018, 5, 8);
        Interval<LocalDate> i3 = between(l3, u3);

        Interval<LocalDate> result = i1.difference(union(i2, i3));

        assertEquals("[[2018-05-01..2018-05-01], [2018-05-05..2018-05-05], [2018-05-09..2018-05-10]]",
                result.toString());
    }

    @Test
    public void testDifferenceOfUnboundedInterval() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> interval = between(l1, u1);
        Interval<LocalDate> unbounded = between(LocalDate.of(2010, 5, 10), null);
        Interval<LocalDate> result = unbounded.difference(interval);
        assertEquals("[[2010-05-10..2018-04-30], [2018-05-11..+∞)]", result.toString());
    }

    @Test
    public void testDifferenceWithNoneInterval() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> i1 = between(l1, u1);
        Interval<LocalDate> result = i1.difference(Interval.none());
        assertEquals("[[2018-05-01..2018-05-10]]", result.toString());
    }

    @Test
    public void testDifferenceWithUnboundedInterval1() {
        Interval<LocalDate> i1 = between(LocalDate.of(2017, 1, 1), null);
        Interval<LocalDate> i2 = between(LocalDate.of(2017, 4, 12), LocalDate.of(2018, 4, 30));
        Interval<LocalDate> i3 = between(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 11, 25));
        Interval<LocalDate> result = i1.difference(union(i2, i3));
        assertEquals("[[2017-01-01..2017-04-11], [2018-11-26..+∞)]", result.toString());
    }

    @Test
    public void testDifferenceWithUnboundedInterval2() {
        Interval<LocalDate> interval1 = between(LocalDate.of(2017, 1, 1), null);
        Interval<LocalDate> i2 = between(LocalDate.of(2017, 4, 12), LocalDate.of(2018, 4, 30));
        Interval<LocalDate> i3 = between(LocalDate.of(2018, 5, 2), LocalDate.of(2018, 11, 25));
        Interval<LocalDate> result = interval1.difference(union(i2, i3));
        assertEquals("[[2017-01-01..2017-04-11], [2018-05-01..2018-05-01], [2018-11-26..+∞)]", result.toString());
    }

    @Test
    public void testDifferenceWithCustomTemporalAmount() {
        LocalDateTime l1 = LocalDateTime.of(baseDate, LocalTime.of(12, 30));
        LocalDateTime u1 = LocalDateTime.of(baseDate, LocalTime.of(16, 0));
        LocalDateTime l2 = LocalDateTime.of(baseDate, LocalTime.of(14, 0));
        LocalDateTime u2 = LocalDateTime.of(baseDate, LocalTime.of(15, 30));

        Interval<LocalDateTime> i1 = between(l1, u1);
        Interval<LocalDateTime> i2 = between(l2, u2);

        Interval<LocalDateTime> result = i1.difference(i2, MINUTES);

        assertEquals("[[2020-01-01T12:30..2020-01-01T13:59], [2020-01-01T15:31..2020-01-01T16:00]]", result.toString());
    }

    //__________________________________other_______________________________________

    @Test
    public void testMonthsStream() {
        LocalDate l1 = LocalDate.of(2017, 12, 1);
        LocalDate u1 = LocalDate.of(2017, 12, 10);
        Interval<LocalDate> i1 = between(l1, u1);

        LocalDate l2 = LocalDate.of(2017, 12, 20);
        LocalDate u2 = LocalDate.of(2018, 1, 31);
        Interval<LocalDate> i2 = between(l2, u2);

        LocalDate l3 = LocalDate.of(2018, 3, 1);
        LocalDate u3 = LocalDate.of(2018, 3, 31);
        Interval<LocalDate> i3 = between(l3, u3);

        List<YearMonth> months = union(union(i1, i2), i3).months().collect(toList());

        assertThat(months).containsExactly(
                YearMonth.of(2017, 12),
                YearMonth.of(2018, 1),
                YearMonth.of(2018, 3));

    }

    @Test
    public void testDaysStream() {
        LocalDate l1 = LocalDate.of(2018, 12, 28);
        LocalDate u1 = LocalDate.of(2018, 12, 30);
        Interval<LocalDate> i1 = between(l1, u1);

        LocalDate l2 = LocalDate.of(2018, 12, 29);
        LocalDate u2 = LocalDate.of(2019, 1, 1);
        Interval<LocalDate> i2 = between(l2, u2);

        LocalDate l3 = LocalDate.of(2019, 1, 3);
        LocalDate u3 = LocalDate.of(2019, 1, 4);
        Interval<LocalDate> i3 = between(l3, u3);

        List<LocalDate> days = union(union(i1, i2), i3).days().collect(toList());

        assertThat(days).containsExactly(
                LocalDate.of(2018, 12, 28),
                LocalDate.of(2018, 12, 29),
                LocalDate.of(2018, 12, 30),
                LocalDate.of(2018, 12, 31),
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 3),
                LocalDate.of(2019, 1, 4));
    }
}
