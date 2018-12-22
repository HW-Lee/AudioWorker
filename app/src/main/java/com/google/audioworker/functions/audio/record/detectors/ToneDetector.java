package com.google.audioworker.functions.audio.record.detectors;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.signalproc.FFT;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ToneDetector extends DetectorBase {
    private final static String TAG = Constants.packageTag("ToneDetector");

    private int mSamplingFreq;
    private int mProcessFrameMillis = Constants.DetectorConfig.ToneDetector.PROCESS_FRAME_MILLIS;
    private ArrayList<Double> mBuffer;
    final private ArrayList<Target> mTargets;

    public static class Target extends DetectorBase.Target {
        private float targetFreq;

        Target(float targetFreq) {
            this.targetFreq = targetFreq;
        }

        @Override
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put(Constants.DetectorConfig.ToneDetector.PARAM_TARGET_FREQ, targetFreq);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }

    public ToneDetector(DetectionListener l, String params) {
        super(l, params);
        int numSamples = mProcessFrameMillis * mSamplingFreq / 1000;
        mBuffer = new ArrayList<>(numSamples);
        for (int i = 0; i < numSamples; i++)
            mBuffer.add(0.);
        mTargets = new ArrayList<>();
        setParameters(params);
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
    public void feed(List<? extends Number> data) {
        while (data.size() > mBuffer.size()) {
            mBuffer.add(0.);
        }
        for (Number d : data) {
            mBuffer.add(Double.valueOf(d.toString()));
            mBuffer.remove(0);
        }

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
            mSamplingFreq = jsonParams.getInt(Constants.DetectorConfig.ToneDetector.PARAM_FS);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean setParameters(String params) {
        if (params == null)
            return true;

        Log.d(TAG, "setParameters(" + params + ")");
        try {
            JSONObject jsonParams = new JSONObject(params);
            processParamsIfClearTargets(jsonParams);
            processParamsOfTargetFreqs(jsonParams);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
        if (!jsonParams.has(Constants.DetectorConfig.ToneDetector.PARAM_CLEAR_TARGETS) ||
                !jsonParams.getBoolean(Constants.DetectorConfig.ToneDetector.PARAM_CLEAR_TARGETS))
            return;

        synchronized (mTargets) {
            mTargets.clear();
        }
    }

    private void processParamsOfTargetFreqs (JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.DetectorConfig.ToneDetector.PARAM_TARGET_FREQ))
            return;
        JSONArray targetFreqs = jsonParams.getJSONArray(Constants.DetectorConfig.ToneDetector.PARAM_TARGET_FREQ);

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
