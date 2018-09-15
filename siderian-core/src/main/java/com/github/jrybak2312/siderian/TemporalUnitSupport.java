package com.github.jrybak2312.siderian;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

/**
 *  @author Igor Rybak
 *  @since 12-Sep-2018
 */
class TemporalUnitSupport {

    public static Optional<TemporalUnit> getPrecision(TemporalAccessor temporalAccessor) {
        return Optional.ofNullable(temporalAccessor.query(TemporalQueries.precision()));
    }

    private TemporalUnitSupport() {
    }

}
