//
// Created by HW Lee on 10/01/2019.
//

#include <jni.h>
#include <string>
#include "PeakDetector.h"
#include "Matrix.h"

extern "C"
{
JNIEXPORT jstring JNICALL Java_com_google_audioworker_utils_signalproc_PeakDetector_getVersion(
        JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("v0.1");
}

JNIEXPORT jdoubleArray JNICALL Java_com_google_audioworker_utils_signalproc_PeakDetector_extractQuadFeature(
        JNIEnv *env, jobject thiz, jdoubleArray jdata, jintArray indices, jdouble min_norm_factor, jint num_points, jint step) {
    peak_detector_params_t params = {
            .targets = std::vector<int>(),
            .step = step,
            .num_points = num_points,
    };

    std::vector<double> data;
    double norm_factor = min_norm_factor;

    {
        int size = env->GetArrayLength(jdata);
        double value[size];
        env->GetDoubleArrayRegion(jdata, 0, size, value);

        data = std::vector<double>(size);
        for (int i = 0; i < size; i++) {
            data[i] = value[i];
            if (data[i] > norm_factor)
                norm_factor = data[i];
        }
    }

    {
        int size = env->GetArrayLength(indices);
        int value[size];
        env->GetIntArrayRegion(indices, 0, size, value);

        for (int i = 0; i < size; i++) {
            params.targets.push_back(value[i]);
        }
    }

    Matrix<double> feature = PeakDetector::quadratic_feature<double>(data, norm_factor, params);
    jdoubleArray jfeature = env->NewDoubleArray(feature.getDimension().ncols * feature.getDimension().nrows);
    for (int i = 0; i < feature.getDimension().nrows; i++) {
        env->SetDoubleArrayRegion(jfeature, i * feature.getDimension().ncols, feature.getDimension().ncols, feature[i]);
    }

    return jfeature;
}
}
