package com.example.dpoae;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;

import java.util.List;

public class MyBarChart extends BarChart {

    protected Paint mYAxisSafeZonePaint;
    Context cxt;

    public MyBarChart(Context context) {
        super(context);
        cxt = context;
    }

    public MyBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        cxt = context;
    }

    public MyBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        cxt = context;
    }

    @Override
    protected void init() {
        super.init();
        mYAxisSafeZonePaint = new Paint();
        mYAxisSafeZonePaint.setStyle(Paint.Style.FILL);
        mYAxisSafeZonePaint.setColor(getContext().getColor(R.color.colorDarkGreen));
        mGridBackgroundPaint.setColor(Color.rgb(255, 0, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        List<LimitLine> limitLines = mAxisLeft.getLimitLines();

        if (limitLines == null || limitLines.size() != 2)
            return;

        float[] pts = new float[4];
        LimitLine l1 = limitLines.get(0);
        LimitLine l2 = limitLines.get(1);

        l1.setLineColor(Color.rgb(255,255,255));
        l2.setLineColor(Color.rgb(255,255,255));

        pts[1] = l1.getLimit();
        pts[3] = l2.getLimit();

        mLeftAxisTransformer.pointValuesToPixel(pts);

        canvas.drawRect(mViewPortHandler.contentLeft(), pts[1], mViewPortHandler.contentRight(), pts[3], mYAxisSafeZonePaint);

        super.onDraw(canvas);
    }
}