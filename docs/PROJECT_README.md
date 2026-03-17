# VisionVoice 项目文档

**项目名称：** VisionVoice - AR 英语学习助手  
**创建日期：** 2026-03-17  
**最后更新：** 2026-03-17  
**版本：** v1.0  

---

## 📱 项目概述

VisionVoice 是一款基于 AR 和 AI 的英语学习应用，通过计算机视觉和语音识别技术，帮助用户在真实环境中学习物体英文名称并练习发音。

### 核心功能
1. **实时 AR 物体识别** - 摄像头扫描周围物体，显示英文名称
2. **拍照识别** - 上传照片进行物体识别
3. **发音练习** - 针对识别的单词进行发音训练和评分
4. **单词收藏** - 收藏学习过的单词
5. **个人中心** - 用户设置和学习统计

---

## 🏗️ 技术架构

### 前端（Android）
- **开发语言：** Java
- **最低 SDK：** 24 (Android 7.0)
- **目标 SDK：** 34 (Android 14)
- **编译 SDK：** 36

### 机器学习
- **物体检测：** YOLOv8n (TensorFlow Lite)
- **发音评估：** Wav2Vec2 (ONNX Runtime Mobile) - 🆕 端侧部署中

### 后端（可选）
- **框架：** Python FastAPI
- **发音评分：** Wav2Vec2 + 音素对齐算法
- **部署方式：** 本地服务器 / 云端

---

## 📂 项目结构

```
D:\AndroidStudioProjects\MyApplication\
├── app/                          # Android 主模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── ml/                      # 机器学习模块
│   │   │   │   │   ├── ObjectRecognitionHelper.java   # YOLO 物体识别
│   │   │   │   │   ├── Wav2Vec2Scorer.java            # 🆕 端侧发音评分器
│   │   │   │   │   └── AudioProcessor.java            # TODO: 音频预处理
│   │   │   │   ├── ui/                        # UI 界面
│   │   │   │   │   ├── main/                  # 主界面
│   │   │   │   │   │   └── MainActivity.java
│   │   │   │   │   ├── home/                  # 首页
│   │   │   │   │   │   └── HomeFragment.java
│   │   │   │   │   ├── ar/                    # AR 实时识别
│   │   │   │   │   │   └── RealtimeActivity.java
│   │   │   │   │   ├── photo/                 # 拍照识别
│   │   │   │   │   │   └── PhotoRecognitionActivity.java
│   │   │   │   │   ├── practice/              # 发音练习
│   │   │   │   │   │   └── PracticeActivity.java
│   │   │   │   │   ├── collection/            # 单词收藏
│   │   │   │   │   │   └── CollectionFragment.java
│   │   │   │   │   └── profile/               # 个人中心
│   │   │   │   │       └── ProfileFragment.java
│   │   │   │   └── view/                      # 自定义视图
│   │   │   │       ├── OverlayView.java       # AR 覆盖层（绿框）
│   │   │   │       └── FocusBoxView.java      # 聚焦框
│   │   │   ├── res/                           # 资源文件
│   │   │   ├── assets/                        # 模型文件
│   │   │   │   ├── yolov8n.tflite            # YOLO 模型
│   │   │   │   ├── labels.txt                 # 标签文件
│   │   │   │   └── onnx/                      # 🆕 Wav2Vec2 模型目录
│   │   │   │       ├── model.onnx             # FP32 模型
│   │   │   │       └── model_quant.onnx       # INT8 量化模型
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                       # 仪器测试
│   │   └── test/                              # 单元测试
│   └── build.gradle.kts                       # 模块级构建配置
├── backend/                        # Python 后端服务
│   ├── server.py                   # FastAPI 服务器
│   └── export_onnx.py              # 🆕 ONNX 模型导出脚本
├── docs/                           # 🆕 项目文档
│   ├── PROJECT_README.md           # 本文档
│   ├── QUICK_START.md              # 端侧部署快速指南
│   ├── WAV2VEC2_ONNX_GUIDE.md      # Wav2Vec2 完整指南
│   └── CHANGELOG.md                # 🆕 变更日志
├── build.gradle.kts                # 项目级构建配置
└── settings.gradle.kts             # 项目设置
```

