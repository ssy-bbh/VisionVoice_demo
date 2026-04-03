package com.example.myapplication.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.ui.ar.RealtimeActivity;
import com.example.myapplication.ui.photo.PhotoRecognitionActivity;
import com.example.myapplication.utils.UserStatsManager;

import java.util.List;

public class HomeFragment extends Fragment {

    // 声明我们要动态更新的两个控件
    private TextView tvStreakCount;
    private LinearLayout containerTasks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View cardRealtime = view.findViewById(R.id.cardRealtimeScan);
        View cardPhoto = view.findViewById(R.id.cardPhotoUpload);

        // 绑定 XML 里的火苗和挑战容器
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        containerTasks = view.findViewById(R.id.containerDailyTasks);

        // 核心跳转：去 RealtimeActivity
        cardRealtime.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RealtimeActivity.class);
            startActivity(intent);
        });

        // 核心跳转：去 PhotoRecognitionActivity
        cardPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PhotoRecognitionActivity.class);
            startActivity(intent);
        });

        return view;
    }

    // 🚨 核心魔法：每次回到首页都会触发，保证火苗是最新的！
    @Override
    public void onResume() {
        super.onResume();
        updateDailyStats();
    }

    private void updateDailyStats() {
        if (getContext() == null) return;

        // 1. 同步获取并设置连胜火苗
        int streak = UserStatsManager.INSTANCE.getStreakCount(getContext());
        if (tvStreakCount != null) {
            tvStreakCount.setText("🔥 " + streak + " Days");
        }

        // 2. 异步加载每日挑战 (通过我们刚写的 Java 专属通讯员)
        if (containerTasks != null) {
            UserStatsManager.INSTANCE.getDailyChallengesAsync(getContext(), tasks -> {
                // 防御性编程：确保在主线程更新 UI
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    containerTasks.removeAllViews(); // 先清空旧的标签

                    for (String taskWord : tasks) {
                        // 动态生成 TextView 作为卡片
                        TextView taskTextView = new TextView(getContext());
                        taskTextView.setText(taskWord);
                        taskTextView.setTextSize(16f);
                        taskTextView.setPadding(40, 15, 40, 15);
                        taskTextView.setTextColor(Color.parseColor("#06B6D4")); // 极客蓝

                        // 如果你之前没有建 bg_circle_gray.xml，这里给你个极其优雅的浅蓝色半透明底色保底
                        taskTextView.setBackgroundColor(Color.parseColor("#1A06B6D4"));

                        // 设置边距，让卡片之间有呼吸感
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(15, 0, 15, 0);
                        taskTextView.setLayoutParams(params);

                        // 把小卡片塞进横向容器里
                        containerTasks.addView(taskTextView);
                    }
                });
            });
        }
    }
}