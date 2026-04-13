package com.example.dpoae;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.widget.EditText;

import java.util.concurrent.locks.ReentrantLock;

public class CheckProbeFragment extends Fragment {

    TextView textView = null;
    SurfaceView surfaceView;
    SurfaceHolder holder;
    Canvas canvas;
    Paint paint;
    View view;
    int phaseIndex;
    boolean isDrawing;
    private RadioButton b1, b2, b3;
    int freqIndex;
    AudioStreamer sp;
    private Listener listener;
    private final int bufferLength = 32768; // also try: 16384 and 65536
    private boolean useCustomFFT = false;
    private boolean isRunning = false;
    private final short[] accumulatedSamples =
        new short[Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS];

    private float vol3a,vol3b,vol1;

    @Override
    public void onStop() {
        this.isRunning = false;
        this.isDrawing = false;
        this.StopSpeakersAndMic();
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //container.removeAllViews();
        view = inflater.inflate(
                R.layout.probe_check,
                container,
                false);
        ((MainActivity) getActivity()).getSupportActionBar().hide();

        surfaceView = view.findViewById(R.id.surfaceView);
        //surfaceView.postInvalidate();
        holder = surfaceView.getHolder();
        holder.getSurface();
        paint = new Paint();
        initView(view);
        final EditText f2ValueTxt = view.findViewById(R.id.f2Value);
        final EditText volumeValueTxt = view.findViewById(R.id.volumeValue);
        final EditText volumeF2te = view.findViewById(R.id.volumeF2v);
        final EditText volumeF1te = view.findViewById(R.id.volumeF1v);
        final Button buttonDraw = view.findViewById(R.id.buttonDraw);
        this.isRunning=true;
        this.isDrawing=false;
        b1 = (RadioButton)view.findViewById(R.id.khz3);
        b2 = (RadioButton)view.findViewById(R.id.khz4);
        b3 = (RadioButton)view.findViewById(R.id.khz5);

        // Find the RadioGroup in the layout. Add a listener to update frequency.
        RadioGroup radioGroup = view.findViewById(R.id.idRadioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                UpdateFreqIndex();

                // Refresh the running params
                if(isDrawing) RunNow();
            }
        });

        f2ValueTxt.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }

                Integer vv = tryParseInt(v.getText().toString());
                if (vv == null)
                {
                    v.setText(Constants.f2[freqIndex]+"");
                }
                else
                {
                    // let's update f1 from f2
                    int f1 = (int) (vv / 1.22);
                    int oae = (int) Math.round(2 * f1 - vv);
                    int oae2 = 2 * vv - f1;

                    // Get old frequencies
                    int prevF2 = Constants.octaves.get(freqIndex);
                    int prevF1 = Constants.f1[freqIndex];

                    // let's set f2 to the entered value
                    Constants.octaves.set(freqIndex, vv);
                    Constants.f2[freqIndex] = vv;

                    // Update related frequencies
                    Constants.freqLookup.put(vv, f1);
                    Constants.oaeLookup.put(vv, oae);
                    Constants.oaeLookup2.put(vv, oae2);
                    Constants.f1[freqIndex] = f1;

                    // Update related volume.
                    Constants.vol1Lookup.put(vv, Constants.vol1Lookup.get(prevF2));
                    Constants.vol1Lookup.put(f1, Constants.vol1Lookup.get(prevF1));
                    Constants.vol3Lookup.put(vv, Constants.vol3Lookup.get(prevF2));
                    Constants.vol3Lookup.put(f1, Constants.vol3Lookup.get(prevF1));

                    // let's persist the new f2 for the frequency index
                    // Save the new value in preferences
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putInt("f2Value_" + freqIndex, vv);
                    editor.commit();

                    if (isDrawing) RunNow();
                }

                return true;
            }
        });

        volumeValueTxt.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }

                int f2 = Constants.octaves.get(freqIndex);
                Float vv = tryParseFloat(v.getText().toString());
                if (vv == null)
                {
                    v.setText(Constants.vol1Lookup.get(f2)+"");
                }
                else
                {
                    // Save the new value in preferences
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putFloat("volumeValue_" + freqIndex, vv);
                    editor.commit();

                    Constants.vol1Lookup.put(f2, vv);

                    // Refresh the running params
                    if (isDrawing) RunNow();
                }

                return true;
            }
        });

        volumeF2te.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }

                Float volumeValue = tryParseFloat(v.getText().toString());
                int f2 = Constants.octaves.get(freqIndex);
                if (volumeValue == null)
                {
                    v.setText(Constants.vol3Lookup.get(f2)+"");
                }
                else
                {
                    // Save the new value in preferences
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putFloat("volumeF2te_" + freqIndex, volumeValue);
                    editor.commit();

                    Constants.vol3Lookup.put(f2, volumeValue);

                    // Refresh the running params
                    if (isDrawing) RunNow();
                }

                return true;
            }
        });

        volumeF1te.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }
                Float volumeValue = tryParseFloat(v.getText().toString());
                int f2 = Constants.octaves.get(freqIndex);
                int f1 = Constants.freqLookup.get(f2);
                if (volumeValue == null) {
                    v.setText(Constants.vol3Lookup.get(f1) + "");
                } else {
                    // Save the new value in preferences
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putFloat("volumeF1te_" + freqIndex, volumeValue);
                    editor.commit();

                    // Update the in memory volume that is shared.
                    Constants.vol3Lookup.put(f1, volumeValue);

                    // Refresh the running params
                    if (isDrawing) RunNow();
                }

                return true;
            }
        });

        buttonDraw.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                isDrawing = !isDrawing;
                if (isDrawing) {
                    buttonDraw.setText("Stop");

                    // Refresh the running params
                    RunNow();
                } else {
                    buttonDraw.setText("Start");
                    StopSpeakersAndMic();
                }
            }
        });

        freqIndex=-1;
        ((RadioButton)view.findViewById(R.id.khz3)).setChecked(true);

        view.setVisibility(View.VISIBLE);
        int w = view.getWidth();
        int h = view.getHeight();
        surfaceView.getHolder().setFixedSize(w - 100, h - 100);

        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.bringToFront();
        surfaceView.setZOrderOnTop(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // do your stuff
                while (true) {
                    if(!isRunning) return;
                    if (isDrawing) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    drawFft();
                                }
                            });
                        }
                    }

                    if(!isRunning) return;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();

        return view;
    }

    private void UpdateFreqIndex() {
        // get newFeqIndex from radio button
        int newFreqIndex = 0;
        if (b2.isChecked()) {
            newFreqIndex = 2;
        } else if (b3.isChecked()) {
            newFreqIndex = 3;
        } else if (b1.isChecked()) {
            newFreqIndex = 1;
        } else {
            newFreqIndex = 0;
        }

        if (freqIndex == newFreqIndex) return;

        freqIndex = newFreqIndex;

        int freq = Constants.octaves.get(freqIndex);
        int f1 = Constants.freqLookup.get(freq);
        int f2 = freq;

        final EditText f2ValueTxt = view.findViewById(R.id.f2Value);
        f2ValueTxt.setText(f2 + "");

        final EditText volumeValueTxt = view.findViewById(R.id.volumeValue);
        volumeValueTxt.setText(Constants.vol1Lookup.get(f2) + "");

        final EditText volumeF2te = view.findViewById(R.id.volumeF2v);
        volumeF2te.setText(Constants.vol3Lookup.get(f2) + "");

        final EditText volumeF1te = view.findViewById(R.id.volumeF1v);
        volumeF1te.setText(Constants.vol3Lookup.get(f1) + "");
    }

    private void RunNow()
    {
        int freq = Constants.octaves.get(freqIndex);
        int f1 = Constants.freqLookup.get(freq);
        int f2 = freq;

        this.StopSpeakersAndMic();
        vol3a = Constants.vol3Lookup.get(f1);
        vol3b = Constants.vol3Lookup.get(f2);

        short[] pulse = SignalGenerator.sine2speaker(
                f1,
                f2,
                Constants.samplingRate,
                Constants.CONSTANT_TONE_LENGTH_IN_SECONDS * Constants.samplingRate,
                vol3a,
                vol3b);

        vol1 = Constants.vol1Lookup.get(f2);

        FragmentActivity activity = getActivity();
        if (activity == null) return;

        // Let the volume buttons work.
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        sp = new AudioStreamer(activity, pulse, Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS * 2,
                Constants.samplingRate, AudioManager.STREAM_SYSTEM, vol1, true);

        sp.play(-1);

        // Start recording the microphone
        listener = new Listener(bufferLength);
        listener.start();
    }

    private void StopSpeakersAndMic()
    {
        if (listener != null) {
            listener.stopit();
            listener = null;
        }

        if (sp != null) {
            sp.stopit();
            sp = null;
        }
    }

    private void drawFft() {
        if (listener == null) return;

        // Read the data
        short[] b = listener.getAudio();

        // Draw it.
        Paint text = new Paint();
        text.setColor(Color.BLACK);
        text.setTextSize(60);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        int ww = view.getWidth();
        int hh = view.getHeight();
        surfaceView.getHolder().setFixedSize(ww - 200, hh - 200);

        canvas = surfaceView.getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        float a, w, h, phase;
        w = canvas.getWidth();
        h = canvas.getHeight();
        canvas.drawRGB(32, 32, 255);    //Clear the canvas to light blue
        Paint axis = new Paint();
        axis.setColor(Color.WHITE);
        axis.setStrokeWidth(5);
        canvas.drawLine(0, h / 1.1f, (int) w, h / 1.1f, axis);//Draw a line from left to right as the y=0 line.

        double[] avbuffer = new double[b.length];
        for (int i = 0; i < b.length; i++) avbuffer[i] = b[i];

        UpdateFreqIndex();
        int f1 = Constants.octaves.get(freqIndex);
        int f2 = Constants.freqLookup.get(f1);
        int fOae = Constants.oaeLookup.get(f1);

        // get the SNR.
        // TODO: To mimic real measurement, create short[288K] and add the current samples to the end of this array.
        // copy the last part to the beginning.
        // or example, when AccSamp.Len is 288K, and b.Len is 16K
        //   copy from 16K to end to index 0
        System.arraycopy(
                this.accumulatedSamples,
                b.length,
                this.accumulatedSamples,
                0,
                this.accumulatedSamples.length-b.length);

        // Append the last read buffer to AccSamples
        System.arraycopy(
                b,
                0,
                this.accumulatedSamples,
                this.accumulatedSamples.length-b.length,
                b.length);
        Constants.done=new boolean[4];
        Signal.work(null, this.accumulatedSamples,f2,f1, freqIndex);
        double snr = Constants.snrs[freqIndex]; //Constants.graphData.get(freqIndex).getY();
        double snr_f1 = Constants.snrs_f1[freqIndex];
        double snr_f2 = Constants.snrs_f2[freqIndex];

        double[] spec =
                this.useCustomFFT ?
                        new FFT().Fft(avbuffer) :
                        MeasureFragment.fftnative(avbuffer, avbuffer.length);

        // Display log scale
        for (int i = 0; i < spec.length; i++)
            spec[i]=Math.log10(spec[i]);

        // freq = i * Constants.samplingRate / b.length
        // draw from 0Hz to 6KHz
        int i6khz=(int)(6000.0/Constants.samplingRate * b.length);
        int xStart=0;
        int xEnd=i6khz;
        int[] freqMarkers=new int[]{f1,f2,fOae};
        paint.setStrokeWidth(5);
        int xLength=xEnd-xStart;

        // draw lines at f1, f2 and oae frequencies:
        axis.setColor(Color.RED);
        axis.setStrokeWidth(2);
        for(int ff : freqMarkers) {
            int x0 = (int) ((float) (ff) / xLength * w);
            int i0=(int)((float)(x0)/Constants.samplingRate * b.length);
            canvas.drawLine(i0, 0, i0, h, axis);
        }

        // compute max magnitude
        double max = 0;
        for (int xx = xStart; xx < xEnd; xx++)
            if (spec[xx] > max) max = spec[xx];

        for (int xx = xStart; xx < xEnd; xx++) {
            int y = (int) ((h / 1.1f) -  h / 1.1f * spec[xx] / max);

            int x = (int) (((float)xx-xStart) / xLength * w);

            canvas.drawPoint(x, y, paint);
        }

        text.setColor(Color.WHITE);
        text.setTextSize(80);
        canvas.drawText(
                String.format("%.2f", max),
                10,
                100,
                text);

        text.setColor(Color.WHITE);
        text.setTextSize(50);
        canvas.drawText(
                String.format(
                        "%d:%.1f, %d:%.1f, %d:%.1f", //, %f, %f, %f",
                        fOae,
                        snr,
                        f2,
                        snr_f2,
                        f1,
                        snr_f1),
                //,vol1,vol3a,vol3b),
                500,
                100,
                text);

        holder.unlockCanvasAndPost(canvas);
    }

    public void checkAction(int c, boolean isChecked) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        Log.e("asdf", "checkaction " + c + "," + isChecked);
        if (c == 1) {
            Log.e("asdf", "set check0 to " + isChecked);
            editor.putBoolean("check0", isChecked);
            Constants.freqs[0] = isChecked;
        } else if (c == 2) {
            editor.putBoolean("check1", isChecked);
            Constants.freqs[1] = isChecked;
        } else if (c == 3) {
            editor.putBoolean("check2", isChecked);
            Constants.freqs[2] = isChecked;
        } else if (c == 4) {
            editor.putBoolean("check3", isChecked);
            Constants.freqs[3] = isChecked;
        }
        for (int i = 0; i < Constants.freqs.length; i++) {
            Log.e("asdf", i + ":" + Constants.freqs[i]);
        }
        editor.commit();
    }

    public void initView(View view) {
        Log.e("asdf", "SET CHECKED ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Constants.CurrentFragment = this;
        Constants.CheckProbeFragment = this;
        this.isRunning=true;
        this.isDrawing=false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Constants.CurrentFragment = this;
        Constants.CheckProbeFragment = this;
        this.isRunning=true;
        this.isDrawing=false;
    }

    private static Integer tryParseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float tryParseFloat(String input) {
        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private class Listener extends Thread {
        private AudioRecord rec;
        int minbuffersize;
        private boolean recording;
        private ReentrantLock audioBufferLock;
        short[] buffer;
        int tail;

        public Listener(int bufLen) {
            minbuffersize = AudioRecord.getMinBufferSize(
                    Constants.samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            rec = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    Constants.samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minbuffersize);

            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc = AutomaticGainControl.create(rec.getAudioSessionId());
                agc.setEnabled(false);
            }

            this.tail = 0;
            this.buffer = new short[bufLen];
            this.audioBufferLock = new ReentrantLock();
        }

        public short[] getAudio() {
            this.audioBufferLock.lock();
            short[] b;
            try {
                if (this.tail == 0) {
                    return this.buffer.clone();
                }

                b = new short[this.buffer.length];
                int start = this.tail;
                System.arraycopy(this.buffer, start, b, 0, this.buffer.length - start);
                System.arraycopy(this.buffer, 0, b, this.buffer.length - start, start);
            } finally {
                this.audioBufferLock.unlock();
            }
            return b;
        }

        public void run() {
            int bytesread;
            rec.startRecording();
            recording = true;
            short[] temp = new short[minbuffersize];
            while (recording) {
                bytesread = rec.read(temp, 0, minbuffersize);

                this.audioBufferLock.lock();
                try {
                    for (int i = 0; i < bytesread; i++) {
                        this.buffer[this.tail] = temp[i];
                        this.tail = (this.tail + 1) % this.buffer.length;
                    }
                } finally {
                    this.audioBufferLock.unlock();
                }
            }
        }

        public void stopit() {
            if (rec.getState() == AudioRecord.STATE_INITIALIZED ||
                    rec.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                rec.stop();
            }

            recording = false;

            rec.release();
        }
    }

    private class FFT {

        public double[] Fft(double[] in) {
            Complex[] out = new Complex[in.length];
            for (int i = 0; i < in.length; i++) {
                out[i] = new Complex(in[i], (double)0.0);
            }

            // fft(out);
            fft2(out);
            for (int i = 0; i < in.length; i++) {
                in[i] = Math.sqrt(out[i].re * out[i].re + out[i].im * out[i].im);
            }

            return in;
        }

        public  void fft2(Complex[] buffer)
        {
            for (int i = 1; i < buffer.length; i++)
            {
                int j = BitReverse(i, buffer.length);
                if (j > i) {
                    // (buffer[j], buffer[i]) =(buffer[i], buffer[j]);
                    Complex tmp=buffer[j];
                    buffer[j]=buffer[i];
                    buffer[i]=tmp;
                }
            }

            for (int i = 1; i <= buffer.length / 2; i *= 2)
            {
                double mult1 = -Math.PI / i;
                for (int j = 0; j < buffer.length; j += (i * 2))
                {
                    for (int k = 0; k < i; k++)
                    {
                        int evenI = j + k;
                        int oddI = j + k + i;
                        Complex temp = new Complex(Math.cos(mult1 * k), Math.sin(mult1 * k));
                        //temp *= buffer[oddI];
                        temp = new Complex(temp.re*buffer[oddI].re-temp.im*buffer[oddI].im, temp.re*buffer[oddI].im+temp.im*buffer[oddI].re);
                        //buffer[oddI] = buffer[evenI] - temp;
                        buffer[oddI] = new Complex(buffer[evenI].re - temp.re,buffer[evenI].im - temp.im);
                        // buffer[evenI] += temp;
                        buffer[evenI] = new Complex(buffer[evenI].re + temp.re,buffer[evenI].im + temp.im);

                    }
                }
            }
        }

        private int BitReverse(int value, int maxValue)
        {
            int maxBitCount = (int)(Math.log(maxValue)/Math.log(2));
            int output = value;
            int bitCount = maxBitCount - 1;

            value >>= 1;
            while (value > 0)
            {
                output = (output << 1) | (value & 1);
                bitCount -= 1;
                value >>= 1;
            }

            return (output << bitCount) & ((1 << maxBitCount) - 1);
        }

        // Compute the FFT of an array of complex numbers
        public void fft(Complex[] x) {
            int n = x.length;

            // Base case
            if (n == 1) return;

            // Check if n is a power of 2
            if (Integer.bitCount(n) != 1) {
                throw new IllegalArgumentException("Length of x must be a power of 2");
            }

            // Divide
            Complex[] even = new Complex[n / 2];
            Complex[] odd = new Complex[n / 2];
            for (int i = 0; i < n / 2; i++) {
                even[i] = x[2 * i];
                odd[i] = x[2 * i + 1];
            }

            // Conquer
            fft(even);
            fft(odd);

            // Combine
            for (int k = 0; k < n / 2; k++) {
                double kth = -2 * k * Math.PI / n;
                Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
                x[k] = even[k].plus(wk.times(odd[k]));
                x[k + n / 2] = even[k].minus(wk.times(odd[k]));
            }
        }

        // Complex number class
        public class Complex {
            private final double re;   // real part
            private final double im;   // imaginary part

            public Complex(double real, double imag) {
                re = real;
                im = imag;
            }

            public Complex plus(Complex b) {
                return new Complex(this.re + b.re, this.im + b.im);
            }

            public Complex minus(Complex b) {
                return new Complex(this.re - b.re, this.im - b.im);
            }

            public Complex times(Complex b) {
                return new Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re);
            }

            @Override
            public String toString() {
                if (im == 0) return re + "";
                return re + " " + im;
            }
        }
    }
}
