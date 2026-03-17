package com.example.myapplication.ui.test;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * ONNX 模型测试 Activity
 * 测试 360MB Wav2Vec2 模型在 Android 上的加载和推理性能
 * 
 * 创建时间：2026-03-17
 * 目的：验证大模型在移动端的可行性
 */
public class OnnxTestActivity extends AppCompatActivity {
    private static final String TAG = "OnnxTest";
    private static final String MODEL_PATH = "onnx/model.onnx";
    
    private TextView tvStatus;
    private TextView tvResult;
    private Button btnTest;
    
    private OrtEnvironment env;
    private OrtSession session;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onnx_test);
        
        tvStatus = findViewById(R.id.tvStatus);
        tvResult = findViewById(R.id.tvResult);
        btnTest = findViewById(R.id.btnTest);
        
        btnTest.setOnClickListener(v -> runTest());
        
        tvStatus.setText("点击按钮开始测试");
    }
    
    private void runTest() {
        btnTest.setEnabled(false);
        tvResult.setText("");

        Runtime runtime = Runtime.getRuntime();
        long freeMemMB = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / 1024 / 1024;
        long maxMemMB = runtime.maxMemory() / 1024 / 1024;
        tvStatus.setText("可用内存: " + freeMemMB + " MB / " + maxMemMB + " MB\n加载中...");
        Log.i(TAG, "可用内存: " + freeMemMB + " MB / " + maxMemMB + " MB");

        // 不再拦截：文件路径加载走 OS mmap，不占 Java 堆，无需检查堆内存
        new Thread(() -> {
            try {
                // 1. 测试模型加载
                long startTime = SystemClock.elapsedRealtime();
                testModelLoading();
                long loadTime = SystemClock.elapsedRealtime() - startTime;

                // 2. 测试推理
                startTime = SystemClock.elapsedRealtime();
                long[] outputShape = testInference();
                long inferenceTime = SystemClock.elapsedRealtime() - startTime;

                // 3. 读取真实数据
                Runtime rt = Runtime.getRuntime();
                long usedMemMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                long maxMB = rt.maxMemory() / 1024 / 1024;

                // 4. 显示真实结果
                runOnUiThread(() -> {
                    String result = String.format(
                        "✅ 测试通过\n\n" +
                        "模型加载时间: %d ms\n" +
                        "推理时间: %d ms\n\n" +
                        "模型文件大小: 360 MB\n" +
                        "当前内存占用: %d MB / %d MB\n" +
                        "输出形状: [%d, %d, %d]\n\n" +
                        "结论: 模型可以运行",
                        loadTime, inferenceTime,
                        usedMemMB, maxMB,
                        outputShape[0], outputShape[1], outputShape[2]
                    );
                    tvStatus.setText("测试完成");
                    tvResult.setText(result);
                    btnTest.setEnabled(true);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "测试失败", e);
                runOnUiThread(() -> {
                    tvStatus.setText("测试失败");
                    tvResult.setText("❌ 错误: " + e.getMessage());
                    btnTest.setEnabled(true);
                    Toast.makeText(this, "测试失败", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void testModelLoading() throws Exception {
        Log.i(TAG, "📦 加载 ONNX 模型...");

        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();

        // ONNX Runtime Android 只支持 String path 或 byte[]
        // byte[] 方式需要把 360MB 全部读进 Java 堆 → OOM
        // 所以先复制到内部存储，再用文件路径加载
        // 文件路径加载时 ONNX Runtime 内部用 OS mmap，不占 Java 堆
        java.io.File modelFile = new java.io.File(getFilesDir(), "model.onnx");
        if (!modelFile.exists()) {
            runOnUiThread(() -> tvStatus.setText("首次运行：复制模型文件...\n（约需 10-20 秒，仅一次）"));
            Log.i(TAG, "复制模型到: " + modelFile.getAbsolutePath());
            copyAssetToFile(MODEL_PATH, modelFile);
            Log.i(TAG, "复制完成，大小: " + modelFile.length() / 1024 / 1024 + " MB");
        } else {
            Log.i(TAG, "模型已存在，直接加载");
        }

        // 用文件路径加载，OS mmap 按需分页，不占 Java 堆
        session = env.createSession(modelFile.getAbsolutePath(), options);

        Log.i(TAG, "✅ 模型加载成功");
        Log.i(TAG, "输入: " + session.getInputNames());
        Log.i(TAG, "输出: " + session.getOutputNames());
    }

    // 分块复制，固定 8KB 缓冲区，不一次性分配大内存
    private void copyAssetToFile(String assetPath, java.io.File destFile) throws java.io.IOException {
        java.io.InputStream is = getAssets().open(assetPath);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile);
        byte[] chunk = new byte[8192]; // 固定 8KB 块，零额外堆分配
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            fos.write(chunk, 0, bytesRead);
        }
        fos.close();
        is.close();
    }

    
    private byte[] readModelFromAssets() {
        try {
            InputStream is = getAssets().open(MODEL_PATH);
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            is.close();
            byte[] bytes = buffer.toByteArray();
            Log.i(TAG, "📁 模型读取完成: " + (bytes.length / 1024 / 1024) + " MB");
            return bytes;
        } catch (IOException e) {
            Log.e(TAG, "❌ 读取模型失败", e);
            throw new RuntimeException("无法读取模型文件: " + MODEL_PATH);
        }
    }
    
    private long[] testInference() throws OrtException {
        Log.i(TAG, "⚡ 测试推理...");
        
        float[] audioData = new float[16000];
        long[] shape = {1, audioData.length};
        FloatBuffer buffer = FloatBuffer.wrap(audioData);
        
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_values", inputTensor);
        
        OrtSession.Result result = session.run(inputs);
        float[][][] logits = (float[][][]) result.get(0).getValue();
        
        long[] outputShape = {logits.length, logits[0].length, logits[0][0].length};
        Log.i(TAG, "✅ 推理成功，输出形状: " + outputShape[0] + "x" + outputShape[1] + "x" + outputShape[2]);
        
        inputTensor.close();
        result.close();
        
        return outputShape;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            Log.e(TAG, "清理失败", e);
        }
    }
}
