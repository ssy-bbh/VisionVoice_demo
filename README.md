# VisionVoice

> AR英语辅助学习应用 — See it. Speak it. Master it.

**Author**: 宋姚博涵 (Song Yaobohan)  
**Institution**: 武汉大学计算机学院  
**Supervisors**: Prof. Rajesh · Prof. Wu Xiaoping  
**Type**: Bachelor's Thesis (Computer Science), 2026

---

## 项目概述

VisionVoice 是一款 Android 端 AR 英语辅助学习应用，核心功能是：

1. **AR 实时识别**：用摄像头扫描真实场景，YOLO-World v2 端侧检测物体（228 类开放词表）并叠加英文词汇
2. **音素级发音评估**：用户朗读后，Wav2Vec2 + Needleman-Wunsch 算法给出逐音素诊断反馈
3. **离线优先**：所有推理在本地完成，声纹数据不离开设备

---

## 目录结构

```
MyApplication/
├── app/src/main/
│   ├── java/com/example/myapplication/
│   │   ├── ml/                    # 机器学习推理层
│   │   │   ├── YoloDetector.kt          # YOLO-World v2 TFLite 推理（开放词表 + NNAPI 加速）
│   │   │   ├── Wav2Vec2Scorer.java      # ONNX Runtime 音素评分（N-W对齐+容错）
│   │   │   ├── AudioProcessor.java      # PCM 预处理（VAD静音修剪 + Z-score归一化）
│   │   │   ├── ModelManager.java       # 模型全局预加载（ApplicationContext 防止泄漏）
│   │   │   └── PhonemeCache.java       # 音素本地缓存
│   │   ├── ui/
│   │   │   ├── ar/RealtimeActivity.java       # AR 实时扫描页
│   │   │   ├── photo/PhotoRecognitionActivity.java  # 相册静态图识别
│   │   │   ├── practice/PracticeActivity.java      # 发音练习页（含在线/离线双模式）
│   │   │   ├── collection/
│   │   │   │   ├── CollectionFragment.java    # 展柜主页（陀螺仪视差 + 扫光动画）
│   │   │   │   ├── GyroParallaxContainer.java # 递归深度视差容器
│   │   │   │   └── ShowcaseAdapter.java        # 展品卡片适配器
│   │   │   ├── custom/
│   │   │   │   ├── HolographicShaderView.java # 全息激光着色器（API 33+ RuntimeShader）
│   │   │   │   └── GravityShimmerView.java    # 重力微光动画
│   │   │   ├── home/HomeFragment.java   # 首页仪表盘
│   │   │   └── profile/ProfileFragment.kt # 用户数据统计
│   │   ├── view/
│   │   │   ├── OverlayView.java    # 检测框叠加层（200ms 防闪烁 + 最小面积点击命中）
│   │   │   └── FocusBoxView.java   # 中心对焦框
│   │   ├── utils/
│   │   │   ├── GyroscopeHelper.kt        # 陀螺仪传感器辅助类（TYPE_ROTATION_VECTOR）
│   │   │   ├── SensorSmoothingFilter.kt  # 一阶滞后低通滤波（α=0.05）
│   │   │   ├── ShaderManager.kt          # RuntimeShader 着色器管理器
│   │   │   └── UserStatsManager.kt        # 用户数据统计管理
│   │   └── data/
│   │       ├── AppDatabase.java      # Room 数据库（读 labels.txt 动态建图鉴，幂等补库）
│   │       ├── AppDao.java           # 数据访问对象
│   │       ├── PracticeRecord.java   # 练习记录实体
│   │       └── ShowcaseItem.java     # 展品实体（艾宾浩斯遗忘曲线视觉降级）
│   └── assets/
│       ├── yolov8s_worldv2.tflite    # YOLO-World v2 模型（228 类开放词表，动态量化，12.9 MB）
│       ├── labels.txt                # 228 类词表（LVIS ∩ CMU 发音词典 ∪ COCO 80）
│       ├── showcase_categories.txt   # 图鉴分类映射表（单词|分类，启动时建库用）
│       ├── cmudict_ar_pro.dict        # CMU 发音词典（32 KB）
│       └── onnx/                     # Wav2Vec2 ONNX 模型
│           └── model.onnx            # INT8 量化版（~303 MB）
├── app/model_backups/          # 旧模型备份（COCO 80 版、FP32@640 版；不打进 APK）
├── backend/                    # Python FastAPI 后端（可选，本地评分用）
│   ├── server.py              # 主服务：发音评估 + 音标查询
│   └── WavReal.py             # Wav2Vec2 推理封装
├── docs/
│   ├── architecture/SAD.md    # 软件架构设计文档（IEEE 1471）
│   ├── architecture/adr/      # 架构决策记录（ADR）
│   ├── logs/EXPERIMENT.md      # 实验记录
│   └── setup/QUICK_START.md    # 快速开始指南


---

## 核心功能详解

### AR 实时识别（RealtimeActivity）

摄像头捕获实时帧流，`YoloDetector` 执行目标检测，结果通过 `OverlayView` 以绿框 + 标签形式叠加渲染。

**检测配置：**
- 模型：YOLO-World v2（yolov8s 骨干）。228 类词表的 CLIP 文本嵌入已在离线阶段重参数化进分类头权重，端侧推理是纯视觉检测器，无需文本编码器
- 模型输入：416×416 像素（直接拉伸，无 Letterbox 黑边）
- 输出张量：[1, 232, 3549]（4 坐标 + 228 类，×锚框），支持自动检测转置格式
- 置信度阈值：0.30（开放词表模型的置信度普遍低于 COCO 专用模型，阈值过高会漏检）
- NMS IoU 阈值：0.45
- 加速：权重 INT8 动态量化 + NNAPI 硬件加速（NPU/GPU/DSP，不可用时自动回退 CPU；`YoloDetector.USE_NNAPI` 开关）

**性能优化（YoloDetector 源码要点）：**

```kotlin
// 预分配内存区，推理循环中不复新建对象，规避 GC 抖动
private lateinit var inputBuffer: ByteBuffer   // 416×416×3×4 = 2.1 MB
private lateinit var outputBuffer: ByteBuffer // 232×3549×4 = 3.3 MB

