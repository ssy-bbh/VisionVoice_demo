# VisionVoice 项目核心技术与架构实现文档
# VisionVoice Project: Core Technologies & Architecture Implementation Documentation

**文档版本 / Document Version：** v1.0  
**项目名称 / Project Name：** VisionVoice — AR英语智能词汇学习系统  
**撰写日期 / Date：** 2026-03-23  
**适用场景 / For：** 毕业设计答辩 / 学术论文参考 / 导师汇报  

---

## 一、项目整体架构设计 / System Architecture Overview

### 1.1 分层架构 — MVVM + Clean Architecture

VisionVoice 项目采用经典的三层分层架构，同时引入现代 Android 最佳实践，形成清晰的单向数据流：

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Presentation)              │
│  RealtimeActivity │ PracticeActivity │ CollectionFragment │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ OverlayView  │  │ BottomSheet  │  │ RecyclerView   │ │
│  │ (Canvas绘制) │  │ (评分反馈)   │  │ (展示柜渲染)   │ │
│  └──────────────┘  └──────────────┘  └────────────────┘ │
│              ▲ 回调 + LiveData/Handler  ▲               │
├─────────────────────────────────────────────────────────┤
│                    ML Layer (Business Logic)             │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │ YoloDetector │  │ Wav2Vec2Scorer │  │AudioProcess│  │
│  │   (视觉)     │  │    (听觉)       │  │  (音频)     │  │
│  │  TFLite/640² │  │  ONNX/16kHz PCM│  │ MediaCodec │  │
│  └──────────────┘  └────────────────┘  └────────────┘  │
│                  ▲ 数据传递      ▲ 评分结果              │
├─────────────────────────────────────────────────────────┤
│                    Data Layer (Persistence)             │
│  ┌─────────────────┐   ┌─────────────────────────────┐   │
│  │  PhonemeCache   │   │     Room Database (待实现)  │   │
│  │ (SharedPreferences│  │ PracticeRecord │ShowcaseItem│   │
│  │  + CMU Dict)    │   │  (遗忘曲线+打卡机制)        │   │
│  └─────────────────┘   └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**架构约束 / Architectural Constraints：**

| 层次 | 职责 | 核心技术 | 线程要求 |
|------|------|----------|----------|
| UI Layer | 渲染、交互、反馈展示 | ViewBinding / Canvas API | 主线程（Android UI Thread） |
| ML Layer | 模型推理、信号处理、算法运算 | TFLite / ONNX Runtime / MediaCodec | 独立后台线程（CameraX / Dedicated Executor） |
| Data Layer | 词典查询、音素缓存、数据持久化 | JSONObject / CountDownLatch / Room | 非UI线程 |

### 1.2 跨层数据流转机制 / Cross-Layer Data Flow

整体数据流遵循 **感知 → 认知 → 反馈** 三段式：

```
[CameraX 硬件帧]
    ↓ ImageAnalysis (RGBA_8888, STRATEGY_KEEP_ONLY_LATEST)
[RealtimeActivity Executor Thread]
    ↓ Bitmap + Rotation Matrix
[YoloDetector.detect(bitmap)]  ← TFLite 后台线程
    ↓ List<YoloDetector.Result>
[OverlayView.setResults()]    ← runOnUiThread()
    ↓ 用户点击触发
[PracticeActivity]
    ↓ MediaRecorder MIC → .m4a
[AudioProcessor.loadAndPreprocess()]  ← AAC→PCM 解码线程
    ↓ float[] PCM (16kHz, [-1,1])
[Wav2Vec2Scorer.score()]      ← 独立推理线程
    ↓ PronunciationScore
[updateUIWithFeedback()]      ← runOnUiThread()
```

**关键工程决策：** 所有 ML 推理严格与 UI 线程解耦。通过 `runOnUiThread()` / `Handler.post()` 确保数据安全回传；同时 `CameraX STRATEGY_KEEP_ONLY_LATEST` 策略防止帧堆积，确保实时响应性。

---

