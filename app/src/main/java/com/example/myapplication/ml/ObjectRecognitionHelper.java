package com.example.myapplication.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObjectRecognitionHelper {

    private static final String TAG = "ObjectRecognitionHelper";
    private static final String MODEL_PATH = "yolov8n.tflite";
    private static final String LABELS_PATH = "labels.txt";

    private Interpreter tflite;
    private List<String> labels;
    private ExecutorService backgroundExecutor;

    private final int inputWidth;
    private final int inputHeight;

    public interface RecognitionCallback {
        void onResult(String recognizedWord, float confidence, RectF boundingBox);
        void onError(String errorMessage);
    }

    public ObjectRecognitionHelper(Context context, int inputWidth, int inputHeight) {
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        backgroundExecutor = Executors.newSingleThreadExecutor();
        loadLabels(context);
        loadModel(context);
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

    private void loadModel(Context context) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(context.getAssets(), MODEL_PATH);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(modelBuffer, options);
            Log.d(TAG, "TFLite model loaded successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model.", e);
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void detectObjects(final Bitmap bitmap, final RecognitionCallback callback) {
        if (tflite == null) {
            callback.onError("TFLite model not initialized.");
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                ImageProcessor imageProcessor = new ImageProcessor.Builder()
                        .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp(0f, 255f))
                        .build();

                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                tensorImage.load(bitmap);
                TensorBuffer processedImageBuffer = imageProcessor.process(tensorImage).getTensorBuffer();
                ByteBuffer byteBuffer = processedImageBuffer.getBuffer();

                int outputTensorSize = labels.size() + 4;
                int numPredictions = 8400;
                float[][][] output = new float[1][outputTensorSize][numPredictions];

                tflite.run(byteBuffer, output);

                postProcess(output, callback, bitmap.getWidth(), bitmap.getHeight());

            } catch (Exception e) {
                Log.e(TAG, "Error during object detection", e);
                callback.onError("Error during object detection: " + e.getMessage());
            }
        });
    }

    // *** MODIFIED HERE: Optimized post-processing logic ***
    private void postProcess(float[][][] output, RecognitionCallback callback, int originalWidth, int originalHeight) {
        float topConfidence = -1.0f;
        String topLabel = "No object detected.";
        RectF topBoundingBox = new RectF();

        int numPredictions = output[0][0].length; // Should be 8400
        int numClasses = labels.size();

        // Iterate through all predictions without transposing
        for (int i = 0; i < numPredictions; i++) {
            // Find the class with the highest score in the current prediction
            float maxScore = 0;
            int maxScoreIndex = -1;
            for (int j = 0; j < numClasses; j++) {
                float score = output[0][j + 4][i];
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreIndex = j;
                }
            }

            // Check if this prediction's max score is the best we've seen so far and above threshold
            if (maxScore > 0.5f && maxScore > topConfidence) {
                topConfidence = maxScore;
                topLabel = labels.get(maxScoreIndex);

                // Get bounding box coordinates for this prediction
                float x_center = output[0][0][i];
                float y_center = output[0][1][i];
                float w = output[0][2][i];
                float h = output[0][3][i];

                // Convert from center_x, center_y, width, height to left, top, right, bottom
                float xmin = x_center - w / 2;
                float ymin = y_center - h / 2;
                float xmax = x_center + w / 2;
                float ymax = y_center + h / 2;
                
                // Scale bounding box to original image size
                float scaleX = (float) originalWidth / inputWidth;
                float scaleY = (float) originalHeight / inputHeight;
                topBoundingBox.set(xmin * scaleX, ymin * scaleY, xmax * scaleX, ymax * scaleY);
            }
        }

        // After checking all predictions, send the single best result via callback
        if (topConfidence > 0.5f) {
            callback.onResult(topLabel, topConfidence, topBoundingBox);
        } else {
            callback.onResult("No object detected.", 0.0f, null);
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
        Log.d(TAG, "ObjectRecognitionHelper resources closed.");
    }
}
