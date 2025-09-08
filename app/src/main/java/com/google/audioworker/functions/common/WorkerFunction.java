package com.google.audioworker.functions.common;

import android.support.annotation.NonNull;

import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class WorkerFunction {
    public interface Parameterizable {
        WorkerFunction.Parameter[] getParameters();

        String[] getAttributes();

        void setParameter(String attr, Object value);

        boolean isValueAccepted(String attr, Object value);
    }

    public abstract JSONObject toJson();

    public abstract boolean isValid();

    protected String mCommandId;
    protected final ArrayList<Ack> mAcks = new ArrayList<>();

    public interface WorkerFunctionListener {
        void onAckReceived(Ack ack);
    }

    public void setCommandId(String commandId) {
        mCommandId = commandId;
    }

    public String getCommandId() {
        return mCommandId;
    }

    public void pushAck(Ack ack) {
        synchronized (mAcks) {
            mAcks.add(ack);
        }
    }

    public ArrayList<Ack> getAcks() {
        return mAcks;
    }

    public boolean isExecuted() {
        return mAcks.size() > 0;
    }

    @NonNull
    @Override
    public String toString() {
        return toJson().toString();
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

        public String getDescription() {
            return (String) getProperty(Constants.MessageSpecification.COMMAND_ACK_DESC);
        }

        public Object[] getReturns() {
            ArrayList<Object> returns = new ArrayList<>();
            JSONArray jsonReturns =
                    (JSONArray) getProperty(Constants.MessageSpecification.COMMAND_ACK_RETURN);
            if (jsonReturns == null) return new Object[0];
            try {
                for (int i = 0; i < jsonReturns.length(); i++) {
                    if (jsonReturns.get(i) != null) returns.add(jsonReturns.get(i));
                }

                return returns.toArray();
            } catch (JSONException e) {
                e.printStackTrace();
                return new Object[0];
            }
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

        public static Ack parseAck(String ackString) {
            try {
                return new Ack(ackString);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public static Ack ackToCommand(JSONObject command) {
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

        public static Ack ackToFunction(WorkerFunction function) {
            Ack ack = new Ack();
            String command_id = "N/A";
            if (function.getCommandId() != null) command_id = function.getCommandId();

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
        private boolean set;
        private T value;

        public Parameter(String attr, boolean required, T defaultValue) {
            this.attr = attr;
            this.required = required;
            this.set = false;
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

        public boolean beenSet() {
            return this.set;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object value) {
            if (value == null) return;

            this.set = true;
            if ((value instanceof Float || value instanceof Integer)
                    && (this.value instanceof Float || this.value instanceof Integer)) {
                float v;
                if (value instanceof Integer) v = (int) value;
                else v = (float) value;

                if (this.value instanceof Float) this.value = (T) Float.valueOf(v);
                else this.value = (T) Integer.valueOf((int) v);

                return;
            }
            this.value = (T) value;
        }
    }
}
