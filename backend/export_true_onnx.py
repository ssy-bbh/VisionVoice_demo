import torch
from transformers import Wav2Vec2ForCTC
from onnxruntime.quantization import quantize_dynamic, QuantType
import os

# 1. 指定真正的国际音标 (IPA) 模型
model_name = "vitouphy/wav2vec2-xls-r-300m-phoneme"
print(f"⏳ 正在从 HuggingFace 下载并加载模型: {model_name} ...")
print("   (模型参数量为 300M，初次下载可能需要几分钟，请耐心等待)")

# 加载模型并设置为评估模式
model = Wav2Vec2ForCTC.from_pretrained(model_name)
model.eval()

# 2. 伪造一个 1 秒钟的 16kHz 假音频输入 (用于定义输入张量形状)
# Wav2Vec2 的输入是 1D 的 raw waveform
dummy_input = torch.randn(1, 16000)

# 3. 导出为未压缩的 ONNX (预计 1.2 GB)
onnx_path = "model.onnx"
print(f"\n⏳ 正在将 PyTorch 导出为 ONNX 格式 (保存为 {onnx_path})...")
torch.onnx.export(
    model,
    dummy_input,
    onnx_path,
    export_params=True,
    opset_version=14,  # 14 是目前对 Android ONNX Runtime 兼容性最好的版本
    do_constant_folding=True,
    input_names=['input_values'], # 对应 Java 里的 inputs.put("input_values", tensor)
    output_names=['logits'],
    dynamic_axes={
        'input_values': {0: 'batch_size', 1: 'sequence_length'},
        'logits': {0: 'batch_size', 1: 'sequence_length'}
    }
)
print(f"✅ 原版 ONNX 导出成功！体积较大。")

# 4. 动态量化为 INT8 (预计压缩至 300 MB 左右)
quant_path = "model_quant.onnx"
print(f"\n⏳ 正在进行 INT8 模型量化 (保存为 {quant_path})，这能大幅降低 Android 内存占用...")
try:
    quantize_dynamic(
        onnx_path,
        quant_path,
        weight_type=QuantType.QUInt8
    )
    print(f"✅ 量化成功！")
    print(f"\n🎉 全部完成！")
    print(f"👉 强烈建议：请把生成的【 {quant_path} 】放进你 Android 项目的 app/src/main/assets/onnx/ 目录下。")
    print(f"👉 并在 Wav2Vec2Scorer.java 中确保加载的是这个 quant 文件！")
except Exception as e:
    print(f"❌ 量化失败: {e}")
    print(f"👉 备用方案：你可以直接把 {onnx_path} 放进 Android 使用，但请注意手机内存。")