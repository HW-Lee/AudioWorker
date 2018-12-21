//
// Created by HW Lee on 28/10/2017.
//

#include <jni.h>
#include <string>
#include "FFT.h"

extern "C"
{
JNIEXPORT jstring JNICALL Java_com_google_audioworker_utils_signalproc_FFT_getVersion(
        JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(FFT::getVersion().c_str());
}

JNIEXPORT jdoubleArray JNICALL Java_com_google_audioworker_utils_signalproc_FFT_transformAbs(
        JNIEnv *env, jobject thiz, jdoubleArray jsignal) {
    jsize size = env->GetArrayLength(jsignal);
    std::vector<complexdbl> spectrum(size);
    std::vector<double> signal(size);

    double value[size];
    env->GetDoubleArrayRegion(jsignal, 0, size, value);

    for (uint32_t i = 0; i < size; i++) {
        signal[i] = value[i];
    }

    spectrum = FFT::transform<double>(signal);
    double spectrum_amp[spectrum.size()];
    for (uint32_t i = 0; i < spectrum.size(); i++) {
        spectrum_amp[i] = std::abs(spectrum[i]);
    }

    jdoubleArray jspectrum_amp = env->NewDoubleArray(spectrum.size());
    env->SetDoubleArrayRegion(jspectrum_amp, 0, spectrum.size(), spectrum_amp);

    return jspectrum_amp;
}
}
