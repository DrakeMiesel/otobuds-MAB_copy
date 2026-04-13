package com.example.dpoae;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import androidx.fragment.app.Fragment;
import android.provider.Settings.Secure;

public class Constants {
    static boolean DEBUG;
//    static PrepareFragment PrepareFragment;
    static MeasureFragment MeasureFragment;
    static SettingsFragment SettingsFragment;
    static CheckProbeFragment CheckProbeFragment;
    static Fragment CurrentFragment;
    static boolean testInProgress;

    static BottomNavigationView nav;
    static short[][] initbuffer;
    static double[][] sumbuffer;
    static int[] numTries;
    static double[] noise;
    static double[] signal;
    static double[] snrs;
    static double[] signal_f1;
    static double[] signal_f2;
    static double[] noise_f1;
    static double[] noise_f2;
    static double[] snrs_f1;
    static double[] snrs_f2;
    static double[] ambient;
    static boolean[] complete;
    static boolean[] done;
    static ArrayList<BarEntry> graphData;
    static ArrayList<Entry> lineData1;
    static ArrayList<Entry> lineData2;
    static View vv;
    static Activity context;

    static boolean AGC = false;
    static int TONE_F1_MIN_THRESH = 60;
    static int TONE_F2_MIN_THRESH = 50;
    // CALIBRATION
    static int PROBE_CHECK_MIN = 120;
    static int PROBE_CHECK_MAX = 150;
    static int PROBE_CHECK_SAFE_ZONE_START = 130;
    static int PROBE_CHECK_SAFE_ZONE_END = 140;

    // TEST
    static double[] SNR_THRESHS = new double[]{0,0,0,0};
    static double[] SNR_THRESHS_1 = new double[]{6,6,11,10};
    static double[] SNR_THRESHS_2 = new double[]{5,4,4,4};
    static double BAND_PASS_THRESH=0;
    static double BAND_PASS_THRESH_1=3;
    static double BAND_PASS_THRESH_2=3;
    static double SPL_THRESH = -50;
    static int patientYear=0;
    static int patientMonth=0;

    static int MAX_TRIES = 1;
    static int samplingRate = 48000;

    static int samplen = (int)(Constants.samplingRate*1.5);
    static double RECORDING_WINDOW_IN_SECONDS = 1.5;
    static double PLAY_WINDOW_IN_MILLISECONDS = 1500;

//    static int PROCESS_WINDOW_LENGTH = (int)(.65*Constants.samplingRate);
    static int PROCESS_WINDOW_LENGTH = (int)(Constants.samplingRate);

//    static int INIT_SIGNAL_TRIM_LENGTH = (int)(Constants.samplingRate);
    static int INIT_SIGNAL_TRIM_LENGTH = (int)((Constants.samplingRate)*1);

    static int SIGNAL_DETECTION_THRESHOLD = 100;
    static int TEMPLATE_LENGTH = 500;

    static int ProbeCheckTone = 80;

    static boolean EARLY_STOPPING = false;
    static double CALIB_VOLUME = .5;
    static boolean OCTAVES;

    static int CheckFitProgress=0;
    static int SEAL_CHECK_THRESH=130;
    static int SEAL_OCCLUSION_THRESH=500;

    static LinkedList<short[]> fullrec;

    public enum Frequency {
        Daily,Weekly,Monthly
    }
    static String ear="";
    static Frequency reminderFrequency=Frequency.Daily;
    static String site="";
    static String[] sites=new String[]{"","KNH","Mathare","Dandora","Riruta"};
    static HashMap<Integer, Integer> freqLookup=new HashMap<Integer,Integer>();
    static HashMap<Integer, Integer> oaeLookup=new HashMap<Integer,Integer>();
    static HashMap<Integer, Integer> oaeLookup2=new HashMap<Integer,Integer>();
    static HashMap<Integer, Float> vol1LookupDefaults=new HashMap<Integer,Float>();
    static HashMap<Integer, Float> vol1Lookup=new HashMap<Integer,Float>();
    static HashMap<Integer, Float> vol2Lookup=new HashMap<Integer,Float>();

