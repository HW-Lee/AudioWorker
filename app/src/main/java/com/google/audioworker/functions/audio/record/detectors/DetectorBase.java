package com.google.audioworker.functions.audio.record.detectors;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;

import com.google.audioworker.functions.audio.record.RecordStartFunction;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class DetectorBase {
    public abstract static class Target {
        protected int id;

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public abstract JSONObject toJson();
    }

    public interface Visualizable {
        <T extends View & DetectionListener> T getVisualizedView(
                Context ctx, String token, DetectorBase detector);
    }

    public interface DetectionListener {
        void onTargetDetected(DetectorBase detector, SparseArray<? extends Target> targets);
    }

    public abstract Target getTargetById(int id);

    public abstract void registerTarget(Target target);

    public abstract void feed(List<? extends Double>[] data);

    public abstract boolean parseParameters(String params);

    public abstract boolean setDetectorParameters(String params);

    public abstract JSONObject getDetectorParameters();

    public abstract String getHandle();

    public abstract String getInfo();

    public abstract void notifySettingChanged();

    public abstract void release();

    protected final ArrayList<WeakReference<DetectionListener>> mListeners = new ArrayList<>();
    protected boolean isValid = true;
    protected RecordStartFunction _function;

    public DetectorBase(DetectionListener l, RecordStartFunction function) {
        this(l, function, null);
    }

    public DetectorBase(DetectionListener l, RecordStartFunction function, String params) {
        if (l == null) throw new IllegalArgumentException("The listener cannot be null");

        _function = function;

        if (params != null && !parseParameters(params)) {
            isValid = false;
        }

        registerDetectionListener(l);
    }

    public RecordStartFunction getStartFunction() {
        return _function;
    }

    public void updateStartFunction(RecordStartFunction function) {
        _function = function;
        notifySettingChanged();
    }

    public void registerDetectionListener(DetectionListener l) {
        if (l == null) return;

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
            if (ref.get() != null) ref.get().onTargetDetected(this, targets);
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public static DetectorBase getDetectorByClassName(String className) {
        return getDetectorByClassName(className, null);
    }

    public static DetectorBase getDetectorByClassName(
            String className, RecordStartFunction function) {
        return getDetectorByClassName(
                className,
                new DetectorBase.DetectionListener() {
                    @Override
                    public void onTargetDetected(
                            DetectorBase detector, SparseArray<? extends Target> targets) {}
                },
                function,
                null);
    }

    public static DetectorBase getDetectorByClassName(
            String className, DetectionListener l, RecordStartFunction function, String params) {
        Class[] types = {
            DetectorBase.DetectionListener.class, RecordStartFunction.class, String.class
        };
        try {
            Object obj =
                    Class.forName(className).getConstructor(types).newInstance(l, function, params);
            if (obj instanceof DetectorBase) return (DetectorBase) obj;
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | java.lang.InstantiationException
                | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
