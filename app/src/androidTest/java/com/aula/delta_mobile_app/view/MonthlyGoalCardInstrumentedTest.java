package com.aula.delta_mobile_app.view;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.HomeDashboard;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MonthlyGoalCardInstrumentedTest {
    private View inflate(int layout) {
        return LayoutInflater.from(new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Deltamobileapp)).inflate(layout, null, false);
    }

    @Test
    public void dashboardCardDoesNotRequireHomeOnlyRecommendations() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View dashboard = inflate(R.layout.fragment_dashboard);
            assertNotNull(dashboard.findViewById(R.id.home_goal_value));
            assertNull(dashboard.findViewById(R.id.home_goal_guidance_title));
            MonthlyGoalCard.renderGuidance(dashboard, new BigDecimal("300"), HomeDashboard.unavailable(), LocalDate.of(2026, 11, 10));
        });
    }

    @Test
    public void homeStillShowsCalculatedRecommendations() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View home = inflate(R.layout.fragment_home);
            HomeDashboard consumption = new HomeDashboard();
            consumption.monthLiters = new BigDecimal("200");
            MonthlyGoalCard.renderGuidance(home, new BigDecimal("300"), consumption, LocalDate.of(2026, 11, 10));
            assertEquals("4,76 L/dia", ((TextView) home.findViewById(R.id.home_daily_limit)).getText().toString());
            assertEquals("50%", ((TextView) home.findViewById(R.id.home_reduction)).getText().toString());
            assertEquals("300 L/mês", ((TextView) home.findViewById(R.id.home_savings)).getText().toString());
        });
    }
}
