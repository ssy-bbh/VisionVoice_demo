# ADR-002: 端侧 Wav2Vec2 离线发音评分

## 状态
✅ **已接受**（v1.1 开发阶段）

## 背景

VisionVoice 的核心差异化功能之一是**发音练习**：用户对着摄像头说出一个单词，系统需要对其发音进行评分和反馈。

传统方案依赖**云端 API**（如 Google Speech-to-Text + 自研评分服务），但存在以下问题：
- **隐私风险**：用户录音需上传到服务器
- **延迟高**：网络往返通常 > 1 秒
- **依赖网络**：离线场景完全不可用
- **成本高**：大量用户使用时 API 调用成本显著

## 问题陈述

如何在保护用户隐私的前提下，在 Android 设备端实现低延迟、高质量的发音评分功能？

## 决策方案（已采纳）

使用 **ONNX Runtime Mobile** 在设备端运行 **Wav2Vec2** 量化模型：

| 组件 | 选择 | 说明 |
|------|------|------|
| 模型 | `wav2vec2-lv-60-espeak-cv-ft` | Meta 开源，擅长英语音素识别 |
| 推理引擎 | `onnxruntime-android:1.17.0` | 跨平台、高性能、官方支持 Android |
| 模型格式 | ONNX（量化版 `model_quant.onnx`）| FP16 量化，体积 ~50MB |
| 评分算法 | Needleman-Wunsch 音素对齐 | 参考 `backend/server.py` 中的 NW 算法 |

### 端侧推理流程

```
用户录音（WAV PCM）
    ↓ AudioProcessor.processAudio()
    ├─ 重采样至 16kHz
    ├─ 噪音门限检测（< 0.015 振幅视为无效）
    └─ 自动增益归一化（峰值归一化到 1.0）
    ↓
Wav2Vec2Scorer.score()
    ├─ transcribe()：ONNX 推理 → Greedy Decoding → 音素序列
    ├─ splitPhonemeString()：参考音素字符串分词
    └─ needlemanWunsch()：序列对齐 → 计算分數
    ↓
PronunciationScore(score, referencePhonemes, userPhonemes, feedback)
```

## 评估的备选方案

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| **ONNX 端侧推理（本采纳）** | 隐私保护、离线可用、低延迟 | APK 体积 +50MB，低端设备性能差 | ✅ |
| 云端 API（Google STT / 自研） | 准确率高，模型随时更新 | 隐私风险、延迟高、依赖网络 | ❌ |
| 纯规则算法（DTW / 频谱对比） | 无模型依赖 | 准确率低，无法真正理解发音 | ❌ |
| WebAssembly + Web 标准 API | 跨平台 | Android 端性能差，缺乏细粒度控制 | ❌ |

## 后果

### ✅ 正面后果
- **隐私零风险**：用户录音永不离开设备
- **离线可用**：无网络环境（如飞机、地铁）下功能完全正常
- **低延迟**：端到端评分 < 1 秒（不含 TTS 播放）
- **零成本**：无需支付云端 API 调用费用

### ❌ 负面后果
- **APK 体积增加**：约 +50MB（量化 ONNX 模型）
- **最低硬件要求**：需要 ≥ 2GB RAM 的设备才能流畅运行
- **模型更新不便**：模型更新需要用户重新安装 APK

### ⚠️ 可迁移后果
- 需要持续关注 ONNX Runtime 的 Android 兼容性问题
- 未来可能需要针对不同档位设备提供差异化模型（激进量化版 vs 完整版）

## 参考资料
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [Wav2Vec2 on HuggingFace](https://huggingface.co/facebook/wav2vec2-lv-60-espeak-cv-ft)
- VisionVoice `backend/server.py` — NW 算法原始实现
