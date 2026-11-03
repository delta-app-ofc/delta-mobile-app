package com.aula.delta_mobile_app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.aula.delta_mobile_app.view.activities.main.MainNavigation;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MainNavigationInstrumentedTest {
    @Navigator.Name("fragment")
    public static final class Screens extends Navigator<NavDestination> {
        @NonNull @Override public NavDestination createDestination() { return new NavDestination(this); }
        @Nullable @Override public NavDestination navigate(@NonNull NavDestination destination, @Nullable Bundle args, @Nullable NavOptions options, @Nullable Extras extras) { return destination; }
        @Override public boolean popBackStack() { return true; }
    }

    @Test
    public void navbarAlwaysOpensItsRootAndDoesNotAccumulateCopies() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            NavController nav = new NavController(InstrumentationRegistry.getInstrumentation().getTargetContext());
            nav.getNavigatorProvider().addNavigator(new Screens());
            nav.setGraph(R.navigation.main_navigation);
            nav.navigate(R.id.costsFragment);
            nav.navigate(R.id.costsHistoryFragment);
            MainNavigation.open(nav, R.id.homeFragment);
            assertEquals(R.id.homeFragment, nav.getCurrentDestination().getId());
            nav.navigate(R.id.profileSettingsFragment);
            nav.navigate(R.id.personalInformationFragment);
            MainNavigation.open(nav, R.id.costsFragment);
            assertEquals(R.id.costsFragment, nav.getCurrentDestination().getId());
            MainNavigation.open(nav, R.id.costsFragment);
            assertTrue(nav.popBackStack());
            assertEquals(R.id.homeFragment, nav.getCurrentDestination().getId());
            nav.navigate(R.id.monthlyGoalsFragment);
            for (int destination : new int[] { R.id.goalsFragment, R.id.tipsFragment, R.id.leakDetectionFragment, R.id.costsFragment, R.id.homeFragment }) {
                MainNavigation.open(nav, destination);
                assertEquals(destination, nav.getCurrentDestination().getId());
            }
        });
    }

    @Test
    public void detailDestinationsHighlightTheirTab() {
        assertEquals(R.id.menu_costs, MainNavigation.tab(R.id.costsHistoryFragment));
        assertEquals(R.id.menu_consumption, MainNavigation.tab(R.id.leakHourlyFragment));
        assertEquals(R.id.menu_consumption, MainNavigation.tab(R.id.leakAbnormalFragment));
        assertEquals(R.id.menu_home, MainNavigation.tab(R.id.monthlyGoalsFragment));
        assertEquals(R.id.menu_home, MainNavigation.tab(R.id.personalInformationFragment));
        assertEquals(R.id.menu_goals, MainNavigation.tab(R.id.goalsFragment));
    }
}
