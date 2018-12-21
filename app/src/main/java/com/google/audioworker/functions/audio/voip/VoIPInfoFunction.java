package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPInfoFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPInfoFunction");

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
