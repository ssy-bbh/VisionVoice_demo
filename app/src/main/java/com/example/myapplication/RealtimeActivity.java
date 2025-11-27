package com.example.myapplication;

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

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealtimeActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "RealtimeActivity";

    private PreviewView previewView;
    private TextView resultTextView;
    private ExtendedFloatingActionButton confirmButton;
    private FocusBoxView focusBoxView;
    private ImageClassifier imageClassifier;
    private List<String> labels;
    private ExecutorService cameraExecutor;

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

        loadLabels();
        initImageClassifier();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        previewView.setOnClickListener(v -> {
            if (currentDetectionResult == null) return; // Don't lock if nothing is detected

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
                    try {
                        if (imageClassifier == null) {
                            imageProxy.close();
                            return;
                        }

                        @SuppressLint("UnsafeOptInUsageError")
                        Bitmap bitmap = imageProxy.toBitmap();
                        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
                        List<Classifications> results = imageClassifier.classify(tensorImage);

                        if (results != null && !results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
                            Category topCategory = results.get(0).getCategories().get(0);
                            String numericLabel = topCategory.getLabel();
                            float score = topCategory.getScore();
                            String finalLabel = "Unknown";

                            try {
                                int index = Integer.parseInt(numericLabel);
                                if (labels != null && index >= 0 && index < labels.size()) {
                                    finalLabel = labels.get(index);
                                }
                            } catch (NumberFormatException e) {
                                // Keep "Unknown"
                            }
                            
                            currentRawLabel = finalLabel;
                            currentDetectionResult = String.format("%s (%.0f%%)", finalLabel, score * 100);

                            if (!isResultLocked) {
                                runOnUiThread(() -> resultTextView.setText(currentDetectionResult));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during real-time classification", e);
                    } finally {
                        imageProxy.close();
                    }
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

    private void loadLabels() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("labels.txt")))) {
            labels = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load labels.", e);
        }
    }

    private void initImageClassifier() {
        try {
            BaseOptions baseOptions = BaseOptions.builder().setNumThreads(4).build();
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(1)
                    .build();
            imageClassifier = ImageClassifier.createFromFileAndOptions(this, "efficientnet-lite2-int8.tflite", options);
        } catch (IOException e) {
            Log.e(TAG, "TFLite failed to load", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
