package com.google.audioworker.functions.audio.playback;

import com.google.audioworker.functions.audio.AudioFunction;

public abstract class PlaybackFunction extends AudioFunction {
    public static final String TASK_OFFLOAD = "offload";
    public static final String TASK_NONOFFLOAD = "non-offload";
}
