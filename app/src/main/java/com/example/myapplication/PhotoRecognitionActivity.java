package com.example.myapplication;

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

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

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

public class PhotoRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "PhotoRecognitionActivity";

    private ImageView ivSelectedImage;
    private TextView tvPhotoRecognitionResult;
    private ExtendedFloatingActionButton btnConfirmPhotoRecognition;

    private ImageClassifier imageClassifier;
    private List<String> labels;
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

        loadLabels();
        initImageClassifier();

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
        if (imageClassifier == null) {
            tvPhotoRecognitionResult.setText("Classifier not initialized.");
            btnConfirmPhotoRecognition.setVisibility(View.GONE);
            return;
        }

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

            currentRecognitionResult = finalLabel; // Store the raw label for practice activity
            String displayResult = String.format("%s (%.0f%%)", finalLabel, score * 100);
            tvPhotoRecognitionResult.setText(displayResult);
            btnConfirmPhotoRecognition.setVisibility(View.VISIBLE);
        } else {
            tvPhotoRecognitionResult.setText("No object detected.");
            btnConfirmPhotoRecognition.setVisibility(View.GONE);
            currentRecognitionResult = null;
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
            Toast.makeText(this, "Failed to load labels for recognition.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initImageClassifier() {
        try {
            BaseOptions baseOptions = BaseOptions.builder().setNumThreads(2).build(); // Use fewer threads for static image
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(1)
                    .build();
            imageClassifier = ImageClassifier.createFromFileAndOptions(this, "efficientnet-lite2-int8.tflite", options);
        } catch (IOException e) {
            Log.e(TAG, "TFLite failed to load", e);
            Toast.makeText(this, "Failed to initialize image classifier.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageClassifier != null) {
            imageClassifier.close();
        }
    }
}