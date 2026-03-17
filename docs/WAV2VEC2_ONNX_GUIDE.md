# Wav2Vec2 端侧部署指南 - VisionVoice 项目

## 📊 当前架构 vs 目标架构

### 当前架构
```
┌─────────────┐      HTTP       ┌──────────────────┐
│   Android   │ ──────────────> │ Python FastAPI   │
│  (录音+UI)  │ <────────────── │ + Wav2Vec2       │
└─────────────┘      JSON       └──────────────────┘
```

### 目标架构（端侧）
```
┌────────────────────────────────────┐
│         Android App                │
│  ┌──────────┐    ┌──────────────┐ │
│  │ 录音模块  │    │ Wav2Vec2     │ │
│  │          │ -> │ ONNX Runtime │ │
│  └──────────┘    └──────────────┘ │
│         ↓              ↓          │
│    UI 渲染 ←──  发音评分逻辑      │
└────────────────────────────────────┘
```

---

## 🚀 实施步骤

### 第一步：转换 Wav2Vec2 为 ONNX 格式

在电脑上创建模型转换脚本：

```bash
# 1. 安装依赖
pip install optimum[onnxruntime] transformers torch torchaudio phonemizer
```

创建 `D:\AndroidStudioProjects\MyApplication\backend\export_onnx.py`:

```python
#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wav2Vec2 ONNX 导出脚本
用于将 HuggingFace 模型转换为 Android 可用的 ONNX 格式
"""

from optimum.onnxruntime import ORTModelForCTC
from transformers import Wav2Vec2FeatureExtractor, Wav2Vec2Processor
import torch
import onnxruntime as ort

# 模型选择（移动端推荐）
# MODEL_NAME = "facebook/wav2vec2-base-960h"  # 95MB, 准确率高
MODEL_NAME = "facebook/wav2vec2-base"  # 95MB
# MODEL_NAME = "jonatasgrosman/wav2vec2-large-xlsr-53-english"  # 335MB, 最高准确率

print(f"🔄 正在导出模型：{MODEL_NAME}")

# 1. 导出为 ONNX
model = ORTModelForCTC.from_pretrained(
    MODEL_NAME,
    export=True,
    provider="CPUExecutionProvider"
)

# 2. 保存模型
save_path = "D:/AndroidStudioProjects/MyApplication/app/src/main/assets/onnx"
model.save_pretrained(save_path)

print(f"✅ 模型已保存到：{save_path}")

# 3. 验证模型
print("🔍 验证模型...")
session = ort.InferenceSession(f"{save_path}/model.onnx")
print(f"✅ 模型输入：{[i.name for i in session.get_inputs()]}")
print(f"✅ 模型输出：{[o.name for o in session.get_outputs()]}")

# 4. 量化（可选，减小模型体积）
from onnxruntime.quantization import quantize_dynamic, QuantType

print("📦 正在进行 INT8 量化...")
quantize_dynamic(
    f"{save_path}/model.onnx",
    f"{save_path}/model_quant.onnx",
    weight_type=QuantType.QUInt8
)
print("✅ 量化完成！")
```

运行脚本：
```bash
cd D:\AndroidStudioProjects\MyApplication\backend
python export_onnx.py
```

---

### 第二步：修改 Android build.gradle.kts

修改 `D:\AndroidStudioProjects\MyApplication\app\build.gradle.kts`:

```kotlin
dependencies {
    // ... 现有依赖保持不变 ...
    
    // TensorFlow Lite (保留，用于 YOLO 物体检测)
    implementation("org.tensorflow:tensorflow-lite:2.10.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    
    // ✅ 新增：ONNX Runtime Mobile (用于 Wav2Vec2)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
    
    // ✅ 新增：音频处理工具
    implementation("com.google.guava:guava:32.1.3-android")
}
```

---

### 第三步：创建端侧发音评分模块

创建 `D:\AndroidStudioProjects\MyApplication\app\src\main\java\com\example\myapplication\ml\Wav2Vec2Scorer.java`:

