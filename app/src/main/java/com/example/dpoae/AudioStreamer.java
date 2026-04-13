package com.example.dpoae;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioStreamer extends Thread {

    AudioTrack track1;
    int SamplingFreq;
    Context mycontext;
    short[] samples;
    int speakerType;
    AudioManager man;

    int[] streams = new int[]{AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ACCESSIBILITY, AudioManager.STREAM_ALARM,
            AudioManager.STREAM_DTMF, AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_VOICE_CALL};

    public AudioStreamer(Context mycontext, short[] samples, int samplen, int samplingFreq, int speakerType, double vol, boolean loop) {
        this.mycontext = mycontext;

        man = (AudioManager)mycontext.getSystemService(Context.AUDIO_SERVICE);
        for (Integer i : streams) {
            man.setStreamMute(i,true);
        }
        man.setStreamMute(speakerType,false);
        man.setStreamVolume(speakerType,(int)(man.getStreamMaxVolume(speakerType)*vol),0);

        SamplingFreq = samplingFreq;
        this.speakerType = speakerType;

        track1 = new AudioTrack(speakerType, SamplingFreq, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, samplen * 2, AudioTrack.MODE_STATIC);

        this.samples = samples;
        int ret = track1.write(samples,0,samples.length);
        if (loop) {
            track1.setLoopPoints(0, samples.length / 2, -1);
        }
    }

//    public void prime(short[] samples, double vol) {
//        this.samples = samples;
//        man.setStreamVolume(speakerType,(int)(man.getStreamMaxVolume(speakerType)*vol),0);
//        track1.write(samples,0,samples.length);
//    }

    public void play(int loops) {
        try {
//            int ret=track1.setLoopPoints(0, samples.length / 2, loops);
//            Log.e("asdf","set loop points "+ret);
//            Log.e("asdf","set loop points "+(samples.length/2)+","+loops);
//            track1.setStereoVolume((float)vol1,(float)vol2);
            track1.setStereoVolume((float) 30, (float) 30);
            track1.play();
        }catch(Exception e) {
            Log.e("err",e.getMessage());
        }
    }

    public void run() {
        track1.setLoopPoints(0,samples.length,-1);
        track1.play();
    }

    public void stopit() {
        track1.flush();
        track1.stop();
    }
}