## 二、核心模块一：基于 YOLO 的 AR 视觉流处理
## Module I: YOLO-Based AR Visual Stream Processing

**实现文件 / Source Files：**
- `ui/ar/RealtimeActivity.java`
- `ml/YoloDetector.kt`
- `view/OverlayView.java`

### 2.1 CameraX 帧获取与旋转校正

系统使用 CameraX 的 `ImageAnalysis` 用例作为视觉输入管道，相比传统的 `Camera2 API`，CameraX 大幅简化了生命周期管理与分辨率适配工作。

**核心配置代码（`RealtimeActivity.java` 第 113–128 行）：**

```java
ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
    .setResolutionSelector(resolutionSelector)          // 请求 16:9 画面
    .setBackpressureStrategy(
        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)        // ⚡ 关键：只保留最新帧
    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    .build();
```

**旋转校正逻辑（第 130–136 行）：**

```java
Bitmap bitmap = imageProxy.toBitmap();
int rotation = imageProxy.getImageInfo().getRotationDegrees();
if (rotation != 0) {
    Matrix matrix = new Matrix();
    matrix.postRotate(rotation);
    bitmap = Bitmap.createBitmap(bitmap, 0, 0,
        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
}
latestBitmap = bitmap; // 旋转后的最新帧存入共享变量
```

> **工程要点：** CameraX 的 `analysis` 分辨率与 `display` 分辨率可能不同，必须使用 `imageProxy.getImageInfo().getRotationDegrees()` 进行旋转变换，而非依赖 `Display.getRotation()`。本系统通过 Bitmap 旋转矩阵在后台线程完成校正后，将结果存入 `latestBitmap`，供用户点击时传递给 `PracticeActivity`。

### 2.2 YoloDetector 推理引擎核心实现

`YoloDetector` 以 Kotlin 实现，核心解决了移动端 YOLO 部署的多个工程难题：

#### 2.2.1 内存预分配 — 消除 GC 卡顿

YOLO 检测需在每帧（约 15–30 FPS）反复分配张量缓冲区。传统实现每帧 `new ByteBuffer()` 会触发频繁 GC，造成 UI 抖动。系统在 `initInterpreter()` 中一次性分配 `DirectBuffer`：

```kotlin
// initInterpreter() 中一次性分配（只执行一次）
val bytesPerChannel = if (isInt8) 1 else 4
inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * bytesPerChannel)
    .order(ByteOrder.nativeOrder())
outputBuffer = ByteBuffer.allocateDirect(1 * outputChannels * outputAnchors * 4)
    .order(ByteOrder.nativeOrder())
intValues = IntArray(inputSize * inputSize)  // 复用像素数组
```

> **性能收益：** `allocateDirect()` 绕过 JVM 堆，直接映射 OS 物理内存；每帧仅重置 `buffer.rewind()` 指针，GC 压力降为零。

#### 2.2.2 模型格式自动探测

YOLOv8 支持多种导出格式（TFLite FP32 / INT8 / 标准 NCHW / NHWC），系统通过运行时探测自动适配，代码零配置：

```kotlin
// 自动判断 NCHW (PyTorch导出) vs NHWC (标准TFLite)
if (inputShape[1] == 3) { isModelNCHW = true }
// 自动判断量化模型
if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) { isInt8 = true }
// 自动判断输出张量是否需要转置
if (shape[1] < shape[2]) { isOutputTransposed = true }
```

#### 2.2.3 全屏拉伸输入策略

系统采用 **Stretch Mode**（直接拉伸而非 Letterbox）将摄像头帧缩放至 640×640：

```kotlin
val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
```

> **权衡分析：** Letterbox 方式保持宽高比但浪费像素（黑边区域无信息）；Stretch 方式利用全屏像素提升"沉浸式"识别体验，尽管轻微形变，但 YOLOv8 对此鲁棒性较强。

#### 2.2.4 归一化坐标体系 — 跨分辨率一致性

YOLO 输出的坐标经过两次归一化变换，确保无论输入分辨率如何，模型输出始终映射到 **[0,1]** 的统一空间：

