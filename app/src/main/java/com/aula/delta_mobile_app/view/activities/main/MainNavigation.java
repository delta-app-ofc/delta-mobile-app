package com.aula.delta_mobile_app.view.activities.main;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import com.aula.delta_mobile_app.R;

public final class MainNavigation {
    private MainNavigation() {}

    public static void open(NavController nav, int destination) {
        if (nav.getCurrentDestination() == null || nav.getCurrentDestination().getId() == destination) return;
        if (destination == R.id.homeFragment && nav.popBackStack(R.id.homeFragment, false)) return;
        nav.navigate(destination, null, new NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).setLaunchSingleTop(true).build());
    }

    public static int tab(int destination) {
        if (destination == R.id.leakDetectionFragment || destination == R.id.consumptionFragment || destination == R.id.leakHourlyFragment
            || destination == R.id.leakAbnormalFragment) return R.id.menu_consumption;
        if (destination == R.id.costsFragment || destination == R.id.costsHistoryFragment) return R.id.menu_costs;
        if (destination == R.id.tipsFragment) return R.id.menu_tips;
        if (destination == R.id.goalsFragment) return R.id.menu_goals;
        return R.id.menu_home;
    }
}
