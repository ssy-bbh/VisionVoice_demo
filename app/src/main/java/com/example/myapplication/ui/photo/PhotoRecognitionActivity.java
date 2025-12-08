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
import com.example.myapplication.ml.ObjectRecognitionHelper;
import com.example.myapplication.ui.practice.PracticeActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.IOException;

public class PhotoRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "PhotoRecognitionActivity";

    private ImageView ivSelectedImage;
    private TextView tvPhotoRecognitionResult;
    private ExtendedFloatingActionButton btnConfirmPhotoRecognition;

    private ObjectRecognitionHelper objectRecognitionHelper; // 新增：识别助手类

    private String currentRecognitionResult = null;

    // ActivityResultLauncher for picking images from gallery
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_recognition);

        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        tvPhotoRecognitionResult = findViewById(R.id.tvPhotoRecognitionResult);
        btnConfirmPhotoRecognition = findViewById(R.id.btnConfirmPhotoRecognition);
        ImageButton btnClosePhoto = findViewById(R.id.btnClosePhoto);

        // 初始化识别助手类
        objectRecognitionHelper = new ObjectRecognitionHelper(this);

        // Initialize ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            ivSelectedImage.setImageBitmap(bitmap);
                            recognizeImage(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading image from gallery", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            tvPhotoRecognitionResult.setText("Failed to load image.");
                            btnConfirmPhotoRecognition.setVisibility(View.GONE);
                        }
                    } else {
                        // User cancelled picking image, finish this activity or show a message
                        Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );

        // Start image picker immediately when activity is created
        openImagePicker();

        btnConfirmPhotoRecognition.setOnClickListener(v -> {
            if (currentRecognitionResult != null && !currentRecognitionResult.isEmpty()) {
                Intent intent = new Intent(PhotoRecognitionActivity.this, PracticeActivity.class);
                intent.putExtra("extra_word", currentRecognitionResult);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No valid recognition result to learn.", Toast.LENGTH_SHORT).show();
            }
        });

        btnClosePhoto.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(galleryIntent);
    }

    private void recognizeImage(Bitmap bitmap) {
        // 使用 ObjectRecognitionHelper 进行图像分类
        objectRecognitionHelper.classifyImage(bitmap, new ObjectRecognitionHelper.RecognitionCallback() {
            @Override
            public void onResult(String recognizedWord, float confidence) {
                currentRecognitionResult = recognizedWord; // Store the raw label for practice activity
                String displayResult = String.format("%s (%.0f%%)", recognizedWord, confidence * 100);
                runOnUiThread(() -> {
                    tvPhotoRecognitionResult.setText(displayResult);
                    btnConfirmPhotoRecognition.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Recognition error: " + errorMessage);
                runOnUiThread(() -> {
                    tvPhotoRecognitionResult.setText("Error: " + errorMessage);
                    btnConfirmPhotoRecognition.setVisibility(View.GONE);
                });
                currentRecognitionResult = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (objectRecognitionHelper != null) {
            objectRecognitionHelper.close(); // 释放助手类资源
        }
    }
}
