package com.example.myapplication.ui.custom; // 🚨 保持你现有的包名

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 🌟 极客炫技：基于重力感应的星空流光 View (科幻数据卡形态)
 */
public class GravityShimmerView extends View implements SensorEventListener {

    // ===== 绘图工具 =====
    private Paint bgPaint;
    private Paint borderPaint;
    private Paint starPaint;
    private LinearGradient bgGradient;
    private RadialGradient borderGlowGradient;

    // 🌟 核心改动：用矩形边界代替圆形半径
    private RectF boundsF = new RectF();
    private float cornerRadius = 48f; // 卡片圆角大小 (更像高级硅胶或装甲的倒角)

    // ===== 粒子系统 (星光) =====
    private List<Star> stars = new ArrayList<>();
    private static final int STAR_COUNT = 100; // 星点数量
    private Random random = new Random();

    // ===== 传感器 (重力) =====
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float gravityX = 0f;
    private float gravityY = 0f;

    // ===== 状态 =====
    private boolean isCompleted = false;

    // ===== 赛博极客色板 =====
    private static final int COLOR_VOID_TOP = 0xFF020617;
    private static final int COLOR_VOID_BOTTOM = 0xFF0F172A;
    private static final int COLOR_CYAN_GLOW = 0xFF06B6D4;
    private static final int COLOR_SUCCESS_GREEN = 0xFF10B981;

    public GravityShimmerView(Context context) { super(context); init(context); }
    public GravityShimmerView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context); }

    private void init(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < STAR_COUNT; i++) stars.add(new Star());
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        bgGradient = null;
        borderGlowGradient = null;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgGradient = new LinearGradient(0, 0, 0, h, COLOR_VOID_TOP, COLOR_VOID_BOTTOM, Shader.TileMode.CLAMP);
        // 留出 4px 的安全边距，防止边框被裁剪
        boundsF.set(4f, 4f, w - 4f, h - 4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        int mainColor = isCompleted ? COLOR_SUCCESS_GREEN : COLOR_CYAN_GLOW;

        // 1. 绘制深邃夜空底色 (圆角矩形)
        bgPaint.setShader(bgGradient);
        canvas.drawRoundRect(boundsF, cornerRadius, cornerRadius, bgPaint);

        // 2. 绘制随重力偏移的霓虹发光边框
        borderGlowGradient = new RadialGradient(cx + gravityX * 12, cy + gravityY * 12, Math.max(w, h) / 1.2f,
                adjustAlpha(mainColor, 0.7f), adjustAlpha(mainColor, 0.0f), Shader.TileMode.CLAMP);
        borderPaint.setShader(borderGlowGradient);
        borderPaint.setStrokeWidth(8f);
        canvas.drawRoundRect(boundsF, cornerRadius, cornerRadius, borderPaint);

        // 3. 绘制实体边界
        borderPaint.setShader(null);
        borderPaint.setColor(adjustAlpha(mainColor, 0.9f));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(boundsF, cornerRadius, cornerRadius, borderPaint);

        // 4. 绘制流动的星光粒子
        for (Star star : stars) {
            star.update(gravityX, gravityY, w, h);
            starPaint.setColor(adjustAlpha(isCompleted ? Color.WHITE : mainColor, star.alpha));
            canvas.drawCircle(star.x, star.y, star.size, starPaint);
        }

        invalidate();
    }

    private int adjustAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(alpha * 255) << 24);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityX = -event.values[0];
            gravityY = event.values[1];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void startListening() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stopListening() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    // 内部粒子类
    private class Star {
        float x, y, size, alpha, speedMultiplier, baseAlpha;
        Star() {
            size = 1f + random.nextFloat() * 3f;
            baseAlpha = 0.2f + random.nextFloat() * 0.8f;
            alpha = baseAlpha;
            speedMultiplier = 0.5f + random.nextFloat() * 2f;
        }
        void update(float gx, float gy, float w, float h) {
            if (x == 0f && y == 0f) { x = random.nextFloat() * w; y = random.nextFloat() * h; }

            x += gx * 0.15f * speedMultiplier;
            y += gy * 0.15f * speedMultiplier;

            // 🌟 核心改动：用矩形空间“无缝穿梭”代替圆形的碰撞反弹
            // 粒子如果从左边飘出去，就从右边钻进来！非常酷炫的空间跃迁效果。
            if (x < -10f) x = w + 10f;
            else if (x > w + 10f) x = -10f;

            if (y < -10f) y = h + 10f;
            else if (y > h + 10f) y = -10f;

            if (random.nextFloat() < 0.05) alpha = baseAlpha * (0.4f + random.nextFloat() * 0.6f);
        }
    }
}