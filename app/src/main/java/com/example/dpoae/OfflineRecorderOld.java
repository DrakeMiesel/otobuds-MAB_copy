//package com.example.dpoae;
//
//import android.app.Activity;
//import android.media.AudioFormat;
//import android.media.AudioRecord;
//import android.media.audiofx.AutomaticGainControl;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.github.mikephil.charting.charts.BarChart;
//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.data.BarEntry;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.google.android.material.chip.Chip;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedList;
//import java.util.Random;
//
//
//public class OfflineRecorderOld extends Thread {
//
//    public short[] samples;
//    public short[] temp;
//    public AudioRecord rec;
//    boolean recording;
//    int count;
//    int minbuffersize;
//    int fidx;
//    int tidx;
//    int freq;
//    BarChart barChart;
//    LineChart lineChart;
//    Activity av;
//    Random random = new Random();
//    boolean calib;
//    AudioStreamer sp;
//    ProgressBar pb;
//    TextView tv;
//    Chip passChip;
//    boolean overrideTcheck=false;
//
//    public OfflineRecorder(int microphone, int fidx, int tidx, int freq, BarChart barChart, LineChart lineChart, Activity av, int bufferLen, boolean calib) {
//        this.calib = calib;
//        this.fidx = fidx;
//        this.tidx = tidx;
//        this.freq = freq;
//        this.barChart = barChart;
//        this.lineChart = lineChart;
//        this.av = av;
//
//        envs = new LinkedList<Integer>();
//        minbuffersize = AudioRecord.getMinBufferSize(
//                Constants.samplingRate,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT);
//
//        rec = new AudioRecord(
//                microphone,
//                Constants.samplingRate,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                minbuffersize);
//
//        if (Constants.AGC) {
//            if (AutomaticGainControl.isAvailable()) {
//                AutomaticGainControl agc = AutomaticGainControl.create(
//                        rec.getAudioSessionId()
//                );
//                agc.setEnabled(true);
//            }
//        }
//        else {
//            if (AutomaticGainControl.isAvailable()) {
//                AutomaticGainControl agc = AutomaticGainControl.create(
//                        rec.getAudioSessionId()
//                );
//                agc.setEnabled(false);
//            }
//        }
//
//        temp = new short[minbuffersize];
//        samples = new short[bufferLen];
//    }
//
//    public void stopit() {
//        Log.e("asdf","rec stopit");
//        if (rec.getState() == AudioRecord.STATE_INITIALIZED||
//                rec.getState() == AudioRecord.RECORDSTATE_RECORDING) {
//            rec.stop();
//        }
//        rec.release();
//        recording = false;
//    }
//
//    public double envelope(short[] sig) {
//        double envOut = 0;
//        double accum=0;
//
//        double attackTime=1;
//        double releaseTime=1;
//        double gainAttack = Math.exp(-1.0/(Constants.samplingRate*attackTime));
//        double gainRelease = Math.exp(-1.0/(Constants.samplingRate*releaseTime));
//
//        for (int i = 0; i < sig.length; i++) {
//            double envIn = Math.abs(sig[i]);
//            if (envOut < envIn) {
//                envOut = envIn + gainAttack*(envOut-envIn);
//            }
//            else {
//                envOut = envIn + gainRelease*(envOut-envIn);
//            }
//            accum += envOut;
//        }
//        return accum/sig.length;
//    }
//
//    public double[] convert2(short[] sig) {
//        double[] out = new double[sig.length];
//        for (int i = 0; i < sig.length; i++) {
//            out[i] = sig[i];
//        }
//        return out;
//    }
//
//    public static double mean(double[] sig, int start, int end) {
//        // DK accumulate individual signal elements
//        double mm = 0;
//        for (int i = start; i <= end; i++) {
//            mm += sig[i];
//        }
//        // DK return (arithmetic) average of input signal elements
//        return mm / (end - start + 1);
//    }
//
//    public static double[] movingaverage(double[] spectrum, int windowsize) {
//        // DK TODO seems this could be a simple double[] array ??
//        // DK TODO seems like the ...add(mean(...)) involve unnecessary casts
//        ArrayList<Double> out = new ArrayList<>();
//        // DK perform on-ramp smoothing operations TODO double-check
//        for (int n = windowsize / 2; n <= windowsize; n++) {
//            out.add(mean(spectrum, 0, n - 1));
//        }
//
//        // DK perform bulk smoothing operations TODO double-check
//        for (int cc = 1; cc <= spectrum.length - windowsize; cc++) {
//            out.add(mean(spectrum, cc, cc + windowsize - 1));
//        }
//
//        // DK perform off-ramp smoothing operations TODO double-check
//        int n = windowsize - 1;
//        int cc = spectrum.length - windowsize + 1;
//        while (out.size() != spectrum.length) {
//            out.add(mean(spectrum, cc, cc + n - 1));
//            n -= 1;
//            cc += 1;
//        }
//
//        // DK cast back data to double[] type
//        // TODO seems like this is unnecessary consequence of declaring
//        // 'out' as (list of) type Double.
//        cc = 0;
//        double[] outarray = new double[spectrum.length];
//        for (Double d : out) {
//            outarray[cc++] = d;
//        }
//        return outarray;
//    }
//
//    LinkedList<Integer> envs;
//    public boolean envCheck(short[] sig) {
//        double eval=0;
//        if (Constants.calibSig.equals("chirp")) {
//            double[] sig2 = convert2(sig);
//            double[] sig3 = MeasureFragment.fftnative(sig2, sig2.length);
//            double[] seg = new double[577];
//            int fcounter = 64;
//            for (int i = 0; i < seg.length; i++) {
//                seg[i] = sig3[fcounter++];
//            }
//            double[] sig4 = movingaverage(seg, 50);
//            eval=mean(sig4,0,sig4.length-1)/4000;
//        }
//        else {
//            eval = envelope(sig);
//        }
//
//        envs.add((int)eval);
////        envsStr.add(System.currentTimeMillis()+","+(int)eval);
//
////        Log.e("logme","EVAL "+eval);
//        boolean tcheck = threshCheck(8, 1, Constants.SEAL_CHECK_THRESH, Constants.SEAL_OCCLUSION_THRESH);
//        if (tcheck||overrideTcheck) {
//            Log.e("asdf","TCHECK TRUE "+progress+","+overrideTcheck);
//            av.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Constants.dialog.dismiss();
//                }
//            });
//            sp.stopit();
//            FileOperations.writeFileToDisk(av,envs);
//            envs.clear();
//            return true;
//        }
//        return false;
//    }
//
//    double progress;
//    public boolean threshCheck(int num, int recnumber, int lowbound, int upbound) {
////        Log.e("asdf","threshcheck");
//        if (envs.size() < num) {
////            Log.e("asdf","threshcheck ret false");
//            return false;
//        }
//
//        av.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                Log.e("asdf","progress "+(int)progress);
//                int p2 = (int)((progress/(double)lowbound)*100);
//                pb.setProgress(p2);
//                tv.setText("Calibrating... ("+(int)progress+"/"+lowbound+")");
//            }
//        });
//
//        for(int i = 1; i<= num; i++) {
////            Log.e("asdf","threshcheck "+envs.get(envs.size()-i));
//            if (envs.get(envs.size()-i) < lowbound) {
//                progress=envs.get(envs.size()-1);
//                Log.e("sealinfo",recnumber+" NO SEAL "+envs.get(envs.size()-i));
//
////                if (Constants.snackBar!=null && Constants.snackBarMsg.equals("Tip is occluded")) {
////                    Constants.snackBar.dismiss();
////                    Constants.snackBarMsg="";
////                }
//
////                envs = new LinkedList<Integer>();
//                return false;
//            }
//            else if(envs.get(envs.size()-i) > upbound) {
//                progress=envs.get(envs.size()-1);
//                Log.e("sealinfo", recnumber+" OCCLUSION "+envs.get(envs.size()-i)+","+upbound);
//
////                ((MainActivity)av).runOnUiThread(new Runnable() {
////                    public void run() {
////                        if (recnumber==1&&!Constants.snackBarMsg.equals("Tip is occluded")) {
////                            Log.e("logme",Constants.snackBarMsg);
////
////                            if (Constants.notifType.equals("toast")) {
////                                Utils.mkToast(((MainActivity) av),
////                                        "Tip is occluded");
////                            }
////                            else {
////                                Utils.mkSnack(((MainActivity) av), av.findViewById(R.id.layout),
////                                        "Tip is occluded");
////                            }
////                        }
////                    }});
//
////                envs = new LinkedList<Integer>();
//                return false;
//            }
//        }
//
////        if (Constants.snackBar!=null && Constants.snackBarMsg.equals("Tip is occluded")) {
////            Constants.snackBar.dismiss();
////            Constants.snackBarMsg="";
////        }
//
//        progress=envs.get(envs.size()-1);
//        Log.e("sealinfo",recnumber+" SEAL "+envs.get(envs.size()-1));
////        envs = new LinkedList<Integer>();
//
//        return true;
//    }
//
//    public void run() {
//        if (passChip!=null) {
//            passChip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                    overrideTcheck = true;
//                }
//            });
//        }
//
//        int bytesread;
//        rec.startRecording();
//        recording = true;
//        while(recording) {
//            bytesread = rec.read(temp,0,minbuffersize);
////            Log.e("asdf","bytes "+bytesread);
//            if (calib) {
//                try {
//                    if (envCheck(temp)) {
//                        Log.e("asdf", "envcheck is true");
//                        stopit();
//                        break;
//                    }
//                }
//                catch(Exception e) {
//                    Log.e("asdf",e.getMessage());
//                }
//            }
//            else {
//                for (int i = 0; i < bytesread; i++) {
//                    if (count >= samples.length) {
//                        recording = false;
////                    Log.e("asdf","rec done");
//                        if (!calib) {
////                        Constants.fullrec.add(samples);
//                        }
//                        break;
//                    } else {
//                        samples[count] = temp[i];
//                        count += 1;
//                    }
//                }
//            }
//        }
//        if (tidx==1) {
//            Log.e("asdf","asdf");
//        }
//
////        if (Constants.INTERLEAVED || calib) {
////            short[] out = Arrays.copyOfRange(samples, Constants.INIT_SIGNAL_TRIM_LENGTH, Constants.INIT_SIGNAL_TRIM_LENGTH + 12000);
////            final double[] out2 = MeasureFragment.fftnative(convert(out), 12000);
////            for (int i = 0; i < out2.length; i++) {
////                out2[i] = (20 * Math.log10(out2[i]));
////            }
////            Constants.volCalibMags.add((int) out2[freq / 4]);
////            //        Constants.volCalibMags.add(0);
////
////            if (!calib) {
////                postExec((int) freq);
////
////                double snr = Constants.signal[fidx] - Constants.noise[fidx];
////                Log.e("out", "*** " + Constants.signal[fidx] + "," + Constants.noise[fidx] + "," + snr);
////                double dbspl = Constants.signal[fidx] - Constants.volOffset.get(freq);
////                // TODO
//////                double dbspl = 0;
////                if (snr >= Constants.SNR_THRESH && dbspl >= Constants.SPL_THRESH) {
////                    Constants.complete[fidx] = true;
////                }
////
////                if (fidx < Constants.graphData.size()) {
////                    Constants.graphData.set(fidx, new BarEntry(freq / 1000f, new float[]{(float) snr}));
////                } else {
////                    Constants.graphData.add(new BarEntry(freq / 1000f, new float[]{(float) snr}));
////                }
////                Grapher.graph(barChart, lineChart, av, Constants.graphData, Constants.lineData1, Constants.lineData2);
////            }
////        }
////        else {
//        if (!calib) {
//            // normal case
//            Constants.fullrec.add(samples);
////            int seglen=Constants.samplingRate;
////            int numsegs=(samples.length-Constants.INIT_SIGNAL_TRIM_LENGTH)/seglen;
////            int cc = Constants.INIT_SIGNAL_TRIM_LENGTH;
////
////            double[] fullsig = new double[seglen];
////            for (int i = 0; i < numsegs; i++) {
////                short[] out = Arrays.copyOfRange(samples, cc, cc + seglen);
////                cc = cc + seglen;
////                for (int j = 0; j < seglen; j++) {
////                    fullsig[j] += out[j];
////                }
////            }
////            for (int i = 0; i < fullsig.length; i++) {
////                fullsig[i] /= numsegs;
////            }
////
////            final double[] spec = MeasureFragment.fftnative(fullsig, seglen);
////            for (int j = 0; j < spec.length; j++) {
////                spec[j] = spec[j]*spec[j];
////            }
////
////            int trackTone = Constants.oaeLookup.get(freq);
////            final int bin = trackTone/(Constants.samplingRate/seglen);
////
////            int tol = 2;
////            int fwindow = (int)Math.ceil(20/(Constants.samplingRate/seglen));
////            double signal = spec[bin];
////
////            double[] arr1 = Arrays.copyOfRange(spec,bin-fwindow-tol,bin-tol);
////            double[] arr2 = Arrays.copyOfRange(spec,bin+tol,bin+tol+fwindow);
////
////            double noisesum = 0;
////            for(Double j : arr1) {
////                noisesum+=j;
////            }
////            for(Double j : arr2) {
////                noisesum+=j;
////            }
////            double noise = (noisesum/(arr1.length+arr2.length));
////
////            Log.e("spl","bin "+bin+","+(int)spec[bin]+","+(int)(10*Math.log10(signal)));
////
////            signal = 10*Math.log10(signal);
////            noise = 10*Math.log10(noise);
////            Constants.signal[fidx] = signal;
////            Constants.noise[fidx] = noise;
////
////            double snr = signal-noise;
////            Log.e("out", "*** " + Constants.signal[fidx] + "," + Constants.noise[fidx] + "," + snr);
////            double dbsplsig = Constants.signal[fidx] - Constants.volOffset.get(trackTone);
////            double dbsplnoise = Constants.noise[fidx] - Constants.volOffset.get(trackTone);
////
////            Log.e("spl",trackTone+","+Constants.volOffset.get(trackTone)+","+(int)dbsplsig+",");
////
////            // TODO
//////            dbspl = 0;
////            if (snr >= Constants.SNR_THRESH && dbsplsig >= Constants.SPL_THRESH) {
////                Constants.complete[fidx] = true;
////            }
////
////            float xx=(float)roundToHalf(Constants.octaves.get(fidx)/1000.0);
////            if (fidx < Constants.graphData.size()) {
////                Constants.graphData.set(fidx, new BarEntry(freq / 1000f, new float[]{(float) snr}));
////                Constants.lineData1.add(new Entry(xx, (float)dbsplsig));
////                Constants.lineData2.add(new Entry(xx, (float)dbsplnoise));
//////                Log.e("asdf","xxx "+xx);
////
////            } else {
////                Constants.graphData.add(new BarEntry(freq / 1000f, new float[]{(float) snr}));
////                Constants.lineData1.add(new Entry(xx, (float)dbsplsig));
////                Constants.lineData2.add(new Entry(xx, (float)dbsplnoise));
////            }
////            Grapher.graph(barChart, lineChart, av, Constants.graphData, Constants.lineData1, Constants.lineData2);
//        }
//    }
//    public static double roundToHalf(double d) {
//        return Math.round(d * 2) / 2.0;
//    }
//    public double[] convert(short[] data) {
//        double[] out = new double[data.length];
//        for(int i = 0 ; i < data.length; i++) {
//            out[i] = data[i];
//        }
//        return out;
//    }
//
//    public void clog(double[] arr) {
//        String ss = "";
//        for (int i = 24000; i < 24000+10; i++) {
//            ss+=arr[i]+",";
//        }
//        Log.e("out",ss);
//    }
//
//    public void postExec(int f2) {
//        int trackTone = Constants.oaeLookup.get(f2);
//
//        short[] trimmedSamples;
//        if (tidx > 0) {
//            short[] template = Arrays.copyOfRange(Constants.initbuffer[fidx], 0, Constants.TEMPLATE_LENGTH);
//            short[] subsig = Arrays.copyOfRange(samples,
//                    Constants.INIT_SIGNAL_TRIM_LENGTH, Constants.INIT_SIGNAL_TRIM_LENGTH+Constants.TEMPLATE_LENGTH);
//            double[] xcorrout = SignalProcessing.xcorr(SignalProcessing.convert(subsig), SignalProcessing.convert(template));
//            int startpoint = SignalProcessing.getmax(xcorrout)+Constants.INIT_SIGNAL_TRIM_LENGTH;
//            Log.e("trim","startpoint "+startpoint+","+samples.length);
//            //TODO startpoint can be negative...
//            if (startpoint < 0) {
//                startpoint = 0;
//            }
//            trimmedSamples = Arrays.copyOfRange(samples, startpoint, startpoint+Constants.PROCESS_WINDOW_LENGTH);
//        }
//        else {
//            trimmedSamples = Arrays.copyOfRange(samples, Constants.INIT_SIGNAL_TRIM_LENGTH,
//                    Constants.INIT_SIGNAL_TRIM_LENGTH+Constants.PROCESS_WINDOW_LENGTH);
//        }
//
//        Constants.fullrec.add(trimmedSamples);
//
//        double[] currentAv = new double[Constants.PROCESS_WINDOW_LENGTH];
//
//        Constants.numTries[fidx] += 1;
//
//        if (tidx == 0) {
//            for (int i = 0; i < Constants.PROCESS_WINDOW_LENGTH; i++) {
//                Constants.initbuffer[fidx][i] = trimmedSamples[i];
//            }
//        }
//
//        for (int i = 0; i < Constants.PROCESS_WINDOW_LENGTH; i++) {
//            Constants.sumbuffer[fidx][i] += trimmedSamples[i];
//            currentAv[i] = Constants.sumbuffer[fidx][i] / Constants.numTries[fidx];
//        }
//
//        int fftwin = Constants.PROCESS_WINDOW_LENGTH;
//        final double[] spec = MeasureFragment.fftnative(currentAv, fftwin);
////        final double[] spec = MeasureFragment.fftnative(convert(trimmedSamples), fftwin);
//
//        final double[] specout = new double[spec.length];
//        for (int i = 0; i < spec.length; i++) {
//            specout[i] = spec[i]*spec[i];
//        }
//
////        final int bin = (int)(trackTone/(Constants.samplingRate/((double)Constants.RECORDING_WINDOW_IN_SECONDS * Constants.samplingRate)));
//        final int bin = trackTone/(Constants.samplingRate/fftwin);
//        Log.e("debug","bin: "+bin);
//
//        int tol = 2;
//        int fwindow = (int)Math.ceil(20/(Constants.samplingRate/Constants.PROCESS_WINDOW_LENGTH));
//        double signal = specout[bin];
//
//        double[] arr1 = Arrays.copyOfRange(specout,bin-fwindow-tol,bin-tol);
//        double[] arr2 = Arrays.copyOfRange(specout,bin+tol,bin+tol+fwindow);
//
//        double noisesum = 0;
//        for(Double i : arr1) {
//            noisesum+=i;
//        }
//        for(Double i : arr2) {
//            noisesum+=i;
//        }
//        double noise = (noisesum/(arr1.length+arr2.length));
//
//        signal = 10*Math.log10(signal);
//        noise = 10*Math.log10(noise);
//
//        Log.e("debug","debug "+(int)signal+","+(int)noise+","+(int)(signal-noise));
//
//        Constants.signal[fidx] = signal;
//        Constants.noise[fidx] = noise;
//    }
//}
