package com.google.audioworker.functions.audio.record;

import java.util.Arrays;
import com.google.audioworker.utils.Constants;

public class RecordDumpFunction extends RecordFunction {
    private final static String TAG = Constants.packageTag("RecordDumpFunction");

    private final static String ATTR_FILENAME = "filename";

    private final static String[] ATTRS = {
            ATTR_FILENAME
    };

    private Parameter<String> PARAM_FILENAME = new Parameter<>(ATTR_FILENAME, true, null);

    private Parameter[] PARAMS = {
            PARAM_FILENAME
    };

    private Parameter[] mParams;

    public RecordDumpFunction() {
        mParams = Arrays.copyOf(PARAMS, PARAMS.length + super.getParameters().length);
        System.arraycopy(
            super.getParameters(), 0, mParams, PARAMS.length, super.getParameters().length);
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
        if (isValueAccepted(attr, value)) {
            switch (attr) {
                case ATTR_FILENAME:
                    PARAM_FILENAME.setValue(value);
            }

            super.setParameter(attr, value);
        }
    }

    public String getFileName() {
        return PARAM_FILENAME.getValue();
    }
}
