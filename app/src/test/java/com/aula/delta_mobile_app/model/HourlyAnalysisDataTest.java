package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.HourlyAnalysisData.*;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

public class HourlyAnalysisDataTest {
    @Test
    public void missingSourceNeverBecomesZeroOrExampleData() {
        HourlyAnalysisData data = HourlyAnalysisData.unavailable();
        assertFalse(data.available);
        assertEquals(6, data.today.size());
        assertEquals(6, data.yesterday.size());
        for (Cell cell : data.today) {
            assertEquals(CellState.MISSING, cell.state);
            assertNull(cell.liters);
            assertNull(cell.intensity);
        }
        assertTrue(data.peaks.isEmpty());
        assertTrue(data.equipment.isEmpty());
        assertNull(data.peakFlow);
        assertNull(data.estimatedTotal);
    }

    @Test
    public void confirmedZeroIsMeasuredWithMinimumIntensity() {
        Cell cell = new Cell(CellState.MEASURED, BigDecimal.ZERO, 4, "00h–04h");
        assertEquals(CellState.MEASURED, cell.state);
        assertEquals(BigDecimal.ZERO, cell.liters);
        assertEquals(Integer.valueOf(0), cell.intensity);
    }

    @Test
    public void futureIntervalCannotExposeAConsumptionValue() {
        Cell cell = new Cell(CellState.FUTURE, new BigDecimal("45"), 4, null);
        assertEquals(CellState.FUTURE, cell.state);
        assertNull(cell.liters);
        assertNull(cell.intensity);
    }

    @Test
    public void invalidVolumeIsMissingRatherThanZero() {
        Cell cell = new Cell(CellState.MEASURED, new BigDecimal("-1"), 3, null);
        assertEquals(CellState.MISSING, cell.state);
        assertNull(cell.liters);
    }

    private HourlyAnalysisData data(String peak, String reference, List<Cell> cells) {
        return new HourlyAnalysisData(true, cells, Collections.emptyList(), Collections.emptyList(), peak == null ? null : new BigDecimal(peak),
            reference == null ? null : new BigDecimal(reference), Collections.emptyList(), null);
    }

    @Test
    public void comparisonDoesNotDivideByMissingOrZeroReference() {
        assertNull(data("25", "0", Collections.emptyList()).increasePercent());
        assertNull(data("25", null, Collections.emptyList()).increasePercent());
        assertEquals(new BigDecimal("213"), data("25", "8", Collections.emptyList()).increasePercent());
    }

    @Test
    public void missingCellPreservesItsPositionInsteadOfShiftingTimeBuckets() {
        Cell first = new Cell(CellState.MEASURED, BigDecimal.ONE, 1, "00h–04h");
        HourlyAnalysisData data = data(null, null, Arrays.asList(first, null));
        assertEquals(2, data.today.size());
        assertSame(first, data.today.get(0));
        assertEquals(CellState.MISSING, data.today.get(1).state);
    }

    @Test
    public void unknownColorScaleIsNotInventedForMeasuredConsumption() {
        Cell cell = new Cell(CellState.MEASURED, BigDecimal.TEN, null, null);
        assertEquals(CellState.MEASURED, cell.state);
        assertNull(cell.intensity);
    }
}
