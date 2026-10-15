package com.example.dpoae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultScoringTest {
    @ParameterizedTest
    @ValueSource(doubles = {4, 5, 6, 10, 11})
    void usesUnroundedSnrAtEachConfiguredThreshold(double threshold) {
        assertFalse(ResultScoring.passesBand(threshold - 0.5, threshold));
        assertFalse(ResultScoring.passesBand(Math.nextDown(threshold), threshold));
        assertTrue(ResultScoring.passesBand(threshold, threshold));
        assertTrue(ResultScoring.passesBand(Math.nextUp(threshold), threshold));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteSnrDoesNotPass(double snr) {
        assertFalse(ResultScoring.passesBand(snr, 4));
    }

    @Test
    void vikramBoundaryExampleDoesNotBecomeAPassThroughRounding() {
        assertFalse(ResultScoring.passesBand(3.65, 4));
        assertTrue(ResultScoring.passesBand(7.16, 6));
    }

    @Test
    void olderAgeNearThresholdRecordingDoesNotBecomeAnOverallPass() {
        boolean[] passed = score(new double[]{5.5, 5.5, 10.5, 0},
                new double[]{6, 6, 11, 10});

        assertArrayEquals(new boolean[]{false, false, false, false}, passed);
        assertEquals(0, ResultScoring.countPassingBands(passed));
    }

    @Test
    void threeBandsAtOlderAgeThresholdsStillPass() {
        boolean[] passed = score(new double[]{6, 6, 11, Double.NaN},
                new double[]{6, 6, 11, 10});

        assertArrayEquals(new boolean[]{true, true, true, false}, passed);
        assertEquals(3, ResultScoring.countPassingBands(passed));
    }

    @Test
    void infantThresholdsUseTheSameRuleAndUnmeasuredBandsDoNotCount() {
        double[] thresholds = {5, 4, 4, 4};
        boolean[] passed = score(new double[]{5, 4, 4, Double.NaN}, thresholds);
        assertEquals(3, ResultScoring.countPassingBands(passed));

        passed = score(new double[]{5, 3.65, 4, Double.NaN}, thresholds);
        assertArrayEquals(new boolean[]{true, false, true, false}, passed);
        assertEquals(2, ResultScoring.countPassingBands(passed));
    }

    @Test
    void noMeasuredBandsCannotProducePassingBands() {
        assertEquals(0, ResultScoring.countPassingBands(new boolean[4]));
        boolean[] invalid = score(new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN},
                new double[]{6, 6, 11, 10});
        assertEquals(0, ResultScoring.countPassingBands(invalid));
    }

    private static boolean[] score(double[] snrs, double[] thresholds) {
        boolean[] passed = new boolean[snrs.length];
        for (int i = 0; i < snrs.length; i++) {
            passed[i] = ResultScoring.passesBand(snrs[i], thresholds[i]);
        }
        return passed;
    }
}