```java
package com.example.myapplication.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wav2Vec2Scorer {
    private static final String TAG = "Wav2Vec2Scorer";
    private static final int SAMPLE_RATE = 16000;
    
    private final OrtEnvironment env;
    private final OrtSession session;
    private final Map<String, Integer> phonemeToId;
    private final Map<Integer, String> idToPhoneme;
    
    public Wav2Vec2Scorer(Context context) {
        try {
            env = OrtEnvironment.getEnvironment();
            
            // 加载 ONNX 模型（从 assets 目录）
            AssetFileDescriptor modelFd = context.getAssets().openFd("onnx/model_quant.onnx");
            session = env.createSession(modelFd.getFileDescriptor(), new OrtSession.SessionOptions());
            modelFd.close();
            
            Log.i(TAG, "✅ Wav2Vec2 模型加载成功");
            Log.i(TAG, "输入节点：" + session.getInputNames());
            Log.i(TAG, "输出节点：" + session.getOutputNames());
            
            // 加载音素词典（简化版，实际需要从模型配置加载）
            phonemeToId = new HashMap<>();
            idToPhoneme = new HashMap<>();
            loadPhonemeDictionary(context);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 模型加载失败", e);
            throw new RuntimeException("Wav2Vec2 模型初始化失败", e);
        }
    }
    
    private void loadPhonemeDictionary(Context context) {
        // TODO: 从 assets 加载音素映射文件
        // 这里先硬编码一些常见音素
        String[] phonemes = {"ɪ", "ɛ", "æ", "ɑ", "ɔ", "ʊ", "u", "eɪ", "aɪ", "ɔɪ", 
                            "aʊ", "oʊ", "ɪr", "ɛr", "ɔr", "ʊr", "ɑr"};
        for (int i = 0; i < phonemes.length; i++) {
            phonemeToId.put(phonemes[i], i);
            idToPhoneme.put(i, phonemes[i]);
        }
    }
    
    /**
     * 转录音频为音素序列
     * @param audioData PCM 音频数据 (16kHz, float32, 归一化到 [-1, 1])
     * @return 音素序列
     */
    public List<String> transcribe(float[] audioData) {
        try {
            // 1. 创建输入张量
            long[] shape = {1, audioData.length};
            FloatBuffer buffer = FloatBuffer.wrap(audioData);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
            
            // 2. 运行推理
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_values", inputTensor);
            
            OrtSession.Result result = session.run(inputs);
            
            // 3. 获取输出 logits
            float[][] logits = (float[][]) result.get(0).getValue();
            
            // 4. CTC 解码（贪婪搜索）
            List<Integer> predictedIds = new ArrayList<>();
            int prevId = -1;
            for (float[] frame : logits[0]) {
                int maxId = argmax(frame);
                if (maxId != prevId && maxId != 0) { // 跳过 blank (id=0)
                    predictedIds.add(maxId);
                }
                prevId = maxId;
            }
            
            // 5. ID 转音素
            List<String> phonemes = new ArrayList<>();
            for (int id : predictedIds) {
                String phoneme = idToPhoneme.getOrDefault(id, "?");
                phonemes.add(phoneme);
            }
            
            inputTensor.close();
            result.close();
            
            return phonemes;
            
        } catch (OrtException e) {
            Log.e(TAG, "推理失败", e);
            return new ArrayList<>();
        }
    }
    
    private int argmax(float[] array) {
        int maxIdx = 0;
        float maxVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
    
    /**
     * 计算发音评分（简化版）
     */
    public PronunciationScore score(String targetWord, float[] audioData) {
        List<String> userPhonemes = transcribe(audioData);
        
        // TODO: 实现 Needleman-Wunsch 对齐算法
        // 这里先返回简化版评分
        float accuracy = calculateAccuracy(targetWord, userPhonemes);
        
        return new PronunciationScore(
            (int)(accuracy * 100),
            userPhonemes,
            new ArrayList<>() // feedback
        );
    }
    
    private float calculateAccuracy(String targetWord, List<String> userPhonemes) {
        // 简化实现：比较长度和匹配度
        // TODO: 实现完整的音素对齐算法
        return 0.8f; // 临时返回 80 分
    }
    
    public void close() {
        try {
            session.close();
            env.close();
        } catch (OrtException e) {
            Log.e(TAG, "关闭失败", e);
        }
    }
    
    // 评分结果类
    public static class PronunciationScore {
        public int score;
        public List<String> userPhonemes;
        public List<String> feedback;
        
        public PronunciationScore(int score, List<String> userPhonemes, List<String> feedback) {
            this.score = score;
            this.userPhonemes = userPhonemes;
            this.feedback = feedback;
        }
    }
}
```

---

### 第四步：修改 PracticeActivity 使用端侧模型

修改 `PracticeActivity.java` 的关键部分：

```java
public class PracticeActivity extends AppCompatActivity {
    // 新增：端侧评分器
    private Wav2Vec2Scorer scorer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化端侧评分器
        new Thread(() -> {
            scorer = new Wav2Vec2Scorer(this);
        }).start();
        
        // ... 其他初始化代码 ...
    }
    
    /**
     * 修改：使用端侧评分（不再调用后端）
     */
    private void evaluatePronunciationOnDevice(String word, File file) {
        new Thread(() -> {
            try {
                // 1. 读取音频文件
                float[] audioData = loadAudioFile(file);
                
                // 2. 端侧评分
                Wav2Vec2Scorer.PronunciationScore result = scorer.score(word, audioData);
                
                // 3. 更新 UI
                runOnUiThread(() -> {
                    updateUIWithFeedback(result);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "端侧评分失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "评分失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private float[] loadAudioFile(File file) throws IOException {
        // TODO: 实现音频文件加载（使用 MediaExtractor 或 FFmpeg）
        // 返回 16kHz PCM float 数组
        return new float[0];
    }
    
    private void updateUIWithFeedback(Wav2Vec2Scorer.PronunciationScore result) {
        // 复用现有的 UI 更新逻辑
        // ...
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scorer != null) {
            scorer.close();
        }
    }
}
```

---

### 第五步：音频预处理工具类

