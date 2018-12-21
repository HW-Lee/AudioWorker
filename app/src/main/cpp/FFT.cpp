//
// Created by HW Lee on 28/10/2017.
//

#include "FFT.h"

std::string FFT::getVersion() { return std::string("FFT-0.9.0"); }

void bitReversalSwap(std::vector<complexdbl>& data)
{
    uint32_t i_br = 0;
    uint32_t N = (uint32_t) data.size();
    for (uint32_t i = 1; i < N-1; i++) {
        uint32_t k = N >> 1;
        while (k & i_br) {
            i_br &= ~k;
            k >>= 1;
        }
        i_br |= k;

        if (i < i_br) {
            complexdbl temp = data[i];
            data[i] = data[i_br];
            data[i_br] = temp;
        }
    }
}

std::vector<complexdbl> getTwiddleFactors(uint32_t N)
{
    std::vector<complexdbl> twiddle_factors(N);
    for (uint32_t i = 0; i < N; i++) {
        double radius = (double) i/N * 2 * M_PI;
        twiddle_factors[i] = complexdbl(cos(radius), -sin(radius));
    }

    return twiddle_factors;
}

uint32_t ceilpw2(uint32_t k)
{
    uint32_t N = 1;
    while (N < k) N <<= 1;
    return N;
}

std::vector<complexdbl> internal_FFT(std::vector<complexdbl> data, std::vector<complexdbl> twiddle)
{
    std::vector<complexdbl> buf(data);
    uint32_t N = (uint32_t) data.size();
    uint32_t twiddle_step = N;

    while (twiddle_step >>= 1) {
        for (uint32_t offset = 0; offset < N; offset+=(N/twiddle_step)) {
            for (uint32_t i = 0; i < (N/twiddle_step/2); i++) {
                complexdbl W = twiddle[i*twiddle_step];
                complexdbl f_even = buf[offset+i] * UNITARY_FACTOR;
                complexdbl f_odd = buf[offset+i+N/twiddle_step/2] * UNITARY_FACTOR;
                buf[offset+i] = f_even + W * f_odd;
                buf[offset+i+N/twiddle_step/2] = f_even - W * f_odd;
            }
        }
    }

    return buf;
}
