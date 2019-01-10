package com.google.audioworker.functions.audio.record.detectors;

import android.os.Handler;
import android.view.View;

import com.google.audioworker.functions.audio.record.RecordStartFunction;


public abstract class VisualizableDetector extends DetectorBase implements DetectorBase.Visualizable {
    abstract public <T extends View & DetectionListener & DetectorBindable> void bindDetectorView(T v);

    public interface DetectorBindable {
        Handler getDetectorViewHandler();
    }

    public VisualizableDetector(DetectionListener l, RecordStartFunction function, String params) {
        super(l, function, params);
    }
}
