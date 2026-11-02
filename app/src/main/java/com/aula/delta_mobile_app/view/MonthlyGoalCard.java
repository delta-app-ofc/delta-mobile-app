package com.aula.delta_mobile_app.view;

import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.MonthlyGoalStorage;
import com.aula.delta_mobile_app.repository.SQLiteGoalStore;
import com.aula.delta_mobile_app.viewmodel.HomeViewModel;
import com.aula.delta_mobile_app.viewmodel.WaterViewModel;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MonthlyGoalCard {
    private MonthlyGoalCard() {}

    public static void bind(Fragment fragment, View view, HomeViewModel home, WaterViewModel water) {
        MonthlyGoalStorage storage = new MonthlyGoalStorage(SQLiteGoalStore.getInstance(fragment.requireContext()));
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        int[] revision = { 0 };
        BigDecimal[] confirmedGoal = { null };
        Integer[] goalProperty = { null };
        YearMonth[] goalMonth = { null };
        Runnable guidance = () -> renderGuidance(view, java.util.Objects.equals(goalProperty[0], water.propertyId) && YearMonth.now().equals(goalMonth[0]) ? confirmedGoal[0] : null,
            home.dashboard().getValue(), java.time.LocalDate.now());
        Runnable render = () -> {
            int expected = ++revision[0];
            if (fragment.getView() != view) return;
            confirmedGoal[0] = null;
            goalProperty[0] = null;
            goalMonth[0] = YearMonth.now();
            guidance.run();
            TextView amount = view.findViewById(R.id.home_goal_value);
            TextView reference = view.findViewById(R.id.home_goal_reference);
            TextView property = view.findViewById(R.id.home_goal_property);
            if (property != null) property.setText("");
            Locale locale = new Locale("pt", "BR");
            YearMonth month = YearMonth.now();
            reference.setText(fragment.getString(R.string.goal_local_month, month.format(DateTimeFormatter.ofPattern("MMMM/uuuu", locale))));
            amount.setText(R.string.goal_not_set);
            ScreenState<Profile> profile = home.user.getValue();
            if (profile.status == ScreenState.Status.ERROR || water.properties.getValue().status == ScreenState.Status.ERROR) {
                amount.setText(R.string.goal_read_error);
                return;
            }
            if (profile.status != ScreenState.Status.SUCCESS || profile.data.id == null || water.properties.getValue().status != ScreenState.Status.SUCCESS) return;
            boolean owned = false;
            for (UserPropertyResponse link : water.properties.getValue().data)
                if (link != null && profile.data.id.equals(link.userId) && link.propertyId != null && link.propertyId.equals(water.propertyId)) {
                    owned = true;
                    if (property != null) property.setText(link.propertyName == null || link.propertyName.trim().isEmpty() ? fragment.getString(R.string.goal_property, link.propertyId) : link.propertyName);
                    break;
                }
            if (!owned) return;
            int userId = profile.data.id, propertyId = water.propertyId;
            goalProperty[0] = propertyId;
            android.content.res.Resources resources = view.getResources();
            amount.setText(R.string.goal_loading);
            worker.execute(() -> {
                String text;
                BigDecimal loaded = null;
                try {
                    BigDecimal liters = storage.read(userId, propertyId, month);
                    loaded = liters;
                    NumberFormat format = NumberFormat.getNumberInstance(locale);
                    format.setMaximumFractionDigits(2);
                    text = liters == null ? resources.getString(R.string.goal_not_set) : resources.getString(R.string.home_liters, format.format(liters));
                } catch (RuntimeException failure) { text = resources.getString(R.string.goal_read_error); }
                String result = text;
                BigDecimal goal = loaded;
                main.post(() -> {
                    if (expected == revision[0] && fragment.getView() == view) {
                        amount.setText(result);
                        confirmedGoal[0] = goal;
                        guidance.run();
                    }
                });
            });
        };
        home.user.observe(fragment.getViewLifecycleOwner(), value -> render.run());
        water.properties.observe(fragment.getViewLifecycleOwner(), value -> render.run());
        home.dashboard().observe(fragment.getViewLifecycleOwner(), value -> {
            if (!YearMonth.now().equals(goalMonth[0]) || !java.util.Objects.equals(goalProperty[0], water.propertyId)) render.run();
            else guidance.run();
        });
        fragment.getViewLifecycleOwner().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override public void onResume(LifecycleOwner owner) { render.run(); }
            @Override public void onDestroy(LifecycleOwner owner) { revision[0]++; worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
        });
    }

    static void renderGuidance(View view, BigDecimal goal, com.aula.delta_mobile_app.model.HomeDashboard dashboard, java.time.LocalDate today) {
        com.aula.delta_mobile_app.model.MonthlyGoalGuidance result = com.aula.delta_mobile_app.model.MonthlyGoalGuidance.calculate(goal, dashboard == null ? null : dashboard.monthLiters, today);
        TextView daily = view.findViewById(R.id.home_daily_limit), reduction = view.findViewById(R.id.home_reduction), savings = view.findViewById(R.id.home_savings);
        TextView title = view.findViewById(R.id.home_goal_guidance_title);
        if (title == null || daily == null || reduction == null || savings == null) return;
        title.setText(result != null && result.exceeded ? R.string.goal_exceeded : R.string.home_stay_on_target);
        if (result == null) {
            daily.setText(R.string.home_pending_metric);
            reduction.setText(R.string.home_pending_metric);
            savings.setText(R.string.home_pending_metric);
            return;
        }
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(2);
        daily.setText(view.getResources().getString(R.string.goal_daily_liters, format.format(result.dailyLiters)));
        reduction.setText(view.getResources().getString(R.string.goal_reduction_percent, format.format(result.reductionPercent)));
        savings.setText(view.getResources().getString(R.string.goal_monthly_savings, format.format(result.savingsLiters)));
    }
}
