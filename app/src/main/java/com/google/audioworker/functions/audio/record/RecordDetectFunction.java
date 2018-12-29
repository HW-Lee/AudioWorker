package com.google.audioworker.functions.audio.record;

import com.google.audioworker.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class RecordDetectFunction extends RecordFunction {
    private final static String TAG = Constants.packageTag("RecordDetectFunction");

    private final static String ATTR_TYPE = "type";
    private final static String ATTR_CLASS_NAME = "class";
    private final static String ATTR_CLASS_HANDLE = "class-handle";
    private final static String ATTR_PARAMS = "params";

    public final static String OP_REGISTER = "register";
    public final static String OP_UNREGISTER = "unregister";
    public final static String OP_SETPARAMS = "setparams";

    private final static String[] OPS = {
            OP_REGISTER,
            OP_UNREGISTER,
            OP_SETPARAMS
    };

    private final static String[] ATTRS = {
            ATTR_TYPE,
            ATTR_CLASS_NAME,
            ATTR_CLASS_HANDLE,
            ATTR_PARAMS
    };

    private Parameter<String> PARAM_TYPE = new Parameter<>(ATTR_TYPE, true, null);
    private Parameter<String> PARAM_CLASS_NAME = new Parameter<>(ATTR_CLASS_NAME, true, null);
    private Parameter<String> PARAM_CLASS_HANDLE = new Parameter<>(ATTR_CLASS_HANDLE, true, null);
    private Parameter<String> PARAM_TPARAMS = new Parameter<>(ATTR_PARAMS, false, null);
    private Parameter[] PARAMS = {
            PARAM_TYPE,
            PARAM_CLASS_NAME,
            PARAM_CLASS_HANDLE,
            PARAM_TPARAMS
    };

    public RecordDetectFunction() {
        this(OP_REGISTER);
    }

    public RecordDetectFunction(String type) {
        super();
        setType(type);

        if (!checkType(type))
            return;

        PARAM_CLASS_NAME.setRequired(type.equals(OP_REGISTER));
        PARAM_CLASS_HANDLE.setRequired(type.equals(OP_UNREGISTER) || type.equals(OP_SETPARAMS));
    }

    public void setType(String type) {
        setParameter(ATTR_TYPE, type);
    }

    @Override
    public Parameter[] getParameters() {
        switch (getOperationType()) {
            case OP_REGISTER:
                return new Parameter[]{PARAM_CLASS_NAME, PARAM_TPARAMS};
            case OP_UNREGISTER:
                return new Parameter[]{PARAM_CLASS_HANDLE};
            case OP_SETPARAMS:
                return new Parameter[]{PARAM_CLASS_HANDLE, PARAM_TPARAMS};
        }
        return new Parameter[0];
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        try {
            switch (attr) {
                case ATTR_TYPE:
                    return checkType((String) value);
                case ATTR_CLASS_NAME:
                case ATTR_CLASS_HANDLE:
                    return checkClassName(value);
                case ATTR_PARAMS:
                    return checkParams(value);
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

    private boolean checkClassName(Object value) {
        return value instanceof String;
    }

    private boolean checkParams(Object value) {
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

    private boolean checkType(String type) {
        for (String op : OPS)
            if (op.equals(type))
                return true;

        return false;
    }

    public String getOperationType() {
        return PARAM_TYPE.getValue();
    }

    public String getDetectorClassName() {
        return PARAM_CLASS_NAME.getValue();
    }

    public String getClassHandle() {
        return PARAM_CLASS_HANDLE.getValue();
    }

    public String getDetectorParams() {
        return PARAM_TPARAMS.getValue();
    }
}
