package com.example.dpoae;

import android.app.Activity;
import android.util.Log;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;

import java.util.Arrays;
import java.util.LinkedList;

public class Signal {
    public static int ampCheck(short[] samples) {
        int counter=0;
        int summer=0;
        int maxav=0;
        for (int i = 0; i < samples.length; i++) {
            summer+=samples[i];
            counter+=1;
            if (counter==1000) {
                int av=summer/1000;
                if (av>maxav) {
                    maxav=av;
                }
                counter=0;
            }
        }
        return maxav;
    }

    public static double calcArt(double[] spec) {
        double ss=0;
        for (int i = 0; i <6000; i++) {
            ss+=10*Math.log10(spec[i]);
        }
        return (ss/6000.0);
    }

    public static void getMinMax(LinkedList<Double> data) {
        double mmin=999;
        double mmax=-999;
        for (Double d : data) {
            if (d < mmin) {
                mmin=d;
            }
            if (d > mmax) {
                mmax=d;
            }
        }
//        Log.e("signal", (int)(mmax-mmin)+"");
    }

    public static double[] convert(short[] ss) {
        double[] a = new double[ss.length];
        for (int i = 0; i < ss.length; i++) {
            a[i]=ss[i];
        }
        return a;
    }

    public static double[] work(Activity av,short[] samples, int freq1, int freq2, int fidx) {
//        int seglen = Constants.samplingRate;
        double fac=1;
        int seglen = (int)(Constants.samplingRate/fac);
        int numsegs = (samples.length - Constants.INIT_SIGNAL_TRIM_LENGTH) / seglen;

        int cc = Constants.INIT_SIGNAL_TRIM_LENGTH;

        int trackTone = (int)Math.ceil(Constants.oaeLookup.get(freq2)/fac);
        int trackTone2 = (int)Math.ceil(Constants.oaeLookup2.get(freq2)/fac);
        double[] sumbuffer = new double[seglen];
        double[] avbuffer = new double[seglen];
        int segcounter=0;
        double[] spec=null;
        LinkedList<Double> snrs = new LinkedList<>();
        LinkedList<Double> signals = new LinkedList<>();
        LinkedList<Double> noises = new LinkedList<>();
        LinkedList<Double> arts = new LinkedList<>();
        for (int i = 0; i < numsegs; i++) {
            short[] out = Arrays.copyOfRange(samples, cc, cc + seglen);
            cc = cc + seglen;
            segcounter += 1;

            for (int j = 0; j < seglen; j++) {
                sumbuffer[j] += out[j];
                avbuffer[j] = sumbuffer[j] / segcounter;
            }

            spec = MeasureFragment.fftnative(avbuffer, seglen);

            for (int k = 0; k < spec.length; k++) {
                spec[k] = spec[k] * spec[k];
            }

            double[] val=calcSNR(spec, seglen, trackTone);
            double snr = (val[0]-val[1]);
            Log.e("debug","> "+freq2+","+((int)val[0])+","+((int)val[1])+","+((int)snr));
//            Log.e("opt",String.format("%.0f,%.0f,%.0f",val[0],val[1],snr));

//            signals.add(val[0]);
//            noises.add(val[1]);
//            snrs.add(snr);
            double art = calcArt(spec);
//            Log.e("art",String.format("%d %.2f %.2f %.2f %.2f",i,val[0],val[1], snr, art));
            boolean OPTIMIZE = true;
            if (arts.size()>=1) {
                // if the artifact has increased here, ignore this segment
                // OR if artifact has decreased but SNR has instead decreased ignore it
//                if (OPTIMIZE && (art-arts.getLast() >= Constants.ART_THRESH || snr < -25 || art < arts.getLast() && snr < snrs.getLast())) {
//                Log.e("debug","snr "+(snr));
//                Log.e("debug","art "+(art));
//                Log.e("debug","noise "+(val[1]));
                if (OPTIMIZE &&
                        (art-arts.getLast() >= Constants.ART_THRESH ||
                                snr < 0 ||
//                                val[1] >= Constants.threshs[fidx] ||
                                (snr < snrs.getLast() && Math.abs(snr - snrs.getLast()) >= Constants.SNR_DIP_THRESH))) {
                    Log.e("debug", "go here ");
//                    for(Double d : arts) {
//                        Log.e("opt","art "+d);
//                    }
//                    Log.e("opt","art "+art);
//                    for(Double d : snrs) {
//                        Log.e("opt","snr "+d);
//                    }
//                    Log.e("opt","snr "+snr);
//                    Log.e("opt","a "+i);
//                if (art-arts.getLast() >= Constants.ART_THRESH || snr < -25) {
//                    Log.e("art", "IGNORE "+i);
                    segcounter -= 1;
                    for (int j = 0; j < seglen; j++) {
                        sumbuffer[j] -= out[j];
                    }
                    for (int j = 0; j < seglen; j++) {
                        avbuffer[j] = sumbuffer[j] / segcounter;
                    }
                    spec = MeasureFragment.fftnative(avbuffer, seglen);
                    for (int k = 0; k < spec.length; k++) {
                        spec[k] = spec[k] * spec[k];
                    }
                    val=calcSNR(spec, seglen, trackTone);
                    snr = (val[0]-val[1]);
                    Log.e("debug",">> "+freq2+","+((int)val[0])+","+((int)val[1])+","+((int)snr));
                    if (signals.size()>0) {
                        signals.removeLast();
                    }
                    signals.add(val[0]);
                    if (noises.size()>0) {
                        noises.removeLast();
                    }
                    noises.add(val[1]);
                    art = calcArt(spec);
                    if (arts.size()>0) {
                        arts.removeLast();
                    }
                    arts.add(art);
                    if (snrs.size()>0) {
                        snrs.removeLast();
                    }
                    snrs.add(snr);
                }
                else {
                    arts.add(art);
                    snrs.add(snr);
                    signals.add(val[0]);
                    noises.add(val[1]);
                }
            }
//            else if (arts.size()==0 && snr < Constants.SNR_THRESH) {
//            else if (OPTIMIZE && snr < Constants.SNR_THRESH) {
            else if (OPTIMIZE) {
                // if this is the first segment, and snr is below thresh
                // look at the time domain to see if there is noise, if there is, ignore it.
//                int out2 = ampCheck(out);
//                if (out2>Constants.AMP_THRESH) {
                if (
//                    val[1]>=Constants.threshs[fidx] ||
                    snr < 0) {
//                if (false) {
//                    Log.e("debug", "IGNORE "+out2);
                    segcounter-=1;
                    for (int j = 0; j < seglen; j++) {
                        sumbuffer[j] -= out[j];
//                        if (i==numsegs-1) {
//                            avbuffer[j] = sumbuffer[j] / segcounter;
//                        }
                    }
                    for (int j = 0; j < seglen; j++) {
                        avbuffer[j] = sumbuffer[j] / segcounter;
                    }
                    spec = MeasureFragment.fftnative(avbuffer, seglen);
                    for (int k = 0; k < spec.length; k++) {
                        spec[k] = spec[k] * spec[k];
                    }
                    val=calcSNR(spec, seglen, trackTone);
                    snr = (val[0]-val[1]);
                    Log.e("debug",">>2 "+freq2+","+((int)val[0])+","+((int)val[1])+","+((int)snr));
                    if (signals.size()>=1) {
                        signals.removeLast();
                    }
                    signals.add(val[0]);
                    if (noises.size()>=1) {
                        noises.removeLast();
                    }
                    noises.add(val[1]);
                    if (arts.size()>=1) {
                        arts.removeLast();
                    }
                    art = calcArt(spec);
                    arts.add(art);
                    if (snrs.size()>=1) {
                        snrs.removeLast();
                    }
                    snrs.add(snr);
                }
                else {
                    arts.add(art);
                    snrs.add(snr);
                    signals.add(val[0]);
                    noises.add(val[1]);
                }
            }
            else {
                arts.add(art);
                snrs.add(snr);
                signals.add(val[0]);
                noises.add(val[1]);
            }
        }

        double maxsnr=0;
        for (Double s : snrs) {
            maxsnr=s;
        }

        getMinMax(signals);
//        String arr = Arrays.toString(spec);
        double[] calcf1=calcSNR(spec, seglen, freq1);
        double[] calcf2=calcSNR(spec, seglen, freq2);

        double[] calc1=calcSNR(spec, seglen, trackTone);
        double[] calc2=calcSNR(spec, seglen, trackTone2);

        Log.e("snr","f1 ("+freq1+"): "+(int)calcf1[0]+","+(int)calcf1[1]+","+(int)(calcf1[0]-calcf1[1]));
        Log.e("snr","f2 ("+freq2+"): "+(int)calcf2[0]+","+(int)calcf2[1]+","+(int)(calcf2[0]-calcf2[1]));
        Log.e("snr","oae ("+trackTone+"): "+(int)calc1[0]+","+(int)calc1[1]+","+(int)(calc1[0]-calc1[1]));

        double ambient_noise = 0;
        int counter=0;
        for (int i = 0; i < 1000; i++) {
            ambient_noise += 10*Math.log10(spec[i]);
            counter++;
        }
        ambient_noise /= counter;

        double snr1=calc1[0]-calc1[1];
        double snr2=calc2[0]-calc2[1];

//        Log.e("opt",String.format("%.0f,%.0f,%.0f",calc1[0],calc1[1],snr1));
        //snr1 is the snr of the OAE frequency 2*f1-f2
        //snr2 is the snr of the IMD frequency 2*f2-f1

        double signal=0;
        double noise=0;
//        if (snr2 > Constants.IMD_THRESH) {
//            //snr is then snr1-snr2
//            signal=snr1;
//            noise=snr2;
////            Log.e("final","imd");
//        }
//        else {
        signal=calc1[0];
        noise=calc1[1];
//        }

        double snr=Math.ceil(signal-noise);
//        Log.e("art",String.format("%.2f %.2f",signal,noise));
        Constants.signal[fidx] = signal;
        Constants.noise[fidx] = noise;
        Constants.snrs[fidx] = signal-noise;
        Constants.ambient[fidx] = ambient_noise;
        Constants.signal_f1[fidx] = calcf1[0];
        Constants.signal_f2[fidx] = calcf2[0];
        Constants.noise_f1[fidx] = calcf1[1];
        Constants.noise_f2[fidx] = calcf2[1];
        Constants.snrs_f1[fidx] = calcf1[0]-calcf1[1];
        Constants.snrs_f2[fidx] = calcf2[0]-calcf2[1];
        Log.e("warning","SIG "+fidx+","+Constants.signal_f1[fidx]+","+Constants.signal_f2[fidx]);
        Log.e("warning","NOISE "+fidx+","+Constants.noise_f1[fidx]+","+Constants.noise_f2[fidx]);
        Log.e("warning","SNRS "+fidx+","+Constants.snrs_f1[fidx]+","+Constants.snrs_f2[fidx]);
//        Log.e("snr",(int)signal+","+(int)noise+","+(int)snr);
        //            Log.e("out", "*** " + Constants.signal[fidx] + "," + Constants.noise[fidx] + "," + snr);
//        double dbsplsig = Constants.signal[fidx] - Constants.volOffset.get(trackTone);
//        double dbsplnoise = Constants.noise[fidx] - Constants.volOffset.get(trackTone);
        double dbsplsig = Constants.signal[fidx];
        double dbsplnoise = Constants.noise[fidx];
//            Log.e("spl",trackTone+","+Constants.volOffset.get(trackTone)+","+(int)dbsplsig+",");

        // TODO
//            dbspl = 0;
//        if (snr >= Constants.SNR_THRESH && dbsplsig >= Constants.SPL_THRESH) {
//            Constants.complete[fidx] = true;
//        }
//        Log.e("justin","SNR: "+Math.ceil(snr));
        if (Math.ceil(snr) >= Constants.SNR_THRESHS[fidx]) {
            Constants.complete[fidx] = true;
        }

        float xx = (float) roundToHalf(Constants.octaves.get(fidx) / 1000.0);
        if (fidx < Constants.graphData.size()) {
            Constants.graphData.set(fidx, new BarEntry(freq2 / 1000f, new float[]{(float) snr}));
            Constants.lineData1.add(new Entry(xx, (float) dbsplsig));
            Constants.lineData2.add(new Entry(xx, (float) dbsplnoise));
        } else {
            Constants.graphData.add(new BarEntry(freq2 / 1000f, new float[]{(float) snr}));
            Constants.lineData1.add(new Entry(xx, (float) dbsplsig));
            Constants.lineData2.add(new Entry(xx, (float) dbsplnoise));
        }
//        Log.e("opt","--------------------------");
//        return 10 * Math.log10(spec[freq]);
//        double signal1 = 10 * Math.log10(spec[freq]);
//        double signal2 = 10 * Math.log10(spec[Constants.freqLookup.get(freq)]);
        Constants.done[fidx] = true;
        return new double[]{calc1[0],calc1[1],ambient_noise};
//        return new double[]{calc1[0],calc1[1],calc2[0],calc2[1]};
    }

    public static double max(double[] arr) {
        double mmax=0;
        for (Double d : arr) {
            if (d > mmax) {
                mmax=d;
            }
        }
        return mmax;
    }

    public static double[] calcSNR(double[] spec, int seglen, int trackTone) {
//        Log.e("asdf","tracktone "+trackTone);
        final int bin = trackTone / (Constants.samplingRate / seglen);
//        final int bin = trackTone;

        int tol = 2;
        int fwindow = (int) Math.ceil(200 / (Constants.samplingRate / seglen));
        double signal = spec[bin];

        double[] arr1 = Arrays.copyOfRange(spec, bin - fwindow - tol, (bin - tol)+1);
        double[] arr2 = Arrays.copyOfRange(spec, bin + tol, (bin + tol + fwindow)+1);

        double noisesum = 0;
        for (Double j : arr1) {
            noisesum += j;
        }
        for (Double j : arr2) {
            noisesum += j;
        }
        double noise = noisesum/(arr1.length+arr2.length);

//        double max1=max(arr1);
//        double max2=max(arr2);
//        double noise = max1>max2?max1:max2;

        signal = 10 * Math.log10(signal);
        noise = 10 * Math.log10(noise);

        return new double[]{signal, noise};
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }
}
