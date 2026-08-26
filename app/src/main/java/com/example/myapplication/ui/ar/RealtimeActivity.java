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
    // 【防闪退】分析线程替换/回收 Bitmap 与点击回调读取 Bitmap 之间的竞态锁
    private final Object bitmapLock = new Object();
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

        // 3. 初始化 YOLO（YOLO-World v2，228 类开放词表；416 输入 + 动态量化版提速）
        yoloDetector = new YoloDetector(this, "yolov8s_worldv2.tflite", "labels.txt", 416, 4);

        // 4. 【核心交互】设置点击绿框的回调
        if (overlayView != null) {
            overlayView.setOnBoxClickListener(result -> {
                isResultLocked = true;
                String detectedWord = result.getLabel();

                Intent intent = new Intent(RealtimeActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", detectedWord);

                // 🚨 获取你点击的那个框 (0.0 ~ 1.0 的百分比坐标)
                android.graphics.RectF normalizedBox = result.getRect();

                // 【防闪退】在锁内取快照，避免拿到正在被分析线程回收的 Bitmap
                Bitmap snapshot;
                synchronized (bitmapLock) {
                    snapshot = latestBitmap;
                }

                if (snapshot != null && !snapshot.isRecycled() && normalizedBox != null) {
                    try {
                        float bmpWidth = snapshot.getWidth();
                        float bmpHeight = snapshot.getHeight();

                        android.graphics.RectF mappedBox = new android.graphics.RectF(
                                normalizedBox.left * bmpWidth,
                                normalizedBox.top * bmpHeight,
                                normalizedBox.right * bmpWidth,
                                normalizedBox.bottom * bmpHeight
                        );

                        // 1. 🌟 提取基底画幅 (这是前景和背景共同的物理尺寸！)
                        Bitmap baseCropBmp = com.example.myapplication.utils.ImageEnhancer.smartFocusCrop(snapshot, mappedBox, 2.0f);

                        // 2. 🌟 在基底上渲染全息羽化前景
                        Bitmap holographicForeground = com.example.myapplication.utils.ImageEnhancer.createHolographicEdgeBlur(baseCropBmp);

                        // 3. 将【全息前景】和【原始基底】一起传过去。因为它们尺寸完全一致，绝对重合！
                        saveDualLayersAndJump(holographicForeground, baseCropBmp, intent);
                    } catch (Exception e) {
                        // Bitmap 被并发回收等极端竞态：降级为无图跳转，绝不闪退
                        Log.e(TAG, "裁剪检测框图像失败，降级为无图跳转", e);
                        startActivity(intent);
                    }
                } else {
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

    // 🌟 新增：将抠好的透明图保存为 PNG 并跳转
    // 🌟 新增：双轨保存引擎 (分离前景与背景，为 3D 视差做准备)
    private void saveDualLayersAndJump(Bitmap foregroundPNG, Bitmap backgroundJPG, Intent intent) {
        // 【防闪退】用户点框后立刻退出页面时，executor 可能已 shutdown，
        // 直接 execute 会抛 RejectedExecutionException 崩溃
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            startActivity(intent);
            return;
        }
        try {
        cameraExecutor.execute(() -> {
            long time = System.currentTimeMillis();
            // 前景路径
            String fgPath = getFilesDir() + "/ar_fg_" + time + ".png";
            // 背景路径
            String bgPath = getFilesDir() + "/ar_bg_" + time + ".jpg";

            try {
                // 1. 保存前景 (全息羽化透明 PNG)
                java.io.FileOutputStream fgOut = new java.io.FileOutputStream(fgPath);
                foregroundPNG.compress(Bitmap.CompressFormat.PNG, 100, fgOut);
                fgOut.flush();
                fgOut.close();

                // 2. 保存背景 (原画质全尺寸 JPG)
                java.io.FileOutputStream bgOut = new java.io.FileOutputStream(bgPath);
                backgroundJPG.compress(Bitmap.CompressFormat.JPEG, 80, bgOut);
                bgOut.flush();
                bgOut.close();

                // 把两条轨迹都塞进 Intent
                intent.putExtra("extra_image_path", fgPath); // 前景
                intent.putExtra("extra_bg_path", bgPath);    // 背景
            } catch (Exception e) {
                Log.e(TAG, "保存图像失败", e);
            }

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    startActivity(intent);
                }
            });
        });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // executor 刚被关闭的极端竞态：直接跳转，保存的图丢弃
            Log.e(TAG, "保存任务被拒绝（页面正在退出）", e);
            startActivity(intent);
        }
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // 【防闪退】页面已销毁时不再绑定相机，避免对 DESTROYED 生命周期绑定抛异常
                if (isFinishing() || isDestroyed()) return;

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
                            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            // 【防OOM】旋转产生了新对象，原始帧立刻回收
                            if (rotated != bitmap) bitmap.recycle();
                            bitmap = rotated;
                        }

                        // 【防OOM】换帧时回收上一帧；锁保证点击回调不会拿到已回收对象
                        synchronized (bitmapLock) {
                            Bitmap old = latestBitmap;
                            latestBitmap = bitmap;
                            if (old != null && old != bitmap && !old.isRecycled()) {
                                old.recycle();
                            }
                        }

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
            // 【防闪退】必须等分析任务跑完再释放模型，
            // 否则 detect() 正在 native 层执行时 close() Interpreter → SIGSEGV
            try {
                if (!cameraExecutor.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    cameraExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cameraExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (yoloDetector != null) {
            yoloDetector.close();
        }
        // 回收最后一帧
        synchronized (bitmapLock) {
            if (latestBitmap != null && !latestBitmap.isRecycled()) {
                latestBitmap.recycle();
            }
            latestBitmap = null;
        }
    }
}