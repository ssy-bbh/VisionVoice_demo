package com.example.myapplication.utils

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object ShaderManager {

    // 把 Shader 代码抽离成一个常量字符串
    private const val SHADER_CODE = """
        uniform shader uBaseMap;
        uniform float2 uGyroOffset;
        uniform float uFresnelPower;
        
        const mat3 kRGBToYIQ = mat3(0.299, 0.587, 0.114, 0.596, -0.275, -0.321, 0.212, -0.523, 0.311);
        const mat3 kYIQToRGB = mat3(1.0, 0.956, 0.621, 1.0, -0.272, -0.647, 1.0, -1.107, 1.704);

        half4 main(float2 fragCoord) {
            half4 baseColor = uBaseMap.eval(fragCoord);
            if (baseColor.a < 0.01) return baseColor; // 性能优化：跳过透明像素
            
            // 1. 光影相位计算 (只算光，不做物理位移)
            float uvPhase = (fragCoord.x - fragCoord.y) * 0.002;
            float sharedPhase = uvPhase + (uGyroOffset.x + uGyroOffset.y) * 12.0;
            
            // 2. YIQ 色相微调
            half3 yiq = kRGBToYIQ * baseColor.rgb;
            float hue = atan(yiq.z, yiq.y) + uGyroOffset.x * 0.3; 
            float chroma = length(yiq.yz);
            yiq.y = chroma * cos(hue);
            yiq.z = chroma * sin(hue);
            half3 shiftedColor = kYIQToRGB * yiq;
            
            // 3. 镭射彩虹与白光刃合成
            half3 rainbowLight = half3(
                0.5 + 0.5 * cos(sharedPhase + 0.0),
                0.5 + 0.5 * cos(sharedPhase + 2.09), 
                0.5 + 0.5 * cos(sharedPhase + 4.18)
            );
            
            float whiteBlade = pow(max(0.0, 0.5 + 0.5 * cos(sharedPhase + 1.0)), 32.0);
            float luma = max(yiq.x, 0.0);
            
            float rainbowIntensity = pow(luma, 1.5) * uFresnelPower * 0.2; 
            float bladeIntensity = luma * uFresnelPower * 0.15; 
            
            half3 finalColor = shiftedColor + (rainbowLight * rainbowIntensity) + (half3(1.0) * whiteBlade * bladeIntensity);
            
            return half4(finalColor, baseColor.a);
        }
    """

    // 🚨 核心改动：这里变成了 fun (方法) 而不是 val (属性)
    // 每次调用，都会返回一个全新、干净的 RuntimeShader 实例！
    fun createNewLaserShader(): RuntimeShader {
        return RuntimeShader(SHADER_CODE)
    }
}