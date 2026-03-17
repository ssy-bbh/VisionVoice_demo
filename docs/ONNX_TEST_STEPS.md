# ONNX 模型移动端测试步骤

**测试日期：** 2026-03-17  
**测试目的：** 验证 360MB Wav2Vec2 模型在 Android 设备上的可行性  
**模型来源：** facebook/wav2vec2-base (ONNX 格式)  
**模型大小：** 360.30 MB  
**预估内存：** ~540 MB

---

## 📋 测试准备

### 1. 环境要求
- Android Studio Hedgehog 或更高版本
- Android 设备（建议 RAM ≥ 4GB）
- ONNX Runtime Mobile 依赖

### 2. 已创建文件
| 文件 | 路径 | 说明 |
|------|------|------|
| OnnxTestActivity.java | `ui/test/OnnxTestActivity.java` | 测试 Activity |
| activity_onnx_test.xml | `res/layout/activity_onnx_test.xml` | 测试界面 |
| model.onnx | `assets/onnx/model.onnx` | ONNX 模型文件 |

---

## 🚀 测试步骤

### 步骤 1：添加 ONNX Runtime 依赖

**文件：** `app/build.gradle.kts`

```kotlin
dependencies {
    // ... 现有依赖 ...
    
    // 添加 ONNX Runtime Mobile
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
}
```

**操作：**
1. 打开 Android Studio
2. 打开 `app/build.gradle.kts`
3. 添加上述依赖
4. 点击 "Sync Now"

---

### 步骤 2：在 AndroidManifest.xml 中注册 Activity

**文件：** `app/src/main/AndroidManifest.xml`

在 `<application>` 标签内添加：

```xml
<activity 
    android:name=".ui.test.OnnxTestActivity"
    android:label="ONNX 模型测试" />
```

---

### 步骤 3：添加测试入口（可选）

**方式 A：在 HomeFragment 中添加测试按钮**

修改 `ui/home/HomeFragment.java`：

```java
// 在 onCreateView 中添加
View btnTestOnnx = view.findViewById(R.id.btnTestOnnx);
btnTestOnnx.setOnClickListener(v -> {
    Intent intent = new Intent(getActivity(), OnnxTestActivity.class);
    startActivity(intent);
});
```

**方式 B：直接从 Android Studio 运行**

1. 打开 `OnnxTestActivity.java`
2. 点击运行按钮
3. 选择设备

---

### 步骤 4：运行测试

1. **连接设备**
   - 使用 USB 连接 Android 手机
   - 开启开发者模式和 USB 调试

2. **运行应用**
   - 点击 Android Studio 运行按钮
   - 等待应用安装

3. **执行测试**
   - 进入 OnnxTestActivity
   - 点击"开始测试"按钮
   - 等待测试完成（可能需要 10-30 秒）

---

## 📊 预期结果

### 成功情况
```
✅ 测试通过

模型加载时间: 2000-5000 ms
推理时间: 500-2000 ms

模型大小: 360 MB
内存占用: ~540 MB (估算)

结论: 模型可以运行，但内存占用较大
```

### 失败情况

**情况 1：OOM（内存溢出）**
```
❌ 错误: java.lang.OutOfMemoryError
```
**原因：** 设备内存不足  
**解决：** 使用更小模型或高端设备

**情况 2：模型加载失败**
```
❌ 错误: OrtException: Model loading failed
```
**原因：** 模型文件损坏或路径错误  
**解决：** 检查 assets/onnx/model.onnx 是否存在

**情况 3：依赖缺失**
```
❌ 错误: ClassNotFoundException: ai.onnxruntime...
```
**原因：** ONNX Runtime 未正确导入  
**解决：** 重新 Sync Gradle

---

## 📝 测试记录表

| 测试项 | 预期结果 | 实际结果 | 状态 |
|--------|----------|----------|------|
| 模型加载 | 成功 | 待记录 | ⏳ |
| 推理时间 | < 5s | 待记录 | ⏳ |
| 内存占用 | ~540MB | 待记录 | ⏳ |
| OOM 错误 | 无 | 待记录 | ⏳ |

**测试设备：** _________________  
**设备 RAM：** _________________  
**Android 版本：** _________________  
**测试时间：** _________________

---

## 🔧 后续优化方向

### 如果测试成功但性能不佳
1. **使用小模型**
   - `facebook/distil-wav2vec2` (~50MB)
   - `openai/whisper-tiny` (~39MB)

2. **模型量化**
   - INT8 量化可减小 70% 体积
   - 需要预处理避免错误

3. **TensorFlow Lite 转换**
   - 可能获得更好性能
   - 需要重新转换模型

### 如果测试失败（OOM）
1. **必须使用小模型**
2. **考虑云端推理**
3. **分段加载模型**（复杂）

---

## 📚 参考文档

- ONNX Runtime Android: https://onnxruntime.ai/docs/get-started/with-java.html
- Wav2Vec2 模型: https://huggingface.co/facebook/wav2vec2-base
- ARIELLE 论文: 2024 IEEE APCCAS

---

**记录时间：** 2026-03-17 15:58  
**记录者：** AI 助手
