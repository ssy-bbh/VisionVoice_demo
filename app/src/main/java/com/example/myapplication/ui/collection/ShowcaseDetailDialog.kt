package com.example.myapplication.ui.collection;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;

import java.io.File;

public class ShowcaseDetailDialog extends DialogFragment {

    private String word;
    private String imagePath;
    private int score;

    public static ShowcaseDetailDialog newInstance(String word, String imagePath, int score) {
        ShowcaseDetailDialog dialog = new ShowcaseDetailDialog();
        Bundle args = new Bundle();
        args.putString("WORD", word);
        args.putString("IMAGE", imagePath);
        args.putInt("SCORE", score);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 【修改点】去掉全屏主题，使用默认透明主题，这样才能悬浮
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
        if (getArguments() != null) {
            word = getArguments().getString("WORD");
            imagePath = getArguments().getString("IMAGE");
            score = getArguments().getInt("SCORE");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_showcase_detail, container, false);

        // 点击透明空白处关闭弹窗
        view.findViewById(R.id.rootContainer).setOnClickListener(v -> dismiss());

        TextView tvWord = view.findViewById(R.id.tvDetailWord);
        TextView tvScore = view.findViewById(R.id.tvDetailScore);
        ImageView ivPhoto = view.findViewById(R.id.ivDetailPhoto);

        tvWord.setText(word.substring(0, 1).toUpperCase() + word.substring(1));
        tvScore.setText(score + "%");

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    ivPhoto.setImageURI(Uri.parse(imagePath));
                } else {
                    File imgFile = new File(imagePath);
                    if (imgFile.exists()) ivPhoto.setImageURI(Uri.fromFile(imgFile));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // 设置窗口本身宽高填满，靠内部的 Padding 挤压出悬浮效果
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 实时模糊背后的界面（展示柜界面）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                window.getAttributes().setBlurBehindRadius(40); // 背景模糊半径
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.6f);
            }
        }
    }
}