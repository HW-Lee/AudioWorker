package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.functions.audio.record.RecordEventFunction;
import com.google.audioworker.utils.Constants;

public class VoIPEventFunction extends VoIPFunction {
    private static final String TAG = Constants.packageTag("VoIPEventFunction");

    private RecordEventFunction mReference = new RecordEventFunction();

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

    public String getDetectEvent() {
        return mReference.getDetectEvent();
    }

    public String getClassHandle() {
        return mReference.getClassHandle();
    }
}
