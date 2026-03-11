package com.example.myapplication.ui.practice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PracticeActivity extends AppCompatActivity {
    // 【新增】：TTS 引擎对象
    private TextToSpeech textToSpeech;
    private static final String TAG = "PracticeActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // 【请注意】这里修改为你运行 Python FastAPI 的电脑的局域网 IP
    // 如果是用 Android Studio 模拟器访问本机后端，通常是 10.0.2.2
    // private static final String SERVER_URL = "http://10.0.2.2:8000/evaluate_pronunciation/";
    // 方案一专属地址
    private static final String SERVER_URL = "http://127.0.0.1:8000/evaluate_pronunciation/";
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View rippleView;
    private TextView tvWord;
    private LinearLayout llPhonemeContainer;
    private TextView tvScore;

    private boolean isRecording = false;
    private String targetWord;

    // 录音相关
    private MediaRecorder mediaRecorder;
    private String audioFilePath;

    // 网络请求相关
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        // 1. 初始化视图
        tvWord = findViewById(R.id.tvTargetWord);
        rippleView = findViewById(R.id.viewRipple);
        llPhonemeContainer = findViewById(R.id.llPhonemeContainer);
        tvScore = findViewById(R.id.tvScore);

        View bottomSheet = findViewById(R.id.bottomSheetFeedback);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // 默认隐藏

        // 2. 获取从识别界面传来的单词
        targetWord = getIntent().getStringExtra("extra_word");
        if (targetWord == null) targetWord = "apple"; // 默认兜底
        tvWord.setText(targetWord);
        // 【新增】：一进页面，立刻把占位符改成“加载中”，并发起请求
        TextView tvPhonetic = findViewById(R.id.tvPhonetic);
        tvPhonetic.setText("加载中...");
        fetchPhonetics(targetWord);
        // 【新增 1】：初始化 TTS 引擎，并设置为美式英语
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // 设置语言为美式英语 (US)
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS不支持该语言或缺少语言包");
                }
            } else {
                Log.e(TAG, "TTS初始化失败");
            }
        });

        // 【新增 2】：绑定喇叭按钮的点击事件
        findViewById(R.id.btnTTS).setOnClickListener(v -> {
            if (targetWord != null && !targetWord.isEmpty()) {
                // QUEUE_FLUSH 意味着如果用户疯狂连按，会打断之前的发音立刻读最新的
                textToSpeech.speak(targetWord, TextToSpeech.QUEUE_FLUSH, null, null);

                // 加个小动画让按钮有反馈感 (可选)
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();
            }
        });
        // 3. 设置录音文件保存路径 (存在 App 的内部缓存目录中)
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/user_record.m4a";

        // 4. 按钮点击事件
        findViewById(R.id.btnRecord).setOnClickListener(v -> handleRecordClick());
        findViewById(R.id.btnNextObject).setOnClickListener(v -> finish());
        findViewById(R.id.btnClosePractice).setOnClickListener(v -> finish());
    }

    /**
     * 处理录音按钮点击逻辑
     */
    private void handleRecordClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        if (!isRecording) {
            startRecording();
        } else {
            // 1. 状态立刻变更为停止，防止用户连点
            isRecording = false;

            // 2. UI 立刻停止动画，给用户“已经按下了”的反馈
            rippleView.animate().cancel();
            rippleView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "正在评估发音...", Toast.LENGTH_SHORT).show();

            // 3. 【核心修复】延迟 500 毫秒再去真正关闭麦克风，把空气中的尾音录完
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::stopRecordingAndSend, 500);
        }
    }

    /**
     * 1. 真实录音逻辑：开始录音
     */
    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 使用 MPEG_4 格式，绝大多数 Python 后端配合 ffmpeg 都能直接读取
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            // UI 动画
            rippleView.setVisibility(View.VISIBLE);
            rippleView.animate().scaleX(1.5f).scaleY(1.5f).setDuration(1000).withLayer().start();
            Toast.makeText(this, "正在录音，点击结束...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "录音准备失败", e);
            Toast.makeText(this, "录音设备启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 2. 真实录音逻辑：停止录音并发送网络请求
     */
    private void stopRecordingAndSend() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // Ignore
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }

        // 开始发送给后端
        File audioFile = new File(audioFilePath);
        evaluatePronunciation(targetWord, audioFile);
    }

    /**
     * 3. 真实网络逻辑：OkHttp 发送录音给 Python 后端
     */
    private void evaluatePronunciation(String word, File file) {
        // 构造表单数据，对应 Python FastAPI 的 target_word 和 audio_file
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("target_word", word)
                .addFormDataPart("audio_file", file.getName(),
                        RequestBody.create(MediaType.parse("audio/mp4"), file))
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "网络请求失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(PracticeActivity.this, "连接服务器失败，请检查网络和IP", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        // ====== 加上这一行，把后端发来的原始字符串打印出来 ======
                        Log.d("VISION_DEBUG", "后端返回的完整JSON: " + responseData);
                        JSONObject json = new JSONObject(responseData);

                        // 按照你后端的返回格式提取数据
                        JSONArray refPhonemes = json.getJSONArray("reference_phonemes");
                        JSONArray userPhonemes = json.getJSONArray("user_phonemes");
                        JSONArray feedback = json.getJSONArray("feedback");

                        // 必须切回主线程更新 UI
                        runOnUiThread(() -> {
                            updateUIWithFeedback(refPhonemes, userPhonemes, feedback);
                            // 数据准备好后，弹出底部面板
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "JSON解析失败", e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(PracticeActivity.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 4. 真实渲染逻辑：根据 JSON 结果动态生成红绿音标块
     */
    /**
     * 动态渲染上下对比的音标打分结果
     */
    private void updateUIWithFeedback(JSONArray refPhonemes, JSONArray userPhonemes, JSONArray feedback) {
        LinearLayout llContainer = findViewById(R.id.llPhonemeContainer);
        llContainer.removeAllViews(); // 清空旧数据

        try {
            // 【关键点】：在这里定义 totalScore，确保整个 try 块都能用到它
            float totalScore = 0f;
            int totalCount = refPhonemes.length();
            int validPhonemeCount = 0;

            for (int i = 0; i < totalCount; i++) {
                String ref = refPhonemes.getString(i);
                String user = userPhonemes.getString(i);
                String fb = feedback.getString(i);

                if (!ref.equals("-")) validPhonemeCount++;

                // 1. 创建上下两行的容器
                LinearLayout pairLayout = new LinearLayout(this);
                pairLayout.setOrientation(LinearLayout.VERTICAL);
                pairLayout.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams pairParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                pairParams.setMargins(0, 0, 16, 0);
                pairLayout.setLayoutParams(pairParams);

                // 2. 顶部的参考音标
                TextView tvRef = new TextView(this);
                tvRef.setText(ref.equals("-") ? " " : ref);
                tvRef.setTextSize(16);
                tvRef.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                tvRef.setGravity(android.view.Gravity.CENTER);
                tvRef.setPadding(0, 0, 0, 4);

                // 3. 底部的用户发音
                TextView tvUser = new TextView(this);
                tvUser.setText(user.equals("-") ? "×" : user);
                tvUser.setTextSize(20);
                tvUser.setPadding(24, 12, 24, 12);
                tvUser.setTypeface(null, android.graphics.Typeface.BOLD);
                tvUser.setGravity(android.view.Gravity.CENTER);

                // 4. 三级容错上色与加权计分
                if (fb.equals("Match")) {
                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_correct);
                    tvUser.setTextColor(ContextCompat.getColor(this, R.color.success_green));
                    totalScore += 1.0f; // 完美，得 1 分
                } else if (fb.startsWith("Flaw:")) {
                    String[] parts = fb.split(":");
                    String reason = parts.length > 1 ? parts[1] : "发音有瑕疵";

                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_warning);
                    tvUser.setTextColor(android.graphics.Color.parseColor("#F57C00"));
                    totalScore += 0.6f; // 瑕疵，得 0.6 分

                    // 点击弹出诊断原因
                    tvUser.setOnClickListener(v -> android.widget.Toast.makeText(this, "AI诊断: " + reason, android.widget.Toast.LENGTH_SHORT).show());
                } else {
                    tvUser.setBackgroundResource(R.drawable.bg_phoneme_error);
                    tvUser.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                    totalScore += 0f; // 错误，得 0 分
                }

                // 5. 拼装 UI
                pairLayout.addView(tvRef);
                pairLayout.addView(tvUser);
                llContainer.addView(pairLayout);
            }

            // 6. 核心业务逻辑：【非线性教育打分曲线】
            if (validPhonemeCount > 0) {
                TextView tvScore = findViewById(R.id.tvScore);

                // 计算底层原始正确率
                float rawAccuracy = totalScore / totalCount;
                int displayScore = 0;

                // 曲线映射 (高分段放水，中分段保及格，低分段惩罚)
                if (rawAccuracy >= 0.8f) {
                    displayScore = (int) (90 + (rawAccuracy - 0.8f) * 50);
                } else if (rawAccuracy >= 0.5f) {
                    displayScore = (int) (60 + (rawAccuracy - 0.5f) * 100);
                } else {
                    displayScore = (int) (rawAccuracy * 120);
                }

                // 兜底，限制在 0-100 之间
                displayScore = Math.max(0, Math.min(100, displayScore));
                tvScore.setText(displayScore + "%");

                // 根据最终展示分数变色
                if (displayScore >= 80) {
                    tvScore.setTextColor(ContextCompat.getColor(this, R.color.success_green));
                } else if (displayScore >= 60) {
                    tvScore.setTextColor(android.graphics.Color.parseColor("#F57C00"));
                } else {
                    tvScore.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                }
            }

        } catch (Exception e) {
            android.util.Log.e("PracticeActivity", "UI更新异常", e);
        }
    }

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
    }
    /**
     * 向 Python 后端请求该单词的标准音标
     */
    private void fetchPhonetics(String word) {
        // 【注意】如果你用的是局域网热点或者 cpolar，记得把这里的 IP 换掉
        String url = "http://127.0.0.1:8000/get_phonetics/?word=" + word;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 如果网络失败，给个占位符
                runOnUiThread(() -> {
                    TextView tvPhonetic = findViewById(R.id.tvPhonetic);
                    tvPhonetic.setText("/.../");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        String phonetics = json.getString("phonetics");

                        // 成功拿到音标，切回主线程更新 UI
                        runOnUiThread(() -> {
                            TextView tvPhonetic = findViewById(R.id.tvPhonetic);
                            tvPhonetic.setText(phonetics);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "解析音标JSON失败", e);
                    }
                }
            }
        });
    }
}