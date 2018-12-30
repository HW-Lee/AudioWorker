package com.google.audioworker.functions.audio.record.detectors;

import android.util.SparseArray;

import org.json.JSONObject;

import java.util.List;

public abstract class DetectorBase {
    public static abstract class Target {
        protected int id;
        public void setId(int id) {
            this.id = id;
        }
        public int getId() {
            return this.id;
        }

        abstract public JSONObject toJson();
    }
    public interface DetectionListener {
        void onTargetDetected(SparseArray<? extends Target> targets);
    }

    abstract public Target getTargetById(int id);
    abstract public void registerTarget(Target target);
    abstract public void feed(List<? extends Double>[] data);
    abstract public boolean parseParameters(String params);
    abstract public boolean setDetectorParameters(String params);
    abstract public JSONObject getDetectorParameters();
    abstract public String getHandle();
    abstract public String getInfo();

    protected DetectionListener mListener;
    protected boolean isValid = true;

    public DetectorBase(DetectionListener l) {
        this(l, null);
    }

    public DetectorBase(DetectionListener l, String params) {
        if (l == null)
            throw new IllegalArgumentException("The listener cannot be null");

        if (params != null && !parseParameters(params)) {
            isValid = false;
        }

        mListener = l;
    }

    public boolean isValid() {
        return isValid;
    }
}
