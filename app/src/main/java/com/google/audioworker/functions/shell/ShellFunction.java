package com.google.audioworker.functions.shell;

import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

public class ShellFunction extends WorkerFunction {
    private final static String TAG = Constants.packageTag("ShellFunction");

    private final static String ATTR_COMMAND = "cmd";

    private final static String[] ATTRS = {
            ATTR_COMMAND
    };

    private Parameter<String> PARAM_COMMAND = new Parameter<>(ATTR_COMMAND, true, null);

    private Parameter[] PARAMS = {
            PARAM_COMMAND
    };

    public ShellFunction(String cmd) {
        super();
        setParameter(ATTR_COMMAND, cmd);
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
}
