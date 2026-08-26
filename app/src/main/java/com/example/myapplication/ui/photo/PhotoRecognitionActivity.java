package com.example.myapplication.ui.photo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ml.YoloDetector;
import com.example.myapplication.ui.practice.PracticeActivity;
import com.example.myapplication.view.OverlayView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.IOException;
import java.util.List;

public class PhotoRecognitionActivity extends AppCompatActivity {
    private String currentImageUri = null;
    private static final String TAG = "VISION_DEBUG";

    private ImageView ivSelectedImage;
    private TextView tvPhotoRecognitionResult;
    private ExtendedFloatingActionButton btnConfirmPhotoRecognition;
    private OverlayView overlayView;

    private YoloDetector yoloDetector;

    private String currentRecognitionResult = null;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_recognition);

        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        tvPhotoRecognitionResult = findViewById(R.id.tvPhotoRecognitionResult);
        btnConfirmPhotoRecognition = findViewById(R.id.btnConfirmPhotoRecognition);
        overlayView = findViewById(R.id.photoOverlayView);
        ImageButton btnClosePhoto = findViewById(R.id.btnClosePhoto);

        if (overlayView != null) {
            overlayView.setVisibility(View.VISIBLE);
            overlayView.bringToFront();
        }

        // Initialize the recognition helper with model input size
        yoloDetector = new YoloDetector(this, "yolov8s_worldv2.tflite", "labels.txt", 416, 4);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        currentImageUri = imageUri != null ? imageUri.toString() : null;
                        try {
                            // 【防OOM闪退】不能用 MediaStore.getBitmap 全尺寸解码，
                            // 用户从相册选一张 4800 万像素的照片会直接撑爆内存。
                            // 这里按目标尺寸降采样解码（检测只需要 640，显示 2000 足够清晰）
                            Bitmap bitmap = decodeSampledBitmap(imageUri, 2000, 2000);
                            if (bitmap == null) {
                                throw new IOException("图片解码失败");
                            }
                            ivSelectedImage.setImageBitmap(bitmap);
                            recognizeImage(bitmap);
                        } catch (IOException e) {
                            Log.wtf(TAG, "!!! 加载图片失败 !!!", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            tvPhotoRecognitionResult.setText("Failed to load image.");
                            btnConfirmPhotoRecognition.setVisibility(View.GONE);
                        }
                    } else {
                        finish();
                    }
                }
        );

        openImagePicker();

        btnConfirmPhotoRecognition.setOnClickListener(v -> {
            if (currentRecognitionResult != null && !currentRecognitionResult.isEmpty()) {
                Intent intent = new Intent(PhotoRecognitionActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", currentRecognitionResult);

                // 核心修改：使用 setData 传递 URI，并赋予读取权限
                if (currentImageUri != null) {
                    intent.setData(android.net.Uri.parse(currentImageUri));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 必须加这行
                }

                startActivity(intent);
            }
        });

        btnClosePhoto.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(galleryIntent);
    }

    private void recognizeImage(Bitmap bitmap) {
        new Thread(() -> {
            try {
                List<YoloDetector.Result> results = yoloDetector.detect(bitmap);
                runOnUiThread(() -> {
                    if (overlayView != null) {
                        overlayView.setResults(results);
                    }
                    if (results.isEmpty()) {
                        currentRecognitionResult = null;
                        tvPhotoRecognitionResult.setText("No object detected.");
                        btnConfirmPhotoRecognition.setVisibility(View.GONE);
                    } else {
                        YoloDetector.Result best = results.get(0);
                        currentRecognitionResult = best.getLabel();
                        String displayResult = String.format("%s (%.0f%%)", best.getLabel(), best.getScore() * 100);
                        tvPhotoRecognitionResult.setText(displayResult);
                        btnConfirmPhotoRecognition.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.wtf(TAG, "!!! 识别过程出错 !!!", e);
                runOnUiThread(() -> {
                    tvPhotoRecognitionResult.setText("Error: " + e.getMessage());
                    btnConfirmPhotoRecognition.setVisibility(View.GONE);
                });
                currentRecognitionResult = null;
            }
        }).start();
    }

    /**
     * 【防OOM】按需降采样解码相册图片，避免超大图全量解码撑爆内存
     */
    private Bitmap decodeSampledBitmap(Uri uri, int reqWidth, int reqHeight) throws IOException {
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();

        // 第一遍：只读尺寸
        options.inJustDecodeBounds = true;
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            android.graphics.BitmapFactory.decodeStream(is, null, options);
        }

        // 计算 2 的幂次采样率
        int inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        // 第二遍：真正解码
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            return android.graphics.BitmapFactory.decodeStream(is, null, options);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (yoloDetector != null) {
            yoloDetector.close();
        }
    }
}
