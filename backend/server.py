import os
import shutil
import re
import torch
import torchaudio
import numpy as np
from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
from transformers import Wav2Vec2FeatureExtractor, Wav2Vec2PhonemeCTCTokenizer, Wav2Vec2Processor, Wav2Vec2ForCTC
from phonemizer import phonemize

app = FastAPI(title="VisionVoice Backend API")

# ==========================================
# 1. 全局加载模型 (避免每次请求重复加载)
# ==========================================
print("正在初始化并加载 Wav2Vec2 模型...")
feature_extractor = Wav2Vec2FeatureExtractor.from_pretrained("facebook/wav2vec2-lv-60-espeak-cv-ft")
tokenizer = Wav2Vec2PhonemeCTCTokenizer.from_pretrained("facebook/wav2vec2-lv-60-espeak-cv-ft")
processor = Wav2Vec2Processor(feature_extractor=feature_extractor, tokenizer=tokenizer)
model = Wav2Vec2ForCTC.from_pretrained("facebook/wav2vec2-lv-60-espeak-cv-ft")
print("模型加载完成！后端服务已准备就绪。")


# ==========================================
# 2. 核心算法模块
# ==========================================
def get_reference_phonemes(text):
    raw_phonemes = phonemize(text, language='en-us', backend='espeak', strip=True, preserve_punctuation=False)
    return raw_phonemes.replace(" ", "").replace("ː", "")


def extract_phonemes(audio_path):
    waveform, sample_rate = torchaudio.load(audio_path)

    # 1. 重采样到 16000Hz
    if sample_rate != 16000:
        resampler = torchaudio.transforms.Resample(orig_freq=sample_rate, new_freq=16000)
        waveform = resampler(waveform)

    # ================= 【核心新增：音频预处理】 =================
    # 获取音频的最大振幅
    max_amplitude = waveform.abs().max().item()

    # 噪音门槛 (Noise Gate)：如果声音极小 (比如低于 0.015)
    # 说明用户可能根本没出声，或者离麦克风太远，全录的是环境白噪音
    if max_amplitude < 0.015:
        print(f"⚠️ 警告: 录音音量极低 (峰值振幅仅为 {max_amplitude:.4f})，已触发防幻觉拦截。")
        # 直接返回空字符串，让后续的打分逻辑直接判定为“全部漏读(Deletion)”，而不是乱给错音
        return ""
    else:
        # 自动增益 (AGC) / 归一化：把用户的声音等比例拉满到 1.0 的最大音量
        waveform = waveform / max_amplitude
    # =========================================================

    # 2. 传入模型推断
    inputs = processor(waveform.squeeze().numpy(), sampling_rate=16000, return_tensors="pt")
    with torch.no_grad():
        logits = model(**inputs).logits

    predicted_ids = torch.argmax(logits, dim=-1)
    transcription_list = processor.batch_decode(predicted_ids)

    # 3. 提取与清洗字符串
    raw_string = transcription_list[0]
    cleaned_string = re.sub(r'[\d.,?!-]', '', raw_string)

    return cleaned_string.replace(" ", "").replace("ː", "")

# ====== 终极版：三级容错与诊断矩阵 ======
def get_error_type(ref, user):
    if ref == user:
        return "Match"

    # 1. 第一级：硬件/环境噪音造成的误差，直接忽略，当做完全正确 (绿色满分)
    IGNORED_NOISE = {
        ('t', 'ts'), ('t', 'tʃ'), ('t', 'ch'),
        ('p', 'b'), ('b', 'p'), ('k', 'g'), ('g', 'k'), ('t', 'd'), ('d', 't'),
        ('v', 'b') , # 麦克风经常把v听成b
        ('k', 'd'),  # 低音量时 k 常被识别为 d
        ('t', 'p'),  # 低音量时 t 常被识别为 p
        ('p', 't')
    }
    if (ref, user) in IGNORED_NOISE:
        return "Ignored"

    # 2. 第二级：真实的发音瑕疵，需要黄框警告并部分扣分 (黄色 60 分)
    FLAWS = {
        ('ɔ', 'o'): '元音发音不够饱满',
        ('ɔ', 'ɑ'): '元音发音位置偏移',
        ('ɔ', 'a'): '元音发音位置偏移',
        ('ɹ', 'r'): '卷舌音不够标准',
        ('ɹ', 'l'): '平翘舌混淆 (r/l不分)',
        ('æ', 'e'): '嘴张得不够大',
        ('æ', 'a'): '嘴张得不够大',
        ('æ', 'ɛ'): '梅花音发音偏差',
        ('v', 'w'): '唇齿音发成了双唇音',
        ('v', 'f'): '发音偏轻 (v/f不分)',
        ('i', 'e'): '长元音发音偏差',
        ('i', 'ɪ'): '长短元音不分',
        ('ʌ', 'a'): '元音发音不够饱满 (低音量影响)',
        ('ʌ', 'e'): '元音发音不够饱满'
    }
    if (ref, user) in FLAWS:
        return f"Flaw:{FLAWS[(ref, user)]}"

    # 3. 第三级：完全错读
    return "Substitution"


