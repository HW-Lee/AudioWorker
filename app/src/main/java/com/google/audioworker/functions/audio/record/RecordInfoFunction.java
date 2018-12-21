package com.google.audioworker.functions.audio.record;

import com.google.audioworker.utils.Constants;

public class RecordInfoFunction extends RecordFunction {
    private final static String TAG = Constants.packageTag("RecordInfoFunction");

    @Override
    public Parameter[] getParameters() {
        return new Parameter[0];
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        return false;
    }

    @Override
    public void setParameter(String attr, Object value) {
    }
}
