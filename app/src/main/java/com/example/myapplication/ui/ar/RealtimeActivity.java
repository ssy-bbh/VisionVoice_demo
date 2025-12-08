package com.example.myapplication.ui.ar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
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

    private PreviewView previewView;
    private TextView resultTextView;
    private ExtendedFloatingActionButton confirmButton;
    private FocusBoxView focusBoxView;
    private ExecutorService cameraExecutor; // 用于CameraX的图像分析

    private ObjectRecognitionHelper objectRecognitionHelper; // 新增：识别助手类

    private String currentDetectionResult = null;
    private String currentRawLabel = null;
    private boolean isResultLocked = false;

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

        // 初始化识别助手类
        objectRecognitionHelper = new ObjectRecognitionHelper(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        previewView.setOnClickListener(v -> {
            if (currentDetectionResult == null) return; // 没有结果时不允许锁定

            isResultLocked = !isResultLocked;
            if (isResultLocked) {
                runOnUiThread(() -> {
                    confirmButton.setVisibility(View.VISIBLE);
                    focusBoxView.setVisibility(View.VISIBLE); // Show focus box
                });
            } else {
                runOnUiThread(() -> {
                    confirmButton.setVisibility(View.GONE);
                    focusBoxView.setVisibility(View.GONE); // Hide focus box
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
                    @SuppressLint("UnsafeOptInUsageError")
                    Bitmap bitmap = imageProxy.toBitmap();

                    // 使用 ObjectRecognitionHelper 进行图像分类
                    objectRecognitionHelper.classifyImage(bitmap, new ObjectRecognitionHelper.RecognitionCallback() {
                        @Override
                        public void onResult(String recognizedWord, float confidence) {
                            currentRawLabel = recognizedWord;
                            currentDetectionResult = String.format("%s (%.0f%%)", recognizedWord, confidence * 100);

                            if (!isResultLocked) {
                                runOnUiThread(() -> resultTextView.setText(currentDetectionResult));
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Recognition error: " + errorMessage);
                            if (!isResultLocked) {
                                runOnUiThread(() -> resultTextView.setText("Error: " + errorMessage));
                            }
                        }
                    });
                    imageProxy.close(); // 确保图像帧被关闭
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
            objectRecognitionHelper.close(); // 释放助手类资源
        }
    }
}
