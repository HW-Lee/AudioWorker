package com.google.audioworker.functions.audio.record.detectors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.ds.CircularArray;
import com.google.audioworker.utils.signalproc.FFT;
import com.google.audioworker.utils.signalproc.PeakDetector;
import com.google.audioworker.views.ToneDetectorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToneDetector extends VisualizableDetector implements WorkerFunction.Parameterizable {
    private final static String TAG = Constants.packageTag("ToneDetector");

    private int mNumChannels;
    private int mSamplingFreq;
    private ArrayList<CircularArray<Double>> mBuffers;

    private class TargetStorage {
        final ArrayList<Target> content = new ArrayList<>();
    }

    private TargetStorage mTargetStorage;
    private String mDumpFilePath;

    public final static String INFO_KEY_SAMPLING_FREQ = "Sampling Frequency";
    public final static String INFO_KEY_FRAME_SIZE = "Process Frame Size";
    public final static String INFO_KEY_TOLERANCE = "Tolerance (semitone)";
    public final static String INFO_KEY_HANDLE = "Handle";
    public final static String INFO_KEY_UNIT = "unit";
    public final static String INFO_KEY_TARGETS = "Targets";
    public final static String INFO_KEY_HISTORY = "Detection History";

    private final static String ATTR_TARGETS = Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ;
    private final static String ATTR_PROCESS_FRAME_MILLIS = Constants.Detectors.ToneDetector.PARAM_PROCESS_FRAME_MILLIS;
    private final static String ATTR_TOL_DIFF_SEMI = Constants.Detectors.ToneDetector.PARAM_TOL_DIFF_SEMI;
    private final static String ATTR_CLEAR_TARGETS = Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS;
    private final static String ATTR_DUMP_HISTORY = Constants.Detectors.ToneDetector.PARAM_DUMP_HISTORY;

    private WorkerFunction.Parameter<String> PARAM_TARGETS;
    private WorkerFunction.Parameter<Integer> PARAM_PROCESS_FRAME_MILLIS;
    private WorkerFunction.Parameter<Integer> PARAM_TOL_DIFF_SEMI;
    private WorkerFunction.Parameter<Boolean> PARAM_CLEAR_TARGETS;
    private WorkerFunction.Parameter<Boolean> PARAM_DUMP_HISTORY;

    @Override
    public String[] getAttributes() {
        initParameters();
        return new String[] {
                ATTR_TARGETS,
                ATTR_PROCESS_FRAME_MILLIS,
                ATTR_TOL_DIFF_SEMI,
                ATTR_CLEAR_TARGETS,
                ATTR_DUMP_HISTORY
        };
    }

    @Override
    public WorkerFunction.Parameter[] getParameters() {
        initParameters();
        return new WorkerFunction.Parameter[]{
                PARAM_TARGETS,
                PARAM_PROCESS_FRAME_MILLIS,
                PARAM_TOL_DIFF_SEMI,
                PARAM_CLEAR_TARGETS,
                PARAM_DUMP_HISTORY
        };
    }

    @Override
    public void setParameter(String attr, Object value) {
        initParameters();
        if (!isValueAccepted(attr, value))
            return;

        switch (attr) {
            case ATTR_TARGETS:
                if (value.toString().startsWith("[") && value.toString().endsWith("]"))
                    PARAM_TARGETS.setValue(value.toString());
                else
                    PARAM_TARGETS.setValue("[" + value.toString() + "]");
                return;
            case ATTR_PROCESS_FRAME_MILLIS:
                PARAM_PROCESS_FRAME_MILLIS.setValue(Integer.valueOf(value.toString()));
                return;
            case ATTR_TOL_DIFF_SEMI:
                PARAM_TOL_DIFF_SEMI.setValue(Integer.valueOf(value.toString()));
                return;
            case ATTR_CLEAR_TARGETS:
                PARAM_CLEAR_TARGETS.setValue(Boolean.valueOf(value.toString()));
                return;
            case ATTR_DUMP_HISTORY:
                PARAM_DUMP_HISTORY.setValue(Boolean.valueOf(value.toString()));
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
            case ATTR_PROCESS_FRAME_MILLIS:
            case ATTR_TOL_DIFF_SEMI:
                return checkTargetInteger(value.toString());
            case ATTR_CLEAR_TARGETS:
            case ATTR_DUMP_HISTORY:
                return true;
        }
        return false;
    }

    private boolean checkTargetString(String s) {
        try {
            JSONArray jsonArray;
            if (s.startsWith("[") && s.endsWith("]"))
                jsonArray = new JSONArray(s);
            else
                jsonArray = new JSONArray("[" + s + "]");
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

    private boolean checkTargetInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends View & DetectionListener> T getVisualizedView(Context ctx, String token, DetectorBase detector) {
        if (!(detector instanceof VisualizableDetector))
            return null;

        return (T) ToneDetectorView.createView(ctx, token, (VisualizableDetector) detector);
    }

    public static class Target extends DetectorBase.Target {
        private float targetFreq;
        private boolean state;

        Target(float targetFreq) {
            this.targetFreq = targetFreq;
            this.state = false;
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

    @Override
    public <T extends View & DetectionListener & DetectorBindable> void bindDetectorView(T v) {
        if (!(v instanceof ToneDetectorView))
            return;

        ToneDetectorView view = (ToneDetectorView) v;
        registerDetectionListener((DetectionListener) view.getDetectorViewHandler());
    }

    public ToneDetector(DetectionListener l, RecordStartFunction function, String params) {
        super(l, function, params);
    }

    private void initParameters() {
        mDumpFilePath = new File(Constants.externalDirectory(""), getHandle() + ".txt").getAbsolutePath();

        if (mTargetStorage == null)
            mTargetStorage = new TargetStorage();

        if (PARAM_TARGETS == null)
            PARAM_TARGETS = new WorkerFunction.Parameter<>(ATTR_TARGETS, false, "[]");

        if (PARAM_PROCESS_FRAME_MILLIS == null)
            PARAM_PROCESS_FRAME_MILLIS = new WorkerFunction.Parameter<>(
                    ATTR_PROCESS_FRAME_MILLIS, false, Constants.Detectors.ToneDetector.Config.PROCESS_FRAME_MILLIS);

        if (PARAM_TOL_DIFF_SEMI == null)
            PARAM_TOL_DIFF_SEMI = new WorkerFunction.Parameter<>(
                    ATTR_TOL_DIFF_SEMI, false, Constants.Detectors.ToneDetector.Config.TOL_DIFF_SEMI);

        if (PARAM_CLEAR_TARGETS == null)
            PARAM_CLEAR_TARGETS = new WorkerFunction.Parameter<>(ATTR_CLEAR_TARGETS, false, false);

        if (PARAM_DUMP_HISTORY == null)
            PARAM_DUMP_HISTORY = new WorkerFunction.Parameter<>(ATTR_DUMP_HISTORY, false, false);
    }

    private void updateCircularBuffers() {
        int numSamples = PARAM_PROCESS_FRAME_MILLIS.getValue() * mSamplingFreq / 1000;
        mBuffers = new ArrayList<>(mNumChannels);
        for (int c = 0; c < mNumChannels; c++) {
            CircularArray<Double> buffer = new CircularArray<>(numSamples);
            for (int i = 0; i < numSamples; i++)
                buffer.add(0.);

            mBuffers.add(buffer);
        }
    }

    @Override
    public Target getTargetById(int id) {
        if (id < mTargetStorage.content.size())
            return mTargetStorage.content.get(id);

        return null;
    }

    @Override
    public void registerTarget(DetectorBase.Target target) {
        if (target instanceof Target) {
            synchronized (mTargetStorage.content) {
                mTargetStorage.content.add((Target) target);
            }
        } else {
            Log.w(TAG, "The invalid registered target");
        }
    }

    @Override
    public void feed(List<? extends Double>[] data) {
        if (data.length != mNumChannels) {
            Log.e(TAG, "The number of channels of the feeding data does not match! (feeding "
                    + data.length + " channels while the active recording activity runs with " + mNumChannels + " channels.");
        }

        for (int c = 0; c < mNumChannels; c++) {
            mBuffers.get(c).addAll(data[c]);
        }

        for (CircularArray<Double> buffer : mBuffers) {
            if (buffer.size() == 0)
                continue;

            final double[] signal = new double[buffer.size()];
            for (int i = 0; i < signal.length; i++)
                signal[i] = buffer.get(i);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    process(signal);
                }
            }).start();
        }
    }

    @Override
    public boolean parseParameters(String params) {
        if (_function != null) {
            mSamplingFreq = _function.getSamplingFreq();
            mNumChannels = _function.getNumChannels();
        }

        if (params == null)
            return true;

        if (!setDetectorParameters(params))
            return false;

        updateCircularBuffers();
        return true;
    }

    @Override
    public boolean setDetectorParameters(String params) {
        initParameters();
        if (params == null)
            return true;

        Log.d(TAG, "setDetectorParameters(" + params + ")");
        boolean success;
        try {
            JSONObject jsonParams = new JSONObject(params);
            processParamsIfClearTargets(jsonParams);
            processParamsOfTargetFreqs(jsonParams);
            processParamsOfProcessFrameMillis(jsonParams);
            processParamsOfTolerance(jsonParams);
            processParamsOfDumpHistory(jsonParams);
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
            params.put(ATTR_PROCESS_FRAME_MILLIS, PARAM_PROCESS_FRAME_MILLIS.getValue());
            params.put(ATTR_TOL_DIFF_SEMI, PARAM_TOL_DIFF_SEMI.getValue());
            params.put(ATTR_TARGETS, new JSONArray(PARAM_TARGETS.getValue()));
            params.put(ATTR_DUMP_HISTORY, PARAM_DUMP_HISTORY.getValue());

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
        try {
            JSONObject obj = new JSONObject();
            JSONObject unitObj = new JSONObject();

            unitObj.put(INFO_KEY_SAMPLING_FREQ, "Hz");
            unitObj.put(INFO_KEY_FRAME_SIZE, "ms");
            unitObj.put(INFO_KEY_TOLERANCE, "keys");

            obj.put(INFO_KEY_HANDLE, getHandle());
            obj.put(INFO_KEY_SAMPLING_FREQ, mSamplingFreq);
            obj.put(INFO_KEY_FRAME_SIZE, PARAM_PROCESS_FRAME_MILLIS.getValue());
            obj.put(INFO_KEY_TOLERANCE, PARAM_TOL_DIFF_SEMI.getValue());
            obj.put(INFO_KEY_UNIT, unitObj);

            JSONArray targets = new JSONArray();
            synchronized (mTargetStorage.content) {
                for (Target t : mTargetStorage.content)
                    targets.put(t.toJson());
            }
            obj.put(INFO_KEY_TARGETS, targets);

            if (PARAM_DUMP_HISTORY.getValue()) {
                obj.put(INFO_KEY_HISTORY, mDumpFilePath);
            }

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return super.toString();
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handle: ").append(getHandle()).append("\n");
        sb.append("Sampling Frequency: ").append(mSamplingFreq).append(" Hz\n");
        sb.append("Process Frame Size: ").append(PARAM_PROCESS_FRAME_MILLIS.getValue()).append(" ms\n");
        synchronized (mTargetStorage.content) {
            sb.append("Targets: ").append(mTargetStorage.content.size()).append(" target(s)").append("\n");
            for (int i = 0; i < mTargetStorage.content.size(); i++)
                sb.append("\t").append(i).append(": ").append(mTargetStorage.content.get(i).toJson()).append("\n");
        }
        sb.append("Detection History: ").append(mDumpFilePath);
        return sb.toString();
    }

    @Override
    public void notifySettingChanged() {
        if (_function != null) {
            mSamplingFreq = _function.getSamplingFreq();
            mNumChannels = _function.getNumChannels();
            updateCircularBuffers();
        }
    }

    @Override
    public void release() {
        File historyFile = new File(mDumpFilePath);
        if (historyFile.exists()) {
            historyFile.delete();
        }
    }

    private void processParamsIfClearTargets(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS) ||
                !jsonParams.getBoolean(Constants.Detectors.ToneDetector.PARAM_CLEAR_TARGETS))
            return;

        PARAM_CLEAR_TARGETS.setValue(true);
    }

    private void processParamsOfTargetFreqs(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ))
            return;

        PARAM_TARGETS.setValue(jsonParams.getJSONArray(Constants.Detectors.ToneDetector.PARAM_TARGET_FREQ).toString());
    }

    private void processParamsOfProcessFrameMillis(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_PROCESS_FRAME_MILLIS))
            return;

        PARAM_PROCESS_FRAME_MILLIS.setValue(jsonParams.getInt(Constants.Detectors.ToneDetector.PARAM_PROCESS_FRAME_MILLIS));
    }

    private void processParamsOfTolerance(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_TOL_DIFF_SEMI))
            return;

        PARAM_TOL_DIFF_SEMI.setValue(jsonParams.getInt(Constants.Detectors.ToneDetector.PARAM_TOL_DIFF_SEMI));
    }

    private void processParamsOfDumpHistory(JSONObject jsonParams) throws JSONException {
        if (!jsonParams.has(Constants.Detectors.ToneDetector.PARAM_DUMP_HISTORY))
            return;

        boolean newValue = jsonParams.getBoolean(Constants.Detectors.ToneDetector.PARAM_DUMP_HISTORY);
        if (newValue == PARAM_DUMP_HISTORY.getValue())
            return;

        PARAM_DUMP_HISTORY.setValue(newValue);
        if (!newValue) {
            File historyFile = new File(mDumpFilePath);
            if (historyFile.exists())
                historyFile.delete();
        }
    }

    private void syncParameters() {
        if (PARAM_CLEAR_TARGETS.getValue()) {
            synchronized (mTargetStorage.content) {
                if (PARAM_DUMP_HISTORY.getValue()) {
                    StringBuilder history = new StringBuilder();
                    for (int i = 0; i < mTargetStorage.content.size(); i++) {
                        Target t = mTargetStorage.content.get(i);
                        history.append(System.currentTimeMillis()).append(": ")
                                .append(t.targetFreq).append(" inactive").append("\n");
                    }

                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(mDumpFilePath), true));
                        pw.write(history.toString());
                        pw.close();
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "failed to dump the detection history");
                        e.printStackTrace();
                    }
                }
                mTargetStorage.content.clear();
            }
        }

        try {
            JSONArray targetFreqs = new JSONArray(PARAM_TARGETS.getValue());
            for (int i = 0; i < targetFreqs.length(); i++) {
                Target t = new Target((float) targetFreqs.getDouble(i));
                Target dummy = null;
                synchronized (mTargetStorage.content) {
                    for (int j = 0; j < mTargetStorage.content.size(); j++) {
                        dummy = mTargetStorage.content.get(j);
                        if (dummy.targetFreq == t.targetFreq)
                            break;
                    }
                    if (dummy == null || dummy.targetFreq != t.targetFreq) {
                        mTargetStorage.content.add(t);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void process(double[] signal) {
        synchronized (mTargetStorage.content) {
            if (mTargetStorage.content.size() == 0)
                return;
        }

        SparseArray<Target> targets = new SparseArray<>();
        double[] spectrum = FFT.transformAbs(signal);

        ArrayList<Double> targetFreqs = new ArrayList<>();
        for (Target t : mTargetStorage.content)
            targetFreqs.add((double) t.targetFreq);

        double minNormFactor = Math.sqrt(spectrum.length/2) / 20.0;
        double stepFreq = (double) mSamplingFreq / spectrum.length;
        PeakDetector.Config config = new PeakDetector.Config.Builder()
                .withMinNormFactor(minNormFactor)
                .withNumPoints(5)
                .withStep(1)
                .withStepFreq(stepFreq)
                .addTargetFreqs(targetFreqs).build();

        ArrayList<double[]> features = PeakDetector.extractFeature(Arrays.copyOf(spectrum, spectrum.length/2), config);
        for (int i = 0; i < features.size(); i++) {
            PeakDetector.QuadraticFeature feature = new PeakDetector.QuadraticFeature(features.get(i), config);
            if (featureActive(feature)) {
                double tfreq = targetFreqs.get(i);
                double freq = tfreq - (feature.coeffs[1] / (2 * feature.coeffs[0])) * stepFreq;
                double amp = Math.pow(feature.coeffs[1], 2) / (4 * feature.coeffs[0]) + feature.coeffs[2];

                if (amp > 0.5 && freq > 0 &&
                        ToneDetector.toneDetected(freq, tfreq, PARAM_TOL_DIFF_SEMI.getValue()))
                    targets.put(i, mTargetStorage.content.get(i));
            }
        }

        if (PARAM_DUMP_HISTORY.getValue()) {
            StringBuilder history = new StringBuilder();
            for (int i = 0; i < mTargetStorage.content.size(); i++) {
                boolean detected = targets.get(i) != null;
                Target t = mTargetStorage.content.get(i);
                if (t.state != detected) {
                    mTargetStorage.content.get(i).state = detected;
                    history.append(System.currentTimeMillis()).append(": ")
                            .append(t.targetFreq).append(" ").append(detected ? "active":"inactive").append("\n");
                }
            }

            if (history.length() > 0) {
                try {
                    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(mDumpFilePath), true));
                    pw.write(history.toString());
                    pw.close();
                    Log.d(TAG, "write: " + history.toString());
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "failed to dump the detection history");
                    e.printStackTrace();
                }
            }
        }

        broadcastTargetDetected(targets);
    }

    private boolean featureActive(PeakDetector.QuadraticFeature feature) {
        return feature.coeffs[0] < 0 && feature.coeffs[2] > 0.5 && feature.dataDensity() > 0.4;
    }

    static public boolean toneDetected(double freq, double tfreq, double diffInSemiTone) {
        return Math.abs(Math.log(freq / tfreq) / Math.log(2)) * 12 < diffInSemiTone;
    }
}
