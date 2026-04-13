package com.example.dpoae;

import android.util.Log;

import java.util.LinkedList;

public class Utils {

    public static String trim (String s){
        return s.substring(1,s.length()-2);
    }

    public static String volcalib(short[] samples) {
//        Log.e("asdf","vol calib");
        short[] pre=Utils.generateChirpSpeaker(1000,4000,.1,Constants.samplingRate,0,.5);
        double[] samples_d = Utils.convert(samples);

        double[] s1=Utils.segment(samples_d,0,Constants.samplingRate/2-1);
        double[] corr=xcorr_helper(Utils.convert(pre),s1);
        int max_idx = (int)max_idx(corr)[0];
        int xcorr_idx = (transform_idx(max_idx, s1.length));
        Log.e("asdf","xcorr "+xcorr_idx);

        int idx=xcorr_idx+pre.length;

        int counter=0;
        double[] defaults=new double[]{};
        double[] maxvals=new double[]{};
        if (Constants.phone.equals("sch")) {
//            defaults=new double[]{.19, .13, .13, .14, .2, .19, .5, .55};
//            maxvals=new double[]{.19, .13, .13, .14, .2, .19, .5, .55};
            defaults=new double[]{.16,.04,.11,.03,.28,.08,.95,.026};
            maxvals=new double[]{.26,.14,.21,.13,.38,.18,1,.05};
        }
        else if (Constants.phone.equals("sch2")) {
//            defaults=new double[]{.11,.05,.13,.04,.14,.03,.16,.05};
//            maxvals=new double[]{.11,.05,.15,.07,.14,.08,.3,.13};
            defaults=new double[]{.01,.05,.13,.04,.14,.03,.16,.05};
            maxvals=new double[]{1,1,1,1,1,1,1,1};
        }
        else {
            defaults = new double[]{.11, .05, .13, .04, .14, .03, .16, .05};
            maxvals = new double[]{.5, .5, .5, .5, .5, .5, .5, .5};
        }

        LinkedList<Float> vols=new LinkedList<>();
        for (int i = 0; i < 4; i++) {
            int ss1 = (int)(idx+(Constants.PAD_CALIB_LENGTH_IN_SECONDS*Constants.samplingRate));
            int ee1 = (int)(ss1+(Constants.EXAMINE_CALIB_LENGTH_IN_SECONDS*Constants.samplingRate));
            int ss2 = (int)(ss1+(Constants.TONE_CALIB_LENGTH_IN_SECONDS*Constants.samplingRate));
            int ee2 = (int)(ss2+(Constants.EXAMINE_CALIB_LENGTH_IN_SECONDS*Constants.samplingRate));

            double[] seg1 = Utils.segment(samples_d,ss1,ee1);
            double[] seg2 = Utils.segment(samples_d,ss2,ee2);

            int freq = Constants.octaves.get(i);
            int f1 = Constants.freqLookup.get(freq);
            int f2 = freq;

            double[] vv1 = compute(f1,seg1);
            double[] vv2 = compute(f2,seg2);

            vols.add((float)vv1[1]);
            vols.add((float)vv2[1]);

            double v1=vv1[0];
            double v2=vv2[0];
            Log.e("calib",String.format("vol calib %d %.2f %.2f %.2f",f1,maxvals[counter],defaults[counter],v1));
            Log.e("calib",String.format("vol calib %d %.2f %.2f %.2f",f2,maxvals[counter+1],defaults[counter+1],v2));

            if (v1 > maxvals[counter] || v2 > maxvals[counter+1]) {
                v1=defaults[counter];
                v2=defaults[counter+1];
            }

            Constants.vol3Lookup.put(f1,(float)v1);
            Constants.vol3Lookup.put(f2,(float)v2);
            idx+=(Constants.TONE_CALIB_LENGTH_IN_SECONDS * Constants.samplingRate)*2;
            counter+=2;
        }
        String out = "";
        for (Float f:vols) {
            out += String.format("%.0f\t",f);
        }

        Log.e("calib","----------------------------");
        return out;
    }

    public static double[] segment(double[] data, int i, int j) {
        double[] out = new double[j-i+1];
        int counter=0;
        for (int k = i; k <= j; k++) {
            out[counter++] = data[k];
        }
        return out;
    }

    public static int transform_idx(int maxidx, int sig_len) {
        return sig_len-(maxidx*2)-1;
    }

    public static double[] max_idx(double[] corr) {
        double maxval=0;
        int maxidx=0;
        for (int i = 0; i < corr.length; i++) {
            if (corr[i]>maxval){
                maxval=corr[i];
                maxidx=i;
            }
        }
        return new double[]{maxidx,maxval};
    }

    public static double[] xcorr_helper(double[] preamble, double[] sig) {
//        Log.e("asdf","xcorr a");
        double[][] a = SignalProcessing.fftcomplexoutnative_double(preamble, sig.length);
//        Log.e("asdf","xcorr b");
        double[][] b = SignalProcessing.fftcomplexoutnative_double(sig, sig.length);
//        Log.e("asdf","xcorr conj");
        SignalProcessing.conjnative(b);
//        Log.e("asdf","xcorr mult");
        double[][] multout = SignalProcessing.timesnative(a, b);
//        Log.e("asdf","xcorr ifft");
        double[] corr = SignalProcessing.ifftnative(multout);
        return corr;
    }

