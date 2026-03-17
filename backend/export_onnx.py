#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wav2Vec2 ONNX 导出脚本 - VisionVoice 项目
用于将 HuggingFace 模型转换为 Android 可用的 ONNX 格式

使用方法:
    python export_onnx.py

输出位置:
    D:\AndroidStudioProjects\MyApplication\app\src\main\assets\onnx\
"""

import os
import sys
from pathlib import Path

# 项目路径
PROJECT_ROOT = Path(r"D:\AndroidStudioProjects\MyApplication")
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "onnx"

def check_dependencies():
    """检查依赖是否已安装"""
    missing = []
    try:
        import torch
    except ImportError:
        missing.append("torch")
    
    try:
        from optimum.onnxruntime import ORTModelForCTC
    except ImportError:
        missing.append("optimum[onnxruntime]")
    
    try:
        import transformers
    except ImportError:
        missing.append("transformers")
    
    try:
        import onnxruntime
    except ImportError:
        missing.append("onnxruntime")
    
    try:
        from onnxruntime.quantization import quantize_dynamic
    except ImportError:
        missing.append("onnxruntime-gpu (for quantization)")
    
    if missing:
        print("❌ 缺少依赖包，请先安装:")
        print(f"   pip install {' '.join(missing)}")
        print("\n推荐安装命令:")
        print("   pip install optimum[onnxruntime] transformers torch torchaudio phonemizer onnxruntime-gpu")
        return False
    
    return True


def export_model():
    """导出 Wav2Vec2 模型为 ONNX 格式"""
    
    print("=" * 60)
    print("🚀 Wav2Vec2 ONNX 导出工具 - VisionVoice 项目")
    print("=" * 60)
    
    # 检查依赖
    if not check_dependencies():
        return False
    
    # 模型选择
    print("\n📦 可选模型:")
    print("   1. facebook/wav2vec2-base           (95MB, 推荐⭐)")
    print("   2. facebook/wav2vec2-base-960h      (95MB, 英语优化)")
    print("   3. jonatasgrosman/wav2vec2-large-xlsr-53-english (335MB, 最高准确率)")
    print()
    
    choice = input("选择模型 (1/2/3, 默认 1): ").strip() or "1"
    
    models = {
        "1": "facebook/wav2vec2-base",
        "2": "facebook/wav2vec2-base-960h",
        "3": "jonatasgrosman/wav2vec2-large-xlsr-53-english"
    }
    
    model_name = models.get(choice, models["1"])
    print(f"✅ 选择模型：{model_name}")
    
    # 创建输出目录
    print(f"\n📁 创建输出目录：{ASSETS_DIR}")
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    
    # 导出模型
    print(f"\n🔄 正在导出模型 (这可能需要 5-10 分钟)...")
    print(f"   从 HuggingFace 下载：{model_name}")
    
    try:
        from optimum.onnxruntime import ORTModelForCTC
        from transformers import Wav2Vec2FeatureExtractor, Wav2Vec2Processor
        import torch
        
        # 加载并导出
        print("   加载模型...")
        model = ORTModelForCTC.from_pretrained(
            model_name,
            export=True,
            provider="CPUExecutionProvider",
            use_auth_token=None  # 如果需要私有模型，在这里添加 token
        )
        
        print("   保存模型...")
        model.save_pretrained(str(ASSETS_DIR))
        
        print(f"✅ 模型已保存到：{ASSETS_DIR}")
        
        # 验证模型
        print("\n🔍 验证模型...")
        import onnxruntime as ort
        
        model_path = ASSETS_DIR / "model.onnx"
        session = ort.InferenceSession(str(model_path))
        
        print(f"✅ 模型输入：{[i.name for i in session.get_inputs()]}")
        print(f"✅ 模型输出：{[o.name for o in session.get_outputs()]}")
        print(f"✅ 模型大小：{model_path.stat().st_size / 1024 / 1024:.2f} MB")
        
        # 量化
        print("\n📦 是否进行 INT8 量化？(减小体积 70%, 可能损失<2% 准确率)")
        quantize_choice = input("   进行量化？(y/n, 默认 y): ").strip().lower() or "y"
        
        if quantize_choice == "y":
            print("   正在进行 INT8 量化...")
            from onnxruntime.quantization import quantize_dynamic, QuantType
            
            quant_path = ASSETS_DIR / "model_quant.onnx"
            quantize_dynamic(
                str(model_path),
                str(quant_path),
                weight_type=QuantType.QUInt8
            )
            
            print(f"✅ 量化完成！")
            print(f"   量化后大小：{quant_path.stat().st_size / 1024 / 1024:.2f} MB")
            print(f"   体积减少：{(1 - quant_path.stat().st_size / model_path.stat().st_size) * 100:.1f}%")
        
        # 创建 README 文件
        readme_path = ASSETS_DIR / "README.txt"
        with open(readme_path, "w", encoding="utf-8") as f:
            f.write(f"Wav2Vec2 ONNX Model - VisionVoice Project\n")
            f.write(f"========================================\n\n")
            f.write(f"Source: {model_name}\n")
            f.write(f"Export Date: {torch.__version__}\n")
            f.write(f"\nFiles:\n")
            f.write(f"- model.onnx: Original FP32 model\n")
            f.write(f"- model_quant.onnx: INT8 quantized model (recommended for mobile)\n")
            f.write(f"\nUsage in Android:\n")
            f.write(f"1. Add dependency: com.microsoft.onnxruntime:onnxruntime-android:1.17.0\n")
            f.write(f"2. Load model from assets/onnx/\n")
            f.write(f"3. Input: float[] audio (16kHz, normalized to [-1, 1])\n")
            f.write(f"4. Output: logits for CTC decoding\n")
        
        print(f"\n✅ 导出完成！")
        print(f"\n📂 文件列表:")
        for f in ASSETS_DIR.iterdir():
            size = f.stat().st_size / 1024 / 1024 if f.is_file() else 0
            print(f"   - {f.name} ({size:.2f} MB)" if f.is_file() else f"   - {f.name}/")
        
        return True
        
    except Exception as e:
        print(f"\n❌ 导出失败：{e}")
        print("\n可能的解决方案:")
        print("1. 检查网络连接（需要访问 HuggingFace）")
        print("2. 检查磁盘空间（需要至少 500MB 可用空间）")
        print("3. 如果使用代理，设置环境变量:")
        print("   export HTTP_PROXY=http://your-proxy:port")
        print("   export HTTPS_PROXY=http://your-proxy:port")
        return False


def test_model():
    """测试导出的模型"""
    print("\n🧪 测试模型...")
    
    model_path = ASSETS_DIR / "model_quant.onnx"
    if not model_path.exists():
        model_path = ASSETS_DIR / "model.onnx"
    
    if not model_path.exists():
        print("❌ 模型文件不存在，请先导出模型")
        return
    
    import onnxruntime as ort
    import numpy as np
    
    session = ort.InferenceSession(str(model_path))
    
    # 创建测试输入（1 秒静音）
    dummy_audio = np.zeros(16000, dtype=np.float32)
    
    print("   运行测试推理...")
    inputs = {session.get_inputs()[0].name: dummy_audio}
    outputs = session.run(None, inputs)
    
    print(f"✅ 测试通过！")
    print(f"   输出形状：{outputs[0].shape}")
    print(f"   输出类型：{outputs[0].dtype}")


if __name__ == "__main__":
    print("\nVisionVoice - Wav2Vec2 ONNX 导出工具\n")
    
    success = export_model()
    
    if success:
        print("\n" + "=" * 60)
        print("✅ 导出成功！")
        print("=" * 60)
        print("\n下一步:")
        print("1. 在 Android Studio 中 Sync Gradle 项目")
        print("2. 添加 ONNX Runtime Android 依赖")
        print("3. 创建 Wav2Vec2Scorer.java 类")
        print("4. 修改 PracticeActivity 使用端侧评分")
        print("\n详细指南请查看:")
        print("   D:\\AndroidStudioProjects\\MyApplication\\WAV2VEC2_ONNX_GUIDE.md")
        print()
        
        # 询问是否测试
        test_choice = input("是否运行测试？(y/n): ").strip().lower()
        if test_choice == "y":
            test_model()
    else:
        print("\n❌ 导出失败，请检查错误信息")
        sys.exit(1)
