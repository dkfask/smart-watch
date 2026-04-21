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
 * 报警级别徽章View
 * 根据报警级别显示不同颜色的圆角标签
 */
public class AlarmLevelBadge extends View {

    private static final int COLOR_CRITICAL = Color.parseColor("#EF4444");
    private static final int COLOR_WARNING = Color.parseColor("#F59E0B");
    private static final int COLOR_INFO = Color.parseColor("#3B82F6");

    private Paint bgPaint;
    private Paint textPaint;
    private RectF bgRect;

    private String levelText = "";
    private int levelColor = COLOR_INFO;

    public AlarmLevelBadge(Context context) {
        super(context);
        init();
    }

    public AlarmLevelBadge(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlarmLevelBadge(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        bgRect = new RectF();
    }

    /**
     * 设置报警级别
     * @param level 级别字符串: "critical", "warning", "info"
     */
    public void setAlarmLevel(String level) {
        if (level == null) level = "info";
        switch (level) {
            case "critical":
                levelText = "紧急";
                levelColor = COLOR_CRITICAL;
                break;
            case "warning":
                levelText = "警告";
                levelColor = COLOR_WARNING;
                break;
            case "info":
            default:
                levelText = "信息";
                levelColor = COLOR_INFO;
                break;
        }
        bgPaint.setColor(levelColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 绘制圆角背景
        bgRect.set(0, 0, width, height);
        canvas.drawRoundRect(bgRect, height / 2f, height / 2f, bgPaint);

        // 绘制文字
        if (!levelText.isEmpty()) {
            textPaint.setTextSize(Math.min(height * 0.6f, 28f));
            canvas.drawText(levelText, width / 2f, height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
        }
    }
}
