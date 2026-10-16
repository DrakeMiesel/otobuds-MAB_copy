package com.example.dpoae;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Named probe settings, adapted from viyer's settings-volume-levels branch.
 * Retains its name-keyed JSON and preference keys; reference readings are
 * optional metadata, not evidence that a preset is calibrated.
 */
final class ProbeProfiles {
    private static final String KEY = "probe_profiles";
    private static final String ACTIVE = "active_probe";
    private static final int FORMAT_VERSION = 1;

    static final class Profile {
        String id;
        int[] f2 = new int[4];
        float[] base = new float[4], f1v = new float[4], f2v = new float[4];
        Boolean onlyLeft; // Legacy presets did not record the channel mode.
        double[] refF1, refF2, refF1Open, refF2Open;
        int refLengthSec = 6;
        long savedAt;

        double[] refF1(boolean openAir) {
            return openAir ? refF1Open : refF1;
        }

        double[] refF2(boolean openAir) {
            return openAir ? refF2Open : refF2;
        }
    }

    private ProbeProfiles() {
    }

    static String defaultsName() {
        return "Phone defaults (" + Constants.phone + ")";
    }

    static List<String> names(Context c) {
        List<String> out = new ArrayList<>();
        JSONObject all = all(c);
        for (java.util.Iterator<String> it = all.keys(); it.hasNext(); ) out.add(it.next());
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        out.add(0, defaultsName());
        return out;
    }

    static String active(Context c) {
        return prefs(c).getString(ACTIVE, defaultsName());
    }

    static String activeLabel(Context c) {
        String name = active(c);
        if (name.isEmpty()) return "Custom";
        Profile saved = name.equals(defaultsName()) ? null : get(c, name);
        if ((!name.equals(defaultsName()) && saved == null) || isModified(c, name, saved)) {
            return "Custom";
        }
        return name;
    }

    /** The settings currently in use, not a copy of a named preset. */
    static Profile current() {
        Profile p = new Profile();
        for (int i = 0; i < 4; i++) {
            int f2 = Constants.octaves.get(i);
            int f1 = Constants.freqLookup.get(f2);
            p.f2[i] = f2;
            p.base[i] = Constants.vol1Lookup.get(f2);
            p.f1v[i] = Constants.vol3Lookup.get(f1);
            p.f2v[i] = Constants.vol3Lookup.get(f2);
        }
        p.onlyLeft = Constants.ONLY_LEFT;
        return p;
    }

    static boolean isModified(Context c, String name, Profile saved) {
        if (saved == null) {
            SharedPreferences sp = prefs(c);
            for (int i = 0; i < 4; i++) {
                if (sp.contains("f2Value_" + i) || sp.contains("volumeValue_" + i)
                        || sp.contains("volumeF1te_" + i) || sp.contains("volumeF2te_" + i)) return true;
            }
            return Constants.ONLY_LEFT;
        }
        return differs(current(), saved);
    }

    private static boolean differs(Profile current, Profile saved) {
        for (int i = 0; i < 4; i++) {
            if (current.f2[i] != saved.f2[i] || Math.abs(current.base[i] - saved.base[i]) > 1e-4
                    || Math.abs(current.f1v[i] - saved.f1v[i]) > 1e-4
                    || Math.abs(current.f2v[i] - saved.f2v[i]) > 1e-4) return true;
        }
        boolean onlyLeft = current.onlyLeft == null ? Constants.ONLY_LEFT : current.onlyLeft;
        return saved.onlyLeft != null && saved.onlyLeft != onlyLeft;
    }

