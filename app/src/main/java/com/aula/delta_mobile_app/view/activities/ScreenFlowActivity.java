package com.aula.delta_mobile_app.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;
import com.aula.delta_mobile_app.view.ScreenFlowController;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class ScreenFlowActivity extends AppCompatActivity {
    private final Map<View, Boolean> pending = new WeakHashMap<>();
    private ScreenFlowController flow;
    private Fragment screen;
    private boolean openingActivity;
    private final Map<NavHostFragment, Boolean> installedHosts = new WeakHashMap<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override public void onFragmentCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment, Bundle saved) {
                if (fragment instanceof NavHostFragment) installHost((NavHostFragment) fragment);
            }
            @Override public void onFragmentViewCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment, @NonNull View view, Bundle saved) {
                present(fragment, view);
            }
            @Override public void onFragmentResumed(@NonNull FragmentManager manager, @NonNull Fragment fragment) {
                if (fragment.getView() == null) return;
                if (fragment != screen) present(fragment, fragment.getView());
                if (flow != null && fragment == screen) flow.resumed(fragment.getView());
            }
        }, true);
    }

    @Override
    public void setContentView(int layout) {
        super.setContentView(layout);
        flow = new ScreenFlowController(findViewById(android.R.id.content));
        for (Fragment fragment : getSupportFragmentManager().getFragments()) if (fragment instanceof NavHostFragment) {
            NavHostFragment host = (NavHostFragment) fragment;
            installHost(host);
            Fragment current = host.getChildFragmentManager().getPrimaryNavigationFragment();
            if (current != null && current.getView() != null) present(current, current.getView());
        }
        if (screen != null && screen.getView() != null) flow.present(screen.getView(), Boolean.TRUE.equals(pending.get(screen.getView())));
    }

    @Override protected void onPostResume() {
        super.onPostResume();
        if (flow == null) return;
        if (screen != null && screen.getView() != null) flow.resumed(screen.getView());
        else if (installedHosts.isEmpty()) {
            ViewGroup root = findViewById(android.R.id.content);
            flow.present(root.getChildAt(0), false);
            flow.resumed(root.getChildAt(0));
        }
    }

    public void setScreenLoading(View owner, boolean loading) {
        if (owner == null) return;
        pending.put(owner, loading);
        if (flow != null) flow.loading(owner, loading);
    }

    private void installHost(NavHostFragment host) {
        if (installedHosts.put(host, true) != null) return;
        host.getNavController().addOnDestinationChangedListener((nav, destination, args) -> { if (flow != null) flow.navigation(); });
    }

    private void present(Fragment fragment, View view) {
        if (!(fragment.getParentFragment() instanceof NavHostFragment)) return;
        NavHostFragment host = (NavHostFragment) fragment.getParentFragment();
        if (host.getChildFragmentManager().getPrimaryNavigationFragment() != fragment) return;
        screen = fragment;
        if (flow != null) flow.present(view, Boolean.TRUE.equals(pending.get(view)));
    }

    protected void blockScreen(boolean block) { if (flow != null) flow.block(block); }

    public void openScreen(Intent intent, boolean finishCurrent) {
        if (openingActivity || isFinishing() || isDestroyed()) return;
        openingActivity = true;
        Runnable action = () -> {
            if (isDestroyed()) return;
            startActivity(intent);
            overridePendingTransition(0, 0);
            if (finishCurrent) finish();
            else openingActivity = false;
        };
        if (flow == null) action.run();
        else flow.openActivity(action);
    }

    @Override protected void onDestroy() { if (flow != null) flow.destroy(); super.onDestroy(); }
}
