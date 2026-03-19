# VisionVoice 项目文档

**AR 英语学习应用** - 通过 AR 视觉识别 + 语音识别帮助用户学习英语单词和发音。

---

## 快速入口

| 需求 | 文档 |
|------|------|
| 首次搭建 | `setup/QUICK_START.md` |
| 模型导出 | `setup/ONNX_GUIDE.md` |
| 项目结构 | `dev/STRUCTURE.md` |
| 变更记录 | `logs/CHANGELOG.md` |

---

## 技术栈

- **前端**: Android (Java) + CameraX + ML Kit
- **AI 模型**: YOLOv8n (物体检测) + Wav2Vec2 (语音识别)
- **后端**: FastAPI (可选，本地优先)

---

## 项目位置

```
D:\AndroidStudioProjects\MyApplication\
├── app/                    # Android 主模块
├── backend/                # Python 后端 (模型导出)
├── docs/                   # 本文档目录
└── README.md              # 本文件
```
