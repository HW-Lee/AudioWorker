package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.utils.Constants;

public class VoIPStartFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPStartFunction");

    private final static String ATTR_TARGET_FREQ = "target-freq";
    private final static String ATTR_RX_AMP = "rx-amplitude";
    private final static String ATTR_RX_FS = "rx-sampling-freq";
    private final static String ATTR_RX_NCH = "rx-num-channels";
    private final static String ATTR_RX_BPS = "rx-pcm-bit-width";
    private final static String ATTR_TX_FS = "tx-sampling-freq";
    private final static String ATTR_TX_NCH = "tx-num-channels";
    private final static String ATTR_TX_BPS = "tx-pcm-bit-width";

    private final static String[] ATTRS = {
            ATTR_TARGET_FREQ,
            ATTR_RX_AMP,
            ATTR_RX_FS,
            ATTR_RX_NCH,
            ATTR_RX_BPS,
            ATTR_TX_FS,
            ATTR_TX_NCH,
            ATTR_TX_BPS
    };

    private Parameter<Float> PARAM_TARGET_FREQ = new Parameter<>(ATTR_TARGET_FREQ, true, null);
    private Parameter<Float> PARAM_RX_AMP = new Parameter<>(ATTR_RX_AMP, false, Constants.VoIPDefaultConfig.Rx.AMPLITUDE);
    private Parameter<Integer> PARAM_RX_FS = new Parameter<>(ATTR_RX_FS, false, Constants.VoIPDefaultConfig.Rx.SAMPLINGFREQ);
    private Parameter<Integer> PARAM_RX_NCH = new Parameter<>(ATTR_RX_NCH, false, Constants.VoIPDefaultConfig.Rx.NUMCHANNELS);
    private Parameter<Integer> PARAM_RX_BPS = new Parameter<>(ATTR_RX_BPS, false, Constants.VoIPDefaultConfig.Rx.BITPERSAMPLE);
    private Parameter<Integer> PARAM_TX_FS = new Parameter<>(ATTR_TX_FS, false, Constants.VoIPDefaultConfig.Tx.SAMPLINGFREQ);
    private Parameter<Integer> PARAM_TX_NCH = new Parameter<>(ATTR_TX_NCH, false, Constants.VoIPDefaultConfig.Tx.NUMCHANNELS);
    private Parameter<Integer> PARAM_TX_BPS = new Parameter<>(ATTR_TX_BPS, false, Constants.VoIPDefaultConfig.Tx.BITPERSAMPLE);
    private Parameter[] PARAMS = {
            PARAM_TARGET_FREQ,
            PARAM_RX_AMP,
            PARAM_RX_FS,
            PARAM_RX_NCH,
            PARAM_RX_BPS,
            PARAM_TX_FS,
            PARAM_TX_NCH,
            PARAM_TX_BPS
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
                    return checkTargetFreq((Float.valueOf(value.toString())));
                case ATTR_RX_AMP:
                    return checkAmplitude(Float.valueOf(value.toString()));
                case ATTR_RX_FS:
                case ATTR_TX_FS:
                    return checkSamplingFreq((int) value);
                case ATTR_RX_NCH:
                case ATTR_TX_NCH:
                    return checkNumChannels((int) value);
                case ATTR_RX_BPS:
                case ATTR_TX_BPS:
                    return checkBitPerSample((int) value);
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

    private boolean checkTargetFreq(float freq) {
        return freq > 0 && freq < PARAM_RX_FS.getValue()/2.0f;
    }

    private boolean checkAmplitude(float amp) {
        return amp >= 0.0 && amp <= 1.0;
    }

    private boolean checkSamplingFreq(int freq) {
        switch (freq) {
            case 8000:
            case 16000:
            case 22050:
            case 24000:
            case 32000:
            case 44100:
                return true;
        }
        return false;
    }

    private boolean checkNumChannels(int nch) {
        switch (nch) {
            case 1:
            case 2:
                return true;
        }
        return false;
    }

    private boolean checkBitPerSample(int bps) {
        switch (bps) {
            case 8:
            case 16:
            case 24:
            case 32:
                return true;
        }
        return false;
    }

    public float getTargetFrequency() {
        return PARAM_TARGET_FREQ.getValue();
    }

    public float getRxAmplitude() {
        return PARAM_RX_AMP.getValue();
    }

    public int getRxSamplingFreq() {
        return PARAM_RX_FS.getValue();
    }

    public int getRxNumChannels() {
        return PARAM_RX_NCH.getValue();
    }

    public int getRxBitWidth() {
        return PARAM_RX_BPS.getValue();
    }

    public int getTxSamplingFreq() {
        return PARAM_TX_FS.getValue();
    }

    public int getTxNumChannels() {
        return PARAM_TX_NCH.getValue();
    }

    public int getTxBitWidth() {
        return PARAM_TX_BPS.getValue();
    }
}
