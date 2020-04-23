package com.google.audioworker.functions.audio.record;

import com.google.audioworker.utils.Constants;

public class RecordStartFunction extends RecordFunction {
    private final static String TAG = Constants.packageTag("RecordStartFunction");

    private final static String ATTR_FS = "sampling-freq";
    private final static String ATTR_NCH = "num-channels";
    private final static String ATTR_BPS = "pcm-bit-width";
    private final static String ATTR_DUMP_BUFFER_SIZE_MS = "dump-buffer-ms";
    private final static String ATTR_BTSCO_ON = "btsco-on";

    private final static String[] ATTRS = {
            ATTR_FS,
            ATTR_NCH,
            ATTR_BPS,
            ATTR_DUMP_BUFFER_SIZE_MS,
            ATTR_BTSCO_ON
    };

    private Parameter<Integer> PARAM_FS = new Parameter<>(ATTR_FS, false, Constants.RecordDefaultConfig.SAMPLINGFREQ);
    private Parameter<Integer> PARAM_NCH = new Parameter<>(ATTR_NCH, false, Constants.RecordDefaultConfig.NUMCHANNELS);
    private Parameter<Integer> PARAM_BPS = new Parameter<>(ATTR_BPS, false, Constants.RecordDefaultConfig.BITPERSAMPLE);
    private Parameter<Integer> PARAM_DUMP_BUFFER_SIZE_MS = new Parameter<>(ATTR_DUMP_BUFFER_SIZE_MS, false, Constants.RecordDefaultConfig.BUFFERSIZEMILLIS);
    private Parameter<Boolean> PARAM_BTSCO_ON = new Parameter<>(ATTR_BTSCO_ON, false, true);
    private Parameter[] PARAMS = {
            PARAM_FS,
            PARAM_NCH,
            PARAM_BPS,
            PARAM_DUMP_BUFFER_SIZE_MS,
            PARAM_BTSCO_ON
    };

    @Override
    public Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        try {
            switch (attr) {
                case ATTR_FS:
                    return checkSamplingFreq((int) value);
                case ATTR_NCH:
                    return checkNumChannels((int) value);
                case ATTR_BPS:
                    return checkBitPerSample((int) value);
                case ATTR_DUMP_BUFFER_SIZE_MS:
                    return (int) value >= 0;
                case ATTR_BTSCO_ON:
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
                case ATTR_BTSCO_ON:
                    PARAMS[idx].setValue(
                            "true".equals(value.toString()) | "1".equals(value.toString())
                    );
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

    private boolean checkSamplingFreq(int freq) {
        switch (freq) {
            case 8000:
            case 16000:
            case 22050:
            case 24000:
            case 32000:
            case 44100:
            case 48000:
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

    public int getSamplingFreq() {
        return PARAM_FS.getValue();
    }

    public int getNumChannels() {
        return PARAM_NCH.getValue();
    }

    public int getBitWidth() {
        return PARAM_BPS.getValue();
    }

    public int getDumpBufferSizeMs() {
        return PARAM_DUMP_BUFFER_SIZE_MS.getValue();
    }

    public void setSamplingFreq(int freq) {
        setParameter(ATTR_FS, freq);
    }

    public void setNumChannels(int nch) {
        setParameter(ATTR_NCH, nch);
    }

    public void setBitWidth(int bps) {
        setParameter(ATTR_BPS, bps);
    }

    public void setDumpBufferSizeMs(int ms) {
        setParameter(ATTR_DUMP_BUFFER_SIZE_MS, ms);
    }

    public boolean bluetoothScoOn() {
        return PARAM_BTSCO_ON.getValue();
    }
}
