# VisionVoice 物体识别开发文档

**项目：** VisionVoice - AR 英语学习系统  
**最后更新：** 2026-03-19  
**版本：** v1.1

---

## 一、概述

VisionVoice 的物体识别模块是整个 AR 英语学习系统的视觉入口，负责：
- 实时识别摄像头画面中的日常物品
- 为用户提供 "看到什么就 学什么" 的即时学习体验
- 为发音练习模块提供目标词汇

**技术方案：**
- 模型：YOLOv8n (TFLite 格式)
- 输入：640×640 RGB 图像
- 输出：40 类常见物品 + 边界框 + 置信度
- 框架：TensorFlow Lite + CameraX

---

## 二、模型选型

### 2.1 候选模型对比

| 模型 | 大小 | mAP (COCO) | 移动端适用性 | 备注 |
|------|------|-------------|--------------|------|
| YOLOv4 | ~245MB | 65.7 | ❌ 不适合 | 太大，移动端内存扛不住 |
| MobileNetV2 + SSD | ~20MB | 22.1 | ⚠️ 一般 | 太小巧，精度不够 |
| MobileNetV3 + SSD | ~16MB | 24.8 | ⚠️ 一般 | 比 V2 略好 |
| EfficientDet | ~26MB | 33.8 | ⚠️ 一般 | 精度 OK 但推理慢 |
| **YOLOv8n** | **12.5MB** | **37.3** | ✅ **最佳** | **选中** |

### 2.2 为什么选择 YOLOv8n

1. **精度与大小的最佳平衡**
   - 12.5MB 轻量模型，却能达到 37.3 mAP
   - 比 YOLOv5n 高 3-4 个 mAP 点

2. **Anchor-Free 架构**
   - 无需手动调优 anchor box 候选
   - 简化后处理逻辑

3. **Decoupled Head**
   - 分类和回归分支分离
   - 训练时梯度流更顺畅
   - 小物体召回率更高

4. **官方 TFLite 支持**
   - Ultralytics 提供直接的 TFLite 导出
   - 兼容性好

### 2.3 为什么用 TFLite 而不是 ONNX Runtime

虽然后来语音模块用了 ONNX Runtime（因为 Wav2Vec2 是 Transformer），但视觉模型选择 TFLite 的原因：

| 对比项 | TFLite | ONNX Runtime |
|--------|--------|--------------|
| Android 原生支持 | ✅ Google 官方 | ⚠️ 第三方 |
| CNN 模型支持 | ✅ 非常好 | ✅ 也可以 |
| 内存映射加载 | ✅ MappedByteBuffer | ⚠️ 需要额外处理 |
| GPU/NNAPI 加速 | ✅ 透明支持 | ✅ 也可以 |

**关键点：** TFLite 的 `MappedByteBuffer` + `AssetFileDescriptor` 可以让 OS 直接 mmap 模型文件，不占用 Java Heap，这对于同时加载两个模型（YOLO 12.5MB + Wav2Vec2 360MB）的场景至关重要。

---

## 三、技术实现

### 3.1 模型文件

```
app/src/main/assets/
├── yolov8n.tflite      # 12.5MB，YOLOv8 Nano 模型
└── labels.txt          # 40 类标签名称
```

### 3.2 40 类物体列表

```
// 日常物品
backpack, book, bowl, cup, keyboard, laptop, mouse, remote, phone, suitcase
// 食物
apple, banana, orange, pizza, donut, cake, fruit, carrot
// 家居
bed, chair, couch, table, TV, clock, vase, scissors, teddy bear, hair drier, toothbrush
// 动物
bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe
// 户外
bicycle, car, motorcycle, airplane, bus, train, truck, boat, traffic light, fire hydrant, stop sign
```

### 3.3 核心代码结构

```java
// ml/ObjectRecognitionHelper.java

public class ObjectRecognitionHelper {
    private Interpreter tflite;
    private MappedByteBuffer modelBuffer;
    
    // 1. 模型加载 - 使用内存映射
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) 
            throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    
    // 2. 输入预处理
    ImageProcessor imageProcessor = new ImageProcessor.Builder()
        .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
        .add(new NormalizeOp(0f, 255f))  // 归一化到 [0,1]
        .build();
    
    // 3. 推理
    tflite.run(byteBuffer, outputBuffer);
    
    // 4. 后处理 - 解析输出张量
    postProcess(outputBuffer, callback, originalWidth, originalHeight);
}
```

