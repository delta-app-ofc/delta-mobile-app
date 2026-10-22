package com.aula.delta_mobile_app.model;

import java.math.*;
import java.util.*;

public final class HourlyAnalysisData {
    public enum CellState {
        MEASURED, MISSING, FUTURE
    }

    public static final class Cell {
        public final CellState state;
        public final BigDecimal liters;
        public final Integer intensity;
        public final String interval;

        public Cell(CellState state, BigDecimal liters, Integer intensity, String interval) {
            this.liters = state == CellState.MEASURED ? valid(liters) : null;
            this.state = state == CellState.FUTURE ? state : this.liters == null ? CellState.MISSING : CellState.MEASURED;
            Integer level = null;
            if (this.liters != null && this.liters.signum() == 0) level = Integer.valueOf(0);
            else if (this.liters != null && intensity != null && intensity >= 0 && intensity <= 4)
                level = intensity;
            this.intensity = level;
            this.interval = interval;
        }
    }

    public static final class Peak {
        public final String interval, description;
        public final BigDecimal liters;
        public final boolean highest;

        public Peak(String interval, String description, BigDecimal liters, boolean highest) {
            this.interval = interval;
            this.description = description;
            this.liters = valid(liters);
            this.highest = highest;
        }
    }

    public static final class Equipment {
        public final String name;
        public final BigDecimal flow;
        public final boolean approximate;

        public Equipment(String name, BigDecimal flow, boolean approximate) {
            this.name = name;
            this.flow = valid(flow);
            this.approximate = approximate;
        }
    }

    public final boolean available;
    public final List<Cell> today, yesterday;
    public final List<Peak> peaks;
    public final List<Equipment> equipment;
    public final BigDecimal peakFlow, baselineFlow, estimatedTotal;

    public HourlyAnalysisData(boolean available, List<Cell> today, List<Cell> yesterday, List<Peak> peaks, BigDecimal peakFlow, BigDecimal baselineFlow, List<Equipment> equipment,
        BigDecimal estimatedTotal) {
        this.available = available;
        this.today = cellSlots(today);
        this.yesterday = cellSlots(yesterday);
        this.peaks = immutable(peaks);
        this.equipment = immutable(equipment);
        this.peakFlow = valid(peakFlow);
        this.baselineFlow = valid(baselineFlow);
        this.estimatedTotal = valid(estimatedTotal);
    }

    public static HourlyAnalysisData unavailable() {
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < 6; i++) cells.add(new Cell(CellState.MISSING, null, null, null));
        return new HourlyAnalysisData(false, cells, cells, Collections.emptyList(), null, null, Collections.emptyList(), null);
    }

    public BigDecimal increasePercent() {
        if (peakFlow == null || baselineFlow == null || baselineFlow.signum() == 0) return null;
        return peakFlow.subtract(baselineFlow).multiply(BigDecimal.valueOf(100)).divide(baselineFlow, 0, RoundingMode.HALF_UP);
    }

    private static List<Cell> cellSlots(List<Cell> values) {
        List<Cell> slots = new ArrayList<>();
        if (values != null)
            for (Cell cell : values)
                slots.add(cell == null ? new Cell(CellState.MISSING, null, null, null) : cell);
        return Collections.unmodifiableList(slots);
    }

    private static BigDecimal valid(BigDecimal value) {
        return value != null && value.signum() >= 0 && Double.isFinite(value.doubleValue()) ? value : null;
    }

    private static <T> List<T> immutable(List<T> values) {
        List<T> clean = new ArrayList<>();
        if (values != null) for (T value : values) if (value != null) clean.add(value);
        return Collections.unmodifiableList(clean);
    }
}
