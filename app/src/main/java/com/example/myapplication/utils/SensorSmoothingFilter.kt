package com.example.myapplication.utils

class SensorSmoothingFilter(
    // alpha 系数决定平滑度。0.15f 带来高级的“粘稠”阻尼感 [cite: 153-156]
    private val alpha: Float = 0.15f
) {
    private var smoothedValues: FloatArray? = null

    fun filter(input: FloatArray): FloatArray {
        if (smoothedValues == null) {
            smoothedValues = input.clone()
            return smoothedValues!!
        }

        for (i in input.indices) {
            // 一阶滞后滤波核心逻辑 [cite: 166-167]
            smoothedValues!![i] += alpha * (input[i] - smoothedValues!![i])
        }
        return smoothedValues!!
    }
}