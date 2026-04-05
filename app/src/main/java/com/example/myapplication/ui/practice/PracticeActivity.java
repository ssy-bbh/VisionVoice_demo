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
import com.example.myapplication.ml.ModelManager;
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

    // 🚨 核心改动：抛弃 MediaRecorder，使用底层 API
    private android.media.AudioRecord audioRecord;
    private Thread recordingThread;
    private String audioFilePath; // 路径后缀从 .m4a 变成 .pcm

    // ===== 端侧模型 =====
    private Wav2Vec2Scorer wav2Vec2Scorer;
    private PhonemeCache phonemeCache;
    // 🚨 核心设定：默认开启端侧离线模型
    private boolean isOfflineMode = true;
    private boolean isModelReady = false;
    private String cachedPhonemeStr = null;

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

        // 获取音标
        TextView tvPhonetic = findViewById(R.id.tvPhonetic);
        String phonemes = phonemeCache.get(targetWord);
        if (phonemes != null) {
            cachedPhonemeStr = phonemes;
            tvPhonetic.setText("/" + phonemes + "/");
            Log.d(TAG, "音素命中：" + targetWord + " → " + phonemes);
        } else {
            tvPhonetic.setText("加载中...");
        }
        fetchPhonetics(targetWord);

        // 处理 AR 截帧路径 和 相册上传 Uri
        ImageView ivTargetImage = findViewById(R.id.ivTargetImage);
        currentImagePath = getIntent().getStringExtra("extra_image_path");
        android.net.Uri imageUri = getIntent().getData();

        if (imageUri != null) {
            ivTargetImage.setImageURI(imageUri);
            ivTargetImage.setImageTintList(null);

            // 将相册图偷偷拷贝进沙盒，转为永久绝对路径
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    String fileName = "/album_capture_" + System.currentTimeMillis() + ".jpg";
                    File newFile = new File(getFilesDir(), fileName);
                    java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream != null) {
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(newFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.close();
                        inputStream.close();
                        // 赋予永久路径，供下方写入数据库使用
                        currentImagePath = newFile.getAbsolutePath();
                        Log.d("VISION_DEBUG", "相册URI已转换为永久本地文件: " + currentImagePath);
                    }
                } catch (Exception e) {
                    Log.e("VISION_DEBUG", "转换相册URI失败", e);
                }
            });

        } else if (currentImagePath != null) {
            ivTargetImage.setImageBitmap(android.graphics.BitmapFactory.decodeFile(currentImagePath));
            ivTargetImage.setImageTintList(null);
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

        // 录音路径：从 m4a 有损格式改为 pcm 裸流格式
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/user_record.pcm";

        // 按钮
        findViewById(R.id.btnRecord).setOnClickListener(v -> handleRecordClick());
        findViewById(R.id.btnNextObject).setOnClickListener(v -> finish());
        findViewById(R.id.btnClosePractice).setOnClickListener(v -> finish());

        // ===== 离线模式开关 =====
        switchOffline.setEnabled(false); // 加载完之前不可拨动
        switchOffline.setChecked(true);  // 默认开启
        updateModeLabel(true);
        switchOffline.setOnCheckedChangeListener((btn, isChecked) -> {
            isOfflineMode = isChecked;
            updateModeLabel(isChecked);
        });

        // 后台预加载模型
        preloadModelAsync();
    }

    // ===== 离线模式 UI =====
    private void updateModeLabel(boolean offline) {
        if (tvModeLabel == null) return;
        if (offline) {
            if (isModelReady) {
                tvModeLabel.setText("🔒 离线模式 (已就绪)");
                tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            } else {
                tvModeLabel.setText("🔒 离线模式 (模型加载中...)");
                tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        } else {
            tvModeLabel.setText("☁️ 在线模式");
            tvModeLabel.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
        }
    }

    // ===== 模型加载 =====
    private void preloadModelAsync() {
        if (ModelManager.isReady()) {
            wav2Vec2Scorer = ModelManager.getScorer();
            isModelReady = true;

            // UI 秒切就绪状态
            switchOffline.setEnabled(true);
            updateModeLabel(true);
            Log.i(TAG, "⚡ 模型秒开成功！");
        } else {
            // 万一用户手速极快，刚开App就扫，模型还没加载完
            Log.w(TAG, "模型还在全局加载中，稍后重试或降级");
            tvModeLabel.setText("🔒 离线模式 (全局加载中...)");
        }
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

        int sampleRate = 16000;
        int channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;

        int minBufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        int bufferSize = Math.max(minBufferSize * 4, 8192);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            audioRecord = new android.media.AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
            );

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(() -> {
                writeAudioDataToFile(bufferSize);
            }, "AudioRecorder_Thread");
            recordingThread.start();

            rippleView.setVisibility(View.VISIBLE);
            rippleView.animate().scaleX(1.5f).scaleY(1.5f).setDuration(1000).withLayer().start();
            Toast.makeText(this, "正在录制无损音频...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "AudioRecord 初始化失败", e);
            Toast.makeText(this, "麦克风被占用或权限拒绝", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeAudioDataToFile(int bufferSize) {
        byte[] data = new byte[bufferSize];
        java.io.FileOutputStream os = null;

        try {
            os = new java.io.FileOutputStream(audioFilePath);
        } catch (java.io.FileNotFoundException e) {
            Log.e(TAG, "无法创建 PCM 临时文件", e);
            return;
        }

        while (isRecording) {
            int read = audioRecord.read(data, 0, bufferSize);
            if (read > 0) {
                try {
                    os.write(data, 0, read);
                } catch (IOException e) {
                    Log.e(TAG, "PCM 写入异常", e);
                }
            }
        }

        try {
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndEvaluate() {
        isRecording = false;

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "停止录音异常", e);
            } finally {
                audioRecord.release();
                audioRecord = null;
                recordingThread = null;
            }
        }

        File audioFile = new File(audioFilePath);

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

                float[] audioData = AudioProcessor.loadAndPreprocess(audioFile);

                if (AudioProcessor.isSilent(audioData)) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "⚠️ 未检测到声音，请靠近麦克风重试", Toast.LENGTH_SHORT).show());
                    return;
                }

                String refPhonemes = phonemeCache.get(word);
                if (refPhonemes == null || refPhonemes.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "⚠️ 该词音素未收录，请联网后重试", Toast.LENGTH_SHORT).show());
                    return;
                }

                Wav2Vec2Scorer.PronunciationScore result = wav2Vec2Scorer.score(refPhonemes, audioData);

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

                runOnUiThread(() -> {
                    // 🚨 传入真正的底层算分 result.score
                    updateUIWithFeedback(refArr, userArr, fbArr, result.score);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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

    // ===== 在线评估 =====
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
                        JSONObject json = new JSONObject(responseData);
                        JSONArray refPhonemes  = json.getJSONArray("reference_phonemes");
                        JSONArray userPhonemes = json.getJSONArray("user_phonemes");
                        JSONArray feedback     = json.getJSONArray("feedback");
                        runOnUiThread(() -> {
                            // 在线模式不传预设分数，使用 -1 走兜底算分逻辑
                            updateUIWithFeedback(refPhonemes, userPhonemes, feedback, -1);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 解析失败", e);
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(PracticeActivity.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // ===== UI 渲染 & 数据库更新 =====
    private void updateUIWithFeedback(JSONArray refPhonemes, JSONArray userPhonemes, JSONArray feedback, int overrideScore) {
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

                // 🌟 核心 UI 净化：如果是环境杂音导致的插入 (Insertion) 或者是没有基准音的对比 (-)，直接跳过不显示！
                if (fb.equals("Insertion") || ref.equals("-")) {
                    continue;
                }

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

            if (validPhonemeCount > 0 || overrideScore >= 0) {
                int finalScore;

                if (overrideScore >= 0) {
                    // 🌟 采用底层算法给出的最高分！
                    finalScore = overrideScore;
                } else {
                    float rawAccuracy = totalScore / validPhonemeCount;
                    int displayScore;
                    if (rawAccuracy >= 0.8f) {
                        displayScore = (int)(90 + (rawAccuracy - 0.8f) * 50);
                    } else if (rawAccuracy >= 0.5f) {
                        displayScore = (int)(60 + (rawAccuracy - 0.5f) * 100);
                    } else {
                        displayScore = (int)(rawAccuracy * 120);
                    }
                    finalScore = Math.max(0, Math.min(100, displayScore));
                }

                tvScore.setText(finalScore + "%");
                colorScore(finalScore);

                // 数据库保存逻辑
                int finalScoreForDb = finalScore;
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        AppDao dao = AppDatabase.getInstance(PracticeActivity.this).appDao();

                        PracticeRecord record = new PracticeRecord(
                                targetWord,
                                finalScoreForDb,
                                System.currentTimeMillis(),
                                currentImagePath
                        );
                        dao.insertRecord(record);
                        android.util.Log.d("VISION_DEBUG", "✅ 成功保存记录流水: " + targetWord + " 得分: " + finalScoreForDb);

                        com.example.myapplication.utils.UserStatsManager.INSTANCE.recordPractice(PracticeActivity.this);
                        // ==========================================
                        // 🌟 每日挑战核销逻辑！(及格 >=60 分才算完成)
                        // ==========================================
                        if (finalScoreForDb >= 60) {
                            boolean newlyCompleted = com.example.myapplication.utils.UserStatsManager.INSTANCE.markChallengeCompleted(PracticeActivity.this, targetWord);
                            if (newlyCompleted) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PracticeActivity.this, "🎉 太棒了！你达成了一个每日挑战: " + targetWord + "!", Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                        ShowcaseItem item = dao.getShowcaseItemByWord(targetWord);

                        if (item != null) {
                            if (!item.isUnlocked) {
                                item.isUnlocked = true;
                                item.unlockTime = System.currentTimeMillis();
                                item.highestScore = finalScoreForDb;
                                item.bestImagePath = currentImagePath;
                                item.lastReviewedTime = System.currentTimeMillis();

                                dao.updateShowcaseItem(item);
                                android.util.Log.d("VISION_DEBUG", "🔓 恭喜！首次解锁展品: " + targetWord);
                            } else {
                                item.lastReviewedTime = System.currentTimeMillis();

                                if (finalScoreForDb > item.highestScore) {
                                    item.highestScore = finalScoreForDb;
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
            tvFeedbackText.setText("Excellent!");
            tvFeedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        } else if (score >= 60) {
            tvScore.setTextColor(android.graphics.Color.parseColor("#F57C00"));
            tvFeedbackText.setText("Good Try!");
            tvFeedbackText.setTextColor(android.graphics.Color.parseColor("#F57C00"));
        } else {
            tvScore.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            tvFeedbackText.setText("Keep Practicing!");
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
                        tvPhonetic.setText("/" + cachedPhonemeStr + "/");
                    } else {
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
                        String phonemeStr = phonetics.replaceAll("^/|/$", "");
                        cachedPhonemeStr = phonemeStr;
                        phonemeCache.put(word, phonemeStr);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 1. 释放 TTS 资源
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        // 2. 核心修复：安全终止后台录音线程与释放 AudioRecord 底层硬件
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "停止 AudioRecord 异常", e);
            } finally {
                audioRecord.release();
                audioRecord = null;
            }
        }
    }
}