package com.example.myapplication.ml;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音频预处理工具类
 * 负责加载音频文件并转换为 Wav2Vec2 所需的格式
 * 
 * 输入：MPEG-4/AAC 格式的录音文件
 * 输出：16kHz PCM float32 数组（归一化到 [-1, 1]）
 */
public class   AudioProcessor {
    private static final String TAG = "AudioProcessor";
    private static final int TARGET_SAMPLE_RATE = 16000;
    
    /**
     * 加载音频文件并转换为 16kHz PCM float 数组
     * 
     * @param audioFile 音频文件（MPEG-4/AAC 格式）
     * @return 16kHz PCM 数据（float32，归一化到 [-1, 1]）
     * @throws IOException 读取音频失败
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
        ByteBuffer buffer = ByteBuffer.allocate((int)audioFile.length() * 4);
        
        while (true) {
            int sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) break;
            extractor.advance();
        }
        
        extractor.release();
        
        // 转换为 float 数组
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        return resampleAndMix(buffer, sampleRate, channelCount);
    }
    
    /**
     * 选择音频轨道
     */
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
    
    /**
     * 重采样和混音处理
     */
    private static float[] resampleAndMix(ByteBuffer buffer, int sourceSampleRate, int channelCount) {
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
        
        // 归一化处理
        normalize(samples);
        
        return samples;
    }
    
    /**
     * 线性插值重采样
     */
    private static float[] resample(float[] samples, int fromRate, int toRate) {
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
    
    /**
     * 音频归一化（增强音量）
     */
    private static void normalize(float[] samples) {
        // 计算最大振幅
        float maxAmplitude = 0;
        for (float sample : samples) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
        }
        
        // 如果音量太低，增强音量
        if (maxAmplitude > 0 && maxAmplitude < 0.5f) {
            float gain = 0.5f / maxAmplitude;
            for (int i = 0; i < samples.length; i++) {
                samples[i] = samples[i] * gain;
            }
        }
        
        // 确保归一化到 [-1, 1]
        float currentMax = 0;
        for (float sample : samples) {
            currentMax = Math.max(currentMax, Math.abs(sample));
        }
        
        if (currentMax > 1.0f) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = samples[i] / currentMax;
            }
        }
    }
}
