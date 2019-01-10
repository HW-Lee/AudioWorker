#include <iostream>
#include <vector>
#include "Matrix.h"

#ifndef __PEAK_DETECTOR_HEADER__
#define __PEAK_DETECTOR_HEADER__

template<class T> class Peak {
public:
    Peak() { this->idx = -1; this->value = -1; }
    Peak(int idx, T value) { this->idx = idx; this->value = value; }

    int get_index() { return idx; }
    void set_index(int idx) { this->idx = idx; }

    T get_value() { return value; }
    void set_value(T value) { this->value = value; }

private:
    int idx;
    T value;
};

typedef struct {
    std::vector<int> targets;
    int step;
    int num_points;
} peak_detector_params_t;

class PeakDetector {
public:
    template<class T> static std::vector< Peak<T> > find_peak(std::vector<T> data, T norm_factor, peak_detector_params_t params);
    template<class T> static Matrix<double> quadratic_feature(std::vector<T> data, T norm_factor, peak_detector_params_t params);

private:
    static std::vector< Peak<double> > _find_peak(std::vector<double> data, peak_detector_params_t params);
    static Matrix<double> _quadratic_feature(std::vector<double> data, peak_detector_params_t params);
};

template<class T>
std::vector< Peak<T> > PeakDetector::find_peak(std::vector<T> data, T norm_factor, peak_detector_params_t params) {
    std::vector<double> _data(data.size());
    for (int i = 0; i < data.size(); i++) {
        _data[i] = (double) data[i] / norm_factor;
    }

    std::vector< Peak<double> > _peaks = PeakDetector::_find_peak(_data, params);
    std::vector< Peak<T> > peaks(_peaks.size());

    for (int i = 0; i < peaks.size(); i++) {
        peaks[i].set_index(_peaks[i].get_index());
        peaks[i].set_value(_peaks[i].get_value());
    }

    return peaks;
}

template<class T>
Matrix<double> PeakDetector::quadratic_feature(std::vector<T> data, T norm_factor, peak_detector_params_t params) {
    std::vector<double> _data(data.size());
    for (int i = 0; i < data.size(); i++) {
        _data[i] = (double) data[i] / norm_factor;
    }

    for (int i = 0; i < params.targets.size(); i++) {
        int target_idx = params.targets[i];
        double norm_factor_l = 0;
        for (int j = 0; j < params.num_points; j++) {
            if (_data[target_idx - params.num_points/2 + j] > norm_factor_l)
                norm_factor_l = _data[target_idx - params.num_points/2 + j];
        }

        if (norm_factor_l < (double) norm_factor/10.0)
            continue;

        for (int j = 0; j < params.num_points; j++)
            _data[target_idx - params.num_points/2 + j] /= norm_factor_l;
    }

    return PeakDetector::_quadratic_feature(_data, params);
}

#endif
