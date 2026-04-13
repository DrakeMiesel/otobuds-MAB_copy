package com.example.dpoae;

import android.app.Activity;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Grapher {

    public static void graph(ExtendedBarChart barChart, LineChart lineChart, Activity av, ArrayList<BarEntry> data, List<Entry> lineData1, List<Entry>lineData2, boolean ss) {
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphHelper(barChart, av, data);
                graphHelper2(av, lineChart, lineData1, lineData2);

                if (ss) {
                    Constants.vv.setDrawingCacheEnabled(true);
                    Constants.vv.buildDrawingCache(true);

                    Bitmap bb = Constants.vv.getDrawingCache();
                    Bitmap bitmap = Bitmap.createBitmap(bb);
                    Constants.vv.setDrawingCacheEnabled(false);

                    MediaStore.Images.Media.insertImage(
                            Constants.context.getContentResolver(), bitmap, Constants.filename, "");  // Saves the image.
                    Toast.makeText(Constants.context, "Screenshot captured", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static void graphHelper2(Activity av, LineChart lineChart, List<Entry> lineData1, List<Entry>lineData2) {
        LineDataSet data1 = new LineDataSet(lineData1, "");
        data1.setColors(new int[] {av.getColor(R.color.colorPrimaryDark)});
        data1.setCircleColor(av.getColor(R.color.colorPrimaryDark));
        data1.setValueTextSize(15f);
        data1.setDrawCircleHole(true);
        data1.setCircleHoleColor(av.getColor(R.color.colorPrimaryDark));

        LineDataSet data2 = new LineDataSet(lineData2, "");
        data2.setColors(new int[] {av.getColor(R.color.colorDarkRed)});
        data2.setCircleColor(av.getColor(R.color.colorDarkRed));
        data2.setValueTextSize(15f);
        data2.setDrawCircleHole(true);
        data2.setCircleHoleColor(av.getColor(R.color.colorDarkRed));

        List<ILineDataSet> data = new ArrayList<>();
        data.add(data1);
        data.add(data2);

        LineData barData = new LineData(data);
        lineChart.setData(barData);
//        barData.setBarWidth(.1f);
        barData.setValueFormatter(new MyValueFormatter());

        lineChart.setDescription(null);    // Hide the description
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getAxisRight().setAxisMinimum(-20);
        lineChart.getAxisRight().setAxisMaximum(50);
        lineChart.getAxisLeft().setAxisMinimum(-20);
        lineChart.getAxisLeft().setAxisMaximum(50);

        lineChart.getXAxis().setAxisMinimum(.5f);
        lineChart.getXAxis().setAxisMaximum(6.5f);
        lineChart.getXAxis().setYOffset(-.5f);
        lineChart.getXAxis().setAvoidFirstLastClipping(true);

        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setTextSize(15);
        lineChart.getAxisLeft().setTextSize(15);

        lineChart.getLegend().setEnabled(false);   // Hide the legend.

        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    public static void graphHelper(BarChart barChart, Activity av, ArrayList<BarEntry> data) {
        BarDataSet dataset = new BarDataSet(data,"");

        int[] carray;
        if (Constants.complete==null) {
            carray = new int[] {av.getColor(R.color.colorDarkGreen)};
        }
        else {
            carray = new int[Constants.complete.length];
            for (int i = 0; i < Constants.complete.length; i++) {
                if (Constants.complete[i]||Math.ceil(data.get(i).getY()) >=Constants.SNR_THRESHS[i]) {
                    Log.e("justin","colorgreen-"+i);
                    carray[i] = av.getColor(R.color.colorDarkGreen);
                } else {
                    Log.e("justin","colorred-"+i);
                    carray[i] = av.getColor(R.color.colorDarkRed);
                }
            }
        }

        dataset.setColors(carray);
        dataset.setValueTextSize(15f);

        BarData barData = new BarData(dataset);
        barChart.setData(barData);
        barData.setBarWidth(.5f);
        barData.setValueFormatter(new MyValueFormatter());
    
        barChart.setDescription(null);    // Hide the description
        barChart.getAxisRight().setDrawLabels(false);
        barChart.getAxisRight().setAxisMinimum(-10);
        barChart.getAxisRight().setAxisMaximum(40);
        barChart.getAxisLeft().setAxisMinimum(-10);
        barChart.getAxisLeft().setAxisMaximum(40);

        barChart.getXAxis().setAxisMinimum(.5f);
        barChart.getXAxis().setAxisMaximum(6.5f);

        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setYOffset(-.5f);
        barChart.getXAxis().setAvoidFirstLastClipping(true);
        barChart.getXAxis().setTextSize(15);
        barChart.getAxisLeft().setTextSize(15);

        barChart.getLegend().setEnabled(false);   // Hide the legend.

        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }
}
