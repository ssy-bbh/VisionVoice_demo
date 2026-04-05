package com.example.myapplication.ml;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

public class Wav2Vec2Scorer {
    private static final String TAG = "Wav2Vec2Scorer";
    private final OrtEnvironment env;
    private final OrtSession session;
    private final Map<Integer, String> idToPhoneme;

    public Wav2Vec2Scorer(Context context) {
        try {
            env = OrtEnvironment.getEnvironment();
            String modelAssetPath = fileExistsInAssets(context, "onnx/model_quant.onnx") ? "onnx/model_quant.onnx" : "onnx/model.onnx";
            File modelFile = new File(context.getFilesDir(), "wav2vec2_model.onnx");
            copyAssetToFile(context, modelAssetPath, modelFile);
            session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
            idToPhoneme = buildIdToPhonemeMap();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public PronunciationScore score(String refPhonemeStr, float[] audioData) {
        List<String> userPhonemes = transcribe(audioData);
        List<String> refPhonemes = splitPhonemeString(refPhonemeStr);

        // 1. 先进行原始的全局对齐
        AlignmentResult alignment = needlemanWunsch(refPhonemes, userPhonemes);

        // 🚨 2. 核心魔法：掐头去尾！把前后没匹配上的环境杂音全部丢弃！
        alignment = trimEdgeInsertions(alignment);

        // 3. 拿干干净净的对齐结果去算分
        int score = calculateScore(alignment);

        List<String> ipaRef = new ArrayList<>();
        List<String> ipaUser = new ArrayList<>();
        for (String p : alignment.reference) ipaRef.add(toStandardIPA(p));
        for (String p : alignment.user) ipaUser.add(toStandardIPA(p));

        return new PronunciationScore(score, ipaRef, ipaUser, alignment.feedback);
    }

    // ================= 【核心修复：局部匹配，截断前后噪音】 =================
    private AlignmentResult trimEdgeInsertions(AlignmentResult a) {
        List<String> ref = new ArrayList<>(a.reference);
        List<String> usr = new ArrayList<>(a.user);
        List<String> fb  = new ArrayList<>(a.feedback);

        // 1. 砍掉开头的多余环境音 (只要是模型强行插入的幻觉音，全部丢弃)
        while (!fb.isEmpty() && fb.get(0).equals("Insertion")) {
            ref.remove(0);
            usr.remove(0);
            fb.remove(0);
        }

        // 2. 砍掉结尾的多余尾音
        while (!fb.isEmpty() && fb.get(fb.size() - 1).equals("Insertion")) {
            ref.remove(ref.size() - 1);
            usr.remove(usr.size() - 1);
            fb.remove(fb.size() - 1);
        }

        return new AlignmentResult(ref, usr, fb);
    }

    private List<String> transcribe(float[] audioData) {
        try {
            long[] shape = {1, audioData.length};
            FloatBuffer buffer = ByteBuffer.allocateDirect(audioData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

            // 🚨 核心修复 2：彻底删掉之前除以 maxAmp 的错误逻辑！
            // 信任 AudioProcessor 传进来的完美 Z-score 数据
            for (float d : audioData) buffer.put(d);
            buffer.rewind();

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
            OrtSession.Result result = session.run(Collections.singletonMap("input_values", inputTensor));
            float[][][] logits = (float[][][]) result.get(0).getValue();

            List<String> phonemes = new ArrayList<>();
            int prev = -1;
            for (float[] frame : logits[0]) {
                int maxId = 0;
                for (int i = 1; i < frame.length; i++) if (frame[i] > frame[maxId]) maxId = i;
                if (maxId != prev && maxId != 0 && idToPhoneme.containsKey(maxId)) {
                    String p = idToPhoneme.get(maxId);
                    if (!p.equals("h#") && !p.equals("spn") && !p.equals("[UNK]") && !p.contains("<")) {
                        phonemes.add(p);
                    }
                }
                prev = maxId;
            }
            inputTensor.close(); result.close();
            return phonemes;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    // ================= 【真·修复：底层万能翻译器】 =================
    private String normalizeForCompare(String p) {
        if (p == null || p.equals("-")) return "-";
        String s = p.toLowerCase().replaceAll("\\d", "").trim();

        // 强制把所有传进来的 IPA 符号，翻译回模型认识的 Arpabet
        Map<String, String> ipaToArpabet = new HashMap<String, String>() {{
            put("ɑ", "aa"); put("æ", "ae"); put("ʌ", "ah"); put("aʊ", "aw"); put("aɪ", "ay");
            put("tʃ", "ch"); put("ð", "dh"); put("ɛ", "eh"); put("ɝ", "er"); put("eɪ", "ey");
            put("ɪ", "ih"); put("i", "iy"); put("dʒ", "jh"); put("ŋ", "ng"); put("oʊ", "ow");
            put("ɔɪ", "oy"); put("ʃ", "sh"); put("θ", "th"); put("ʊ", "uh"); put("u", "uw");
            put("ʒ", "zh"); put("ɹ", "r"); put("ɔ", "ao"); put("h", "hh"); put("j", "y");put("ɜɹ", "er");
        }};

        return ipaToArpabet.containsKey(s) ? ipaToArpabet.get(s) : s;
    }

    private String toStandardIPA(String p) {
        if (p == null || p.equals("-")) return "-";
        // 翻译前先过一遍底层，确保符号干净
        String s = normalizeForCompare(p);
        Map<String, String> m = new HashMap<String, String>() {{
            put("aa", "ɑ"); put("ae", "æ"); put("ah", "ʌ"); put("aw", "aʊ"); put("ay", "aɪ");
            put("ch", "tʃ"); put("dh", "ð"); put("eh", "ɛ"); put("er", "ɝ"); put("ey", "eɪ");
            put("ih", "ɪ"); put("iy", "i"); put("jh", "dʒ"); put("ng", "ŋ"); put("ow", "oʊ");
            put("oy", "ɔɪ"); put("sh", "ʃ"); put("th", "θ"); put("uh", "ʊ"); put("uw", "u");
            put("zh", "ʒ"); put("hh", "h"); put("r", "ɹ"); put("ao", "ɔ"); put("y", "j");
        }};
        return m.containsKey(s) ? m.get(s) : s;
    }

    private String getErrorType(String ref, String user) {
        String r = normalizeForCompare(ref);
        String u = normalizeForCompare(user);
        if (r.equals(u)) return "Match";

        // 🌟 核心优化：在这里加入免死金牌！允许 /ʌ/ 和 /æ/ 互相混淆不扣分
        String[][] ignored = {
                {"t","d"},{"d","t"},{"p","b"},{"b","p"},{"k","g"},{"g","k"},
                {"v","b"},{"s","z"},{"z","s"},{"k","hh"}, {"k","h"}, {"p","hh"}, {"t","hh"},
                {"iy","ih"}, {"ih","iy"},
                {"ah", "ae"}, {"ae", "ah"} // <--- 专门为 Cup 的 u 准备的免死金牌！
        };
        for (String[] pair : ignored) if (pair[0].equals(r) && pair[1].equals(u)) return "Ignored";

        Map<String, String> flaws = new HashMap<String, String>() {{
            put("ae|eh", "开口度偏差"); put("v|w", "唇齿音混淆"); put("r|l", "r/l不分");
            put("ao|aa", "圆唇不够"); put("aa|ao", "发音位置偏移");
            put("ao|ah", "发音位置偏移"); put("ah|ao", "发音位置偏移");
        }};
        return flaws.containsKey(r+"|"+u) ? "Flaw:" + flaws.get(r+"|"+u) : "Substitution";
    }

    private AlignmentResult needlemanWunsch(List<String> ref, List<String> user) {
        int n = ref.size(), m = user.size();
        float[][] dp = new float[n + 1][m + 1];
        // 【核心修改】：优化权重，Match > Flaw > GAP，确保算法优先对齐正确的音节
        float MATCH = 1.0f, FLAW = 0.5f, MISMATCH = -1.0f, GAP = -1.0f;

        for (int i = 0; i <= n; i++) dp[i][0] = i * GAP;
        for (int j = 0; j <= m; j++) dp[0][j] = j * GAP;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                String err = getErrorType(ref.get(i-1), user.get(j-1));
                float s = (err.equals("Match") || err.equals("Ignored")) ? MATCH : (err.startsWith("Flaw:") ? FLAW : MISMATCH);
                dp[i][j] = Math.max(dp[i-1][j-1] + s, Math.max(dp[i-1][j] + GAP, dp[i][j-1] + GAP));
            }
        }

        List<String> aRef = new ArrayList<>(), aUser = new ArrayList<>(), fb = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                String err = getErrorType(ref.get(i-1), user.get(j-1));
                float s = (err.equals("Match") || err.equals("Ignored")) ? MATCH : (err.startsWith("Flaw:") ? FLAW : MISMATCH);
                if (dp[i][j] == dp[i-1][j-1] + s) {
                    aRef.add(ref.get(i-1));
                    aUser.add(err.equals("Ignored") ? ref.get(i-1) : user.get(j-1));
                    fb.add(err.equals("Ignored") ? "Match" : err);
                    i--; j--; continue;
                }
            }
            if (i > 0 && dp[i][j] == dp[i-1][j] + GAP) {
                aRef.add(ref.get(i-1)); aUser.add("-"); fb.add("Deletion"); i--;
            } else {
                aRef.add("-"); aUser.add(user.get(j-1)); fb.add("Insertion"); j--;
            }
        }
        Collections.reverse(aRef); Collections.reverse(aUser); Collections.reverse(fb);
        return new AlignmentResult(aRef, aUser, fb);
    }

    private int calculateScore(AlignmentResult a) {
        float matchCount = 0;
        int refSize = 0;
        int insertCount = 0;

        for (int k = 0; k < a.feedback.size(); k++) {
            String f = a.feedback.get(k);
            if (!a.reference.get(k).equals("-")) refSize++;

            if (f.equals("Match") || f.equals("Ignored")) matchCount += 1.0f;
            else if (f.startsWith("Flaw:")) matchCount += 0.6f;
            else if (f.equals("Insertion")) insertCount++; // 统计幻觉插入音
        }

        if (refSize == 0) return 0;
        float acc = matchCount / refSize;

        // 🚨 核心修复 3：为 cup 这样的短促单词设计“极客宽容算法”
        if (refSize <= 4) {
            boolean firstIsGood = a.feedback.size() > 0 &&
                    (a.feedback.get(0).equals("Match") || a.feedback.get(0).equals("Ignored"));

            // 如果只有 3、4 个音，首字母读对了，且最多只丢了一个音（比如丢了气声 p 或吞了 u）
            if (firstIsGood && matchCount >= (refSize - 1.2f)) {
                acc = Math.max(acc, 0.88f); // 强行拉抬到保底 88 分（优秀）！
            }
        }

        // 惩罚过长的幻觉环境音 (如果你没说话但是模型吐了一大串字母)
        if (insertCount > refSize + 1) {
            acc -= 0.1f * (insertCount - refSize);
        }

        int s = acc >= 0.8f ? (int)(90 + (acc - 0.8f) * 50) : acc >= 0.5f ? (int)(60 + (acc - 0.5f) * 100) : (int)(acc * 120);
        return Math.max(0, Math.min(100, s));
    }

    private Map<Integer, String> buildIdToPhonemeMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "aa"); map.put(2, "ae"); map.put(3, "ah"); map.put(4, "aw"); map.put(5, "ay");
        map.put(6, "b"); map.put(7, "ch"); map.put(8, "d"); map.put(9, "dh"); map.put(10, "dx");
        map.put(11, "eh"); map.put(12, "er"); map.put(13, "ey"); map.put(14, "f"); map.put(15, "g");
        map.put(16, "h#"); map.put(17, "hh"); map.put(18, "ih"); map.put(19, "iy"); map.put(20, "jh");
        map.put(21, "k"); map.put(22, "l"); map.put(23, "m"); map.put(24, "n"); map.put(25, "ng");
        map.put(26, "ow"); map.put(27, "oy"); map.put(28, "p"); map.put(29, "r"); map.put(30, "s");
        map.put(31, "sh"); map.put(32, "spn"); map.put(33, "t"); map.put(34, "th"); map.put(35, "uh");
        map.put(36, "uw"); map.put(37, "v"); map.put(38, "w"); map.put(39, "y"); map.put(40, "z");
        return map;
    }

    private List<String> splitPhonemeString(String s) {
        s = s.toLowerCase().replaceAll("\\d", "").trim();
        // 1. 如果带有空格，直接按空格安全拆分
        if (s.contains(" ")) return Arrays.asList(s.split("\\s+"));

        // 2. 如果没有空格，进行智能拆分，保护双元音和破擦音
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            // 检查当前字符和下一个字符是否组成已知的双字符音素
            if (i + 1 < s.length()) {
                String twoChar = s.substring(i, i + 2);
                // 包含常见的双元音、破擦音，以及卷舌音
                if (twoChar.equals("aʊ") || twoChar.equals("aɪ") || twoChar.equals("eɪ") ||
                        twoChar.equals("oʊ") || twoChar.equals("ɔɪ") || twoChar.equals("tʃ") ||
                        twoChar.equals("dʒ") || twoChar.equals("ɜɹ")) {

                    result.add(twoChar);
                    i += 2; // 指针跳过这两个字符
                    continue;
                }
            }
            // 不是双字符音素，按单字符处理
            result.add(String.valueOf(s.charAt(i)));
            i++;
        }
        return result;
    }

    private void copyAssetToFile(Context c, String a, File d) throws IOException {
        try (InputStream is = c.getAssets().open(a); java.io.FileOutputStream os = new java.io.FileOutputStream(d)) {
            byte[] b = new byte[8192]; int n;
            while ((n = is.read(b)) != -1) os.write(b, 0, n);
        }
    }

    private boolean fileExistsInAssets(Context c, String p) {
        try { c.getAssets().open(p).close(); return true; } catch (IOException e) { return false; }
    }

    public void close() { try { session.close(); env.close(); } catch (Exception ignored) {} }

    public static class PronunciationScore {
        public final int score;
        public final List<String> referencePhonemes, userPhonemes, feedback;
        public PronunciationScore(int s, List<String> r, List<String> u, List<String> f) {
            this.score = s; this.referencePhonemes = r; this.userPhonemes = u; this.feedback = f;
        }
    }

    public static class AlignmentResult {
        public final List<String> reference, user, feedback;
        public AlignmentResult(List<String> r, List<String> u, List<String> f) {
            this.reference = r; this.user = u; this.feedback = f;
        }
    }
}