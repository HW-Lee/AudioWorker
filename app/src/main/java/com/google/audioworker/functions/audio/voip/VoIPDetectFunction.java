package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.utils.Constants;

public class VoIPDetectFunction extends VoIPFunction {
    private static final String TAG = Constants.packageTag("VoIPDetectFunction");

    private RecordDetectFunction mReference;

    public VoIPDetectFunction() {
        mReference = new RecordDetectFunction();
    }

    public VoIPDetectFunction(String type) {
        mReference = new RecordDetectFunction(type);
    }

    @Override
    public String[] getAttributes() {
        return mReference.getAttributes();
    }

    @Override
    public Parameter[] getParameters() {
        return mReference.getParameters();
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        return mReference.isValueAccepted(attr, value);
    }

    @Override
    public void setParameter(String attr, Object value) {
        mReference.setParameter(attr, value);
    }

    public String getOperationType() {
        return mReference.getOperationType();
    }

    public String getDetectorClassName() {
        return mReference.getDetectorClassName();
    }

    public String getClassHandle() {
        return mReference.getClassHandle();
    }

    public String getDetectorParams() {
        return mReference.getDetectorParams();
    }
}
