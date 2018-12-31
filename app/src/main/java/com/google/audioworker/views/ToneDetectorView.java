package com.google.audioworker.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.google.audioworker.functions.audio.record.detectors.DetectorBase;

public class ToneDetectorView extends LinearLayout {
    public ToneDetectorView(Context context) {
        super(context);
        init();
    }

    public ToneDetectorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToneDetectorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    static public ToneDetectorView createView(Context ctx, String token, DetectorBase detector) {
        return null;
    }
}