# 辅助函数：判断是否在可接受的路径内（无论是忽略还是瑕疵，都允许对齐）
def is_acceptable(ref, user):
    return get_error_type(ref, user) != "Substitution"


# ====== 核心打分算法 ======
def needleman_wunsch(ref_seq, user_seq, match_score=1, mismatch_penalty=-1, gap_penalty=-1):
    n, m = len(ref_seq), len(user_seq)
    score_matrix = np.zeros((n + 1, m + 1))

    for i in range(n + 1): score_matrix[i, 0] = i * gap_penalty
    for j in range(m + 1): score_matrix[0, j] = j * gap_penalty

    for i in range(1, n + 1):
        for j in range(1, m + 1):
            match = score_matrix[i - 1][j - 1] + (
                match_score if is_acceptable(ref_seq[i - 1], user_seq[j - 1]) else mismatch_penalty)
            delete = score_matrix[i - 1][j] + gap_penalty
            insert = score_matrix[i][j - 1] + gap_penalty
            score_matrix[i][j] = max(match, delete, insert)

    align_ref, align_user, feedback = [], [], []
    i, j = n, m
    while i > 0 or j > 0:
        current_score = score_matrix[i][j]
        if i > 0 and j > 0 and current_score == score_matrix[i - 1][j - 1] + (
                match_score if is_acceptable(ref_seq[i - 1], user_seq[j - 1]) else mismatch_penalty):

            err_type = get_error_type(ref_seq[i - 1], user_seq[j - 1])

            if err_type == "Match" or err_type == "Ignored":
                # 【神级优化】：如果是噪音误差，强行把用户的发音改成标准音！让UI完美标绿！
                align_ref.append(ref_seq[i - 1])
                align_user.append(ref_seq[i - 1])
                feedback.append("Match")
            elif err_type.startswith("Flaw:"):
                # 如果是真实瑕疵，保留用户读错的音标，返回黄框原因
                align_ref.append(ref_seq[i - 1])
                align_user.append(user_seq[j - 1])
                feedback.append(err_type)
            else:
                align_ref.append(ref_seq[i - 1])
                align_user.append(user_seq[j - 1])
                feedback.append("Substitution (错读)")

            i -= 1
            j -= 1
        elif i > 0 and current_score == score_matrix[i - 1][j] + gap_penalty:
            align_ref.append(ref_seq[i - 1])
            align_user.append("-")
            feedback.append("Deletion (漏读)")
            i -= 1
        else:
            align_ref.append("-")
            align_user.append(user_seq[j - 1])
            feedback.append("Insertion (多读)")
            j -= 1

    return align_ref[::-1], align_user[::-1], feedback[::-1]

# ==========================================
# 3. API 接口定义
# ==========================================

# --- 新增的接口：用于 Android 进入页面时获取顶部显示的音标 ---
@app.get("/get_phonetics/")
async def get_phonetics_api(word: str):
    try:
        phonemes = get_reference_phonemes(word)
        # 加上斜杠，展示效果更像音标，如 /kibɔɹd/
        display_phonetics = f"/{phonemes}/"
        return JSONResponse(content={
            "word": word,
            "phonetics": display_phonetics
        })
    except Exception as e:
        print(f"获取音标出错: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})


# --- 核心打分接口 ---
@app.post("/evaluate_pronunciation/")
async def evaluate_pronunciation(target_word: str = Form(...), audio_file: UploadFile = File(...)):
    temp_audio_path = f"temp_{audio_file.filename}"
    with open(temp_audio_path, "wb") as buffer:
        shutil.copyfileobj(audio_file.file, buffer)

    try:
        ref_phonemes = get_reference_phonemes(target_word)
        user_phonemes = extract_phonemes(temp_audio_path)
        ref_aligned, user_aligned, feedback = needleman_wunsch(ref_phonemes, user_phonemes)

        # 增加打印，方便电脑端查看结果
        print(f"\n--- 收到评测请求 ---")
        print(f"目标单词: {target_word}")
        print(f"标准音素: {ref_aligned}")
        print(f"用户发音: {user_aligned}")
        print(f"对比结果: {feedback}")
        print(f"--------------------\n")

        return JSONResponse(content={
            "target_word": target_word,
            "reference_phonemes": ref_aligned,
            "user_phonemes": user_aligned,
            "feedback": feedback  # 键名已经修复为 feedback，确保 Android 能够解析
        })
    except Exception as e:
        print(f"后端处理发生错误: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})
    finally:
        if os.path.exists(temp_audio_path):
            os.remove(temp_audio_path)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)