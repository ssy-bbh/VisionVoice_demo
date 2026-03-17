package com.example.myapplication.ml;

import android.content.Context;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wav2Vec2 端侧发音评分器
 * 
 * 使用 ONNX Runtime Mobile 在 Android 设备上运行 Wav2Vec2 模型
 * 实现离线发音评估，无需联网
 * 
 * @author VisionVoice Team
 * @version 1.0
 */
public class Wav2Vec2Scorer {
    private static final String TAG = "Wav2Vec2Scorer";
    private static final int SAMPLE_RATE = 16000;
    
    private final OrtEnvironment env;
    private final OrtSession session;
    private final Map<String, Integer> phonemeToId;
    private final Map<Integer, String> idToPhoneme;
    
    /**
     * 构造函数 - 加载 ONNX 模型
     * 
     * @param context Android 上下文
     * @throws RuntimeException 如果模型加载失败
     */
    public Wav2Vec2Scorer(Context context) {
        try {
            env = OrtEnvironment.getEnvironment();

            String modelAssetPath = "onnx/model_quant.onnx";
            if (!fileExistsInAssets(context, modelAssetPath)) {
                modelAssetPath = "onnx/model.onnx";
            }

            Log.i(TAG, "📦 加载模型：" + modelAssetPath);

            // 复制到内部存储，用文件路径加载（避免 OOM）
            java.io.File modelFile = new java.io.File(context.getFilesDir(), "wav2vec2_model.onnx");
            if (!modelFile.exists()) {
                Log.i(TAG, "首次运行，复制模型到内部存储...");
                copyAssetToFile(context, modelAssetPath, modelFile);
            }

            session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());

            Log.i(TAG, "✅ Wav2Vec2 模型加载成功");
            Log.i(TAG, "输入节点：" + session.getInputNames());
            Log.i(TAG, "输出节点：" + session.getOutputNames());

            phonemeToId = new HashMap<>();
            idToPhoneme = new HashMap<>();
            loadPhonemeDictionary();

        } catch (Exception e) {
            Log.e(TAG, "❌ 模型加载失败", e);
            throw new RuntimeException("Wav2Vec2 模型初始化失败：" + e.getMessage(), e);
        }
    }

    private void copyAssetToFile(Context context, String assetPath, java.io.File destFile) throws IOException {
        InputStream is = context.getAssets().open(assetPath);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile);
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            fos.write(chunk, 0, bytesRead);
        }
        fos.close();
        is.close();
    }
    
    private boolean fileExistsInAssets(Context context, String path) {
        try {
            context.getAssets().open(path).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 读取 assets 文件为 byte[]
     * 使用 ByteArrayOutputStream 循环读取，避免压缩文件 available() 不准确的问题
     */
    private byte[] readAssetToBytes(Context context, String path) throws IOException {
        InputStream is = context.getAssets().open(path);
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        is.close();
        return buffer.toByteArray();
    }
    
    /**
     * 加载音素词典
     * 实际应该从模型配置文件加载，这里先硬编码常见英语音素
     */
    private void loadPhonemeDictionary() {
        // IPA 音素表（英语）
        String[] vowels = {
            "ɪ", "i", "e", "ɛ", "æ", "ɑ", "ɔ", "ʊ", "u", "ʌ", "ə",
            "eɪ", "aɪ", "ɔɪ", "aʊ", "oʊ", "ɪr", "ɛr", "ɔr", "ʊr", "ɑr"
        };
        
        String[] consonants = {
            "p", "b", "t", "d", "k", "g", "f", "v", "θ", "ð",
            "s", "z", "ʃ", "ʒ", "h", "m", "n", "ŋ", "l", "ɹ", "w", "j",
            "tʃ", "dʒ", "tr", "dr", "ts", "dz"
        };
        
        int id = 1; // 0 保留给 blank
        for (String p : vowels) {
            phonemeToId.put(p, id);
            idToPhoneme.put(id, p);
            id++;
        }
        for (String p : consonants) {
            phonemeToId.put(p, id);
            idToPhoneme.put(id, p);
            id++;
        }
        
        Log.i(TAG, "✅ 加载音素词典：" + phonemeToId.size() + " 个音素");
    }
    
    /**
     * 转录音频为音素序列
     * 
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
            float[][][] logits = (float[][][]) result.get(0).getValue();
            
            // 4. CTC 解码（贪婪搜索）
            List<Integer> predictedIds = ctcGreedyDecode(logits[0]);
            
            // 5. ID 转音素
            List<String> phonemes = new ArrayList<>();
            for (int id : predictedIds) {
                String phoneme = idToPhoneme.getOrDefault(id, "?");
                phonemes.add(phoneme);
            }
            
            // 清理资源
            inputTensor.close();
            result.close();
            
            Log.d(TAG, "✅ 转录完成：" + phonemes.size() + " 个音素");
            return phonemes;
            
        } catch (OrtException e) {
            Log.e(TAG, "❌ 推理失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * CTC 贪婪搜索解码
     * 
     * @param logits 模型输出的 logits (time_steps x vocab_size)
     * @return 解码后的音素 ID 序列
     */
    private List<Integer> ctcGreedyDecode(float[][] logits) {
        List<Integer> result = new ArrayList<>();
        int prevId = -1;
        
        for (float[] frame : logits) {
            int maxId = argmax(frame);
            
            // 跳过 blank (id=0) 和重复
            if (maxId != prevId && maxId != 0) {
                result.add(maxId);
            }
            
            prevId = maxId;
        }
        
        return result;
    }
    
    /**
     * 求数组最大值的索引
     */
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
     * 计算发音评分
     * 
     * @param targetWord 目标单词
     * @param audioData 音频数据
     * @return 评分结果
     */
    public PronunciationScore score(String targetWord, float[] audioData) {
        long startTime = System.currentTimeMillis();
        
        // 1. 转录用户发音
        List<String> userPhonemes = transcribe(audioData);
        
        // 2. 获取标准音素（简化实现，应该调用 phonemizer）
        List<String> refPhonemes = getReferencePhonemes(targetWord);
        
        // 3. 对齐和评分
        AlignmentResult alignment = alignPhonemes(refPhonemes, userPhonemes);
        
        // 4. 计算分数
        int score = calculateScore(alignment);
        
        long endTime = System.currentTimeMillis();
        Log.i(TAG, "⏱️ 评分耗时：" + (endTime - startTime) + "ms");
        Log.i(TAG, "📊 得分：" + score);
        
        return new PronunciationScore(
            score,
            refPhonemes,
            userPhonemes,
            alignment.feedback
        );
    }
    
    /**
     * 获取单词的标准音素（简化版）
     * TODO: 集成 phonemizer 或 CMU Dict
     */
    private List<String> getReferencePhonemes(String word) {
        // 简化实现：返回占位符
        // 实际应该使用 CMU Pronouncing Dictionary 或 phonemizer
        List<String> phonemes = new ArrayList<>();
        
        // 临时硬编码一些常见单词
        Map<String, String[]> commonWords = new HashMap<>();
        commonWords.put("apple", new String[]{"æ", "p", "ə", "l"});
        commonWords.put("book", new String[]{"b", "ʊ", "k"});
        commonWords.put("cat", new String[]{"k", "æ", "t"});
        commonWords.put("dog", new String[]{"d", "ɔ", "g"});
        
        String[] ref = commonWords.get(word.toLowerCase());
        if (ref != null) {
            for (String p : ref) {
                phonemes.add(p);
            }
        } else {
            // 未知单词：每个字母当作一个音素（临时方案）
            for (char c : word.toCharArray()) {
                phonemes.add(String.valueOf(c));
            }
        }
        
        return phonemes;
    }
    
    /**
     * 音素对齐（Needleman-Wunsch 算法简化版）
     */
    private AlignmentResult alignPhonemes(List<String> ref, List<String> user) {
        List<String> feedback = new ArrayList<>();
        
        int n = ref.size();
        int m = user.size();
        
        // 简化对齐：直接比较
        int maxLen = Math.max(n, m);
        for (int i = 0; i < maxLen; i++) {
            String r = i < n ? ref.get(i) : "-";
            String u = i < m ? user.get(i) : "-";
            
            if (r.equals(u)) {
                feedback.add("Match");
            } else if (r.equals("-")) {
                feedback.add("Insertion (多读)");
            } else if (u.equals("-")) {
                feedback.add("Deletion (漏读)");
            } else {
                feedback.add("Substitution (错读)");
            }
        }
        
        return new AlignmentResult(ref, user, feedback);
    }
    
    /**
     * 计算最终得分
     */
    private int calculateScore(AlignmentResult alignment) {
        int matchCount = 0;
        int totalCount = alignment.reference.size();
        
        for (String fb : alignment.feedback) {
            if (fb.equals("Match")) {
                matchCount++;
            }
        }
        
        if (totalCount == 0) return 0;
        
        float accuracy = (float)matchCount / totalCount;
        
        // 非线性打分曲线
        int displayScore;
        if (accuracy >= 0.8f) {
            displayScore = (int)(90 + (accuracy - 0.8f) * 50);
        } else if (accuracy >= 0.5f) {
            displayScore = (int)(60 + (accuracy - 0.5f) * 100);
        } else {
            displayScore = (int)(accuracy * 120);
        }
        
        return Math.max(0, Math.min(100, displayScore));
    }
    
    /**
     * 释放资源
     */
    public void close() {
        try {
            session.close();
            env.close();
        } catch (OrtException e) {
            Log.e(TAG, "❌ 关闭失败", e);
        }
    }
    
    // ==================== 结果类 ====================
    
    public static class PronunciationScore {
        public int score;
        public List<String> referencePhonemes;
        public List<String> userPhonemes;
        public List<String> feedback;
        
        public PronunciationScore(int score, List<String> ref, List<String> user, List<String> feedback) {
            this.score = score;
            this.referencePhonemes = ref;
            this.userPhonemes = user;
            this.feedback = feedback;
        }
    }
    
    public static class AlignmentResult {
        public List<String> reference;
        public List<String> user;
        public List<String> feedback;
        
        public AlignmentResult(List<String> ref, List<String> user, List<String> feedback) {
            this.reference = ref;
            this.user = user;
            this.feedback = feedback;
        }
    }
}
