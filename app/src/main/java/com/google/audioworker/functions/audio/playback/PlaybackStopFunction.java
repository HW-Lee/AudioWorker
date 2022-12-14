package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.utils.Constants;

public class PlaybackStopFunction extends PlaybackFunction {
    private final static String TAG = Constants.packageTag("PlaybackStopFunction");

    private final static String ATTR_TYPE = PlaybackStartFunction.ATTR_TYPE;
    private final static String ATTR_PLAYBACK_ID = PlaybackStartFunction.ATTR_PLAYBACK_ID;

    private final static String[] ATTRS = {
            ATTR_TYPE,
            ATTR_PLAYBACK_ID
    };

    private Parameter<String> PARAM_TYPE = new Parameter<>(ATTR_TYPE, true, null);
    private Parameter<Integer> PARAM_PLAYBACK_ID = new Parameter<>(ATTR_PLAYBACK_ID, true, -1);
    private Parameter[] PARAMS = {
            PARAM_TYPE,
            PARAM_PLAYBACK_ID
    };

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
        try {
            switch (attr) {
                case ATTR_TYPE:
                    return checkType((String) value);
                case ATTR_PLAYBACK_ID:
                    return checkPlaybackId((int) value);
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

    private boolean checkType(String type) {
        return TASK_OFFLOAD.equals(type) || TASK_NONOFFLOAD.equals(type);
    }

    private boolean checkPlaybackId(int id) {
        return id >= 0;
    }

    public String getPlaybackType() {
        return PARAM_TYPE.getValue();
    }

    public int getPlaybackId() {
        return PARAM_PLAYBACK_ID.getValue();
    }

    public void setPlaybackId(int id) {
        PARAM_PLAYBACK_ID.setValue(id);
    }

    public void setPlaybackType(String type) {
        PARAM_TYPE.setValue(type);
    }
}