    // Key: Frequency
    // Value: volume for that frequency
    // Typically, there will be 8 frequencies ad their corresponding volume here.
    static HashMap<Integer, Float> vol3Lookup=new HashMap<Integer,Float>();

    static LinkedList<Integer> octaves=new LinkedList<>();
    static LinkedList<Integer> volCalibMags=new LinkedList<>();
    static HashMap<Integer,Integer> volOffset = new HashMap<Integer,Integer>();
    static HashMap<Integer,Integer> volTarget = new HashMap<Integer,Integer>();
    static boolean[] freqs;
    static AlertDialog dialog;
    static AlertDialog measureDialog;
    static boolean CALIBRATE = true;
    static boolean INTERLEAVED = false;
    static int CONSTANT_TONE_LENGTH_IN_SECONDS = 6;
    static double TONE_CALIB_LENGTH_IN_SECONDS = 0.2;
    static double EXAMINE_CALIB_LENGTH_IN_SECONDS = 0.1;
    static double PAD_CALIB_LENGTH_IN_SECONDS = 0.05;
    static int ART_THRESH = 10;
    static int SNR_DIP_THRESH = 1;
//    static int IMD_THRESH=100;
    static int IMD_THRESH=200;
    static long test_timestamp=0;
    static String filename;
    static boolean SPL_CHECK = true;
    static int AMP_THRESH = 500;
    static boolean ONLY_LEFT = false;
    static boolean CHECK_FIT = true;
    static boolean NOISE_CHECK = true;
    static boolean SOUND_VOLUME_CHECK = true;
    static boolean VOL_CALIB=false;
    static String calibSig="tone";
    static double[] chirp;
    static boolean MEASURE_LOADER=true;
    static boolean SHOW_RESULT=true;
    static double totalSeconds=0;
    static double secondCounter=0;
    static String phone="kenyaC";

    // This is only used in calibration, which is not used.
    static double volDefault;
    static double[] threshs=new double[]{68,73,55,55};
    //lab is for paper experiments

//    static int[] f1=new int[]{1640,2438,3282,4078};
//    static int[] f2=new int[]{2016,2953,3985,4969};
//    static int[] oaes=new int[]{1264,1923,2579,3187};
//    static int[] oaes2=new int[]{2392,3468,4688,5860};
    static int[] f1=new int[4];
    static int[] f2=new int[4];
    static int[] oaes=new int[4];
    static int[] oaes2=new int[4];

    static String android_id;
    public static void init(Context context) {

        Constants.android_id = Secure.getString(context.getContentResolver(),
                Secure.ANDROID_ID);

        Log.e("androidid",android_id);

        if (android_id.equals("4440db34e0494a9f")||android_id.equals("5c036c180f521a09")||android_id.equals("a2ee357cb0a51ef7")||android_id.equals("215cc03f47f1cbad")||android_id.equals("86c59a9d0f5cdec9")) {
            phone="kenyaA";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3900;
            f2[3]=4900;
        }
        else if (android_id.equals("3afa33822017a204")||android_id.equals("63854a8fb712ddbb")) {
            phone="kenyaB";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3900;
            f2[3]=4900;
        }
        else if (android_id.equals("5bbe53f75582e923") || android_id.equals("96f19309931bddbd")) {
            phone="kenyaC";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3900;
            f2[3]=5000;
        }
        else if (android_id.equals("085c2a2ff98304f8")) {
            phone="kenyaD";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3800;
            f2[3]=4800;
        }
        else if (android_id.equals("2fcd4276f800f7ca")) {
            // Original phone for benchmarking
            phone="Seattle0";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3800;
            f2[3]=4800;
        }
        else if (android_id.equals("8a3811dd68d8ca9f")) {
            // Phone build 3/24
            phone="Redmond1";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3800;
            f2[3]=4800;
        }
        else if (android_id.equals("938fc3fd45d7215c")) {
            // Phone build 3/25
            phone="Redmond2";
            f2[0]=1900;
            f2[1]=2900;
            f2[2]=3800;
            f2[3]=4800;
        }
        else {
            phone="unknownPhone";
            f2[0]=2016;
            f2[1]=2953;
            f2[2]=3985;
            f2[3]=4969;
        }

        freqs=new boolean[f2.length];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 0; i < freqs.length; i++) {
            freqs[i]=true;
            f2[i] = prefs.getInt("f2Value_" + i, f2[i]);
        }

