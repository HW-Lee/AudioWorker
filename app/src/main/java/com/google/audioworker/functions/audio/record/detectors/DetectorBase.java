package com.google.audioworker.functions.audio.record.detectors;

import android.util.SparseArray;

import java.util.List;

public abstract class DetectorBase {
    public static class Target {}
    interface DetectionListener {
        void onTargetDetected(SparseArray<Target> targets);
    }

    abstract public Target getTargetById(int id);
    abstract public void registerTarget(Target target);
    abstract public void feed(List<? extends Number> data);

    protected DetectionListener mListener;
    public DetectorBase(DetectionListener l) {
        if (l == null)
            throw new IllegalArgumentException("The listener cannot be null");

        mListener = l;
    }
}
