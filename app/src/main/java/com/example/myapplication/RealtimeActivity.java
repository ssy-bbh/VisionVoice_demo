package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.classifier.Classifications;

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
    private ImageClassifier imageClassifier;
    private List<String> labels;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        previewView = findViewById(R.id.preview_view);
        resultTextView = findViewById(R.id.realtime_result_text);

        cameraExecutor = Executors.newSingleThreadExecutor();

        loadLabels();
        initImageClassifier();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
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
                            return;
                        }

                        // *** THE CORRECT AND FINAL FIX IS HERE ***
                        // 1. Convert the ImageProxy to a Bitmap using the official method.
                        @SuppressLint("UnsafeOptInUsageError")
                        Bitmap bitmap = imageProxy.toBitmap();

                        // 2. Convert the bitmap to a TensorImage.
                        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);

                        // 3. Classify the TensorImage - this method is guaranteed to exist.
                        List<Classifications> results = imageClassifier.classify(tensorImage);

                        if (results != null && !results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
                            Category topCategory = results.get(0).getCategories().get(0);
                            String numericLabel = topCategory.getLabel();
                            float score = topCategory.getScore();
                            String finalLabel = numericLabel;

                            try {
                                int index = Integer.parseInt(numericLabel);
                                if (labels != null && index >= 0 && index < labels.size()) {
                                    finalLabel = labels.get(index);
                                }
                            } catch (NumberFormatException e) {
                               // Continue with the numeric label if parsing fails
                            }

                            String resultStr = String.format("Object: %s\nConfidence: %.2f%%", finalLabel, score * 100);
                            runOnUiThread(() -> resultTextView.setText(resultStr));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during real-time classification", e);
                    } finally {
                        // IMPORTANT: Always close the ImageProxy to allow the next frame to be processed.
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
