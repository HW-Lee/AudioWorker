package com.google.audioworker.functions.shell;

import android.content.Intent;

import com.google.audioworker.functions.common.ParameterizedWorkerFunction;
import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ShellFunction extends ParameterizedWorkerFunction {
    private final static String TAG = Constants.packageTag("ShellFunction");

    private final static String ATTR_COMMAND = "cmd";

    private final static String[] ATTRS = {
            ATTR_COMMAND
    };

    private Parameter<String> PARAM_COMMAND = new Parameter<>(ATTR_COMMAND, true, null);

    private Parameter[] PARAMS = {
            PARAM_COMMAND
    };

    private Intent mBroadcastIntent;

    public ShellFunction(JSONObject obj) throws JSONException {
        super();
        String action = obj.getString(Constants.MessageSpecification.COMMAND_BROADCAST_INTENT);
        mBroadcastIntent = new Intent();
        mBroadcastIntent.setAction(action);
        StringBuilder intent = new StringBuilder("am broadcast -a");
        intent.append(" ").append(action);
        if (!obj.has(Constants.MessageSpecification.COMMAND_BROADCAST_PARAMS)) {
            setParameter(ATTR_COMMAND, intent.toString());
        } else {
            JSONArray params = obj.getJSONArray(Constants.MessageSpecification.COMMAND_BROADCAST_PARAMS);
            for (int i = 0; i < params.length(); i++) {
                JSONArray kvpair = params.getJSONArray(i);
                if (kvpair.length() < 2)
                    continue;

                intent.append(" ").append(getParamString(kvpair.getString(0), kvpair.get(1)));

                if (kvpair.get(1) instanceof Integer)
                    mBroadcastIntent.putExtra(kvpair.getString(0), Integer.valueOf(kvpair.get(1).toString()));
                else if (kvpair.get(1) instanceof Number)
                    mBroadcastIntent.putExtra(kvpair.getString(0), Float.valueOf(kvpair.get(1).toString()));
                else if (kvpair.get(1) instanceof String)
                    mBroadcastIntent.putExtra(kvpair.getString(0), (String) kvpair.get(1));
            }

            setParameter(ATTR_COMMAND, intent.toString());
        }
    }

    public ShellFunction(String cmd) {
        super();
        setParameter(ATTR_COMMAND, cmd);
    }

    private String getParamString(String key, Object value) {
        if (value instanceof Integer)
            return "--ei \"" + key + "\" " + Integer.valueOf(value.toString());

        if (value instanceof Double || value instanceof Float)
            return "--ef \"" + key + "\" " + Float.valueOf(value.toString());

        if (value instanceof String)
            return "--es \"" + key + "\" \"" + value + "\"";

        return "";
    }

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
            case ATTR_COMMAND:
                return value instanceof String;

            default:
                return false;
        }
    }

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

    public String getCommand() {
        return PARAM_COMMAND.getValue();
    }

    public boolean isBroadcastFunction() {
        return mBroadcastIntent != null;
    }

    public Intent getBroadcastIntent() {
        return mBroadcastIntent;
    }
}
