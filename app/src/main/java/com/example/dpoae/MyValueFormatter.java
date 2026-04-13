package com.example.dpoae;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class MyValueFormatter extends ValueFormatter {
    // When provided, appends a pass/fail symbol to each bar label so that
    // pass vs. refer is conveyed by text, not color alone (WCAG 1.4.1).
    private final boolean[] passFlags;

    public MyValueFormatter() {
        super();
        this.passFlags = null;
    }

    public MyValueFormatter(boolean[] passFlags) {
        super();
        this.passFlags = passFlags;
    }

    @Override
    public String getFormattedValue(float value) {
        return new DecimalFormat("##").format(value);
    }

    @Override
    public String getBarLabel(BarEntry barEntry) {
        String snr = new DecimalFormat("##").format(barEntry.getY());
        if (passFlags != null && Constants.octaves != null) {
            // Match the bar's X value (freq in kHz) to its index in octaves
            for (int i = 0; i < Constants.octaves.size() && i < passFlags.length; i++) {
                float freqKHz = Constants.octaves.get(i) / 1000f;
                if (Math.abs(barEntry.getX() - freqKHz) < 0.1f) {
                    return snr + (passFlags[i] ? " \u2713" : " \u2717");
                }
            }
        }
        return snr;
    }
}
