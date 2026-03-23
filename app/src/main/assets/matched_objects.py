import json
import urllib.request
import ssl

def filter_and_match(your_dict_file):
    # 忽略 SSL 证书验证 (防止部分网络环境下 urllib 报错)
    context = ssl._create_unverified_context()

    # 1. 获取 LVIS 类别
    print("正在拉取 LVIS 类别列表 (作为物理实体过滤器)...")
    url = "https://raw.githubusercontent.com/lvis-dataset/lvis-api/master/data/lvis_v1_val.json"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        response = urllib.request.urlopen(req, context=context)
        lvis_data = json.loads(response.read())
        # 将 LVIS 的类别 (如 coffee_maker) 统一转为空格分隔并小写
        lvis_categories = {cat['name'].replace('_', ' ').lower() for cat in lvis_data['categories']}
    except Exception as e:
        print(f"获取 LVIS 数据失败: {e}")
        return

    # 2. 解析你的音素词表
    print("正在解析你的单词音素表...")
    my_dict = {}
    try:
        with open(your_dict_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line: continue

                # 按第一个空格切分：前面是单词，后面是音素
                parts = line.split(maxsplit=1)
                if len(parts) > 0:
                    word = parts[0].lower().replace('_', ' ')
                    phonemes = parts[1] if len(parts) > 1 else ""
                    my_dict[word] = phonemes
    except FileNotFoundError:
        print(f"找不到文件: {your_dict_file}")
        return

    # 3. 双向筛选
    my_word_set = set(my_dict.keys())

    # 交集：具体物体，且可以直接用开源数据集
    matched_words = my_word_set.intersection(lvis_categories)
    # 差集：大概率是抽象名词、或者 LVIS 遗漏的罕见物体
    unmatched_words = my_word_set - lvis_categories

    print("\n" + "="*40)
    print(f"总处理单词数: {len(my_word_set)}")
    print(f"✅ 成功匹配的实体物体: {len(matched_words)} 个")
    print(f"❓ 被过滤或未匹配的词: {len(unmatched_words)} 个")
    print("="*40 + "\n")

    # 4. 导出结果 (带上原有的音素)
    with open('matched_objects_with_phonemes.txt', 'w', encoding='utf-8') as f:
        for w in sorted(matched_words):
            f.write(f"{w} {my_dict[w]}\n")

    with open('filtered_out_words.txt', 'w', encoding='utf-8') as f:
        for w in sorted(unmatched_words):
            f.write(f"{w} {my_dict[w]}\n")

    print("已生成:\n1. matched_objects_with_phonemes.txt (留作模型训练)\n2. filtered_out_words.txt (建议简单人工扫一眼)")

# 使用方法：将 'your_list.txt' 替换为你实际的文本文件路径
# filter_and_match('your_list.txt')