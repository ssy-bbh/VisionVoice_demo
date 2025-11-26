package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";

    private ImageView imageView;
    private TextView resultTextView;
    private ImageClassifier imageClassifier;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.result_text);
        Button cameraButton = findViewById(R.id.camera_button);

        loadLabels();
        initImageClassifier();

        cameraButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
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
            Toast.makeText(this, "Labels file failed to load.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initImageClassifier() {
        try {
            BaseOptions baseOptions = BaseOptions.builder().setNumThreads(4).build();
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(1) // We only want the top 1 result
                    .build();

            imageClassifier = ImageClassifier.createFromFileAndOptions(this, "efficientnet-lite2-int8.tflite", options);

        } catch (IOException e) {
            Log.e(TAG, "TFLite failed to load", e);
            Toast.makeText(this, "Image classifier failed to load.", Toast.LENGTH_SHORT).show();
        }
    }

    private void classifyImage(Bitmap bitmap) {
        if (imageClassifier == null || bitmap == null) {
            Toast.makeText(this, "Uninitialized image classifier or invalid bitmap.", Toast.LENGTH_SHORT).show();
            return;
        }

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        List<Classifications> results = imageClassifier.classify(tensorImage);

        if (results != null && !results.isEmpty() && results.get(0).getCategories() != null && !results.get(0).getCategories().isEmpty()) {
            Category topCategory = results.get(0).getCategories().get(0);
            String numericLabel = topCategory.getLabel(); // This is the number, e.g., "598"
            float score = topCategory.getScore();
            String finalLabel = numericLabel; // Default to the number

            try {
                int index = Integer.parseInt(numericLabel);
                if (labels != null && index >= 0 && index < labels.size()) {
                    finalLabel = labels.get(index); // Translate index to text label
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse label to an integer: " + numericLabel, e);
            }

            String resultStr = String.format("Object: %s\nConfidence: %.2f%%", finalLabel, score * 100);
            resultTextView.setText(resultStr);
        } else {
            resultTextView.setText("Could not classify image.");
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap originalPhoto = (Bitmap) data.getExtras().get("data");

                if (originalPhoto != null) {
                    // Crop the image to the center square to improve accuracy
                    int width = originalPhoto.getWidth();
                    int height = originalPhoto.getHeight();
                    int size = Math.min(width, height); // Get the smaller dimension for a square crop
                    int x = (width - size) / 2;
                    int y = (height - size) / 2;

                    Bitmap croppedPhoto = Bitmap.createBitmap(originalPhoto, x, y, size, size);

                    // Display the cropped photo and classify it
                    imageView.setImageBitmap(croppedPhoto);
                    classifyImage(croppedPhoto);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
