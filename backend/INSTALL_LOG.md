# VisionVoice - 安装日志

## 安装日期：2026-03-17

### 步骤 1：激活 conda 环境 ✅
```
conda activate visionvoice
python --version  # Python 3.13.11
```

---

### 步骤 2：安装 PyTorch + Torchaudio ✅
**命令：**
```bash
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
```

**安装时间：** ~4 分钟  
**安装包：**
- torch-2.10.0+cpu (113.7 MB)
- torchaudio-2.10.0+cpu (473 kB)
- 依赖：filelock, sympy, networkx, jinja2, fsspec, mpmath, MarkupSafe

---

## 待执行步骤

### 步骤 3：安装 transformers + optimum
### 步骤 4：安装 phonemizer + onnxruntime-gpu
### 步骤 5：运行导出脚本

---

**记录时间：** 15:28  
**继续？** 是/否
