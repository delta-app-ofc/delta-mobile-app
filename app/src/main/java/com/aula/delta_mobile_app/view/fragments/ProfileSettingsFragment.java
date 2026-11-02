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

public final class ProfileSettingsFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private ProfileSettingsViewModel model;
    private WaterViewModel water;
    private ImageView avatar;
    private TextView greeting, notice, residenceName;
    private View identitySection, firstSection;
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
        showLoading(true);
        water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        view.findViewById(R.id.profile_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(ProfileSettingsFragment.this::back); }
        });
        LinearLayout identity = new LinearLayout(requireContext());
        identitySection = identity;
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
        LinearLayout labels = new LinearLayout(requireContext());
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(greeting, new LinearLayout.LayoutParams(-1, -2));
        residenceName = new TextView(requireContext());
        residenceName.setTextAppearance(R.style.Profile_Body);
        residenceName.setTextSize(12);
        residenceName.setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_muted));
        LinearLayout.LayoutParams residenceParams = new LinearLayout.LayoutParams(-1, -2);
        residenceParams.topMargin = dp(4);
        labels.addView(residenceName, residenceParams);
        identity.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton logout = new ImageButton(requireContext());
        logout.setImageResource(R.drawable.profile_logout);
        logout.setContentDescription(getString(R.string.auth_logout));
        logout.setBackgroundResource(android.R.color.transparent);
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true)) logout.setBackgroundResource(ripple.resourceId);
        logout.setPadding(dp(12), dp(12), dp(12), dp(12));
        logout.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.auth_logout)
            .setMessage(R.string.profile_logout_confirmation)
            .setNegativeButton(R.string.profile_cancel, null)
            .setPositiveButton(R.string.auth_logout, (dialog, which) -> {
                registration().reset();
                water.reset();
                new ViewModelProvider(requireActivity()).get(LoginViewModel.class).logout();
            }).show());
        identity.addView(logout, new LinearLayout.LayoutParams(dp(48), dp(48)));
        body.addView(identity);
        notice = new com.aula.delta_mobile_app.view.StatusNoticeView(requireContext());
        notice.setTextAppearance(R.style.Profile_Body);
        notice.setTextColor(0xFFCED6E6);
        notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
        noticeParams.topMargin = dp(12);
        body.addView(notice, noticeParams);
        LinearLayout first = group(body);
        firstSection = first;
        row(first, R.string.profile_personal_title, R.drawable.home_icon_person, () -> leaveProfile(() -> navigate(R.id.personalInformationFragment)));
        divider(first);
        row(first, R.string.profile_residential_title, R.drawable.main_icon_home, () -> leaveProfile(() -> navigate(R.id.residentialInformationFragment)));
        divider(first);
        row(first, R.string.profile_units, R.drawable.profile_icon_units, this::units);
        model.profile.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                String name = value.data.name == null ? "" : value.data.name.trim();
                greeting.setText(name.isEmpty() ? getString(R.string.profile_hello_default) : getString(R.string.profile_hello, name.split("\\s+")[0]));
                model.loadPhoto();
            } else greeting.setText(R.string.profile_hello_default);
            renderNotice();
        });
        model.photo.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                Uri uri = Uri.fromFile(value.data);
                if (!java.util.Objects.equals(shownPhoto, uri)) { avatar.setImageURI(uri); shownPhoto = uri; }
            } else if (value.status == ScreenState.Status.IDLE) {
                avatar.setImageDrawable(null);
                shownPhoto = null;
            } else if (value.status == ScreenState.Status.ERROR) {
                String error = value.message;
                model.discardPhoto();
                new MaterialAlertDialogBuilder(requireContext()).setMessage(error).setPositiveButton(android.R.string.ok, null).show();
            }
            renderNotice();
        });
        model.loadProfile(true);
        water.properties.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS && value.data != null && water.propertyId == null) {
                for (UserPropertyResponse link : value.data) if (link != null && link.propertyId != null) { water.propertyId = link.propertyId; break; }
            }
            renderNotice();
        });
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
        model.residence.observe(getViewLifecycleOwner(), value -> renderNotice());
    }

    private void renderNotice() {
        ScreenState<?> photo = model.photo.getValue(), profile = model.profile.getValue();
        ScreenState<?> properties = water.properties.getValue();
        boolean ready = profile.status == ScreenState.Status.SUCCESS && properties.status == ScreenState.Status.SUCCESS;
        showLoading(profile.status == ScreenState.Status.IDLE || profile.busy() || properties.status == ScreenState.Status.IDLE || properties.busy() || photo.busy());
        identitySection.setVisibility(ready ? View.VISIBLE : View.GONE);
        firstSection.setVisibility(ready ? View.VISIBLE : View.GONE);
        String propertyName = "";
        if (ready && water.properties.getValue().data != null) {
            for (UserPropertyResponse link : water.properties.getValue().data) {
                if (link == null || !java.util.Objects.equals(link.userId, model.profile.getValue().data.id) || link.propertyId == null || !link.propertyId.equals(water.propertyId)) continue;
                propertyName = link.propertyName == null ? "" : link.propertyName.trim();
                ScreenState<com.aula.delta_mobile_app.model.ProfileSettingsData> residence = model.residence.getValue();
                if (residence.status == ScreenState.Status.SUCCESS && link.propertyId.equals(residence.data.property.id))
                    propertyName = residence.data.property.name == null ? "" : residence.data.property.name.trim();
                break;
            }
        }
        residenceName.setText(propertyName);
        residenceName.setVisibility(propertyName.isEmpty() ? View.GONE : View.VISIBLE);
        String text = profile.busy() ? getString(R.string.profile_loading) : profile.status == ScreenState.Status.ERROR ? profile.message
            : properties.status == ScreenState.Status.ERROR ? properties.message : "";
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
        leaveProfile(() -> navigate(R.id.unitsFragment));
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

    private void message(int text) { new MaterialAlertDialogBuilder(requireContext()).setMessage(text).setPositiveButton(android.R.string.ok, null).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void leaveProfile(Runnable action) {
        if (!model.photo.getValue().busy()) action.run();
    }

    @Override public void onDestroyView() { avatar = null; greeting = notice = residenceName = null; identitySection = firstSection = null; shownPhoto = null; super.onDestroyView(); }
}
