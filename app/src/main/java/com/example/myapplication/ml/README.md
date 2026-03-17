# ML 模块说明

本目录包含所有机器学习相关的 Java 类。

## 📁 文件列表

| 文件 | 说明 |
|------|------|
| `ObjectRecognitionHelper.java` | YOLO 物体检测工具（TFLite） |
| `Wav2Vec2Scorer.java` | 端侧发音评分器（ONNX Runtime） |
| `AudioProcessor.java` | 音频预处理工具（重采样、归一化） |

## 🔄 工作流程

```
录音 (MPEG-4/AAC)
    ↓
AudioProcessor.loadAndPreprocess()
    ↓
16kHz PCM float32 [-1, 1]
    ↓
Wav2Vec2Scorer.score()
    ↓
音素序列 + 发音评分
```

## 📚 相关文档

- 项目主文档：`../docs/PROJECT_README.md`
- 快速指南：`../docs/QUICK_START.md`
- 完整指南：`../docs/WAV2VEC2_ONNX_GUIDE.md`
