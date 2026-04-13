package com.example.dpoae;

import java.util.Arrays;

public class SignalProcessing {

    public static double[] convert(short[] sig) {
        double[] out = new double[sig.length];
        for(int i = 0; i < sig.length; i++) {
            out[i] = sig[i];
        }
        return out;
    }

    public static double[] xcorr(double[] sig, double[] preamble) {
        double[][] a = SignalProcessing.fftcomplexoutnative(preamble,sig.length*2);

        double[][] b = SignalProcessing.fftcomplexoutnative(sig,sig.length*2);

        for (int i = 0; i < b[1].length; i++) {
            b[1][i] = -b[1][i];
        }

        double[][] multout = SignalProcessing.timesnative(a,b);

        double[] out = SignalProcessing.ifftnative(multout);

        int o = out.length/2;
        for (int i = 0; i < out.length/2; i++) {
            double temp = out[i];
            out[i] = out[o];
            out[o] = temp;
            o++;
        }

        reverse(out);

        return out;
    }


    public static void reverse(double[] sig) {
        for(int i = 0; i < sig.length / 2; i++) {
            double temp = sig[i];
            sig[i] = sig[sig.length - i - 1];
            sig[sig.length - i - 1] = temp;
        }
    }

    public static int getmax(double[] sig) {
        int maxind = 0;
        double maxval = 0;
        for (int i = 0; i < sig.length; i++) {
            if (sig[i] > maxval) {
                maxval = sig[i];
                maxind = i;
            }
        }
        return (maxind-(sig.length/2))+1;
    }

    public static native double[][] fftcomplexoutnative_short(double[] data, int N);
    public static native double[][] fftcomplexoutnative_double(double[] data, int N);
    public static native double[] fftnative(double[] data, int N);
    public static native double[][] fftcomplexoutnative(double[] data, int N);
    public static native double[] ifftnative(double[][] data);
    public static native void conjnative(double[][] data);
    public static native double[][] timesnative(double[][] data1,double[][] data2);
}
