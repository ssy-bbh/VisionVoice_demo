# ML 模块说明

项目目录中机器学习相关的 Java 类。

## 文件列表

| 文件 | 说明 |
|------|------|
| `ObjectRecognitionHelper.java` | YOLO 目标检测（TFLite） |
| `Wav2Vec2Scorer.java` | 端侧语音评分（ONNX Runtime） |
| `AudioProcessor.java` | 音频预处理（解码+重采样+归一化） |
| `PhonemeCache.java` | 音素缓存加速 |

## 处理流程

```
录音 (MPEG-4/AAC)
    │
AudioProcessor.loadAndPreprocess()
    │
16kHz PCM float32 [-1, 1]
    │
Wav2Vec2Scorer.score()
    │
识别结果 + 评分反馈
```

## 相关文档

- 项目文档：`docs/dev/STRUCTURE.md`
- 快速开始：`docs/setup/QUICK_START.md`
- 模型导出：`docs/setup/ONNX_GUIDE.md`
