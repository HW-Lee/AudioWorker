package com.google.audioworker.utils.signalproc;

import com.google.audioworker.utils.Constants;

public class FFT {
    final static private String TAG = Constants.packageTag("FFT");

    static {
        System.loadLibrary("native-fft");
    }

    native static public double[] transformAbs(double[] signal);
    native static public String getVersion();
}
