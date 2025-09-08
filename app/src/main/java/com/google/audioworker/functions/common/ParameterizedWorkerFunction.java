package com.google.audioworker.functions.common;

import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public abstract class ParameterizedWorkerFunction extends WorkerFunction
        implements WorkerFunction.Parameterizable {
    public static final String KEY_CLASS_NAME = "class";
    public static final String KEY_HAS_ACK = "has-ack";
    public static final String KEY_ACK = "ack";
    public static final String KEY_PARAMS = "params";
    public static final String KEY_UNDEFINED_REQUIRED = "required-not-defined";
    public static final String KEY_COMMAND_ID = Constants.MessageSpecification.COMMAND_ID;

    @Override
    public boolean isValid() {
        if (getRequiredNotDefinedAttributes().size() > 0) return false;

        for (Parameter parameter : getParameters()) {
            if (!isValueAccepted(parameter.getAttribute(), parameter.getValue())) return false;
        }

        return true;
    }

    private Collection<String> getRequiredNotDefinedAttributes() {
        ArrayList<String> requiredNotDefined = new ArrayList<>();
        for (Parameter parameter : getParameters()) {
            if (parameter.isRequired() && !parameter.beenSet())
                requiredNotDefined.add(parameter.getAttribute());
        }

        return requiredNotDefined;
    }

    @Override
    public JSONObject toJson() {
        JSONObject info = new JSONObject();
        JSONObject params = new JSONObject();
        JSONArray requiedNotDefined = new JSONArray();
        save_put(info, KEY_CLASS_NAME, getClass().asSubclass(getClass()).getName());
        save_put(info, KEY_HAS_ACK, isExecuted());
        if (isExecuted()) {
            JSONArray arr = new JSONArray();
            synchronized (mAcks) {
                for (Ack ack : mAcks) arr.put(ack.toString());
            }
            save_put(info, KEY_ACK, arr);
        }
        for (Parameter parameter : getParameters()) {
            save_put(params, parameter.getAttribute(), parameter.getValue());
        }
        save_put(info, KEY_PARAMS, params);
        for (String attr : getRequiredNotDefinedAttributes()) {
            requiedNotDefined.put(attr);
        }
        if (requiedNotDefined.length() > 0) {
            save_put(info, KEY_UNDEFINED_REQUIRED, requiedNotDefined);
        }
        if (mCommandId != null) {
            save_put(info, KEY_COMMAND_ID, mCommandId);
        }
        return info;
    }

    private void save_put(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
