package com.example.dpoae;

import static com.example.dpoae.Constants.CONSTANT_TONE_LENGTH_IN_SECONDS;
import static com.example.dpoae.Constants.populateVolume;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MeasureFragment extends Fragment {

    Button leftButton,rightButton;
    Button cancelButton;
    ExtendedBarChart barChart;
    LineChart lineChart;
    static TextView result;
    static TextView fnameView,noiseView,probeSoundLowView;
    static TextView yearText,monthText;
    View view;
    View view2;
    View view3;
    MaterialAutoCompleteTextView siteSpinner;
    TextInputEditText et1,et2;
    Random random = new Random(1);
    AudioStreamer gsp;
    static boolean noisyDisplayed=false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.measure, container, false);
        ((MainActivity) getActivity()).getSupportActionBar().hide();
        barChart = view.findViewById(R.id.barchart);
        lineChart = view.findViewById(R.id.linechart);
        Log.e("justin","start measure");
//        Grapher.graphHelper(barChart, getActivity(), dummyData());
//        Grapher.graphHelper2(getActivity(), lineChart, dummyLineData2(), dummyLineData());

        fnameView = view.findViewById(R.id.fnameView);
        noiseView = view.findViewById(R.id.noiseView);
        yearText=view.findViewById(R.id.yearText);
        monthText=view.findViewById(R.id.monthText);
        probeSoundLowView = view.findViewById(R.id.probeSoundLowView);
        result = view.findViewById(R.id.result);
        et1 = view.findViewById(R.id.patientID);
        et2 = view.findViewById(R.id.userID);

        siteSpinner = view.findViewById(R.id.siteSpinner);
        List<String> spinnerArray =  new ArrayList<String>();
        for (String s:Constants.sites) {
            spinnerArray.add(s);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        siteSpinner.setAdapter(adapter);
        siteSpinner.setText(Constants.sites[0], false);

        siteSpinner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Constants.site=siteSpinner.getText().toString();
                Log.e("site",Constants.site);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        leftButton = view.findViewById(R.id.leftButton);
        rightButton = view.findViewById(R.id.rightButton);

        leftButton.setEnabled(true);
        rightButton.setEnabled(true);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Constants.ear="left";
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et1.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(et2.getWindowToken(), 0);

                ((Activity)getContext()).getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                measure("left");
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Constants.ear="right";
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et1.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(et2.getWindowToken(), 0);

                ((Activity)getContext()).getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                measure("right");
            }
        });

        cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setEnabled(false);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Constants.ear="";
                measure.cancel(true);
                if (gsp==null) {
                    Log.e("nullref", "gsp");
                }

                try {
                    gsp.stopit();
                    gsp.join();
                } catch (InterruptedException e) {
                    Log.e("ex",e.getMessage());
                }

                if(rec==null) {
                    Log.e("nullref", "rec");
                }

                try {
                    rec.stopit();
                    rec.join();
                } catch (InterruptedException e) {
                    Log.e("ex",e.getMessage());
                }
                enableBottomBar(true);
                leftButton.setEnabled(true);
                rightButton.setEnabled(true);
                cancelButton.setEnabled(false);
                FileOperations.writeRecToDisk(getActivity(), fnameView);

                TextInputEditText et=(TextInputEditText)getActivity().findViewById(R.id.patientID);
                TextInputEditText et2 = (TextInputEditText)getActivity().findViewById(R.id.userID);
                String pid = et.getText().toString().length() == 0 ? "0" : et.getText().toString();
                String uid = et2.getText().toString();
                String attemptNumber = FileOperations.getAttemptNumber(getActivity(),pid,uid,Constants.ear,Constants.test_timestamp);
                String noiseVal=checkNoiseThresholds(getActivity());
                boolean result2=checkStatus(getActivity());
                FileOperations.writeCSV(getActivity(),pid,uid,Constants.ear,attemptNumber,Constants.signal,Constants.noise,
                        Constants.snrs,noiseVal,Constants.ambient,result2,Constants.CONSTANT_TONE_LENGTH_IN_SECONDS,false);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

        EditText pidVal = view.findViewById(R.id.patientID);
        int pid = prefs.getInt("pid",0);
        pidVal.setText(pid+"");

        pidVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                String ss = pidVal.getText().toString();
                if (ss.length()>0) {
                    editor.putInt("pid", Integer.parseInt(ss));
                    editor.commit();
                }
            }
        });

        pidVal.setSelectAllOnFocus(true);

        ImageView upButton = (ImageView)view.findViewById(R.id.upButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ss = pidVal.getText().toString();
                if (ss.length() == 0) {
//                    pidVal.setText(prefs.getInt("pid",0)+"");
                }
                else {
                    int pid = Integer.parseInt(ss);
                    pidVal.setText((pid + 1) + "");
                    editor.putInt("pid", (pid + 1));
                    editor.commit();
                }
            }
        });
        ImageView downButton = (ImageView)view.findViewById(R.id.downButton);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ss = pidVal.getText().toString();
                int pid = Integer.parseInt(ss);
                if (ss.length() == 0) {
//                    pidVal.setText(prefs.getInt("pid",0)+"");
                }
                else {
                    if (pid > 0) {
                        pidVal.setText((pid - 1) + "");
                        editor.putInt("pid",(pid-1));
                        editor.commit();
                    }
                }
            }
        });

        pidVal.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    //Clear focus here from edittext
                    pidVal.clearFocus();
                }
                return false;
            }
        });

        int n = Constants.octaves.size();
        Constants.initbuffer = new short[n][Constants.PROCESS_WINDOW_LENGTH];
        Constants.sumbuffer = new double[n][Constants.PROCESS_WINDOW_LENGTH];
        Constants.complete=new boolean[n];
        Constants.numTries = new int[n];
        Constants.signal = new double[n];
        Constants.noise = new double[n];
        Constants.snrs = new double[n];
        Constants.ambient = new double[n];
        Constants.signal_f1 = new double[n];
        Constants.signal_f2 = new double[n];
        Constants.noise_f1 = new double[n];
        Constants.noise_f2 = new double[n];
        Constants.snrs_f1 = new double[n];
        Constants.snrs_f2 = new double[n];

        Constants.graphData = new ArrayList<>();
        Constants.lineData1 = new ArrayList<>();
        Constants.lineData2 = new ArrayList<>();
        Log.e("justin","adding ");
        float[] vals=new float[]{0,0,0,0,30,22,15,27,15,17,25,15,28,27,19,24,28,22,26,24,30,30,28,26,22,30,30,28,26,22};
        for (int i = 0; i < n; i++) {
            Log.e("justin","add "+i+","+Constants.octaves.get(i)/1000f+","+vals[i]);
            Constants.graphData.add(i, new BarEntry(Constants.octaves.get(i)/1000f, new float[]{(float) vals[i]}));
        }
        Grapher.graphHelper(barChart, getActivity(), Constants.graphData);

        boolean showOld=true;
        String[] fnames=new String[]{
//                "2024-01-29 13:29:17-0-Name-left-3-kenyaB",
//                "2024-01-29 13:32:04-0-Name-left-6-kenyaB",
//                "2024-01-29 13:35:22-0-Name-left-7-kenyaB",
//                "2024-01-29 13:39:00-0-Name-right-2-kenyaB",
//                "2024-01-29 13:39:58-0-Name-right-3-kenyaB",
//                "2024-01-29 13:41:40-0-Name-right-4-kenyaB",
//                "2024-01-29 13:52:48-0-Name-right-6-kenyaB",
//
//                "2024-01-29 13:36:29-0-Name-left-8-kenyaB",
//                "2024-01-29 13:54:11-0-Name-right-9-kenyaB",

//                "2024-01-30 13:13:10-1-Name-right-10-kenyaA",
//                "2024-01-30 13:20:05-1-Name-right-15-kenyaA",
//                "2024-01-30 13:06:23-1-Name-right-4-kenyaA"

//                "2024-01-29 09:55:53-0-Name-left-1-kenyaA",
//                "2024-01-29 09:58:28-0-Name-left-2-kenyaA"
//
//                "2024-01-30 12:44:21-1-Name-right-1-kenyaA",
//                "2024-01-30 12:45:09-1-Name-right-2-kenyaA",
//                "2024-01-30 12:46:24-1-Name-left-1-kenyaA",
//                "2024-01-30 12:47:04-1-Name-left-2-kenyaA",
//                "2024-01-30 12:53:34-1-Name-left-3-kenyaA",
//                "2024-01-30 12:54:20-1-Name-left-4-kenyaA",
//                "2024-01-30 12:55:31-1-Name-left-5-kenyaA",
//                "2024-01-30 12:56:15-1-Name-left-6-kenyaA",
//                "2024-01-30 12:57:52-1-Name-left-7-kenyaA",
//
//                "2024-01-30 13:04:49-1-Name-left-8-kenyaA",
//                "2024-01-30 13:05:32-1-Name-right-3-kenyaA",
//                "2024-01-30 13:06:23-1-Name-right-4-kenyaA",
//                "2024-01-30 13:08:02-1-Name-right-5-kenyaA",
//                "2024-01-30 13:08:44-1-Name-right-6-kenyaA",
//                "2024-01-30 13:09:08-1-Name-right-7-kenyaA",
//                "2024-01-30 13:10:19-1-Name-right-8-kenyaA",
//                "2024-01-30 13:11:21-1-Name-right-9-kenyaA",

//                "2024-01-30 13:13:10-1-Name-right-10-kenyaA",
//                "2024-01-30 13:15:14-1-Name-right-11-kenyaA",
//                "2024-01-30 13:16:33-1-Name-right-12-kenyaA",
//                "2024-01-30 13:18:09-1-Name-right-13-kenyaA",
//                "2024-01-30 13:18:55-1-Name-right-14-kenyaA",
//                "2024-01-30 13:20:05-1-Name-right-15-kenyaA",

//                "2024-07-24 084319-0-Name-right-1-kenyaA", // ERROR 2khz too noisy. tones too soft 2,3khz.
//                "2024-07-24 090749-0-Name-right-2-kenyaA", // ERROR 5khz too noisy. tones too soft 5khz.
//                "2024-07-24 111023-0-Name-left-1-kenyaA", // ERROR 3khz too noisy. tones being played are too soft 3khz.
//                "2024-07-24 111232-0-Name-right-1-kenyaA",
//                "2024-07-24 111527-0-Name-left-2-kenyaA",
//                "2024-07-24 111857-0-Name-left-3-kenyaA",
//                "2024-07-24 111954-0-Name-right-2-kenyaA",
//                "2024-07-24 112921-0-Name-left-4-kenyaA",
//                "2024-07-24 113015-0-Name-right-3-kenyaA",

//                "2024-07-25 081014-0-Name-right-1-kenyaA", // NAN
//                "2024-07-25 081101-0-Name-left-1-kenyaA", // ERROR 3khz too noisy
//                "2024-07-25 085934-1-Name-right-1-kenyaA",
//                "2024-07-25 090144-1-Name-left-1-kenyaA", // ERROR 2khz too soft
//                "2024-07-25 090645-2-Name-left-1-kenyaA",
//                "2024-07-25 090814-2-Name-right-1-kenyaA", // ERROR 2khz too soft
//                "2024-07-26 111901-2-Name-right-1-kenyaA",
//                "2024-07-26 114914-0001-EWG-left-1-kenyaA", // ERROR 4khz too soft.

//                "2024-07-26 11_50_47-0-Name-right-1-kenyaB",
//                "2024-07-26 11_51_46-0001-EWG-left-1-kenyaB", // ERROR 2khz too soft
//                "2024-07-26 11_53_18-0001-EWG-right-1-kenyaB",

//                "2024-07-29 11_50_58-0003-EWG-left-1-kenyaB",
//                "2024-07-29 11_52_19-0003-EWG-left-2-kenyaB",
//                "2024-07-29 12_15_31-0003-EWG-right-1-kenyaB",
//                "2024-07-29 12_16_12-0003-EWG-right-2-kenyaB",
//                "2024-07-31 11_01_02-0005-EWG-right-1-kenyaB",
//                "2024-07-31 11_01_36-0005-EWG-right-2-kenyaB",
//                "2024-07-31 11_02_16-0005-EWG-right-3-kenyaB",
//                "2024-07-31 11_15_49-0005-EWG-right-4-kenyaB",
//                "2024-07-31 11_18_21-0005-EWG-right-1-kenyaA",
//                "2024-07-31 11_18_58-0005-EWG-right-2-kenyaA",
//                "2024-07-31 11_19_37-0005-EWG-right-3-kenyaA",
//                "2024-07-31 11_20_44-0005-EWG-left-1-kenyaA",
//                "2024-07-31 11_21_22-0005-EWG-left-2-kenyaA",
//                "2024-07-31 11_37_56-0006-EWG-left-1-kenyaA",
//                "2024-07-31 11_43_51-0006-EWG-right-1-kenyaA",
//                "2024-07-31 11_44_32-0006-EWG-right-2-kenyaA",
//                "2024-07-31 11_45_02-0006-EWG-right-3-kenyaA",
//                "2024-07-31 11_45_33-0006-EWG-right-4-kenyaA"

//                "2024-08-02 10_06_13-0008-EWG-left-1-kenyaB", //24,33,15,12 pass
//                "2024-08-02 10_08_34-0008-EWG-right-1-kenyaB", //2,5 soft, 7,4,8,14 refer
//                "2024-08-02 10_15_10-0008-EWG-right-2-kenyaB", //21,10,27,29 pass
//                "2024-08-02 10_29_27-0009-EWG-left-1-kenyaB", //0,0,0,0
//                "2024-08-02 10_32_01-0009-EWG-left-1-kenyaB", //3,8,4,4 refer
//                "2024-08-02 10_32_58-0009-EWG-left-2-kenyaB", //3,5 soft 22,11,9,23 pass
//                "2024-08-02 10_35_08-0009-EWG-right-1-kenyaB", //32,39,6,20 pass
//                "2024-08-02 10_54_16-0010-EWG-right-1-kenyaB", //5 soft nan,2,5,2 refer
//                "2024-08-02 10_54_50-0010-EWG-right-2-kenyaB", //2,4,2,1, refer
//                "2024-08-02 10_55_40-0010-EWG-right-3-kenyaB", //nan,8,1,6 refer
//                "2024-08-02 10_56_56-0010-EWG-right-4-kenyaB", //1,3,nan,3 refer
//                "2024-08-02 10_59_25-0010-EWG-left-1-kenyaB", // 5 soft 2,5,2,3 refer
//                "2024-08-02 11_00_20-0010-EWG-left-2-kenyaB" // 4,8,1,2 refer
        };
