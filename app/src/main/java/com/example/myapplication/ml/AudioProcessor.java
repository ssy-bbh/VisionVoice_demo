package com.example.myapplication.ml;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * 现代 AI 音频预处理工具类
 *
 * 输入：由 AudioRecord 直接生成的无损 16-bit PCM 裸文件 (.pcm)
 * 架构：彻底摒弃 MediaCodec 解码与 AAC 损伤
 * 输出：执行了零均值与单位方差 (Zero Mean & Unit Variance) 归一化的 Float 数组
 */
public class AudioProcessor {
    private static final String TAG = "AudioProcessor";

    /**
     * 直接读取 PCM 裸文件并进行端到端的数学归一化处理
     */
    public static float[] loadAndPreprocess(File audioFile) throws IOException {
        // 1. 直接读取 16-bit PCM 裸字节，无需任何媒体解码器
        FileInputStream fis = new FileInputStream(audioFile);
        byte[] pcmBytes = new byte[(int) audioFile.length()];
        fis.read(pcmBytes);
        fis.close();

        // 2. 将 Byte 转换为 Float (除以 32768.0f 映射到 [-1.0, 1.0])
        float[] floatData = new float[pcmBytes.length / 2];
        ByteBuffer bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sb = bb.asShortBuffer();
        for (int i = 0; i < floatData.length; i++) {
            floatData[i] = sb.get() / 32768.0f;
        }

        Log.d(TAG, "PCM 读取完毕，采样点数=" + floatData.length);

        // 🚨 3. 核心决胜点：执行严格的零均值与单位方差数学归一化
        return normalize(floatData);
    }

    /**
     * 严谨的数学归一化算法 (替代粗糙的峰值归一化)
     * 依据研究报告公式: X_norm = (X - μ) / (σ + ε)
     * 目的：让 Wav2Vec2 只看波形形状，彻底无视绝对响度干扰
     */
    private static float[] normalize(float[] audioData) {
        if (audioData == null || audioData.length == 0) return audioData;

        // 1. 计算均值 (μ)
        double sum = 0;
        for (float val : audioData) {
            sum += val;
        }
        double mean = sum / audioData.length;

        // 2. 计算方差求标准差 (σ)
        double varianceSum = 0;
        for (float val : audioData) {
            varianceSum += Math.pow(val - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / audioData.length);

        // 极小常数 ε，防止在绝对静音环境（全 0 数据）中发生除以零的崩溃
        double epsilon = 1e-7;

        // 3. 应用 Z-score 标准化
        float[] normalized = new float[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            normalized[i] = (float) ((audioData[i] - mean) / (stdDev + epsilon));
        }

        Log.d(TAG, "数学归一化完成");
        return normalized;
    }

    /**
     * 简单的静音检测 (拦截无声片段)
     */
    public static boolean isSilent(float[] audioData) {
        double rms = 0;
        for (float val : audioData) {
            rms += val * val;
        }
        rms = Math.sqrt(rms / audioData.length);

        boolean silent = rms < 0.01;
        if (silent) Log.w(TAG, "⚠️ 录音能量极低（RMS=" + rms + "），触发静音拦截");
        return silent;
    }
}