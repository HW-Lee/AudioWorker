package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPStopFunction extends VoIPFunction {
    private static final String TAG = Constants.packageTag("VoIPStopFunction");

    @Override
    public String[] getAttributes() {
        return new String[0];
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[0];
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        return false;
    }

    @Override
    public void setParameter(String attr, Object value) {}
}