---

## 🎯 功能模块详解

### 1. 首页 (HomeFragment)
**文件：** `ui/home/HomeFragment.java`

**功能：**
- 两个主要入口卡片：
  - **实时扫描** → 跳转到 `RealtimeActivity`
  - **拍照上传** → 跳转到 `PhotoRecognitionActivity`

**布局：** `res/layout/fragment_home.xml`

---

### 2. AR 实时识别 (RealtimeActivity) ⭐
**文件：** `ui/ar/RealtimeActivity.java`

**核心功能：**
- CameraX 实时摄像头预览
- YOLOv8n 物体检测（40 类物体）
- 绿框标注识别结果
- 点击绿框跳转到发音练习

**关键技术点：**
```java
// CameraX 配置 - 16:9 画面
ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
    .build();

// YOLO 检测 - 最多显示 3 个框
List<YoloDetector.Result> topResults = new ArrayList<>();
for (int i = 0; i < Math.min(3, results.size()); i++) {
    topResults.add(results.get(i));
}

// 点击绿框跳转
overlayView.setOnBoxClickListener(result -> {
    isResultLocked = true; // 锁定状态，防止数据刷新
    Intent intent = new Intent(RealtimeActivity.this, PracticeActivity.class);
    intent.putExtra("extra_word", detectedWord);
    startActivity(intent);
});
```

**性能优化：**
- 使用 `STRATEGY_KEEP_ONLY_LATEST` 避免帧堆积
- 后台线程执行 YOLO 推理
- 跳转时锁定扫描状态

---

### 3. 发音练习 (PracticeActivity) ⭐
**文件：** `ui/practice/PracticeActivity.java`

**核心功能：**
- 显示物体英文名称和国际音标
- TTS 发音示范（喇叭按钮）
- 录音功能（按住录音，松开停止）
- 发音评分（后端 API / 端侧 ONNX）
- 音素级对比反馈（红/黄/绿）

**评分流程：**
```
用户录音 (MPEG-4/AAC)
    ↓
音频预处理 (16kHz PCM)
    ↓
Wav2Vec2 转录 → 音素序列
    ↓
Needleman-Wunsch 对齐算法
    ↓
三级容错评分:
  - Match (绿色): 完全正确 → 100 分
  - Flaw (黄色): 发音瑕疵 → 60 分
  - Substitution (红色): 错读 → 0 分
    ↓
非线性打分曲线 → 最终得分
```

**后端 API（当前模式）：**
```python
POST http://127.0.0.1:8000/evaluate_pronunciation/
FormData: target_word, audio_file

Response:
{
  "reference_phonemes": ["k", "æ", "t"],
  "user_phonemes": ["k", "æ", "t"],
  "feedback": ["Match", "Match", "Match"]
}
```

**端侧模式（开发中）：**
```java
Wav2Vec2Scorer scorer = new Wav2Vec2Scorer(context);
float[] audioData = AudioProcessor.loadAndPreprocess(file);
PronunciationScore result = scorer.score(word, audioData);
```

---

### 4. 拍照识别 (PhotoRecognitionActivity)
**文件：** `ui/photo/PhotoRecognitionActivity.java`

**功能：** 从相册选择图片进行物体识别

---

### 5. 单词收藏 (CollectionFragment)
**文件：** `ui/collection/CollectionFragment.java`

**功能：** 查看和管理收藏的单词

---

### 6. 个人中心 (ProfileFragment)
**文件：** `ui/profile/ProfileFragment.java`

**功能：** 用户设置、学习统计、关于页面

---

## 🔧 自定义视图

### OverlayView
**文件：** `view/OverlayView.java`

**功能：**
- 在摄像头预览上绘制检测框
- 支持点击检测框
- 根据置信度显示不同颜色（绿色）

**绘制逻辑：**
```java
@Override
protected void onDraw(Canvas canvas) {
    for (Result result : results) {
        // 绘制矩形框
        canvas.drawRect(result.getBoundingBox(), boxPaint);
        // 绘制标签
        canvas.drawText(result.getLabel(), x, y, textPaint);
    }
}
```

