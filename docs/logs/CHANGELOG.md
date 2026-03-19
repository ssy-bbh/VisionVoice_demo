# 变更日志

## [未发布]

### ✨ 新增 - 端侧语音评分
- 新增 Wav2Vec2 ONNX 导出脚本 (`backend/export_onnx.py`)
- 新增端侧语音评分模块 (`ml/Wav2Vec2Scorer.java`)
- 新增文档 (`setup/ONNX_GUIDE.md`, `setup/QUICK_START.md`)
- 计划支持离线发音评分 + 在线评分切换

### 📝 文档
- 新增项目文档 (`docs/PROJECT_README.md`)
- 新增变更日志 (`logs/CHANGELOG.md`)
- 新增项目结构文档 (`dev/STRUCTURE.md`)

### 🔧 功能改进
- 支持离线模式（端侧/在线切换）
- 优化音频预处理流程
- 实现 Needleman-Wunsch 发音对齐算法

---

## [1.0.0] - 2026-03-17

### 🚀 初次发布

#### 核心功能
- 🌐 AR 实时物体识别 (CameraX + YOLOv8n)
- 📷 照片识别
- 🎤 口语练习（评分，后端 API）
- 📚 单词收藏
- 👤 用户资料

#### 技术栈
- Android (Java, SDK 24-36)
- TensorFlow Lite (YOLOv8n)
- Python FastAPI (在线服务)
- Wav2Vec2 (语音识别)

#### UI 组件
- MainActivity (底部导航)
- HomeFragment (首页)
- RealtimeActivity (AR 实时识别)
- PhotoRecognitionActivity (照片识别)
- PracticeActivity (口语练习)
- CollectionFragment (单词收藏)
- ProfileFragment (用户资料)
