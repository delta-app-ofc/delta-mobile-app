package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;

public abstract class AuthFlowFragment extends Fragment {
    private android.app.Dialog loadingDialog;
    private boolean loadingRequested;

    protected void showLoading(boolean loading) {
        loadingRequested = loading;
        if (!loading) {
            dismissLoading();
            return;
        }
        if (loadingDialog != null || !isAdded()) return;
        loadingDialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Material_NoActionBar);
        loadingDialog.setCancelable(canCancelLoading());
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setOnCancelListener(dialog -> {
            loadingDialog = null;
            loadingRequested = false;
            onLoadingBack();
        });
        loadingDialog.setContentView(new com.aula.delta_mobile_app.view.LoadingView(requireContext()));
        loadingDialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP) onLoadingBack();
                return true;
            }
            return false;
        });
        if (loadingDialog.getWindow() != null)
            loadingDialog.getWindow().setWindowAnimations(R.style.Animation_Delta_Loading);
        loadingDialog.show();
        android.view.Window window = loadingDialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(0xFF222222);
            window.setNavigationBarColor(0xFF222222);
            androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
            androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(false);
        }
    }

    protected void onLoadingBack() {}

    protected boolean canCancelLoading() {
        return false;
    }

    private void dismissLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (loadingRequested) showLoading(true);
    }

    @Override
    public void onStop() {
        dismissLoading();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        loadingRequested = false;
        dismissLoading();
        super.onDestroyView();
    }

    protected AuthFlowFragment(@LayoutRes int layoutId) {
        super(layoutId);
    }

    protected Bundle flow() {
        return registration().draft;
    }

    protected com.aula.delta_mobile_app.viewmodel.SignupViewModel registration() {
        return new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.aula.delta_mobile_app.viewmodel.SignupViewModel.class);
    }

    protected TextView addStatus(View root) {
        android.view.ViewGroup content = root instanceof android.widget.ScrollView ? (android.view.ViewGroup) ((android.widget.ScrollView) root).getChildAt(0)
            : (android.view.ViewGroup) root;
        TextView status = new com.aula.delta_mobile_app.view.StatusNoticeView(requireContext());
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(-1, -2);
        params.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        content.addView(status, Math.max(0, content.getChildCount() - 2), params);
        return status;
    }

    protected void showStatus(TextView label, View button, com.aula.delta_mobile_app.model.ScreenState<?> state) {
        if (state == null) return;
        label.setText(state.displayMessage());
        button.setEnabled(!state.busy());
        button.setAlpha(state.busy() ? 0.5f : 1f);
    }

    protected void bindPasswordVisibility(EditText field, View toggle) {
        toggle.setContentDescription("Mostrar senha");
        toggle.setOnClickListener(v -> {
            boolean hidden = field.getTransformationMethod()
            instanceof android.text.method.PasswordTransformationMethod;
            int start = field.getSelectionStart(), end = field.getSelectionEnd();
            field.setTransformationMethod(hidden ? android.text.method.HideReturnsTransformationMethod.getInstance() : android.text.method.PasswordTransformationMethod
                .getInstance());
            if (start >= 0 && end >= 0) field.setSelection(start, end);
            toggle.setContentDescription(hidden ? "Ocultar senha" : "Mostrar senha");
        });
    }

    protected void openAuthenticated() {
        if (!(requireActivity() instanceof com.aula.delta_mobile_app.view.activities.main.MainActivity)) {
            startActivity(new android.content.Intent(requireContext(), com.aula.delta_mobile_app.view.activities.main.MainActivity.class));
            requireActivity().finish();
            return;
        }
        androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.homeFragment, null, new NavOptions.Builder().setEnterAnim(R.anim.fragment_enter_left)
            .setExitAnim(R.anim.fragment_exit_left).setPopUpTo(R.id.auth_navigation, true).build());
    }

    protected void navigate(int destinationId) {
        NavOptions transition = new NavOptions.Builder().setEnterAnim(R.anim.fragment_enter_left).setExitAnim(R.anim.fragment_exit_left)
            .setPopEnterAnim(R.anim.fragment_enter_right).setPopExitAnim(R.anim.fragment_exit_right).build();
        NavHostFragment.findNavController(this).navigate(destinationId, null, transition);
    }

    protected void back() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    protected void backToLogin() {
        NavHostFragment.findNavController(this).popBackStack(R.id.loginFragment, false);
    }

    protected void setupHeader(View root, int step,
        @StringRes int title,
        @StringRes int subtitle,
        @StringRes int description) {
        setupHeader(root, step, title, subtitle, description, v -> back());
    }

    protected void setupHeader(View root, int step,
        @StringRes int title,
        @StringRes int subtitle,
        @StringRes int description, View.OnClickListener backAction) {
        setScreenColor(R.color.auth_blue);
        root.findViewById(R.id.back_button).setOnClickListener(backAction);
        ((TextView) root.findViewById(R.id.step_title)).setText(title);
        ((TextView) root.findViewById(R.id.step_subtitle)).setText(subtitle);
        ((TextView) root.findViewById(R.id.step_description)).setText(description);
        int[] progressIds = {
            R.id.progress_0, R.id.progress_1, R.id.progress_2, R.id.progress_3, R.id.progress_4
        };
        for (int i = 0; i < progressIds.length; i++) {
            View line = root.findViewById(progressIds[i]);
            line.setBackgroundColor(ContextCompat.getColor(requireContext(), i <= step ? R.color.auth_white : R.color.auth_muted));
        }
    }

    protected void setScreenColor(int colorId) {
        View host = requireActivity().findViewById(R.id.auth_nav_host);
        if (host == null) host = requireActivity().findViewById(R.id.main_nav_host);
        host.setBackgroundColor(ContextCompat.getColor(requireContext(), colorId));
    }

    protected void restore(EditText field, String key) {
        boolean secret = key.equals("password") || key.equals("passwordConfirm");
        if (secret) field.setSaveEnabled(false);
        field.setText(secret ? registration().secret(key) : flow().getString(key, ""));
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (secret) registration().secret(key, s.toString());
                else flow().putString(key, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    protected void save(EditText field, String key) {
        flow().putString(key, text(field));
    }

    protected String text(EditText field) {
        return field.getText().toString().trim();
    }

    protected boolean required(EditText field) {
        if (!text(field).isEmpty()) return true;
        field.setError(getString(R.string.auth_required));
        field.requestFocus();
        return false;
    }

    protected void watchButton(TextView button, EditText... fields) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                update();
            }

            @Override
            public void afterTextChanged(Editable s) {}

            private void update() {
                boolean filled = true;
                for (EditText field : fields) filled &= !text(field).isEmpty();
                button.setBackgroundResource(filled ? R.drawable.auth_button_white : R.drawable.auth_button_muted);
                button.setTextColor(ContextCompat.getColor(requireContext(), filled ? R.color.auth_blue : R.color.auth_white));
            }
        };
        for (EditText field : fields) field.addTextChangedListener(watcher);
        boolean filled = true;
        for (EditText field : fields) filled &= !text(field).isEmpty();
        button.setBackgroundResource(filled ? R.drawable.auth_button_white : R.drawable.auth_button_muted);
        button.setTextColor(ContextCompat.getColor(requireContext(), filled ? R.color.auth_blue : R.color.auth_white));
    }
}
