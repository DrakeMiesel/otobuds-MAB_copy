package com.example.dpoae;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings, container, false);
        ((MainActivity) getActivity()).getSupportActionBar().hide();
        initView(view);
        return view;
    }

    public void checkAction(int c, boolean isChecked) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        Log.e("asdf","checkaction "+c+","+isChecked);
        if (c==1) {
            Log.e("asdf","set check0 to "+isChecked);
            editor.putBoolean("check0", isChecked);
            Constants.freqs[0] = isChecked;
        }
        else if (c==2) {
            editor.putBoolean("check1", isChecked);
            Constants.freqs[1] = isChecked;
        }
        else if (c==3) {
            editor.putBoolean("check2", isChecked);
            Constants.freqs[2] = isChecked;
        }
        else if (c==4) {
            editor.putBoolean("check3", isChecked);
            Constants.freqs[3] = isChecked;
        }
        for (int i = 0; i < Constants.freqs.length; i++) {
            Log.e("asdf",i+":"+Constants.freqs[i]);
        }
        editor.commit();
    }

    public void initView(View view) {
        final TextInputEditText constantToneLength = view.findViewById(R.id.constantToneLength);
        final TextInputEditText checkFitThresh = view.findViewById(R.id.checkFitThresh);
        final Switch onlyLeftSwitch = view.findViewById(R.id.onlyLeftSwitch);
        final Switch checkFitSwitch = view.findViewById(R.id.checkFitSwitch);
        final Switch noiseCheckSwitch = view.findViewById(R.id.noiseCheckSwitch);
        final Switch soundCheckSwitch = view.findViewById(R.id.soundVolSwitch);
        final CheckBox c1 = view.findViewById(R.id.check1);
        final CheckBox c2 = view.findViewById(R.id.check2);
        final CheckBox c3 = view.findViewById(R.id.check3);
        final CheckBox c4 = view.findViewById(R.id.check4);
        final TextView tv8 = view.findViewById(R.id.textView8);
        tv8.setText("Phone ID: "+Constants.phone);

        MaterialAutoCompleteTextView autoview = view.findViewById(R.id.filled_exposed_dropdown);

        List<String> spinnerArray =  new ArrayList<String>();
        spinnerArray.add("Daily");
        spinnerArray.add("Weekly");
        spinnerArray.add("Monthly");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        autoview.setAdapter(adapter);

        Log.e("asdf","SET CHECKED ");
        c1.setChecked(Constants.freqs[0]);
        c2.setChecked(Constants.freqs[1]);
        c3.setChecked(Constants.freqs[2]);
        c4.setChecked(Constants.freqs[3]);
        constantToneLength.setText(Constants.CONSTANT_TONE_LENGTH_IN_SECONDS+"");
        onlyLeftSwitch.setChecked(Constants.ONLY_LEFT);
        checkFitThresh.setText(Constants.SEAL_CHECK_THRESH+"");
        checkFitSwitch.setChecked(Constants.CHECK_FIT);
        noiseCheckSwitch.setChecked(Constants.NOISE_CHECK);
        soundCheckSwitch.setChecked(Constants.SOUND_VOLUME_CHECK);
        autoview.setText(autoview.getAdapter().getItem(Constants.reminderFrequency.ordinal()).toString(), false);

        c1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkAction(1,isChecked);
            }
        });
        c2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkAction(2,isChecked);
            }
        });
        c3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkAction(3,isChecked);
            }
        });
        c4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkAction(4,isChecked);
            }
        });
        autoview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                Constants.Frequency freq = Constants.Frequency.valueOf(autoview.getText().toString());
                editor.putInt("reminderFrequency", freq.ordinal());
                editor.commit();
                Constants.reminderFrequency=Constants.Frequency.values()[freq.ordinal()];
                Log.e("freq",Constants.reminderFrequency+"");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        constantToneLength.addTextChangedListener(new TextWatcher() {
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
                String ss = constantToneLength.getText().toString();
                if (ss.length()>0) {
                    if (Integer.parseInt(ss) > 6) {
                        constantToneLength.setText("6");
                        editor.putInt("constantToneLength", 6);
                        editor.commit();
                        Constants.CONSTANT_TONE_LENGTH_IN_SECONDS = Integer.parseInt(ss);
                    }
                    else if (Integer.parseInt(ss) < 2) {
                        constantToneLength.setText("2");
                        editor.putInt("constantToneLength", 2);
                        editor.commit();
                        Constants.CONSTANT_TONE_LENGTH_IN_SECONDS = Integer.parseInt(ss);
                    }
                    else {
                        editor.putInt("constantToneLength", Integer.parseInt(ss));
                        editor.commit();
                        Constants.CONSTANT_TONE_LENGTH_IN_SECONDS = Integer.parseInt(ss);
                    }
                }
            }
        });

        checkFitThresh.addTextChangedListener(new TextWatcher() {
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
                String ss = checkFitThresh.getText().toString();
                if (ss.length()>0) {
                    editor.putInt("checkFitThresh", Integer.parseInt(ss));
                    editor.commit();
                    Constants.SEAL_CHECK_THRESH = Integer.parseInt(ss);
                }
            }
        });

        onlyLeftSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean("onlyLeft", isChecked);
                editor.commit();
                Constants.ONLY_LEFT  = isChecked;
            }
        });
        checkFitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean("checkFit", isChecked);
                editor.commit();
                Constants.CHECK_FIT  = isChecked;
            }
        });
        noiseCheckSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean("noiseCheck", isChecked);
                editor.commit();
                Constants.NOISE_CHECK  = isChecked;
            }
        });
        soundCheckSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean("soundVolCheck", isChecked);
                editor.commit();
                Constants.SOUND_VOLUME_CHECK  = isChecked;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Constants.CurrentFragment = this;
        Constants.SettingsFragment = this;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Constants.CurrentFragment = this;
        Constants.SettingsFragment = this;
    }
}
