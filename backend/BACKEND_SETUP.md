# VisionVoice 后端环境搭建与模型导出

**项目：** VisionVoice  
**文档版本：** v1.0  
**更新日期：** 2026-03-18  
**适用系统：** Windows 10/11，Miniconda

---

## 一、环境概览

| 项目 | 内容 |
|------|------|
| Python 版本 | 3.13.11 |
| Conda 环境名 | `visionvoice` |
| Conda 路径 | `D:\miniconda3\envs\visionvoice` |
| 后端框架 | FastAPI + Uvicorn |
| 推理框架 | PyTorch（CPU）+ Torchaudio |
| 在线评估模型 | `facebook/wav2vec2-lv-60-espeak-cv-ft` |
| ONNX 导出模型 | `vitouphy/wav2vec2-xls-r-300m-phoneme` |

---

## 二、环境搭建步骤

### 2.1 创建并激活 Conda 环境

```bash
conda create -n visionvoice python=3.13
conda activate visionvoice
```

### 2.2 安装 PyTorch + Torchaudio（CPU 版）

```bash
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
```

安装包：`torch-2.10.0+cpu`（113.7 MB）、`torchaudio-2.10.0+cpu`

### 2.3 安装其余依赖

```bash
pip install transformers fastapi uvicorn phonemizer onnxruntime onnxruntime-tools numpy
```

> **phonemizer 说明：** 依赖系统级 `espeak-ng`。Windows 下需单独安装：  
> 下载地址：https://github.com/espeak-ng/espeak-ng/releases  
> 安装后确认 `espeak-ng` 在系统 PATH 中可调用。

### 2.4 验证环境

```bash
python -c "import torch, torchaudio, transformers, fastapi, phonemizer, onnxruntime; print('OK')"
```

---

## 三、后端服务启动

### 3.1 启动命令

```bash
cd D:\AndroidStudioProjects\MyApplication\backend
conda activate visionvoice
python server.py
```

服务启动后监听 `http://0.0.0.0:8000`，Android 端通过 `http://127.0.0.1:8000` 访问。

### 3.2 接口说明

| 接口 | 方法 | 说明 |
|------|------|------|
| `/get_phonetics/?word=apple` | GET | 返回单词的 IPA 音标，如 `/æpəl/` |
| `/evaluate_pronunciation/` | POST | 上传音频 + 目标单词，返回音素对齐与评分 |

### 3.3 核心处理流程（server.py）

```
/evaluate_pronunciation/
    │
    ├── get_reference_phonemes(word)
    │       phonemize(text, language='en-us', backend='espeak')
    │       → IPA 音素字符串（如 "æpəl"）
    │
    ├── extract_phonemes(audio_path)
    │       torchaudio.load() → 重采样到 16kHz
    │       噪音门限检测（peak < 0.015 → 返回空串）
    │       自动增益（waveform / max_amplitude）
    │       Wav2Vec2ForCTC 推理 → CTC 解码 → IPA 音素
    │
    └── needleman_wunsch(ref, user)
            容错矩阵（Ignored / Flaw / Substitution）
            动态规划对齐 + 回溯
            → reference_phonemes, user_phonemes, feedback
```

---

## 四、ONNX 模型导出

### 4.1 导出脚本

脚本位置：`backend/export_true_onnx.py`

```bash
conda activate visionvoice
cd D:\AndroidStudioProjects\MyApplication\backend
python export_true_onnx.py
```

### 4.2 脚本执行流程

```
1. 从 HuggingFace 下载 vitouphy/wav2vec2-xls-r-300m-phoneme（约 1.2 GB）
        ↓
2. torch.onnx.export() → model.onnx（FP32，约 1.2 GB）
   - opset_version=14（Android ONNX Runtime 兼容性最佳）
   - dynamic_axes：支持任意长度音频输入
        ↓
3. quantize_dynamic() → model_quant.onnx（INT8，约 300 MB）
   - weight_type=QuantType.QUInt8
```

### 4.3 导出产物

```
backend/
├── model.onnx          # FP32 原始模型，约 1.2 GB
└── model_quant.onnx    # INT8 量化模型，约 300 MB
```

导出完成后，将模型文件复制到 Android assets：

```bash
copy model_quant.onnx D:\AndroidStudioProjects\MyApplication\app\src\main\assets\onnx\
copy model.onnx       D:\AndroidStudioProjects\MyApplication\app\src\main\assets\onnx\
```

### 4.4 模型词表提取

导出后需确认模型的音素 ID 映射，运行：

```bash
python WavReal.py
```

该脚本读取 `vitouphy/wav2vec2-xls-r-300m-phoneme` 的 tokenizer 词表，输出 `id → 音素` 的 Java 映射代码，用于 `Wav2Vec2Scorer.java` 中的 `buildIdToPhonemeMap()`。

**实际词表（40 类 ARPAbet 音素）：**

```
ID 1→aa   ID 2→ae   ID 3→ah   ID 4→aw   ID 5→ay
ID 6→b    ID 7→ch   ID 8→d    ID 9→dh   ID 10→dx
ID 11→eh  ID 12→er  ID 13→ey  ID 14→f   ID 15→g
ID 16→h#  ID 17→hh  ID 18→ih  ID 19→iy  ID 20→jh
ID 21→k   ID 22→l   ID 23→m   ID 24→n   ID 25→ng
ID 26→ow  ID 27→oy  ID 28→p   ID 29→r   ID 30→s
ID 31→sh  ID 32→spn ID 33→t   ID 34→th  ID 35→uh
ID 36→uw  ID 37→v   ID 38→w   ID 39→y   ID 40→z
（ID 0 = blank，CTC 解码时跳过）
```

---

## 五、模型验证

### 5.1 PC 端验证

```bash
python test_model.py
```

验证内容：
- 模型文件是否存在（`assets/onnx/model.onnx`）
- ONNX Runtime 能否正常加载
- 输入输出节点名称与形状
- 1 秒静音输入的推理输出形状

**预期输出：**
```
✅ 模型文件存在: 360.30 MB
✅ 模型加载成功
   输入: ['input_values']
   输入形状: [['batch_size', 'sequence_length']]
   输出: ['logits']
   输出形状: [['batch_size', 'sequence_length', 32]]
✅ 推理成功
   输出形状: (1, 49, 32)
```

### 5.2 Android 端验证

通过 `OnnxTestActivity`（HomeFragment 长按"实时扫描"卡片进入）验证：

| 指标 | 实测值 |
|------|--------|
| 模型加载时间 | ~3727 ms |
| 单次推理时间（1秒音频） | ~321 ms |
| Java 堆占用 | 11 MB / 384 MB |
| 输出形状 | `[1, 49, 32]` |

---

## 六、常见问题

**Q：`phonemizer` 报错 `espeak not found`**  
A：需安装系统级 espeak-ng，并确保其在 PATH 中。Windows 下从官方 Release 页面下载安装包。

**Q：`quantize_dynamic` 报错 `Expected .../Mul_output_0 to be an initializer`**  
A：该模型部分算子不支持动态量化。可直接使用 FP32 的 `model.onnx`，Android 端 mmap 加载不会 OOM。

**Q：HuggingFace 下载超时**  
A：设置镜像：
```bash
set HF_ENDPOINT=https://hf-mirror.com
python export_true_onnx.py
```

**Q：Android 加载模型时 OOM**  
A：确认使用文件路径加载（`createSession(String path)`），不要用 `createSession(byte[])`。详见 `docs/WAV2VEC2_ONNX_GUIDE.md`。
