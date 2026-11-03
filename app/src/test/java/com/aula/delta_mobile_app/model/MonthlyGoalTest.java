package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.repository.MonthlyGoalStorage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class MonthlyGoalTest {
    private final Map<String, String> values = new HashMap<>();
    private final MonthlyGoalStorage.Store store = new MonthlyGoalStorage.Store() {
        @Override public String read(String key) { return values.get(key); }
        @Override public boolean write(String key, String value) { values.put(key, value); return true; }
    };
    private final YearMonth month = YearMonth.of(2026, 11);

    @Test
    public void acceptsDecimalCommaOrDotWithoutLosingPrecision() {
        assertEquals(0, new BigDecimal("1234.56").compareTo(MonthlyGoal.parse(" 1234,56 ")));
        assertEquals(0, new BigDecimal("1234.56").compareTo(MonthlyGoal.parse("1234.56")));
        assertEquals(0, new BigDecimal("1000").compareTo(MonthlyGoal.parse("1000,00")));
    }

    @Test
    public void rejectsZeroNegativeMalformedAndAmbiguousNumbers() {
        for (String input : new String[] { "", " ", "0", "0,00", "-10", "1.000,50", "1,000.50", "1.234", "1e4", "NaN", "12,", "abc" })
            assertThrows(input, IllegalArgumentException.class, () -> MonthlyGoal.parse(input));
    }

    @Test
    public void scopeSeparatesAccountPropertyAndYearMonth() throws IOException {
        MonthlyGoalStorage storage = new MonthlyGoalStorage(store);
        assertNull(storage.read(7, 10, month));
        storage.save(7, 10, month, "1234,56");
        assertEquals(0, new BigDecimal("1234.56").compareTo(new MonthlyGoalStorage(store).read(7, 10, month)));
        assertNull(storage.read(8, 10, month));
        assertNull(storage.read(7, 20, month));
        assertNull(storage.read(7, 10, month.plusMonths(1)));
        assertNull(storage.read(7, 10, month.plusYears(1)));
    }

    @Test
    public void editingOnlyReplacesTheSelectedGoal() throws IOException {
        MonthlyGoalStorage storage = new MonthlyGoalStorage(store);
        storage.save(7, 10, month, "100");
        storage.save(7, 20, month, "200");
        storage.save(7, 10, month, "150");
        assertEquals(0, new BigDecimal("150").compareTo(storage.read(7, 10, month)));
        assertEquals(0, new BigDecimal("200").compareTo(storage.read(7, 20, month)));
    }

    @Test
    public void invalidInputNeverWritesAndInvalidScopesAreRejected() {
        MonthlyGoalStorage storage = new MonthlyGoalStorage(store);
        assertThrows(IllegalArgumentException.class, () -> storage.save(7, 10, month, "-1"));
        assertThrows(IllegalArgumentException.class, () -> storage.save(0, 10, month, "10"));
        assertThrows(IllegalArgumentException.class, () -> storage.read(7, 0, month));
        assertTrue(values.isEmpty());
    }

    @Test
    public void failedWriteAndUnconfirmedValueAreNotSuccess() {
        MonthlyGoalStorage failed = new MonthlyGoalStorage(new MonthlyGoalStorage.Store() {
            @Override public String read(String key) { return null; }
            @Override public boolean write(String key, String value) { return false; }
        });
        assertThrows(IOException.class, () -> failed.save(7, 10, month, "100"));
        MonthlyGoalStorage unconfirmed = new MonthlyGoalStorage(new MonthlyGoalStorage.Store() {
            @Override public String read(String key) { return "50"; }
            @Override public boolean write(String key, String value) { return true; }
        });
        assertThrows(IOException.class, () -> unconfirmed.save(7, 10, month, "100"));
    }

    @Test
    public void corruptStoredValueDoesNotBecomeMissingOrZero() {
        values.put(MonthlyGoal.key(7, 10, month), "invalid");
        assertThrows(IllegalArgumentException.class, () -> new MonthlyGoalStorage(store).read(7, 10, month));
    }
}
