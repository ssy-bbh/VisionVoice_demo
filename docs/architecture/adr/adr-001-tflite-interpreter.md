# ADR-001: 采用 TFLite Interpreter API 而非高级 ObjectDetector API

## 状态
✅ **已接受**（v1.0 开发阶段）

## 背景

在项目从图像分类模型升级到目标检测模型的过程中，我们选定了 **YOLOv8n.tflite** 作为目标检测模型。然而，将模型集成到 Android 应用时遇到了重大障碍：

**问题**：新模型由第三方工具（`export_tflite.py`）转换，**缺少 TensorFlow Lite Task Library 高级 API 所必需的元数据**（特别是 `NormalizationOptions`）。这导致标准集成方案 —— `ObjectDetector` API —— 在初始化时直接崩溃：

```
RuntimeException: Failed to load metadata from model
```

## 问题陈述

如何在没有模型元数据的情况下，在 Android 端成功运行 YOLOv8n 目标检测模型？

## 决策方案（已采纳）

**放弃高级 API，切换到底层 `Interpreter` API**，手动实现完整的 ML 推理管线：

1. **手动内存映射**：`loadModelFile()` 方法通过 `FileChannel.map()` 将模型文件高效加载为 `MappedByteBuffer`
2. **手动图像预处理**：`ImageProcessor` + `ResizeOp` + `NormalizeOp`，将任意 Bitmap 缩放到 640×640 并归一化
3. **手动后处理**：单次遍历算法直接解析 YOLOv8 的 `[1][84][8400]` 输出张量，无需转置

## 评估的备选方案

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| **Interpreter API（本采纳）** | 完全控制，灵活性高，可深度优化 | 代码量大，需自行处理坐标系转换 | ✅ |
| 重新转换模型（添加元数据） | 可使用高级 API | YOLOv8 → TFLite 转换工具链复杂，不一定成功 | ❌ |
| 使用其他目标检测库 | API 更友好 | 体积大、性能差 | ❌ |

## 后果

### ✅ 正面后果
- 成功适配"非标准" YOLOv8 模型，突破技术瓶颈
- 获得对推理流程的完全控制权
- 单次遍历后处理算法将时间复杂度从 O(2N) 优化到 O(N)
- 为未来的精细化性能调优（多目标展示、自适应阈值）奠定了基础

### ⚠️ 可迁移后果
- 代码量增加：约 +150 行（vs 使用高级 API）
- 需要自行维护坐标系转换逻辑
- 对团队有一定学习成本

## 参考资料
- [TensorFlow Lite Interpreter API](https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/Interpreter)
- [YOLOv8 Export to TFLite](https://docs.ultralytics.com/integrations/tflite/)
