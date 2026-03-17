# 🚀 端侧推理实施进度

## ✅ 已完成（第 0-1 步）

| 项目 | 状态 | 位置 |
|------|------|------|
| Python 依赖安装 | ⏳ 待执行 | `D:\AndroidStudioProjects\MyApplication\backend` |
| ONNX 模型导出 | ⏳ 待执行 | `backend/export_onnx.py` |
| AudioProcessor.java | ✅ 已创建 | `app/src/main/java/.../ml/AudioProcessor.java` |
| Wav2Vec2Scorer.java | ✅ 已创建 | `app/src/main/java/.../ml/Wav2Vec2Scorer.java` |

## 📋 下一步（第 2-3 步）

### 第 2 步：更新 Android 依赖
**文件：** `app/build.gradle.kts`

```kotlin
dependencies {
    // ... 现有依赖 ...
    
    // ✅ 添加：ONNX Runtime Mobile
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
    
    // ✅ 添加：音频处理（可选）
    implementation("com.google.guava:guava:32.1.3-android")
}
```

**操作：** 在 Android Studio 中打开项目 -> Sync Gradle

### 第 3 步：创建 assets/onnx/ 目录
**位置：** `app/src/main/assets/onnx/`

**需要放的文件：**
- `model.onnx` - FP32 模型（~95MB）
- `model_quant.onnx` - INT8 量化模型（~25MB）

**来源：** 运行 `python backend/export_onnx.py` 后自动生成

## 🎯 推荐执行顺序

### 方案 A：按部就班（推荐新手）
```
1️⃣ 运行 export_onnx.py → 获取 ONNX 模型
2️⃣ 手动更新 build.gradle.kts
3️⃣ Sync Gradle
4️⃣ 测试端侧模型加载
```

### 方案 B：一步到位（推荐熟练者）
```
1️⃣ 打开 Android Studio
2️⃣ 在 build.gradle.kts 中添加 ONNX Runtime 依赖
3️⃣ Sync Gradle
4️⃣ 复制 Wav2Vec2Scorer.java 和 AudioProcessor.java
5️⃣ 创建 assets/onnx/ 目录
6️⃣ 导出模型并复制到 assets/onnx/
7️⃣ 修改 PracticeActivity
```

---

## 📄 参考文档

| 文档 | 用途 |
|------|------|
| `docs/QUICK_START.md` | 快速实施指南 |
| `docs/WAV2VEC2_ONNX_GUIDE.md` | 详细实施步骤 |
| `backend/export_onnx.py` | ONNX 模型导出脚本 |

---

## 💡 提示

1. **先运行导出脚本**，获取 ONNX 模型
2. **修改 Android 文件前**，先测试模型能正常加载
3. **遇到问题时**，可以回退到后端模式继续开发

---

**开始执行？** 你想先运行导出脚本，还是先在 Android Studio 中更新依赖？