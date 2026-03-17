@echo off
echo ========================================
echo VisionVoice - 环境设置脚本
echo ========================================
echo.

echo [1/3] 激活 conda 环境...
call conda activate visionvoice

echo [2/3] 检查 Python 版本...
python --version

echo [3/3] 安装依赖包...
pip install optimum[onnxruntime] transformers torch torchaudio phonemizer onnxruntime-gpu

echo.
echo ========================================
echo 安装完成！
echo ========================================
echo.
echo 下一步：
echo 1. cd D:\AndroidStudioProjects\MyApplication\backend
echo 2. python export_onnx.py
echo.

pause
