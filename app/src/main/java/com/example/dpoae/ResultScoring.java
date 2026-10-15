package com.example.dpoae;

final class ResultScoring {
    private ResultScoring() {
    }

    static boolean passesBand(double snr, double threshold) {
        return Double.isFinite(snr) && snr >= threshold;
    }

    static int countPassingBands(boolean[] passedBands) {
        int count = 0;
        for (boolean passed : passedBands) {
            if (passed) count++;
        }
        return count;
    }
}
