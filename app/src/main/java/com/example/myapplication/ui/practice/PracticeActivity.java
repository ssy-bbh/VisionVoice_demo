package com.example.myapplication.ui.practice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDao;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.PracticeRecord;
import com.example.myapplication.data.ShowcaseItem;
import com.example.myapplication.ml.AudioProcessor;
import com.example.myapplication.ml.PhonemeCache;
import com.example.myapplication.ml.Wav2Vec2Scorer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PracticeActivity extends AppCompatActivity {
    private String currentImagePath = null;
    private static final String TAG = "PracticeActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String SERVER_URL = "http://127.0.0.1:8000/evaluate_pronunciation/";
    private TextView tvFeedbackText;
    // ===== 视图 =====
    private TextToSpeech textToSpeech;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View rippleView;
    private TextView tvWord;
    private TextView tvScore;
    private TextView tvModeLabel;
    private LinearLayout llPhonemeContainer;
    private Switch switchOffline;

    // ===== 录音 =====
    private boolean isRecording = false;
    private String targetWord;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;

    // ===== 端侧模型 =====
    private Wav2Vec2Scorer wav2Vec2Scorer;
    private PhonemeCache phonemeCache;
    private boolean isOfflineMode = false;
    private boolean isModelReady = false;
    private String cachedPhonemeStr = null; // 当前单词的音素字符串

    // ===== 网络 =====
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        tvFeedbackText = findViewById(R.id.tvFeedbackText);
            // 初始化视图
        tvWord      = findViewById(R.id.tvTargetWord);
        rippleView  = findViewById(R.id.viewRipple);
        llPhonemeContainer = findViewById(R.id.llPhonemeContainer);
        tvScore     = findViewById(R.id.tvScore);
        switchOffline = findViewById(R.id.switchOfflineMode);
        tvModeLabel   = findViewById(R.id.tvModeLabel);

        View bottomSheet = findViewById(R.id.bottomSheetFeedback);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // 初始化音素缓存
        phonemeCache = new PhonemeCache(this);

        // 获取目标单词
        targetWord = getIntent().getStringExtra("extra_word");
        if (targetWord == null) targetWord = "apple";
        tvWord.setText(targetWord);
        currentImagePath = getIntent().getStringExtra("extra_image_path");
        // 获取音标：PhonemeCache.get() 已整合运行时缓存 + CMU Dict，直接调用
        TextView tvPhonetic = findViewById(R.id.tvPhonetic);
        String phonemes = phonemeCache.get(targetWord);
        if (phonemes != null) {
            cachedPhonemeStr = phonemes;
            tvPhonetic.setText("/" + phonemes + "/");
            Log.d(TAG, "音素命中：" + targetWord + " → " + phonemes);
        } else {
            tvPhonetic.setText("加载中...");
        }
        fetchPhonetics(targetWord); // 后台请求后端，更新为 phonemizer 结果并写入缓存

        // ... 在 findViewById 之后 ...
        ImageView ivTargetImage = findViewById(R.id.ivTargetImage);

// 1. 获取 Intent 传来的图片数据
        String imagePath = getIntent().getStringExtra("extra_image_path"); // 接收实时画面截图
        android.net.Uri imageUri = getIntent().getData(); // 接收相册 URI（注意这里改用 getData 了）

// 2. 加载图片并清除灰色 Tint
        if (imageUri != null) {
            ivTargetImage.setImageURI(imageUri);
            ivTargetImage.setImageTintList(null); // 核心：移除灰色遮罩
        } else if (imagePath != null) {
            ivTargetImage.setImageBitmap(android.graphics.BitmapFactory.decodeFile(imagePath));
            ivTargetImage.setImageTintList(null); // 核心：移除灰色遮罩
        }
        // TTS
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS 不支持该语言");
                }
            }
        });
        findViewById(R.id.btnTTS).setOnClickListener(v -> {
            if (targetWord != null && !targetWord.isEmpty()) {
                textToSpeech.speak(targetWord, TextToSpeech.QUEUE_FLUSH, null, null);
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            }
        });

        // 录音路径
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/user_record.m4a";

        // 按钮
        findViewById(R.id.btnRecord).setOnClickListener(v -> handleRecordClick());
        findViewById(R.id.btnNextObject).setOnClickListener(v -> finish());
        findViewById(R.id.btnClosePractice).setOnClickListener(v -> finish());

        // ===== 离线模式开关 =====
        // ===== 离线模式开关 =====
        // 初始禁用，模型就绪后才能拨动
        switchOffline.setEnabled(false);
        switchOffline.setChecked(false);
        updateModeLabel(false);
        switchOffline.setOnCheckedChangeListener((btn, isChecked) -> {
            isOfflineMode = isChecked;
            updateModeLabel(isChecked);
        });

        // 后台预加载模型，加载完成才解锁开关
        preloadModelAsync();
    }

    // ===== 离线模式 UI =====

    private void updateModeLabel(boolean offline) {
        if (tvModeLabel == null) return;
        if (offline) {
            tvModeLabel.setText("🔒 离线模式");
            tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        } else {
            tvModeLabel.setText("☁️ 在线模式");
            tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
        }
    }

    // ===== 模型加载 =====

    /** 后台静默预加载，不影响 UI */
    private void preloadModelAsync() {
        new Thread(() -> {
            try {
                Log.i(TAG, "后台预加载 Wav2Vec2 模型...");
                runOnUiThread(() -> {
                    tvModeLabel.setText("☁️ 在线模式（加载中...）");
                    tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                });

                wav2Vec2Scorer = new Wav2Vec2Scorer(this);
                isModelReady = true;

                Log.i(TAG, "✅ 模型预加载完成");
                runOnUiThread(() -> {
                    switchOffline.setEnabled(true);  // ✅ 解锁开关
                    tvModeLabel.setText("☁️ 在线模式（离线可用）");
                    tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
                    // 如果音标还是"加载中..."，说明后端也没返回，用缓存或占位符
                    TextView tvPhonetic = findViewById(R.id.tvPhonetic);
                    if ("加载中...".equals(tvPhonetic.getText().toString())) {
                        // CMU Dict 后台加载完成后再查一次
                        String p = phonemeCache.get(targetWord);
                        if (p != null) {
                            cachedPhonemeStr = p;
                            tvPhonetic.setText("/" + p + "/");
                        } else {
                            tvPhonetic.setText("/.../");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "模型预加载失败", e);
                runOnUiThread(() -> {
                    switchOffline.setEnabled(false);  // 加载失败，保持禁用
                    tvModeLabel.setText("☁️ 在线模式（离线不可用）");
                    tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                });
            }
        }).start();
    }

    // ===== 录音逻辑 =====

    private void handleRecordClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        if (!isRecording) {
            startRecording();
        } else {
            isRecording = false;
            rippleView.animate().cancel();
            rippleView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "正在评估发音...", Toast.LENGTH_SHORT).show();
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(this::stopRecordingAndEvaluate, 500);
        }
    }

    private void startRecording() {
        llPhonemeContainer.removeAllViews();
        tvScore.setText("--%");
        tvScore.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            rippleView.setVisibility(View.VISIBLE);
            rippleView.animate().scaleX(1.5f).scaleY(1.5f).setDuration(1000).withLayer().start();
            Toast.makeText(this, "正在录音，点击结束...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "录音准备失败", e);
            Toast.makeText(this, "录音设备启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndEvaluate() {
        // 停止录音
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (RuntimeException ignored) {}
            mediaRecorder.release();
            mediaRecorder = null;
        }

        File audioFile = new File(audioFilePath);

        // 根据模式选择评估方式
        if (isOfflineMode && isModelReady) {
            evaluateOnDevice(targetWord, audioFile);
        } else {
            if (isOfflineMode && !isModelReady) {
                Toast.makeText(this, "模型未就绪，已切换到在线模式", Toast.LENGTH_SHORT).show();
                isOfflineMode = false;
                runOnUiThread(() -> {
                    switchOffline.setChecked(false);
                    updateModeLabel(false);
                });
            }
            evaluatePronunciation(targetWord, audioFile);
        }
    }

    // ===== 端侧评估 =====

    private void evaluateOnDevice(String word, File audioFile) {
        new Thread(() -> {
            try {
                Log.i(TAG, "🔒 端侧评估开始：" + word);
                long t0 = System.currentTimeMillis();

                // 1. 加载音频
                float[] audioData = AudioProcessor.loadAndPreprocess(audioFile);
                Log.d(TAG, "音频加载完成：" + audioData.length + " 采样点");

                // 2. 静音检测
                if (AudioProcessor.isSilent(audioData)) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "⚠️ 未检测到声音，请靠近麦克风重试", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 3. 标准音素：直接从 PhonemeCache 取（已整合运行时缓存 + CMU Dict）
                String refPhonemes = phonemeCache.get(word);
                if (refPhonemes == null || refPhonemes.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "⚠️ 该词音素未收录，请联网后重试", Toast.LENGTH_SHORT).show());
                    return;
                }
                Log.d(TAG, "标准音素：" + refPhonemes);

                // 4. 端侧评分（传入音素字符串）
                Wav2Vec2Scorer.PronunciationScore result = wav2Vec2Scorer.score(refPhonemes, audioData);
                long elapsed = System.currentTimeMillis() - t0;
                Log.i(TAG, "✅ 端侧评估完成，得分=" + result.score + "，耗时=" + elapsed + "ms");

                // 3. 转换为 UI 所需格式
                JSONArray refArr  = new JSONArray();
                JSONArray userArr = new JSONArray();
                JSONArray fbArr   = new JSONArray();

                int maxLen = Math.max(result.referencePhonemes.size(), result.userPhonemes.size());
                List<String> refs  = result.referencePhonemes;
                List<String> users = result.userPhonemes;
                List<String> fbs   = result.feedback;

                for (int i = 0; i < maxLen; i++) {
                    refArr.put(i < refs.size()  ? refs.get(i)  : "-");
                    userArr.put(i < users.size() ? users.get(i) : "-");
                    fbArr.put(i < fbs.size()     ? fbs.get(i)   : "Deletion");
                }

                // 4. 更新 UI
                runOnUiThread(() -> {
                    updateUIWithFeedback(refArr, userArr, fbArr);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    // 用端侧计算的得分直接覆盖
                    tvScore.setText(result.score + "%");
                    colorScore(result.score);
                });

            } catch (Exception e) {
                Log.e(TAG, "端侧评估失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "端侧评估失败，切换到在线模式重试", Toast.LENGTH_SHORT).show();
                    isOfflineMode = false;
                    switchOffline.setChecked(false);
                    updateModeLabel(false);
                    evaluatePronunciation(word, audioFile);
                });
            }
        }).start();
    }

    // ===== 在线评估（原有逻辑，保持不变）=====

    private void evaluatePronunciation(String word, File file) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("target_word", word)
                .addFormDataPart("audio_file", file.getName(),
                        RequestBody.create(MediaType.parse("audio/mp4"), file))
                .build();

        Request request = new Request.Builder().url(SERVER_URL).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "网络请求失败", e);
                runOnUiThread(() ->
                        Toast.makeText(PracticeActivity.this,
                                "连接服务器失败，请检查网络和 IP", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "后端返回：" + responseData);
                        JSONObject json = new JSONObject(responseData);
                        JSONArray refPhonemes  = json.getJSONArray("reference_phonemes");
                        JSONArray userPhonemes = json.getJSONArray("user_phonemes");
                        JSONArray feedback     = json.getJSONArray("feedback");
                        runOnUiThread(() -> {
                            updateUIWithFeedback(refPhonemes, userPhonemes, feedback);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 解析失败", e);
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(PracticeActivity.this,
                                    "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // ===== UI 渲染（原有逻辑，保持不变）=====

    private void updateUIWithFeedback(JSONArray refPhonemes, JSONArray userPhonemes, JSONArray feedback) {
        LinearLayout llContainer = findViewById(R.id.llPhonemeContainer);
        llContainer.removeAllViews();
        try {
            float totalScore = 0f;
            int totalCount = refPhonemes.length();
            int validPhonemeCount = 0;

            for (int i = 0; i < totalCount; i++) {
                String ref  = refPhonemes.getString(i);
                String user = userPhonemes.getString(i);
                String fb   = feedback.getString(i);

                if (!ref.equals("-")) validPhonemeCount++;

                LinearLayout pairLayout = new LinearLayout(this);
                pairLayout.setOrientation(LinearLayout.VERTICAL);
                pairLayout.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams pairParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                pairParams.setMargins(0, 0, 16, 0);
                pairLayout.setLayoutParams(pairParams);

                TextView tvRef = new TextView(this);
                tvRef.setText(ref.equals("-") ? " " : ref);
                tvRef.setTextSize(16);
                tvRef.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                tvRef.setGravity(android.view.Gravity.CENTER);
                tvRef.setPadding(0, 0, 0, 4);

                TextView tvUser = new TextView(this);
                tvUser.setText(user.equals("-") ? "×" : user);
                tvUser.setTextSize(20);
                tvUser.setPadding(24, 12, 24, 12);
                tvUser.setTypeface(null, android.graphics.Typeface.BOLD);
                tvUser.setGravity(android.view.Gravity.CENTER);

                if (fb.equals("Match")) {
                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_correct);
                    tvUser.setTextColor(ContextCompat.getColor(this, R.color.success_green));
                    totalScore += 1.0f;
                } else if (fb.startsWith("Flaw:")) {
                    String[] parts = fb.split(":");
                    String reason = parts.length > 1 ? parts[1] : "发音有瑕疵";
                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_warning);
                    tvUser.setTextColor(android.graphics.Color.parseColor("#F57C00"));
                    totalScore += 0.6f;
                    tvUser.setOnClickListener(v ->
                            Toast.makeText(this, "AI诊断: " + reason, Toast.LENGTH_SHORT).show());
                } else {
                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_error);
                    tvUser.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                }

                pairLayout.addView(tvRef);
                pairLayout.addView(tvUser);
                llContainer.addView(pairLayout);
            }

            if (validPhonemeCount > 0) {
                float rawAccuracy = totalScore / totalCount;
                int displayScore;
                if (rawAccuracy >= 0.8f) {
                    displayScore = (int)(90 + (rawAccuracy - 0.8f) * 50);
                } else if (rawAccuracy >= 0.5f) {
                    displayScore = (int)(60 + (rawAccuracy - 0.5f) * 100);
                } else {
                    displayScore = (int)(rawAccuracy * 120);
                }
                displayScore = Math.max(0, Math.min(100, displayScore));
                tvScore.setText(displayScore + "%");
                colorScore(displayScore);
                int finalScore = displayScore;

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        // 获取数据库操作接口 (DAO)
                        AppDao dao = AppDatabase.getInstance(PracticeActivity.this).appDao();

                        // ================= 1. 记录这次练习流水 (你已经写好的) =================
                        PracticeRecord record = new PracticeRecord(
                                targetWord,
                                finalScore,
                                System.currentTimeMillis(),
                                currentImagePath
                        );
                        dao.insertRecord(record);
                        android.util.Log.d("VISION_DEBUG", "✅ 成功保存记录流水: " + targetWord + " 得分: " + finalScore);

                        // ================= 2. 展柜解锁与“擦亮”逻辑 (新增) =================
                        ShowcaseItem item = dao.getShowcaseItemByWord(targetWord);

                        if (item != null) {
                            // 说明展柜里预设了这个物品！
                            if (!item.isUnlocked) {
                                // 情况 A：首次解锁该物品触发隐藏成就！
                                item.isUnlocked = true;
                                item.unlockTime = System.currentTimeMillis();
                                item.highestScore = finalScore;
                                item.bestImagePath = currentImagePath;
                                item.lastReviewedTime = System.currentTimeMillis();

                                dao.updateShowcaseItem(item);
                                android.util.Log.d("VISION_DEBUG", "🔓 恭喜！首次解锁展品: " + targetWord);

                                // TODO: 之后我们可以在这里用 runOnUiThread 弹出一个酷炫的“解锁成功”对话框
                            } else {
                                // 情况 B：物品之前已经解锁过了
                                item.lastReviewedTime = System.currentTimeMillis(); // 更新时间，清除“蒙尘”状态

                                if (finalScore > item.highestScore) {
                                    // 刷新了历史最高分！替换最美截图！
                                    item.highestScore = finalScore;
                                    if (currentImagePath != null) {
                                        item.bestImagePath = currentImagePath;
                                    }
                                    android.util.Log.d("VISION_DEBUG", "🏆 展品 [" + targetWord + "] 打破最高分纪录！");
                                } else {
                                    android.util.Log.d("VISION_DEBUG", "✨ 展品 [" + targetWord + "] 成功复习并擦亮！");
                                }

                                dao.updateShowcaseItem(item);
                            }
                        } else {
                            // 如果查不到，说明这个词不在我们的“官方收集图鉴”里，只记流水即可
                            android.util.Log.d("VISION_DEBUG", "ℹ️ 单词 [" + targetWord + "] 属于额外词汇，不计入官方展柜。");
                        }

                    } catch (Exception e) {
                        android.util.Log.e("VISION_DEBUG", "❌ 数据库保存失败!!!", e);
                    }
                });

            }
        } catch (Exception e) {
            Log.e(TAG, "UI 更新异常", e);
        }
    }

    private void colorScore(int score) {
        if (score >= 80) {
            tvScore.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            tvFeedbackText.setText("Excellent!"); // 优秀
            tvFeedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        } else if (score >= 60) {
            tvScore.setTextColor(android.graphics.Color.parseColor("#F57C00"));
            tvFeedbackText.setText("Good Try!"); // 良好
            tvFeedbackText.setTextColor(android.graphics.Color.parseColor("#F57C00"));
        } else {
            tvScore.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            tvFeedbackText.setText("Keep Practicing!"); // 加油
            tvFeedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }
    }

    // ===== 音标获取 =====

    private void fetchPhonetics(String word) {
        String url = "http://127.0.0.1:8000/get_phonetics/?word=" + word;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    TextView tvPhonetic = findViewById(R.id.tvPhonetic);
                    if (cachedPhonemeStr != null) {
                        // 已有缓存（本次进页面时命中），保持显示
                        tvPhonetic.setText("/" + cachedPhonemeStr + "/");
                    } else {
                        // 完全没有缓存，显示占位符
                        tvPhonetic.setText("/.../");
                        Log.w(TAG, "后端不可用且无缓存：" + word);
                    }
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String phonetics = json.getString("phonetics");
                        // 去掉斜杠存入缓存和内存
                        String phonemeStr = phonetics.replaceAll("^/|/$", "");
                        cachedPhonemeStr = phonemeStr;
                        phonemeCache.put(word, phonemeStr); // 写入本地缓存
                        runOnUiThread(() -> {
                            TextView tvPhonetic = findViewById(R.id.tvPhonetic);
                            tvPhonetic.setText(phonetics);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "解析音标 JSON 失败", e);
                    }
                }
            }
        });
    }

    // ===== 生命周期 =====

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (wav2Vec2Scorer != null) {
            wav2Vec2Scorer.close();
        }
    }
}
