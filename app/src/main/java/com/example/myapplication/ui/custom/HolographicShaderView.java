package com.example.myapplication.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import com.example.myapplication.utils.ShaderManager;

public class HolographicShaderView extends View {
    private Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Object laserShader; // 🌟 改成 Object，避免旧版本类加载直接崩溃

    public HolographicShaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // 🚨 只有 API 33+ 才初始化 Shader
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            laserShader = ShaderManager.INSTANCE.createNewLaserShader();
            shaderPaint.setShader((RuntimeShader) laserShader);
        } else {
            // 🌟 保底方案：旧手机显示一个半透明的深蓝色
            shaderPaint.setColor(Color.parseColor("#1A06B6D4"));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 🚨 只有 API 33+ 才更新 Uniform
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && laserShader instanceof RuntimeShader) {
            RuntimeShader shader = (RuntimeShader) laserShader;
            // 【防闪退】当前共享着色器源码里没有声明 uTime / uSize，
            // setFloatUniform 遇到未声明的 uniform 会抛 IllegalArgumentException，
            // 而 onDraw 里的异常会直接导致闪退，所以必须兜住
            try {
                shader.setFloatUniform("uTime", System.currentTimeMillis() % 100000 / 1000f);
                shader.setFloatUniform("uSize", getWidth(), getHeight());
                // 如果你有陀螺仪数据，也在这里 set
                shader.setFloatUniform("uGyroOffset", 0f, 0f);
            } catch (IllegalArgumentException e) {
                // uniform 不存在则跳过动画参数，只画静态效果
            }
            canvas.drawRect(0, 0, getWidth(), getHeight(), shaderPaint);
            invalidate();
        } else {
            // 旧手机直接画保底颜色，不需要 invalidate 重绘，节省性能
            canvas.drawRect(0, 0, getWidth(), getHeight(), shaderPaint);
        }
    }
}