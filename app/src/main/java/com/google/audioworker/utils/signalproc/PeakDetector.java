package com.google.audioworker.utils.signalproc;

import com.google.audioworker.utils.Constants;

import java.util.ArrayList;
import java.util.Collection;

public class PeakDetector {
    private static final String TAG = Constants.packageTag("PeakDetector");

    public static class QuadraticFeature {
        public double[] coeffs;
        public double[] y;

        public QuadraticFeature(double[] feature, Config config) {
            if (feature.length != 3 + config.numPoints) return;

            coeffs = new double[3];
            y = new double[config.numPoints];

            System.arraycopy(feature, 0, coeffs, 0, 3);
            System.arraycopy(feature, 3, y, 0, config.numPoints);
        }

        public double dataDensity() {
            double normsq = 0;
            for (double v : y) normsq += (v * v);
            normsq /= y.length;

            return Math.sqrt(normsq);
        }

        public double dataVariance() {
            double mean = 0;
            for (double v : y) mean += v;
            mean /= y.length;

            return Math.pow(dataDensity(), 2) - Math.pow(mean, 2);
        }
    }

    public static class Config {
        double[] targetFreqs;
        int numPoints;
        int step;
        double fstep;
        double minNormFactor;

        public static class Builder {
            ArrayList<Double> targetFreqs;
            int numPoints;
            int step;
            double fstep;
            double minNormFactor;

            public Builder() {
                targetFreqs = new ArrayList<>();
                numPoints = 1;
                step = 1;
                fstep = -1;
                minNormFactor = -1;
            }

            public Builder withStepFreq(double fstep) {
                this.fstep = fstep;
                return this;
            }

            public Builder withNumPoints(int numPoints) {
                this.numPoints = numPoints;
                return this;
            }

            public Builder withStep(int step) {
                this.step = step;
                return this;
            }

            public Builder addTargetFreq(double target) {
                if (this.targetFreqs.contains(target)) return this;

                this.targetFreqs.add(target);
                return this;
            }

            public Builder addTargetFreqs(Collection<? extends Double> targets) {
                Builder b = this;
                for (Double v : targets) b = b.addTargetFreq(v);

                return b;
            }

            public Builder withMinNormFactor(double minNormFactor) {
                this.minNormFactor = minNormFactor;
                return this;
            }

            public Config build() {
                Config config = new Config();
                config.targetFreqs = new double[targetFreqs.size()];
                for (int i = 0; i < config.targetFreqs.length; i++)
                    config.targetFreqs[i] = targetFreqs.get(i);
                config.numPoints = numPoints;
                config.step = step;
                config.fstep = fstep;
                config.minNormFactor = minNormFactor;

                return config;
            }
        }
    }

    static {
        System.loadLibrary("native-peak-detector");
    }

    public static ArrayList<double[]> extractFeature(double[] data, Config config) {
        int ncols = config.targetFreqs.length;
        int nrows = 3 + config.numPoints;

        int[] targetIndices = new int[ncols];
        for (int i = 0; i < ncols; i++) {
            targetIndices[i] = (int) Math.round(config.targetFreqs[i] / config.fstep);
        }

        double[] nativeFeatures =
                PeakDetector.extractQuadFeature(
                        data, targetIndices, config.minNormFactor, config.numPoints, config.step);
        ArrayList<double[]> features = new ArrayList<>(ncols);

        for (int i = 0; i < ncols; i++) {
            features.add(new double[nrows]);
            for (int j = 0; j < nrows; j++) {
                features.get(i)[j] = nativeFeatures[j * ncols + i];
            }
        }

        return features;
    }

    public static native String getVersion();

    private static native double[] extractQuadFeature(
            double[] data, int[] targetIndices, double minNormFactor, int numPoints, int step);
}
