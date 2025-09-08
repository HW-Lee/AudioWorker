package com.google.audioworker.functions.audio.record;

import com.google.audioworker.functions.audio.AudioFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.Constants.Controllers.Config.RecordTask;

public class RecordFunction extends AudioFunction {
    private static final String ATTR_INDEX = "task-index";

    private static final String[] ATTRS = {ATTR_INDEX};

    private Parameter<Integer> PARAM_INDEX =
            new Parameter<>(ATTR_INDEX, false, Constants.RecordDefaultConfig.INDEX);

    private Parameter[] PARAMS = {PARAM_INDEX};

    @Override
    public Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public String[] getAttributes() {
        return ATTRS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        switch (attr) {
            case ATTR_INDEX:
                return checkIndex((int) value);
            default:
                break;
        }
        return false;
    }

    @Override
    public void setParameter(String attr, Object value) {
        RecordFunction check = new RecordFunction();
        if (check.isValueAccepted(attr, value)) {
            PARAM_INDEX.setValue(value);
        }
    }

    public int getIndex() {
        return PARAM_INDEX.getValue();
    }

    private boolean checkIndex(int index) {
        if ((index >= RecordTask.INDEX_DEFAULT && index < RecordTask.MAX_NUM)
                || index == RecordTask.TASK_ALL) {
            return true;
        }
        return false;
    }
}
