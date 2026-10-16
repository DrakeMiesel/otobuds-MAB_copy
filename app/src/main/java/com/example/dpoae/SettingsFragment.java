package com.example.dpoae;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    // SharedPreferences key prefixes read by Constants.populateVolume(), one
    // column each in the volume table: base speaker volume, f1 tone, f2 tone.
    private static final String[] VOLUME_PREF_PREFIXES = {"volumeValue_", "volumeF1te_", "volumeF2te_"};

    private TextView buildInfo;

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

        buildInfo = view.findViewById(R.id.buildInfo);
        updateBuildInfo();

        final LinearLayout volumeTable = view.findViewById(R.id.volumeTable);
        initVolumeTable(volumeTable);
        view.findViewById(R.id.resetVolumes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    resetVolumes();
                    initVolumeTable(volumeTable);
                } catch (IllegalStateException e) {
                    Log.e("ProbeProfiles", e.getMessage(), e);
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

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

    private void updateBuildInfo() {
        buildInfo.setText(
                "Version " + BuildConfig.VERSION_NAME
                        + "  ·  " + BuildConfig.GIT_BRANCH
                        + "\n" + BuildConfig.BUILD_LABEL
                        + "\nBuilt " + BuildConfig.BUILD_TIME
                        + "\n" + activeCalibrationSummary());
    }

    /**
     * Fills the volume table with one row per test frequency. Fields are
     * created fresh (listeners attached after the initial text is set) so
     * rebuilding the table never writes values back to preferences.
     */
    private void initVolumeTable(LinearLayout table) {
        table.removeAllViews();
        table.addView(volumeRow(new TextView[]{
                label("Frequency"), label("Base"), label("f1"), label("f2")}));

        for (int i = 0; i < Constants.f2.length; i++) {
            Float[] current = {
                    Constants.vol1LookupDefaults.get(Constants.f2[i]),
                    Constants.vol3Lookup.get(Constants.f1[i]),
                    Constants.vol3Lookup.get(Constants.f2[i])};

            TextView[] cells = new TextView[4];
            cells[0] = label(Constants.f2[i] + " Hz");
            for (int col = 0; col < VOLUME_PREF_PREFIXES.length; col++) {
                EditText field = new EditText(getActivity());
                field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                field.setImeOptions(EditorInfo.IME_ACTION_DONE);
                field.setSelectAllOnFocus(true);
                field.setGravity(Gravity.CENTER);
                field.setText(current[col] != null ? String.valueOf(current[col]) : "");
                field.addTextChangedListener(volumeWatcher(field, i, col));
                cells[col + 1] = field;
            }
            table.addView(volumeRow(cells));
        }
    }

    private TextView label(String text) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private LinearLayout volumeRow(TextView[] cells) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (TextView cell : cells) {
            row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return row;
    }

    private TextWatcher volumeWatcher(final EditText field, final int freqIndex, final int col) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Float v = null;
                try {
                    v = Float.parseFloat(s.toString());
                } catch (NumberFormatException ignored) {
                }
                // Both volumes are fractions of full scale; >1 would clip the
                // stream volume and overflow the 16-bit tone samples.
                if (v == null || !Float.isFinite(v) || v < 0 || v > 1) {
                    field.setError("0-1");
                    return;
                }
                try {
                    ProbeProfiles.requireIdle();
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putFloat(VOLUME_PREF_PREFIXES[col] + freqIndex, v);
                    ProbeProfiles.commit(editor);
                    applyVolume(freqIndex, col, v);
                    updateBuildInfo();
                    field.setError(null);
                } catch (IllegalStateException e) {
                    Log.e("ProbeProfiles", e.getMessage(), e);
                    field.setError(e.getMessage());
                }
            }
        };
    }

    /**
     * Updates the in-memory lookups the same way Constants.populateVolume()
     * does, without re-running it (it also resets SEAL_CHECK_THRESH).
     */
    private void applyVolume(int freqIndex, int col, float v) {
        int f1 = Constants.f1[freqIndex];
        int f2 = Constants.f2[freqIndex];
        if (col == 0) {
            Constants.vol1LookupDefaults.put(f2, v);
            Constants.vol1LookupDefaults.put(f1, v);
            Constants.vol1Lookup.put(f2, v);
            Constants.vol1Lookup.put(f1, v);
        } else if (col == 1) {
            Constants.vol3Lookup.put(f1, v);
        } else {
            Constants.vol3Lookup.put(f2, v);
        }
    }

    /** Drops saved volume overrides so the phone profile's defaults apply again. */
    private void resetVolumes() {
        ProbeProfiles.requireIdle();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        for (int i = 0; i < Constants.f2.length; i++) {
            for (String prefix : VOLUME_PREF_PREFIXES) {
                editor.remove(prefix + i);
            }
        }
        ProbeProfiles.commit(editor);
        ProbeProfiles.reloadVolumes(getActivity());
        updateBuildInfo();
    }

    /**
     * Builds a one-glance summary of the calibration profile that is actually
     * resolved at runtime. This reflects the important part of the per-device
     * calibration work (e.g. Kass's kassPhone profile): the selected phone
     * profile, the f2 probe frequencies, and the per-frequency volume defaults.
     */
    private String activeCalibrationSummary() {
        StringBuilder s = new StringBuilder("Calibration: ").append(Constants.phone);
        try {
            s.append("\nPreset: ").append(ProbeProfiles.activeLabel(getActivity()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.e("ProbeProfiles", e.getMessage(), e);
            s.append("\nPreset storage unavailable");
        }
        if (Constants.f2 != null && Constants.f2.length > 0) {
            StringBuilder freqs = new StringBuilder();
            StringBuilder vols = new StringBuilder();
            for (int i = 0; i < Constants.f2.length; i++) {
                if (i > 0) {
                    freqs.append("/");
                    vols.append("/");
                }
                freqs.append(Constants.f2[i]);
                Float v = Constants.vol1LookupDefaults != null
                        ? Constants.vol1LookupDefaults.get(Constants.f2[i]) : null;
                vols.append(v != null ? String.format("%.2f", v) : "?");
            }
            s.append("\nf2=").append(freqs).append(" Hz");
            s.append("\nvol=").append(vols);
        }
        return s.toString();
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
