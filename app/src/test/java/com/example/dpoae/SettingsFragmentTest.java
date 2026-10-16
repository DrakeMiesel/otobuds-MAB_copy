package com.example.dpoae;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29, manifest = Config.NONE)
public class SettingsFragmentTest {
    private static final String[] PREFIXES = {"volumeValue_", "volumeF1te_", "volumeF2te_"};
    private Context context;
    private SharedPreferences prefs;
    private ActivityController<AppCompatActivity> controller;
    private View view;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        assertTrue(prefs.edit().clear().commit());
        Constants.phone = "kassPhone";
        Constants.samplingRate = 48000;
        Constants.testInProgress = false;
        Constants.CheckProbeFragment = null;
        Constants.MeasureFragment = null;
        Constants.ONLY_LEFT = false;
        Constants.freqs = new boolean[]{true, true, true, true};
        Constants.f2Defaults = new int[]{1900, 2900, 3900, 4900};
        Constants.setProbeFrequencies(Constants.f2Defaults);
        Constants.populateVolume(context);
        Constants.SEAL_CHECK_THRESH = 137;
    }

    @After
    public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
        Constants.testInProgress = false;
        Constants.CheckProbeFragment = null;
        Constants.SettingsFragment = null;
        Constants.CurrentFragment = null;
    }

    @Test
    public void openingSettingsDisplaysEveryCurrentGainWithoutSavingOverrides() {
        Map<String, ?> before = prefs.getAll();
        showSettings();
        assertEquals(5, ((LinearLayout) view.findViewById(R.id.volumeTable)).getChildCount());
        ProbeProfiles.Profile current = ProbeProfiles.current();
        for (int i = 0; i < 4; i++) {
            LinearLayout row = (LinearLayout) ((LinearLayout) view.findViewById(R.id.volumeTable)).getChildAt(i + 1);
            assertEquals(current.f2[i] + " Hz", ((TextView) row.getChildAt(0)).getText().toString());
            assertEquals(current.base[i], Float.parseFloat(field(i, 0).getText().toString()), 0);
            assertEquals(current.f1v[i], Float.parseFloat(field(i, 1).getText().toString()), 0);
            assertEquals(current.f2v[i], Float.parseFloat(field(i, 2).getText().toString()), 0);
        }
        assertEquals(before, prefs.getAll());
    }

    @Test
    public void editsPersistEveryColumnUpdateRuntimeAndMarkThePresetCustom() {
        ProbeProfiles.save(context, "Saved", ProbeProfiles.current());
        String saved = prefs.getString("probe_profiles", "");
        showSettings();
        for (int i = 0; i < 4; i++) {
            for (int col = 0; col < 3; col++) {
                float value = .05f * (1 + i * 3 + col);
                field(i, col).setText(String.valueOf(value));
                assertNull(field(i, col).getError());
                assertEquals(value, prefs.getFloat(PREFIXES[col] + i, -1), 0);
                if (col == 0) {
                    assertEquals(value, Constants.vol1Lookup.get(Constants.f2[i]), 0);
                    assertEquals(value, Constants.vol1Lookup.get(Constants.f1[i]), 0);
                    assertEquals(value, Constants.vol1LookupDefaults.get(Constants.f2[i]), 0);
                    assertEquals(value, Constants.vol1LookupDefaults.get(Constants.f1[i]), 0);
                } else {
                    int tone = col == 1 ? Constants.f1[i] : Constants.f2[i];
                    assertEquals(value, Constants.vol3Lookup.get(tone), 0);
                }
            }
        }
        field(0, 0).setText("0");
        assertEquals(0, prefs.getFloat("volumeValue_0", -1), 0);
        field(0, 0).setText("1");
        assertEquals(1, prefs.getFloat("volumeValue_0", -1), 0);
        assertEquals(137, Constants.SEAL_CHECK_THRESH);
        assertEquals(saved, prefs.getString("probe_profiles", ""));
        assertEquals("Custom", ProbeProfiles.activeLabel(context));
        assertTrue(((TextView) view.findViewById(R.id.buildInfo)).getText().toString().contains("Preset: Custom"));
    }

    @Test
    public void invalidOrNonFiniteTextLeavesTheLastValidGainInUse() {
        showSettings();
        EditText field = field(0, 1);
        field.setText("0.31");
        Map<String, ?> before = prefs.getAll();
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        for (String invalid : new String[]{"", ".", "NaN", "Infinity", "-Infinity", "-0.01", "1.01"}) {
            field.setText(invalid);
            assertNotNull(field.getError());
            assertEquals(before, prefs.getAll());
            assertEquals(.31f, Constants.vol3Lookup.get(Constants.f1[0]), 0);
        }
        field.setText("0.32");
        assertNull(field.getError());
        assertEquals(.32f, prefs.getFloat("volumeF1te_0", -1), 0);
    }

    @Test
    public void resetRestoresOnlyPhoneVolumeDefaultsAndRefreshesTheTable() {
        ProbeProfiles.Profile p = ProbeProfiles.current();
        p.f2 = new int[]{2016, 2953, 3985, 4969};
        p.base = new float[]{.1f, .2f, .3f, .4f};
        p.f1v = new float[]{.2f, .3f, .4f, .5f};
        p.f2v = new float[]{.3f, .4f, .5f, .6f};
        p.onlyLeft = true;
        ProbeProfiles.save(context, "Saved", p);
        ProbeProfiles.apply(context, "Saved", p);
        String saved = prefs.getString("probe_profiles", "");
        assertTrue(prefs.edit().putInt("constantToneLength", 4).putInt("checkFitThresh", 137).commit());
        showSettings();
        view.findViewById(R.id.resetVolumes).performClick();
        assertArrayEquals(p.f2, Constants.f2);
        assertTrue(Constants.ONLY_LEFT);
        assertTrue(prefs.getBoolean("onlyLeft", false));
        assertEquals(saved, prefs.getString("probe_profiles", ""));
        assertEquals("Saved", prefs.getString("active_probe", ""));
        assertEquals("Custom", ProbeProfiles.activeLabel(context));
        assertEquals(4, prefs.getInt("constantToneLength", 0));
        assertEquals(137, prefs.getInt("checkFitThresh", 0));
        assertEquals(137, Constants.SEAL_CHECK_THRESH);
        float[][] defaults = {{.15f, .45f, .45f, .50f}, {1, 1, 1, 1}, {.2f, .9f, 1, 1}};
        ProbeProfiles.Profile current = ProbeProfiles.current();
        assertArrayEquals(defaults[0], current.base, 0);
        assertArrayEquals(defaults[1], current.f1v, 0);
        assertArrayEquals(defaults[2], current.f2v, 0);
        for (int i = 0; i < 4; i++) {
            assertEquals(p.f2[i], prefs.getInt("f2Value_" + i, 0));
            for (int col = 0; col < 3; col++) {
                assertFalse(prefs.contains(PREFIXES[col] + i));
                assertEquals(defaults[col][i], Float.parseFloat(field(i, col).getText().toString()), 0);
            }
        }
    }

    @Test
    public void editsAndResetCannotChangeVolumesWhileAudioIsActive() {
        showSettings();
        field(0, 0).setText("0.31");
        Map<String, ?> before = prefs.getAll();
        Constants.CheckProbeFragment = new CheckProbeFragment();
        Constants.CheckProbeFragment.isDrawing = true;
        field(0, 0).setText("0.33");
        assertNotNull(field(0, 0).getError());
        view.findViewById(R.id.resetVolumes).performClick();
        assertTrue(ShadowToast.getTextOfLatestToast().contains("Stop the measurement or probe audio"));
        assertEquals(before, prefs.getAll());
        assertEquals(.31f, Constants.vol1Lookup.get(Constants.f2[0]), 0);

        Constants.CheckProbeFragment.isDrawing = false;
        Constants.testInProgress = true;
        field(0, 0).setText("0.34");
        assertNotNull(field(0, 0).getError());
        view.findViewById(R.id.resetVolumes).performClick();
        assertEquals(before, prefs.getAll());
        assertEquals(.31f, Constants.vol1Lookup.get(Constants.f2[0]), 0);

        Constants.testInProgress = false;
        field(0, 0).setText("0.35");
        assertNull(field(0, 0).getError());
        assertEquals(.35f, prefs.getFloat("volumeValue_0", -1), 0);
    }

    private void showSettings() {
        controller = Robolectric.buildActivity(AppCompatActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        AppCompatActivity activity = controller.setup().get();
        SettingsFragment fragment = new TestSettingsFragment();
        activity.getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
        view = fragment.requireView();
    }

    private EditText field(int band, int column) {
        LinearLayout table = view.findViewById(R.id.volumeTable);
        return (EditText) ((LinearLayout) table.getChildAt(band + 1)).getChildAt(column + 1);
    }

    public static class TestSettingsFragment extends SettingsFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            View view = inflater.inflate(R.layout.settings, container, false);
            initView(view);
            return view;
        }
    }
}
