package com.example.dpoae;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.LinkedList;


public class OfflineRecorder extends Thread {

    public short[] samples;
    public short[] temp;
    public AudioRecord rec;
    boolean recording;
    int count;
    int minbuffersize;
    int fidx;
    int tidx;
    int freq;
    ExtendedBarChart barChart;
    LineChart lineChart;
    Activity av;
    boolean checkfit;
    AudioStreamer sp;
    ProgressBar pb;
    TextView tv;
    Chip passChip;
    boolean ss=false;
    boolean overrideTcheck=false;
    boolean calib;

    public OfflineRecorder(int microphone, int fidx, int tidx, int freq, ExtendedBarChart barChart, LineChart lineChart, Activity av, int bufferLen, boolean checkfit, boolean calib) {
        this.checkfit = checkfit;
        this.calib = calib;
        this.fidx = fidx;
        this.tidx = tidx;
        this.freq = freq;
        this.barChart = barChart;
        this.lineChart = lineChart;
        this.av = av;

        envs = new LinkedList<Integer>();
        minbuffersize = AudioRecord.getMinBufferSize(
                Constants.samplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        rec = new AudioRecord(
                microphone,
                Constants.samplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minbuffersize);

        if (Constants.AGC) {
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc = AutomaticGainControl.create(
                        rec.getAudioSessionId()
                );
                agc.setEnabled(true);
            }
        }
        else {
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc = AutomaticGainControl.create(
                        rec.getAudioSessionId()
                );
                agc.setEnabled(false);
            }
        }

        temp = new short[minbuffersize];
        samples = new short[bufferLen];
    }

    public void stopit() {
        Log.e("asdf","rec stopit");
        if (rec.getState() == AudioRecord.STATE_INITIALIZED||
            rec.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            rec.stop();
        }
        rec.release();
        recording = false;
    }

    public double envelope(short[] sig) {
        double envOut = 0;
        double accum=0;

        double attackTime=1;
        double releaseTime=1;
        double gainAttack = Math.exp(-1.0/(Constants.samplingRate*attackTime));
        double gainRelease = Math.exp(-1.0/(Constants.samplingRate*releaseTime));

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
        return accum/sig.length;
    }

    public double[] convert2(short[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = sig[i];
        }
        return out;
    }

    public static double mean(double[] sig, int start, int end) {
        double mm = 0;
        for (int i = start; i <= end; i++) {
            mm += sig[i];
        }
        return mm / (end - start + 1);
    }

    public static double[] movingaverage(double[] spectrum, int windowsize) {
        ArrayList<Double> out = new ArrayList<>();
        for (int n = windowsize / 2; n <= windowsize; n++) {
            out.add(mean(spectrum, 0, n - 1));
        }

        for (int cc = 1; cc <= spectrum.length - windowsize; cc++) {
            out.add(mean(spectrum, cc, cc + windowsize - 1));
        }

        int n = windowsize - 1;
        int cc = spectrum.length - windowsize + 1;
        while (out.size() != spectrum.length) {
            out.add(mean(spectrum, cc, cc + n - 1));
            n -= 1;
            cc += 1;
        }

        cc = 0;
        double[] outarray = new double[spectrum.length];
        for (Double d : out) {
            outarray[cc++] = d;
        }
        return outarray;
    }

    LinkedList<Integer> envs;
    public boolean envCheck(short[] sig) {
        double eval=0;
        if (Constants.calibSig.equals("chirp")) {
            double[] sig2 = convert2(sig);
            double[] sig3 = MeasureFragment.fftnative(sig2, sig2.length);
            double[] seg = new double[577];
            int fcounter = 64;
            for (int i = 0; i < seg.length; i++) {
                seg[i] = sig3[fcounter++];
            }
            double[] sig4 = movingaverage(seg, 50);
            eval=mean(sig4,0,sig4.length-1)/4000;
        }
        else {
            eval = envelope(sig);
        }

        envs.add((int)eval);

        boolean tcheck = threshCheck(25, 1, Constants.SEAL_CHECK_THRESH, Constants.SEAL_OCCLUSION_THRESH);
        if (tcheck||overrideTcheck) {
            Log.e("asdf","TCHECK TRUE "+Constants.CheckFitProgress+","+overrideTcheck);
            av.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Constants.dialog.dismiss();
                }
            });
            sp.stopit();
            FileOperations.writeFileToDisk(av,envs);
            envs.clear();
            return true;
        }
        return false;
    }

    public boolean threshCheck(int num, int recnumber, int lowbound, int upbound) {
        if (envs.size() < num) {
            return false;
        }

        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int p2 = (int)((Constants.CheckFitProgress/(double)lowbound)*100);
                if (pb!=null) {
                    pb.setProgress(p2);
                }
                if (tv!=null) {
                    tv.setText("Checking fit (" + (int) Constants.CheckFitProgress + "/" + lowbound + ")");
//                tv.setText("Checking fit");
                }
            }
        });
        
        for(int i = 1; i<= num; i++) {
            if (envs.get(envs.size()-i) < lowbound) {
                Constants.CheckFitProgress=envs.get(envs.size()-1);
                Log.e("sealinfo",recnumber+" NO SEAL "+envs.get(envs.size()-i)+","+lowbound);
                return false;
            }
            else if(envs.get(envs.size()-i) > upbound) {
                Constants.CheckFitProgress=envs.get(envs.size()-1);
                Log.e("sealinfo", recnumber+" OCCLUSION "+envs.get(envs.size()-i)+","+upbound);

                return false;
            }
        }

        Constants.CheckFitProgress=envs.get(envs.size()-1);
        Log.e("sealinfo",recnumber+" SEAL "+envs.get(envs.size()-1));

        return true;
    }

    public void run() {
        if (passChip!=null) {
            passChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    overrideTcheck = true;
                }
            });
        }

        int bytesread;
        rec.startRecording();
        recording = true;