    static Profile get(Context c, String name) {
        JSONObject all = all(c);
        if (!all.has(name)) return null;
        try {
            JSONObject o = all.getJSONObject(name);
            if (o.has("formatVersion") && (!(o.get("formatVersion") instanceof Integer)
                    || o.getInt("formatVersion") != FORMAT_VERSION)) {
                throw new IllegalArgumentException("Unsupported preset format: " + name);
            }
            Profile p = new Profile();
            if (o.has("id")) {
                p.id = o.getString("id");
                if (!UUID.fromString(p.id).toString().equalsIgnoreCase(p.id)) {
                    throw new IllegalArgumentException("Invalid preset ID: " + name);
                }
            }
            String[] keys = {"f2", "base", "f1v", "f2v"};
            for (String key : keys) {
                if (o.getJSONArray(key).length() != 4) {
                    throw new IllegalArgumentException("Preset must have four bands: " + name);
                }
            }
            for (int i = 0; i < 4; i++) {
                Object frequency = o.getJSONArray("f2").get(i);
                if (!(frequency instanceof Integer)) {
                    throw new IllegalArgumentException("Preset frequencies must be integer Hz");
                }
                p.f2[i] = (Integer) frequency;
                p.base[i] = (float) o.getJSONArray("base").getDouble(i);
                p.f1v[i] = (float) o.getJSONArray("f1v").getDouble(i);
                p.f2v[i] = (float) o.getJSONArray("f2v").getDouble(i);
            }
            if (o.has("onlyLeft")) {
                if (!(o.get("onlyLeft") instanceof Boolean)) {
                    throw new IllegalArgumentException("Invalid preset channel mode");
                }
                p.onlyLeft = o.getBoolean("onlyLeft");
            }
            p.refF1 = readArray(o, "refF1");
            p.refF2 = readArray(o, "refF2");
            p.refF1Open = readArray(o, "refF1Open");
            p.refF2Open = readArray(o, "refF2Open");
            p.refLengthSec = o.optInt("refLengthSec", 6);
            p.savedAt = o.optLong("savedAt");
            validate(p);
            return p;
        } catch (JSONException e) {
            throw new IllegalStateException("Cannot read preset: " + name, e);
        }
    }

    static void save(Context c, String name, Profile p) {
        requireIdle();
        if (name == null || name.trim().isEmpty() || name.equals(defaultsName())) {
            throw new IllegalArgumentException("Enter a preset name other than the phone defaults");
        }
        validate(p);
        Profile old = get(c, name);
        String id = old != null && old.id != null ? old.id : UUID.randomUUID().toString();
        boolean changed = old != null && differs(p, old);
        boolean onlyLeft = p.onlyLeft == null ? Constants.ONLY_LEFT : p.onlyLeft;
        try {
            JSONObject all = all(c);
            JSONObject o = old == null ? new JSONObject()
                    : new JSONObject(all.getJSONObject(name).toString());
            JSONArray f2 = new JSONArray(), base = new JSONArray(), f1v = new JSONArray(), f2v = new JSONArray();
            for (int i = 0; i < 4; i++) {
                f2.put(p.f2[i]);
                base.put((double) p.base[i]);
                f1v.put((double) p.f1v[i]);
                f2v.put((double) p.f2v[i]);
            }
            long savedAt = System.currentTimeMillis();
            o.put("formatVersion", FORMAT_VERSION).put("id", id)
                    .put("f2", f2).put("base", base).put("f1v", f1v).put("f2v", f2v)
                    .put("onlyLeft", onlyLeft)
                    .put("savedAt", savedAt);
            String[] refs = {"refF1", "refF2", "refF1Open", "refF2Open"};
            double[][] values = {p.refF1, p.refF2, p.refF1Open, p.refF2Open};
            for (int i = 0; i < refs.length; i++) {
                if (changed) o.remove(refs[i]);
                else if (values[i] != null) o.put(refs[i], new JSONArray(values[i]));
            }
            if (changed) o.remove("refLengthSec");
            else o.put("refLengthSec", p.refLengthSec);
            all.put(name, o);
            commit(prefs(c).edit().putString(KEY, all.toString()).putString(ACTIVE, name));
            p.id = id;
            p.savedAt = savedAt;
            p.onlyLeft = onlyLeft;
        } catch (JSONException e) {
            throw new IllegalStateException("Cannot save preset: " + name, e);
        }
    }

    static void delete(Context c, String name) {
        requireIdle();
        if (name.equals(defaultsName())) {
            throw new IllegalArgumentException("The phone defaults cannot be deleted");
        }
        JSONObject all = all(c);
        all.remove(name);
        SharedPreferences.Editor e = prefs(c).edit().putString(KEY, all.toString());
        if (name.equals(active(c))) e.putString(ACTIVE, "");
        commit(e);
    }