```kotlin
// 第一次：像素级/归一化 → 统一像素级
val cx = if (rawW <= 1.0f) rawCx * inputSize else rawCx
val cy = if (rawH <= 1.0f) rawCy * inputSize else rawCy

// 第二次：像素级 → 归一化（对应屏幕百分比）
val rect = RectF(
    (cx - w / 2f) / inputSize,   // left
    (cy - h / 2f) / inputSize,   // top
    (cx + w / 2f) / inputSize,   // right
    (cy + h / 2f) / inputSize    // bottom
)
```

这一 `[0,1]` 归一化坐标最终直接传递给 `OverlayView`，由 `onDraw()` 按屏幕实际像素动态还原，实现跨设备一致性。

### 2.3 NMS 后处理与点击分发

**非极大值抑制（NMS）** 用于去除重叠检测框，核心逻辑（`YoloDetector.kt`）为：

```kotlin
boxes.sortWith { o1, o2 -> o2.score.compareTo(o1.score) }  // 按置信度降序
for (i in boxes.indices) {
    if (isSuppressed[i]) continue
    selectedBoxes.add(boxes[i])
    for (j in i + 1 until boxes.size) {
        if (calculateIoU(boxes[i].rect, boxes[j].rect) > threshold) {
            isSuppressed[j] = true  // 抑制重叠框
        }
    }
}
```

用户点击事件的处理在 `OverlayView.onTouchEvent()` 中实现（倒序遍历优先响应上层框）：

```kotlin
for (int i = results.size() - 1; i >= 0; i--) {
    // 还原归一化坐标至屏幕像素
    float left = normalizedRect.left * width;
    // 判断触摸点是否落在框内（扩大点击热区）
    if (x >= left && x <= right && y >= top && y <= bottom) {
        listener.onBoxClick(result);  // 触发跳转至 PracticeActivity
    }
}
```

---

## 三、核心模块二：基于 Wav2Vec2 的声学诊断引擎
## Module II: Wav2Vec2-Based Acoustic Diagnostic Engine

**实现文件 / Source Files：**
- `ui/practice/PracticeActivity.java`
- `ml/Wav2Vec2Scorer.java`
- `ml/AudioProcessor.java`
- `ml/PhonemeCache.java`

### 3.1 音频采集与预处理流水线

#### 3.1.1 MediaRecorder 录音（M4A 格式）

系统使用 `MediaRecorder.AudioSource.MIC` 采集原始音频，输出为 MPEG-4/AAC 格式（.m4a）：

```java
mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
mediaRecorder.setOutputFile(audioFilePath);  // /data/user/.../cache/user_record.m4a
```

> **AAC 压缩必要性：** 原始 PCM 16-bit / 16kHz / Mono 每秒约 32KB；AAC 压缩后可降至约 1–2KB/s，大幅降低存储与 I/O 开销。

#### 3.1.2 AAC→PCM 解码（MediaCodec）

**痛点：** Android 无法直接读取 `.m4a` 作为 PCM 数据。系统使用 `MediaCodec` 硬解码器实现零依赖解码：

```java
MediaCodec codec = MediaCodec.createDecoderByType(mime);  // "audio/mp4a-latm"
codec.configure(format, null, null, 0);
codec.start();
// EOS 信号触发结束解码
codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
```

关键坑点：`MediaCodec.BufferInfo` 中的 `offset` 和 `size` 必须严格遵守，否则解码数据会错位：

```java
outputBuf.position(bufferInfo.offset);
outputBuf.limit(bufferInfo.offset + bufferInfo.size);
byte[] chunk = new byte[bufferInfo.size];
outputBuf.get(chunk);  // 精确读取解码数据段
```

#### 3.1.3 重采样与归一化

录音设备采样率可能为 44.1kHz / 48kHz，Wav2Vec2 模型要求固定 16kHz。系统实现线性插值重采样：

