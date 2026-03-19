# 项目结构

## 目录概览

```
MyApplication/
├── app/                    # Android 主模块
│   └── src/main/
│       ├── java/com/example/myapplication/
│       │   ├── ml/               # ML 模型
│       │   │   ├── Wav2Vec2Scorer.java
│       │   │   ├── AudioProcessor.java
│       │   │   └── PhonemeCache.java
│       │   └── ui/              # UI 组件
│       │       ├── home/
│       │       ├── ar/
│       │       ├── practice/
│       │       └── collection/
│       ├── assets/               # 资源文件
│       │   ├── onnx/             # Wav2Vec2 模型
│       │   └── yolov8n.tflite    # YOLO 模型
│       └── res/                  # 布局/图片
├── backend/                # Python 后端
│   ├── server.py
│   └── export_true_onnx.py
└── docs/                   # 文档
```

## 核心模块

### ML 模块 (`ml/`)
- **Wav2Vec2Scorer** - 语音识别 + 评分 (ONNX)
- **AudioProcessor** - 音频预处理
- **ObjectRecognitionHelper** - YOLO 目标检测

### UI 模块 (`ui/`)
- **HomeFragment** - 首页
- **RealtimeActivity** - AR 实时识别
- **PracticeActivity** - 口语练习
- **CollectionFragment** - 单词收藏

---

**参考**: `app/src/main/java/com/example/myapplication/` - 源代码
