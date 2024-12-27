package com.google.audioworker.functions.audio.playback;

import android.media.AudioManager;
import com.google.audioworker.functions.audio.AudioFunction;
import com.google.audioworker.utils.Constants;

import java.util.ArrayList;

public class PlaybackStartFunction extends PlaybackFunction {
    private final static String TAG = Constants.packageTag("PlaybackStartFunction");

    public final static String ATTR_TYPE = "type";
    public final static String ATTR_TARGET_FREQS = "target-freqs";
    public final static String ATTR_PLAYBACK_ID = "playback-id";
    public final static String ATTR_PLAYBACK_USE_LL = "low-latency-mode";
    public final static String ATTR_AMPLITUDE = "amplitude";
    public final static String ATTR_FS = "sampling-freq";
    public final static String ATTR_NCH = "num-channels";
    public final static String ATTR_BPS = "pcm-bit-width";
    public final static String ATTR_FILE = "file";
    public final static String ATTR_STREAM_TYPE = "stream-type";
    private final static String[] ATTRS = {
            ATTR_TYPE,
            ATTR_TARGET_FREQS,
            ATTR_PLAYBACK_ID,
            ATTR_PLAYBACK_USE_LL,
            ATTR_AMPLITUDE,
            ATTR_FS,
            ATTR_NCH,
            ATTR_BPS,
            ATTR_FILE,
            ATTR_STREAM_TYPE
    };

    private Parameter<String> PARAM_TYPE = new AudioFunction.Parameter<>(ATTR_TYPE, true, null);
    private Parameter<String> PARAM_TARGET_FREQS = new AudioFunction.Parameter<>(ATTR_TARGET_FREQS, true, null);
    private Parameter<Integer> PARAM_PLAYBACK_ID = new AudioFunction.Parameter<>(ATTR_PLAYBACK_ID, true, -1);
    private Parameter<Boolean> PARAM_PLAYBACK_USE_LL = new Parameter<>(ATTR_PLAYBACK_USE_LL, false, false);
    private Parameter<Float> PARAM_AMPLITUDE = new AudioFunction.Parameter<>(ATTR_AMPLITUDE, false, Constants.PlaybackDefaultConfig.AMPLITUDE);
    private Parameter<Integer> PARAM_FS = new AudioFunction.Parameter<>(ATTR_FS, false, Constants.PlaybackDefaultConfig.SAMPLING_FREQ);
    private Parameter<Integer> PARAM_NCH = new AudioFunction.Parameter<>(ATTR_NCH, false, Constants.PlaybackDefaultConfig.NUM_CHANNELS);
    private Parameter<Integer> PARAM_BPS = new AudioFunction.Parameter<>(ATTR_BPS, false, Constants.PlaybackDefaultConfig.BIT_PER_SAMPLE);
    private Parameter<String> PARAM_FILE = new AudioFunction.Parameter<>(ATTR_FILE, false, Constants.PlaybackDefaultConfig.FILE_NAME);
    private Parameter<Integer> PARAM_STREAM_TYPE = new AudioFunction.Parameter<>(ATTR_STREAM_TYPE, false, Constants.PlaybackDefaultConfig.STREAM_TYPE);

    private Parameter[] PARAMS = {
            PARAM_TYPE,
            PARAM_TARGET_FREQS,
            PARAM_PLAYBACK_ID,
            PARAM_PLAYBACK_USE_LL,
            PARAM_AMPLITUDE,
            PARAM_FS,
            PARAM_NCH,
            PARAM_BPS,
            PARAM_FILE,
            PARAM_STREAM_TYPE
    };

    @Override
    public String[] getAttributes() {
        return ATTRS;
    }