```java
float ratio = (float) fromRate / toRate;  // e.g. 44100/16000 = 2.756
int newLength = (int)(src.length / ratio);
for (int i = 0; i < newLength; i++) {
    float pos = i * ratio;
    int idx = (int) pos;
    float frac = pos - idx;
    result[i] = src[idx] * (1 - frac) + src[idx + 1] * frac;  // 线性插值
}
```

**静音检测（噪声门限）：**

```java
if (maxAmp < 0.05f) {
    // 峰值振幅 < 5%，判定为静音，返回 null，触发 Toast 提示
    return true;
}
```

### 3.2 双数据源归一化机制 — 核心架构突破

#### 3.2.1 问题根源：异构符号域冲突

项目最核心的工程难题在于：两套数据源使用了不同的音标表示体系（Symbolic Domain Mismatch）：

| 数据路径 | 符号体系 | 示例 | 技术规范 |
|----------|----------|------|----------|
| 基准音素（CMU Dict / 后端 phonemizer） | IPA（Unicode 国际音标） | `æ`, `ɹ`, `ɔ`, `i` | 语言学家规范，高精度 |
| Wav2Vec2 模型输出（`vitouphy/wav2vec2-xls-r-300m-phoneme`） | Arpabet（ASCII 字节码） | `ae`, `r`, `ao`, `iy` | 深度学习标注规范 |

这导致即使用户发音完全正确，`"ɹ".equals("r")` 在 Java 中返回 `false`，系统误判为 Substitution 错读，产生大量 False Negatives。

#### 3.2.2 解决方案：双层翻译状态机

系统引入两级翻译函数，彻底解耦比对逻辑与渲染逻辑：

**第一层 — 底层降维（归一化，比对前执行）：**

```java
private String normalizeForCompare(String p) {
    Map<String, String> ipaToArpabet = new HashMap<>() {{
        put("ɑ", "aa"); put("æ", "ae"); put("ɹ", "r");
        put("ɔ", "ao"); put("i", "iy"); put("ɪ", "ih");
        put("ʃ", "sh"); put("θ", "th"); put("tʃ", "ch");
        put("dʒ", "jh"); put("ŋ", "ng"); put("ʒ", "zh");
        // ... 共覆盖 25+ IPA-Arpabet 映射对
    }};
    String s = p.toLowerCase().replaceAll("\\d", "").trim();
    return ipaToArpabet.containsKey(s) ? ipaToArpabet.get(s) : s;
}
```

**第二层 — 表层升维（标准化，渲染前执行）：**

```java
private String toStandardIPA(String p) {
    String s = normalizeForCompare(p);  // 先降维清理
    Map<String, String> arpabetToIpa = new HashMap<>() {{
        put("aa", "ɑ"); put("ae", "æ"); put("r", "ɹ");
        put("ao", "ɔ"); put("iy", "i"); put("ih", "ɪ");
        // ... 镜像映射
    }};
    return arpabetToIpa.containsKey(s) ? arpabetToIpa.get(s) : s;
}
```

> **架构价值：** 这一设计将"比对平面"（Arpabet）与"展示平面"（IPA）彻底分离。底层 DP 算法始终在统一的 Arpabet 空间运算，最终回传给 UI 的 `referencePhonemes` / `userPhonemes` 均为标准 IPA，保证用户看到的音标格式统一且专业。

### 3.3 Needleman-Wunsch 全局序列对齐

#### 3.3.1 算法原理

用户发音长度（帧数）与标准音素数量（音节数）天然不一致（吞音、多读）。传统的逐位比较失效，系统引入生物信息学中的 **Needleman-Wunsch 全局动态规划对齐算法**：

```java
float[][] dp = new float[n + 1][m + 1];
float MATCH = 1.0f, FLAW = 0.5f, MISMATCH = -1.0f, GAP = -1.0f;

// 初始化边界（空序列惩罚）
for (int i = 0; i <= n; i++) dp[i][0] = i * GAP;
for (int j = 0; j <= m; j++) dp[0][j] = j * GAP;

// DP 递推
for (int i = 1; i <= n; i++) {
    for (int j = 1; j <= m; j++) {
        String err = getErrorType(ref.get(i-1), user.get(j-1));
        float s = (err.equals("Match") || err.equals("Ignored")) ? MATCH
               : (err.startsWith("Flaw:") ? FLAW : MISMATCH);
        dp[i][j] = Math.max(
            dp[i-1][j-1] + s,                      // 对角（匹配/替换）
            Math.max(dp[i-1][j] + GAP,             // 上方（漏读 Deletion）
                    dp[i][j-1] + GAP));            // 左方（多读 Insertion）
    }
}
```

