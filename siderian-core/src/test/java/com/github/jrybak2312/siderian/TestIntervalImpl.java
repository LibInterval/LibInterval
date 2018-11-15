package com.github.jrybak2312.siderian;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

import static com.github.jrybak2312.siderian.Interval.between;
import static com.github.jrybak2312.siderian.Interval.unionOf;
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
public class TestIntervalImpl {
    private LocalDate baseDate;

    @Before
    public void setUp() {
        baseDate = LocalDate.of(2020, 1, 1);
    }

    //_________________________________factory methods_____________________________________________

    @Test
    public void testFrom() {
        assertEquals("[[2020-01-01..+∞)]", Interval.from(baseDate).toString());
    }

    @Test
    public void testTo() {
        assertEquals("[(-∞..2020-01-01]]", Interval.to(baseDate).toString());
    }

    @Test
    public void testBetween() {
        assertEquals("[[2020-01-01..2020-01-11]]", Interval.between(baseDate, baseDate.plusDays(10)).toString());
    }

    @Test
    public void testAtLeast() {
        assertEquals("[[2020-01-01..+∞)]", Interval.atLeast(baseDate).toString());
    }

    @Test
    public void testAtMost() {
        assertEquals("[(-∞..2020-01-01]]", Interval.atMost(baseDate).toString());
    }

    @Test
    public void testClosed() {
        assertEquals("[[2020-01-01..2020-01-21]]", Interval.closed(baseDate, baseDate.plusDays(20)).toString());
    }

    //__________________________________intersection________________________________________________

    @Test
    public void testGetIntersectionOf3Intervals() {
        Interval<LocalDate> intersection = Interval.intersectionOf(
                between(baseDate.plusDays(1), baseDate.plusDays(90)),
                between(baseDate.plusDays(60), baseDate.plusDays(100)),
                between(baseDate.plusDays(50), baseDate.plusDays(95)));

        assertEquals("[[2020-03-01..2020-03-31]]", intersection.toString());
    }

    @Test
    public void testGetNoIntersection() {
        Interval<LocalDate> intersection = Interval.intersectionOf(
                between(baseDate.plusDays(1), baseDate.plusDays(20)),
                between(baseDate.plusDays(30), baseDate.plusDays(50)),
                between(baseDate.plusDays(40), baseDate.plusDays(60)));

        assertFalse(intersection.isPresent());
    }

    @Test
    public void testIntersectionWithUnboundedInterval() {
        Interval<LocalDate> intersection = Interval.intersectionOf(
                between(baseDate.plusDays(1), baseDate.plusDays(30)),
                between(baseDate.plusDays(25), null));

        assertEquals("[[2020-01-26..2020-01-31]]", intersection.toString());
    }

    @Test
    public void testIntersectionOf2UnboundedIntervals() {
        Interval<? extends Comparable<?>> intersection = Interval.intersectionOf(
                between(null, null),
                between(null, null));

        assertEquals("[(-∞..+∞)]", intersection.toString());
    }

    @Test
    public void testOneDayIntersection() {
        Interval<LocalDate> intersection = Interval.intersectionOf(
                between(null, baseDate.plusDays(30)),
                between(baseDate.plusDays(30), null));

        assertEquals("[[2020-01-31..2020-01-31]]", intersection.toString());
    }

    @Test
    public void testIntersectionWithIntervalWitchHasGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(20);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> union = unionOf(between(l1, u1), between(l2, u2));

        LocalDate l3 = baseDate.plusDays(5);
        LocalDate u3 = baseDate.plusDays(25);

        Interval<LocalDate> interval = Interval.intersectionOf(union, between(l3, u3));

