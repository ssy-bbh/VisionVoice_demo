package com.example.myapplication.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroscopeHelper(
    context: Context,
    private val onGyroChanged: (pitch: Float, roll: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // 强制使用底层的旋转矢量传感器，自带卡尔曼滤波防止漂移 [cite: 137]
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val filter = SensorSmoothingFilter(0.05f)

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // 提取旋转矩阵并计算欧拉角 [cite: 140-142]
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // 低通滤波平滑处理 [cite: 147-148]
            val smoothedAngles = filter.filter(orientationAngles)

            // 回调 Pitch (绕 X 轴) 和 Roll (绕 Y 轴)
            onGyroChanged(smoothedAngles[1], smoothedAngles[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}