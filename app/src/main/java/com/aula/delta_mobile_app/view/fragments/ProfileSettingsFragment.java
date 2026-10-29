package com.aula.delta_mobile_app.view.fragments;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ProfileSettingsFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private ProfileSettingsViewModel model;
    private WaterViewModel water;
    private ImageView avatar;
    private TextView greeting, notice;
    private View previewActions;
    private ActivityResultLauncher<Uri> camera;
    private ActivityResultLauncher<PickVisualMediaRequest> gallery;
    private ActivityResultLauncher<String> permission;
    private Uri shownPhoto;

    public ProfileSettingsFragment() { super(R.layout.fragment_profile_settings); }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        model = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        camera = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            String value = model.captureUri();
            if (Boolean.TRUE.equals(success) && value != null) model.preparePhoto(Uri.parse(value));
            else cleanupCapture();
        });
        gallery = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> { if (uri != null) model.preparePhoto(uri); });
        permission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), allowed -> {
            if (allowed) takePhoto();
            else Toast.makeText(requireContext(), R.string.profile_camera_denied, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.profile_background);
        water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        view.findViewById(R.id.profile_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(ProfileSettingsFragment.this::back); }
        });
        LinearLayout identity = new LinearLayout(requireContext());
        identity.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout photo = new FrameLayout(requireContext());
        avatar = new ImageView(requireContext());
        avatar.setBackgroundResource(R.drawable.profile_avatar);
        avatar.setClipToOutline(true);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setContentDescription(getString(R.string.profile_edit_photo));
        FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(dp(68), dp(68), Gravity.TOP | Gravity.START);
        photo.addView(avatar, avatarParams);
        ImageButton edit = new ImageButton(requireContext());
        edit.setImageResource(R.drawable.profile_edit);
        edit.setBackground(new android.graphics.drawable.InsetDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.profile_blue_icon), dp(10)));
        edit.setPadding(dp(13), dp(13), dp(13), dp(13));
        edit.setContentDescription(getString(R.string.profile_edit_photo));
        photo.addView(edit, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.BOTTOM | Gravity.END));
        edit.setOnClickListener(v -> choosePhoto());
        identity.addView(photo, new LinearLayout.LayoutParams(dp(84), dp(84)));
        greeting = new TextView(requireContext());
        greeting.setTextAppearance(R.style.Profile_Title);
        greeting.setTextSize(20);
        greeting.setText(R.string.profile_hello_default);
        identity.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(identity);
        notice = new TextView(requireContext());
        notice.setTextAppearance(R.style.Profile_Body);
        notice.setTextColor(0xFFCED6E6);
        notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
        noticeParams.topMargin = dp(12);
        body.addView(notice, noticeParams);
        LinearLayout preview = new LinearLayout(requireContext());
        preview.setOrientation(LinearLayout.VERTICAL);
        button(preview, R.string.profile_send_photo, () -> message(R.string.profile_upload_unsupported));
        button(preview, R.string.profile_discard_photo, () -> model.discardPhoto());
        body.addView(preview);
        previewActions = preview;
        previewActions.setVisibility(View.GONE);
        LinearLayout first = group(body);
        row(first, R.string.profile_personal_title, R.drawable.home_icon_person, () -> leaveProfile(() -> navigate(R.id.personalInformationFragment)));
        divider(first);
        row(first, R.string.profile_residential_title, R.drawable.main_icon_home, () -> leaveProfile(() -> navigate(R.id.residentialInformationFragment)));
        divider(first);
        row(first, R.string.profile_units, R.drawable.main_icon_goals, this::units);
        LinearLayout second = group(body);
        row(second, R.string.profile_sensors, R.drawable.home_icon_bell, this::sensors);
        divider(second);
        row(second, R.string.profile_performance, R.drawable.main_icon_consumption, () -> message(R.string.profile_performance_unavailable));
        button(body, R.string.profile_retry, () -> { model.loadProfile(); water.loadProperties(); });
        button(body, R.string.auth_logout, () -> {
            registration().reset();
            water.reset();
            new ViewModelProvider(requireActivity()).get(LoginViewModel.class).logout();
        });
        model.profile.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                String name = value.data.name == null ? "" : value.data.name.trim();
                greeting.setText(name.isEmpty() ? getString(R.string.profile_hello_default) : getString(R.string.profile_hello, name.split("\\s+")[0]));
            } else greeting.setText(R.string.profile_hello_default);
            renderNotice();
        });
        model.photo.observe(getViewLifecycleOwner(), value -> {
            Uri uri = value.data == null ? null : Uri.fromFile(value.data);
            if (!java.util.Objects.equals(shownPhoto, uri)) { avatar.setImageURI(uri); shownPhoto = uri; }
            previewActions.setVisibility(value.status == ScreenState.Status.SUCCESS ? View.VISIBLE : View.GONE);
            renderNotice();
        });
        model.loadProfile();
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
    }

    private void renderNotice() {
        ScreenState<?> photo = model.photo.getValue(), profile = model.profile.getValue();
        String text = photo.busy() ? getString(R.string.profile_photo_loading) : photo.status == ScreenState.Status.ERROR ? photo.message
            : photo.status == ScreenState.Status.SUCCESS ? getString(R.string.profile_photo_preview)
            : profile.busy() ? getString(R.string.profile_loading) : profile.status == ScreenState.Status.ERROR ? profile.message : "";
        notice.setText(text);
        notice.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void choosePhoto() {
        if (model.photo.getValue().busy()) return;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_edit_photo).setItems(new String[] {
            getString(R.string.profile_take_photo), getString(R.string.profile_choose_photo)
        }, (dialog, which) -> {
            if (which == 1) gallery.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto();
            else permission.launch(Manifest.permission.CAMERA);
        }).setNegativeButton(R.string.profile_cancel, null).show();
    }

    private void takePhoto() {
        cleanupCapture();
        try {
            File directory = new File(requireContext().getCacheDir(), "profile");
            if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException();
            File file = File.createTempFile("capture-", ".jpg", directory);
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".profile.files", file);
            model.captureUri(uri.toString());
            camera.launch(uri);
        } catch (java.io.IOException | ActivityNotFoundException | IllegalArgumentException failure) {
            cleanupCapture();
            Toast.makeText(requireContext(), R.string.profile_camera_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void cleanupCapture() {
        String capture = model.captureUri();
        if (capture == null) return;
        try { requireContext().getContentResolver().delete(Uri.parse(capture), null, null); } catch (RuntimeException ignored) {}
        model.captureUri(null);
    }

    private void units() {
        ScreenState<List<UserPropertyResponse>> value = water.properties.getValue();
        if (value.status != ScreenState.Status.SUCCESS) { water.loadProperties(); message(R.string.profile_loading); return; }
        List<UserPropertyResponse> links = new ArrayList<>();
        for (UserPropertyResponse link : value.data) if (link != null && link.propertyId != null) links.add(link);
        if (links.isEmpty()) { message(R.string.home_no_properties); return; }
        String[] names = new String[links.size()];
        int checked = -1;
        for (int i = 0; i < links.size(); i++) {
            names[i] = links.get(i).propertyName == null ? getString(R.string.dash_property) : links.get(i).propertyName;
            if (java.util.Objects.equals(water.propertyId, links.get(i).propertyId)) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_select_property).setSingleChoiceItems(names, checked, (dialog, item) -> {
            dialog.dismiss();
            if (!java.util.Objects.equals(water.propertyId, links.get(item).propertyId)) {
                water.select(links.get(item).propertyId);
                model.loadResidence(water.propertyId, true);
            }
        }).setNegativeButton(R.string.profile_cancel, null).show();
    }

    private void sensors() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_sensors)
            .setMessage(R.string.profile_sensors_unavailable).setNegativeButton(R.string.profile_cancel, null);
        if (water.propertyId != null) dialog.setPositiveButton(R.string.profile_link_sensor, (d, which) -> navigate(R.id.qrScanFragment));
        dialog.show();
    }

    private LinearLayout group(LinearLayout parent) {
        LinearLayout group = new LinearLayout(requireContext());
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundResource(R.drawable.profile_group);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(28);
        parent.addView(group, params);
        return group;
    }

    private void row(LinearLayout parent, int label, int drawable, Runnable action) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(60));
        row.setPadding(dp(16), dp(10), dp(12), dp(10));
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(drawable);
        icon.setColorFilter(0xFFFFFFFF);
        icon.setBackgroundResource(R.drawable.profile_blue_icon);
        icon.setPadding(dp(7), dp(7), dp(7), dp(7));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView text = new TextView(requireContext());
        text.setTextAppearance(R.style.Profile_Body);
        text.setTextSize(18);
        text.setText(label);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMarginStart(dp(12));
        row.addView(text, textParams);
        TextView arrow = new TextView(requireContext());
        arrow.setText("›");
        arrow.setTextColor(0xFFFFFFFF);
        arrow.setTextSize(30);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) row.setForeground(ContextCompat.getDrawable(requireContext(), ripple.resourceId));
        parent.addView(row);
    }

    private void divider(LinearLayout parent) {
        View line = new View(requireContext());
        line.setBackgroundColor(0x224D5052);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMarginStart(dp(4));
        params.setMarginEnd(dp(4));
        parent.addView(line, params);
    }

    private void button(LinearLayout parent, int label, Runnable action) {
        com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(requireContext(), null, android.R.attr.borderlessButtonStyle);
        button.setText(label);
        button.setTextColor(0xFFB5CAF6);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-2, -2));
    }

    private void message(int text) { new MaterialAlertDialogBuilder(requireContext()).setMessage(text).setPositiveButton(android.R.string.ok, null).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void leaveProfile(Runnable action) {
        if (model.photo.getValue().status == ScreenState.Status.SUCCESS || model.photo.getValue().busy()) {
            new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
                .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { model.discardPhoto(); action.run(); }).show();
        } else action.run();
    }

    @Override public void onDestroyView() { avatar = null; greeting = notice = null; previewActions = null; shownPhoto = null; super.onDestroyView(); }
}