回溯阶段（`Wav2Vec2Scorer.java` 第 95–115 行）生成对齐序列：

```java
while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && dp[i][j] == dp[i-1][j-1] + s) {
        aRef.add(ref.get(i-1)); aUser.add(user.get(j-1));
        fb.add(err.equals("Ignored") ? "Match" : err);
        i--; j--;
    } else if (i > 0 && dp[i][j] == dp[i-1][j] + GAP) {
        aRef.add(ref.get(i-1)); aUser.add("-"); fb.add("Deletion"); i--;
    } else {
        aRef.add("-"); aUser.add(user.get(j-1)); fb.add("Insertion"); j--;
    }
}
```

#### 3.3.2 非线性打分曲线

基于第二语言习得（SLA）教学心理学，分段映射确保分数分布合理：

```
准确率 ≥ 0.8  →  [90, 100] 分（优秀区，扩大鼓励）
0.5 ≤ 准确率 < 0.8  →  [60, 90) 分（进步区，敏感反馈）
准确率 < 0.5  →  [0, 60) 分（薄弱区，严格判定）
```

### 3.4 三级声学容错矩阵 — SLA 理论与工程实践

#### 3.4.1 清浊音容忍（Plosive Puff Effect 补偿）

**物理现象：** 清塞音 `/k/`, `/p/`, `/t/` 发音时，高压气流冲击手机麦克风振膜，产生非线性物理失真，导致模型将 `/k/` 误识别为 `/hh/`（哈气声）。

**工程补偿（`Wav2Vec2Scorer.java` `getErrorType()`）：**

```java
String[][] ignored = {
    {"k", "hh"}, {"k", "h"},    // 清塞音气流干扰豁免
    {"p", "hh"}, {"t", "hh"},  // 同上
    {"t", "d"}, {"p", "b"},    // 清浊音混淆（对中国学生宽容）
    {"iy", "ih"}, {"ih", "iy"} // 元音长短偏差（不影响语义）
};
```

#### 3.4.2 分级反馈体系

| 等级 | 判定条件 | 分数 | 反馈语义 | 学习心理学依据 |
|------|----------|------|----------|----------------|
| **完全匹配（Match）** | 音素精确一致 | 1.0 | ✅ 绿色 | Intrinsic Feedback，正强化 |
| **完全豁免（Ignored）** | SLA 容忍范围内的偏差 | 1.0 | ✅ 绿色 | Accentedness 豁免，保护自信心 |
| **发音瑕疵（Flaw）** | 教学重点偏差（如 r/l 不分） | 0.6 | 🟠 橙色 + AI诊断说明 | Comprehensible Output，渐进修正 |
| **错读（Substitution）** | 语义破坏性替换 | 0.0 | ❌ 红色 | Error Correction，需重点练习 |

---

## 四、核心模块三：游戏化展示柜与数据持久化系统设计
## Module III: Gamified Showcase & Data Persistence System Design
### （系统架构设计方案 — 尚未编写 Java 代码 / Architecture Design — Code Pending）

### 4.1 系统设计理念

传统单词本以"列表"形式存储，抽象且缺乏激励性。VisionVoice 将其重构为 **"打卡 + 虚拟展示柜"** 双轨机制：

- **打卡轨（Practice Records）：** 记录每一次发音练习的流水，不可修改，用于溯源与统计。
- **展示柜轨（Showcase Items）：** 将单词封装为"虚拟展品"，具象化学习成就，并内置遗忘曲线复习提醒。

