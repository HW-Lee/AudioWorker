#include "PeakDetector.h"
#include <iostream>
#include <vector>
#include "Matrix.h"

#ifndef __PEAK_DETECTOR_CPP__
#define __PEAK_DETECTOR_CPP__

std::vector< Peak<double> > PeakDetector::_find_peak(std::vector<double> data, peak_detector_params_t params) {
    std::vector< Peak<double> > peaks(0);
    return peaks;
}

Matrix<double> PeakDetector::_quadratic_feature(std::vector<double> data, peak_detector_params_t params) {
    Matrix<double> fmat(3 + params.num_points, params.targets.size());

    for (int i = 0; i < params.targets.size(); i++) {
        int target = params.targets[i];
        Matrix<double> A(params.num_points, 3);
        Matrix<double> y(params.num_points, 1);

        for (int j = 0; j < params.num_points; j++) {
            int idx = target - (params.num_points/2 - j)*params.step;
            double v = 1;
            for (int k = 0; k < 3; k++) {
                A[j][2-k] = v;
                v *= (idx - target);
            }
            y[j][0] = data[idx];
        }

        Matrix<double> x = (A.transpose() * A).pinv() * A.transpose() * y;
        for (int j = 0; j < 3; j++)
            fmat[j][i] = x[j][0];
        for (int j = 0; j < params.num_points; j++)
            fmat[j+3][i] = y[j][0];
    }

    return fmat;
}

#endif