    public static short[] generateChirpSpeaker2(double startFreq, double endFreq, double time, double fs, double initialPhase,double scale) {
        int samplen=(int)(time*fs);
        short[] signal = new short[samplen*2];
        short[] signal1;
        short[] signal2;

        signal1 = generateChirpSpeaker(startFreq,endFreq,time,fs,initialPhase,scale);
        signal2 = generateChirpSpeaker(startFreq,endFreq,time,fs,initialPhase,scale);

        int counter = 0;
        for (int i = 0; i < signal.length; i+=2) {
            signal[i] = signal1[counter];
            signal[i+1] = signal2[counter];
            counter += 1;
        }

        return signal;
    }

    public static short[] generateChirpSpeaker(double startFreq, double endFreq, double time, double fs, double initialPhase,double scale) {
        int N = (int) (time * fs);
        short[] ans = new short[N];
        double f = startFreq;
        double k = (endFreq - startFreq) / time;
        double mult=(32767)*scale;
        for (int i = 0; i < N; i++) {
            double t = (double) i / fs;
            double phase = initialPhase + 2*Math.PI*(startFreq * t + 0.5 * k * t * t);
            phase = AngularMath.Normalize(phase);
            ans[i] = (short) (Math.sin(phase) * mult);
        }

        return ans;
    }

    public static double[] mag2db(double[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = 20*Math.log10(sig[i]);
        }
        return out;
    }

    public static double[] convert(short[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = sig[i];
        }
        return out;
    }

