package com.smartwatch.monitor.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * 自定义电池指示器View
 * 根据电量百分比显示不同颜色的电池图标
 */
public class BatteryIndicatorView extends View {

    private static final int COLOR_HIGH = Color.parseColor("#10B981");
    private static final int COLOR_MEDIUM = Color.parseColor("#F59E0B");
    private static final int COLOR_LOW = Color.parseColor("#EF4444");

    private Paint batteryPaint;
    private Paint levelPaint;
    private Paint tipPaint;
    private RectF batteryRect;
    private RectF levelRect;
    private RectF tipRect;

    private int batteryLevel = 100;

    public BatteryIndicatorView(Context context) {
        super(context);
        init();
    }

    public BatteryIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        batteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        batteryPaint.setStyle(Paint.Style.STROKE);
        batteryPaint.setStrokeWidth(2f);
        batteryPaint.setColor(Color.parseColor("#9CA3AF"));

        levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setStyle(Paint.Style.FILL);
        levelPaint.setColor(COLOR_HIGH);

        tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setStyle(Paint.Style.FILL);
        tipPaint.setColor(Color.parseColor("#9CA3AF"));

        batteryRect = new RectF();
        levelRect = new RectF();
        tipRect = new RectF();
    }

    /**
     * 设置电池电量
     * @param level 电量百分比 (0-100)
     */
    public void setBatteryLevel(int level) {
        this.batteryLevel = Math.max(0, Math.min(100, level));
        updateColor();
        invalidate();
    }

    private void updateColor() {
        if (batteryLevel > 50) {
            levelPaint.setColor(COLOR_HIGH);
        } else if (batteryLevel > 20) {
            levelPaint.setColor(COLOR_MEDIUM);
        } else {
            levelPaint.setColor(COLOR_LOW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float padding = 2f;
        float tipWidth = 3f;
        float tipHeight = height * 0.3f;

        // 电池主体
        float batteryLeft = padding;
        float batteryTop = padding;
        float batteryRight = width - padding - tipWidth;
        float batteryBottom = height - padding;

        batteryRect.set(batteryLeft, batteryTop, batteryRight, batteryBottom);
        canvas.drawRoundRect(batteryRect, 2f, 2f, batteryPaint);

        // 电池尖端
        float tipLeft = batteryRight;
        float tipTop = (height - tipHeight) / 2f;
        float tipRight = width - padding;
        float tipBottom = tipTop + tipHeight;
        tipRect.set(tipLeft, tipTop, tipRight, tipBottom);
        canvas.drawRoundRect(tipRect, 1f, 1f, tipPaint);

        // 电量填充
        if (batteryLevel > 0) {
            float availableWidth = batteryRight - batteryLeft - 4f;
            float fillWidth = availableWidth * (batteryLevel / 100f);
            levelRect.set(
                batteryLeft + 2f,
                batteryTop + 2f,
                batteryLeft + 2f + fillWidth,
                batteryBottom - 2f
            );
            canvas.drawRoundRect(levelRect, 1f, 1f, levelPaint);
        }
    }
}
