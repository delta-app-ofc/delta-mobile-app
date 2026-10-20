package com.aula.delta_mobile_app.view.fragments;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.aula.delta_mobile_app.R;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class QrScanFragment extends AuthFlowFragment {
    private LifecycleCameraController cameraController;
    private BarcodeScanner barcodeScanner;
    private boolean codeAccepted;
    private boolean permissionDenied;
    private com.aula.delta_mobile_app.viewmodel.WaterViewModel water;
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        permissionDenied = !granted;
        if (getView() == null) return;
        if (granted) startCamera(getView());
        else
            showCameraStatus(getView(), shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ? R.string.auth_camera_permission : R.string.auth_camera_settings);
    });

    public QrScanFragment() {
        super(R.layout.fragment_qr_scan);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        codeAccepted = false;
        water = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.aula.delta_mobile_app.viewmodel.WaterViewModel.class);
        setupHeader(view, 4, R.string.auth_qr_title, R.string.auth_qr_subtitle, R.string.auth_qr_description);
        view.findViewById(R.id.qr_back_bottom).setOnClickListener(v -> back());
        view.findViewById(R.id.qr_manual).setOnClickListener(v -> new ManualCodeDialogFragment().show(getChildFragmentManager(), "manual_code"));
        view.findViewById(R.id.camera_status).setOnClickListener(v -> {
            if (hasCameraPermission()) startCamera(view);
            else if (permissionDenied && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().getPackageName())));
            } else cameraPermission.launch(Manifest.permission.CAMERA);
        });
        getChildFragmentManager().setFragmentResultListener("manual_code", getViewLifecycleOwner(), (requestKey, result) -> {
            String code = result.getString("code", "").trim();
            acceptCode(code);
        });
        water.meter.observe(getViewLifecycleOwner(), state -> {
            view.findViewById(R.id.qr_manual).setEnabled(!state.busy());
            if (state.busy()) {
                codeAccepted = true;
                ((TextView) view.findViewById(R.id.camera_status)).setText(state.displayMessage());
                view.findViewById(R.id.camera_status).setVisibility(View.VISIBLE);
            } else if (state.status == com.aula.delta_mobile_app.model.ScreenState.Status.SUCCESS) {
                flow().putString("meterCode", state.data.deviceId);
                water.consumeMeter();
                stopCamera();
                navigate(R.id.qrSuccessFragment);
            } else if (state.status == com.aula.delta_mobile_app.model.ScreenState.Status.ERROR) {
                stopCamera();
                codeAccepted = false;
                ((TextView) view.findViewById(R.id.camera_status)).setText("");
                view.findViewById(R.id.camera_status).setVisibility(View.GONE);
            }
        });
        if (hasCameraPermission()) startCamera(view);
        else cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void showCameraStatus(View root, int message) {
        TextView status = root.findViewById(R.id.camera_status);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void startCamera(View root) {
        if (getView() != root || codeAccepted || cameraController != null) return;
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        LifecycleCameraController controller = new LifecycleCameraController(requireContext());
        BarcodeScanner scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());
        cameraController = controller;
        barcodeScanner = scanner;
        try {
            controller.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
            controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS);
            controller.setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
            controller.setImageAnalysisAnalyzer(executor, new MlKitAnalyzer(Collections.singletonList(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor, result -> {
                if (getView() != root || cameraController != controller || codeAccepted || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.RESUMED) || getChildFragmentManager().findFragmentByTag("manual_code") != null) return;
                List<Barcode> codes = result.getValue(scanner);
                if (codes == null) return;
                for (Barcode barcode : codes) {
                    String value = barcode.getRawValue();
                    if (value != null && !value.trim().isEmpty()) {
                        acceptCode(value.trim());
                        break;
                    }
                }
            }));
            ((PreviewView) root.findViewById(R.id.camera_preview)).setController(controller);
            controller.bindToLifecycle(getViewLifecycleOwner());
            root.findViewById(R.id.camera_status).setVisibility(View.GONE);
            controller.getInitializationFuture().addListener(() -> {
                if (getView() != root || cameraController != controller) return;
                try {
                    controller.getInitializationFuture().get();
                    if (!controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        stopCamera();
                        showCameraStatus(root, R.string.auth_camera_error);
                    }
                } catch (Exception error) {
                    stopCamera();
                    showCameraStatus(root, R.string.auth_camera_error);
                }
            }, executor);
        } catch (RuntimeException error) {
            stopCamera();
            showCameraStatus(root, R.string.auth_camera_error);
        }
    }

    private void acceptCode(String code) {
        if (codeAccepted || code.isEmpty() || getView() == null) return;
        codeAccepted = true;
        stopCamera();
        water.registerMeter(code);
    }

    private void stopCamera() {
        if (cameraController != null) {
            cameraController.clearImageAnalysisAnalyzer();
            cameraController.unbind();
            cameraController = null;
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && hasCameraPermission()) startCamera(getView());
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        super.onDestroyView();
    }

    public static final class ScannerFrame extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator animator;
        private float scanProgress;

        public ScannerFrame(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1800L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(value -> {
                scanProgress = (float) value.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            animator = null;
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            float d = getResources().getDisplayMetrics().density;
            float inset = 4f * d;
            float length = 40f * d;
            float left = inset, top = inset, right = getWidth() - inset, bottom = getHeight() - inset;
            paint.setStrokeWidth(5f * d);
            paint.setAlpha(255);
            canvas.drawLine(left, top + length, left, top + 25f * d, paint);
            canvas.drawArc(left, top, left + 50f * d, top + 50f * d, 180, 90, false, paint);
            canvas.drawLine(left + 25f * d, top, left + length, top, paint);
            canvas.drawLine(right - length, top, right - 25f * d, top, paint);
            canvas.drawArc(right - 50f * d, top, right, top + 50f * d, 270, 90, false, paint);
            canvas.drawLine(right, top + 25f * d, right, top + length, paint);
            canvas.drawLine(left, bottom - length, left, bottom - 25f * d, paint);
            canvas.drawArc(left, bottom - 50f * d, left + 50f * d, bottom, 90, 90, false, paint);
            canvas.drawLine(left + 25f * d, bottom, left + length, bottom, paint);
            canvas.drawLine(right - length, bottom, right - 25f * d, bottom, paint);
            canvas.drawArc(right - 50f * d, bottom - 50f * d, right, bottom, 0, 90, false, paint);
            canvas.drawLine(right, bottom - 25f * d, right, bottom - length, paint);
            paint.setStrokeWidth(2f * d);
            paint.setAlpha(85);
            float y = top + 40f * d + scanProgress * Math.max(0, bottom - top - 80f * d);
            canvas.drawLine(left + 25f * d, y, right - 25f * d, y, paint);
        }
    }
}
