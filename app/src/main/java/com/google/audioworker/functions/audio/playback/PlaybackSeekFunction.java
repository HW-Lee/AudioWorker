package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.utils.Constants;

public class PlaybackSeekFunction extends PlaybackFunction {

    private final static String TAG = Constants.packageTag("PlaybackSeekFunction");

    private final static String ATTR_TYPE = PlaybackStartFunction.ATTR_TYPE;
    private final static String ATTR_PLAYBACK_ID = PlaybackStartFunction.ATTR_PLAYBACK_ID;
    private final static String ATTR_SEEK_POSITION_MS = "seek-position-ms";
    private final static String[] ATTRS = {
            ATTR_TYPE,
            ATTR_PLAYBACK_ID,
            ATTR_SEEK_POSITION_MS
    };

    private Parameter<String> PARAM_TYPE = new Parameter<>(ATTR_TYPE, true, null);
    private Parameter<Integer> PARAM_PLAYBACK_ID = new Parameter<>(ATTR_PLAYBACK_ID, true, -1);
    private Parameter<Integer> PARAM_SEEK_POSITION_MS = new Parameter<>(ATTR_SEEK_POSITION_MS, true, 0);
    private Parameter[] PARAMS = {
            PARAM_TYPE,
            PARAM_PLAYBACK_ID,
            PARAM_SEEK_POSITION_MS
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
                case ATTR_SEEK_POSITION_MS:
                    return checkSeekPositionInMs((int) value);
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

    private boolean checkSeekPositionInMs(int position) {
        return position >= 0;
    }

    public String getPlaybackType() {
        return PARAM_TYPE.getValue();
    }

    public int getPlaybackId() {
        return PARAM_PLAYBACK_ID.getValue();
    }

    public int getSeekPositionInMs() {
        return PARAM_SEEK_POSITION_MS.getValue();
    }

    public void setPlaybackId(int id) {
        PARAM_PLAYBACK_ID.setValue(id);
    }

    public void setPlaybackType(String type) {
        PARAM_TYPE.setValue(type);
    }
    
    public void setSeekPositionInMs(int position) {
        PARAM_SEEK_POSITION_MS.setValue(position);
    }

}
