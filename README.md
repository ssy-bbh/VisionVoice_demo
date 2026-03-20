# VisionVoice 项目

**AR 英语学习应用** - 集Sight Words、语音识别、口语练习于一身

---

## 核心功能

### 🌐 AR 实时识别
- 手机摄像头扫描周围环境
- 实时显示英语单词
- 40 类物体识别（家具/食物/动物...）

### 🎤 口语练习
- AI 语音识别
- 评分对比反馈
- TTS 示范读音
- ✨ **端侧离线评分**（无需网络）

### 📚 单词收藏
- 收藏学习过的单词
- 学习统计
- 成就系统

---

## 技术栈

### 前端
- **语言**: Java
- **SDK**: 24-36
- **相机**: CameraX
- **UI**: Material Design

### 机器学习
- **物体检测**: YOLOv8n (TFLite)
- **语音识别**: Wav2Vec2 (ONNX)
- **端侧推理**: ONNX Runtime Mobile

### 后端（可选）
- **框架**: Python FastAPI
- **模型**: Wav2Vec2 + 自研算法

---

## 文档

| 文档 | 说明 |
|------|------|
| [快速开始](docs/setup/QUICK_START.md) | 5分钟快速搭建 |
| [模型导出](docs/setup/ONNX_GUIDE.md) | Wav2Vec2 端侧部署 |
| [项目结构](docs/dev/STRUCTURE.md) | 目录结构说明 |
| [变更日志](docs/logs/CHANGELOG.md) | 版本历史 |

---

## 项目结构

```
MyApplication/
├── app/                    # Android 主模块
│   └── src/main/
│       ├── java/.../
│       │   ├── ml/         # ML 模型
│       │   └── ui/         # UI 组件
│       └── assets/         # 模型文件
├── backend/                # Python 后端
└── docs/                   # 文档
```

---

## 开发环境

### 必须
- Android Studio Hedgehog+
- JDK 11+
- Android SDK 24-36

### 可选（后端）
- Python 3.8+
- FastAPI
- PyTorch

---

## 参考

- **论文**: ARIELLE: AR-based Independent and Experiential Language Learner on the Edge (2024 IEEE APCCAS)
- **ONNX Runtime**: https://onnxruntime.ai/
- **Wav2Vec2**: https://huggingface.co/facebook/wav2vec2-base

---

**维护团队**: SONG  
**AI 协助**: OpenClaw
