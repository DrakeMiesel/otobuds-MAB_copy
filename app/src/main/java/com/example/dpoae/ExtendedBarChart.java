package com.example.dpoae;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;

public class ExtendedBarChart extends BarChart {
    boolean done=false;
    View vv;
    Activity context;
    public ExtendedBarChart(Context context) {
        super(context);
    }

    public ExtendedBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (done) {
//            Bitmap bb = Bitmap.createBitmap(vv.getWidth(), vv.getHeight(), Bitmap.Config.ARGB_8888);
//            PixelCopy.request(context.getWindow(), new Rect(0, 0, vv.getWidth(), vv.getHeight()), bb, new PixelCopy.OnPixelCopyFinishedListener() {
//                @Override
//                public void onPixelCopyFinished(int i) {
//                    return;
//                }
//            }, new Handler());
//
//            MediaStore.Images.Media.insertImage(
//                    context.getContentResolver(), bb, Constants.filename, "");  // Saves the image.
//
//            Toast.makeText(context, "Screenshot captured", Toast.LENGTH_LONG).show();

//            View v = context.getWindow().getDecorView().getRootView();
    //            vv.setDrawingCacheEnabled(true);
    //            vv.buildDrawingCache(true);

        }
    }
}