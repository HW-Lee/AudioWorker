package com.google.audioworker.utils.signalproc;

import com.google.audioworker.utils.Constants;

public class FFT {
    private static final String TAG = Constants.packageTag("FFT");

    static {
        System.loadLibrary("native-fft");
    }

    public static native double[] transformAbs(double[] signal);

    public static native String getVersion();
}
