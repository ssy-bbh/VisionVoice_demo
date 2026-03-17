# ==========================================
# VisionVoice - Conda 环境设置脚本
# ==========================================

# 1. 激活 conda 环境
conda activate visionvoice

# 2. 检查 Python 版本
python --version

# 3. 安装依赖包
pip install optimum[onnxruntime] transformers torch torchaudio phonemizer onnxruntime-gpu

# 4. 验证安装
python -c "import torch; from optimum.onnxruntime import ORTModelForCTC; print('✅ 依赖安装成功')"
