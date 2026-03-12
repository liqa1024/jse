/*
    Copyright 2022 Zheyong Fan, Junjie Wang, Eric Lindgren
    This file is part of NEP_CPU.
    NEP_CPU is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    NEP_CPU is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with NEP_CPU.  If not, see <http://www.gnu.org/licenses/>.
*/

/*----------------------------------------------------------------------------80
A CPU implementation of the neuroevolution potential (NEP)
Ref: Zheyong Fan et al., Neuroevolution machine learning potentials:
Combining high accuracy and low cost in atomistic simulations and application to
heat transport, Phys. Rev. B. 104, 104309 (2021).
------------------------------------------------------------------------------*/

/**
 * This file has been simplified and modified to fit the implementation in jse
 * @author liqa
 */

#ifndef NEP_CORE_H
#define NEP_CORE_H

#include <algorithm>
#include <cmath>

namespace JSE_NEP {

static constexpr int MAX_NEURON = 120; // maximum number of neurons in the hidden layer
static constexpr int MN = 1000; // maximum number of neighbors for one atom
static constexpr int NUM_OF_ABC = 80; // 3 + 5 + 7 + 9 + 11 + 13 + 15 + 17 for L_max = 8
static constexpr int MAX_NUM_N = 17; // basis_size_radial+1 = 16+1
static constexpr int MAX_DIM = 103;
static constexpr int MAX_DIM_ANGULAR = 90;
static constexpr double C3B[NUM_OF_ABC] = {
    0.238732414637843, 0.119366207318922, 0.119366207318922, 0.099471839432435, 0.596831036594608,
    0.596831036594608, 0.149207759148652, 0.149207759148652, 0.139260575205408, 0.104445431404056,
    0.104445431404056, 1.044454314040563, 1.044454314040563, 0.174075719006761, 0.174075719006761,
    0.011190581936149, 0.223811638722978, 0.223811638722978, 0.111905819361489, 0.111905819361489,
    1.566681471060845, 1.566681471060845, 0.195835183882606, 0.195835183882606, 0.013677377921960,
    0.102580334414698, 0.102580334414698, 2.872249363611549, 2.872249363611549, 0.119677056817148,
    0.119677056817148, 2.154187022708661, 2.154187022708661, 0.215418702270866, 0.215418702270866,
    0.004041043476943, 0.169723826031592, 0.169723826031592, 0.106077391269745, 0.106077391269745,
    0.424309565078979, 0.424309565078979, 0.127292869523694, 0.127292869523694, 2.800443129521260,
    2.800443129521260, 0.233370260793438, 0.233370260793438, 0.004662742473395, 0.004079899664221,
    0.004079899664221, 0.024479397985326, 0.024479397985326, 0.012239698992663, 0.012239698992663,
    0.538546755677165, 0.538546755677165, 0.134636688919291, 0.134636688919291, 3.500553911901575,
    3.500553911901575, 0.250039565135827, 0.250039565135827, 0.000082569397966, 0.005944996653579,
    0.005944996653579, 0.104037441437634, 0.104037441437634, 0.762941237209318, 0.762941237209318,
    0.114441185581398, 0.114441185581398, 5.950941650232678, 5.950941650232678, 0.141689086910302,
    0.141689086910302, 4.250672607309055, 4.250672607309055, 0.265667037956816, 0.265667037956816
};
static constexpr double C4B[5] = {
    -0.007499480826664, -0.134990654879954, 0.067495327439977, 0.404971964639861, -0.809943929279723
};
static constexpr double C5B[3] = {0.026596810706114, 0.053193621412227, 0.026596810706114};

static constexpr double Z_COEFFICIENT_1[2][2] = {{0.0, 1.0}, {1.0, 0.0}};

static constexpr double Z_COEFFICIENT_2[3][3] = {{-1.0, 0.0, 3.0}, {0.0, 1.0, 0.0}, {1.0, 0.0, 0.0}};

static constexpr double Z_COEFFICIENT_3[4][4] = {
    {0.0, -3.0, 0.0, 5.0}, {-1.0, 0.0, 5.0, 0.0}, {0.0, 1.0, 0.0, 0.0}, {1.0, 0.0, 0.0, 0.0}
};

static constexpr double Z_COEFFICIENT_4[5][5] = {
    {3.0, 0.0, -30.0, 0.0, 35.0},
    {0.0, -3.0, 0.0, 7.0, 0.0},
    {-1.0, 0.0, 7.0, 0.0, 0.0},
    {0.0, 1.0, 0.0, 0.0, 0.0},
    {1.0, 0.0, 0.0, 0.0, 0.0}
};

static constexpr double Z_COEFFICIENT_5[6][6] = {
    {0.0, 15.0, 0.0, -70.0, 0.0, 63.0}, {1.0, 0.0, -14.0, 0.0, 21.0, 0.0},
    {0.0, -1.0, 0.0, 3.0, 0.0, 0.0},    {-1.0, 0.0, 9.0, 0.0, 0.0, 0.0},
    {0.0, 1.0, 0.0, 0.0, 0.0, 0.0},     {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}
};

static constexpr double Z_COEFFICIENT_6[7][7] = {
    {-5.0, 0.0, 105.0, 0.0, -315.0, 0.0, 231.0}, {0.0, 5.0, 0.0, -30.0, 0.0, 33.0, 0.0},
    {1.0, 0.0, -18.0, 0.0, 33.0, 0.0, 0.0},      {0.0, -3.0, 0.0, 11.0, 0.0, 0.0, 0.0},
    {-1.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0},       {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
};

static constexpr double Z_COEFFICIENT_7[8][8] = {
    {0.0, -35.0, 0.0, 315.0, 0.0, -693.0, 0.0, 429.0},
    {-5.0, 0.0, 135.0, 0.0, -495.0, 0.0, 429.0, 0.0},
    {0.0, 15.0, 0.0, -110.0, 0.0, 143.0, 0.0, 0.0},
    {3.0, 0.0, -66.0, 0.0, 143.0, 0.0, 0.0, 0.0},
    {0.0, -3.0, 0.0, 13.0, 0.0, 0.0, 0.0, 0.0},
    {-1.0, 0.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
};

static constexpr double Z_COEFFICIENT_8[9][9] = {
    {35.0, 0.0, -1260.0, 0.0, 6930.0, 0.0, -12012.0, 0.0, 6435.0},
    {0.0, -35.0, 0.0, 385.0, 0.0, -1001.0, 0.0, 715.0, 0.0},
    {-1.0, 0.0, 33.0, 0.0, -143.0, 0.0, 143.0, 0.0, 0.0},
    {0.0, 3.0, 0.0, -26.0, 0.0, 39.0, 0.0, 0.0, 0.0},
    {1.0, 0.0, -26.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0},
    {0.0, -1.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {-1.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
    {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
};

static constexpr double K_C_SP = 14.399645; // 1/(4*PI*epsilon_0)
static constexpr double PI = 3.141592653589793;
static constexpr double PI_HALF = 1.570796326794897;
static constexpr int NUM_ELEMENTS = 94;
static constexpr double COVALENT_RADIUS[NUM_ELEMENTS] = {
    0.426667, 0.613333, 1.6,     1.25333, 1.02667, 1.0,     0.946667, 0.84,    0.853333, 0.893333,
    1.86667,  1.66667,  1.50667, 1.38667, 1.46667, 1.36,    1.32,     1.28,    2.34667,  2.05333,
    1.77333,  1.62667,  1.61333, 1.46667, 1.42667, 1.38667, 1.33333,  1.32,    1.34667,  1.45333,
    1.49333,  1.45333,  1.53333, 1.46667, 1.52,    1.56,    2.52,     2.22667, 1.96,     1.85333,
    1.76,     1.65333,  1.53333, 1.50667, 1.50667, 1.44,    1.53333,  1.64,    1.70667,  1.68,
    1.68,     1.64,     1.76,    1.74667, 2.78667, 2.34667, 2.16,     1.96,    2.10667,  2.09333,
    2.08,     2.06667,  2.01333, 2.02667, 2.01333, 2.0,     1.98667,  1.98667, 1.97333,  2.04,
    1.94667,  1.82667,  1.74667, 1.64,    1.57333, 1.54667, 1.48,     1.49333, 1.50667,  1.76,
    1.73333,  1.73333,  1.81333, 1.74667, 1.84,    1.89333, 2.68,     2.41333, 2.22667,  2.10667,
    2.02667,  2.04,     2.05333, 2.06667
};

static void complex_product(const double a, const double b, double &real_part, double &imag_part) {
    const double real_temp = real_part;
    real_part = a * real_temp - b * imag_part;
    imag_part = a * imag_part + b * real_temp;
}

template <int DIM, int NUM_NEURONS1, int NEED_B_PROJ>
static void apply_ann_one_layer(
    const double *w0,
    const double *b0,
    const double *w1,
    const double *b1,
    double *q,
    double &energy,
    double *energy_derivative,
    double *latent_space,
    double *B_projection) {
    for (int n = 0; n < NUM_NEURONS1; ++n) {
        double w0_times_q = 0.0;
        for (int d = 0; d < DIM; ++d) {
            w0_times_q += w0[n * DIM + d] * q[d];
        }
        double x1 = tanh(w0_times_q - b0[n]);
        double tan_der = 1.0 - x1 * x1;

        if (NEED_B_PROJ) {
            // calculate B_projection:
            // dE/dw0
            for (int d = 0; d < DIM; ++d)
                B_projection[n * (DIM + 2) + d] = tan_der * q[d] * w1[n];
            // dE/db0
            B_projection[n * (DIM + 2) + DIM] = -tan_der * w1[n];
            // dE/dw1
            B_projection[n * (DIM + 2) + DIM + 1] = x1;
        }

        latent_space[n] = w1[n] * x1; // also try x1
        energy += w1[n] * x1;
        for (int d = 0; d < DIM; ++d) {
            double y1 = tan_der * w0[n * DIM + d];
            energy_derivative[d] += w1[n] * y1;
        }
    }
    energy -= b1[0];
}

template <int DIM, int NUM_NEURONS1>
static void apply_ann_one_layer_nep5(
    const double *w0,
    const double *b0,
    const double *w1,
    const double *b1,
    double *q,
    double &energy,
    double *energy_derivative,
    double *latent_space) {
    for (int n = 0; n < NUM_NEURONS1; ++n) {
        double w0_times_q = 0.0;
        for (int d = 0; d < DIM; ++d) {
            w0_times_q += w0[n * DIM + d] * q[d];
        }
        double x1 = tanh(w0_times_q - b0[n]);
        latent_space[n] = w1[n] * x1; // also try x1
        energy += w1[n] * x1;
        for (int d = 0; d < DIM; ++d) {
            double y1 = (1.0 - x1 * x1) * w0[n * DIM + d];
            energy_derivative[d] += w1[n] * y1;
        }
    }
    energy -= w1[NUM_NEURONS1] + b1[0]; // typewise bias + common bias
}

static void find_fc(double rc, double rcinv, double d12, double &fc) {
    if (d12 < rc) {
        double x = d12 * rcinv;
        fc = 0.5 * cos(PI * x) + 0.5;
    }
    else {
        fc = 0.0;
    }
}

static void find_fc_and_fcp(double rc, double rcinv, double d12, double &fc, double &fcp) {
    if (d12 < rc) {
        double x = d12 * rcinv;
        fc = 0.5 * cos(PI * x) + 0.5;
        fcp = -PI_HALF * sin(PI * x);
        fcp *= rcinv;
    }
    else {
        fc = 0.0;
        fcp = 0.0;
    }
}

static void find_fc_and_fcp_zbl(double r1, double r2, double d12, double &fc, double &fcp) {
    if (d12 < r1) {
        fc = 1.0;
        fcp = 0.0;
    }
    else if (d12 < r2) {
        double pi_factor = PI / (r2 - r1);
        fc = cos(pi_factor * (d12 - r1)) * 0.5 + 0.5;
        fcp = -sin(pi_factor * (d12 - r1)) * pi_factor * 0.5;
    }
    else {
        fc = 0.0;
        fcp = 0.0;
    }
}

static void find_phi_and_phip_zbl(double a, double b, double x, double &phi, double &phip) {
    double tmp = a * exp(-b * x);
    phi += tmp;
    phip -= b * tmp;
}

static void find_f_and_fp_zbl(
    double zizj,
    double a_inv,
    double rc_inner,
    double rc_outer,
    double d12,
    double d12inv,
    double &f,
    double &fp) {
    double x = d12 * a_inv;
    f = fp = 0.0;
    double Zbl_para[8] = {0.18175, 3.1998, 0.50986, 0.94229, 0.28022, 0.4029, 0.02817, 0.20162};
    find_phi_and_phip_zbl(Zbl_para[0], Zbl_para[1], x, f, fp);
    find_phi_and_phip_zbl(Zbl_para[2], Zbl_para[3], x, f, fp);
    find_phi_and_phip_zbl(Zbl_para[4], Zbl_para[5], x, f, fp);
    find_phi_and_phip_zbl(Zbl_para[6], Zbl_para[7], x, f, fp);
    f *= zizj;
    fp *= zizj * a_inv;
    fp = fp * d12inv - f * d12inv * d12inv;
    f *= d12inv;
    double fc, fcp;
    find_fc_and_fcp_zbl(rc_inner, rc_outer, d12, fc, fcp);
    fp = fp * fc + f * fcp;
    f *= fc;
}

static void find_f_and_fp_zbl(
    double *zbl_para, double zizj, double a_inv, double d12, double d12inv, double &f, double &fp) {
    double x = d12 * a_inv;
    f = fp = 0.0;
    find_phi_and_phip_zbl(zbl_para[2], zbl_para[3], x, f, fp);
    find_phi_and_phip_zbl(zbl_para[4], zbl_para[5], x, f, fp);
    find_phi_and_phip_zbl(zbl_para[6], zbl_para[7], x, f, fp);
    find_phi_and_phip_zbl(zbl_para[8], zbl_para[9], x, f, fp);
    f *= zizj;
    fp *= zizj * a_inv;
    fp = fp * d12inv - f * d12inv * d12inv;
    f *= d12inv;
    double fc, fcp;
    find_fc_and_fcp_zbl(zbl_para[0], zbl_para[1], d12, fc, fcp);
    fp = fp * fc + f * fcp;
    f *= fc;
}

template <int N>
static void find_fn(const double rcinv, const double d12, const double fc12, double &fn) {
    if (N == 0) {
        fn = fc12;
    }
    else if (N == 1) {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        fn = (x + 1.0) * 0.5 * fc12;
    }
    else {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        double t0 = 1.0;
        double t1 = x;
        double t2;
        for (int m = 2; m <= N; ++m) {
            t2 = 2.0 * x * t1 - t0;
            t0 = t1;
            t1 = t2;
        }
        fn = (t2 + 1.0) * 0.5 * fc12;
    }
}

template <int N>
static void find_fn_and_fnp(
    const double rcinv,
    const double d12,
    const double fc12,
    const double fcp12,
    double &fn,
    double &fnp) {
    if (N == 0) {
        fn = fc12;
        fnp = fcp12;
    }
    else if (N == 1) {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        fn = (x + 1.0) * 0.5;
        fnp = 2.0 * (d12 * rcinv - 1.0) * rcinv * fc12 + fn * fcp12;
        fn *= fc12;
    }
    else {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        double t0 = 1.0;
        double t1 = x;
        double t2;
        double u0 = 1.0;
        double u1 = 2.0 * x;
        double u2;
        for (int m = 2; m <= N; ++m) {
            t2 = 2.0 * x * t1 - t0;
            t0 = t1;
            t1 = t2;
            u2 = 2.0 * x * u1 - u0;
            u0 = u1;
            u1 = u2;
        }
        fn = (t2 + 1.0) * 0.5;
        fnp = N * u0 * 2.0 * (d12 * rcinv - 1.0) * rcinv;
        fnp = fnp * fc12 + fn * fcp12;
        fn *= fc12;
    }
}

template <int NMAX>
static void find_fn(const double rcinv, const double d12, const double fc12, double *fn) {
    double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
    fn[0] = 1.0;
    fn[1] = x;
    for (int m = 2; m <= NMAX; ++m) {
        fn[m] = 2.0 * x * fn[m - 1] - fn[m - 2];
    }
    for (int m = 0; m <= NMAX; ++m) {
        fn[m] = (fn[m] + 1.0) * 0.5 * fc12;
    }
}

template <int NMAX>
static void find_fn_and_fnp(
    const double rcinv,
    const double d12,
    const double fc12,
    const double fcp12,
    double *fn,
    double *fnp) {
    double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
    fn[0] = 1.0;
    fnp[0] = 0.0;
    fn[1] = x;
    fnp[1] = 1.0;
    double u0 = 1.0;
    double u1 = 2.0 * x;
    double u2;
    for (int m = 2; m <= NMAX; ++m) {
        fn[m] = 2.0 * x * fn[m - 1] - fn[m - 2];
        fnp[m] = m * u1;
        u2 = 2.0 * x * u1 - u0;
        u0 = u1;
        u1 = u2;
    }
    for (int m = 0; m <= NMAX; ++m) {
        fn[m] = (fn[m] + 1.0) * 0.5;
        fnp[m] *= 2.0 * (d12 * rcinv - 1.0) * rcinv;
        fnp[m] = fnp[m] * fc12 + fn[m] * fcp12;
        fn[m] *= fc12;
    }
}

static void get_f12_4body(
    const double d12,
    const double d12inv,
    const double fn,
    const double fnp,
    const double Fp,
    const double *s,
    const double *r12,
    double *f12) {
    double fn_factor = Fp * fn;
    double fnp_factor = Fp * fnp * d12inv;
    double y20 = (3.0 * r12[2] * r12[2] - d12 * d12);

    // derivative wrt s[0]
    double tmp0 = C4B[0] * 3.0 * s[0] * s[0] + C4B[1] * (s[1] * s[1] + s[2] * s[2]) +
        C4B[2] * (s[3] * s[3] + s[4] * s[4]);
    double tmp1 = tmp0 * y20 * fnp_factor;
    double tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0] - tmp2 * 2.0 * r12[0];
    f12[1] += tmp1 * r12[1] - tmp2 * 2.0 * r12[1];
    f12[2] += tmp1 * r12[2] + tmp2 * 4.0 * r12[2];

    // derivative wrt s[1]
    tmp0 = C4B[1] * s[0] * s[1] * 2.0 - C4B[3] * s[3] * s[1] * 2.0 + C4B[4] * s[2] * s[4];
    tmp1 = tmp0 * r12[0] * r12[2] * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0] + tmp2 * r12[2];
    f12[1] += tmp1 * r12[1];
    f12[2] += tmp1 * r12[2] + tmp2 * r12[0];

    // derivative wrt s[2]
    tmp0 = C4B[1] * s[0] * s[2] * 2.0 + C4B[3] * s[3] * s[2] * 2.0 + C4B[4] * s[1] * s[4];
    tmp1 = tmp0 * r12[1] * r12[2] * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0];
    f12[1] += tmp1 * r12[1] + tmp2 * r12[2];
    f12[2] += tmp1 * r12[2] + tmp2 * r12[1];

    // derivative wrt s[3]
    tmp0 = C4B[2] * s[0] * s[3] * 2.0 + C4B[3] * (s[2] * s[2] - s[1] * s[1]);
    tmp1 = tmp0 * (r12[0] * r12[0] - r12[1] * r12[1]) * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0] + tmp2 * 2.0 * r12[0];
    f12[1] += tmp1 * r12[1] - tmp2 * 2.0 * r12[1];
    f12[2] += tmp1 * r12[2];

    // derivative wrt s[4]
    tmp0 = C4B[2] * s[0] * s[4] * 2.0 + C4B[4] * s[1] * s[2];
    tmp1 = tmp0 * (2.0 * r12[0] * r12[1]) * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0] + tmp2 * 2.0 * r12[1];
    f12[1] += tmp1 * r12[1] + tmp2 * 2.0 * r12[0];
    f12[2] += tmp1 * r12[2];
}

static void get_f12_5body(
    const double d12,
    const double d12inv,
    const double fn,
    const double fnp,
    const double Fp,
    const double *s,
    const double *r12,
    double *f12) {
    double fn_factor = Fp * fn;
    double fnp_factor = Fp * fnp * d12inv;
    double s1_sq_plus_s2_sq = s[1] * s[1] + s[2] * s[2];

    // derivative wrt s[0]
    double tmp0 = C5B[0] * 4.0 * s[0] * s[0] * s[0] + C5B[1] * s1_sq_plus_s2_sq * 2.0 * s[0];
    double tmp1 = tmp0 * r12[2] * fnp_factor;
    double tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0];
    f12[1] += tmp1 * r12[1];
    f12[2] += tmp1 * r12[2] + tmp2;

