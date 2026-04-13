package com.example.dpoae;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

public class MyValueFormatter extends ValueFormatter {
    public MyValueFormatter() {
        super();
    }

    @Override
    public String getFormattedValue(float value) {
        return new DecimalFormat("##").format(value);
    }
}