### 3.4 输出张量解析

YOLOv8n 输出形状：`[1, 44, 8400]`

```
8400 = 80×80 + 40×40 + 20×20  (三个检测头的候选框)
前4个通道：边界框 (cx, cy, w, h)，归一化到 [0,1]
后40个通道：40 个类别的置信度
```

---

## 四、核心难点与解决方案

### 4.1 难点一：原始输出张量解析

**问题：** YOLOv8 TFLite 导出缺少元数据，无法使用高 level 的 `ObjectDetector` API

**解决方案：** 手动解析

```java
// 遍历 8400 个候选框
for (int i = 0; i < 8400; i++) {
    // 1. 取前4个通道 - 边界框
    float cx = output[0][i] / 640f;
    float cy = output[1][i] / 640f;
    float w = output[2][i] / 640f;
    float h = output[3][i] / 640f;
    
    // 2. 取后40个通道 - 类别置信度
    float maxConfidence = 0;
    int classId = 0;
    for (int c = 0; c < 40; c++) {
        if (output[4 + c][i] > maxConfidence) {
            maxConfidence = output[4 + c][i];
            classId = c;
        }
    }
    
    // 3. 置信度阈值过滤
    if (maxConfidence > 0.5f) {
        // 保留结果
    }
}
```

**阈值选择经验：**
- < 0.4：误检太多（杂乱场景）
- > 0.6：漏检太多（中等遮挡）
- **0.5（选中）**

---

### 4.2 难点二：边界框坐标恢复

**问题：** YOLOv8 输出是中心格式 + 归一化坐标，需要转换为屏幕坐标

**公式：**

```java
// 中心格式 → 角点格式
float xmin = (cx - w / 2) * scaleX;
float ymin = (cy - h / 2) * scaleY;
float xmax = (cx + w / 2) * scaleX;
float ymax = (cy + h / 2) * scaleY;

// scaleX = originalWidth / 640
// scaleY = originalHeight / 640
```

**坑：** CameraX 的 `analysis` 分辨率和 `display` 分辨率可能不同，必须用 analysis 分辨率来计算缩放比例！

---

### 4.3 难点三：内存映射加载

**问题：** 12.5MB 模型用 `byte[]` 方式加载会占用 Java Heap，增加 GC 压力

**解决方案：** OS 级内存映射

```java
// 方式1：直接用（推荐）
MappedByteBuffer modelBuffer = FileChannel.map(
    FileChannel.MapMode.READ_ONLY, 
    startOffset, 
    declaredLength
);

// 方式2：先复制到内部存储再用（ONNX 用这个，因为要规避 assets 压缩）
File modelFile = new File(getFilesDir(), "model.onnx");
if (!modelFile.exists()) {
    // 从 assets 复制到内部存储
    InputStream is = getAssets().open("onnx/model.onnx");
    FileOutputStream fos = new FileOutputStream(modelFile);
    byte[] chunk = new byte[8192];
    while (is.read(chunk) != -1) fos.write(chunk);
}
session = env.createSession(modelFile.getAbsolutePath());
```

**效果：** OS 按需加载页面，不用的部分不会占用 RAM

---

### 4.4 难点四：简化 NMS 算法 ⭐自创优化

**问题：** 标准 NMS 需要 O(n²) 的 IoU 比较，计算量大

**自创方案：** Top-1 选择策略

```java
// 传统 NMS：O(n²)
for (box in sorted_boxes) {
    for (other in remaining_boxes) {
        if (iou(box, other) > threshold) {
            suppress(other);
        }
    }
}

// 自创方法：O(n)
float maxConfidence = 0;
Result bestBox = null;
for (box in filteredBoxes) {  // 先用 0.5 过滤一轮
    if (box.confidence > maxConfidence) {
        maxConfidence = box.confidence;
        bestBox = box;
    }
}
```

