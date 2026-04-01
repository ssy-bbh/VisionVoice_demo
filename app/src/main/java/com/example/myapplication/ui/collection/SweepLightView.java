package com.example.myapplication.ui.collection;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class SweepLightView extends View {
    private Paint paint;
    private Matrix matrix;
    private float translateX;
    private ValueAnimator animator;
    private int width;

    public SweepLightView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        matrix = new Matrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        // 创建一个倾斜的线性渐变：透明 -> 半透明白 -> 透明
        LinearGradient shader = new LinearGradient(
                -w, 0, 0, h,
                new int[]{Color.TRANSPARENT, Color.parseColor("#4DFFFFFF"), Color.TRANSPARENT},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);

        if (animator != null) animator.cancel();
        // 光效从左上角扫到右下角，范围是 width * 2.5
        animator = ValueAnimator.ofFloat(0, w * 2.5f);
        animator.setDuration(3000); // 3秒扫一次
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            translateX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (paint.getShader() != null) {
            matrix.setTranslate(translateX, 0);
            paint.getShader().setLocalMatrix(matrix);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}