        for (int i = 0; i < f1.length; i++) {
            f1[i]=(int)(f2[i]/1.22);
        }
        for (int i = 0; i < f1.length; i++) {
            oaes[i]=(int)Math.round(2*f1[i]-f2[i]);
        }
        for (int i = 0; i < f1.length; i++) {
            oaes2[i]=2*f2[i]-f1[i];
        }

        Log.e("asdf","init "+ Build.MODEL);
        Log.e("asdf","PHONE "+phone);

        MeasureFragment = new MeasureFragment();
        SettingsFragment = new SettingsFragment();
        CheckProbeFragment = new CheckProbeFragment();

        chirp=FileOperations.readrawasset(context,R.raw.chirp);

        Constants.MAX_TRIES=prefs.getInt("maxtries",Constants.MAX_TRIES);
        Constants.OCTAVES=prefs.getBoolean("octaves",Constants.OCTAVES);
        Constants.EARLY_STOPPING=prefs.getBoolean("earlystop",Constants.EARLY_STOPPING);
        Constants.CALIBRATE=prefs.getBoolean("calibrate",Constants.CALIBRATE);
        Constants.INTERLEAVED =prefs.getBoolean("adaptive",Constants.INTERLEAVED);
        Constants.SPL_CHECK =prefs.getBoolean("spl",Constants.SPL_CHECK);
        Constants.CONSTANT_TONE_LENGTH_IN_SECONDS=prefs.getInt("constantToneLength",Constants.CONSTANT_TONE_LENGTH_IN_SECONDS);
//      Constants.SEAL_CHECK_THRESH =prefs.getInt("checkFitThresh",Constants.SEAL_CHECK_THRESH);
        Constants.CHECK_FIT =prefs.getBoolean("checkFit",Constants.CHECK_FIT);
        Constants.ONLY_LEFT =prefs.getBoolean("onlyLeft",Constants.ONLY_LEFT);
        Constants.NOISE_CHECK =prefs.getBoolean("noiseCheck",Constants.NOISE_CHECK);
        Constants.SOUND_VOLUME_CHECK =prefs.getBoolean("soundVolCheck",Constants.SOUND_VOLUME_CHECK);
        Constants.VOL_CALIB =prefs.getBoolean("volCalib",Constants.VOL_CALIB);
        Constants.MEASURE_LOADER =prefs.getBoolean("measureLoader",Constants.MEASURE_LOADER);
        Constants.SHOW_RESULT =prefs.getBoolean("showResult",Constants.SHOW_RESULT);
        Constants.reminderFrequency=Frequency.values()[prefs.getInt("reminderFrequency",Constants.reminderFrequency.ordinal())];

        for (int i = 0; i < freqs.length; i++) {
            freqs[i] = prefs.getBoolean("check"+i, freqs[i]);
        }

