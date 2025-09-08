package com.google.audioworker.functions.audio.record;

import android.media.MediaRecorder;

import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.Constants.Controllers.Config.AudioApi;
import com.google.audioworker.utils.Constants.Controllers.Config.PerformanceMode;

import java.util.Arrays;

public class RecordStartFunction extends RecordFunction {
    private static final String TAG = Constants.packageTag("RecordStartFunction");

    private static final String ATTR_FS = "sampling-freq";
    private static final String ATTR_NCH = "num-channels";
    private static final String ATTR_BPS = "pcm-bit-width";
    private static final String ATTR_DUMP_BUFFER_SIZE_MS = "dump-buffer-ms";
    private static final String ATTR_BTSCO_ON = "btsco-on";
    private static final String ATTR_INPUT_SRC = "input-src";
    private static final String ATTR_AUDIO_API = "audio-api";
    private static final String ATTR_AUDIO_PERF = "audio-perf";

    private static final String[] ATTRS = {
        ATTR_FS,
        ATTR_NCH,
        ATTR_BPS,
        ATTR_DUMP_BUFFER_SIZE_MS,
        ATTR_BTSCO_ON,
        ATTR_INPUT_SRC,
        ATTR_AUDIO_API,
        ATTR_AUDIO_PERF
    };

    private Parameter<Integer> PARAM_FS =
            new Parameter<>(ATTR_FS, false, Constants.RecordDefaultConfig.SAMPLING_FREQ);
    private Parameter<Integer> PARAM_NCH =
            new Parameter<>(ATTR_NCH, false, Constants.RecordDefaultConfig.NUM_CHANNELS);
    private Parameter<Integer> PARAM_BPS =
            new Parameter<>(ATTR_BPS, false, Constants.RecordDefaultConfig.BIT_PER_SAMPLE);
    private Parameter<Integer> PARAM_DUMP_BUFFER_SIZE_MS =
            new Parameter<>(
                    ATTR_DUMP_BUFFER_SIZE_MS,
                    false,
                    Constants.RecordDefaultConfig.BUFFER_SIZE_MILLIS);
    private Parameter<Boolean> PARAM_BTSCO_ON = new Parameter<>(ATTR_BTSCO_ON, false, true);
    private Parameter<Integer> PARAM_INPUT_SRC =
            new Parameter<>(ATTR_INPUT_SRC, false, Constants.RecordDefaultConfig.INPUT_SRC);
    private Parameter<Integer> PARAM_AUDIO_API =
            new Parameter<>(ATTR_AUDIO_API, false, Constants.RecordDefaultConfig.AUDIO_API);
    private Parameter<Integer> PARAM_AUDIO_PERF =
            new Parameter<>(ATTR_AUDIO_PERF, false, Constants.RecordDefaultConfig.AUDIO_PERF);
    private Parameter[] PARAMS = {
        PARAM_FS,
        PARAM_NCH,
        PARAM_BPS,
        PARAM_DUMP_BUFFER_SIZE_MS,
        PARAM_BTSCO_ON,
        PARAM_INPUT_SRC,
        PARAM_AUDIO_API,
        PARAM_AUDIO_PERF
    };

    private Parameter[] mParams;
    private String[] mAttrs;

    public RecordStartFunction() {
        mParams = Arrays.copyOf(PARAMS, PARAMS.length + super.getParameters().length);
        mAttrs = Arrays.copyOf(ATTRS, ATTRS.length + super.getAttributes().length);
        System.arraycopy(
                super.getParameters(), 0, mParams, PARAMS.length, super.getParameters().length);
        System.arraycopy(
                super.getAttributes(), 0, mAttrs, ATTRS.length, super.getAttributes().length);
    }

    @Override
    public Parameter[] getParameters() {
        return mParams;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        if (super.isValueAccepted(attr, value)) return true;

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
                case ATTR_INPUT_SRC:
                    return checkInputSrc((int) value);
                case ATTR_AUDIO_API:
                    return checkAudioApi((int) value);
                case ATTR_AUDIO_PERF:
                    return checkAudioPerf((int) value);
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
        super.setParameter(attr, value);
        if (isValueAccepted(attr, value)) {
            int idx = toIndex(attr);
            if (idx < 0) return;

            switch (attr) {
                case ATTR_BTSCO_ON:
                    PARAMS[idx].setValue(
                            "true".equals(value.toString()) | "1".equals(value.toString()));
                    return;
                default:
                    PARAMS[idx].setValue(value);
            }
        }
    }

    private int toIndex(String attr) {
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i].equals(attr)) return i;
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
            case 96000:
            case 192000:
                return true;
        }
        return false;
    }

    private boolean checkNumChannels(int nch) {
        switch (nch) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
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

    private boolean checkInputSrc(int src) {
        switch (src) {
            case MediaRecorder.AudioSource.MIC:
            case MediaRecorder.AudioSource.CAMCORDER:
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
            case MediaRecorder.AudioSource.UNPROCESSED:
            case MediaRecorder.AudioSource.VOICE_PERFORMANCE:
                return true;
        }
        return false;
    }

    private boolean checkAudioApi(int api) {
        switch (api) {
            case AudioApi.NONE:
            case AudioApi.OPENSLES:
            case AudioApi.AAUDIO:
                return true;
        }
        return false;
    }

    private boolean checkAudioPerf(int perf) {
        switch (perf) {
            case PerformanceMode.POWERSAVING:
            case PerformanceMode.LOWLATENCY:
            case PerformanceMode.NONE:
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

    public void setInputSrc(int src) {
        setParameter(ATTR_INPUT_SRC, src);
    }

    public int getInputSrc() {
        return PARAM_INPUT_SRC.getValue();
    }

    public void setAudioAPI(int api) {
        setParameter(ATTR_AUDIO_API, api);
    }

    public int getAudioAPI() {
        return PARAM_AUDIO_API.getValue();
    }

    public void setAudioPerf(int perf) {
        setParameter(ATTR_AUDIO_PERF, perf);
    }

    public int getAudioPerf() {
        return PARAM_AUDIO_PERF.getValue();
    }

    public boolean checkOpenSL() {
        return getAudioAPI() == AudioApi.OPENSLES;
    }

    public boolean checkAAudio() {
        return getAudioAPI() == AudioApi.AAUDIO;
    }

    public boolean usingExtApi() {
        return getAudioAPI() == AudioApi.AAUDIO || getAudioAPI() == AudioApi.OPENSLES;
    }
}