    @Override
    public AudioFunction.Parameter[] getParameters() {
        return PARAMS;
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        try {
            switch (attr) {
                case ATTR_TYPE:
                    return checkType((String) value);
                case ATTR_TARGET_FREQS:
                    return checkTargetFreqs(value.toString());
                case ATTR_PLAYBACK_ID:
                    return checkPlaybackId((int) value);
                case ATTR_PLAYBACK_USE_LL:
                    return true;
                case ATTR_AMPLITUDE:
                    return checkAmplitude(Float.valueOf(value.toString()));
                case ATTR_FS:
                    return checkSamplingFreq((int) value);
                case ATTR_NCH:
                    return checkNumChannels((int) value);
                case ATTR_BPS:
                    return checkBitPerSample((int) value);
                case ATTR_FILE:
                    return checkFileName((String) value);
                case ATTR_STREAM_TYPE:
                    return checkStreamType((int) value);
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
            switch (attr) {
                case ATTR_TARGET_FREQS:
                    PARAM_TARGET_FREQS.setValue(value.toString());
                    return;
                case ATTR_AMPLITUDE:
                    PARAM_AMPLITUDE.setValue(Float.valueOf(value.toString()));
                    return;
                case ATTR_PLAYBACK_USE_LL:
                    PARAM_PLAYBACK_USE_LL.setValue(
                            "true".equals(value.toString()) | "1".equals(value.toString())
                    );
                    return;
                case ATTR_FILE:
                    PARAM_FILE.setValue(value.toString());
                    return;
                default:
                    int idx = toIndex(attr);
                    if (idx >= 0)
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

    private boolean checkType(String type) {
        return TASK_OFFLOAD.equals(type) || TASK_NONOFFLOAD.equals(type);
    }

    private boolean checkTargetFreqs(String freqs) {
        if (freqs.split(",").length == 0)
            return false;

        for (String freqStr : freqs.split(",")) {
            try {
                Double.parseDouble(freqStr);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private boolean checkPlaybackId(int id) {
        return id >= 0;
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
    private boolean checkFileName(String fileName) {
        switch (getPlaybackType()) {
            case TASK_NONOFFLOAD:
                return fileName.endsWith(".wav") ||
                        Constants.PlaybackDefaultConfig.FILE_NAME.equals(fileName);

            case TASK_OFFLOAD:
                return fileName.endsWith(".mp3") ||
                        fileName.endsWith(".aac") ||
                        Constants.PlaybackDefaultConfig.FILE_NAME.equals(fileName);

            default:
                return false;
        }
    }
    private boolean checkStreamType(int stream_type) {
        switch (stream_type) {
            case AudioManager.STREAM_VOICE_CALL:
            case AudioManager.STREAM_SYSTEM:
            case AudioManager.STREAM_RING:
            case AudioManager.STREAM_MUSIC:
            case AudioManager.STREAM_ALARM:
            case AudioManager.STREAM_NOTIFICATION:
            case AudioManager.STREAM_DTMF:
            case AudioManager.STREAM_ACCESSIBILITY:
                return true;
        }

        return false;
    }

    public String getPlaybackType() {
        return PARAM_TYPE.getValue();
    }

    public String getTargetFrequenciesString() {
        return PARAM_TARGET_FREQS.getValue();
    }

    public ArrayList<Double> getTargetFrequencies() {
        ArrayList<Double> freqs = new ArrayList<>();

        for (String freqStr : PARAM_TARGET_FREQS.getValue().split(",")) {
            freqs.add(Double.parseDouble(freqStr));
        }

        return freqs;
    }

    public int getPlaybackId() {
        return PARAM_PLAYBACK_ID.getValue();
    }

    public float getAmplitude() {
        return PARAM_AMPLITUDE.getValue();
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

    public boolean isLowLatencyMode() {
        return PARAM_PLAYBACK_USE_LL.getValue();
    }

    public String getPlaybackFile() {
        return PARAM_FILE.getValue();
    }

    public int getStreamType() {
        return PARAM_STREAM_TYPE.getValue();
    }

    public void setPlaybackType(String type) {
        setParameter(ATTR_TYPE, type);
    }

    public void setAmplitude(float amp) {
        setParameter(ATTR_AMPLITUDE, amp);
    }

    public void setTargetFrequency(float freq) {
        setTargetFrequencies(String.valueOf(freq));
    }

    public void setTargetFrequencies(String freqs) {
        setParameter(ATTR_TARGET_FREQS, freqs);
    }

    public void setPlaybackId(int id) {
        setParameter(ATTR_PLAYBACK_ID, id);
    }

    public void setUseLowLatency(boolean b) {
        setParameter(ATTR_PLAYBACK_USE_LL, b);
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

    public void setPlaybackFile(String file) {
        setParameter(ATTR_FILE, file);
    }

    public void setStreamType(int stream_type) {
        setParameter(ATTR_STREAM_TYPE, stream_type);
    }
}
