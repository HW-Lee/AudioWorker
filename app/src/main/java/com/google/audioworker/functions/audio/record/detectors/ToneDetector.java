package com.google.audioworker.functions.audio.record.detectors;

import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.utils.Constants;

import java.util.List;

public class ToneDetector extends DetectorBase {
    private final static String TAG = Constants.packageTag("ToneDetector");

    private int mSamplingFreq;

    public static class Target extends DetectorBase.Target {
    }

    public ToneDetector(DetectionListener l, int fs) {
        super(l);
        mSamplingFreq = fs;
    }

    @Override
    public Target getTargetById(int id) {
        return null;
    }

    @Override
    public void registerTarget(DetectorBase.Target target) {
    }

    @Override
    public void feed(List<? extends Number> data) {
    }
}
