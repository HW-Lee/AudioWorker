package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.utils.Constants;

public class PlaybackInfoFunction extends PlaybackFunction {
    private static final String TAG = Constants.packageTag("PlaybackInfoFunction");

    private static final String ATTR_FILENAME = "filename";

    private static final String[] ATTRS = {ATTR_FILENAME};

    private Parameter<String> PARAM_FILENAME = new Parameter<>(ATTR_FILENAME, false, "");

    private Parameter[] PARAMS = {PARAM_FILENAME};

    @Override
    public String[] getAttributes() {
        return ATTRS;
    }

    @Override
    public Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        switch (attr) {
            case ATTR_FILENAME:
                return value instanceof String;

            default:
                break;
        }
        return false;
    }

    @Override
    public void setParameter(String attr, Object value) {
        if (isValueAccepted(attr, value)) PARAM_FILENAME.setValue(value);
    }

    public String getFileName() {
        return PARAM_FILENAME.getValue();
    }
}
