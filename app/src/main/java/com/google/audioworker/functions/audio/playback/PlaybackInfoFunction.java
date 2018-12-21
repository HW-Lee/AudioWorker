package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.utils.Constants;

public class PlaybackInfoFunction extends PlaybackFunction {
    private final static String TAG = Constants.packageTag("PlaybackInfoFunction");

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
