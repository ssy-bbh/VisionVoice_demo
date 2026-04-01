package com.example.myapplication.ui.collection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GyroParallaxContainer extends FrameLayout implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float[] rotationMatrix = new float[9];
    private float[] orientationValues = new float[3];

    // 灵敏度调节，数值越大移动越明显
    private static final float PARALLAX_SENSITIVITY = 150f;

    public GyroParallaxContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sensorManager != null && rotationSensor != null) {
            // 使用 GAME_ROTATION_VECTOR 或 ROTATION_VECTOR，SENSOR_DELAY_GAME 保证丝滑
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationValues);

            float roll = orientationValues[2];
            float pitch = orientationValues[1];

            // 调用强大的递归方法，无论嵌套多深都能移动！
            applyParallax(this, roll, pitch);
        }
    }

    // 【新增的递归核心方法】
    private void applyParallax(android.view.ViewGroup parent, float roll, float pitch) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null) {
                try {
                    float depth = Float.parseFloat(tag.toString());
                    child.setTranslationX(-roll * PARALLAX_SENSITIVITY * depth);
                    child.setTranslationY(-pitch * PARALLAX_SENSITIVITY * depth);
                } catch (NumberFormatException ignored) {}
            }
            // 如果子控件也是个容器（比如 ConstraintLayout），继续往里找！
            if (child instanceof android.view.ViewGroup) {
                applyParallax((android.view.ViewGroup) child, roll, pitch);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}