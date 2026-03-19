# 实验记录

## 2026-03-17: Wav2Vec2 模型移动端测试

**目标**: 探索模型 ~360MB 的移动端推理可行性

### 实验结果

| 指标 | 数值 |
|------|------|
| 模型大小 | 360.30 MB (FP32) |
| 模型加载时间 | ~3727 ms |
| 推理时间 (1秒音频) | ~321 ms |
| Java 堆占用 | 11 MB / 384 MB |
| ONNX 输出形状 | `[1, 49, 32]` |

### 关键发现

- 使用 mmap 加载可避免 OOM（Java 堆仅 11MB）
- INT8 量化可减少 70% 体积

---

## 2026-03-18: 模型选型

| 模型 | 输出格式 | 音素来源 |
|------|----------|----------|
| `facebook/wav2vec2-lv-60-espeak-cv-ft` | IPA | phonemizer |
| `vitouphy/wav2vec2-xls-r-300m-phoneme` | ARPAbet | CMU Dict |

**选择**: vitouphy/wav2vec2-xls-r-300m-phoneme
- 直接输出音素标签，无需 phonemizer
- 与 CMU Dict 的 ARPAbet 格式对齐
