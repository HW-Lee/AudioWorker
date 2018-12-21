package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.functions.audio.AudioFunction;

public abstract class PlaybackFunction extends AudioFunction {
    public final static String TASK_OFFLOAD = "offload";
    public final static String TASK_NONOFFLOAD = "non-offload";
}