---

### FocusBoxView
**文件：** `view/FocusBoxView.java`

**功能：** 屏幕中央的聚焦框，辅助用户对准物体

---

## 🤖 机器学习模块

### YOLO 物体检测
**文件：** `ml/ObjectRecognitionHelper.java`

**模型：** `yolov8n.tflite`  
**输入：** 640x640 RGB 图像  
**输出：** 8400 个检测结果（40 类物体）

**检测流程：**
```java
// 1. 图像预处理
ImageProcessor imageProcessor = new ImageProcessor.Builder()
    .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
    .add(new NormalizeOp(0f, 255f))
    .build();

// 2. TFLite 推理
tflite.run(byteBuffer, output);

// 3. 后处理 - 提取最佳结果
postProcess(output, callback, originalWidth, originalHeight);
```

**40 类物体：**
- 教室：Backpack, Book, Chair, Desk, Laptop...
- 家庭：Apple, Banana, Bed, Cup, Fish...
- 农村：Wok, Cabbage, Chicken, Lantern...
- 户外：Bicycle, Bird, Bus, Car, Umbrella...

---

### Wav2Vec2 发音评估 🆕
**文件：** `ml/Wav2Vec2Scorer.java`

**模型：** `facebook/wav2vec2-base` (ONNX 格式)  
**大小：** 95MB (FP32) → 25MB (INT8 量化)  
**输入：** 16kHz PCM 音频 (float32, 归一化)  
**输出：** 音素序列 + 评分

**部署方式：**
1. **后端模式（当前）** - Python FastAPI + Wav2Vec2
2. **端侧模式（开发中）** - ONNX Runtime Mobile

**集成步骤：** 详见 `docs/QUICK_START.md`

---

## 🌐 后端服务

### FastAPI 服务器
**文件：** `backend/server.py`

**API 接口：**

#### 1. 获取音标
```
GET /get_phonetics/?word=apple
Response: {"word": "apple", "phonetics": "/ˈæpəl/"}
```

#### 2. 发音评分
```
POST /evaluate_pronunciation/
FormData: target_word, audio_file
Response: {
  "reference_phonemes": [...],
  "user_phonemes": [...],
  "feedback": [...]
}
```

**核心算法：**
- **音素提取：** `phonemize` + espeak
- **音频转录：** Wav2Vec2 (LV-60-espeak)
- **音素对齐：** Needleman-Wunsch 算法
- **三级容错：**
  - Ignored: 硬件噪音误差（如 t→ts）→ 忽略
  - Flaw: 发音瑕疵（如 ɔ→o）→ 黄框警告
  - Substitution: 完全错读 → 红框错误

**启动服务器：**
```bash
cd backend
pip install fastapi uvicorn transformers torch torchaudio phonemizer
python server.py
# 访问：http://localhost:8000/docs
```

---

## 🆕 端侧部署计划

### 目标
将 Wav2Vec2 模型部署到 Android 设备，实现离线发音评估。

### 实施步骤
1. ✅ 创建导出脚本 (`backend/export_onnx.py`)
2. ✅ 创建端侧评分器 (`ml/Wav2Vec2Scorer.java`)
3. ⏳ 导出 ONNX 模型
4. ⏳ 集成 ONNX Runtime Mobile
5. ⏳ 创建音频处理器 (`ml/AudioProcessor.java`)
6. ⏳ 修改 PracticeActivity 支持端侧模式
7. ⏳ 测试和优化

### 性能预期
| 指标 | 预期值 |
|------|--------|
| 模型大小 | 25MB (INT8) |
| 推理时间 | 300-800ms |
| 内存占用 | ~200MB |
| 准确率 | 95-98% (vs 后端) |

**详细指南：** `docs/WAV2VEC2_ONNX_GUIDE.md`

---

## 📝 开发日志

### 2026-03-17 - AI 助手协助开发

#### 操作记录

