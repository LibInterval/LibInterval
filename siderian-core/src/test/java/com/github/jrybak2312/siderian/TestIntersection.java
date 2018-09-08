package com.github.jrybak2312.siderian;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.github.jrybak2312.siderian.Interval.intersection;
import static com.github.jrybak2312.siderian.Interval.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Igor Rybak
 * @since 15.11.2017
 */
@RunWith(JUnit4.class)
public class TestIntersection {

    private LocalDate now;

    @Before
    public void setUp() {
        now = LocalDate.now();
    }

    @Test
    public void testGetIntersectionOf3Intervals() {
        Interval<LocalDate> intersection = intersection(
                Interval.between(now.plusDays(1), now.plusDays(90)),
                Interval.between(now.plusDays(60), now.plusDays(100)),
                Interval.between(now.plusDays(50), now.plusDays(95))).get();

        assertEquals(now.plusDays(60), intersection.getLowerEndpoint().get());
        assertEquals(now.plusDays(90), intersection.getUpperEndpoint().get());
    }

    @Test
    public void testGetNoIntersection() {
        Optional<Interval<LocalDate>> intersection = intersection(
                Interval.between(now.plusDays(1), now.plusDays(20)),
                Interval.between(now.plusDays(30), now.plusDays(50)),
                Interval.between(now.plusDays(40), now.plusDays(60))
        );

        assertFalse(intersection.isPresent());
    }

    @Test
    public void testNullableInterval() {
        Interval<LocalDate> intersection = intersection(
                Interval.between(now.plusDays(1), now.plusDays(30)),
                Interval.between(now.plusDays(25), null)).get();

        assertEquals(now.plusDays(25), intersection.getLowerEndpoint().get());
        assertEquals(now.plusDays(30), intersection.getUpperEndpoint().get());
    }

    @Test
    public void testUnboundedInterval() {
        Interval<? extends Comparable<?>> intersection = intersection(
                Interval.between(null, null),
                Interval.between(null, null)).get();

        assertFalse(intersection.getLowerEndpoint().isPresent());
        assertFalse(intersection.getUpperEndpoint().isPresent());
    }

    @Test
    public void testOneDayIntersection() {
        Interval<LocalDate> intersection = intersection(
                Interval.between(null, now.plusDays(30)),
                Interval.between(now.plusDays(30), null)).get();

        assertEquals(now.plusDays(30), intersection.getLowerEndpoint().get());
        assertEquals(now.plusDays(30), intersection.getUpperEndpoint().get());
    }

    @Test
    public void testBrokenIntervalIntersection() {
        LocalDate l1 = now;
        LocalDate u1 = l1.plusDays(10);

        LocalDate l2 = l1.plusDays(20);
        LocalDate u2 = l1.plusDays(30);
        Interval<LocalDate> union = union(Interval.between(l1, u1), Interval.between(l2, u2));

        LocalDate l3 = now.plusDays(5);
        LocalDate u3 = now.plusDays(25);
        Interval<LocalDate> interval = intersection(union, Interval.between(l3, u3)).get();
        Set<Interval<LocalDate>> intervals = interval.getSubIntervals();

        assertThat(intervals)
                .extracting(i -> i.getLowerEndpoint().get())
                .containsExactlyInAnyOrder(now.plusDays(5), now.plusDays(20));

        assertThat(intervals)
                .extracting(i -> i.getUpperEndpoint().get())
                .containsExactlyInAnyOrder(now.plusDays(10), now.plusDays(25));
    }
}
