package com.example.myapplication.ui.ar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.ml.ObjectRecognitionHelper;
import com.example.myapplication.ui.practice.PracticeActivity;
import com.example.myapplication.view.FocusBoxView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealtimeActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "RealtimeActivity";
    private static final long ANALYSIS_INTERVAL_MS = 500; // 每500毫秒分析一次

    private PreviewView previewView;
    private TextView resultTextView;
    private ExtendedFloatingActionButton confirmButton;
    private FocusBoxView focusBoxView;
    private ExecutorService cameraExecutor;

    private ObjectRecognitionHelper objectRecognitionHelper;

    private String currentDetectionResult = null;
    private String currentRawLabel = null;
    private boolean isResultLocked = false;
    private long lastAnalysisTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        previewView = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.result_text_view);
        confirmButton = findViewById(R.id.btn_confirm);
        focusBoxView = findViewById(R.id.focusBox);
        ImageButton btnClose = findViewById(R.id.btnClose);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // TODO: Replace 640 with your model's actual input size
        objectRecognitionHelper = new ObjectRecognitionHelper(this, 640, 640);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        previewView.setOnClickListener(v -> {
            if (currentDetectionResult == null) return;

            isResultLocked = !isResultLocked;
            if (isResultLocked) {
                runOnUiThread(() -> {
                    confirmButton.setVisibility(View.VISIBLE);
                    focusBoxView.setVisibility(View.VISIBLE);
                });
            } else {
                runOnUiThread(() -> {
                    confirmButton.setVisibility(View.GONE);
                    focusBoxView.setVisibility(View.GONE);
                });
            }
        });

        confirmButton.setOnClickListener(v -> {
            if (isResultLocked && currentRawLabel != null) {
                Intent intent = new Intent(RealtimeActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", currentRawLabel);
                startActivity(intent);
            }
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // *** MODIFIED HERE: Throttling logic ***
                    long currentTime = SystemClock.elapsedRealtime();
                    if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
                        imageProxy.close();
                        return;
                    }
                    lastAnalysisTime = currentTime;

                    @SuppressLint("UnsafeOptInUsageError")
                    Bitmap bitmap = imageProxy.toBitmap();

                    objectRecognitionHelper.detectObjects(bitmap, new ObjectRecognitionHelper.RecognitionCallback() {
                        @Override
                        public void onResult(String recognizedWord, float confidence, RectF boundingBox) {
                            currentRawLabel = recognizedWord;
                            currentDetectionResult = String.format("%s (%.0f%%)", recognizedWord, confidence * 100);

                            if (!isResultLocked) {
                                runOnUiThread(() -> resultTextView.setText(currentDetectionResult));
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Detection error: " + errorMessage);
                            if (!isResultLocked) {
                                runOnUiThread(() -> resultTextView.setText("Error: " + errorMessage));
                            }
                        }
                    });
                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (objectRecognitionHelper != null) {
            objectRecognitionHelper.close();
        }
    }
}
