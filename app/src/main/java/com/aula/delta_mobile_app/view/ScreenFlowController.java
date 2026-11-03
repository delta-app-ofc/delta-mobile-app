package com.aula.delta_mobile_app.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenTransitionState;

public final class ScreenFlowController {
    private static final long SLIDE_MS = 280;
    private static final long LOADING_HOLD_MS = 120;
    private final View content;
    private final FrameLayout overlay;
    private final LoadingView loading;
    private final ScreenTransitionState state = new ScreenTransitionState();
    private View owner;
    private boolean busy, resumed, blocked, destroyed;
    private long loadingAt;
    private ImageView snapshot;
    private Bitmap bitmap;
    private Runnable activityAction, revealTask;

    public ScreenFlowController(ViewGroup root) {
        content = root.getChildAt(0);
        overlay = new FrameLayout(root.getContext());
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setBackgroundColor(0xFF222222);
        loading = new LoadingView(root.getContext());
        loading.setId(R.id.screen_flow_loading);
        overlay.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        root.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
        content.setVisibility(View.INVISIBLE);
        int token = state.begin();
        state.loadingVisible(token);
        loadingAt = SystemClock.uptimeMillis();
    }

    public void navigation() {
        if (state.phase() == ScreenTransitionState.Phase.CONTENT || state.phase() == ScreenTransitionState.Phase.ENTERING_CONTENT) enterLoading(true);
        else {
            owner = null;
            resumed = false;
            busy = false;
            state.ready(false);
        }
    }

    private void enterLoading(boolean changeOwner) {
        if (destroyed) return;
        boolean capture = state.phase() == ScreenTransitionState.Phase.CONTENT;
        int token = state.begin();
        cancelAnimations();
        clearSnapshot();
        if (changeOwner) { owner = null; resumed = false; }
        if (capture && content.getWidth() > 0 && content.getHeight() > 0) {
            try {
                bitmap = Bitmap.createBitmap(content.getWidth(), content.getHeight(), Bitmap.Config.ARGB_8888);
                content.draw(new Canvas(bitmap));
                snapshot = new ImageView(content.getContext());
                snapshot.setImageBitmap(bitmap);
                snapshot.setScaleType(ImageView.ScaleType.FIT_XY);
                overlay.addView(snapshot, 0, new FrameLayout.LayoutParams(-1, -1));
            } catch (RuntimeException | OutOfMemoryError failure) { clearSnapshot(); }
        }
        content.setVisibility(View.INVISIBLE);
        content.setTranslationX(0);
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();
        int width = Math.max(overlay.getWidth(), content.getWidth());
        loading.setTranslationX(snapshot == null ? 0 : width);
        if (snapshot != null) snapshot.animate().translationX(-width).setDuration(SLIDE_MS).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        if (snapshot == null) entered(token);
        else loading.animate().translationX(0).setDuration(SLIDE_MS).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(() -> entered(token)).start();
    }

    private void entered(int token) {
        if (destroyed || token != state.generation()) return;
        clearSnapshot();
        state.loadingVisible(token);
        loadingAt = SystemClock.uptimeMillis();
        if (activityAction != null) {
            Runnable action = activityAction;
            activityAction = null;
            action.run();
        } else revealWhenReady();
    }

    public void present(View view, boolean loadingData) {
        owner = view;
        busy = loadingData;
        resumed = false;
        revealWhenReady();
    }

    public void resumed(View view) { if (owner == view) { resumed = true; revealWhenReady(); } }

    public void loading(View view, boolean value) {
        if (owner != view || destroyed) return;
        busy = value;
        if (value && (state.phase() == ScreenTransitionState.Phase.CONTENT || state.phase() == ScreenTransitionState.Phase.ENTERING_CONTENT)) enterLoading(false);
        revealWhenReady();
    }

    public void block(boolean value) { blocked = value; revealWhenReady(); }

    public void openActivity(Runnable action) {
        activityAction = action;
        if (state.phase() == ScreenTransitionState.Phase.CONTENT || state.phase() == ScreenTransitionState.Phase.ENTERING_CONTENT) enterLoading(true);
        else if (state.phase() == ScreenTransitionState.Phase.LOADING) {
            activityAction = null;
            action.run();
        }
    }

    private void revealWhenReady() {
        if (destroyed) return;
        state.ready(owner != null && resumed && !busy && !blocked && activityAction == null);
        if (state.phase() != ScreenTransitionState.Phase.LOADING || owner == null || !resumed || busy || blocked || activityAction != null) return;
        if (revealTask != null) overlay.removeCallbacks(revealTask);
        int token = state.generation();
        revealTask = () -> {
            if (destroyed || !state.reveal(token)) return;
            int width = Math.max(overlay.getWidth(), content.getWidth());
            content.setVisibility(View.VISIBLE);
            content.setTranslationX(width);
            loading.animate().translationX(-width).setDuration(SLIDE_MS).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            content.animate().translationX(0).setDuration(SLIDE_MS).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(() -> {
                if (destroyed || token != state.generation()) return;
                overlay.setVisibility(View.GONE);
                loading.setTranslationX(0);
                state.complete(token);
            }).start();
        };
        overlay.postDelayed(revealTask, Math.max(0, LOADING_HOLD_MS - (SystemClock.uptimeMillis() - loadingAt)));
    }

    private void cancelAnimations() {
        content.animate().cancel();
        loading.animate().cancel();
        if (snapshot != null) snapshot.animate().cancel();
        if (revealTask != null) overlay.removeCallbacks(revealTask);
    }

    private void clearSnapshot() {
        if (snapshot != null) { snapshot.setImageDrawable(null); overlay.removeView(snapshot); snapshot = null; }
        bitmap = null;
    }

    public void destroy() {
        destroyed = true;
        cancelAnimations();
        clearSnapshot();
        activityAction = null;
        owner = null;
    }
}
