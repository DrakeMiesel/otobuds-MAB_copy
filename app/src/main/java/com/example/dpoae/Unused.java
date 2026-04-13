package com.example.dpoae;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.ProgressBar;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Unused {

//    public void calibVolume() {
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Constants.dialog = loadingDialog();
//                Constants.dialog.show();
//            }
//        });
//
//        int calibLen=32000;
//        AudioStreamer sp1 = new AudioStreamer(context, calibLen*2,
//                Constants.samplingRate, AudioManager.STREAM_SYSTEM);
//        sp=sp1;
//        ArrayList<Integer> freqs = getList();
//
//        for(int i = 0; i < freqs.size(); i++) {
//            long ts = System.currentTimeMillis();
//
//            float vol3a = Constants.vol3Lookup.get(freqs.get(i));
//            float vol3b = Constants.vol3Lookup.get(freqs.get(i));
//
//            short[] pulse = SignalGenerator.sine2speaker(
//                    freqs.get(i), freqs.get(i), Constants.samplingRate, calibLen,
//                    vol3a, vol3b);
//
//            sp.prime(pulse, Constants.vol1LookupDefaults.get(freqs.get(i)));
//
//            int micType;
//            if (Constants.AGC) {
//                AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
//                if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
//                    micType = (MediaRecorder.AudioSource.UNPROCESSED);
//                } else {
//                    micType = (MediaRecorder.AudioSource.VOICE_RECOGNITION);
//                }
//            } else {
//                micType = MediaRecorder.AudioSource.DEFAULT;
//            }
//
//            // TODO: make sure the recording and speaker overlap in time for sufficiently long
//            OfflineRecorder orec = new OfflineRecorder(micType, -1, -1,
//                    freqs.get(i), barChart, lineChart, context, calibLen,true);
//            rec = orec;
//
//            try {
//                orec.start();
//
////                    Log.e("calib","volume "+Constants.vol2Lookup.get(freqs.get(i)));
////                    sp.play(Constants.vol2Lookup.get(freqs.get(i)), 0, -1);
//
//                Thread.sleep(700);
//
//                sp.stopit();
//
//                while (orec.recording) {
//                    Thread.sleep(10);
//                }
//                orec.stopit();
//            } catch (Exception e) {
//                Log.e("ex", "sendtone");
//                Log.e("ex", e.getMessage());
//            }
//
//            long tout = (System.currentTimeMillis()-ts);
////                Log.e("calib","calib time "+freqs.get(i)+","+tout);
//
//            int freq = freqs.get(i);
//            int mag = Constants.volCalibMags.get(i);
//            int dbspl = mag-Constants.volOffset.get(freq);
//
//            int diff=Constants.volTarget.get(freq)-mag;
//            double incs=(diff/3f)*.1;
//
//            float oldvol = Constants.vol1LookupDefaults.get(freq);
//            float newvol = (float)(oldvol);
//            // don't calibrate if the diff is too high, probably an error
//            if (Math.abs(incs) < .3) {
//                newvol = (float) (oldvol + incs);
//            }
//            Constants.vol1Lookup.put(freq, newvol);
//
//            DecimalFormat format = new DecimalFormat(".#");
//            Log.e("calib","calib "+freqs.get(i)+","+mag+","+dbspl+","+format.format(oldvol)+"=>"+format.format(newvol));
//
//            int finalI = i+2;
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ProgressBar pb = view2.findViewById(R.id.progressBar2);
//                    int p = (int)(((double)finalI /freqs.size())*100);
//                    pb.setProgress(p);
//                }
//            });
//        }
//        Log.e("calib","---------------------------------------");
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Constants.dialog.dismiss();
//            }
//        });
//    }

//        public void calibPulse() {
//            short[] pulse = SignalGenerator.sine2speaker(1000, 1000, Constants.samplingRate, Constants.samplen);
//
//            sp.prime(pulse, .1);
//
//            int micType;
//            if (Constants.AGC) {
//                AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
//                if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
//                    micType = (MediaRecorder.AudioSource.UNPROCESSED);
//                } else {
//                    micType = (MediaRecorder.AudioSource.VOICE_RECOGNITION);
//                }
//            }
//            else {
//                micType = MediaRecorder.AudioSource.DEFAULT;
//            }
//
//            // TODO: make sure the recording and speaker overlap in time for sufficiently long
//            OfflineRecorder orec = new OfflineRecorder(micType, -1, -1, 0, barChart, context,
//                    (int)(Constants.RECORDING_WINDOW_IN_SECONDS *Constants.samplingRate), true);
//            rec = orec;
//
//            try {
//                orec.start();
//
//                sp.play(Constants.CALIB_VOLUME,Constants.CALIB_VOLUME, -1);
//
//                Thread.sleep((long)(Constants.PLAY_WINDOW_IN_MILLISECONDS));
//
//                sp.stopit();
//
//                while (orec.recording) {
////                    Log.e("out","waiting");
//                    Thread.sleep(10);
//                }
//                orec.stopit();
//
////                Log.e("out","orec is done");
//            } catch (Exception e) {
//                Log.e("ex","sendtone");
//                Log.e("ex",e.getMessage());
//            }
//        }
}