//        long start = System.currentTimeMillis();
        while(recording) {
            bytesread = rec.read(temp,0,minbuffersize);
//            double secondsElapsed=temp.length/48000.0;
//            Constants.secondCounter+=secondsElapsed;
//            double progress = Constants.secondCounter/Constants.totalSeconds;

//            Log.e("asdf","bytes "+bytesread+","+calib);
            if (checkfit) {
                try {
//                    Log.e("env",(System.currentTimeMillis()-start)+"");
                    if (envCheck(temp)) {
                        Log.e("asdf", "envcheck is true");
                        stopit();
                        break;
                    }
                }
                catch(Exception e) {
                    Log.e("asdf",e.getMessage());
                }
                for (int i = 0; i < bytesread; i++) {
                    if (count >= samples.length) {
                    } else {
                        samples[count] = temp[i];
                        count += 1;
                    }
                }
            }
            else {
                for (int i = 0; i < bytesread; i++) {
                    if (count >= samples.length) {
                        recording = false;
//                    Log.e("asdf","rec done");
                        if (!checkfit) {
//                        Constants.fullrec.add(samples);
                        }
                        break;
                    } else {
                        samples[count] = temp[i];
                        count += 1;
                    }
                }
            }
        }
        if (tidx==1) {
            Log.e("asdf","asdf");
        }
        if (!checkfit &&!calib) {
            // normal case
            Constants.fullrec.add(samples);
            int f2 = freq;
            int f1 = Constants.freqLookup.get(f2);
            Signal.work(av,samples,f1,f2, fidx);
            Grapher.graph(barChart, lineChart, av, Constants.graphData, Constants.lineData1, Constants.lineData2, ss);
        }
//        if (checkfit||calib) {
//            FileOperations.writeFileToDisk(av,samples,checkfit,calib);
//        }
        if (calib) {
            Utils.volcalib(samples);
            stopit();
        }
    }
    public double[] convert(short[] data) {
        double[] out = new double[data.length];
        for(int i = 0 ; i < data.length; i++) {
            out[i] = data[i];
        }
        return out;
    }
}
