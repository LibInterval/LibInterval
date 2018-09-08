package com.github.jrybak2312.siderian;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.jrybak2312.siderian.Interval.union;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Igor Rybak
 * @since 21-Mar-2018
 */
@RunWith(JUnit4.class)
public class TestInterval {
    private LocalDate now;

    @Before
    public void setUp() {
        now = LocalDate.now();
    }

    @Test
    public void testShouldCreateOneIntervalWithGap() {
        LocalDate l1 = now;
        LocalDate u1 = l1.plusDays(10);

        LocalDate l2 = l1.plusDays(20);
        LocalDate u2 = l1.plusDays(30);
        Interval<LocalDate> union = union(Interval.between(l1, u1), Interval.between(l2, u2));
        assertTrue(union.contains(l1.plusDays(5)));
        assertFalse(union.contains(l1.plusDays(15)));
        assertTrue(union.contains(l1.plusDays(25)));
    }

    @Test(expected = IllegalStateException.class)
    public void testShouldThrowExceptionIfIntervalHasGaps() {
        LocalDate l1 = now;
        LocalDate u1 = now.plusDays(10);

        LocalDate l2 = now.plusDays(20);
        LocalDate u2 = now.plusDays(30);
        Interval<LocalDate> union = union(Interval.between(l1, u1), Interval.between(l2, u2));
        union.hasLowerBound();
    }

    @Test
    public void testShouldCreateOneIntervalFromTwoWithOverlappingDates() {
        LocalDate l1 = now;
        LocalDate u1 = now.plusDays(10);

        LocalDate l2 = now.plusDays(8);
        LocalDate u2 = now.plusDays(30);
        Interval<LocalDate> union = union(Interval.between(l1, u1), Interval.between(l2, u2));
        assertEquals(now, union.getLowerEndpoint().get());
        assertEquals(now.plusDays(30), union.getUpperEndpoint().get());
    }

    @Test
    public void testMonthsStream() {
        LocalDate l1 = LocalDate.of(2017, 12, 1);
        LocalDate u1 = LocalDate.of(2017, 12, 10);
        Interval<LocalDate> i1 = Interval.between(l1, u1);

        LocalDate l2 = LocalDate.of(2017, 12, 20);
        LocalDate u2 = LocalDate.of(2018, 1, 31);
        Interval<LocalDate> i2 = Interval.between(l2, u2);

        LocalDate l3 = LocalDate.of(2018, 3, 1);
        LocalDate u3 = LocalDate.of(2018, 3, 31);
        Interval<LocalDate> i3 = Interval.between(l3, u3);

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
        Interval<LocalDate> i1 = Interval.between(l1, u1);

        LocalDate l2 = LocalDate.of(2018, 12, 29);
        LocalDate u2 = LocalDate.of(2019, 1, 1);
        Interval<LocalDate> i2 = Interval.between(l2, u2);

        LocalDate l3 = LocalDate.of(2019, 1, 3);
        LocalDate u3 = LocalDate.of(2019, 1, 4);
        Interval<LocalDate> i3 = Interval.between(l3, u3);

        List<LocalDate> days = union(union(i1, i2), i3).days().collect(toList());

        assertThat(days).containsExactly(
                LocalDate.of(2018, 12, 28),
                LocalDate.of(2018, 12, 29),
                LocalDate.of(2018, 12, 30),
                LocalDate.of(2018, 12, 31),
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 3),
                LocalDate.of(2019, 1, 4)
        );
    }

    @Test
    public void testDifference() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> i1 = Interval.between(l1, u1);

        LocalDate l2 = LocalDate.of(2018, 5, 2);
        LocalDate u2 = LocalDate.of(2018, 5, 4);
        Interval<LocalDate> i2 = Interval.between(l2, u2);

        LocalDate l3 = LocalDate.of(2018, 5, 6);
        LocalDate u3 = LocalDate.of(2018, 5, 8);
        Interval<LocalDate> i3 = Interval.between(l3, u3);

        Interval<LocalDate> difference = i1.difference(union(i2, i3));
        List<LocalDate> days = difference.days()
                .collect(Collectors.toList());

        assertThat(days).containsExactly(
                LocalDate.of(2018, 5, 1),
                LocalDate.of(2018, 5, 5),
                LocalDate.of(2018, 5, 9),
                LocalDate.of(2018, 5, 10));
    }

    @Test
    public void testDifferenceOfUnboundedInterval() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> interval = Interval.between(l1, u1);
        Interval<LocalDate> unbounded = Interval.between(LocalDate.of(2010, 5, 10), null);
        Interval<LocalDate> result = unbounded.difference(interval);
        assertEquals("[[2010-05-10..2018-04-30], [2018-05-11..+∞)]", result.toString());
    }

    @Test
    public void testDifferenceWithNoneInterval() {
        LocalDate l1 = LocalDate.of(2018, 5, 1);
        LocalDate u1 = LocalDate.of(2018, 5, 10);
        Interval<LocalDate> i1 = Interval.between(l1, u1);
        Interval<LocalDate> result = i1.difference(Interval.none());
        assertEquals("[[2018-05-01..2018-05-10]]", result.toString());
    }

    @Test
    public void testDifference1(){
        Interval<LocalDate> interval1 = Interval.between(LocalDate.of(2017, 1, 1), null);
        Interval<LocalDate> i2 = Interval.between(LocalDate.of(2017, 4, 12), LocalDate.of(2018, 4, 30));
        Interval<LocalDate> i3 = Interval.between(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 11, 25));
        Interval<LocalDate> result = interval1.difference(union(i2, i3));
        assertEquals("[[2017-01-01..2017-04-11], [2018-11-26..+∞)]", result.toString());
    }

    @Test
    public void testDifference2(){
        Interval<LocalDate> interval1 = Interval.between(LocalDate.of(2017, 1, 1), null);
        Interval<LocalDate> i2 = Interval.between(LocalDate.of(2017, 4, 12), LocalDate.of(2018, 4, 30));
        Interval<LocalDate> i3 = Interval.between(LocalDate.of(2018, 5, 2), LocalDate.of(2018, 11, 25));
        Interval<LocalDate> result = interval1.difference(union(i2, i3));
        assertEquals("[[2017-01-01..2017-04-11], [2018-05-01..2018-05-01], [2018-11-26..+∞)]", result.toString());
    }
}
