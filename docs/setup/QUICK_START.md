# 快速开始

## 1. 安装 Python 依赖 (~5分钟)

```bash
cd D:\AndroidStudioProjects\MyApplication\backend
pip install optimum[onnxruntime] transformers torch torchaudio phonemizer
```

## 2. 导出 ONNX 模型 (~15分钟)

```bash
python export_onnx.py
# 选择模型 1: facebook/wav2vec2-base
# 确认导出: y
```

模型会保存到 `app/src/main/assets/onnx/model_quant.onnx` (~25MB)

## 3. Android 配置 (~2分钟)

在 `app/build.gradle.kts` 添加：

```kotlin
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
```

然后 **Sync Gradle**

## 4. 运行测试

在 Android Studio 中：
1. Sync 项目
2. Build > Rebuild Project
3. 运行 App 到设备

---

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 模型加载失败 | 检查 `assets/onnx/model_quant.onnx` 是否存在 |
| 评分耗时过长 | 确认使用 INT8 量化模型 (~25MB) |
| 评分不准确 | 确保音频是 16kHz PCM 格式 |

---

**参考**: `setup/ONNX_GUIDE.md` - 详细模型导出指南
