package com.example.myapplication.utils; // 🚨 注意确认一下包名是不是你自己的

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class ImageEnhancer {

    /**
     * 🌟 1. 智能聚焦裁剪 (Smart Focus Crop)
     */
    public static Bitmap smartFocusCrop(Bitmap original, RectF box, float zoomFactor) {
        if (original == null || original.isRecycled() || box == null) return original;

        int imgWidth = original.getWidth();
        int imgHeight = original.getHeight();

        int cx = (int) box.centerX();
        int cy = (int) box.centerY();

        int objectSize = (int) Math.max(box.width(), box.height());

        // 🌟 新增：最小裁剪尺寸保护 (至少占原图短边的 40%)
        int minSize = (int) (Math.min(imgWidth, imgHeight) * 0.4f);

        // 如果物体放大 1.5 倍后依然太小，就强制使用最小尺寸兜底！
        int targetSize = Math.max((int) (objectSize * zoomFactor), minSize);

        int left = cx - targetSize / 2;
        int top = cy - targetSize / 2;
        int right = left + targetSize;
        int bottom = top + targetSize;

        if (left < 0) { right += Math.abs(left); left = 0; }
        if (top < 0) { bottom += Math.abs(top); top = 0; }
        if (right > imgWidth) { left -= (right - imgWidth); right = imgWidth; }
        if (bottom > imgHeight) { top -= (bottom - imgHeight); bottom = imgHeight; }

        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(imgWidth, right);
        bottom = Math.min(imgHeight, bottom);

        int cropWidth = right - left;
        int cropHeight = bottom - top;

        if (cropWidth <= 0 || cropHeight <= 0) return original;
        return Bitmap.createBitmap(original, left, top, cropWidth, cropHeight);
    }

    /**
     * 🪄 2. 黑客级纯离线：径向边缘羽化滤镜 (Holographic Edge Blur)
     * 将方块图片的边缘渐变溶解为透明，制造科幻全息投影感
     */
    public static Bitmap createHolographicEdgeBlur(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        // 创建一个支持 Alpha (透明度) 通道的新画布
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 先把原图画上去
        canvas.drawBitmap(original, 0, 0, null);

        // 准备橡皮擦画笔，使用 DST_IN 模式（仅保留两者相交的部分）
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        // 径向渐变：中心不透明，边缘全透明
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.max(width, height) / 2f;

        // 核心光学参数：前 50% 保持绝对实体，剩下的向外渐变消失
        int[] colors = {0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF};
        float[] stops = {0.0f, 0.2f, 1.0f};

        RadialGradient gradient = new RadialGradient(
                centerX, centerY, radius, colors, stops, Shader.TileMode.CLAMP);
        paint.setShader(gradient);

        // 画上渐变遮罩，方块的生硬边缘瞬间融化！
        canvas.drawRect(0, 0, width, height, paint);

        return output;
    }
}