    /** Validate first, then persist the complete setup in one preference edit. */
    static void apply(Context c, String name, Profile p) {
        requireIdle();
        validate(p);
        boolean onlyLeft = p.onlyLeft == null ? Constants.ONLY_LEFT : p.onlyLeft;
        SharedPreferences.Editor e = prefs(c).edit();
        for (int i = 0; i < 4; i++) {
            e.putInt("f2Value_" + i, p.f2[i]);
            e.putFloat("volumeValue_" + i, p.base[i]);
            e.putFloat("volumeF1te_" + i, p.f1v[i]);
            e.putFloat("volumeF2te_" + i, p.f2v[i]);
        }
        commit(e.putBoolean("onlyLeft", onlyLeft).putString(ACTIVE, name));
        Constants.setProbeFrequencies(p.f2);
        reloadVolumes(c);
        Constants.ONLY_LEFT = onlyLeft;
    }

    static void applyDefaults(Context c) {
        requireIdle();
        SharedPreferences.Editor e = prefs(c).edit();
        for (int i = 0; i < 4; i++) {
            e.remove("f2Value_" + i).remove("volumeValue_" + i)
                    .remove("volumeF1te_" + i).remove("volumeF2te_" + i);
        }
        commit(e.remove("onlyLeft").putString(ACTIVE, defaultsName()));
        Constants.setProbeFrequencies(Constants.f2Defaults);
        reloadVolumes(c);
        Constants.ONLY_LEFT = false;
    }

    static void validate(Profile p) {
        if (p == null || p.f2 == null || p.base == null || p.f1v == null || p.f2v == null
                || p.f2.length != 4 || p.base.length != 4 || p.f1v.length != 4 || p.f2v.length != 4) {
            throw new IllegalArgumentException("A preset must contain four complete bands");
        }
        Set<Integer> primaryTones = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            int f2 = p.f2[i], f1 = (int) (f2 / 1.22);
            if (i > 0 && f2 <= p.f2[i - 1]) {
                throw new IllegalArgumentException("Preset frequencies must be distinct and increasing");
            }
            for (int tone : new int[]{f1, f2, 2 * f1 - f2, 2 * f2 - f1}) {
                if (tone < 202 || tone >= Constants.samplingRate / 2 - 202) {
                    throw new IllegalArgumentException("Preset tones and noise windows must fit below Nyquist");
                }
            }
            if (!primaryTones.add(f1) || !primaryTones.add(f2)) {
                throw new IllegalArgumentException("Preset primary frequencies must not overlap");
            }
            for (float gain : new float[]{p.base[i], p.f1v[i], p.f2v[i]}) {
                if (!Float.isFinite(gain) || gain < 0 || gain > 1) {
                    throw new IllegalArgumentException("Volumes must be finite values between 0 and 1");
                }
            }
            for (double[] reference : new double[][]{p.refF1, p.refF2, p.refF1Open, p.refF2Open}) {
                if (reference == null) continue;
                if (reference.length != 4) throw new IllegalArgumentException("Reference readings must have four bands");
                for (double level : reference) {
                    if (!Double.isFinite(level)) throw new IllegalArgumentException("Reference readings must be finite");
                }
            }
        }
    }

    static void requireIdle() {
        if (isBusy()) {
            throw new IllegalStateException("Stop the measurement or probe audio before changing presets");
        }
    }

    static boolean isBusy() {
        return Constants.testInProgress
                || (Constants.CheckProbeFragment != null && Constants.CheckProbeFragment.isDrawing)
                || (Constants.MeasureFragment != null && Constants.MeasureFragment.measure != null
                && Constants.MeasureFragment.measure.getStatus() != AsyncTask.Status.FINISHED);
    }

    static void reloadVolumes(Context c) {
        int seal = Constants.SEAL_CHECK_THRESH;
        Constants.populateVolume(c);
        Constants.SEAL_CHECK_THRESH = seal;
    }

    static void commit(SharedPreferences.Editor editor) {
        if (!editor.commit()) throw new IllegalStateException("Could not persist probe settings");
    }

    private static double[] readArray(JSONObject o, String key) throws JSONException {
        if (!o.has(key)) return null;
        JSONArray a = o.getJSONArray(key);
        if (a.length() != 4) throw new JSONException("Invalid reference array: " + key);
        double[] out = new double[4];
        for (int i = 0; i < 4; i++) {
            out[i] = a.getDouble(i);
            if (!Double.isFinite(out[i])) throw new JSONException("Non-finite reference: " + key);
        }
        return out;
    }

    private static JSONObject all(Context c) {
        try {
            return new JSONObject(prefs(c).getString(KEY, "{}"));
        } catch (JSONException | ClassCastException e) {
            throw new IllegalStateException("Saved presets are unreadable; stored data was not changed", e);
        }
    }

    private static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }
}
