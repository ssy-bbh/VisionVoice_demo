package com.example.myapplication.ml;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioProcessor {
    private static final String TAG = "AudioProcessor";

    public static float[] loadAndPreprocess(File audioFile) throws IOException {
        FileInputStream fis = new FileInputStream(audioFile);
        byte[] pcmBytes = new byte[(int) audioFile.length()];
        fis.read(pcmBytes);
        fis.close();

        float[] floatData = new float[pcmBytes.length / 2];
        ByteBuffer bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sb = bb.asShortBuffer();
        for (int i = 0; i < floatData.length; i++) {
            floatData[i] = sb.get() / 32768.0f;
        }

        // 🚨 核心修复 1：VAD (静音检测)，在放大前切除头尾的空白环境音！
        // 0.02f 是一个经过测试的经验阈值，能过滤呼吸声，但保留气声
        floatData = trimSilence(floatData, 0.02f);

        Log.d(TAG, "PCM 读取完毕，VAD裁剪后采样点数=" + floatData.length);
        return normalize(floatData);
    }

    // 🔪 极简高能：掐头去尾算法
    private static float[] trimSilence(float[] audioData, float threshold) {
        if (audioData == null || audioData.length == 0) return audioData;

        int start = 0;
        int end = audioData.length - 1;
        int buffer = 1600; // 0.1秒的缓冲时间，防止切掉 p、t 等爆破音的尾音

        for (int i = 0; i < audioData.length; i++) {
            if (Math.abs(audioData[i]) > threshold) {
                start = Math.max(0, i - buffer);
                break;
            }
        }
        for (int i = audioData.length - 1; i >= 0; i--) {
            if (Math.abs(audioData[i]) > threshold) {
                end = Math.min(audioData.length - 1, i + buffer);
                break;
            }
        }

        if (start >= end) return audioData; // 如果太安静，原样返回

        float[] trimmed = new float[end - start + 1];
        System.arraycopy(audioData, start, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private static float[] normalize(float[] audioData) {
        if (audioData == null || audioData.length == 0) return audioData;
        double sum = 0;
        for (float val : audioData) sum += val;
        double mean = sum / audioData.length;

        double varianceSum = 0;
        for (float val : audioData) varianceSum += Math.pow(val - mean, 2);
        double stdDev = Math.sqrt(varianceSum / audioData.length);

        double epsilon = 1e-7;
        float[] normalized = new float[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            normalized[i] = (float) ((audioData[i] - mean) / (stdDev + epsilon));
        }
        return normalized;
    }

    public static boolean isSilent(float[] audioData) {
        double rms = 0;
        for (float val : audioData) rms += val * val;
        rms = Math.sqrt(rms / audioData.length);
        return rms < 0.01;
    }
}