package com.google.audioworker.functions.audio.record.detectors;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
    public interface Visualizable {
        View getVisualizedView(Context ctx, String token, DetectorBase detector);
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

    protected final ArrayList<WeakReference<DetectionListener>> mListeners = new ArrayList<>();
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

        registerDetectionListener(l);
    }

    public void registerDetectionListener(DetectionListener l) {
        if (l == null)
            return;

        synchronized (mListeners) {
            ArrayList<WeakReference<DetectionListener>> removes = new ArrayList<>();
            for (WeakReference<DetectionListener> ref : mListeners) {
                if (ref.get() == null) {
                    removes.add(ref);
                    continue;
                }
                if (ref.get() == l) {
                    mListeners.removeAll(removes);
                    return;
                }
            }

            mListeners.removeAll(removes);
            mListeners.add(new WeakReference<>(l));
        }
    }

    public void unregisterDetectionListener(DetectionListener l) {
        synchronized (mListeners) {
            for (WeakReference<DetectionListener> ref : mListeners) {
                if (ref.get() == l) {
                    mListeners.remove(ref);
                    return;
                }
            }
        }
    }

    protected void broadcastTargetDetected(SparseArray<? extends Target> targets) {
        for (WeakReference<DetectionListener> ref : mListeners) {
            if (ref.get() != null)
                ref.get().onTargetDetected(targets);
        }
    }

    public boolean isValid() {
        return isValid;
    }

    static public DetectorBase getDetectorByClassName(String className) {
        return getDetectorByClassName(className, new DetectorBase.DetectionListener() {
            @Override
            public void onTargetDetected(SparseArray<? extends DetectorBase.Target> targets) {
            }
        }, null);
    }

    static public DetectorBase getDetectorByClassName(String className, DetectionListener l, String params) {
        Class[] types = {DetectorBase.DetectionListener.class, String.class};
        try {
            Object obj = Class.forName(className).getConstructor(types).newInstance(l, params);
            if (obj instanceof DetectorBase)
                return (DetectorBase) obj;
        } catch (ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | java.lang.InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
