package com.google.audioworker.functions.audio.record;

import com.google.audioworker.utils.Constants;

import java.util.Arrays;

public class RecordInfoFunction extends RecordFunction {
    private static final String TAG = Constants.packageTag("RecordInfoFunction");

    private static final String ATTR_FILENAME = "filename";

    private static final String[] ATTRS = {ATTR_FILENAME};

    private Parameter<String> PARAM_FILENAME = new Parameter<>(ATTR_FILENAME, false, "");

    private Parameter[] PARAMS = {PARAM_FILENAME};

    private Parameter[] mParams;
    private String[] mAttrs;

    public RecordInfoFunction() {
        mParams = Arrays.copyOf(PARAMS, PARAMS.length + super.getParameters().length);
        mAttrs = Arrays.copyOf(ATTRS, ATTRS.length + super.getAttributes().length);
        System.arraycopy(
                super.getParameters(), 0, mParams, PARAMS.length, super.getParameters().length);
        System.arraycopy(
                super.getAttributes(), 0, mAttrs, ATTRS.length, super.getAttributes().length);
    }

    @Override
    public Parameter[] getParameters() {
        return mParams;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        if (super.isValueAccepted(attr, value)) return true;

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
        super.setParameter(attr, value);
        if (isValueAccepted(attr, value)) {
            switch (attr) {
                case ATTR_FILENAME:
                    PARAM_FILENAME.setValue(value);
            }
        }
    }

    public String getFileName() {
        return PARAM_FILENAME.getValue();
    }
}
