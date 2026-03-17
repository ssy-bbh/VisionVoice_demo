# 实验记录 - 端侧 Wav2Vec2 模型部署

**实验日期：** 2026-03-17  
**实验目的：** 探索大模型（360MB）在移动端的部署可行性  
**实验环境：** Android + ONNX Runtime Mobile  
**模型：** facebook/wav2vec2-base (ONNX FP32)

---

## 1. 实验背景

### 1.1 研究动机
- 现有 VisionVoice 应用依赖后端 API 进行发音评估
- 需要网络连接，无法在离线环境使用
- 目标：将 Wav2Vec2 模型部署到移动端，实现离线评估

### 1.2 技术挑战
- Wav2Vec2 base 模型大小：360MB
- 运行时内存占用：~540MB
- 移动端资源受限（内存、存储、计算）

---

## 2. 实验方法

### 2.1 模型导出
```python
# 使用 Optimum 库导出 ONNX
from optimum.onnxruntime import ORTModelForCTC

model = ORTModelForCTC.from_pretrained(
    "facebook/wav2vec2-base",
    export=True,
    provider="CPUExecutionProvider"
)
model.save_pretrained("assets/onnx/")
```

**导出结果：**
- 原始模型：360.30 MB
- 量化尝试：失败（模型复杂度高）
- 输出格式：ONNX FP32

### 2.2 移动端集成
- **框架：** ONNX Runtime Mobile 1.17.0
- **平台：** Android (Java)
- **测试设备：** [待填写]

### 2.3 测试指标
1. 模型加载时间
2. 推理时间（1秒音频）
3. 内存占用
4. 是否 OOM

---

## 3. 实验结果

### 3.1 模型验证（PC 端）
| 指标 | 结果 |
|------|------|
| 模型加载 | ✅ 成功 |
| 推理测试 | ✅ 成功 |
| 输入形状 | [1, 16000] (1秒音频) |
| 输出形状 | [1, 49, 32] |

### 3.2 移动端测试（待完成）
| 指标 | 预期 | 实际 | 状态 |
|------|------|------|------|
| 模型加载时间 | < 5s | - | ⏳ |
| 推理时间 | < 2s | - | ⏳ |
| 内存占用 | ~540MB | - | ⏳ |
| OOM 错误 | 无 | - | ⏳ |

---

## 4. 结果分析

### 4.1 可行性评估
- **技术可行性：** [待根据测试结果填写]
- **性能表现：** [待根据测试结果填写]
- **实用性：** [待根据测试结果填写]

### 4.2 与 ARIELLE 论文对比
| 项目 | ARIELLE | 本实验 |
|------|---------|--------|
| 硬件 | Intel N97 (边缘设备) | Android 手机 |
| 模型大小 | ~95MB (INT8) | 360MB (FP32) |
| 量化工具 | Intel OpenVINO | ONNX Runtime |
| 推理时间 | 未公开 | [待测试] |

**关键差异：**
- ARIELLE 使用 Intel 专用量化工具，模型更小
- 本实验使用 ONNX Runtime，量化失败
- 移动端 ARM 架构与 Intel x86 架构差异

---

## 5. 结论与展望

### 5.1 当前结论
- [待根据测试结果填写]

### 5.2 优化方向
1. **使用小模型**
   - DistilWav2Vec2 (~50MB)
   - Whisper Tiny (~39MB)

2. **改进量化方法**
   - 预处理 + 静态量化
   - 动态量化（运行时）

3. **架构优化**
   - 混合模式（端侧+云端）
   - 模型分片加载

---

## 6. 论文引用素材

### 6.1 技术方案
> "本研究尝试将 Wav2Vec2 模型部署到 Android 移动端，使用 ONNX Runtime Mobile 框架进行推理。模型导出采用 HuggingFace Optimum 工具链，将 PyTorch 模型转换为 ONNX 格式。"

### 6.2 实验挑战
> "实验中发现，Wav2Vec2 base 模型（360MB）在移动端部署面临严峻挑战。模型大小远超移动应用常规限制（<100MB），运行时内存占用估计达 540MB，对设备性能要求较高。"

### 6.3 与现有工作对比
> "与 ARIELLE 系统相比，本研究面向更通用的 Android 设备（ARM 架构），而非专用的 Intel 边缘设备。ARIELLE 利用 Intel OpenVINO 实现 INT8 量化，模型压缩至 95MB；而本实验在 ONNX Runtime 框架下量化失败，凸显了跨平台模型优化的复杂性。"

---

## 附录

### A. 项目文件清单
```
MyApplication/
├── app/src/main/
│   ├── java/.../ml/
│   │   ├── Wav2Vec2Scorer.java    # 端侧评分器
│   │   └── AudioProcessor.java    # 音频预处理
│   ├── assets/onnx/
│   │   └── model.onnx             # 360MB ONNX 模型
│   └── ui/test/
│       └── OnnxTestActivity.java  # 测试 Activity
├── backend/
│   ├── export_onnx.py             # 模型导出脚本
│   └── test_model.py              # 模型验证脚本
└── docs/
    ├── ONNX_TEST_STEPS.md         # 测试步骤
    └── EXPERIMENT_RECORD.md       # 本文件
```

### B. 关键代码片段
[待补充]

---

**记录时间：** 2026-03-17 15:59  
**最后更新：** 2026-03-17 15:59
