# Wav2Vec2 模型导出指南

## 模型选型

| 项目 | 内容 |
|------|------|
| 模型 | `vitouphy/wav2vec2-xls-r-300m-phoneme` |
| 参数量 | 300M |
| 输出 | ARPAbet 音素（40类） |

## 导出步骤

### 1. 环境准备

```bash
cd D:\AndroidStudioProjects\MyApplication\backend
pip install torch transformers onnxruntime onnxruntime-tools
```

### 2. 运行导出

```bash
python export_true_onnx.py
```

### 3. 部署到 Android

```
app/src/main/assets/onnx/
├── model.onnx          # FP32 (~1.2 GB)
└── model_quant.onnx    # INT8 (~300 MB)
```

**配置** `app/build.gradle.kts`：
```kotlin
androidResources {
    noCompress += ".onnx"
}
```

## Android 推理架构

```
音频 (m4a/AAC)
    → AudioProcessor (解码+16kHz+归一化)
    → float[] (16kHz PCM)
    → Wav2Vec2Scorer (ONNX推理+CTC解码)
    → 音素序列
    → Needleman-Wunsch对齐
    → 评分 (0-100)
```

## 核心类

| 类 | 职责 |
|----|------|
| `AudioProcessor` | AAC解码、重采样、归一化 |
| `Wav2Vec2Scorer` | ONNX推理、CTC解码、评分 |
| `PhonemeCache` | CMU Dict查表、缓存 |

## 性能

| 指标 | 数值 |
|------|------|
| 模型加载 | ~800ms (mmap) |
| 推理时间 | ~320ms (1秒音频) |
| Java堆占用 | ~11MB |

---

**参考**: `backend/export_true_onnx.py` - 导出脚本
