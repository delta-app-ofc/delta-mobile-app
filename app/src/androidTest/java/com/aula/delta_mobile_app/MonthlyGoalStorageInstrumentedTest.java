package com.aula.delta_mobile_app;

import android.content.Context;
import com.aula.delta_mobile_app.repository.MonthlyGoalStorage;
import com.aula.delta_mobile_app.repository.SharedPreferencesGoalStore;
import com.aula.delta_mobile_app.repository.SQLiteGoalStore;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MonthlyGoalStorageInstrumentedTest {
    @Test
    public void goalsSurviveStorageRecreationWithoutMixingScopes() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        context.deleteDatabase(SQLiteGoalStore.DATABASE);
        context.getSharedPreferences(SharedPreferencesGoalStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        try {
            YearMonth month = YearMonth.of(2026, 11);
            try (SQLiteGoalStore store = new SQLiteGoalStore(context)) {
                MonthlyGoalStorage first = new MonthlyGoalStorage(store);
                first.save(7, 10, month, "1234,56");
                first.save(7, 11, month, "500");
            }
            try (SQLiteGoalStore store = new SQLiteGoalStore(context)) {
                MonthlyGoalStorage reopened = new MonthlyGoalStorage(store);
                assertEquals(0, new BigDecimal("1234.56").compareTo(reopened.read(7, 10, month)));
                assertEquals(0, new BigDecimal("500").compareTo(reopened.read(7, 11, month)));
                assertNull(reopened.read(8, 10, month));
                assertNull(reopened.read(7, 10, month.plusMonths(1)));
            }
        } finally {
            context.deleteDatabase(SQLiteGoalStore.DATABASE);
            context.getSharedPreferences(SharedPreferencesGoalStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    @Test
    public void migratesLegacyGoalsOnceAndKeepsLaterEdits() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        context.deleteDatabase(SQLiteGoalStore.DATABASE);
        android.content.SharedPreferences preferences = context.getSharedPreferences(SharedPreferencesGoalStore.PREFERENCES, Context.MODE_PRIVATE);
        YearMonth month = YearMonth.of(2026, 11);
        preferences.edit().clear().putString(com.aula.delta_mobile_app.model.MonthlyGoal.key(7, 10, month), "200")
            .putString("goal:7:10:invalid", "100").commit();
        try {
            try (SQLiteGoalStore store = new SQLiteGoalStore(context)) {
                MonthlyGoalStorage storage = new MonthlyGoalStorage(store);
                assertEquals(0, new BigDecimal("200").compareTo(storage.read(7, 10, month)));
                storage.save(7, 10, month, "350");
            }
            try (SQLiteGoalStore store = new SQLiteGoalStore(context)) {
                assertEquals(0, new BigDecimal("350").compareTo(new MonthlyGoalStorage(store).read(7, 10, month)));
            }
        } finally {
            context.deleteDatabase(SQLiteGoalStore.DATABASE);
            preferences.edit().clear().commit();
        }
    }
}
