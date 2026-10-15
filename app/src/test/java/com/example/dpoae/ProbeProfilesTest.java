package com.example.dpoae;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29, manifest = Config.NONE)
public class ProbeProfilesTest {
    private Context context;
    private SharedPreferences prefs;
    private final int[] phoneFrequencies = {1900, 2900, 3900, 4900};

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
        Constants.f2Defaults = phoneFrequencies.clone();
        Constants.setProbeFrequencies(phoneFrequencies);
        Constants.populateVolume(context);
        Constants.SEAL_CHECK_THRESH = 137;
    }

    @Test
    public void savedPresetHasStableVersionedIdentityAndSaveAsCreatesAnotherIdentity() throws Exception {
        ProbeProfiles.save(context, "Probe A", ProbeProfiles.current());
        JSONObject stored = stored("Probe A");
        assertEquals(1, stored.getInt("formatVersion"));
        String id = stored.getString("id");
        assertEquals(id, UUID.fromString(id).toString());
        ProbeProfiles.Profile loaded = ProbeProfiles.get(context, "Probe A");
        assertNotNull(loaded);
        assertEquals(id, loaded.id);
        assertTrue(loaded.savedAt > 0);
        ProbeProfiles.save(context, "Probe A", loaded);
        assertEquals(id, ProbeProfiles.get(context, "Probe A").id);
        ProbeProfiles.save(context, "Probe B", loaded);
        assertNotEquals(id, ProbeProfiles.get(context, "Probe B").id);
        assertEquals(id, ProbeProfiles.get(context, "Probe A").id);
        assertEquals("Probe B", ProbeProfiles.activeLabel(context));
    }

    @Test
    public void legacyPresetIsReadableAndGetsAnIdOnlyWhenSaved() throws Exception {
        ProbeProfiles.save(context, "Legacy", ProbeProfiles.current());
        JSONObject all = new JSONObject(prefs.getString("probe_profiles", ""));
        JSONObject legacy = all.getJSONObject("Legacy");
        legacy.remove("formatVersion");
        legacy.remove("id");
        legacy.remove("onlyLeft");
        assertTrue(prefs.edit().putString("probe_profiles", all.toString()).commit());
        String before = prefs.getString("probe_profiles", "");
        ProbeProfiles.Profile loaded = ProbeProfiles.get(context, "Legacy");
        assertNull(loaded.id);
        assertNull(loaded.onlyLeft);
        assertNull(ProbeProfiles.get(context, "Legacy").id);
        assertEquals(before, prefs.getString("probe_profiles", ""));
        Constants.ONLY_LEFT = true;
        ProbeProfiles.apply(context, "Legacy", loaded);
        assertTrue(Constants.ONLY_LEFT);
        ProbeProfiles.save(context, "Legacy", loaded);
        String id = ProbeProfiles.get(context, "Legacy").id;
        assertNotNull(id);
        ProbeProfiles.save(context, "Legacy", loaded);
        assertEquals(id, ProbeProfiles.get(context, "Legacy").id);
        assertTrue(stored("Legacy").getBoolean("onlyLeft"));
    }

    @Test
    public void loadRestoresEveryBandChannelAndDerivedLookupWithoutChangingSealThreshold() {
        ProbeProfiles.Profile p = alternateSetup();
        ProbeProfiles.apply(context, "Probe A", p);
        assertArrayEquals(p.f2, Constants.f2);
        assertTrue(Constants.ONLY_LEFT);
        assertTrue(prefs.getBoolean("onlyLeft", false));
        assertEquals(137, Constants.SEAL_CHECK_THRESH);
        assertEquals(4, Constants.freqLookup.size());
        assertEquals(8, Constants.vol3Lookup.size());
        assertFalse(Constants.vol1Lookup.containsKey(1900));
        for (int i = 0; i < 4; i++) {
            int f1 = (int) (p.f2[i] / 1.22);
            assertEquals(f1, Constants.f1[i]);
            assertEquals(p.f2[i], (int) Constants.octaves.get(i));
            assertEquals(2 * f1 - p.f2[i], Constants.oaes[i]);
            assertEquals(2 * p.f2[i] - f1, Constants.oaes2[i]);
            assertEquals(f1, (int) Constants.freqLookup.get(p.f2[i]));
            assertEquals(p.base[i], Constants.vol1Lookup.get(p.f2[i]), 0);
            assertEquals(p.base[i], Constants.vol1LookupDefaults.get(f1), 0);
            assertEquals(p.f1v[i], Constants.vol3Lookup.get(f1), 0);
            assertEquals(p.f2v[i], Constants.vol3Lookup.get(p.f2[i]), 0);
            assertEquals(p.f2[i], prefs.getInt("f2Value_" + i, 0));
            assertEquals(p.base[i], prefs.getFloat("volumeValue_" + i, 0), 0);
        }
    }

    @Test
    public void invalidLastBandDoesNotPartiallyChangePreferencesOrRuntime() {
        ProbeProfiles.Profile p = alternateSetup();
        p.base[3] = Float.NaN;
        Map<String, ?> before = prefs.getAll();
        ProbeProfiles.Profile old = ProbeProfiles.current();
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.apply(context, "Invalid", p));
        assertEquals(before, prefs.getAll());
        assertArrayEquals(old.f2, Constants.f2);
        assertArrayEquals(old.base, ProbeProfiles.current().base, 0);
        assertFalse(Constants.ONLY_LEFT);
    }

    @Test
    public void rejectsNonFiniteOutOfRangeAndAmbiguousFrequenciesBeforeSaving() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -0.01f, 1.01f}) {
            ProbeProfiles.Profile p = ProbeProfiles.current();
            p.f1v[2] = invalid;
            assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.save(context, "Bad", p));
        }
        for (int invalid : new int[]{0, 300, 21000, Integer.MAX_VALUE}) {
            ProbeProfiles.Profile p = ProbeProfiles.current();
            p.f2[0] = invalid;
            assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.save(context, "Bad", p));
        }
        ProbeProfiles.Profile duplicate = ProbeProfiles.current();
        duplicate.f2[1] = duplicate.f2[0];
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.save(context, "Bad", duplicate));
        ProbeProfiles.Profile overlap = alternateSetup();
        overlap.f2[1] = 2460; // Its f1 aliases band 0's f2 in the shared tone map.
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.save(context, "Bad", overlap));
        assertFalse(prefs.contains("probe_profiles"));
    }

    @Test
    public void unreadableAndUnsupportedStorageIsNotSilentlyOverwritten() throws Exception {
        assertTrue(prefs.edit().putString("probe_profiles", "{broken").commit());
        assertThrows(IllegalStateException.class, () -> ProbeProfiles.names(context));
        assertThrows(IllegalStateException.class,
                () -> ProbeProfiles.save(context, "New", ProbeProfiles.current()));
        assertEquals("{broken", prefs.getString("probe_profiles", ""));

        prefs.edit().remove("probe_profiles").commit();
        ProbeProfiles.save(context, "Future", ProbeProfiles.current());
        JSONObject all = new JSONObject(prefs.getString("probe_profiles", ""));
        all.getJSONObject("Future").put("formatVersion", 2);
        String future = all.toString();
        assertTrue(prefs.edit().putString("probe_profiles", future).commit());
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.get(context, "Future"));
        assertThrows(IllegalArgumentException.class,
                () -> ProbeProfiles.save(context, "Future", ProbeProfiles.current()));
        assertEquals(future, prefs.getString("probe_profiles", ""));
    }

    @Test
    public void malformedProfileShapeAndIdsAreRejectedWithoutApplying() throws Exception {
        ProbeProfiles.save(context, "Probe", ProbeProfiles.current());
        JSONObject all = new JSONObject(prefs.getString("probe_profiles", ""));
        JSONObject p = all.getJSONObject("Probe");
        p.put("base", new JSONArray("[0.1,0.2,0.3]"));
        assertTrue(prefs.edit().putString("probe_profiles", all.toString()).commit());
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.get(context, "Probe"));
        p.put("base", new JSONArray("[0.1,0.2,0.3,0.4]"));
        p.put("id", "not-a-uuid");
        assertTrue(prefs.edit().putString("probe_profiles", all.toString()).commit());
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.get(context, "Probe"));
        assertArrayEquals(phoneFrequencies, Constants.f2);
    }

    @Test
    public void resetRestoresKassDefaultsWithoutDeletingPresetsOrOtherSettings() throws Exception {
        ProbeProfiles.save(context, "Probe", alternateSetup());
        String saved = prefs.getString("probe_profiles", "");
        ProbeProfiles.apply(context, "Probe", ProbeProfiles.get(context, "Probe"));
        prefs.edit().putInt("constantToneLength", 4).putInt("reminderFrequency", 2).commit();
        ProbeProfiles.applyDefaults(context);
        assertArrayEquals(phoneFrequencies, Constants.f2);
        assertArrayEquals(new float[]{.15f, .45f, .45f, .50f}, ProbeProfiles.current().base, 0);
        assertArrayEquals(new float[]{1, 1, 1, 1}, ProbeProfiles.current().f1v, 0);
        assertArrayEquals(new float[]{.2f, .9f, 1, 1}, ProbeProfiles.current().f2v, 0);
        assertFalse(Constants.ONLY_LEFT);
        assertFalse(prefs.contains("f2Value_0"));
        assertFalse(prefs.contains("volumeValue_0"));
        assertFalse(prefs.contains("onlyLeft"));
        assertEquals(saved, prefs.getString("probe_profiles", ""));
        assertEquals(4, prefs.getInt("constantToneLength", 0));
        assertEquals(2, prefs.getInt("reminderFrequency", 0));
        assertEquals(137, Constants.SEAL_CHECK_THRESH);
        assertEquals(ProbeProfiles.defaultsName(), ProbeProfiles.activeLabel(context));
    }

    @Test
    public void manualFrequencyEditAndReloadUseTheSamePersistedPerBandGains() {
        ProbeProfiles.save(context, "Probe", ProbeProfiles.current());
        Constants.setF2(context, 0, 2000);
        assertEquals("Custom", ProbeProfiles.activeLabel(context));
        assertEquals(2000, prefs.getInt("f2Value_0", 0));
        assertFalse(Constants.vol1Lookup.containsKey(1900));
        assertEquals(.15f, Constants.vol1Lookup.get(2000), 0);
        assertEquals(.2f, Constants.vol3Lookup.get(2000), 0);
        assertArrayEquals(phoneFrequencies, Constants.f2Defaults);
        assertEquals(137, Constants.SEAL_CHECK_THRESH);
        Map<String, ?> before = prefs.getAll();
        assertThrows(IllegalArgumentException.class, () -> Constants.setF2(context, 0, 0));
        assertEquals(before, prefs.getAll());
        assertEquals(2000, Constants.f2[0]);
    }

    @Test
    public void preservesReferenceAndUnknownMetadataButDropsReferencesWhenSetupChanges() throws Exception {
        ProbeProfiles.Profile p = ProbeProfiles.current();
        p.refF1 = new double[]{120, 121, 122, 123};
        p.refF2Open = new double[]{110, 111, 112, 113};
        ProbeProfiles.save(context, "Probe", p);
        JSONObject all = new JSONObject(prefs.getString("probe_profiles", ""));
        all.getJSONObject("Probe").put("note", "Keep this metadata");
        prefs.edit().putString("probe_profiles", all.toString()).commit();
        ProbeProfiles.save(context, "Probe", ProbeProfiles.current());
        assertArrayEquals(p.refF1, ProbeProfiles.get(context, "Probe").refF1, 0);
        assertArrayEquals(p.refF2Open, ProbeProfiles.get(context, "Probe").refF2Open, 0);
        assertEquals("Keep this metadata", stored("Probe").getString("note"));
        ProbeProfiles.Profile edited = ProbeProfiles.get(context, "Probe");
        edited.base[0] = .25f;
        String id = edited.id;
        ProbeProfiles.save(context, "Probe", edited);
        assertEquals(id, ProbeProfiles.get(context, "Probe").id);
        assertFalse(stored("Probe").has("refF1"));
        assertFalse(stored("Probe").has("refF2Open"));
        assertEquals("Keep this metadata", stored("Probe").getString("note"));
    }

    @Test
    public void deletionAndManualChangesDoNotMislabelCurrentValuesAsPhoneDefaults() {
        ProbeProfiles.save(context, "Probe", ProbeProfiles.current());
        Constants.vol1Lookup.put(1900, .25f);
        assertEquals("Custom", ProbeProfiles.activeLabel(context));
        ProbeProfiles.delete(context, "Probe");
        assertEquals("Custom", ProbeProfiles.activeLabel(context));
        assertEquals(.25f, Constants.vol1Lookup.get(1900), 0);
        assertThrows(IllegalArgumentException.class, () -> ProbeProfiles.delete(context, ProbeProfiles.defaultsName()));
    }

    @Test
    public void presetsCannotChangeWhileProbeOrMeasurementIsActive() {
        ProbeProfiles.Profile p = alternateSetup();
        Constants.CheckProbeFragment = new CheckProbeFragment();
        Constants.CheckProbeFragment.isDrawing = true;
        Map<String, ?> before = prefs.getAll();
        assertThrows(IllegalStateException.class, () -> ProbeProfiles.apply(context, "Probe", p));
        assertThrows(IllegalStateException.class, () -> ProbeProfiles.applyDefaults(context));
        assertThrows(IllegalStateException.class, () -> ProbeProfiles.save(context, "Probe", p));
        Constants.CheckProbeFragment.isDrawing = false;
        Constants.MeasureFragment = new MeasureFragment();
        Constants.MeasureFragment.measure = new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... values) {
                return null;
            }
        };
        assertThrows(IllegalStateException.class, () -> ProbeProfiles.apply(context, "Probe", p));
        assertEquals(before, prefs.getAll());
        assertArrayEquals(phoneFrequencies, Constants.f2);
    }

    @Test
    public void probeLayoutRetainsExistingEditorAndAddsOnlyPresetActions() {
        View view = LayoutInflater.from(context).inflate(R.layout.probe_check, null);
        for (int id : new int[]{R.id.profileSave, R.id.profileLoad, R.id.profileReset,
                R.id.profileStatus, R.id.f2Value, R.id.volumeValue, R.id.volumeF1v,
                R.id.volumeF2v, R.id.buttonDraw, R.id.surfaceView}) {
            assertNotNull(view.findViewById(id));
        }
        assertEquals(4, ((RadioGroup) view.findViewById(R.id.idRadioGroup)).getChildCount());
    }

    private ProbeProfiles.Profile alternateSetup() {
        ProbeProfiles.Profile p = ProbeProfiles.current();
        p.f2 = new int[]{2016, 2953, 3985, 4969};
        p.base = new float[]{.12f, .3f, .4f, .5f};
        p.f1v = new float[]{.6f, .7f, .8f, .9f};
        p.f2v = new float[]{.2f, .3f, .4f, .5f};
        p.onlyLeft = true;
        return p;
    }

    private JSONObject stored(String name) throws Exception {
        return new JSONObject(prefs.getString("probe_profiles", "")).getJSONObject(name);
    }
}
