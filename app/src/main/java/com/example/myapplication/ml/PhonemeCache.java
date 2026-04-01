package com.example.myapplication.ml;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 音素获取与本地缓存
 *
 * 优先级：
 *   1. 运行时缓存（phoneme_cache.json）：联网时后端 phonemizer 生成，最准确
 *   2. CMU Pronouncing Dictionary（assets/cmudict.dict）：13.5万词，完全离线
 *
 * 使用方式：
 *   PhonemeCache cache = new PhonemeCache(context);  // onCreate 时初始化
 *   String phonemes = cache.get("apple");             // 优先缓存，没有查 CMU Dict
 *   cache.put("apple", "æpəl");                       // 联网后写入缓存
 */
public class PhonemeCache {
    private static final String TAG = "PhonemeCache";
    private static final String CACHE_FILE = "phoneme_cache.json";

    private final File cacheFile;
    private final Context appContext;
    private JSONObject runtimeCache;      // 运行时缓存（phonemizer 生成）
    private final java.util.concurrent.CountDownLatch dictLatch = new java.util.concurrent.CountDownLatch(1);
    private Map<String, String> cmuDict;  // CMU Dict（懒加载）

    public PhonemeCache(Context context) {
        this.appContext = context.getApplicationContext();
        this.cacheFile = new File(context.getFilesDir(), CACHE_FILE);
        this.runtimeCache = loadRuntimeCache();
        // CMU Dict 在后台线程预加载，不阻塞 UI
        new Thread(this::loadCmuDict).start();
    }

    // ==================== 公开 API ====================

    /**
     * 获取单词音素字符串（如 "æpəl"）
     * 优先级：运行时缓存 > CMU Dict
     * 未找到返回 null
     */
    /**
     * 获取单词或词组的音素字符串（如 "coffee cup" -> 拼接两个词的音素）
     * 优先级：运行时缓存 > CMU Dict
     * 未找到返回 null
     */
    public String get(String word) {
        String normalizedInput = normalize(word);
        if (normalizedInput.isEmpty()) return null;

        // 1. 按空格切分，应对 "coffee cup" 这种词组
        String[] parts = normalizedInput.split("\\s+");
        StringBuilder combinedPhonemes = new StringBuilder();

        // 2. 确保 CMU 词典已经加载完成（移到循环外，避免多次阻塞）
        if (cmuDict == null) {
            try {
                Log.w(TAG, "⏳ CMU 词典正在加载，阻塞等待中...");
                dictLatch.await(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "等待词典加载被中断", e);
            }
        }

        // 3. 遍历词组中的每一个单词去查字典
        for (String part : parts) {
            String partPhonemes = null;

            // 优先查运行时缓存（phonemizer，最准确）
            partPhonemes = runtimeCache.optString(part, null);

            // 如果缓存没有，查 CMU Dict（离线）
            if (partPhonemes == null && cmuDict != null) {
                partPhonemes = cmuDict.get(part);
            }

            // 拼接到最终结果中
            if (partPhonemes != null) {
                combinedPhonemes.append(partPhonemes);
            } else {
                Log.e(TAG, "❌ 离线字典里找不到单词: " + part);
            }
        }

        // 如果全部都没查到，返回 null
        if (combinedPhonemes.length() == 0) {
            return null;
        }

        return combinedPhonemes.toString();
    }

    /**
     * 返回显示用音标，如 /æpəl/
     * 未找到返回 null
     */
    public String getDisplay(String word) {
        String p = get(word);
        return p != null ? "/" + p + "/" : null;
    }

    /**
     * 写入运行时缓存（联网后调用）
     */
    public void put(String word, String phonemes) {
        String key = normalize(word);
        try {
            runtimeCache.put(key, phonemes);
            saveRuntimeCache();
            Log.d(TAG, "缓存写入：" + key + " → " + phonemes);
        } catch (Exception e) {
            Log.e(TAG, "缓存写入失败", e);
        }
    }

    /** 运行时缓存是否命中（不含 CMU Dict） */
    public boolean hasRuntimeCache(String word) {
        return runtimeCache.has(normalize(word));
    }

