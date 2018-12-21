package com.google.audioworker.utils.signalproc;

import android.util.SparseArray;

import com.google.audioworker.utils.Constants;

public class SinusoidalGenerator {
    private final static String TAG = Constants.packageTag("SinusoidalGenerator");
    private double phaseOffset;

    static public class ModelInfo {
        public double analogMagnitude;
        public double analogFrequency;

        public static final int INTERP_STEP = 0;
        public static final int INTERP_LINEAR = 1;

        public ModelInfo() {
            this(0, 0);
        }

        public ModelInfo(double mag, double freq) {
            analogMagnitude = mag;
            analogFrequency = freq;
        }
    }

    public SinusoidalGenerator() {
        this(0);
    }

    public SinusoidalGenerator(double initPhase) {
        phaseOffset = initPhase;
    }

    public void render(double[] dest, SparseArray<ModelInfo> signalInfo, double samplingFreq) {
        render(dest, signalInfo, samplingFreq, ModelInfo.INTERP_LINEAR);
    }

    public void render(double[] dest, SparseArray<ModelInfo> signalInfo, double samplingFreq, int interp) {
        if (signalInfo.size() == 0) {
            for (int i = 0; i < dest.length; i++)
                dest[i] = 0;
            return;
        }

        for (int i = 0; i < dest.length; i++) {
            ModelInfo info = interpInfo(signalInfo, i, interp);
            dest[i] = info.analogMagnitude * Math.cos(phaseOffset);
            phaseOffset += (2*Math.PI * info.analogFrequency) / samplingFreq;
        }
    }

    private ModelInfo interpInfo(SparseArray<ModelInfo> signalInfo, int idx, int interp) {
        switch (interp) {
            case ModelInfo.INTERP_LINEAR: {
                int from = idx;
                for (int j = signalInfo.size()-1; j >= 0; j--) {
                    if (idx == signalInfo.keyAt(j))
                        return signalInfo.get(idx);
                    if (idx > signalInfo.keyAt(j)) {
                        from = signalInfo.keyAt(j);
                        break;
                    }
                }
                int to = from;
                for (int j = signalInfo.indexOfKey(from); j < signalInfo.size(); j++) {
                    if (idx == signalInfo.keyAt(j))
                        return signalInfo.get(idx);
                    if (idx < signalInfo.keyAt(j)) {
                        to = signalInfo.keyAt(j);
                        break;
                    }
                }
                if (from == to) {
                    return signalInfo.get(from);
                }
                ModelInfo info1 = signalInfo.get(from);
                ModelInfo info2 = signalInfo.get(to);
                ModelInfo info = new ModelInfo();
                double weight = (double) (idx - from) / (to - from);
                info.analogFrequency = info1.analogFrequency + (info2.analogFrequency - info1.analogFrequency) * weight;
                info.analogMagnitude = info1.analogMagnitude + (info2.analogMagnitude - info1.analogMagnitude) * weight;

                return info;
            }
            case ModelInfo.INTERP_STEP:
            default: {
                ModelInfo info = new ModelInfo();
                for (int i = signalInfo.size()-1; i >= 0; i--) {
                    if (idx >= signalInfo.keyAt(i))
                        return signalInfo.get(signalInfo.keyAt(i));
                }
                return info;
            }
        }
    }
}
