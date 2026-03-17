#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 ONNX 模型加载和推理
验证模型是否能正常工作
"""

import sys
from pathlib import Path

ASSETS_DIR = Path(r"D:\AndroidStudioProjects\MyApplication\app\src\main\assets\onnx")
MODEL_PATH = ASSETS_DIR / "model.onnx"

def test_model():
    print("=" * 60)
    print("🧪 ONNX 模型测试")
    print("=" * 60)
    
    # 1. 检查模型文件
    print(f"\n📁 检查模型文件...")
    if not MODEL_PATH.exists():
        print(f"❌ 模型文件不存在: {MODEL_PATH}")
        return False
    
    size_mb = MODEL_PATH.stat().st_size / 1024 / 1024
    print(f"✅ 模型文件存在: {size_mb:.2f} MB")
    
    # 2. 加载模型
    print(f"\n📦 加载 ONNX 模型...")
    try:
        import onnxruntime as ort
        
        session = ort.InferenceSession(str(MODEL_PATH))
        print(f"✅ 模型加载成功")
        
        # 3. 检查输入输出
        print(f"\n🔍 模型信息:")
        print(f"   输入: {[i.name for i in session.get_inputs()]}")
        print(f"   输入形状: {[i.shape for i in session.get_inputs()]}")
        print(f"   输出: {[o.name for o in session.get_outputs()]}")
        print(f"   输出形状: {[o.shape for o in session.get_outputs()]}")
        
        # 4. 测试推理
        print(f"\n⚡ 测试推理...")
        import numpy as np
        
        # 创建测试输入（1秒静音）
        dummy_input = np.zeros(16000, dtype=np.float32)
        inputs = {session.get_inputs()[0].name: dummy_input.reshape(1, -1)}
        
        outputs = session.run(None, inputs)
        
        print(f"✅ 推理成功")
        print(f"   输出形状: {outputs[0].shape}")
        print(f"   输出类型: {outputs[0].dtype}")
        
        # 5. 测试内存占用估计
        print(f"\n💾 内存占用估计:")
        print(f"   模型文件: {size_mb:.2f} MB")
        print(f"   运行时内存: ~{size_mb * 1.5:.2f} MB (估算)")
        
        return True
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = test_model()
    
    print("\n" + "=" * 60)
    if success:
        print("✅ 模型测试通过")
        print("=" * 60)
        print("\n结论:")
        print("- 模型可以正常加载和推理")
        print("- 文件大小 360MB 对移动端来说很大")
        print("- 建议后续使用小模型优化")
        sys.exit(0)
    else:
        print("❌ 模型测试失败")
        print("=" * 60)
        sys.exit(1)