//        String[] fnames = new String[]{"0-left-1655605472059","0-left-1655605490797","0-left-1655605508473","0-left-1655606023171","0-left-1655606041669","0-left-1655606099161","0-left-1655605781684","0-left-1655605975773","0-left-1655605996011","0-left-1655605622874","0-left-1655605643500","0-left-1655605662817","0-left-1655605707583","0-left-1655605726860","0-left-1655605745289"};
//        String[] fnames = new String[]{"1-right-1643308698736","1-left-1643309070293","2-right-1643316965081","2-left-1643317149293","4-right-combined","5-right-1643329220820","5-left-1643328985635","11-right-1646853068138","11-left-1646852965735","13-right-1646931100765","13-left-1646931184462","15-right-1646940140158","15-left-1646940194912","17-right-1646950623532","17-left-1646950478308","13-right-1647475571658","13-left-1647475672044","14-right-1647622320739","14-left-1647622177654","15-right-1647628160109","16-right-1647629951500","16-left-1647629814049","17-left-1647636143349","18-left-1647636752179","26-right-1648493058494","26-left-1648493138582","27-right-1648501871192","27-left-1648501953220","28-right-1648574358019","31-right-1648670376087","31-left-1648670195221","33-right-1648681857274","33-left-1648681777607","33-right-combined","33-left-1649094533489","34-right-1648853089083","34-left-1648853012630","35-right-1648853821817","36-right-combined","36-left-1649096280646","39-right-1649273816213","39-left-1649274756487","40-left-1649282286421","41-right-1649438242350","42-right-1649439582718","42-left-1649439497367","44-left-1649457751513","45-left-1649458864950","46-right-1649872930642","46-left-1649872856338","47-left-1649885268375","48-right-1649890234487","48-left-1649890188044","50-right-1649968438591","50-left-1649968361363","51-right-1649970330084","52-left-1649972929997","54-right-1650041814448","54-left-1650041877737","55-right-1650050214396","55-left-1650049897963","56-left-1650058000752","57-right-1650304046651","57-left-1650303944012","58-right-1650308297236","58-left-1650308383116","32-left-1648673829539","37-right-1649266235074","37-left-1649266073252","38-right-1649267611177","38-left-1649267446217","62-right-1650905273149","62-left-1650905106477","63-left-1650906609619","64-left-1650911568553","65-right-1651009908843","65-left-1651010280162","66-right-1651098161199","66-left-1651098236382","67-right-1651170169914","67-left-1651169572708","68-right-combined","68-left-1651183321804","69-right-1651252417326","69-left-1651252096985","70-right-1651526140231","73-right-1651694679544","73-left-1651694950115","101-right-combined","101-left-1648578860737","102-right-combined","102-left-1648598277517","103-right-1649095119478","103-left-1649095063272","104-right-1649203179545","104-left-1649203118066","105-right-1649286250165","105-left-1649285992935","106-right-1649788298877","106-left-1649788257709","107-right-1649804969072","107-left-1649805173880","108-left-1649892251571","109-left-1650493920385","110-right-1651517846241","110-left-1651517769282"};
//        String[]fnames=new String[]{"74-right-1651703222780","74-right-1651703155678","74-left-1651703442144","74-left-1651703485097","76-right-1651878879554","76-left-1651878958867","77-right-1652119279589","77-left-1652118964671"};
//        for (String fname : fnames) {
//            String filename = fname+"-volcalib.txt";
//            String dir = getActivity().getExternalFilesDir(null).toString();
//            File file = new File(dir + File.separator + filename);
//            if(file.exists()) {
//                short[] ss2 = FileOperations.readfromfile_short(getActivity(), filename);
//                String out = Utils.volcalib(ss2);
//                Log.e("asdf",filename+" "+out);
//            }
//            else {
//                Log.e("asdf",filename);
//            }
//        }

        if (showOld) {
            int timeLimit=6;
            Constants.CONSTANT_TONE_LENGTH_IN_SECONDS=timeLimit;
            for (String fname:fnames) {
                computeNoiseThreshold();
                short[] ss = FileOperations.readfromfile_short(getActivity(), fname+".txt");
//                Log.e("final","reading "+ss.length);
                if (ss.length > 0) {
                    int numfreqs = 4;
                    int defaultCutLen=(int)(ss.length/48e3/numfreqs);
                    int defaultSegLen = (int)(ss.length/48e3/numfreqs);
                    int beginCutLen=0;
                    int endCutLen = Constants.samplingRate * defaultCutLen;
                    int segLen = Constants.samplingRate * defaultSegLen;

                    String out1 = "";
                    String out2 = "";
                    int passcounter = 0;
                    double[] snrs = new double[]{0, 0, 0, 0};
                    double[] signal = new double[]{0, 0, 0, 0};
                    double[] noise = new double[]{0, 0, 0, 0};
                    double[] ambient = new double[]{0, 0, 0, 0};
                    Constants.done=new boolean[numfreqs];
                    Constants.complete=new boolean[numfreqs];
                    for (int i = 0; i < numfreqs; i++) {
                        if (i == 0 || i == 1 || i==2 || i==3) {
//                        if (i == 3) {
                            int cc = segLen*i+beginCutLen;
                            int ee = segLen*i+endCutLen;
                            int cut = (int)(48e3*(6-timeLimit));
                            short[] samples = Arrays.copyOfRange(ss, cc, ee-cut);
                            int freq2 = Constants.f2[i];
                            int freq1 = Constants.f1[i];
                            double[] ret = Signal.work(getActivity(), samples, (int) freq1, (int) freq2, i);
                            snrs[i] = ret[0]-ret[1];
                            signal[i] = ret[0];
                            noise[i] = ret[1];
                            ambient[i] = ret[2];
                        }
                    }
                    boolean status=checkStatus(getActivity());
                    Log.e("final",fname);
                    warningCheck();
//                    Log.e("final",((int)Math.ceil(noise[0]))+","+((int)Math.ceil(noise[1]))+","+((int)Math.ceil(noise[2]))+","+((int)Math.ceil(noise[3]))+","+status);
                    String noisy=checkNoiseThresholds(getActivity());
                    Grapher.graph(barChart, lineChart, getActivity(), Constants.graphData, Constants.lineData1, Constants.lineData2, false);
                    Log.e("final",noisy);
//                    boolean result = checkStatus(getActivity());
                    String warningf1=warning_f1();
                    String warningf2=warning_f2();

                    Log.e("final",((int)Math.ceil(snrs[0]))+","+((int)Math.ceil(snrs[1]))+","+((int)Math.ceil(snrs[2]))+","+((int)Math.ceil(snrs[3]))+","+status);
                    Log.e("final",((int)Math.ceil(Constants.snrs_f1[0]))+","+((int)Math.ceil(Constants.snrs_f1[1]))+","+((int)Math.ceil(Constants.snrs_f1[2]))+","+((int)Math.ceil(Constants.snrs_f1[3]))+","+warningf1);
                    Log.e("final",((int)Math.ceil(Constants.snrs_f2[0]))+","+((int)Math.ceil(Constants.snrs_f2[1]))+","+((int)Math.ceil(Constants.snrs_f2[2]))+","+((int)Math.ceil(Constants.snrs_f2[3]))+","+warningf2);
                }
            }
        }
        else {
            int numfreqs = 4;
            for (int i = 0; i < numfreqs; i++) {
                Log.e("justin","hello "+i+","+Constants.freqs[i]);
                if (Constants.freqs[i]) {
                    Constants.complete[i]=true;
                    float freq = Constants.octaves.get(i);
                    Log.e("justin","hello "+i+","+freq / 1000f+","+vals[i]);
                    Constants.graphData.set(i, new BarEntry(freq / 1000f, new float[]{(float) vals[i]}));
                    float xx = (float) roundToHalf(Constants.octaves.get(i) / 1000.0);
                    Constants.lineData1.add(new Entry(xx, (float) 0));
                    Constants.lineData2.add(new Entry(xx, (float) 0));
                }
            }
            Grapher.graph(barChart, lineChart, getActivity(), Constants.graphData, Constants.lineData1, Constants.lineData2, false);

//            boolean result2=checkStatus(getActivity());
        }

        return view;
    }

    public static void computeNoiseThreshold() {
        if (Constants.phone.equals("kenyaA")) {
            Constants.threshs=new double[]{80,80,80,80};
        }
        else if (Constants.phone.equals("kenyaB")) {
            Constants.threshs=new double[]{80,80,80,80};
        }
        else if (Constants.phone.equals("kenyaC")) {
            Constants.threshs=new double[]{80,80,70,80};
        }
        else if (Constants.phone.equals("kenyaD")) {
            Constants.threshs=new double[]{80,80,80,80};
        }
        else {
            Constants.threshs=new double[]{80,80,80,80};
        }

        String outthresh="";
        for(int i = 0; i < Constants.threshs.length; i++) {
            double decrement = 10*Math.log10(Constants.CONSTANT_TONE_LENGTH_IN_SECONDS-1);
            Constants.threshs[i]=(Constants.threshs[i]-decrement)+25;
            outthresh+=(int)(Constants.threshs[i])+",";
        }
        Log.e("thresh",Constants.CONSTANT_TONE_LENGTH_IN_SECONDS+"");
        Log.e("thresh","oae "+outthresh);
    }

    public static String checkNoiseThresholds(Activity a) {
        computeNoiseThreshold();
        String out="";
        int ncounter=0;
        String humanout="";
        for (int i = 0; i < Constants.threshs.length; i++) {
            if (Constants.noise[i] >= Constants.threshs[i] && !Constants.complete[i]) {
                out+="noisy, ";
                humanout+=(int)(Math.ceil(Constants.octaves.get(i)/1000)+1)+"kHz, ";
                ncounter+=1;
            }
            else {
                out+="ok, ";
            }
        }
        if (ncounter >= 1) {
            out += "retry";
        }
        else {
            out += "noise-pass";
        }
        humanout=humanout.trim();
        if (humanout.length()>3) {
            humanout = humanout.substring(0, humanout.length() - 1);
        }
        if (out.contains("retry") && Constants.NOISE_CHECK) {
            noisyDisplayed=true;
            String finalHumanout = humanout;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(a)
                            .setMessage("Warning\n\nToo noisy ("+ finalHumanout +"). Consider retrying.")
                            .setPositiveButton("Ok", null)
                            .setCancelable(false).create();
                    dialog.show();
                    TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                    textView.setTextSize(20);
                    noiseView.setText("Too noisy ("+ finalHumanout +")");
                }
            });
        }
        return out;
    }

    public static boolean checkStatus(Activity a) {
        Log.e("justin","check status");
        int ncounter=0;
        for (int i = 0; i < Constants.complete.length; i++) {
//            Log.e("justin","loop "+Constants.complete[i]);
            if (Constants.complete[i]) {
                ncounter++;
            }
        }

//        Log.e("justin","ncounter "+ncounter);

//        if (Constants.phone.contains("kenya")) {
            int finalNcounter = ncounter;
            Log.e("justin","finalNcounter "+finalNcounter);
//            a.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//            if (Constants.SHOW_RESULT) {
                if (finalNcounter >= Constants.BAND_PASS_THRESH) {
                    result.setText("Pass");
                    return true;
                } else {
                    result.setText("Refer");
                    return false;
                }
//            }
//                }
//            });
//        }
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    public ArrayList<BarEntry> dummyData() {
        ArrayList<BarEntry> data = new ArrayList<>();
        float[] dat=new float[]{5,5,5,5};
        for (int i = 0; i < Constants.octaves.size(); i++) {
            float xx= Constants.octaves.get(i)/1000f;
            data.add(new BarEntry(xx,new float[]{dat[i]}));
        }
        return data;
    }
//
//    public ArrayList<Entry> dummyLineData() {
//        ArrayList<Entry> data = new ArrayList<>();
//
//        data.add(new Entry(1.000f, 9.7f));
//        data.add(new Entry(1.500f, 12.3f));
//        data.add(new Entry(2.000f, 16.2f));
//        data.add(new Entry(3.000f, 15.2f));
//        data.add(new Entry(4.000f, 12.6f));
//        data.add(new Entry(5.000f, 15f));
//        data.add(new Entry(6.000f, 8.2f));
//        data.add(new Entry(8.000f, 10.8f));
//        return data;
//    }
//
//    public ArrayList<Entry> dummyLineData2() {
//        ArrayList<Entry> data = new ArrayList<>();
//        data.add(new Entry(1.000f, 1.3f));
//        data.add(new Entry(1.500f, 1.6f));
//        data.add(new Entry(2.000f, -12.8f));
//        data.add(new Entry(3.000f, -8.6f));
//        data.add(new Entry(4.000f, -11.2f));
//        data.add(new Entry(5.000f, -11f));
//        data.add(new Entry(6.000f, -19.3f));
//        data.add(new Entry(8.000f, -18.4f));
//        return data;
//    }

    public double[] convert2(short[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = sig[i];
        }
        return out;
    }

    AsyncTask<Integer,Void,Void> measure;
    OfflineRecorder rec;
    public void measure(String ear) {
        int year = Integer.parseInt(yearText.getText().toString());
        int month = Integer.parseInt(monthText.getText().toString());
        Constants.patientYear=year;
        Constants.patientMonth=month;
        int total_months=year*12+month;
        if (total_months<=6) {
            Constants.SNR_THRESHS=Constants.SNR_THRESHS_2;
            Constants.BAND_PASS_THRESH=Constants.BAND_PASS_THRESH_2;
        }
        else {
            Constants.SNR_THRESHS=Constants.SNR_THRESHS_1;
            Constants.BAND_PASS_THRESH=Constants.BAND_PASS_THRESH_1;
        }

        try {
            measure = new SendSignal(getActivity(), barChart, lineChart, leftButton, rightButton, cancelButton, ear).execute();
        }
        catch(Exception e) {
            Log.e("ex","measure");
            Log.e("ex",e.getMessage());
        }
    }

    private void enableBottomBar(boolean enable){
        for (int i = 0; i < Constants.nav.getMenu().size(); i++) {
            Constants.nav.getMenu().getItem(i).setEnabled(enable);
        }
    }

    public void calibration_reminder() {
        File file = new File(getActivity().getExternalFilesDir(null).toString() + File.separator + "last_calibration.txt");
        if (file.exists()) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader(file));
                long timestamp=Long.parseLong(buf.readLine());
                long freq = 0;
                if (Constants.reminderFrequency == Constants.Frequency.Daily) {
                    freq=24 * 60 * 60 * 1000;
                }
                else if (Constants.reminderFrequency == Constants.Frequency.Weekly) {
                    freq=7 * 24 * 60 * 60 * 1000;
                }
                else if (Constants.reminderFrequency == Constants.Frequency.Monthly) {
                    freq=(long) (30.44 * 24 * 60 * 60 * 1000);
                }
                if (System.currentTimeMillis() - timestamp >= freq) {
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(file, false));
                    outfile.append(System.currentTimeMillis() + "\n");
                    outfile.close();
                    calibration_popup();
                }
            }
            catch(Exception e) {
                Log.e("asdf",e.getMessage());
            }
        }
        else {
            try {
                file.createNewFile();
                BufferedWriter outfile = new BufferedWriter(new FileWriter(file, false));
                outfile.append(System.currentTimeMillis() + "\n");
                outfile.close();
                calibration_popup();
            }
            catch(Exception e) {
                Log.e("asdf",e.getMessage());
            }
        }
    }

    public void calibration_popup() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
                        .setMessage("Probe check reminder\n\nThis is a regular reminder to check the integrity of the probe.\n\n" +
                                "1) Perform a measurement in open air, outside the ear and verify that the test fails.\n\n" +
                                "2) Perform a measurement in a healthy ear and verify that the test passes.")
                        .setPositiveButton("Ok", null)
                        .setCancelable(false).create();
                dialog.show();
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(20);
            }
        });
    }

    public String warning_f1() {
        String freqs="";
        for (int i = 0; i < Constants.snrs_f1.length; i++) {
            if (Constants.freqs[i]) {
                if (Constants.snrs_f1[i] < Constants.TONE_F1_MIN_THRESH) {
                    if (i==0) {
                        freqs += "2kHz, ";
                    }
                    else if (i==1) {
                        freqs += "3kHz, ";
                    }
                    else if (i==2) {
                        freqs += "4kHz, ";
                    }
                    else if (i==3) {
                        freqs += "5kHz, ";
                    }
                }
            }
        }
        return freqs;
    }

    public String warning_f2() {
        String freqs="";
        for (int i = 0; i < Constants.snrs_f2.length; i++) {
            if (Constants.freqs[i]) {
                if (Constants.snrs_f2[i] < Constants.TONE_F2_MIN_THRESH) {
                    if (i==0) {
                        freqs += "2kHz, ";
                    }
                    else if (i==1) {
                        freqs += "3kHz, ";
                    }
                    else if (i==2) {
                        freqs += "4kHz, ";
                    }
                    else if (i==3) {
                        freqs += "5kHz, ";
                    }
                }
            }
        }
        return freqs;
    }

    public String warningCheck() {
        String freqs="";
        for (int i = 0; i < Constants.snrs_f1.length; i++) {
            if (Constants.freqs[i]) {
                Log.e("mywarning",Constants.snrs_f1[i]+","+Constants.snrs_f2[i]);
                if (Constants.snrs_f1[i] < Constants.TONE_F1_MIN_THRESH ||
                    Constants.snrs_f2[i] < Constants.TONE_F2_MIN_THRESH) {
                    String snrValues=
                            String.format("%.1f:%.1f, ",Constants.snrs_f1[i],Constants.snrs_f2[i]);
                    if (i==0) {
                        freqs += "2kHz:"+snrValues;
                    }
                    else if (i==1) {
                        freqs += "3kHz:"+snrValues;
                    }
                    else if (i==2) {
                        freqs += "4kHz:"+snrValues;
                    }
                    else if (i==3) {
                        freqs += "5kHz:"+snrValues;
                    }
                }
            }
        }
        if (freqs.length()>0) {
            freqs=freqs.trim();
            freqs = freqs.substring(0, freqs.length() - 1);
            if (!noisyDisplayed && Constants.SOUND_VOLUME_CHECK) {
                warning(freqs);
            }
            probeSoundLowView.setText("Tones being played are too soft ("+freqs+")");
        }
        return freqs;
    }

    public void warning(String freqs) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