    public int runtimeCacheSize() { return runtimeCache.length(); }

    // ==================== CMU Dict 加载 ====================

    private void loadCmuDict() {
        Map<String, String> dict = new HashMap<>(2000);
        try {
            InputStream is = appContext.getAssets().open("cmudict_ar_pro.dict");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                // 1. 跳过注释和空行（极速判断）
                if (line.isEmpty() || line.charAt(0) == ';') continue;

                // 2. 找到第一个空格的位置，分割单词和音标
                int firstSpace = line.indexOf(' ');
                if (firstSpace == -1) continue;

                String wordPart = line.substring(0, firstSpace);

                // 3. 极速处理 apple(2) 这种多音字后缀（替代耗时的 replaceAll）
                int parenIndex = wordPart.indexOf('(');
                String word = (parenIndex != -1 ? wordPart.substring(0, parenIndex) : wordPart).toLowerCase();

                // 4. 处理后面的音素
                String[] phonemeParts = line.substring(firstSpace + 1).trim().split("\\s+");
                StringBuilder ipa = new StringBuilder();
                for (String p : phonemeParts) {
                    String mapped = arpabetToIpa(p);
                    if (mapped != null) ipa.append(mapped);
                }

                if (!dict.containsKey(word) && ipa.length() > 0) {
                    dict.put(word, ipa.toString());
                }
            }
            reader.close();
            cmuDict = dict;
            Log.i(TAG, "✅ CMU Dict 极速加载完成，共 " + dict.size() + " 词");
        } catch (IOException e) {
            Log.e(TAG, "❌ CMU Dict 加载失败", e);
            cmuDict = new HashMap<>();
        } finally {
            dictLatch.countDown();
        }
    }

    // ARPAbet → IPA 映射（与 server.py espeak en-us 输出对齐）
    private static final Map<String, String> ARPABET_IPA = new HashMap<String, String>() {{
        put("AA", "ɑ");  put("AE", "æ");  put("AH", "ʌ");  put("AO", "ɔ");
        put("AW", "aʊ"); put("AY", "aɪ"); put("EH", "ɛ");  put("ER", "ɜɹ");
        put("EY", "eɪ"); put("IH", "ɪ");  put("IY", "i");  put("OW", "oʊ");
        put("OY", "ɔɪ"); put("UH", "ʊ");  put("UW", "u");
        put("B",  "b");  put("CH", "tʃ"); put("D",  "d");  put("DH", "ð");
        put("F",  "f");  put("G",  "g");  put("HH", "h");  put("JH", "dʒ");
        put("K",  "k");  put("L",  "l");  put("M",  "m");  put("N",  "n");
        put("NG", "ŋ");  put("P",  "p");  put("R",  "ɹ");  put("S",  "s");
        put("SH", "ʃ");  put("T",  "t");  put("TH", "θ");  put("V",  "v");
        put("W",  "w");  put("Y",  "j");  put("Z",  "z");  put("ZH", "ʒ");
    }};

    private String arpabetToIpa(String arpabet) {
        // 去掉重音数字：AE1 → AE
        String base = arpabet.replaceAll("[0-9]", "").toUpperCase();
        return ARPABET_IPA.get(base);
    }

    // ==================== 运行时缓存读写 ====================

    private JSONObject loadRuntimeCache() {
        if (!cacheFile.exists()) return new JSONObject();
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new FileReader(cacheFile));
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            JSONObject obj = new JSONObject(sb.toString());
            Log.d(TAG, "运行时缓存加载：" + obj.length() + " 条");
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "运行时缓存加载失败", e);
            return new JSONObject();
        }
    }

    private void saveRuntimeCache() {
        try {
            FileWriter w = new FileWriter(cacheFile);
            w.write(runtimeCache.toString());
            w.close();
        } catch (IOException e) {
            Log.e(TAG, "运行时缓存保存失败", e);
        }
    }

    private String normalize(String word) {
        return word == null ? "" : word.toLowerCase().trim();
    }
}
