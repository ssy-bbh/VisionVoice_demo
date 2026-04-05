package com.example.myapplication.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.ui.ar.RealtimeActivity;
import com.example.myapplication.ui.custom.GravityShimmerView;
import com.example.myapplication.ui.photo.PhotoRecognitionActivity;
import com.example.myapplication.utils.UserStatsManager;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvStreakCount;
    private LinearLayout containerTasks;
    private TextView tvChallengeTitle;

    private List<GravityShimmerView> shimmerViews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View cardRealtime = view.findViewById(R.id.cardRealtimeScan);
        View cardPhoto = view.findViewById(R.id.cardPhotoUpload);

        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        containerTasks = view.findViewById(R.id.containerDailyTasks);
        tvChallengeTitle = view.findViewById(R.id.tvChallengeTitle);

        if (tvChallengeTitle != null) {
            tvChallengeTitle.setOnLongClickListener(v -> {
                Toast.makeText(getContext(), "🔄 开发者模式：重新洗牌！", Toast.LENGTH_SHORT).show();
                loadChallenges(true);
                return true;
            });
        }

        cardRealtime.setOnClickListener(v -> startActivity(new Intent(getActivity(), RealtimeActivity.class)));
        cardPhoto.setOnClickListener(v -> startActivity(new Intent(getActivity(), PhotoRecognitionActivity.class)));
        // 绑定底层的重力星空
        GravityShimmerView bgScan = view.findViewById(R.id.shimmerBgScan);
        GravityShimmerView bgPhoto = view.findViewById(R.id.shimmerBgPhoto);
        if (bgScan != null) shimmerViews.add(bgScan);
        if (bgPhoto != null) shimmerViews.add(bgPhoto);

        // 🌟 新增：绑定表层的镭射膜，并把相册卡片切换为“紫金”暖色调
        com.example.myapplication.ui.collection.SweepLightView sweepLightPhoto = view.findViewById(R.id.sweepLightPhoto);
        if (sweepLightPhoto != null) {
            sweepLightPhoto.setLightTheme(1); // 1 = 日落霓虹主题
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            int streak = UserStatsManager.INSTANCE.getStreakCount(getContext());
            if (tvStreakCount != null) tvStreakCount.setText("🔥 " + streak + " Days");
        }
        loadChallenges(false);

        for (GravityShimmerView sv : shimmerViews) sv.startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        for (GravityShimmerView sv : shimmerViews) sv.stopListening();
    }

    // 🌟 辅助工具：DP 转 像素 (之前报错就是因为缺了这个)
    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadChallenges(boolean forceRefresh) {
        if (getContext() == null || containerTasks == null) return;

        UserStatsManager.INSTANCE.getDailyChallengesAsync(getContext(), forceRefresh, tasks -> {
            if (getActivity() == null) return;
            List<String> completedTasks = UserStatsManager.INSTANCE.getCompletedChallenges(getContext());

            getActivity().runOnUiThread(() -> {
                containerTasks.removeAllViews();
                shimmerViews.clear();

                for (String taskWord : tasks) {
                    boolean isCompleted = false;
                    for (String c : completedTasks) {
                        if (c.equalsIgnoreCase(taskWord)) { isCompleted = true; break; }
                    }

                    // 1. 卡片外框设置
                    FrameLayout tagRoot = new FrameLayout(getContext());
                    LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                            0, dpToPx(120), 1.0f); // 优雅的 120dp 高度
                    rootParams.setMargins(dpToPx(6), 0, dpToPx(6), 0);
                    tagRoot.setLayoutParams(rootParams);

                    // 2. 铺上重力星空底层
                    GravityShimmerView shimmerBg = new GravityShimmerView(getContext());
                    shimmerBg.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    shimmerBg.setCompleted(isCompleted);
                    shimmerViews.add(shimmerBg);
                    tagRoot.addView(shimmerBg);

                    // 3. 铺上悬浮的文字
                    TextView tvWord = new TextView(getContext());
                    String displayWord = taskWord.substring(0, 1).toUpperCase() + taskWord.substring(1);

                    // 🌟 针对 Tennis racket 等长词的终极杀招：空格换行
                    displayWord = displayWord.replace(" ", "\n");

                    tvWord.setTextSize(14f);
                    tvWord.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvWord.setGravity(Gravity.CENTER); // 多行强制居中

                    // 设置安全边距，防撞边框
                    int pad = dpToPx(8);
                    tvWord.setPadding(pad, pad, pad, pad);

                    FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    textParams.gravity = Gravity.CENTER;
                    tvWord.setLayoutParams(textParams);

                    if (isCompleted) {
                        tvWord.setText(displayWord + "\n✅");
                        tvWord.setTextColor(Color.WHITE);
                        tvWord.setPaintFlags(tvWord.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        tvWord.setText(displayWord);
                        tvWord.setTextColor(Color.WHITE);
                    }
                    tagRoot.addView(tvWord);

                    containerTasks.addView(tagRoot);
                }

                for (GravityShimmerView sv : shimmerViews) sv.startListening();
            });
        });
    }
}