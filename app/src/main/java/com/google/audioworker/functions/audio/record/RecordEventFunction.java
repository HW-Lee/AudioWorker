package com.google.audioworker.functions.audio.record;

import com.google.audioworker.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class RecordEventFunction extends RecordFunction {
    private final static String TAG = Constants.packageTag("RecordEventFunction");

    private final static String ATTR_EVENT = "event";
    private final static String ATTR_CLASS_HANDLE = "class-handle";

    private final static String[] ATTRS = {
            ATTR_EVENT,
            ATTR_CLASS_HANDLE
    };

    private Parameter<String> PARAM_EVENT = new Parameter<>(ATTR_EVENT, true, null);
    private Parameter<String> PARAM_CLASS_HANDLE = new Parameter<>(ATTR_CLASS_HANDLE, true, null);
    private Parameter[] PARAMS = {
            PARAM_EVENT,
            PARAM_CLASS_HANDLE
    };

    @Override
    public Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        try {
            switch (attr) {
                case ATTR_EVENT:
                    return checkEvent(value);
                case ATTR_CLASS_HANDLE:
                    return checkClassHandle(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setParameter(String attr, Object value) {
        if (isValueAccepted(attr, value)) {
            int idx = toIndex(attr);
            if (idx >= 0)
                PARAMS[idx].setValue(value);
        }
    }

    private int toIndex(String attr) {
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i].equals(attr))
                return i;
        }
        return -1;
    }

    private boolean checkClassHandle(Object value) {
        return value instanceof String;
    }

    private boolean checkEvent(Object value) {
        if (!(value instanceof String))
            return false;

        try {
            new JSONObject((String) value);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getDetectEvent() {
        return PARAM_EVENT.getValue();
    }

    public String getClassHandle() {
        return PARAM_CLASS_HANDLE.getValue();
    }
}
