package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPConfigFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPConfigFunction");

    private final static String ATTR_TARGET_FREQ = "rx-target-freq";
    private final static String ATTR_RX_AMP = "rx-amplitude";

    private final static String[] ATTRS = {
            ATTR_TARGET_FREQ,
            ATTR_RX_AMP
    };

    private Parameter<Float> PARAM_TARGET_FREQ = new Parameter<>(ATTR_TARGET_FREQ, false, -1f);
    private Parameter<Float> PARAM_RX_AMP = new Parameter<>(ATTR_RX_AMP, false, -1f);
    private Parameter[] PARAMS = {
            PARAM_TARGET_FREQ,
            PARAM_RX_AMP
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

    private boolean checkTargetFrequency(float freq) {
        return freq > 0;
    }

    private boolean checkAmplitude(float amp) {
        return amp >= 0.0 && amp <= 1.0;
    }

    public float getTargetFrequency() {
        return PARAM_TARGET_FREQ.getValue();
    }

    public float getRxAmplitude() {
        return PARAM_RX_AMP.getValue();
    }
}
