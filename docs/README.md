# VisionVoice 项目 - 快速开始

## 🎯 下一步操作（按顺序）

### 1️⃣ 安装 Python 依赖（5分钟）
```bash
cd D:\AndroidStudioProjects\MyApplication\backend
pip install optimum[onnxruntime] transformers torch torchaudio phonemizer onnxruntime-gpu
```

### 2️⃣ 导出 ONNX 模型（10-15分钟）
```bash
python export_onnx.py
# 选择模型 1 (facebook/wav2vec2-base)
# 是否量化？y
```

### 3️⃣ 更新 Android 依赖
打开 `app/build.gradle.kts`，添加：
```kotlin
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
```
然后 **Sync Gradle**

### 4️⃣ 测试端侧模型
- 打开 Android Studio
- 同步项目
- 运行 App 测试

## 📚 完整参考
- `docs/QUICK_START.md` - 详细步骤
- `docs/WAV2VEC2_ONNX_GUIDE.md` - 完整指南
- `docs/CHANGELOG.md` - 变更日志

---

**项目位置：** `D:\AndroidStudioProjects\MyApplication\`
