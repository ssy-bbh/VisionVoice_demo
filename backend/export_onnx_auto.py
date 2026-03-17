#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wav2Vec2 ONNX 导出脚本 - 自动版（无需交互）

默认使用 facebook/wav2vec2-base，自动量化
"""

import os
import sys
from pathlib import Path

# 项目路径
PROJECT_ROOT = Path(r"D:\AndroidStudioProjects\MyApplication")
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "onnx"

# 默认模型
MODEL_NAME = "facebook/wav2vec2-base"

def export_model_auto():
    """自动导出 Wav2Vec2 模型"""
    
    print("=" * 60)
    print("🚀 Wav2Vec2 ONNX 导出工具 - 自动版")
    print("=" * 60)
    print(f"\n📦 使用模型：{MODEL_NAME}")
    
    # 创建输出目录
    print(f"\n📁 创建输出目录：{ASSETS_DIR}")
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    
    # 导出模型
    print(f"\n🔄 正在导出模型 (这可能需要 5-10 分钟)...")
    print(f"   从 HuggingFace 下载：{MODEL_NAME}")
    
    try:
        from optimum.onnxruntime import ORTModelForCTC
        from transformers import Wav2Vec2FeatureExtractor, Wav2Vec2Processor
        import torch
        
        # 加载并导出
        print("   加载模型...")
        model = ORTModelForCTC.from_pretrained(
            MODEL_NAME,
            export=True,
            provider="CPUExecutionProvider"
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
        print("\n📦 正在进行 INT8 量化...")
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
        
        # 创建 README
        readme_path = ASSETS_DIR / "README.txt"
        with open(readme_path, "w", encoding="utf-8") as f:
            f.write(f"Wav2Vec2 ONNX Model - VisionVoice Project\n")
            f.write(f"========================================\n\n")
            f.write(f"Source: {MODEL_NAME}\n")
            f.write(f"Export Date: {torch.__version__}\n")
        
        print(f"\n✅ 导出完成！")
        print(f"\n📂 文件列表:")
        for f in ASSETS_DIR.iterdir():
            size = f.stat().st_size / 1024 / 1024 if f.is_file() else 0
            print(f"   - {f.name} ({size:.2f} MB)" if f.is_file() else f"   - {f.name}/")
        
        return True
        
    except Exception as e:
        print(f"\n❌ 导出失败：{e}")
        return False


if __name__ == "__main__":
    print("\nVisionVoice - Wav2Vec2 ONNX 导出工具（自动版）\n")
    
    success = export_model_auto()
    
    if success:
        print("\n" + "=" * 60)
        print("✅ 导出成功！")
        print("=" * 60)
        print("\n下一步:")
        print("1. 在 Android Studio 中 Sync Gradle 项目")
        print("2. 添加 ONNX Runtime Android 依赖")
        print("3. 测试端侧模型加载")
        sys.exit(0)
    else:
        print("\n❌ 导出失败")
        sys.exit(1)