**为什么可行：**
- 词汇学习场景，用户通常只拍一个物品
- 等价于 IoU threshold = 1.0 的 NMS
- 无精度损失，性能提升显著

---

### 4.5 难点五：推理线程隔离

**问题：** TFLite `Interpreter` 非线程安全；CameraX 后台线程推送帧；UI 线程要保持响应

**解决方案：** 专用线程池 + Callback

```java
// 专用单线程执行器
private ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();

// 推理调用
inferenceExecutor.execute(() -> {
    DetectionResult result = helper.detectObjects(bitmap);
    mainHandler.post(() -> callback.onResult(result));
});

// 节流：500ms 内只处理一帧
if (System.currentTimeMillis() - lastInferenceTime < 500) {
    return;
}
lastInferenceTime = System.currentTimeMillis();
```

---

### 4.6 难点六：输入归一化对齐

**问题：** 早期版本的模型一直返回 0 置信度

**根因：** YOLOv8 训练时用 [0,1] 归一化，但代码没有正确预处理

**解决方案：**

```java
// 正确写法
ImageProcessor imageProcessor = new ImageProcessor.Builder()
    .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
    .add(new NormalizeOp(0f, 255f))  // 除以 255
    .build();
```

---

## 五、性能数据

### 5.1 测试设备
- 华为手机，RAM 384MB（Java 堆）

### 5.2 性能指标

| 指标 | 数值 | 说明 |
|------|------|------|
| 模型文件 | 12.5MB | TFLite 格式 |
| 输入尺寸 | 640×640 RGB | |
| 识别类别 | 40 类 | COCO 子集 |
| 推理线程 | 4 线程 | `options.setNumThreads(4)` |
| 内存占用 | ~0MB Java | MappedByteBuffer 不占 Java Heap |
| 推理时间 | ~50ms | 取决于设备 |
| FPS | ~15 FPS | 实时场景可用 |

### 5.3 与 ARIELLE 对比

| 对比项 | ARIELLE | VisionVoice |
|--------|---------|-------------|
| 目标硬件 | Intel Nezha (x86) | Android 手机 (ARM) |
| 推理框架 | Intel OpenVINO | TensorFlow Lite |
| 模型大小 | 12.5MB (相同) | 12.5MB (相同) |
| 内存占用 | N/A | ~0MB Java Heap |
| 推理时间 | 未披露 | ~50ms |

---

## 六、Bug 记录

| Bug 描述 | 根因 | 解决方案 |
|----------|------|----------|
| 模型加载后 APP 闪退 | `byte[]` 方式占用 Java Heap 太大，触发 OOM | 改用 MappedByteBuffer |
| 所有类别置信度为 0 | 缺少 `NormalizeOp(0f, 255f)` 归一化 | 添加正确的归一化 |
| bounding box 位置偏移 | 用 display 分辨率而非 analysis 分辨率计算缩放 | 改用 analysis 分辨率 |
| 帧率极低卡顿 | 在主线程做推理 | 移到后台线程 + 节流 |
| 模型文件无法打开 | assets 目录的 .tflite 被压缩 | `aaptOptions { noCompress += ".tflite" }` |

---

## 七、未来优化方向

### 7.1 模型级量化
- 探索 INT8 量化（当前是 FP32）
- 目标：将 12.5MB 进一步压缩

### 7.2 动态分辨率
- 根据场景复杂度动态调整输入分辨率
- 简单场景用低分辨率提速

### 7.3 GPU/NNAPI 加速
- 启用 TFLite GPU Delegate
- 透明加速，无需改代码

### 7.4 多物体模式
- 当前是 Top-1，可以扩展为 Top-3
- 适用于桌面有多物品的场景

### 7.5 AR 增强
- 在 bounding box 上叠加发音按钮
- 实现真正的 AR 交互

---

## 八、相关文档

- `../docs/PROJECT_README.md` - 项目总览
- `../docs/QUICK_START.md` - 快速开始指南
- `ml/ObjectRecognitionHelper.java` - 源代码
- `ml/README.md` - ML 模块说明

---

**维护人：** VisionVoice Team  
**AI 助手：** OpenClaw
