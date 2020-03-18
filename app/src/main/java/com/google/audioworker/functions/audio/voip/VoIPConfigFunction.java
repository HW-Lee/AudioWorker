package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPConfigFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPConfigFunction");

    private final static String ATTR_TARGET_FREQ = "rx-target-freq";
    private final static String ATTR_RX_AMP = "rx-amplitude";
    private final static String ATTR_RX_USE_SPKR = "rx-use-spkr";

    private final static String[] ATTRS = {
            ATTR_TARGET_FREQ,
            ATTR_RX_AMP,
            ATTR_RX_USE_SPKR
    };

    private Parameter<Float> PARAM_TARGET_FREQ = new Parameter<>(ATTR_TARGET_FREQ, false, -1f);
    private Parameter<Float> PARAM_RX_AMP = new Parameter<>(ATTR_RX_AMP, false, -1f);
    private Parameter<Boolean> PARAM_RX_USE_SPKR = new Parameter<>(ATTR_RX_USE_SPKR, false, false);
    private Parameter[] PARAMS = {
            PARAM_TARGET_FREQ,
            PARAM_RX_AMP,
            PARAM_RX_USE_SPKR
    };

    @Override
    public Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        try {
            switch (attr) {
                case ATTR_TARGET_FREQ:
                    return checkTargetFrequency((Float.valueOf(value.toString())));
                case ATTR_RX_AMP:
                    return checkAmplitude(Float.valueOf(value.toString()));
                case ATTR_RX_USE_SPKR:
                    return true;
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
            if (idx < 0)
                return;

            switch(attr) {
                case ATTR_RX_USE_SPKR:
                    PARAMS[idx].setValue(Boolean.valueOf(value.toString()));
                    return;
                default:
                    PARAMS[idx].setValue(value);
            }
        }
    }

    private int toIndex(String attr) {
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i].equals(attr))
                return i;
        }
        return -1;
    }

    private boolean checkTargetFrequency(float freq) {
        return freq > 0 || freq == -1f;
    }

    private boolean checkAmplitude(float amp) {
        return amp >= 0.0 && amp <= 1.0 || amp == -1f;
    }

    public float getTargetFrequency() {
        return PARAM_TARGET_FREQ.getValue();
    }

    public float getRxAmplitude() {
        return PARAM_RX_AMP.getValue();
    }

    public boolean switchSpeakerPhone() {
        return PARAM_RX_USE_SPKR.getValue();
    }
}