//                            .setTitle("Warning")
                        .setMessage("Warning\n\nOur system has detected that the tones being played are too soft ("+freqs+").\n\n" +
                                "Please check that the probe hardware is intact. Restart the app, and " +
                                "perform testing is performed in a quiet environment.")
                        .setPositiveButton("Ok", null)
                        .setCancelable(false).create();
                dialog.show();
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(20);
            }
        });
    }

    private class SendSignal extends AsyncTask<Integer,Void,Void> {
        Activity context;
        Button leftButton,rightButton;
        Button cancelButton;
        BarChart chart;
        LineChart lineChart;
        TextInputEditText et;
        TextInputEditText et2;
        String pid,uid,ear;
        String attemptNumber="";
        String site;

        public SendSignal(Activity context, BarChart chart, LineChart lineChart, Button leftButton, Button rightButton, Button cancelButton, String ear) {
            this.leftButton = leftButton;
            this.rightButton=rightButton;
            this.cancelButton = cancelButton;
            this.chart = chart;
            this.lineChart=lineChart;
            this.context=context;
            this.et=(TextInputEditText)context.findViewById(R.id.patientID);
            this.et2 = (TextInputEditText)context.findViewById(R.id.userID);
            this.pid = et.getText().toString().length() == 0 ? "0" : et.getText().toString();
            this.uid = et2.getText().toString();
            this.ear = ear;
        }
        protected void onPreExecute() {
            super.onPreExecute();
            noisyDisplayed=false;
            enableBottomBar(false);
            this.leftButton.setEnabled(false);
            this.rightButton.setEnabled(false);
            this.cancelButton.setEnabled(true);
            probeSoundLowView.setText("");
            noiseView.setText("");
            Constants.test_timestamp = System.currentTimeMillis();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            this.attemptNumber = FileOperations.getAttemptNumber(context,pid,uid,ear,Constants.test_timestamp);
            Constants.filename = format.format(new Date(Constants.test_timestamp))+"-"+pid+"-"+uid+"-"+ear+"-"+attemptNumber+"-"+Constants.phone;
            Log.e("filename",Constants.filename);

            fnameView.setText(Constants.filename);
        }

        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            try {
                boolean incomplete = true;
                while (incomplete) {
                    incomplete=false;
                    for (int i = 0; i < Constants.freqs.length; i++) {
                        if (Constants.freqs[i] && !Constants.done[i]) {
                            incomplete=true;
                            Thread.sleep(100);
                        }
                    }
                }
            }
            catch(Exception e) {
                Log.e("asdf",e.getMessage());
            }

            String noiseVal=checkNoiseThresholds(getActivity());
            boolean result2=checkStatus(getActivity());
            FileOperations.writeCSV(getActivity(),pid,uid,ear,attemptNumber,Constants.signal,Constants.noise,Constants.snrs,
                    noiseVal,Constants.ambient,result2,Constants.CONSTANT_TONE_LENGTH_IN_SECONDS,true);
            this.leftButton.setEnabled(true);
            this.rightButton.setEnabled(true);
            this.cancelButton.setEnabled(false);
            enableBottomBar(true);
            FileOperations.writeRecToDisk(context, fnameView);

            warningCheck();

            Constants.ear="";
        }

        public Void doInBackground(Integer... params) {
            int n = Constants.octaves.size();

            Constants.initbuffer = new short[n][Constants.PROCESS_WINDOW_LENGTH];
            Constants.sumbuffer = new double[n][Constants.PROCESS_WINDOW_LENGTH];
            Constants.numTries = new int[n];
            Constants.signal = new double[n];
            Constants.noise = new double[n];
            Constants.snrs = new double[n];
            Constants.ambient = new double[n];
            Constants.complete = new boolean[n];
            Constants.done = new boolean[n];
            Constants.signal_f1 = new double[n];
            Constants.signal_f2 = new double[n];
            Constants.noise_f1 = new double[n];
            Constants.noise_f2 = new double[n];
            Constants.snrs_f1 = new double[n];
            Constants.snrs_f2 = new double[n];

            Constants.graphData = new ArrayList<>();
            Constants.lineData1 = new ArrayList<>();
            Constants.lineData2 = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Constants.graphData.add(i, new BarEntry(Constants.octaves.get(i)/1000f, new float[]{(float) 0}));
            }
            Constants.fullrec = new LinkedList<>();

            for (Integer i : Constants.vol1LookupDefaults.keySet()) {
                Constants.vol1Lookup.put(i, Constants.vol1LookupDefaults.get(i));
            }

            Log.e("asdf","constant");
            if (Constants.CHECK_FIT) {
                checkfit();
            }

            if (Constants.VOL_CALIB) {
                vol_calib();
            }
            else {
                populateVolume(getContext());
            }

            ProgressBar pb2 = null;
            if (Constants.MEASURE_LOADER) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Constants.measureDialog = measureDialog();
                        Constants.measureDialog.show();
                    }
                });
                try {
                    while (view3 == null) {
                        Log.e("asdf", "view3 is null");
                        Thread.sleep(100);
                    }
                    pb2 = view3.findViewById(R.id.progressBar3);
                    while (pb2==null) {
                        pb2=view3.findViewById(R.id.progressBar3);
                        Thread.sleep(100);
                    }
                }
                catch(Exception e) {
                    Log.e("asdf",e.getMessage());
                }
            }

            double fcountertotal=0.0;
            for (int i = 0; i < n; i++) {
                if (Constants.freqs[i]) {
                    fcountertotal+=1;
                }
            }

            Constants.vv=getActivity().getWindow().getDecorView().getRootView();
            Constants.context=getActivity();

            Constants.totalSeconds = (Constants.CONSTANT_TONE_LENGTH_IN_SECONDS+1.5)*fcountertotal;
            int fcounter=1;
            Log.e("asdf","tones");
            for (int i = 0; i < Constants.freqs.length; i++) {
                Log.e("debug",i+":"+Constants.freqs[i]);
            }

            for (int i = 0; i < n; i++) {
                if (Constants.freqs[i]) {
                    float freq = Constants.octaves.get(i);
                    final int finalFcounter = fcounter;
                    final double finalFcountertotal = fcountertotal;
                    final int progress = (int) ((finalFcounter / finalFcountertotal)*100);

                    if (progress==100) {
                        sendTone((int) freq, i, 0, true);
                    }
                    else {
                        sendTone((int) freq, i, 0, false);
                    }

                    if (Constants.MEASURE_LOADER) {
                        final ProgressBar finalPb = pb2;
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("asdf","prevprog1 "+finalPb.getProgress());
                                Log.e("asdf","progress "+progress);
                                finalPb.setProgress(progress);
                                Log.e("asdf","prevprog2 "+finalPb.getProgress());
                            }
                        });
                    }

                    if (fcounter < fcountertotal) {
                        Log.e("asdf","sleep");
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                            Log.e("asdf", e.getMessage());
                        }
                    }

                    fcounter+=1;
                }
            }

            if (Constants.MEASURE_LOADER) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.measureDialog!= null) {
                            Constants.measureDialog.dismiss();
                            Constants.measureDialog=null;
                        }
                    }
                });
                view3=null;
            }

            Log.e("asdf","done");
            return null;
        }

        public
        <T extends Comparable<? super T>> ArrayList<T> asSortedList(Collection<T> c) {
            ArrayList<T> list = new ArrayList<T>(c);
            java.util.Collections.sort(list);
            return list;
        }

        public ArrayList<Integer> getList() {
            ArrayList<Integer> freqs = asSortedList(Constants.vol1LookupDefaults.keySet());
            ArrayList<Integer> freqs2 = new ArrayList<>();
            for (int i = 0; i < freqs.size(); i++) {
                if (Constants.freqs[i/2]) {
                    freqs2.add(freqs.get(i));
                }
            }
            return freqs2;
        }

        public AlertDialog loadingDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflator = getLayoutInflater();
            view2 = inflator.inflate(R.layout.load_dialog, null);
            builder.setView(view2);
            builder.setCancelable(false);
            return builder.create();
        }

        public AlertDialog measureDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflator = getLayoutInflater();
            view3 = inflator.inflate(R.layout.ongoing, null);
            builder.setView(view3);
            builder.setCancelable(true);
            return builder.create();
        }

        public void vol_calib() {
            short[] pre=Utils.generateChirpSpeaker2(1000,4000,.1,Constants.samplingRate,0,1);
            int padlen=4800;
            short[] output_signal=new short[padlen+(int)((pre.length+(8*Constants.TONE_CALIB_LENGTH_IN_SECONDS * Constants.samplingRate)))*2];
            int counter=padlen;
            for (int i = 0; i < pre.length; i++) {
                output_signal[counter++]=pre[i];
            }

            for (int i = 0; i < 4; i++) {
                int freq = Constants.octaves.get(i);

                int f1 = Constants.freqLookup.get(freq);
                int f2 = freq;

                short[] pulse1;
                short[] pulse2;
                Log.e("asdf", "get " + f1);
                Log.e("asdf", "get " + f2);
                float vol3a = Constants.vol3Lookup.get(f1);
                float vol3b = Constants.vol3Lookup.get(f2);

                pulse1 = SignalGenerator.sine2speaker(f1, f2,
                        Constants.samplingRate,
                        (int)(Constants.TONE_CALIB_LENGTH_IN_SECONDS * Constants.samplingRate),
                        vol3a,
                        0);
                pulse2 = SignalGenerator.sine2speaker(f1, f2,
                        Constants.samplingRate,
                        (int)(Constants.TONE_CALIB_LENGTH_IN_SECONDS * Constants.samplingRate),
                        0,
                        vol3b);
                for (int j = 0; j < pulse1.length; j++) {
                    output_signal[counter++]=pulse1[j];
                }
                for (int j = 0; j < pulse2.length; j++) {
                    output_signal[counter++]=pulse2[j];
                }
            }

            // $TODO: is volDefault populated?
            AudioStreamer sp = new AudioStreamer(context, output_signal,
                    output_signal.length, Constants.samplingRate,
                    AudioManager.STREAM_SYSTEM,Constants.volDefault,false);
            gsp=sp;

            int micType;
            AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            Log.e("AGC","2agc "+Constants.AGC+","+audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
            if (Constants.AGC) {
                if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
                    micType = (MediaRecorder.AudioSource.UNPROCESSED);
                } else {
                    micType = (MediaRecorder.AudioSource.VOICE_RECOGNITION);
                }
            }
            else {
                micType = MediaRecorder.AudioSource.DEFAULT;
            }

//            // TODO: make sure the recording and speaker overlap in time for sufficiently long
            OfflineRecorder orec;

            orec = new OfflineRecorder(micType, 0, 0, 0, barChart, lineChart, context,
                    (int) (output_signal.length/2+(Constants.samplingRate*Constants.PAD_CALIB_LENGTH_IN_SECONDS)),
                    false, true);
            orec.sp=sp;

            rec = orec;

            try {
                orec.start();
//                Thread.sleep(100);
                sp.play(1);
                while (orec.recording || orec.rec.getState() == AudioRecord.RECORDSTATE_STOPPED) {
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                Log.e("ex","sendtone");
                Log.e("ex",e.getMessage());
            }
            Log.e("asdf","done with vol calib");
        }

        public void checkfit() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Constants.dialog = loadingDialog();
                    Constants.dialog.show();
                }
            });

            AudioStreamer sp;
            if (Constants.calibSig.equals("chirp")) {
                short[] chirpCopy = new short[Constants.chirp.length];
                for (int i = 0; i < chirpCopy.length; i++) {
                    chirpCopy[i] = (short) (Constants.chirp[i] * 32000);
                }
//                sp.prime(chirpCopy, .4);
                sp = new AudioStreamer(context, chirpCopy, (Constants.samplingRate*Constants.CONSTANT_TONE_LENGTH_IN_SECONDS) * 2,
                        Constants.samplingRate, AudioManager.STREAM_SYSTEM,.4,true);
            }
            else {
                int checkfreq=226;
                short[] pulse = SignalGenerator.sine2speaker(checkfreq, checkfreq,
                        Constants.samplingRate,
                        Constants.CONSTANT_TONE_LENGTH_IN_SECONDS*Constants.samplingRate,
                        .8,
                        .8);
//                sp.prime(pulse, .4);
                if (Constants.phone.equals("sch")) {
                    sp = new AudioStreamer(context, pulse, (Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS) * 2,
                            Constants.samplingRate, AudioManager.STREAM_SYSTEM, .6, true);
                }
                else if (Constants.phone.equals("sch2")) {
                    sp = new AudioStreamer(context, pulse, (Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS) * 2,
                            Constants.samplingRate, AudioManager.STREAM_SYSTEM, .4, true);
                }
                else if (Constants.phone.contains("kenya")) {
                    sp = new AudioStreamer(context, pulse, (Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS) * 2,
                            Constants.samplingRate, AudioManager.STREAM_SYSTEM, .4, true);
                }
                else {
                    // Use some defaults for unknown phones.
                    sp = new AudioStreamer(context, pulse, (Constants.samplingRate * Constants.CONSTANT_TONE_LENGTH_IN_SECONDS) * 2,
                            Constants.samplingRate, AudioManager.STREAM_SYSTEM, .4, true);
                }
            }
            gsp=sp;

            int micType;
            AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            Log.e("AGC","1agc "+Constants.AGC+","+audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
            if (Constants.AGC) {
                if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
                    micType = (MediaRecorder.AudioSource.UNPROCESSED);
                } else {
                    micType = (MediaRecorder.AudioSource.VOICE_RECOGNITION);
                }
            }
            else {
                micType = MediaRecorder.AudioSource.DEFAULT;
            }

            // TODO: make sure the recording and speaker overlap in time for sufficiently long
            OfflineRecorder orec;

            orec = new OfflineRecorder(micType, 0, 0, 0, barChart, lineChart, context,
                    (int) (30 * Constants.samplingRate), true, false);
            orec.sp=sp;

            while (view2==null) {

            }
            Chip passChip = view2.findViewById(R.id.rightChip);
            while (passChip==null) {
                passChip = view2.findViewById(R.id.rightChip);
            }
            orec.passChip=passChip;

            Chip failChip = view2.findViewById(R.id.leftChip);
            failChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    measure.cancel(true);
                    try {
                        gsp.stopit();
                        gsp.join();
                    } catch (InterruptedException e) {
                        Log.e("ex",e.getMessage());
                    }

                    try {
                        rec.stopit();
                        rec.join();
                    } catch (InterruptedException e) {
                        Log.e("ex",e.getMessage());
                    }
                    enableBottomBar(true);
                    leftButton.setEnabled(true);
                    rightButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    FileOperations.writeRecToDisk(getActivity(), fnameView);
                    Constants.dialog.dismiss();
                }
            });

            ProgressBar pb = view2.findViewById(R.id.progressBar2);
            orec.pb=pb;
            TextView tv=view2.findViewById(R.id.textView);
            orec.tv=tv;

            rec = orec;

            try {
                orec.start();
                sp.play(-1);
                while (orec.recording || orec.rec.getState() == AudioRecord.RECORDSTATE_STOPPED) {
                    Thread.sleep(10);
                }
//                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e("ex","sendtone");
                Log.e("ex",e.getMessage());
            }
            Log.e("asdf","done with checkfit");
        }

        private void sendTone(int freq, int fidx, int tidx, boolean ss) {
            int f1 = Constants.freqLookup.get(freq);
            int f2 = freq;

            short[] pulse;
            Log.e("justin","sendtone get "+f1+","+freq);
            Log.e("justin","send tone get "+f2+","+freq);
            float vol3a = Constants.vol3Lookup.get(f1);
            float vol3b = Constants.vol3Lookup.get(f2);

            pulse = SignalGenerator.sine2speaker(f1, f2,
                    Constants.samplingRate,
                    Constants.CONSTANT_TONE_LENGTH_IN_SECONDS*Constants.samplingRate,
                    vol3a,
                    vol3b);

            Log.e("out","speaker "+f1+","+f2);

            float vol1 = Constants.vol1Lookup.get(f2);

            AudioStreamer sp = new AudioStreamer(context, pulse, Constants.samplingRate*Constants.CONSTANT_TONE_LENGTH_IN_SECONDS*2,
                    Constants.samplingRate, AudioManager.STREAM_SYSTEM,vol1,false);
            gsp=sp;
//            sp.prime(pulse, vol1);
//            Log.e("vol","vol1 , "+f2+","+vol1);

            int micType;
            AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            Log.e("AGC","3agc "+Constants.AGC+","+audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
            if (Constants.AGC) {
                if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
                    micType = (MediaRecorder.AudioSource.UNPROCESSED);
                } else {
                    micType = (MediaRecorder.AudioSource.VOICE_RECOGNITION);
                }
            }
            else {
                micType = MediaRecorder.AudioSource.DEFAULT;
            }

            // TODO: make sure the recording and speaker overlap in time for sufficiently long
            OfflineRecorder orec;
            orec = new OfflineRecorder(micType, fidx, tidx, freq, barChart, lineChart, context,
                    (int) (Constants.CONSTANT_TONE_LENGTH_IN_SECONDS * Constants.samplingRate), false, false);

            orec.ss=ss;
            rec = orec;

            try {
                orec.start();

                sp.play(-1);
                Log.e("debug",Constants.CONSTANT_TONE_LENGTH_IN_SECONDS+"");
                Thread.sleep((long) (Constants.CONSTANT_TONE_LENGTH_IN_SECONDS*1000));

                Log.e("asdf","stop it");
                sp.stopit();

                while (orec.recording||
                        orec.rec.getState()!=AudioRecord.RECORDSTATE_STOPPED||
                        sp.track1.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    Thread.sleep(100);
                }

                Log.e("asdf","STOP");

            } catch (Exception e) {
                Log.e("ex","sendtone");
                Log.e("ex",e.getMessage());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Constants.CurrentFragment = this;
        Constants.MeasureFragment = this;
        calibration_reminder();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Constants.CurrentFragment = this;
        Constants.MeasureFragment = this;

        Log.e("asdf","MEASURE FREQS");
        for (int i = 0; i < Constants.freqs.length; i++) {
            Log.e("asdf",i+":"+Constants.freqs[i]);
        }
    }

//    public static native double[] fftnative_short(short[] data, int N);
    public static native double[] fftnative(double[] data, int N);
}
