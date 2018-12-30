package com.google.audioworker.functions.audio.record.detectors;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.ds.CircularArray;
import com.google.audioworker.utils.signalproc.FFT;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ToneDetector extends DetectorBase implements WorkerFunction.Parameterizable {
    private final static String TAG = Constants.packageTag("ToneDetector");

    private int mSamplingFreq;
    private int mProcessFrameMillis = Constants.Detectors.ToneDetector.Config.PROCESS_FRAME_MILLIS;
    private CircularArray<Double> mBuffer;
    final private ArrayList<Target> mTargets;

    private final static String ATTR_TARGETS = Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ;
    private final static String ATTR_CLEAR_TARGETS = Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS;

    private final WorkerFunction.Parameter<String> PARAM_TARGETS = new WorkerFunction.Parameter<>(ATTR_TARGETS, false, "[]");
    private final WorkerFunction.Parameter<Boolean> PARAM_CLEAR_TARGETS = new WorkerFunction.Parameter<>(ATTR_CLEAR_TARGETS, false, false);

    @Override
    public WorkerFunction.Parameter[] getParameters() {
        return new WorkerFunction.Parameter[]{PARAM_TARGETS, PARAM_CLEAR_TARGETS};
    }

    @Override
    public void setParameter(String attr, Object value) {
        if (!isValueAccepted(attr, value))
            return;

        switch (attr) {
            case ATTR_TARGETS:
                PARAM_TARGETS.setValue("[" + value.toString() + "]");
                return;
            case ATTR_CLEAR_TARGETS:
                PARAM_CLEAR_TARGETS.setValue(Boolean.valueOf(value.toString()));
                return;

            default:
                break;
        }
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        switch (attr) {
            case ATTR_TARGETS:
                return checkTargetString(value.toString());
            case ATTR_CLEAR_TARGETS:
                return true;
        }
        return false;
    }

    private boolean checkTargetString(String s) {
        try {
            JSONArray jsonArray = new JSONArray("[" + s + "]");
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getDouble(i) <= 0)
                    return false;
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class Target extends DetectorBase.Target {
        private float targetFreq;

        Target(float targetFreq) {
            this.targetFreq = targetFreq;
        }

        @Override
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put(Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ, targetFreq);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }

    public ToneDetector(DetectionListener l, String params) {
        super(l, params);
        int numSamples = mProcessFrameMillis * mSamplingFreq / 1000;
        mBuffer = new CircularArray<>(numSamples);
        for (int i = 0; i < numSamples; i++)
            mBuffer.add(0.);
        mTargets = new ArrayList<>();
        setDetectorParameters(params);
    }

    @Override
    public Target getTargetById(int id) {
        if (id < mTargets.size())
            return mTargets.get(id);

        return null;
    }

    @Override
    public void registerTarget(DetectorBase.Target target) {
        if (target instanceof Target) {
            synchronized (mTargets) {
                mTargets.add((Target) target);
            }
        } else {
            Log.w(TAG, "The invalid registered target");
        }
    }

    @Override
    public void feed(List<? extends Double>[] data) {
        mBuffer.addAll(data[0]);

        final double[] signal = new double[mBuffer.size()];
        for (int i = 0; i < signal.length; i++)
            signal[i] = mBuffer.get(i);

        new Thread(new Runnable() {
            @Override
            public void run() {
                process(signal);
            }
        }).start();
    }

    @Override
    public boolean parseParameters(String params) {
        if (params == null)
            return true;
        try {
            JSONObject jsonParams = new JSONObject(params);
            mSamplingFreq = jsonParams.getInt(Constants.Detectors.ToneDetector.PARAM_FS);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean setDetectorParameters(String params) {
        if (params == null)
            return true;

        Log.d(TAG, "setParameters(" + params + ")");
        boolean success;
        try {
            JSONObject jsonParams = new JSONObject(params);
            processParamsIfClearTargets(jsonParams);
            processParamsOfTargetFreqs(jsonParams);
            success = true;
        } catch (JSONException e) {
            e.printStackTrace();
            success = false;
        }

        syncParameters();
        return success;
    }

    @Override
    public JSONObject getDetectorParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put(ATTR_CLEAR_TARGETS, PARAM_CLEAR_TARGETS.getValue());
            params.put(ATTR_TARGETS, new JSONArray(PARAM_TARGETS.getValue()));

            return params;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getHandle() {
        return super.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return getInfo();
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handle: ").append(getHandle()).append("\n");
        sb.append("Sampling Frequency: ").append(mSamplingFreq).append(" Hz\n");
        sb.append("Process Frame Size: ").append(mProcessFrameMillis).append(" ms\n");
        synchronized (mTargets) {
            sb.append("Targets: ").append(mTargets.size()).append(" target(s)").append("\n");
            for (int i = 0; i < mTargets.size(); i++)
                sb.append("\t").append(i).append(": ").append(mTargets.get(i).toJson()).append("\n");
        }
        return sb.toString();
    }

    private void processParamsIfClearTargets(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS) ||
                !jsonParams.getBoolean(Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS)) {
            PARAM_CLEAR_TARGETS.setValue(false);
            return;
        }

        PARAM_CLEAR_TARGETS.setValue(true);
    }

    private void processParamsOfTargetFreqs(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ)) {
            PARAM_TARGETS.setValue("[]");
            return;
        }

        PARAM_TARGETS.setValue(jsonParams.getJSONArray(Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ).toString());
    }

    private void syncParameters() {
        if (PARAM_CLEAR_TARGETS.getValue()) {
            synchronized (mTargets) {
                mTargets.clear();
            }
        }

        try {
            JSONArray targetFreqs = new JSONArray(PARAM_TARGETS.getValue());
            for (int i = 0; i < targetFreqs.length(); i++) {
                Target t = new Target((float) targetFreqs.getDouble(i));
                Target dummy = null;
                synchronized (mTargets) {
                    for (int j = 0; j < mTargets.size(); j++) {
                        dummy = mTargets.get(j);
                        if (dummy.targetFreq == t.targetFreq)
                            break;
                    }
                    if (dummy == null || dummy.targetFreq != t.targetFreq) {
                        mTargets.add(t);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void process(double[] signal) {
        synchronized (mTargets) {
            if (mTargets.size() == 0)
                return;
        }

        SparseArray<Target> targets = new SparseArray<>();
        double[] spectrum = FFT.transformAbs(signal);

        if (targets.size() > 0) {
            mListener.onTargetDetected(targets);
        }
    }
}