**14:11** - 打开 Zotero 文献
- 找到 ARIELLE 论文（AR 语言学习系统）
- 论文路径：`C:\Users\Dell\Zotero\storage\X4FEQTEL\`
- 标题：ARIELLE: AR-based Independent and Experiential Language Learner on the Edge

**14:18** - 安装 PDF 阅读工具
- 创建目录：`D:\Tools\`
- 安装 PyPDF：`pip install pypdf --target "D:\Tools\python-packages"`
- 创建阅读脚本：`D:\Tools\read_pdf.py`

**14:23** - 阅读 ARIELLE 论文
- 读取前 3 页内容
- 总结核心设计：
  - YOLOv8n 物体检测（40 类物体）
  - Wav2Vec2 发音评估（Intel OpenVINO INT8）
  - 双分支评分（ASR + 信号处理）

**14:33** - 分析 VisionVoice 项目
- 找到项目位置：`D:\AndroidStudioProjects\MyApplication\`
- 查看后端代码：`backend/server.py`
- 查看 Android 代码：`PracticeActivity.java`, `RealtimeActivity.java`

**14:50** - 创建端侧部署文档
- 创建完整指南：`docs/WAV2VEC2_ONNX_GUIDE.md` (15KB)
- 创建快速指南：`docs/QUICK_START.md` (12KB)
- 创建导出脚本：`backend/export_onnx.py` (7KB)
- 创建评分器模板：`ml/Wav2Vec2Scorer.java` (11KB)

**15:00** - 整理项目结构
- 移动文档到 `docs/` 目录
- 移动 Java 文件到 `ml/` 目录
- 创建变更日志：`docs/CHANGELOG.md`
- 创建项目文档：`docs/PROJECT_README.md`（本文档）

#### 文件变更
```
新增:
  docs/PROJECT_README.md          # 项目主文档
  docs/QUICK_START.md             # 端侧部署快速指南
  docs/WAV2VEC2_ONNX_GUIDE.md     # Wav2Vec2 完整指南
  docs/CHANGELOG.md               # 变更日志
  backend/export_onnx.py          # ONNX 模型导出脚本
  ml/Wav2Vec2Scorer.java          # 端侧发音评分器

移动:
  QUICK_START.md → docs/QUICK_START.md
  WAV2VEC2_ONNX_GUIDE.md → docs/WAV2VEC2_ONNX_GUIDE.md
  Wav2Vec2Scorer.java → ml/Wav2Vec2Scorer.java
```

---

## 🚀 下一步计划

### 短期（1-2 天）
- [ ] 运行 `backend/export_onnx.py` 导出 ONNX 模型
- [ ] 在 `app/build.gradle.kts` 中添加 ONNX Runtime 依赖
- [ ] 创建 `AudioProcessor.java` 音频预处理类
- [ ] 测试端侧模型加载

### 中期（3-5 天）
- [ ] 完善 `Wav2Vec2Scorer.java` 音素词典
- [ ] 实现 Needleman-Wunsch 对齐算法
- [ ] 修改 `PracticeActivity` 支持混合模式
- [ ] 性能测试和优化

### 长期（1-2 周）
- [ ] 集成 CMU 发音词典
- [ ] 实现智能切换（端侧/后端）
- [ ] 用户测试和反馈收集
- [ ] 发布 Beta 版本

---

## 📚 参考资料

### 论文
- ARIELLE: AR-based Independent and Experiential Language Learner on the Edge (2024 IEEE APCCAS)

### 技术文档
- ONNX Runtime Android: https://onnxruntime.ai/docs/get-started/with-java.html
- Wav2Vec2: https://huggingface.co/facebook/wav2vec2-base
- YOLOv8: https://docs.ultralytics.com/

### 项目文档
- `docs/QUICK_START.md` - 端侧部署快速指南
- `docs/WAV2VEC2_ONNX_GUIDE.md` - Wav2Vec2 完整指南

---

## 👥 开发团队

**开发者：** VisionVoice Team  
**AI 助手：** OpenClaw  
**开始日期：** 2026-03-17

---

**最后更新：** 2026-03-17 15:00 GMT+8