// 支持 NCHW（PyTorch导出）和 NHWC 两种输入格式自动识别
// 支持 INT8 / FP32 模型自动检测
```

点击检测框进入发音练习页，`OverlayView` 用最小面积优先算法解决重叠框的点击命中问题。

---

### 发音练习（PracticeActivity）

支持**在线模式**（调用 FastAPI 后端）和**离线模式**（ONNX Runtime Mobile 端侧推理）。

**离线推理流程（源码链路）：**

```
AudioRecord 16kHz PCM → AudioProcessor → Wav2Vec2Scorer → N-W 对齐 → UI 更新
```

**AudioProcessor 预处理三步：**

```java
// 1. PCM 16bit little-endian 解码
ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

// 2. VAD 静音修剪（阈值 0.02，保留 1600 样本缓冲保护爆破音 p/t/k）
trimSilence(floatData, 0.02f, buffer=1600)

// 3. Z-score 声学归一化
normalized[i] = (x - μ) / (σ + 1e-7)
```

**Wav2Vec2Scorer 三级评分（源码实现）：**

| 级别 | 条件 | 得分 | 源码位置 |
|------|------|------|----------|
| Match | 完全匹配 | +1.0 | `getErrorType() → "Match"` |
| Ignored | 16对麦克风容错对（如 ah↔ae, t↔d） | +1.0 | `getErrorType() → "Ignored"` |
| Flaw | 6对典型L2偏误（r/l, v/w 等） | +0.6 | `getErrorType() → "Flaw:..."` |
| Substitution | 其他替换错误 | −1.0 | `getErrorType() → "Substitution"` |

**短词宽容算法：**
```java
if (refSize <= 4 && firstIsGood && matchCount >= (refSize - 1.2f)) {
    acc = Math.max(acc, 0.88f);  // 保底 88 分
}
```

**trimEdgeInsertions**：NW 对齐后，双向扫描剔除首尾因 CTC 边界误解码产生的 Insertion 标记。

---

### 展柜（CollectionFragment）

陀螺仪视差 + 艾宾浩斯遗忘曲线视觉反馈。

**陀螺仪视差实现（GyroParallaxContainer）：**

```java
// 递归遍历 View Tree，每层子控件按 tag 深度值计算偏移
private void applyParallax(ViewGroup parent, float roll, float pitch) {
    for (View child : ...) {
        float depth = Float.parseFloat(child.getTag().toString()); // 如 "0.05" "0.20" "0.50"
        child.setTranslationX(-roll * 150f * depth);
        child.setTranslationY(-pitch * 150f * depth);
        // 递归处理嵌套容器
        if (child instanceof ViewGroup) applyParallax((ViewGroup) child, roll, pitch);
    }
}
```

**一阶滞后低通滤波（SensorSmoothingFilter）：**
```kotlin
// GyroscopeHelper 中 alpha=0.05；GyroParallaxContainer 中 PARALLAX_SENSITIVITY=150f
smoothed[i] += alpha * (input[i] - smoothed[i])
```

---

### 全息激光着色器（HolographicShaderView）

API 33+ 使用 Android `RuntimeShader`（AGSL）实现 YIQ 色相旋转激光动画；旧版本降级为半透明蓝底色：

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    laserShader = ShaderManager.createNewLaserShader();
    shader.setFloatUniform("uTime", currentTime);
    canvas.drawRect(0, 0, w, h, shaderPaint);
    invalidate(); // 持续刷新实现动画
} else {
    shaderPaint.setColor(Color.parseColor("#1A06B4D4")); // 保底
}
```