        if (octaves.size() == 0) {
            for(int i = 0; i < f1.length; i++){
                freqLookup.put(f2[i],f1[i]);
            }

            for(int i = 0; i < f1.length; i++){
                oaeLookup.put(f2[i],oaes[i]);
            }

            for(int i = 0; i < f1.length; i++){
                oaeLookup2.put(f2[i],oaes2[i]);
            }

            for (Integer i : f2) {
                Log.e("justin","octaves add "+i);
                octaves.add(i);
            }

            /////////////////////////////////////////??
            populateVolume(context);
        }
    }

    public static void populateVolume(Context context) {
        float[]vols=new float[f1.length];
        if (phone.equals("kenyaA")) {
            vols[0]=.5f;
            vols[1]=.5f;
            vols[2]=.5f;
            vols[3]=.6f;
            SEAL_CHECK_THRESH=130;
        }
        else if (phone.equals("kenyaB")) {
            vols[0]=.5f;
            vols[1]=.5f;
            vols[2]=.5f;
            vols[3]=.6f;
            SEAL_CHECK_THRESH=130;
        }
        else if (phone.equals("kenyaC")) {
            vols[0]=.5f;
            vols[1]=.5f;
            vols[2]=.6f;
            vols[3]=.6f;
            SEAL_CHECK_THRESH=130;
        }
        else if (phone.equals("kenyaD")) {
            vols[0]=.6f;
            vols[1]=.4f;
            vols[2]=.6f;
            vols[3]=.6f;
            SEAL_CHECK_THRESH=130;
        }
        else if (phone.equals("Redmond1")) {
            vols[0]=.2f;
            vols[1]=.2f;
            vols[2]=.2f;
            vols[3]=.2f;
            SEAL_CHECK_THRESH=130;
        }
        else if (phone.equals("Redmond2")) {
            vols[0]=.3f;
            vols[1]=.3f;
            vols[2]=.3f;
            vols[3]=.3f;
            SEAL_CHECK_THRESH=130;
        }
        else {
            vols[0]=.5f;
            vols[1]=.5f;
            vols[2]=.5f;
            vols[3]=.6f;
            SEAL_CHECK_THRESH=130;
        }

        ///////////////////////////////////
        float[]vol1=new float[f1.length];
        float[]vol2=new float[f1.length];
        if (phone.equals("kenyaA")) {
            vol1[0]=1f;
            vol2[0]=.2f;

            vol1[1]=1f;
            vol2[1]=.9f;

            vol1[2]=1f;
            vol2[2]=1f;

            vol1[3]=1f;
            vol2[3]=1f;
        }
        else if (phone.equals("kenyaB")) {
            vol1[0]=1f;
            vol2[0]=.2f;

            vol1[1]=1f;
            vol2[1]=.9f;

            vol1[2]=1f;
            vol2[2]=1f;

            vol1[3]=1f;
            vol2[3]=1f;
        }
        else if (phone.equals("kenyaC")) {
            vol1[0]=1f;
            vol2[0]=.3f;

            vol1[1]=1f;
            vol2[1]=1f;

            vol1[2]=1f;
            vol2[2]=1f;

            vol1[3]=1f;
            vol2[3]=1f;
        }
        else if (phone.equals("kenyaD")) {
            vol1[0]=1f;
            vol2[0]=.15f;

            vol1[1]=1f;
            vol2[1]=1f;

            vol1[2]=1f;
            vol2[2]=1f;

            vol1[3]=1f;
            vol2[3]=1f;
        }
        else {
            vol1[0]=1f;
            vol2[0]=.2f;

            vol1[1]=1f;
            vol2[1]=.9f;

            vol1[2]=1f;
            vol2[2]=1f;

            vol1[3]=1f;
            vol2[3]=1f;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 0; i < f1.length; i++) {
            // Read adjusted values from SharedPreferences if they exist.
            // Use defaults when they do do not exist.
            float volV = prefs.getFloat("volumeValue_"+i, vols[i]);
            vol1LookupDefaults.put(f2[i], volV);

            float f1v = prefs.getFloat("volumeF1te_"+i, vol1[i]);
            float f2v = prefs.getFloat("volumeF2te_"+i, vol2[i]);
            vol3Lookup.put(f1[i],f1v);
            vol3Lookup.put(f2[i],f2v);
        }

        /////////////////////////////////////////
        // Just vol1Lookup.put(f2[i], ...) should be enough.
        // $TODO: remove below and all vol1LookupDefaults from code.
        for (Integer i : octaves) {
            float val = vol1LookupDefaults.get(i);
            vol1LookupDefaults.put(freqLookup.get(i), val);
        }

        for(Integer i : vol1LookupDefaults.keySet()) {
            float val = vol1LookupDefaults.get(i);
            vol1Lookup.put(i,val);
        }
    }
}
