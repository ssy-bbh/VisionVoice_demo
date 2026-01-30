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

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "VISION_DEBUG";

    private PreviewView previewView;
    private TextView resultTextView;
    // private ExtendedFloatingActionButton confirmButton; // 已移除：点击框直接跳转，不再需要确认按钮
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
        // 强制使用 TextureView 模式，避免 SurfaceView 遮挡 OverlayView
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

        resultTextView = findViewById(R.id.result_text_view);
        // confirmButton = findViewById(R.id.btn_confirm); // 已移除
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
        // 请确保 assets 目录下有 yolov8n.tflite 和 labels.txt
        yoloDetector = new YoloDetector(this, "yolov8n.tflite", "labels.txt", 640, 4);

        // 4. 【核心交互】设置点击绿框的回调
        // 当用户点击屏幕上的绿框时，会执行这里的代码
        if (overlayView != null) {
            overlayView.setOnBoxClickListener(result -> {
                // (1) 锁定结果，防止跳转过程中数据刷新
                isResultLocked = true;

                // (2) 获取单词和置信度
                String detectedWord = result.getLabel();
                float score = result.getScore();

                Log.d(TAG, "点击了单词: " + detectedWord);

                // (3) 跳转到 PracticeActivity (或者 ResultActivity)
                Intent intent = new Intent(RealtimeActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", detectedWord);
                // intent.putExtra("extra_score", score); // 如果需要传分数可以解开这行
                startActivity(intent);
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

    /**
     * 页面恢复时，重新开始扫描
     */
    @Override
    protected void onResume() {
        super.onResume();
        isResultLocked = false; // 解锁，继续识别
        if (resultTextView != null) {
            resultTextView.setText("Scanning...");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // =========================================================
                // 【核心配置】使用 ResolutionSelector 请求 16:9 画面
                // =========================================================
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build();

                // 1. 预览配置
                Preview preview = new Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. 分析器配置
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // 如果被锁定（正在跳转中），直接忽略这一帧
                    if (isResultLocked) {
                        imageProxy.close();
                        return;
                    }

                    try {
                        @SuppressLint("UnsafeOptInUsageError")
                        Bitmap bitmap = imageProxy.toBitmap();
                        int rotation = imageProxy.getImageInfo().getRotationDegrees();

                        // 【旋转修正】
                        if (rotation != 0) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(rotation);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        }

                        // 【YOLO 识别】
                        List<YoloDetector.Result> results = yoloDetector.detect(bitmap);

                        // 【UI 更新】
                        runOnUiThread(() -> {
                            if (results != null && !results.isEmpty()) {
                                // 只取第一名 (Top 1)
                                YoloDetector.Result best = results.get(0);

                                // 构造单元素列表传给 OverlayView
                                List<YoloDetector.Result> topOneList = new ArrayList<>();
                                topOneList.add(best);

                                if (overlayView != null) {
                                    overlayView.setResults(topOneList);
                                }

                                // 更新底部文字
                                String labelText = best.getLabel() + String.format(" %.0f%%", best.getScore() * 100);
                                resultTextView.setText(labelText);

                            } else {
                                // 没有识别到物体
                                if (overlayView != null) {
                                    overlayView.setResults(new ArrayList<>());
                                }
                                resultTextView.setText("Scanning...");
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Analysis error", e);
                    } finally {
                        // 必须关闭，否则相机卡死
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