    public static double[] compute(int freq, double[] sig) {
        int fidx=0;
        int idx=0;
        int target=0;
        double[] p1_arr=new double[]{};
        double[] p2_arr=new double[]{};
        double[] a_arr=new double[]{};
        double[] b_arr=new double[]{};
        if (Constants.phone.equals("sch")) {
//            p1_arr = new double[]{1.0053, 0.9125, 1.0098, 0.805, 0.9277, 0.6434, 0.8187, 0.632};
//            p2_arr = new double[]{-51.0025, -36.7244, -47.901, -30.0993, -50.2189, -15.1802, -36.1408, -12.4538};
//            a_arr = new double[]{6.44E-09, 4.53E-07, 3.60E-07, 9.21E-07, 1.34E-07, 7.90E-07, 2.24E-07, 1.38E-06};
//            b_arr = new double[]{0.137, 0.1156, 0.1105, 0.1093, 0.1149, 0.1164, 0.1154, 0.1155};
            p1_arr = new double[]{1.007452171,	1.017885848	,1.042927416,	0.8731073507,	0.6004675468,	0.964022883,	0.8201684785,	0.6677347943};
            p2_arr = new double[]{-56.39272591,	-52.13974248,	-55.08372389,	-26.95887362,	-3.569798211,	-48.20108798,	-35.93794362,	-14.10560189};
            a_arr = new double[]{4.84E-08,	1.36E-07,	1.05E-07,	3.70E-07,	2.22E-07,	5.82E-07,	4.52E-07,	9.94E-07};
            b_arr = new double[]{0.115511649,	0.1144855946,	0.1137014971,	0.1106786399,	0.1117217011,	0.115077944,	0.1155163777,	0.1152659994};
        }
        else if (Constants.phone.equals("sch2")) {
            p1_arr = new double[]{0.9987	,0.9431	,0.9716,	0.9245	,0.9786,	0.8732,	0.8763,	0.8571};
            p2_arr = new double[]{-52.7356,	-44.4052	,-50.4828,	-39.9062	,-46.5441	,-28.9041,	-30.4703,	-25.5625};
            a_arr = new double[]{4.84E-08	,1.36E-07,	1.05E-07	,3.70E-07,	2.22E-07	,5.82E-07	,4.52E-07,	9.94E-07};
            b_arr = new double[]{0.1155,	0.1145	,0.1137,	0.1107,	0.1117,	0.1151	,0.1155	,0.1153};
        }
        else if (Constants.phone.equals("pixel_calib")) {
            p1_arr = new double[]{8.899738e-01,9.188492e-01,8.031414e-01,7.648207e-01,8.895420e-01,6.494005e-01,5.693497e-01,9.814025e-01,};
            p2_arr = new double[]{-4.783751e+01,-5.028895e+01,-3.908130e+01,-3.830530e+01,-4.961602e+01,-1.889693e+01,-1.340673e+01,-5.816146e+01};
            a_arr = new double[]{2.150073e-07,1.793038e-07,2.732658e-07,2.041620e-07,1.584278e-07,7.946715e-07,8.109509e-07,1.561679e-07};
            b_arr = new double[]{1.141896e-01,1.149387e-01,1.152425e-01,1.142550e-01,1.151937e-01,1.167582e-01,1.142981e-01,1.159984e-01};
        }
        else if (Constants.phone.equals("doogee_calib")) {
            p1_arr = new double[]{1.006488e+00,1.037554e+00,1.017368e+00,1.007323e+00,1.031130e+00,1.082373e+00,1.082226e+00,1.144910e+00};
            p2_arr = new double[]{-4.970536e+01,-5.254169e+01,-5.328073e+01,-5.773659e+01,-5.781152e+01,-4.571608e+01,-4.849192e+01,-5.669839e+01};
            a_arr = new double[]{2.603486e-07,1.635032e-07,2.813279e-07,2.144288e-07,1.792826e-07,9.899952e-07,7.992111e-07,2.185157e-07};
            b_arr = new double[]{1.147314e-01,1.137447e-01,1.143069e-01,1.138815e-01,1.140720e-01,1.152128e-01,1.151401e-01,1.146910e-01};
        }
        else if (Constants.phone.equals("s9_calib")) {
            p1_arr = new double[]{1.030956e+00,1.113889e+00,1.054289e+00,1.113885e+00,1.145863e+00,1.113196e+00,1.111393e+00,1.002606e+00};
            p2_arr = new double[]{-6.544654e+01,-7.390370e+01,-7.060380e+01,-8.357173e+01,-8.519220e+01,-6.269027e+01,-6.521994e+01,-4.890644e+01,};
            a_arr = new double[]{5.940501e-09,8.681964e-51,1.437035e-08,1.225836e-13,1.644459e-16,9.181461e-08,7.256393e-08,1.353063e-08};
            b_arr = new double[]{1.235853e-01,7.559621e-01,1.181949e-01,1.950025e-01,2.370042e-01,1.155345e-01,1.154535e-01,1.184705e-01,};
        }
        else if (Constants.phone.equals("infinix_calib")) {
            p1_arr = new double[]{1.036786e+00,1.102240e+00,1.077586e+00,1.179059e+00,1.104962e+00,8.318945e-01,1.103898e+00,1.069293e+00};
            p2_arr = new double[]{-5.341115e+01,-5.969034e+01,-6.101754e+01,-7.821308e+01,-6.333625e+01,-2.350830e+01,-6.227088e+01,-5.487936e+01};
            a_arr = new double[]{8.894243e-13,1.402285e-14,7.588520e-10,9.899953e-14,1.609218e-14,1.848852e-07,1.365041e-07,1.413295e-17};
            b_arr = new double[]{1.910244e-01,2.207547e-01,1.457586e-01,2.071174e-01,2.181065e-01,1.151009e-01,1.153789e-01,2.671818e-01};
        }

        if (freq==1640) {
            fidx=164;
            idx=0;
        } else if (freq==2016) {
            fidx=202;
            idx=1;
        }
        else if (freq==2438) {
            fidx=244;
            idx=2;
        } else if (freq==2953) {
            fidx=295;
            idx=3;
            target=55;
        } else if (freq==3282) {
            fidx=328;
            idx=4;
        } else if (freq==3985) {
            fidx=399;
            idx=5;
        } else if (freq==4078) {
            fidx=408;
            idx=6;
        } else if (freq==4969) {
            fidx=497;
            idx=7;
        }

        if (Constants.phone.equals("sch")) {
            if (idx == 0 || idx == 2) {
                target = 65;
            } else if (idx == 1 || idx == 3) {
                target = 55;
            }
            if (idx == 4 || idx == 6) {
                target = 65;
            }
            if (idx == 5 || idx == 7) {
                target = 55;
            }
        }
        else if (Constants.phone.equals("sch2")) {
            if (idx == 0 || idx == 2) {
                target = 65;
            } else if (idx == 1 || idx == 3) {
                target = 55;
            }
            if (idx == 4 || idx == 6) {
                target = 65;
            }
            if (idx == 5 || idx == 7) {
                target = 55;
            }
//            if (idx == 0) {
//                target = 63;
//            }
//            else if (idx==2) {
//                target=58;
//            } else if (idx == 1 || idx == 3) {
//                target = 50;
//            }
//            if (idx == 4) {
//                target = 70;
//            }
//            else if (idx==6) {
//                target=75;
//            }
//            if (idx == 5 || idx == 7) {
//                target = 50;
//            }
        }
        else {
            if (idx == 0 || idx == 2) {
                target = 60;
            } else if (idx == 1 || idx == 3) {
                target = 50;
            }
            if (idx == 4 || idx == 6) {
                target = 60;
            }
            if (idx == 5 || idx == 7) {
                target = 50;
            }
        }

        double[] spec=SignalProcessing.fftnative(sig,sig.length);
        spec = mag2db(spec);
        double amp = spec[fidx];
        double spl = p1_arr[idx]*amp+p2_arr[idx];
        double delta = target-spl;
        double target_rx_idx = amp+delta;
        double target_tx_idx=a_arr[idx]*Math.exp(b_arr[idx]*target_rx_idx);

//        Log.e("asdf",String.format("amp %.0f spl %.0f delta %.0f target_rx_idx %.0f",amp,spl,delta,target_rx_idx));

        return new double[]{target_tx_idx,amp};
    }
}