理论基础：**艾宾浩斯遗忘曲线（Ebbinghaus Forgetting Curve）** 表明，有意义的信息若不复习，记忆保留率随时间指数衰减。通过设计"展品蒙尘"机制，激励用户在真实场景中重新扫描物体，实现"具身认知（Embodied Cognition）"式的主动复习。

### 4.2 Room 数据库核心数据模型

#### 4.2.1 练习记录表 — 流水维度

```
PracticeRecord (Entity)
├── id: Long (Primary Key, autoGenerate = true)
├── word: String           — 目标单词（来源：YOLO 识别结果）
├── score: Int             — 发音得分（0-100，Wav2Vec2Scorer 返回值）
├── timestamp: Long        — 练习完成时间戳（epoch ms）
└── imagePath: String      — 实景截图路径（`latestBitmap` 快照）
```

**约束：** 此表为append-only 流水表（Immutable Log），禁止 UPDATE 操作，仅允许 INSERT 与按条件 SELECT。这保证了学习记录的不可篡改性，为后续统计分析提供可信数据源。

#### 4.2.2 展示柜成就表 — 成就维度

```
ShowcaseItem (Entity)
├── id: Long (Primary Key)
├── targetWord: String     — 目标单词（唯一约束）
├── category: String       — 场景分类（"Office" / "Kitchen" / "Nature" 等）
├── isUnlocked: Boolean    — 是否在真实世界中被扫描解锁
├── unlockTime: Long       — 首次解锁时间戳（用于计算"收藏时长"）
├── bestImagePath: String  — 最高分练习对应的实景图片（展柜高亮渲染）
├── highestScore: Int      — 历史最高发音得分（用于展品等级评定）
└── lastReviewedTime: Long — 【遗忘曲线核心】最后成功练习时间戳
```

### 4.3 核心业务流程（Architecture Design）

#### 4.3.1 练习结算流程（Practice Settlement Flow）

```
[发音评估完成，PronunciationScore 返回]
         │
         ▼
[事务 Transaction]
  ┌──────────────────────────────────────┐
  │ 1. 写入 PracticeRecord（流水）        │
  │    INSERT INTO practice_records ...   │
  │                                      │
  │ 2. 查询 ShowcaseItem（是否存在该词）  │
  │    SELECT * FROM showcase_items      │
  │       WHERE targetWord = ?            │
  │                                      │
  │ 3A. 不存在 → INSERT 新展品            │
  │      isUnlocked = true               │
  │      unlockTime = NOW()              │
  │      highestScore = score            │
  │      bestImagePath = imagePath        │
  │      lastReviewedTime = NOW()        │
  │                                      │
  │ 3B. 存在但未解锁 → UPDATE 解锁        │
  │      isUnlocked = true               │
  │                                      │
  │ 3C. 已解锁 → 两个分支:                 │
  │      IF score > highestScore:        │
  │          UPDATE highestScore,        │
  │          UPDATE bestImagePath        │
  │      ALWAYS:                         │
  │          UPDATE lastReviewedTime     │
  └──────────────────────────────────────┘
```

#### 4.3.2 每日打卡溯源算法

不额外维护"打卡状态表"，基于流水表动态计算：

```kotlin
// 伪代码：展示柜渲染时调用
fun getStreakCount(userId: Long): Int {
    var streak = 0
    var currentDate = LocalDate.now().minusDays(1)  // 从昨天开始

    while (true) {
        val startOfDay = currentDate.atStartOfDay().toEpochMilli()
        val endOfDay   = currentDate.plusDays(1).atStartOfDay().toEpochMilli()

        val count = dao.countPracticeRecords(userId, startOfDay, endOfDay)
        if (count > 0) {
            streak++
            currentDate = currentDate.minusDays(1)
        } else {
            break  // 遇到第一个空白日，停止
        }
    }
    return streak
}
```

#### 4.3.3 "蒙尘"遗忘曲线机制

**数据结构预留：** `lastReviewedTime` 字段作为遗忘曲线的时间基准。

**蒙尘判定逻辑（CollectionFragment 渲染时执行）：**

