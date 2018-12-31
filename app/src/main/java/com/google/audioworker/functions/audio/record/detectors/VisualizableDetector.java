package com.google.audioworker.functions.audio.record.detectors;

public abstract class VisualizableDetector extends DetectorBase implements DetectorBase.Visualizable {
    public VisualizableDetector(DetectionListener l) {
        super(l);
    }

    public VisualizableDetector(DetectionListener l, String params) {
        super(l, params);
    }
}
