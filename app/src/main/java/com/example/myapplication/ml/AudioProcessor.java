package com.example.myapplication.ml;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频预处理工具类
 *
 * 输入：MPEG-4/AAC 格式的录音文件（.m4a）
 * 输出：16kHz PCM float32 数组（归一化到 [-1, 1]）
 *
 * 关键：.m4a 是 AAC 压缩格式，必须用 MediaCodec 解码为 PCM，
 *       不能直接用 MediaExtractor.readSampleData() 当 PCM 用。
 */
public class AudioProcessor {
    private static final String TAG = "AudioProcessor";
    private static final int TARGET_SAMPLE_RATE = 16000;
    private static final long TIMEOUT_US = 10000; // 10ms

    /**
     * 加载 .m4a 文件，解码为 16kHz PCM float32 数组
     */
    public static float[] loadAndPreprocess(File audioFile) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(audioFile.getAbsolutePath());

        // 1. 找到音频轨道
        int trackIndex = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = i;
                format = f;
                break;
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release();
            throw new IOException("未找到音频轨道");
        }

        int sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        String mime = format.getString(MediaFormat.KEY_MIME);

        Log.d(TAG, "音频信息：mime=" + mime
                + " 采样率=" + sourceSampleRate
                + " 声道数=" + channelCount);

        extractor.selectTrack(trackIndex);

        // 2. 用 MediaCodec 解码 AAC → PCM
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        List<byte[]> pcmChunks = new ArrayList<>();
        boolean inputDone = false;
        boolean outputDone = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (!outputDone) {
            // 喂数据给解码器
            if (!inputDone) {
                int inputIndex = codec.dequeueInputBuffer(TIMEOUT_US);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuf = codec.getInputBuffer(inputIndex);
                    inputBuf.clear();
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (sampleSize < 0) {
                        // 数据读完，发 EOS
                        codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputIndex, 0, sampleSize,
                                presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            // 取解码后的 PCM 数据
            int outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            if (outputIndex >= 0) {
                ByteBuffer outputBuf = codec.getOutputBuffer(outputIndex);
                if (outputBuf != null && bufferInfo.size > 0) {
                    // 严格按照底层提供的 offset 和 limit 读取
                    outputBuf.position(bufferInfo.offset);
                    outputBuf.limit(bufferInfo.offset + bufferInfo.size);

                    byte[] chunk = new byte[bufferInfo.size];
                    outputBuf.get(chunk);
                    pcmChunks.add(chunk);
                }
                codec.releaseOutputBuffer(outputIndex, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }

        codec.stop();
        codec.release();
        extractor.release();

        // 3. 合并所有 PCM 块
        int totalBytes = 0;
        for (byte[] chunk : pcmChunks) totalBytes += chunk.length;

        ByteBuffer pcmBuffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (byte[] chunk : pcmChunks) pcmBuffer.put(chunk);
        pcmBuffer.rewind();

        Log.d(TAG, "解码完成，PCM 字节数=" + totalBytes);

        // 4. PCM int16 → float32，混音，重采样，归一化
        return resampleAndNormalize(pcmBuffer, sourceSampleRate, channelCount);
    }

    /**
     * int16 PCM → float32，多声道混音，重采样到 16kHz，归一化
     */
    private static float[] resampleAndNormalize(ByteBuffer pcm,
                                                int sourceSampleRate,
                                                int channelCount) {
        int numFrames = pcm.remaining() / (2 * channelCount); // int16 = 2 bytes
        float[] mono = new float[numFrames];

        for (int i = 0; i < numFrames; i++) {
            float sum = 0;
            for (int ch = 0; ch < channelCount; ch++) {
                sum += pcm.getShort() / 32768.0f;
            }
            mono[i] = sum / channelCount;
        }

        // 重采样
        if (sourceSampleRate != TARGET_SAMPLE_RATE) {
            mono = resample(mono, sourceSampleRate, TARGET_SAMPLE_RATE);
        }

        // 归一化
        normalize(mono);

        Log.d(TAG, "处理完成，采样点数=" + mono.length
                + "（约 " + (mono.length / TARGET_SAMPLE_RATE) + " 秒）");
        return mono;
    }

    /**
     * 线性插值重采样
     */
    private static float[] resample(float[] src, int fromRate, int toRate) {
        float ratio = (float) fromRate / toRate;
        int newLength = (int) (src.length / ratio);
        float[] result = new float[newLength];
        for (int i = 0; i < newLength; i++) {
            float pos = i * ratio;
            int idx = (int) pos;
            float frac = pos - idx;
            result[i] = (idx + 1 < src.length)
                    ? src[idx] * (1 - frac) + src[idx + 1] * frac
                    : src[idx];
        }
        return result;
    }

    /**
     * 峰值归一化 + 噪音门限，与 server.py 逻辑对齐
     * - 振幅 < 0.015：静音/噪音，返回 null（调用方应视为未发声）
     * - 否则：等比例拉满到峰值 0.9（与后端 waveform / max_amplitude 等效）
     */
    private static void normalize(float[] samples) {
        float peak = 0;
        for (float s : samples) peak = Math.max(peak, Math.abs(s));

        if (peak < 1e-6f) return; // 全静音

        // 目标峰值 0.9，与后端 waveform / max_amplitude 等效
        float gain = 0.9f / peak;
        for (int i = 0; i < samples.length; i++) samples[i] *= gain;
    }

    /**
     * 检查音频是否有效（振幅门限与 server.py 一致：< 0.015 视为静音）
     */
    public static boolean isSilent(float[] samples) {
        float peak = 0;
        for (float s : samples) peak = Math.max(peak, Math.abs(s));
        boolean silent = peak < 0.05f;
        if (silent) Log.w(TAG, "⚠️ 录音音量极低（峰值=" + peak + "），触发静音检测");
        return silent;
    }
}
