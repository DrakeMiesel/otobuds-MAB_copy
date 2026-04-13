package com.example.dpoae;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioSpeaker extends Thread {

    AudioTrack track1;
    int SamplingFreq;
    Context mycontext;
    short[] samples;
    int speakerType;
    AudioManager man;
    boolean stereo;

    int[] streams = new int[]{AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ACCESSIBILITY, AudioManager.STREAM_ALARM,
            AudioManager.STREAM_DTMF, AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_VOICE_CALL};

    public AudioSpeaker(Context mycontext,short[] samples,int samplingFreq, int speakerType) {
        this.mycontext = mycontext;
        man = (AudioManager)mycontext.getSystemService(Context.AUDIO_SERVICE);
        for (Integer i : streams) {
            man.setStreamMute(i,true);
        }
        man.setStreamMute(speakerType,false);
        man.setStreamVolume(speakerType,(int)(man.getStreamMaxVolume(speakerType)*.1),0);

        SamplingFreq = samplingFreq;
        this.samples = samples;
        this.speakerType = speakerType;

        track1 = new AudioTrack(speakerType, SamplingFreq, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, samples.length * 2, AudioTrack.MODE_STATIC);

        track1.write(samples,0,samples.length);
    }

    public void play(double vol, int loops) {
        try {
            track1.setLoopPoints(0, samples.length / 2, loops);
            track1.setVolume((float) vol);
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
        track1.stop();
    }
}
