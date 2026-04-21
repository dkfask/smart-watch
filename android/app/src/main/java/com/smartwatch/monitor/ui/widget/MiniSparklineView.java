package com.smartwatch.monitor.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 迷你趋势线View
 * 使用Canvas绘制轻量级迷你折线图，无需额外库依赖
 */
public class MiniSparklineView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Path linePath;
    private Path fillPath;

    private List<Float> values = new ArrayList<>();
    private int lineColor = Color.parseColor("#3B82F6");
    private int fillColor = Color.parseColor("#333B82F6");

    public MiniSparklineView(Context context) {
        super(context);
        init();
    }

    public MiniSparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniSparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(lineColor);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);

        linePath = new Path();
        fillPath = new Path();
    }

    /**
     * 设置趋势数据
     * @param values 数据点列表
     */
    public void setValues(List<Float> values) {
        this.values = values != null ? values : new ArrayList<>();
        invalidate();
    }

    /**
     * 设置线条颜色
     * @param color 颜色值
     */
    public void setLineColor(int color) {
        this.lineColor = color;
        this.fillColor = Color.argb(51, Color.red(color), Color.green(color), Color.blue(color));
        linePaint.setColor(lineColor);
        fillPaint.setColor(fillColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values.size() < 2) return;

        int width = getWidth();
        int height = getHeight();
        float padding = 4f;

        // 计算最小值和最大值
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        for (float v : values) {
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
        }
        float range = maxVal - minVal;
        if (range == 0) range = 1f;

        // 绘制折线
        linePath.reset();
        fillPath.reset();

        float drawWidth = width - padding * 2;
        float drawHeight = height - padding * 2;
        float stepX = drawWidth / (values.size() - 1);

        for (int i = 0; i < values.size(); i++) {
            float x = padding + i * stepX;
            float y = padding + drawHeight - ((values.get(i) - minVal) / range) * drawHeight;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, height);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // 闭合填充路径
        fillPath.lineTo(padding + (values.size() - 1) * stepX, height);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