```kotlin
// 遗忘阈值（可配置，默认 3 天）
val DUST_THRESHOLD_MS = 3 * 24 * 60 * 60 * 1000L

val timeSinceLastReview = System.currentTimeMillis() - item.lastReviewedTime
val isDusty = timeSinceLastReview > DUST_THRESHOLD_MS

// UI 渲染策略：
if (!item.isUnlocked) {
    // 渲染占位剪影 + 锁定图标（灰色）
} else if (isDusty) {
    // 渲染 bestImagePath + 灰度遮罩 + 灰尘图标
    // 视觉提示："该展品已蒙尘，请重新扫描以擦亮！"
} else {
    // 明亮渲染 + 最高分角标 + "NEW" 标签（如 unlockTime 距今 < 24h）
}
```

**复习触发机制（Spaced Repetition）：**

用户重新在真实场景中扫描该物体 → 触发 `PracticeActivity` → 结算时将 `lastReviewedTime` 刷新为当前时间 → 下次访问 Collection 时展品恢复明亮状态。

> **设计哲学：** 传统的 App 内"提醒复习"机制依赖用户主动打开 App；而 VisionVoice 的"蒙尘"机制要求用户**主动在物理世界中寻找物体**才能擦亮展品，将数字激励与现实行动强绑定，实现真正的 AR 增强学习体验。

### 4.4 架构分层设计

```
Data Layer (com.example.myapplication.data)
├── AppDatabase (单例，管理所有 Room DAO)
├── PracticeRecord (Entity, @Entity(tableName = "practice_records"))
├── ShowcaseItem  (Entity, @Entity(tableName = "showcase_items",
│                  indices = [Index(value = ["targetWord"], unique = true)]))
└── AppDao (Data Access Object)
    ├── insertPracticeRecord(record: PracticeRecord)
    ├── queryByDateRange(userId, startMs, endMs): List<PracticeRecord>
    ├── getStreakCount(userId): Int  // 动态计算连续打卡天数
    ├── upsertShowcaseItem(item: ShowcaseItem)  // 原子性插入或更新
    └── queryByCategory(category): List<ShowcaseItem>

UI Layer (com.example.myapplication.ui.collection)
├── CollectionFragment
│   ├── TabLayout (按 category 分组: Office / Kitchen / Animals ...)
│   ├── RecyclerView (GridLayoutManager, 3列)
│   │   └── ShowcaseAdapter → ShowcaseViewHolder
│   │       ├── 未解锁：SilhouetteCardView（剪影 + Lock图标）
│   │       ├── 明亮卡片：ImageCardView + ScoreBadge
│   │       └── 蒙尘卡片：ImageCardView + GrayMaskOverlay + DustIcon
│   └── CalendarView (底部：打卡日历，标记打卡日)
└── ViewModel (PracticeHistoryViewModel)
    ├── practiceRecords: LiveData<List<PracticeRecord>>
    ├── showcaseItems: LiveData<Map<String, List<ShowcaseItem>>>
    └── currentStreak: LiveData<Int>
```

---

## 五、关键技术指标总结 / Key Technical Metrics Summary

| 维度 | 指标项 | 数值 | 技术依据 |
|------|--------|------|----------|
| **视觉** | YOLO 模型大小 | 12.5 MB | YOLOv8n TFLite FP32 |
| **视觉** | 识别类别数 | 40 类 | COCO 子集 |
| **视觉** | 输入分辨率 | 640×640 | YOLO 最佳精度/速度比 |
| **视觉** | Java 堆占用 | ≈0 MB | `MappedByteBuffer` OS 级 mmap |
| **视觉** | 推理时间 | ~50 ms/帧 | TFLite 4线程 |
| **听觉** | Wav2Vec2 模型 | ~360 MB (FP32) / ~300 MB (INT8 量化) | `vitouphy/wav2vec2-xls-r-300m-phoneme` |
| **听觉** | 端侧推理耗时 | ~321 ms (1秒音频) | EXPERIMENT.md 2026-03-17 |
| **听觉** | 音频采样率 | 16 kHz | Wav2Vec2 模型要求 |
| **听觉** | 静音门限 | 峰值振幅 < 5% | 物理抗噪阈值 |
| **算法** | Needleman-Wunsch 权重 | MATCH=1.0 / FLAW=0.5 / MISMATCH=-1.0 / GAP=-1.0 | 优先对齐正确音节 |
| **算法** | IPA ↔ Arpabet 映射 | 25+ 对映射 | 双数据源归一化核心 |
| **系统** | Android 兼容版本 | API 24 (Android 7.0) ~ API 34 | CameraX / ML Kit 最低要求 |
| **系统** | 离线支持 | ✅ 已实现 | ONNX Runtime + CMU Dict |

