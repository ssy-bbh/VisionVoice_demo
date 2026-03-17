from transformers import Wav2Vec2Processor

# ⚠️ 极其重要：把这里换成你导出 ONNX 时所用的那个真实的模型名称！
# 比如："vitouphy/wav2vec2-xls-r-300m-phoneme" 或者你本地的模型文件夹路径
model_name = "vitouphy/wav2vec2-xls-r-300m-phoneme"
print(f"⏳ 正在读取 {model_name} 的底层词表...")

try:
    processor = Wav2Vec2Processor.from_pretrained(model_name)
    vocab = processor.tokenizer.get_vocab()

    print("\n🎉 提取成功！请将以下代码完整复制：\n")
    print("-" * 50)

    # 按照模型底层的真实 ID 排序
    for phoneme, id_val in sorted(vocab.items(), key=lambda x: x[1]):
        # 清理占位符和特殊符号
        clean_p = phoneme.replace("<pad>", "").replace("<s>", "").replace("</s>", "").replace("|", "").strip()

        # 只要不是空的，就生成 Java 映射代码
        if clean_p:
            print(f'map.put({id_val}, "{clean_p}");')

    print("-" * 50)
except Exception as e:
    print(f"❌ 读取模型失败: {e}")