    // derivative wrt s[1]
    tmp0 = C5B[1] * s[0] * s[0] * s[1] * 2.0 + C5B[2] * s1_sq_plus_s2_sq * s[1] * 4.0;
    tmp1 = tmp0 * r12[0] * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0] + tmp2;
    f12[1] += tmp1 * r12[1];
    f12[2] += tmp1 * r12[2];

    // derivative wrt s[2]
    tmp0 = C5B[1] * s[0] * s[0] * s[2] * 2.0 + C5B[2] * s1_sq_plus_s2_sq * s[2] * 4.0;
    tmp1 = tmp0 * r12[1] * fnp_factor;
    tmp2 = tmp0 * fn_factor;
    f12[0] += tmp1 * r12[0];
    f12[1] += tmp1 * r12[1] + tmp2;
    f12[2] += tmp1 * r12[2];
}

template <int L, int NMAX_A>
static void calculate_s_one(const int n, const double *Fp, const double *sum_fxyz, double *s) {
    constexpr int n_max_angular_plus_1 = NMAX_A + 1;
    constexpr int L_minus_1 = L - 1;
    constexpr int L_twice_plus_1 = 2 * L + 1;
    constexpr int L_square_minus_1 = L * L - 1;
    double Fp_factor = 2.0 * Fp[L_minus_1 * n_max_angular_plus_1 + n];
    s[0] = sum_fxyz[n * NUM_OF_ABC + L_square_minus_1] * C3B[L_square_minus_1] * Fp_factor;
    Fp_factor *= 2.0;
    for (int k = 1; k < L_twice_plus_1; ++k) {
        s[k] = sum_fxyz[n * NUM_OF_ABC + L_square_minus_1 + k] * C3B[L_square_minus_1 + k] * Fp_factor;
    }
}