---

## 六、核心工程难题与解决方案汇总
## Summary of Core Engineering Challenges & Solutions

| # | 难题名称 | 现象 | 根因 | 解决方案 |
|---|----------|------|------|----------|
| 1 | IPA-Arpabet 异构冲突 | 正确发音被判为错读 | CMU Dict→IPA 与 Wav2Vec2→Arpabet 符号域不同 | 双层翻译状态机（降维比对 + 升维渲染） |
| 2 | 清塞音物理失真 | `/k/` `/p/` `/t/` 误识别为 `/hh/` | 麦克风振膜被高压气流冲击 | SLA 容错矩阵：硬编码 k→hh、p→hh 豁免路径 |
| 3 | YOLO 置信度全零 | 所有物体识别置信度为 0 | 输入预处理缺少归一化（除以 255） | 添加 `NormalizeOp(0f, 255f)` |
| 4 | 多模型并发 OOM | 同时加载 YOLO+Wav2Vec2 崩溃 | 360MB 模型用 `byte[]` 占用 Java Heap | TFLite 用 `MappedByteBuffer`，ONNX 复制到 FilesDir 后用 `mmap` |
| 5 | TFLite 非线程安全 | CameraX 推送帧时 UI 卡死 | `Interpreter` 在 CameraX 后台线程被并发调用 | 专用单线程 `ExecutorService` + `STRATEGY_KEEP_ONLY_LATEST` |
| 6 | NCHW/NHWC 格式歧义 | PyTorch 导出 vs 标准 TFLite 输出不一致 | 不同导出工具的张量排列不同 | 运行时探测 `inputShape[1]==3` 和 `outputShape[1]<outputShape[2]` |
| 7 | CMU Dict 加载阻塞 | 首次查询音标时 UI 卡顿 | `loadCmuDict()` 在主线程执行 | `CountDownLatch` 后台加载 + 最多 3s 等待 + 运行时缓存兜底 |
| 8 | AIGC 幻觉音素 | 用户未发声时解码出无意义音素碎片 | Wav2Vec2 对静音白噪声的误判 | 前处理阶段峰值振幅门限 < 0.015 强制拦截 |

---

## 七、文档参考文献 / References

1. Hochreiter, S. & Schmidhuber, J. (1997). Long Short-Term Memory. *Neural Computation*, 9(8).
2. Graves, A. et al. (2006). Connectionist Temporal Classification: Labelling Unsegmented Sequence Data with Recurrent Neural Networks.
3. Munro, M. J. & Derwing, T. M. (1995). Processing Time, Accent, and Comprehensibility in the Perception of Native and Foreign-Accented Speech. *Language and Speech*.
4. Ultralytics (2023). YOLOv8 Documentation. *https://docs.ultralytics.com/*
5. Baevski, A. et al. (2020). wav2vec 2.0: A Framework for Self-Supervised Learning of Speech Representations. *NeurIPS 2020*.
6. Ebbinghaus, H. (1885). *Über das Gedächtnis* (Memory: A Contribution to Experimental Psychology).
7. VisionVoice 项目源码：`app/src/main/java/com/example/myapplication/`
8. VisionVoice 核心算法文档：`docs/logs/PHONEME_ALGORITHM.md`
9. VisionVoice 展示柜设计：`app/docs/requirements/collection/SHOWCASE_SYSTEM_DESIGN.md`

---