# VisionVoice 🎓📱

**AR 英语学习助手 - 看见世界，学会英语**

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com/)
[![SDK](https://img.shields.io/badge/SDK-24--36-blue.svg)](https://developer.android.com/studio/releases/platforms)
[![ML](https://img.shields.io/badge/ML-YOLO+Wav2Vec2-orange.svg)](https://github.com/ultralytics/yolov8)

---

## 🌟 功能亮点

### 📷 AR 实时识别
- 摄像头扫描周围物体
- 实时显示英文名称
- 40 类物体识别（教室/家庭/农村/户外）

### 🎤 发音练习
- AI 发音评分
- 音素级对比反馈
- TTS 发音示范
- 🆕 **端侧评估**（离线可用）

### 📚 单词管理
- 收藏学过的单词
- 学习统计
- 个人中心

---

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone <your-repo-url>
cd MyApplication
```

### 2. 打开项目
用 Android Studio 打开项目目录：
```
D:\AndroidStudioProjects\MyApplication
```

### 3. 同步依赖
Android Studio 会自动同步 Gradle 依赖

### 4. 运行应用
连接 Android 设备或启动模拟器，点击 Run

---

## 📖 文档

| 文档 | 说明 |
|------|------|
| [**项目主文档**](docs/PROJECT_README.md) | 完整的项目说明和技术架构 |
| [**快速指南**](docs/QUICK_START.md) | 端侧部署 5 分钟快速开始 |
| [**完整指南**](docs/WAV2VEC2_ONNX_GUIDE.md) | Wav2Vec2 端侧部署详细步骤 |
| [**变更日志**](docs/CHANGELOG.md) | 版本历史和开发日志 |

---

## 🏗️ 技术架构

### 前端
- **语言：** Java
- **SDK：** 24-36
- **相机：** CameraX
- **UI：** Material Design

### 机器学习
- **物体检测：** YOLOv8n (TFLite)
- **发音评估：** Wav2Vec2 (ONNX)
- **端侧推理：** ONNX Runtime Mobile

### 后端（可选）
- **框架：** Python FastAPI
- **发音评分：** Wav2Vec2 + 音素对齐
- **部署：** 本地/云端

---

## 📂 项目结构

```
MyApplication/
├── app/                          # Android 主模块
│   └── src/main/java/.../
│       ├── ml/                   # ML 模块 ⭐
│       │   ├── ObjectRecognitionHelper.java  # YOLO 检测
│       │   ├── Wav2Vec2Scorer.java           # 端侧评分
│       │   ├── AudioProcessor.java           # 音频预处理
│       │   └── README.md                     # ML 模块说明
│       ├── ui/                   # UI 界面
│       │   ├── home/
│       │   ├── ar/
│       │   ├── practice/
│       │   └── ...
│       └── view/                 # 自定义视图
├── backend/                      # Python 后端
│   ├── server.py
│   └── export_onnx.py
└── docs/                         # 文档
    ├── PROJECT_README.md
    ├── QUICK_START.md
    └── ...
```

---

## 🔧 开发环境

### 必需
- Android Studio Hedgehog 或更高版本
- JDK 11+
- Android SDK 24-36

### 可选（后端开发）
- Python 3.8+
- FastAPI
- PyTorch
- Transformers

---

## 📝 开发日志

### 2026-03-17
- ✅ 创建完整项目文档
- ✅ 添加 Wav2Vec2 端侧部署支持
- ✅ 创建 ONNX 导出脚本
- ✅ 整理项目结构
- ✅ Git 提交和版本控制

**详细记录：** [docs/CHANGELOG.md](docs/CHANGELOG.md)

---

## 🎯 下一步计划

### 短期
- [ ] 导出 ONNX 模型
- [ ] 集成 ONNX Runtime Mobile
- [ ] 测试端侧推理

### 中期
- [ ] 完善音素词典
- [ ] 实现混合模式切换
- [ ] 性能优化

### 长期
- [ ] 用户测试
- [ ] 发布 Beta 版本
- [ ] 功能扩展

---

## 📚 参考资料

- **论文：** ARIELLE: AR-based Independent and Experiential Language Learner on the Edge (2024 IEEE APCCAS)
- **ONNX Runtime:** https://onnxruntime.ai/
- **Wav2Vec2:** https://huggingface.co/facebook/wav2vec2-base
- **YOLOv8:** https://docs.ultralytics.com/

---

## 👥 团队

**开发者：** VisionVoice Team  
**AI 助手：** OpenClaw  

---

## 📄 许可证

本项目仅供学习和研究使用。

---

**最后更新：** 2026-03-17  
**版本：** v1.0.0