template <int L>
static void accumulate_f12_one(
    const double d12inv,
    const double fn,
    const double fnp,
    const double *s,
    const double *r12,
    double *f12) {
    const double dx[3] = {
        (1.0 - r12[0] * r12[0]) * d12inv, -r12[0] * r12[1] * d12inv, -r12[0] * r12[2] * d12inv
    };
    const double dy[3] = {
        -r12[0] * r12[1] * d12inv, (1.0 - r12[1] * r12[1]) * d12inv, -r12[1] * r12[2] * d12inv
    };
    const double dz[3] = {
        -r12[0] * r12[2] * d12inv, -r12[1] * r12[2] * d12inv, (1.0 - r12[2] * r12[2]) * d12inv
    };

    double z_pow[L + 1] = {1.0};
    for (int n = 1; n <= L; ++n) {
        z_pow[n] = r12[2] * z_pow[n - 1];
    }

    double real_part = 1.0;
    double imag_part = 0.0;
    for (int n1 = 0; n1 <= L; ++n1) {
        int n2_start = (L + n1) % 2 == 0 ? 0 : 1;
        double z_factor = 0.0;
        double dz_factor = 0.0;
        for (int n2 = n2_start; n2 <= L - n1; n2 += 2) {
            if (L == 1) {
                z_factor += Z_COEFFICIENT_1[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_1[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 2) {
                z_factor += Z_COEFFICIENT_2[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_2[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 3) {
                z_factor += Z_COEFFICIENT_3[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_3[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 4) {
                z_factor += Z_COEFFICIENT_4[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_4[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 5) {
                z_factor += Z_COEFFICIENT_5[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_5[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 6) {
                z_factor += Z_COEFFICIENT_6[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_6[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 7) {
                z_factor += Z_COEFFICIENT_7[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_7[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
            if (L == 8) {
                z_factor += Z_COEFFICIENT_8[n1][n2] * z_pow[n2];
                if (n2 > 0) {
                    dz_factor += Z_COEFFICIENT_8[n1][n2] * n2 * z_pow[n2 - 1];
                }
            }
        }
        if (n1 == 0) {
            for (int d = 0; d < 3; ++d) {
                f12[d] += s[0] * (z_factor * fnp * r12[d] + fn * dz_factor * dz[d]);
            }
        }
        else {
            double real_part_n1 = n1 * real_part;
            double imag_part_n1 = n1 * imag_part;
            for (int d = 0; d < 3; ++d) {
                double real_part_dx = dx[d];
                double imag_part_dy = dy[d];
                complex_product(real_part_n1, imag_part_n1, real_part_dx, imag_part_dy);
                f12[d] += (s[2 * n1 - 1] * real_part_dx + s[2 * n1 - 0] * imag_part_dy) * z_factor * fn;
            }
            complex_product(r12[0], r12[1], real_part, imag_part);
            const double xy_temp = s[2 * n1 - 1] * real_part + s[2 * n1 - 0] * imag_part;
            for (int d = 0; d < 3; ++d) {
                f12[d] += xy_temp * (z_factor * fnp * r12[d] + fn * dz_factor * dz[d]);
            }
        }
    }
}

template <int LMAX, int NUML, int NMAX_A>
static void accumulate_f12(
    const int n,
    const double d12,
    const double *r12,
    double fn,
    double fnp,
    const double *Fp,
    const double *sum_fxyz,
    double *f12) {
    const double fn_original = fn;
    const double fnp_original = fnp;
    const double d12inv = 1.0 / d12;
    const double r12unit[3] = {r12[0] * d12inv, r12[1] * d12inv, r12[2] * d12inv};

    fnp = fnp * d12inv - fn * d12inv * d12inv;
    fn = fn * d12inv;
    if (NUML >= LMAX + 2) {
        double s1[3] = {
            sum_fxyz[n * NUM_OF_ABC + 0], sum_fxyz[n * NUM_OF_ABC + 1], sum_fxyz[n * NUM_OF_ABC + 2]
        };
        get_f12_5body(d12, d12inv, fn, fnp, Fp[(LMAX+1) * (NMAX_A+1) + n], s1, r12, f12);
    }

    if (LMAX >= 1) {
        double s1[3];
        calculate_s_one<1, NMAX_A>(n, Fp, sum_fxyz, s1);
        accumulate_f12_one<1>(d12inv, fn_original, fnp_original, s1, r12unit, f12);
    }

    fnp = fnp * d12inv - fn * d12inv * d12inv;
    fn = fn * d12inv;
    if (NUML >= LMAX + 1) {
        double s2[5] = {
            sum_fxyz[n * NUM_OF_ABC + 3], sum_fxyz[n * NUM_OF_ABC + 4], sum_fxyz[n * NUM_OF_ABC + 5],
            sum_fxyz[n * NUM_OF_ABC + 6], sum_fxyz[n * NUM_OF_ABC + 7]
        };
        get_f12_4body(d12, d12inv, fn, fnp, Fp[LMAX * (NMAX_A+1) + n], s2, r12, f12);
    }

    if (LMAX >= 2) {
        double s2[5];
        calculate_s_one<2, NMAX_A>(n, Fp, sum_fxyz, s2);
        accumulate_f12_one<2>(d12inv, fn_original, fnp_original, s2, r12unit, f12);
    }

    if (LMAX >= 3) {
        double s3[7];
        calculate_s_one<3, NMAX_A>(n, Fp, sum_fxyz, s3);
        accumulate_f12_one<3>(d12inv, fn_original, fnp_original, s3, r12unit, f12);
    }

    if (LMAX >= 4) {
        double s4[9];
        calculate_s_one<4, NMAX_A>(n, Fp, sum_fxyz, s4);
        accumulate_f12_one<4>(d12inv, fn_original, fnp_original, s4, r12unit, f12);
    }

    if (LMAX >= 5) {
        double s5[11];
        calculate_s_one<5, NMAX_A>(n, Fp, sum_fxyz, s5);
        accumulate_f12_one<5>(d12inv, fn_original, fnp_original, s5, r12unit, f12);
    }

    if (LMAX >= 6) {
        double s6[13];
        calculate_s_one<6, NMAX_A>(n, Fp, sum_fxyz, s6);
        accumulate_f12_one<6>(d12inv, fn_original, fnp_original, s6, r12unit, f12);
    }

    if (LMAX >= 7) {
        double s7[15];
        calculate_s_one<7, NMAX_A>(n, Fp, sum_fxyz, s7);
        accumulate_f12_one<7>(d12inv, fn_original, fnp_original, s7, r12unit, f12);
    }

    if (LMAX >= 8) {
        double s8[17];
        calculate_s_one<8, NMAX_A>(n, Fp, sum_fxyz, s8);
        accumulate_f12_one<8>(d12inv, fn_original, fnp_original, s8, r12unit, f12);
    }
}

template <int L>
static void accumulate_s_one(const double x12, const double y12, const double z12, const double fn, double *s) {
    int s_index = L * L - 1;
    double z_pow[L + 1] = {1.0};
    for (int n = 1; n <= L; ++n) {
        z_pow[n] = z12 * z_pow[n - 1];
    }
    double real_part = x12;
    double imag_part = y12;
    for (int n1 = 0; n1 <= L; ++n1) {
        int n2_start = (L + n1) % 2 == 0 ? 0 : 1;
        double z_factor = 0.0;
        for (int n2 = n2_start; n2 <= L - n1; n2 += 2) {
            if (L == 1) {
                z_factor += Z_COEFFICIENT_1[n1][n2] * z_pow[n2];
            }
            if (L == 2) {
                z_factor += Z_COEFFICIENT_2[n1][n2] * z_pow[n2];
            }
            if (L == 3) {
                z_factor += Z_COEFFICIENT_3[n1][n2] * z_pow[n2];
            }
            if (L == 4) {
                z_factor += Z_COEFFICIENT_4[n1][n2] * z_pow[n2];
            }
            if (L == 5) {
                z_factor += Z_COEFFICIENT_5[n1][n2] * z_pow[n2];
            }
            if (L == 6) {
                z_factor += Z_COEFFICIENT_6[n1][n2] * z_pow[n2];
            }
            if (L == 7) {
                z_factor += Z_COEFFICIENT_7[n1][n2] * z_pow[n2];
            }
            if (L == 8) {
                z_factor += Z_COEFFICIENT_8[n1][n2] * z_pow[n2];
            }
        }
        z_factor *= fn;
        if (n1 == 0) {
            s[s_index++] += z_factor;
        }
        else {
            s[s_index++] += z_factor * real_part;
            s[s_index++] += z_factor * imag_part;
            complex_product(x12, y12, real_part, imag_part);
        }
    }
}

template <int LMAX>
static void accumulate_s(const double d12, double x12, double y12, double z12, const double fn, double *s) {
    const double d12inv = 1.0 / d12;
    x12 *= d12inv;
    y12 *= d12inv;
    z12 *= d12inv;
    if (LMAX >= 1) {
        accumulate_s_one<1>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 2) {
        accumulate_s_one<2>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 3) {
        accumulate_s_one<3>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 4) {
        accumulate_s_one<4>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 5) {
        accumulate_s_one<5>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 6) {
        accumulate_s_one<6>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 7) {
        accumulate_s_one<7>(x12, y12, z12, fn, s);
    }
    if (LMAX >= 8) {
        accumulate_s_one<8>(x12, y12, z12, fn, s);
    }
}

template <int L>
static double find_q_one(const double *s) {
    constexpr int start_index = L * L - 1;
    constexpr int num_terms = 2 * L + 1;
    double q = 0.0;
    for (int k = 1; k < num_terms; ++k) {
        q += C3B[start_index + k] * s[start_index + k] * s[start_index + k];
    }
    q *= 2.0;
    q += C3B[start_index] * s[start_index] * s[start_index];
    return q;
}

template <int LMAX, int NUML, int NMAX_A>
static void find_q(const int n, const double *s, double *q) {
    constexpr int n_max_angular_plus_1 = NMAX_A + 1;
    if (LMAX >= 1) {
        q[0 * n_max_angular_plus_1 + n] = find_q_one<1>(s);
    }
    if (LMAX >= 2) {
        q[1 * n_max_angular_plus_1 + n] = find_q_one<2>(s);
    }
    if (LMAX >= 3) {
        q[2 * n_max_angular_plus_1 + n] = find_q_one<3>(s);
    }
    if (LMAX >= 4) {
        q[3 * n_max_angular_plus_1 + n] = find_q_one<4>(s);
    }
    if (LMAX >= 5) {
        q[4 * n_max_angular_plus_1 + n] = find_q_one<5>(s);
    }
    if (LMAX >= 6) {
        q[5 * n_max_angular_plus_1 + n] = find_q_one<6>(s);
    }
    if (LMAX >= 7) {
        q[6 * n_max_angular_plus_1 + n] = find_q_one<7>(s);
    }
    if (LMAX >= 8) {
        q[7 * n_max_angular_plus_1 + n] = find_q_one<8>(s);
    }
    if (NUML >= LMAX + 1) {
        q[LMAX * n_max_angular_plus_1 + n] =
            C4B[0] * s[3] * s[3] * s[3] + C4B[1] * s[3] * (s[4] * s[4] + s[5] * s[5]) +
            C4B[2] * s[3] * (s[6] * s[6] + s[7] * s[7]) + C4B[3] * s[6] * (s[5] * s[5] - s[4] * s[4]) +
            C4B[4] * s[4] * s[5] * s[7];
    }
    if (NUML >= LMAX + 2) {
        double s0_sq = s[0] * s[0];
        double s1_sq_plus_s2_sq = s[1] * s[1] + s[2] * s[2];
        q[(LMAX + 1) * n_max_angular_plus_1 + n] = C5B[0] * s0_sq * s0_sq +
            C5B[1] * s0_sq * s1_sq_plus_s2_sq +
            C5B[2] * s1_sq_plus_s2_sq * s1_sq_plus_s2_sq;
    }
}

static constexpr int table_length = 2001;
static constexpr int table_segments = table_length - 1;
static constexpr double table_resolution = 0.0005;

static void find_index_and_weight(
    const double d12_reduced,
    int &index_left,
    int &index_right,
    double &weight_left,
    double &weight_right) {
    double d12_index = d12_reduced * table_segments;
    index_left = int(d12_index);
    if (index_left == table_segments) {
        --index_left;
    }
    index_right = index_left + 1;
    weight_right = d12_index - index_left;
    weight_left = 1.0 - weight_right;
}

template <int VERSION, int NTYPES, int NTYPES_SQ, int NMAX, int BSIZE>
static void construct_table_radial_or_angular(
    const double rc,
    const double rcinv,
    const double *c,
    double *gn,
    double *gnp) {
    for (int table_index = 0; table_index < table_length; ++table_index) {
        double d12 = table_index * table_resolution * rc;
        double fc12, fcp12;
        find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
        for (int t1 = 0; t1 < NTYPES; ++t1) {
            for (int t2 = 0; t2 < NTYPES; ++t2) {
                int t12 = t1 * NTYPES + t2;
                double fn12[MAX_NUM_N];
                double fnp12[MAX_NUM_N];
                find_fn_and_fnp<BSIZE>(rcinv, d12, fc12, fcp12, fn12, fnp12);
                for (int n = 0; n <= NMAX; ++n) {
                    double gn12 = 0.0;
                    double gnp12 = 0.0;
                    for (int k = 0; k <= BSIZE; ++k) {
                        gn12 += fn12[k] * c[(n * (BSIZE + 1) + k) * NTYPES_SQ + t12];
                        gnp12 += fnp12[k] * c[(n * (BSIZE + 1) + k) * NTYPES_SQ + t12];
                    }
                    int index_all = (table_index * NTYPES_SQ + t12) * (NMAX + 1) + n;
                    gn[index_all] = gn12;
                    gnp[index_all] = gnp12;
                }
            }
        }
    }
}
template <int VERSION, int NTYPES, int NMAX_R, int NMAX_A, int BSIZE_R, int BSIZE_A, int NUMC_R, int NUM_PARA_ANN>
static void construct_table(const double *parameters,
    const double rc_radial, const double rc_angular,
    double *gn_radial, double *gn_angular,
    double *gnp_radial, double *gnp_angular) {
    constexpr int num_types_sq = NTYPES * NTYPES;
    const double rcinv_radial = 1.0 / rc_radial;
    const double rcinv_angular = 1.0 / rc_angular;
    
    const double *c_pointer = parameters + NUM_PARA_ANN;
    construct_table_radial_or_angular<VERSION, NTYPES, num_types_sq, NMAX_R, BSIZE_R>(
        rc_radial, rcinv_radial, c_pointer,
        gn_radial, gnp_radial
    );
    construct_table_radial_or_angular<VERSION, NTYPES, num_types_sq, NMAX_A, BSIZE_A>(
        rc_angular, rcinv_angular, c_pointer+NUMC_R,
        gn_angular, gnp_angular
    );
}


template <int USE_TABLE, int VERSION, int NTYPES, int TW_CUTOFF,
          int NMAX_R, int BSIZE_R, int NUMC_R,
          int NMAX_A, int BSIZE_A, int LMAX, int NUML,
          int ANN_DIM, int NUM_NEURONS1>
static void find_descriptor(
    const int *atomic_numbers,
    double typewise_cutoff_radial_factor, double typewise_cutoff_angular_factor,
    double rc_radial, double rc_angular,
    const double *q_scaler,
    const double **ann_w0, const double **ann_b0, const double **ann_w1, const double *ann_b1,
    const double *ann_c,
    int nlocal,
    int cidx, int ctype,
    int num_neigh,
    const int *nl_type,
    const double *nl_dx, const double *nl_dy, const double *nl_dz,
    const double *g_gn_radial,
    const double *g_gn_angular,
    double *g_Fp,
    double *g_sum_fxyz,
    double *g_potential) {
    constexpr int num_types_sq = NTYPES * NTYPES;
    const double rcinv_radial = 1.0 / rc_radial;
    const double rcinv_angular = 1.0 / rc_angular;

    int n1 = cidx;
    int t1 = ctype;
    double q[MAX_DIM] = {0.0};

    for (int i1 = 0; i1 < num_neigh; ++i1) {
        double r12[3] = {nl_dx[i1], nl_dy[i1], nl_dz[i1]};
        double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
        if (d12sq >= rc_radial * rc_radial) {
            continue;
        }
        double d12 = sqrt(d12sq);
        int t2 = nl_type[i1];

        if (USE_TABLE) {
            int index_left, index_right;
            double weight_left, weight_right;
            find_index_and_weight(d12 * rcinv_radial, index_left, index_right, weight_left, weight_right);
            int t12 = t1 * NTYPES + t2;
            for (int n = 0; n <= NMAX_R; ++n) {
                q[n] +=
                    g_gn_radial[(index_left * num_types_sq + t12) * (NMAX_R + 1) + n] * weight_left +
                    g_gn_radial[(index_right * num_types_sq + t12) * (NMAX_R + 1) + n] * weight_right;
            }
        } else {
            double fc12;
            double rc = rc_radial;
            double rcinv = rcinv_radial;
            if (TW_CUTOFF) {
                rc = std::min(rc, (COVALENT_RADIUS[atomic_numbers[t1]] + COVALENT_RADIUS[atomic_numbers[t2]]) * typewise_cutoff_radial_factor);
                rcinv = 1.0 / rc;
            }
            find_fc(rc, rcinv, d12, fc12);
            double fn12[MAX_NUM_N];
            find_fn<BSIZE_R>(rcinv, d12, fc12, fn12);
            for (int n = 0; n <= NMAX_R; ++n) {
                double gn12 = 0.0;
                for (int k = 0; k <= BSIZE_R; ++k) {
                    int c_index = (n * (BSIZE_R + 1) + k) * num_types_sq;
                    c_index += t1 * NTYPES + t2;
                    gn12 += fn12[k] * ann_c[c_index];
                }
                q[n] += gn12;
            }
        }
    }

    for (int n = 0; n <= NMAX_A; ++n) {
        double s[NUM_OF_ABC] = {0.0};
        for (int i1 = 0; i1 < num_neigh; ++i1) {
            double r12[3] = {nl_dx[i1], nl_dy[i1], nl_dz[i1]};
            double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
            if (d12sq >= rc_angular * rc_angular) {
                continue;
            }
            double d12 = sqrt(d12sq);
            int t2 = nl_type[i1];

            if (USE_TABLE) {
                int index_left, index_right;
                double weight_left, weight_right;
                find_index_and_weight(d12 * rcinv_angular, index_left, index_right, weight_left, weight_right);
                int t12 = t1 * NTYPES + t2;
                double gn12 =
                    g_gn_angular[(index_left * num_types_sq + t12) * (NMAX_A + 1) + n] * weight_left +
                    g_gn_angular[(index_right * num_types_sq + t12) * (NMAX_A + 1) + n] * weight_right;
                accumulate_s<LMAX>(d12, r12[0], r12[1], r12[2], gn12, s);
            } else {
                double fc12;
                double rc = rc_angular;
                double rcinv = rcinv_angular;
                if (TW_CUTOFF) {
                    rc = std::min(rc, (COVALENT_RADIUS[atomic_numbers[t1]] + COVALENT_RADIUS[atomic_numbers[t2]]) * typewise_cutoff_angular_factor);
                    rcinv = 1.0 / rc;
                }
                find_fc(rc, rcinv, d12, fc12);
                double fn12[MAX_NUM_N];
                find_fn<BSIZE_A>(rcinv, d12, fc12, fn12);
                double gn12 = 0.0;
                for (int k = 0; k <= BSIZE_A; ++k) {
                    int c_index = (n * (BSIZE_A + 1) + k) * num_types_sq;
                    c_index += t1 * NTYPES + t2 + NUMC_R;
                    gn12 += fn12[k] * ann_c[c_index];
                }
                accumulate_s<LMAX>(d12, r12[0], r12[1], r12[2], gn12, s);
            }
        }
        find_q<LMAX, NUML, NMAX_A>(n, s, q + (NMAX_R+1));
        for (int abc = 0; abc < NUM_OF_ABC; ++abc) {
            g_sum_fxyz[(n * NUM_OF_ABC + abc) * nlocal + n1] = s[abc];
        }
    }

    for (int d = 0; d < ANN_DIM; ++d) {
        q[d] = q[d] * q_scaler[d];
    }

    double F = 0.0, Fp[MAX_DIM] = {0.0}, latent_space[MAX_NEURON] = {0.0};

    if (VERSION == 5) {
        apply_ann_one_layer_nep5<ANN_DIM, NUM_NEURONS1>(
            ann_w0[t1], ann_b0[t1], ann_w1[t1], ann_b1, q, F, Fp,
            latent_space
        );
    } else {
        apply_ann_one_layer<ANN_DIM, NUM_NEURONS1, 0>(
            ann_w0[t1], ann_b0[t1], ann_w1[t1], ann_b1, q, F, Fp,
            latent_space, nullptr
        );
    }
    
    g_potential[n1] += F;
    
    for (int d = 0; d < ANN_DIM; ++d) {
        g_Fp[d * nlocal + n1] = Fp[d] * q_scaler[d];
    }
}

template <int USE_TABLE, int NTYPES, int TW_CUTOFF,
          int NMAX_R, int BSIZE_R>
static void find_force_radial(
    const int *atomic_numbers,
    double typewise_cutoff_radial_factor,
    double rc_radial,
    const double *ann_c,
    int nlocal,
    int cidx, int ctype,
    int num_neigh,
    const int *nl_type,
    const double *nl_dx, const double *nl_dy, const double *nl_dz,
    double *g_Fp,
    const double *g_gnp_radial,
    double *nl_fx, double *nl_fy, double *nl_fz) {
    constexpr int num_types_sq = NTYPES * NTYPES;
    const double rcinv_radial = 1.0 / rc_radial;
    
    int n1 = cidx;
    int t1 = ctype;
    for (int i1 = 0; i1 < num_neigh; ++i1) {
        int t2 = nl_type[i1];
        double r12[3] = {nl_dx[i1], nl_dy[i1], nl_dz[i1]};
        double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
        if (d12sq >= rc_radial * rc_radial) {
            continue;
        }
        double d12 = sqrt(d12sq);
        double d12inv = 1.0 / d12;
        double f12[3] = {0.0};
        if (USE_TABLE) {
            int index_left, index_right;
            double weight_left, weight_right;
            find_index_and_weight(d12 * rcinv_radial, index_left, index_right, weight_left, weight_right);
            int t12 = t1 * NTYPES + t2;
            for (int n = 0; n <= NMAX_R; ++n) {
                double gnp12 =
                    g_gnp_radial[(index_left * num_types_sq + t12) * (NMAX_R + 1) + n] * weight_left +
                    g_gnp_radial[(index_right * num_types_sq + t12) * (NMAX_R + 1) + n] * weight_right;
                double tmp12 = g_Fp[n1 + n * nlocal] * gnp12 * d12inv;
                for (int d = 0; d < 3; ++d) {
                    f12[d] += tmp12 * r12[d];
                }
            }
        } else {
            double fc12, fcp12;
            double rc = rc_radial;
            double rcinv = rcinv_radial;
            if (TW_CUTOFF) {
                rc = std::min(rc, (COVALENT_RADIUS[atomic_numbers[t1]] + COVALENT_RADIUS[atomic_numbers[t2]]) * typewise_cutoff_radial_factor);
                rcinv = 1.0 / rc;
            }
            find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
            double fn12[MAX_NUM_N];
            double fnp12[MAX_NUM_N];
            find_fn_and_fnp<BSIZE_R>(rcinv, d12, fc12, fcp12, fn12, fnp12);
            for (int n = 0; n <= NMAX_R; ++n) {
                double gnp12 = 0.0;
                for (int k = 0; k <= BSIZE_R; ++k) {
                    int c_index = (n * (BSIZE_R + 1) + k) * num_types_sq;
                    c_index += t1 * NTYPES + t2;
                    gnp12 += fnp12[k] * ann_c[c_index];
                }
                double tmp12 = g_Fp[n1 + n * nlocal] * gnp12 * d12inv;
                for (int d = 0; d < 3; ++d) {
                    f12[d] += tmp12 * r12[d];
                }
            }
        }

        nl_fx[i1] -= f12[0];
        nl_fy[i1] -= f12[1];
        nl_fz[i1] -= f12[2];
    }
}

template <int USE_TABLE, int NTYPES, int TW_CUTOFF,
          int NMAX_R, int NUMC_R,
          int NMAX_A, int BSIZE_A, int LMAX, int NUML, int ANN_DIM_A>
static void find_force_angular(
    const int *atomic_numbers,
    double typewise_cutoff_angular_factor,
    double rc_angular,
    const double *ann_c,
    int nlocal,
    int cidx, int ctype,
    int num_neigh,
    const int *nl_type,
    const double *nl_dx, const double *nl_dy, const double *nl_dz,
    double *g_Fp,
    double *g_sum_fxyz,
    const double *g_gn_angular,
    const double *g_gnp_angular,
    double *nl_fx, double *nl_fy, double *nl_fz) {
    constexpr int num_types_sq = NTYPES * NTYPES;
    const double rcinv_angular = 1.0 / rc_angular;
    
    int n1 = cidx;
    int t1 = ctype;
    
    double Fp[MAX_DIM_ANGULAR] = {0.0};
    double sum_fxyz[NUM_OF_ABC * MAX_NUM_N];
    for (int d = 0; d < ANN_DIM_A; ++d) {
        Fp[d] = g_Fp[(NMAX_R + 1 + d) * nlocal + n1];
    }
    for (int d = 0; d < (NMAX_R + 1) * NUM_OF_ABC; ++d) {
        sum_fxyz[d] = g_sum_fxyz[d * nlocal + n1];
    }

    for (int i1 = 0; i1 < num_neigh; ++i1) {
        int t2 = nl_type[i1];
        double r12[3] = {nl_dx[i1], nl_dy[i1], nl_dz[i1]};
        double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
        if (d12sq >= rc_angular * rc_angular) {
            continue;
        }
        double d12 = sqrt(d12sq);
        double f12[3] = {0.0};

        if (USE_TABLE) {
            int index_left, index_right;
            double weight_left, weight_right;
            find_index_and_weight(d12 * rcinv_angular, index_left, index_right, weight_left, weight_right);
            int t12 = t1 * NTYPES + t2;
            for (int n = 0; n <= NMAX_A; ++n) {
                int index_left_all = (index_left * num_types_sq + t12) * (NMAX_A + 1) + n;
                int index_right_all = (index_right * num_types_sq + t12) * (NMAX_A + 1) + n;
                double gn12 = g_gn_angular[index_left_all] * weight_left + g_gn_angular[index_right_all] * weight_right;
                double gnp12 = g_gnp_angular[index_left_all] * weight_left + g_gnp_angular[index_right_all] * weight_right;
                accumulate_f12<LMAX, NUML, NMAX_A>(n, d12, r12, gn12, gnp12, Fp, sum_fxyz, f12);
            }
        } else {
            double fc12, fcp12;
            double rc = rc_angular;
            double rcinv = rcinv_angular;
            if (TW_CUTOFF) {
                rc = std::min(rc, (COVALENT_RADIUS[atomic_numbers[t1]] + COVALENT_RADIUS[atomic_numbers[t2]]) * typewise_cutoff_angular_factor);
                rcinv = 1.0 / rc;
            }
            find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
            double fn12[MAX_NUM_N];
            double fnp12[MAX_NUM_N];
            find_fn_and_fnp<BSIZE_A>(rcinv, d12, fc12, fcp12, fn12, fnp12);
            for (int n = 0; n <= NMAX_A; ++n) {
                double gn12 = 0.0;
                double gnp12 = 0.0;
                for (int k = 0; k <= BSIZE_A; ++k) {
                    int c_index = (n * (BSIZE_A + 1) + k) * num_types_sq;
                    c_index += t1 * NTYPES + t2 + NUMC_R;
                    gn12 += fn12[k] * ann_c[c_index];
                    gnp12 += fnp12[k] * ann_c[c_index];
                }
                accumulate_f12<LMAX, NUML, NMAX_A>(n, d12, r12, gn12, gnp12, Fp, sum_fxyz, f12);
            }
        }

        nl_fx[i1] -= f12[0];
        nl_fy[i1] -= f12[1];
        nl_fz[i1] -= f12[2];
    }
}

template <int NTYPES, int TW_CUTOFF_ZBL, int ZBL_FLEXIBLED>
static void find_force_ZBL(
    const int *atomic_numbers,
    double typewise_cutoff_zbl_factor,
    const double *zbl_para,
    double zbl_rc_inner, double zbl_rc_outer,
    int cidx, int ctype,
    int num_neigh,
    const int *nl_type,
    const double *nl_dx, const double *nl_dy, const double *nl_dz,
    double *nl_fx, double *nl_fy, double *nl_fz,
    double *g_potential) {
    int n1 = cidx;
    int type1 = ctype;
    int zi = atomic_numbers[type1] + 1;
    double pow_zi = pow((double)zi, 0.23);
    for (int i1 = 0; i1 < num_neigh; ++i1) {
        double r12[3] = {nl_dx[i1], nl_dy[i1], nl_dz[i1]};
        double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];;
        double max_rc_outer = 2.5;
        if (d12sq >= max_rc_outer * max_rc_outer) {
            continue;
        }
        double d12 = sqrt(d12sq);

        double d12inv = 1.0 / d12;
        double f, fp;
        int type2 = nl_type[i1];
        int zj = atomic_numbers[type2] + 1;
        double a_inv = (pow_zi + pow((double)zj, 0.23)) * 2.134563;
        double zizj = K_C_SP * zi * zj;
        if (ZBL_FLEXIBLED) {
            int t1, t2;
            if (type1 < type2) {
                t1 = type1;
                t2 = type2;
            }
            else {
                t1 = type2;
                t2 = type1;
            }
            int zbl_index = t1 * NTYPES - (t1 * (t1 - 1)) / 2 + (t2 - t1);
            double ZBL_para[10];
            for (int i = 0; i < 10; ++i) {
                ZBL_para[i] = zbl_para[10 * zbl_index + i];
            }
            find_f_and_fp_zbl(ZBL_para, zizj, a_inv, d12, d12inv, f, fp);
        } else {
            double rc_inner = zbl_rc_inner;
            double rc_outer = zbl_rc_outer;
            if (TW_CUTOFF_ZBL) {
                // zi and zj start from 1, so need to minus 1 here
                rc_outer = std::min(rc_outer, (COVALENT_RADIUS[zi - 1] + COVALENT_RADIUS[zj - 1]) * typewise_cutoff_zbl_factor);
                rc_inner = rc_outer * 0.5f;
            }
            find_f_and_fp_zbl(zizj, a_inv, rc_inner, rc_outer, d12, d12inv, f, fp);
        }
        double f2 = fp * d12inv * 0.5;
        double f12[3] = {r12[0] * f2, r12[1] * f2, r12[2] * f2};
        
        nl_fx[i1] -= f12[0];
        nl_fy[i1] -= f12[1];
        nl_fz[i1] -= f12[2];
        
        g_potential[n1] += f * 0.5;
    }
}

} // namespace

#endif //NEP_CORE_H
