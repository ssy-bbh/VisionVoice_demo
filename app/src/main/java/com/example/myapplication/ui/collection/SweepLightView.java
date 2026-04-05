package com.example.myapplication.ui.collection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

public class SweepLightView extends View implements SensorEventListener {

    private Paint paint;
    private LinearGradient holographicGradient;
    private Matrix matrix;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float currentTx = 0f;
    private float currentTy = 0f;

    // 🌟 新增：0=冷色(蓝青), 1=暖色(紫金)
    private int themeMode = 0;

    public SweepLightView(Context context) { super(context); init(context); }
    public SweepLightView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        matrix = new Matrix();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    // 🌟 暴露给外部调用的变色方法
    public void setLightTheme(int mode) {
        this.themeMode = mode;
        updateGradient(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient(w, h);
    }

    private void updateGradient(int w, int h) {
        if (w == 0 || h == 0) return;
        int[] colors;

        if (themeMode == 0) {
            // 💎 主题 0: 冰川钛银 (AR Scan)
            colors = new int[]{
                    Color.TRANSPARENT,
                    Color.parseColor("#1A8B5CF6"), // 微弱紫
                    Color.parseColor("#330EA5E9"), // 冰川蓝
                    Color.parseColor("#80FFFFFF"), // 银白高光
                    Color.parseColor("#4D06B6D4"), // 极客青
                    Color.parseColor("#1A10B981"), // 幽暗绿
                    Color.TRANSPARENT
            };
        } else {
            // 🌅 主题 1: 日落霓虹 (Photo)
            colors = new int[]{
                    Color.TRANSPARENT,
                    Color.parseColor("#1A0EA5E9"), // 微弱蓝
                    Color.parseColor("#338B5CF6"), // 幻影紫
                    Color.parseColor("#80FFFFFF"), // 银白高光
                    Color.parseColor("#4DF59E0B"), // 琥珀金 (暖色核心)
                    Color.parseColor("#1AEC4899"), // 霓虹粉
                    Color.TRANSPARENT
            };
        }

        float[] positions = new float[]{0f, 0.2f, 0.4f, 0.5f, 0.65f, 0.85f, 1f};
        holographicGradient = new LinearGradient(
                -w, -h, w * 2f, h * 2f,
                colors, positions, Shader.TileMode.CLAMP
        );
        paint.setShader(holographicGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (holographicGradient == null) return;
        matrix.reset();
        matrix.setTranslate(currentTx, currentTy);
        holographicGradient.setLocalMatrix(matrix);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float gx = event.values[0];
            float gy = event.values[1];
            float normalizedX = Math.max(-1f, Math.min(1f, gx / 9.8f));
            float normalizedY = Math.max(-1f, Math.min(1f, gy / 9.8f));
            float targetTx = -normalizedX * (getWidth() * 1.2f);
            float targetTy = normalizedY * (getHeight() * 1.2f);
            currentTx += (targetTx - currentTx) * 0.1f;
            currentTy += (targetTy - currentTy) * 0.1f;
            postInvalidate();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }
}