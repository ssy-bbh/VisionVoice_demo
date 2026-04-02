package com.example.myapplication.ui.ar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.ml.YoloDetector;
import com.example.myapplication.ui.practice.PracticeActivity;
import com.example.myapplication.view.OverlayView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealtimeActivity extends AppCompatActivity {
    private Bitmap latestBitmap = null;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "VISION_DEBUG";

    private PreviewView previewView;
    private TextView resultTextView;
    private OverlayView overlayView;
    private ExecutorService cameraExecutor;

    private YoloDetector yoloDetector;

    // 控制扫描状态：true=暂停扫描，false=正在扫描
    private boolean isResultLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        // 1. 初始化视图
        previewView = findViewById(R.id.viewFinder);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

        resultTextView = findViewById(R.id.result_text_view);
        overlayView = findViewById(R.id.overlayView);

        // 2. 检查 OverlayView 是否正常加载
        if (overlayView == null) {
            Log.wtf(TAG, "!!! 错误：在布局文件中找不到 overlayView !!!");
        } else {
            overlayView.setVisibility(View.VISIBLE);
            overlayView.bringToFront(); // 确保它在最上层
        }

        ImageButton btnClose = findViewById(R.id.btnClose);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 3. 初始化 YOLO
        yoloDetector = new YoloDetector(this, "yolov8n.tflite", "labels.txt", 640, 4);

        // 4. 【核心交互】设置点击绿框的回调
        if (overlayView != null) {
            overlayView.setOnBoxClickListener(result -> {
                isResultLocked = true;
                String detectedWord = result.getLabel();

                Intent intent = new Intent(RealtimeActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", detectedWord);

                if (latestBitmap != null) {
                    // 🚨 核心优化：将耗时的压缩存图操作放到后台线程执行
                    cameraExecutor.execute(() -> {
                        String fileName = "/ar_capture_" + System.currentTimeMillis() + ".jpg";
                        String imagePath = getFilesDir() + fileName;

                        try {
                            java.io.FileOutputStream out = new java.io.FileOutputStream(imagePath);
                            // 耗时操作：图片压缩
                            latestBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            out.flush();
                            out.close();
                            intent.putExtra("extra_image_path", imagePath);
                        } catch (Exception e) {
                            Log.e(TAG, "保存AR截帧失败", e);
                        }

                        // 🚨 存图完成后，切回主线程进行跳转
                        runOnUiThread(() -> startActivity(intent));
                    });
                } else {
                    // 如果因为某种原因没拿到图，直接跳，不卡流程
                    startActivity(intent);
                }
            });
        }

        // 5. 关闭按钮
        btnClose.setOnClickListener(v -> finish());

        // 6. 权限检查与启动相机
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResultLocked = false;
        if (resultTextView != null) {
            resultTextView.setText("Scanning...");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build();

                Preview preview = new Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isResultLocked) {
                        imageProxy.close();
                        return;
                    }
                    try {
                        @SuppressLint("UnsafeOptInUsageError")
                        Bitmap bitmap = imageProxy.toBitmap();
                        int rotation = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotation != 0) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(rotation);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        }

                        latestBitmap = bitmap;

                        List<YoloDetector.Result> results = yoloDetector.detect(bitmap);

                        runOnUiThread(() -> {
                            if (results != null && !results.isEmpty()) {
                                List<YoloDetector.Result> topResults = new ArrayList<>();
                                for (int i = 0; i < Math.min(3, results.size()); i++) {
                                    topResults.add(results.get(i));
                                }

                                if (overlayView != null) {
                                    overlayView.setResults(topResults);
                                }

                                YoloDetector.Result best = topResults.get(0);
                                String labelText = best.getLabel() + String.format(" %.0f%%", best.getScore() * 100);
                                resultTextView.setText(labelText);

                            } else {
                                if (overlayView != null) {
                                    overlayView.setResults(new ArrayList<>());
                                }
                                resultTextView.setText("Scanning...");
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Analysis error", e);
                    } finally {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.wtf(TAG, "!!! 相机绑定失败 !!!", e);
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
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (yoloDetector != null) {
            yoloDetector.close();
        }
    }
}