---

## 数据层

**Room 数据库（AppDatabase）—— 图鉴动态建库：** 图鉴词条不再硬编码。启动时 `syncShowcaseFromLabels()` 读取 `assets/labels.txt`（当前模型的词表），与库中已有词条做差集，只插入缺失项（幂等，不覆盖已解锁的展品数据）；分类名从 `assets/showcase_categories.txt` 查表（`单词|分类` 格式），未收录的词自动归入 "Other"。当前 10 个分类：

Traffic & Street · Animals · Clothing & Wearables · Accessories · Sports & Outdoors · Kitchen & Dining · Food · Home & Electronics · Tools & Stationery · Everyday Items

> 换更大词表的模型时，只需替换模型文件和 `labels.txt`，新词会在下次启动时自动入库；想给新词归类就往 `showcase_categories.txt` 加一行。

**PracticeRecord**：每次练习记录（单词、分值、时间戳、图片路径），用于雷达图统计。

---

## 稳定性加固与性能优化

针对闪退与卡顿做过一轮系统性排查，主要修复：

- **TFLite use-after-free**：`YoloDetector` 推理/释放共用一把锁（`inferLock` + `closed` 标志），消除 `onDestroy` 时 `close()` 与相机线程 `detect()` 并发导致的 native SIGSEGV；`RealtimeActivity` 销毁时先 `awaitTermination` 再释放模型
- **Bitmap 竞态**：分析线程换帧回收与点击回调读取之间用 `bitmapLock` 保护快照；已回收的 Bitmap 在 `detect()` 入口拦截
- **线程池拒绝**：向已 `shutdown` 的 executor 提交任务前先检查并捕获 `RejectedExecutionException`（用户点框后立刻退出的极端时序）
- **大图解码 OOM**：相册识别改为两遍 `inSampleSize` 降采样解码（显示用长边 ≤2000px），不再全尺寸解码 4800 万像素照片
- **相机生命周期竞态**：`ProcessCameraProvider` 回调时检查 `isFinishing()/isDestroyed()`，避免向 DESTROYED 生命周期绑定
- **推理提速（约 3.7×，桌面端实测 161ms → 44ms/帧）**：模型输入 640→416、权重动态量化、NNAPI 硬件加速三管齐下；检测阈值同步下调到 0.30 适配开放词表分数分布

---

## 后端（可选）

`backend/server.py`（FastAPI）提供两个接口：

- `POST /evaluate_pronunciation/` — 上传音频 + 目标单词，返回逐音素对齐结果
- `GET /get_phonetics/?word=xxx` — 查询音标（由后端 `phonemizer` 生成）

后端与端侧使用**相同的 N-W 三级容错矩阵**，确保评分标准一致。

---

## 开发环境

| 依赖 | 版本 |
|------|------|
| Android Studio | Hedgehog+ (2024.1+) |
| JDK | 11+ |
| Android SDK | 24–36 (API Level 24+) |
| Gradle | 8+ |
| TFLite | via `org.tensorflow:tensorflow-lite` |
| ONNX Runtime | `ai.onnxruntime:onnxruntime-mobile` |
| Room | `androidx.room:room-*` |
| CameraX | `androidx.camera:camera-*` |
| Glide | `com.github.bumptech.glide:glide` |

**模型导出：**
- YOLO-World v2 → TFLite（三步，Windows 可用）：
  1. `model.set_classes([...])` 用 CLIP 将 228 词表嵌入烘焙进分类头权重（词表 = LVIS 1203 类 ∩ CMU 发音词典 ∪ COCO 80，保证每个词都能被发音模块评估）
  2. `ultralytics` 导出 ONNX（该版本 `litert` 通道仅限 Linux/macOS，故绕行 ONNX）
  3. `onnx2tf` 转 saved_model 后做 **动态范围量化**（dynamic range quantization）
  > ⚠️ 实验结论：对本模型尝试过全部静态全整型量化方案（TF 转换器 per-tensor/per-channel、onnx2tf per-tensor/per-channel），分类头得分全部归零——CLIP 烘焙头的 logits 动态范围超出静态激活量化能力。动态量化是唯一可用方案，精度损失可忽略（bus 检测分 0.837 vs FP32 0.82+）。
- Wav2Vec2 → ONNX：`transformers.onnx` 导出，再用 ONNX Runtime INT8 动态量化

---

## 参考

- YOLOv8: Bochkovskiy et al., arXiv 2020
- YOLO-World: Cheng et al., arXiv 2024（开放词表检测，CLIP 重参数化）
- LVIS: Gupta et al., CVPR 2019（1203 类长尾词表）
- Wav2Vec2: Baevski et al., NeurIPS 2020
- ARIELLE: Man et al., IEEE APCCAS 2024
