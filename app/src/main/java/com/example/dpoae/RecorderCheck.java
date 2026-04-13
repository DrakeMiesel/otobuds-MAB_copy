package com.example.dpoae;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;


public class RecorderCheck extends Thread {

    public short[] temp;
    public AudioRecord rec;
    boolean recording;
    int count;
    int minbuffersize;
    double gainAttack;
    double gainRelease;
    double attackTime=1;
    double releaseTime=1;
    MyBarChart barChart;
    Activity av;
    int freq;
    TextView prepTextView;

    public RecorderCheck(int microphone, int freq, MyBarChart chart, Activity av, TextView prepTextView) {
        this.barChart = chart;
        this.av = av;
        this.freq = freq;
        this.prepTextView = prepTextView;

        minbuffersize = AudioRecord.getMinBufferSize(Constants.samplingRate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        minbuffersize = 24000;

        rec = new AudioRecord(microphone,
                Constants.samplingRate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minbuffersize);

        gainAttack = Math.exp(-1.0/(Constants.samplingRate*attackTime));
        gainRelease = Math.exp(-1.0/(Constants.samplingRate*releaseTime));

        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl agc = AutomaticGainControl.create(
                    rec.getAudioSessionId()
            );
            agc.setEnabled(false);
        }

        temp = new short[minbuffersize];
    }

    public double envelope(short[] sig) {
        double envOut = 0;
        double accum = 0;
        for (int i = 0; i < sig.length; i++) {
            double envIn = Math.abs(sig[i]);
            if (envOut < envIn) {
                envOut = envIn + gainAttack*(envOut-envIn);
            }
            else {
                envOut = envIn + gainRelease*(envOut-envIn);
            }
            accum += envOut;
        }
        double out = accum/sig.length;
        return 20*Math.log10(out);
    }

    public double[] convert2double(short[] data) {
        double[] out = new double[data.length];
        for(int i = 0 ; i < data.length; i++) {
            out[i] = data[i];
        }
        return out;
    }

    public int[] convert(double[] data) {
        int[] out = new int[data.length];
        for(int i = 0 ; i < data.length; i++) {
            out[i] = (int)(20*Math.log10(data[i]));
        }
        return out;
    }

    public void slog(String veryLongString) {
        int maxLogSize = 1000;
        for(int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i+1) * maxLogSize;
            end = end > veryLongString.length() ? veryLongString.length() : end;
            Log.v("logme", veryLongString.substring(start, end));
        }
    }

    public double spec(short[] sig) {
        int fac = Constants.samplingRate/sig.length;
        double[] pows = MeasureFragment.fftnative(convert2double(sig),sig.length);
        double bin = pows[freq/fac];
        bin = 20 * Math.log10(bin);
//        Log.e("out","bin "+bin);
        return bin;
    }

    public void run() {
        rec.startRecording();
        recording = true;
        while(recording) {
            rec.read(temp,0,minbuffersize);
            double val = spec(temp);
            graphme((float)val);
        }
    }

    public void stopit() {
        rec.stop();
        rec.release();
        recording = false;
    }

    public void graphme(float val) {
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepTextView.setText((int)val+"");
                ArrayList<BarEntry> data = new ArrayList<>();
                data.add(new BarEntry(0,new float[]{val}));
                BarDataSet dataset = new BarDataSet(data,"");
                dataset.setValueTextSize(0f);
                dataset.setColors(new int[]{av.getColor(R.color.colorPrimary)});

                BarData barData = new BarData(dataset);
                barData.setBarWidth(.25f);
                barChart.setData(barData);

                barChart.setDescription(null);    // Hide the description
                barChart.getAxisRight().setDrawLabels(false);
                barChart.getAxisLeft().setAxisMinimum(Constants.PROBE_CHECK_MIN);
                barChart.getAxisLeft().setAxisMaximum(Constants.PROBE_CHECK_MAX);
                barChart.getAxisRight().setAxisMinimum(Constants.PROBE_CHECK_MIN);
                barChart.getAxisRight().setAxisMaximum(Constants.PROBE_CHECK_MAX);
                barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

                barChart.setFitBars(true);
                barChart.getLegend().setEnabled(false);   // Hide the legend.

                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }
        });
    }
}
