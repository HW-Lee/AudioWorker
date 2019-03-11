package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPInfoFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPInfoFunction");

    private final static String ATTR_FILENAME = "filename";

    private final static String[] ATTRS = {
            ATTR_FILENAME
    };

    private Parameter<String> PARAM_FILENAME = new Parameter<>(ATTR_FILENAME, false, "");

    private Parameter[] PARAMS = {
            PARAM_FILENAME
    };

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
        if (isValueAccepted(attr, value))
            PARAM_FILENAME.setValue(value);
    }

    public String getFileName() {
        return PARAM_FILENAME.getValue();
    }
}