创建 `AudioProcessor.java`:

```java
package com.example.myapplication.ml;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioProcessor {
    private static final String TAG = "AudioProcessor";
    private static final int TARGET_SAMPLE_RATE = 16000;
    
    /**
     * 加载音频文件并转换为 16kHz PCM float 数组
     */
    public static float[] loadAndPreprocess(File audioFile) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(audioFile.getAbsolutePath());
        
        int trackIndex = selectAudioTrack(extractor);
        if (trackIndex < 0) {
            throw new IOException("未找到音频轨道");
        }
        
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        
        Log.d(TAG, "音频信息：采样率=" + sampleRate + ", 声道数=" + channelCount);
        
        extractor.selectTrack(trackIndex);
        
        // 读取音频数据
        ByteBuffer buffer = ByteBuffer.allocate(audioFile.length() * 2);
        while (true) {
            int sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) break;
            extractor.advance();
        }
        
        extractor.release();
        
        // 转换为 float 数组
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // 重采样和混音
        return resampleAndMix(buffer, sampleRate, channelCount);
    }
    
    private static int selectAudioTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }
    
    private static float[] resampleAndMix(ByteBuffer buffer, int sourceSampleRate, int channelCount) {
        // 简化的重采样实现
        // TODO: 实现完整的重采样算法（或使用 libsamplerate）
        int numSamples = buffer.remaining() / (2 * channelCount);
        float[] samples = new float[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            float sample = 0;
            for (int ch = 0; ch < channelCount; ch++) {
                short s = buffer.getShort();
                sample += s / 32768.0f;
            }
            samples[i] = sample / channelCount;
        }
        
        // 重采样到 16kHz
        if (sourceSampleRate != TARGET_SAMPLE_RATE) {
            samples = resample(samples, sourceSampleRate, TARGET_SAMPLE_RATE);
        }
        
        return samples;
    }
    
    private static float[] resample(float[] samples, int fromRate, int toRate) {
        // 简化的线性插值重采样
        float ratio = (float)fromRate / toRate;
        int newLength = (int)(samples.length / ratio);
        float[] resampled = new float[newLength];
        
        for (int i = 0; i < newLength; i++) {
            float srcIdx = i * ratio;
            int idx = (int)srcIdx;
            float frac = srcIdx - idx;
            
            if (idx + 1 < samples.length) {
                resampled[i] = samples[idx] * (1 - frac) + samples[idx + 1] * frac;
            } else {
                resampled[i] = samples[idx];
            }
        }
        
        return resampled;
    }
}
```

---

## 📋 完整实施清单

- [ ] **1. 模型转换**
  - [ ] 运行 `export_onnx.py` 导出 ONNX 模型
  - [ ] 量化模型（减小体积）
  - [ ] 将模型文件放入 `app/src/main/assets/onnx/`

- [ ] **2. 依赖更新**
  - [ ] 添加 ONNX Runtime Android 依赖
  - [ ] Sync Gradle

- [ ] **3. 代码实现**
  - [ ] 创建 `Wav2Vec2Scorer.java`
  - [ ] 创建 `AudioProcessor.java`
  - [ ] 修改 `PracticeActivity.java` 使用端侧评分

- [ ] **4. 测试**
  - [ ] 模型加载测试
  - [ ] 推理速度测试（目标：<500ms）
  - [ ] 评分准确性对比（vs 后端）

- [ ] **5. 优化**
  - [ ] 内存优化（避免 OOM）
  - [ ] 线程优化（异步推理）
  - [ ] 模型量化（INT8）

---

## ⚡ 性能预期

| 指标 | 预期值 |
|------|--------|
| 模型大小（FP32） | ~95MB |
| 模型大小（INT8） | ~25MB |
| 推理时间（中端手机） | 300-800ms |
| 内存占用 | ~200MB |
| 准确率损失（量化后） | <2% |

---

## 🎯 推荐实施顺序

1. **先做原型验证**（1-2 天）
   - 导出模型 → 集成 ONNX Runtime → 测试推理
   
2. **完善音频处理**（2-3 天）
   - 实现完整的音频加载和预处理
   - 实现音素对齐算法
   
3. **优化和测试**（3-5 天）
   - 性能优化
   - 准确性调优
   - 用户测试

---

## 💡 替代方案

如果觉得 ONNX Runtime 太复杂，可以考虑：

### 方案 A：TensorFlow Lite（更成熟）
```python
# 使用 TFLite 导出
import tensorflow as tf
converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()
```

### 方案 B：MediaPipe Audio（最简单）
- Google 现成方案
- 但 Wav2Vec2 支持有限

### 方案 C：混合架构（过渡方案）
- 端侧：简单关键词识别
- 云端：完整 Wav2Vec2 评分
- 根据网络情况自动切换

---

## 📚 参考资料

1. ONNX Runtime Android: https://onnxruntime.ai/docs/get-started/with-java.html
2. Wav2Vec2 官方文档：https://huggingface.co/facebook/wav2vec2-base
3. 最优模型选择：https://github.com/ONNX/models/tree/main/speech_recognition
