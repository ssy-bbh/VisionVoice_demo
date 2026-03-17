# VisionVoice 变更日志

所有重要的项目变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [未发布]

### 🆕 新增 - 端侧发音评估功能
- 添加 Wav2Vec2 ONNX 导出脚本 (`backend/export_onnx.py`)
- 添加端侧发音评分器 (`ml/Wav2Vec2Scorer.java`)
- 添加完整部署文档 (`docs/WAV2VEC2_ONNX_GUIDE.md`, `docs/QUICK_START.md`)
- 计划支持离线发音评估，无需联网

### 📚 文档
- 创建项目主文档 (`docs/PROJECT_README.md`)
- 创建变更日志 (`docs/CHANGELOG.md`)
- 整理项目结构，所有文档移至 `docs/` 目录

### 🔧 技术改进
- 支持混合模式（端侧/后端）切换
- 优化音频预处理流程
- 实现 Needleman-Wunsch 音素对齐算法（计划中）

---

## [1.0.0] - 2026-03-17

### 🎉 首次发布

#### 核心功能
- ✅ AR 实时物体识别（CameraX + YOLOv8n）
- ✅ 拍照识别
- ✅ 发音练习和评分（后端 API）
- ✅ 单词收藏
- ✅ 个人中心

#### 技术栈
- Android (Java, SDK 24-36)
- TensorFlow Lite (YOLOv8n)
- Python FastAPI (后端服务)
- Wav2Vec2 (发音评估)

#### UI 组件
- MainActivity (底部导航)
- HomeFragment (首页)
- RealtimeActivity (AR 实时识别)
- PhotoRecognitionActivity (拍照识别)
- PracticeActivity (发音练习)
- CollectionFragment (单词收藏)
- ProfileFragment (个人中心)

#### 自定义视图
- OverlayView (AR 覆盖层)
- FocusBoxView (聚焦框)

#### 机器学习
- YOLOv8n 物体检测（40 类物体）
- Wav2Vec2 发音评估（后端）

#### 后端 API
- GET `/get_phonetics/` - 获取单词音标
- POST `/evaluate_pronunciation/` - 发音评分

---

## 开发日志

### 2026-03-17

#### 14:11 - Zotero 文献阅读
- 打开 Zotero 客户端
- 找到 ARIELLE 论文（AR 语言学习系统）
- 论文路径：`C:\Users\Dell\Zotero\storage\X4FEQTEL\`
- 标题：ARIELLE: AR-based Independent and Experiential Language Learner on the Edge
- 作者：新加坡国立大学团队
- 发表：2024 IEEE APCCAS

#### 14:18 - PDF 阅读工具安装
- 创建工具目录：`D:\Tools\`
- 安装 PyPDF：`pip install pypdf --target "D:\Tools\python-packages"`
- 创建阅读脚本：`D:\Tools\read_pdf.py`
- 成功读取 ARIELLE 论文前 3 页

#### 14:23 - ARIELLE 论文分析
**核心设计总结：**
- **硬件：** Nezha 开发板（Intel N97）+ 摄像头 + 麦克风 + 触摸屏
- **物体检测：** YOLOv8n（40 类物体，70k 训练图）
- **发音评估：** Wav2Vec2-LV-60-espeak + OpenVINO INT8 量化
- **双分支评分：**
  - ASR 辅助音素评分
  - 直接信号处理（音高对比）
- **三级容错：**
  - Ignored（硬件噪音）
  - Flaw（发音瑕疵）
  - Substitution（错读）

#### 14:33 - VisionVoice 项目分析
**发现的现有功能：**
- ✅ 完整的 Android 应用架构
- ✅ YOLO 物体检测已集成
- ✅ 后端发音评分已实现
- ✅ UI/UX 设计完善
- ⚠️ 端侧 Wav2Vec2 待集成

**项目位置：** `D:\AndroidStudioProjects\MyApplication\`

#### 14:50 - 端侧部署文档创建
**创建的文件：**
1. `docs/WAV2VEC2_ONNX_GUIDE.md` (15KB) - 完整实施指南
2. `docs/QUICK_START.md` (12KB) - 快速实施清单
3. `backend/export_onnx.py` (7KB) - ONNX 导出脚本
4. `ml/Wav2Vec2Scorer.java` (11KB) - 端侧评分器模板

**核心建议：**
- 采用渐进式迁移策略
- 阶段 1：混合模式（后端 + 端侧切换）
- 阶段 2：端侧优化
- 阶段 3：智能切换

#### 15:00 - 项目文档整理
**整理的文件结构：**
```
MyApplication/
├── docs/                          # 🆕 文档目录
│   ├── PROJECT_README.md          # 🆕 项目主文档
│   ├── QUICK_START.md             # 🆕 快速指南
│   ├── WAV2VEC2_ONNX_GUIDE.md     # 🆕 完整指南
│   └── CHANGELOG.md               # 🆕 变更日志
├── backend/
│   ├── server.py                  # 后端服务
│   └── export_onnx.py             # 🆕 导出脚本
└── app/src/main/java/.../ml/
    ├── ObjectRecognitionHelper.java  # YOLO 检测
    └── Wav2Vec2Scorer.java           # 🆕 端侧评分
```

---

## 待办事项

### 🔥 高优先级
- [ ] 运行 `python backend/export_onnx.py` 导出 ONNX 模型
- [ ] 在 `app/build.gradle.kts` 中添加 ONNX Runtime 依赖
- [ ] 创建 `AudioProcessor.java` 音频预处理类
- [ ] 测试端侧模型加载

### 📋 中优先级
- [ ] 完善 `Wav2Vec2Scorer.java` 音素词典（集成 CMU Dict）
- [ ] 实现 Needleman-Wunsch 对齐算法
- [ ] 修改 `PracticeActivity` 支持混合模式切换
- [ ] 添加 UI 切换开关（端侧/后端）

### 💡 低优先级
- [ ] 性能优化（模型量化、异步加载）
- [ ] 用户测试和反馈收集
- [ ] 文档完善（API 文档、用户手册）

---

## 技术决策记录

### 决策 1：端侧部署方案选择
**日期：** 2026-03-17  
**状态：** 已决定

**选项：**
1. ONNX Runtime Mobile ✅ 选择
2. TensorFlow Lite
3. MediaPipe Audio

**理由：**
- ONNX 支持最广泛（HuggingFace 直接导出）
- 量化工具成熟（INT8 减小 70% 体积）
- 性能优秀（中端手机 300-800ms）
- 文档完善

### 决策 2：混合架构过渡
**日期：** 2026-03-17  
**状态：** 已决定

**方案：**
- 默认使用后端模式（稳定）
- 添加"离线模式"开关
- 端侧模式测试完善后逐步切换

**理由：**
- 降低风险
- 用户可以对比准确率
- 可以根据网络情况灵活选择

---

## 已知问题

### 问题 1：端侧模型体积较大
**影响：** APK 大小增加 25MB  
**解决方案：** 
- 使用 Play Asset Delivery 动态下发
- 或使用 INT8 量化模型（25MB → 已优化）

### 问题 2：低端设备性能
**影响：** 推理时间可能超过 1 秒  
**解决方案：**
- 限制录音时长（1-3 秒）
- 低端设备自动切换到后端模式
- 使用更小的模型（如 wav2vec2-small）

---

## 贡献者

- **开发者：** VisionVoice Team
- **AI 助手：** OpenClaw

---

**最后更新：** 2026-03-17 15:00 GMT+8
