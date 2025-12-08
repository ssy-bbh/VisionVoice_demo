package com.example.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FocusBoxView extends View {

    private Paint paint;
    private Rect focusRect;
    private int boxSize;

    public FocusBoxView(Context context) {
        super(context);
        init();
    }

    public FocusBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusBoxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setAntiAlias(true);

        boxSize = dpToPx(200); // Default size for the focus box, 200dp
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Calculate the center of the view
        int centerX = w / 2;
        int centerY = h / 2;

        // Define the focus rectangle in the center
        focusRect = new Rect(
                centerX - boxSize / 2,
                centerY - boxSize / 2,
                centerX + boxSize / 2,
                centerY + boxSize / 2
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (focusRect != null) {
            canvas.drawRect(focusRect, paint);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
