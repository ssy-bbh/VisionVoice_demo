package com.example.myapplication.ui.practice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class PracticeActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View rippleView;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        // 1. 获取单词
        String word = getIntent().getStringExtra("extra_word");
        TextView tvWord = findViewById(R.id.tvTargetWord);
        tvWord.setText(word != null ? word : "Unknown");

        // 2. 初始化 BottomSheet
        View bottomSheet = findViewById(R.id.bottomSheetFeedback);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // 默认隐藏

        // 3. 录音按钮逻辑
        rippleView = findViewById(R.id.viewRipple);
        findViewById(R.id.btnRecord).setOnClickListener(v -> toggleRecording());
        
        // 4. 下一个物体
        findViewById(R.id.btnNextObject).setOnClickListener(v -> {
            // 返回首页或重新扫描
            finish();
        });
        
        // 5. 关闭
        findViewById(R.id.btnClosePractice).setOnClickListener(v -> finish());
    }

    private void toggleRecording() {
        if (!isRecording) {
            // 开始录音 (模拟)
            isRecording = true;
            rippleView.setVisibility(View.VISIBLE);
            rippleView.animate().scaleX(1.5f).scaleY(1.5f).setDuration(1000).withLayer().start();
            
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();

            // 模拟 2秒后自动完成
            new Handler(Looper.getMainLooper()).postDelayed(this::finishRecordingAndShowFeedback, 2000);
        }
    }
    
    private void finishRecordingAndShowFeedback() {
        isRecording = false;
        rippleView.animate().cancel();
        rippleView.setVisibility(View.INVISIBLE);
        
        // 弹出反馈面板
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}