package com.example.myapplication.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

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

public class ObjectRecognitionHelper {

    private static final String TAG = "ObjectRecognitionHelper";
    private static final String MODEL_PATH = "efficientnet-lite2-int8.tflite";
    private static final String LABELS_PATH = "labels.txt";

    private ImageClassifier imageClassifier;
    private List<String> labels;
    private ExecutorService backgroundExecutor;

    // 回调接口，用于将识别结果传递给调用者
    public interface RecognitionCallback {
        void onResult(String recognizedWord, float confidence);
        void onError(String errorMessage);
    }

    public ObjectRecognitionHelper(Context context) {
        backgroundExecutor = Executors.newSingleThreadExecutor();
        loadLabels(context);
        initImageClassifier(context);
    }

    private void loadLabels(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(LABELS_PATH)))) {
            labels = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            Log.d(TAG, "Labels loaded successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load labels from " + LABELS_PATH, e);
        }
    }

    private void initImageClassifier(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder().setNumThreads(2).build(); // 默认使用2个线程
            ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(1)
                    .build();
            imageClassifier = ImageClassifier.createFromFileAndOptions(context, MODEL_PATH, options);
            Log.d(TAG, "ImageClassifier initialized successfully.");
        } catch (IOException e) {
            Log.e(TAG, "TFLite failed to load model from " + MODEL_PATH, e);
        }
    }

    /**
     * 对给定的Bitmap执行图像识别。
     * 结果将通过RecognitionCallback异步返回。
     *
     * @param bitmap 待识别的图片Bitmap。
     * @param callback 接收识别结果的回调。
     */
    public void classifyImage(final Bitmap bitmap, final RecognitionCallback callback) {
        if (imageClassifier == null) {
            callback.onError("Image classifier not initialized.");
            return;
        }
        if (labels == null || labels.isEmpty()) {
            callback.onError("Labels not loaded.");
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
                List<Classifications> results = imageClassifier.classify(tensorImage);

                if (results != null && !results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
                    Category topCategory = results.get(0).getCategories().get(0);
                    String numericLabel = topCategory.getLabel();
                    float score = topCategory.getScore();
                    String finalLabel = "Unknown";

                    try {
                        int index = Integer.parseInt(numericLabel);
                        if (index >= 0 && index < labels.size()) {
                            finalLabel = labels.get(index);
                        }
                    } catch (NumberFormatException e) {
                        // 如果标签不是数字，保留"Unknown"
                    }
                    callback.onResult(finalLabel, score);
                } else {
                    callback.onResult("No object detected.", 0.0f);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during image classification", e);
                callback.onError("Error during image classification: " + e.getMessage());
            }
        });
    }

    /**
     * 释放ImageClassifier资源。
     * 应该在不再需要时（例如在Activity的onDestroy中）调用。
     */
    public void close() {
        if (imageClassifier != null) {
            imageClassifier.close();
            imageClassifier = null;
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
        Log.d(TAG, "ObjectRecognitionHelper resources closed.");
    }
}