        assertEquals("[[2020-01-06..2020-01-11], [2020-01-21..2020-01-26]]", interval.toString());
    }

    //___________________________________union___________________________________

    @Test
    public void testUnionWithTheResultOfIntervalWhichHasGap() {
        LocalDate l1 = baseDate;
        LocalDate u1 = l1.plusDays(10);

        LocalDate l2 = l1.plusDays(20);
        LocalDate u2 = l1.plusDays(30);
        Interval<LocalDate> result = unionOf(between(l1, u1), between(l2, u2));

        assertEquals("[[2020-01-01..2020-01-11], [2020-01-21..2020-01-31]]", result.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testShouldThrowExceptionIfIntervalHasGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(20);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> result = unionOf(between(l1, u1), between(l2, u2));
        result.hasLowerBound();
    }

    @Test
    public void testUnionWithTheResultOfIntervalWitchDoesNotHaveGaps() {
        LocalDate l1 = baseDate;
        LocalDate u1 = baseDate.plusDays(10);

        LocalDate l2 = baseDate.plusDays(8);
        LocalDate u2 = baseDate.plusDays(30);
        Interval<LocalDate> result = unionOf(between(l1, u1), between(l2, u2));
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

        Interval<LocalDate> result = i1.difference(unionOf(i2, i3));

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
        Interval<LocalDate> result = i1.difference(unionOf(i2, i3));
        assertEquals("[[2017-01-01..2017-04-11], [2018-11-26..+∞)]", result.toString());
    }

    @Test
    public void testDifferenceWithUnboundedInterval2() {
        Interval<LocalDate> interval1 = between(LocalDate.of(2017, 1, 1), null);
        Interval<LocalDate> i2 = between(LocalDate.of(2017, 4, 12), LocalDate.of(2018, 4, 30));
        Interval<LocalDate> i3 = between(LocalDate.of(2018, 5, 2), LocalDate.of(2018, 11, 25));
        Interval<LocalDate> result = interval1.difference(unionOf(i2, i3));
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

    //___________________________________map________________________________________

    @Test
    public void testDaysToTimeInterval() {
        LocalDate l1 = LocalDate.of(2020, 4, 10);
        LocalDate u1 = LocalDate.of(2021, 10, 18);
        Interval<LocalDateTime> result = between(l1, u1).toTimeInterval();

        assertEquals("[[2020-04-10T00:00..2021-10-18T23:59:59.999999999]]", result.toString());
    }

    @Test
    public void testDaysToMonthsInterval() {
        LocalDate l1 = LocalDate.of(2017, 12, 1);
        LocalDate u1 = LocalDate.of(2017, 12, 10);
        Interval<LocalDate> i1 = between(l1, u1);

        LocalDate l2 = LocalDate.of(2018, 1, 20);
        LocalDate u2 = LocalDate.of(2018, 5, 14);
        Interval<LocalDate> i2 = between(l2, u2);

        LocalDate l3 = LocalDate.of(2018, 11, 5);
        LocalDate u3 = LocalDate.of(2019, 3, 10);
        Interval<LocalDate> i3 = between(l3, u3);

        // .map(YearMonth::from) is better in this case
        Interval<YearMonth> result = unionOf(unionOf(i1, i2), i3).toMonthsInterval();

        assertEquals("[[2017-12..2017-12], [2018-01..2018-05], [2018-11..2019-03]]", result.toString());
    }

    @Test
    public void testMonthToTimeInterval() {
        YearMonth l = YearMonth.of(2022, 3);
        YearMonth u = YearMonth.of(2026, 9);
        Interval<YearMonth> monthsInterval = Interval.between(l, u);
        Interval<LocalDateTime> result = monthsInterval.toTimeInterval();
        assertEquals("[[2022-03-01T00:00..2026-09-30T23:59:59.999999999]]", result.toString());
    }

    @Test
    public void testMonthToDaysInterval() {
        YearMonth l = YearMonth.of(2020, 1);
        YearMonth u = YearMonth.of(2020, 2);
        Interval<YearMonth> monthsInterval = Interval.between(l, u);
        Interval<LocalDate> result = monthsInterval.toDaysInterval();
        assertEquals("[[2020-01-01..2020-02-29]]", result.toString());
    }

    @Test
    public void testMonthToYearsInterval() {
        YearMonth l = YearMonth.of(2020, 12);
        YearMonth u = YearMonth.of(2025, 5);
        Interval<YearMonth> monthsInterval = Interval.between(l, u);
        Interval<Year> result = monthsInterval.map(Year::from);
        assertEquals("[[2020..2025]]", result.toString());
    }

    @Test
    public void testYearsToTimeInterval() {
        Year l1 = Year.of(2020);
        Year u1 = Year.of(2024);

        Interval<LocalDateTime> result = between(l1, u1).toTimeInterval();
        assertEquals("[[2020-01-01T00:00..2024-12-31T23:59:59.999999999]]", result.toString());
    }

    @Test
    public void testYearsToDaysInterval() {
        Year l1 = Year.of(2019);
        Year u1 = Year.of(2021);

        Interval<LocalDate> result = between(l1, u1).toDaysInterval();
        assertEquals("[[2019-01-01..2021-12-31]]", result.toString());
    }

    @Test
    public void testYearsToMonthsInterval() {
        Year l1 = Year.of(2018);
        Year u1 = Year.of(2020);

        Interval<YearMonth> result = between(l1, u1).toMonthsInterval();
        assertEquals("[[2018-01..2020-12]]", result.toString());
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

        List<YearMonth> result = unionOf(unionOf(i1, i2), i3).months().collect(toList());

        assertThat(result).containsExactly(
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

        List<LocalDate> result = unionOf(unionOf(i1, i2), i3).days().collect(toList());

        assertThat(result).containsExactly(
                LocalDate.of(2018, 12, 28),
                LocalDate.of(2018, 12, 29),
                LocalDate.of(2018, 12, 30),
                LocalDate.of(2018, 12, 31),
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 3),
                LocalDate.of(2019, 1, 4));
    }

    @Test
    public void testEmptyNotNoneInterval() {
        Interval<LocalDate> noneInterval = Interval.none();
        assertFalse(noneInterval.notNoneInterval().isPresent());
    }

    @Test
    public void testNotEmptyNotNoneInterval() {
        Interval<LocalDate> i = between(baseDate, baseDate.plusDays(10));
        Interval<LocalDate> result = i.notNoneInterval().get();
        assertEquals("[[2020-01-01..2020-01-11]]", result.toString());
    }

    @Test
    public void testAllInterval() {
        assertEquals("[(-∞..+∞)]", Interval.all().toString());
    }

    @Test
    public void testNoneInterval() {
        assertEquals("[]", Interval.none().toString());
    }

    @Test
    public void testCountDays() {
        LocalDate l1 = LocalDate.of(2019, 12, 31);
        LocalDate u1 = LocalDate.of(2020, 4, 30);
        Interval<LocalDate> i1 = between(l1, u1);

        LocalDate l2 = LocalDate.of(2020, 6, 2);
        LocalDate u2 = LocalDate.of(2020, 12, 31);
        Interval<LocalDate> i2 = between(l2, u2);

        long days = unionOf(i1, i2).countDays();
        assertEquals(335L, days);
    }

}
