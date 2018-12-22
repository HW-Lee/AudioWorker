package com.google.audioworker.functions.common;

import android.support.annotation.NonNull;

import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class WorkerFunction {
    abstract public WorkerFunction.Parameter[] getParameters();
    abstract public boolean isValueAccepted(String attr, Object value);
    abstract public void setParameter(String attr, Object value);

    protected String mCommandId;
    protected Ack mAck;

    public interface WorkerFunctionListener {
        public void onAckReceived(Ack ack);
    }

    public void setCommandId(String commandId) {
        mCommandId = commandId;
    }

    public String getCommandId() {
        return mCommandId;
    }

    public void setAck(Ack ack) {
        mAck = ack;
    }

    public Ack getAck() {
        return mAck;
    }

    public boolean isExecuted() {
        return mAck != null;
    }

    public boolean isValid() {
        return getRequiredNotDefinedAttributes().size() == 0;
    }

    private Collection<String> getRequiredNotDefinedAttributes() {
        ArrayList<String> requiredNotDefined = new ArrayList<>();
        for (Parameter parameter : getParameters()) {
            if (parameter.isRequired() && parameter.getValue() == null)
                requiredNotDefined.add(parameter.getAttribute());
        }

        return requiredNotDefined;
    }

    protected JSONObject toJson() {
        JSONObject info = new JSONObject();
        JSONObject params = new JSONObject();
        JSONArray requiedNotDefined = new JSONArray();
        save_put(info, "class", getClass().asSubclass(getClass()).getName());
        save_put(info, "has-ack", isExecuted());
        if (isExecuted()) {
            save_put(info, "ack", mAck);
        }
        for (Parameter parameter : getParameters()) {
            save_put(params, parameter.getAttribute(), parameter.getValue());
        }
        save_put(info, "params", params);
        for (String attr : getRequiredNotDefinedAttributes()) {
            requiedNotDefined.put(attr);
        }
        if (requiedNotDefined.length() > 0) {
            save_put(info, "required-not-defined", requiedNotDefined);
        }
        if (mCommandId != null) {
            save_put(info, "command-id", mCommandId);
        }
        return info;
    }

    @NonNull
    @Override
    public String toString() {
        return toJson().toString();
    }

    private void save_put(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static class Ack extends JSONObject {
        private Ack() {
            super();
        }

        private Ack(String s) throws JSONException {
            super(s);
        }

        private Object getProperty(String key) {
            try {
                return get(key);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        public int getReturnCode() {
            return (int) getProperty(Constants.MessageSpecification.COMMAND_ACK_RETURN_CODE);
        }

        public String getTarget() {
            return "" + getProperty(Constants.MessageSpecification.COMMAND_ACK_TARGET);
        }

        public void setDescription(String desc) {
            try {
                put(Constants.MessageSpecification.COMMAND_ACK_DESC, desc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void setReturnCode(int code) {
            try {
                put(Constants.MessageSpecification.COMMAND_ACK_RETURN_CODE, code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void setReturns(List<Object> list) {
            try {
                JSONArray json_array = new JSONArray(list);
                put(Constants.MessageSpecification.COMMAND_ACK_RETURN, json_array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        static public Ack parseAck(String ackString) {
            try {
                return new Ack(ackString);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        static public Ack ackToCommand(JSONObject command) {
            Ack ack = new Ack();
            String command_id = "N/A";
            try {
                command_id = command.getString(Constants.MessageSpecification.COMMAND_ID);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                ack.put(Constants.MessageSpecification.COMMAND_ACK_TARGET, command_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ack.setDescription("");
            ack.setReturnCode(-1);
            ack.setReturns(new ArrayList<>());

            return ack;
        }

        static public Ack ackToFunction(WorkerFunction function) {
            Ack ack = new Ack();
            String command_id = "N/A";
            if (function.getCommandId() != null)
                command_id = function.getCommandId();

            try {
                ack.put(Constants.MessageSpecification.COMMAND_ACK_TARGET, command_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ack.setDescription("");
            ack.setReturnCode(-1);
            ack.setReturns(new ArrayList<>());

            return ack;
        }
    }

    public static class Parameter<T> {
        private String attr;
        private boolean required;
        private T value;

        public Parameter(String attr, boolean required, T defaultValue) {
            this.attr = attr;
            this.required = required;
            this.value = defaultValue;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getAttribute() {
            return attr;
        }

        public boolean isRequired() {
            return required;
        }

        public T getValue() {
            return value;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object value) {
            if ((value instanceof Float || value instanceof Integer)
                    && (this.value instanceof Float || this.value instanceof Integer)) {
                float v;
                if (value instanceof Integer)
                    v = Float.valueOf(value.toString());
                else
                    v = (Float) value;

                if (this.value instanceof Float)
                    this.value = (T) Float.valueOf(v);
                else
                    this.value = (T) Integer.valueOf((int) v);

                return;
            }
            this.value = (T) value;
        }
    }
}
