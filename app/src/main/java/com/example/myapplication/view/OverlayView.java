package com.example.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.myapplication.ml.YoloDetector;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    // 画笔定义
    private Paint boxPaint;
    private Paint textBgPaint;
    private Paint textPaint;

    // 数据源
    private List<YoloDetector.Result> results = new ArrayList<>();

    // 【防闪烁核心】记录最后一次识别到物体的时间
    private long lastDetectionTime = 0;

    // 复用对象（性能优化，避免在 onDraw 中重复创建）
    private final RectF boxRect = new RectF();

    // =========================================================
    // 【核心新增】点击事件监听器接口
    // =========================================================
    public interface OnBoxClickListener {
        void onBoxClick(YoloDetector.Result result);
    }

    private OnBoxClickListener listener;

    public void setOnBoxClickListener(OnBoxClickListener listener) {
        this.listener = listener;
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. 绿色边框
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);
        boxPaint.setAntiAlias(true);

        // 2. 文字背景 (半透明黑)
        textBgPaint = new Paint();
        textBgPaint.setColor(Color.BLACK);
        textBgPaint.setAlpha(160);
        textBgPaint.setStyle(Paint.Style.FILL);

        // 3. 文字 (白色加粗)
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);
    }

    /**
     * 接收数据的方法 (含防抖动逻辑)
     */
    public void setResults(List<YoloDetector.Result> detectionResults) {
        long currentTime = System.currentTimeMillis();

        if (detectionResults != null && !detectionResults.isEmpty()) {
            // 1. 如果有数据：立刻更新，并更新时间戳
            this.results = detectionResults;
            this.lastDetectionTime = currentTime;
            postInvalidate(); // 刷新界面
        } else {
            // 2. 如果是空数据：检查距离上次识别过了多久
            // 只有超过 200ms 没有识别到物体，才清空屏幕
            // 这样可以防止因为偶尔丢帧导致的框闪烁
            if (currentTime - lastDetectionTime > 200) {
                this.results.clear();
                postInvalidate();
            }
        }
    }

    /**
     * 【核心新增】处理屏幕触摸事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 我们只关心手指抬起的那一刻 (Click)
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            if (results != null && !results.isEmpty()) {
                float width = getWidth();
                float height = getHeight();

                // 倒序遍历，优先响应最上层的框（如果有重叠）
                for (int i = results.size() - 1; i >= 0; i--) {
                    YoloDetector.Result result = results.get(i);
                    RectF normalizedRect = result.getRect();

                    // 还原为屏幕坐标
                    float left = normalizedRect.left * width;
                    float top = normalizedRect.top * height;
                    float right = normalizedRect.right * width;
                    float bottom = normalizedRect.bottom * height;

                    // 判断手指是否点中了这个框
                    // 稍微扩大一点点击区域，提升用户体验
                    if (x >= left && x <= right && y >= top && y <= bottom) {
                        if (listener != null) {
                            listener.onBoxClick(result);
                            return true; // 消耗掉事件，不再向下传递
                        }
                    }
                }
            }
        }
        // 必须返回 true，表示我们消费了这个触摸事件，否则收不到 ACTION_UP
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (results == null || results.isEmpty()) return;

        // 获取当前屏幕的实际宽高
        float width = getWidth();
        float height = getHeight();

        for (YoloDetector.Result result : results) {
            // 获取归一化坐标 (0.0 ~ 1.0)
            RectF normalizedRect = result.getRect();

            // =========================================================
            // 【核心坐标映射】 0~1 小数 --> 屏幕像素坐标
            // =========================================================
            float left = normalizedRect.left * width;
            float top = normalizedRect.top * height;
            float right = normalizedRect.right * width;
            float bottom = normalizedRect.bottom * height;

            // 简单的边界保护
            if (left < 0) left = 0;
            if (top < 0) top = 0;
            if (right > width) right = width;
            if (bottom > height) bottom = height;

            // 使用复用的对象，避免内存抖动
            boxRect.set(left, top, right, bottom);

            // 1. 画绿框
            canvas.drawRect(boxRect, boxPaint);

            // 2. 准备文字
            String labelText = result.getLabel() + String.format(" %.0f%%", result.getScore() * 100);

            // 3. 计算文字位置 (防止画出屏幕外)
            float textWidth = textPaint.measureText(labelText);
            float textHeight = 60f;
            float padding = 10f;

            float bgTop = top - textHeight;
            float bgBottom = top;

            // 如果文字超出屏幕顶部，就画在框的内部
            if (bgTop < 0) {
                bgTop = top;
                bgBottom = top + textHeight;
            }

            // 4. 画背景和文字
            canvas.drawRect(left, bgTop, left + textWidth + 2 * padding, bgBottom, textBgPaint);
            canvas.drawText(labelText, left + padding, bgBottom - 15f, textPaint);
        }
    }
}