#ifndef BASIS_SPHERICAL_UTIL_H
#define BASIS_SPHERICAL_UTIL_H

#include "basis_SphericalUtil0.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGENS_X 0
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

template <int M, int L>
static inline void realNormalizedLegendreInterLoopSubSub_(double aX, double *rDest) noexcept {
    constexpr double tSHAlm = SH_Alm[L*(L+1)/2 + M];
    constexpr double tSHBlm = SH_Blm[L*(L+1)/2 + M];
    const double tPlm = tSHAlm * (aX*rDest[(L-1)*(L-1)+(L-1) + M] + tSHBlm*rDest[(L-2)*(L-2)+(L-2) + M]);
    if (M == 0) {
        rDest[L*L+L] = tPlm;
    } else {
        rDest[L*L+L + M] = tPlm;
        rDest[L*L+L - M] = tPlm;
    }
}
template <int L>
static inline void realNormalizedLegendreInterLoopSub_(double aX, double *rDest) noexcept {
// >>> NNAPGEN REPEAT
    if (L-1==NNAPGENS_X) {return;} realNormalizedLegendreInterLoopSubSub_<NNAPGENS_X, L>(aX, rDest);
// <<< NNAPGEN REPEAT 0..12
}
template <int L>
static inline void realNormalizedLegendreInterLoop_(double aX, double aY, double *rDest, double &rPll) noexcept {
    realNormalizedLegendreInterLoopSub_<L>(aX, rDest);
    constexpr double tMul1 = SQRT_2LM1P3[L];
    const double tPlm = tMul1 * aX * rPll;
    rDest[L*L+L + (L-1)] = tPlm;
    rDest[L*L+L - (L-1)] = tPlm;
    constexpr double tMul2 = -SQRT_1P1D2L[L];
    rPll *= tMul2 * aY;
    rDest[L*L+L + L] = rPll;
    rDest[L*L+L - L] = rPll;
}
template <int LMAX>
static inline void realNormalizedLegendreFull(double aX, double aY, double *rDest) noexcept {
    double tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
    rDest[0] = tPll;
    if (LMAX == 0) return;
    rDest[2] = SQRT3 * aX * tPll;
    tPll *= (-SQRT3DIV2 * aY);
    rDest[2+1] = tPll;
    rDest[2-1] = tPll;
    if (LMAX == 1) return;
// >>> NNAPGEN REPEAT
    realNormalizedLegendreInterLoop_<NNAPGENS_X>(aX, aY, rDest, tPll); if (LMAX==NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 2..12
}

template <int M, int L>
static inline void realSphericalHarmonicsFull4InterLoopSubSub_(double aSqrt2CosMPhi, double aSqrt2SinMPhi, double *rDest) noexcept {
    rDest[L*L+L + M] *= aSqrt2CosMPhi;
    rDest[L*L+L - M] *= aSqrt2SinMPhi;
}
template <int M, int LMAX>
static inline void realSphericalHarmonicsFull4InterLoopSub_(double aSqrt2CosMPhi, double aSqrt2SinMPhi, double *rDest) noexcept {
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+NNAPGENS_X>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int M, int LMAX>
static inline void realSphericalHarmonicsFull4InterLoop_(double aCosPhi2, double &rSinMPhi, double &rSinMmmPhi, double &rCosMPhi, double &rCosMmmPhi, double *rDest) noexcept {
    const double fSqrt2CosMPhi = SQRT2*rCosMPhi;
    const double fSqrt2SinMPhi = SQRT2*rSinMPhi;
    realSphericalHarmonicsFull4InterLoopSub_<M, LMAX>(fSqrt2CosMPhi, fSqrt2SinMPhi, rDest);
    const double tSinMppPhi = aCosPhi2 * rSinMPhi - rSinMmmPhi;
    const double tCosMppPhi = aCosPhi2 * rCosMPhi - rCosMmmPhi;
    rSinMmmPhi = rSinMPhi; rCosMmmPhi = rCosMPhi;
    rSinMPhi = tSinMppPhi; rCosMPhi = tCosMppPhi;
}
template <int LMAX>
static inline void realSphericalHarmonicsFull4(double aX, double aY, double aZ, double aDis, double *rDest) noexcept {
    const double tXY = hypot(aX, aY);
    const double tCosTheta = aZ / aDis;
    const double tSinTheta = tXY / aDis;
    double tCosPhi;
    double tSinPhi;
    // avoid nan
    if (numericEqual(tXY, 0.0)) {
        tCosPhi = 1.0;
        tSinPhi = 0.0;
    } else {
        tCosPhi = aX / tXY;
        tSinPhi = aY / tXY;
    }
    // cal real Legendre
    realNormalizedLegendreFull<LMAX>(tCosTheta, tSinTheta, rDest);
    if (LMAX == 0) return;
    // cal sinMPhi & conMPhi
    double rSinMmmPhi = 0.0;
    double rCosMmmPhi = 1.0;
    double rSinMPhi = tSinPhi;
    double rCosMPhi = tCosPhi;
    const double tCosPhi2 = rCosMPhi+rCosMPhi;
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoop_<NNAPGENS_X, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX==NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 1..12
}

template <int L>
static inline void calYPphi(double *rYPphi, double *aY) noexcept {
    constexpr int tStart = L*L;
    constexpr int tIdx = tStart+L;
    for (int m = -L; m <= L; ++m) {
        rYPphi[tIdx+m] = -m * aY[tIdx-m];
    }
}
template <int L>
static inline void calYPtheta(double aCosPhi, double aSinPhi, double *rYPtheta, double *aY) noexcept {
    switch(L) {
    case 0: {
        rYPtheta[0] = 0.0;
        return;
    }
    case 1: {
        constexpr double tMul = SQRT_LPM_LMM1[2]*SQRT2_INV;
        rYPtheta[1] = -tMul * aSinPhi*aY[2];
        rYPtheta[2] =  tMul * (aCosPhi*aY[3] + aSinPhi*aY[1]);
        rYPtheta[3] = -tMul * aCosPhi*aY[2];
        return;
    }
    default: {
        constexpr int tStart = L*L;
        constexpr int tIdx = tStart+L;
        constexpr double tMul = SQRT_LPM_LMM1[tIdx]*SQRT2_INV;
        rYPtheta[tIdx] = tMul * (aCosPhi*aY[tIdx+1] + aSinPhi*aY[tIdx-1]);
        rYPtheta[tIdx+1] = -tMul * aCosPhi*aY[tIdx];
        rYPtheta[tIdx-1] = -tMul * aSinPhi*aY[tIdx];
        for (int m = 2; m <= L; ++m) {
            const double tMul2 = -0.5*SQRT_LPM_LMM1[tIdx+m];
            rYPtheta[tIdx+m] = tMul2 * (aCosPhi*aY[tIdx+m-1] - aSinPhi*aY[tIdx-m+1]);
            rYPtheta[tIdx-m] = tMul2 * (aCosPhi*aY[tIdx-m+1] + aSinPhi*aY[tIdx+m-1]);
        }
        for (int m = 1; m < L; ++m) {
            const double tMul2 = 0.5*SQRT_LPM1_LMM[tIdx+m];
            rYPtheta[tIdx+m] += tMul2 * (aCosPhi*aY[tIdx+m+1] + aSinPhi*aY[tIdx-m-1]);
            rYPtheta[tIdx-m] += tMul2 * (aCosPhi*aY[tIdx-m-1] - aSinPhi*aY[tIdx+m+1]);
        }
        return;
    }}
}
template <int LMAX>
static void calYPphiPtheta(double *rYPphi, double aCosPhi, double aSinPhi, double *rYPtheta, double *aY) noexcept {
// >>> NNAPGEN REPEAT
    calYPphi<NNAPGENS_X>(rYPphi, aY); calYPtheta<NNAPGENS_X>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX==NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int N>
static inline void convertYPPhiPtheta2YPxyz(double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, double aDis, double aDxy, int aDxyCloseZero,
                                            double *rYPx, double *rYPy, double *rYPz, double *aYPtheta, double *aYPphi) noexcept {
    const double thetaPx = -aCosTheta * aCosPhi / aDis;
    const double thetaPy = -aCosTheta * aSinPhi / aDis;
    const double thetaPz =  aSinTheta / aDis;
    const double phiPx = aDxyCloseZero ? 0.0 :  aSinPhi / aDxy;
    const double phiPy = aDxyCloseZero ? 0.0 : -aCosPhi / aDxy;
    for (int i = 0; i < N; ++i) {
        const double tYPtheta = aYPtheta[i];
        const double tYPphi = aYPphi[i];
        rYPx[i] = tYPtheta*thetaPx + tYPphi*phiPx;
        rYPy[i] = tYPtheta*thetaPy + tYPphi*phiPy;
        rYPz[i] = tYPtheta*thetaPz;
    }
}
template <int LMAX>
static void calYPxyz(double *aY, double aDx, double aDy, double aDz, double aDis,
                     double *rYPx, double *rYPy, double *rYPz, double *rYPtheta, double *rYPphi) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    const double dxy = hypot(aDx, aDy);
    const double cosTheta = aDz / aDis;
    const double sinTheta = dxy / aDis;
    double cosPhi;
    double sinPhi;
    const int dxyCloseZero = numericEqual(dxy, 0.0);
    if (dxyCloseZero) {
        cosPhi = 1.0;
        sinPhi = 0.0;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    calYPphiPtheta<LMAX>(rYPphi, cosPhi, sinPhi, rYPtheta, aY);
    if (dxyCloseZero) {
        // fix singularity
        for (int k = 0; k < tLMAll; ++k) rYPphi[k] = 0.0;
    }
    // conert to Pxyz
    convertYPPhiPtheta2YPxyz<tLMAll>(cosTheta, sinTheta, cosPhi, sinPhi, aDis, dxy, dxyCloseZero,
                                     rYPx, rYPy, rYPz, rYPtheta, rYPphi);
}


template <int NMAX, int LMAX, int MPLUS, int WTFLAG>
static void setormplusCnlm_(double *rCnlm, double *rCnlmWt, double *aY, double aFc, double *aRn, double aWt) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    double *tCnlm = rCnlm;
    double *tCnlmWt = rCnlmWt;
    for (int n = 0; n <= NMAX; ++n) {
        const double tMul = aFc*aRn[n];
        for (int k = 0; k < tLMAll; ++k) {
            const double tValue = tMul*aY[k];
            if (MPLUS) {
                tCnlm[k] += tValue;
                if (WTFLAG) tCnlmWt[k] += aWt*tValue;
            } else {
                tCnlm[k] = tValue;
                if (WTFLAG) tCnlmWt[k] = aWt*tValue;
            }
        }
        tCnlm += tLMAll;
        if (WTFLAG) tCnlmWt += tLMAll;
    }
}
template <int NMAX, int LMAX>
static inline void calBnlm(double *rBnlm, double *aY, double aFc, double *aRn) noexcept {
    setormplusCnlm_<NMAX, LMAX, FALSE, FALSE>(rBnlm, NULL, aY, aFc, aRn, 0);
}
template <int NMAX, int LMAX>
static inline void mplusCnlm(double *rCnlm, double *aY, double aFc, double *aRn) noexcept {
    setormplusCnlm_<NMAX, LMAX, TRUE, FALSE>(rCnlm, NULL, aY, aFc, aRn, 0);
}
template <int NMAX, int LMAX>
static inline void mplusCnlmWt(double *rCnlm, double *rCnlmWt, double *aY, double aFc, double *aRn, double aWt) noexcept {
    setormplusCnlm_<NMAX, LMAX, TRUE, TRUE>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt);
}

template <int LMAX>
static void mplusLM(double *rAlm, double *aMul, double *aBlm) {
    double *tAlm = rAlm, *tBlm = aBlm;
// >>> NNAPGEN REPEAT
    mplus<2*NNAPGENS_X+1>(tAlm, aMul[NNAPGENS_X], tBlm); if (LMAX==NNAPGENS_X) {return;} tAlm += (2*NNAPGENS_X+1); tBlm += (2*NNAPGENS_X+1);
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static void dotLM(double *rDot, double *aAlm, double *aBlm) {
    double *tAlm = aAlm, *tBlm = aBlm;
// >>> NNAPGEN REPEAT
    rDot[NNAPGENS_X] += dot<2*NNAPGENS_X+1>(tAlm, tBlm); if (LMAX==NNAPGENS_X) {return;} tAlm += (2*NNAPGENS_X+1); tBlm += (2*NNAPGENS_X+1);
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static void multiplyLM(double *rClm, double *aMul) {
    double *tClm = rClm;
// >>> NNAPGEN REPEAT
    multiply<2*NNAPGENS_X+1>(tClm, aMul[NNAPGENS_X]); if (LMAX==NNAPGENS_X) {return;} tClm += (2*NNAPGENS_X+1);
// <<< NNAPGEN REPEAT 0..12
}

template <int NMAX, int LMAX, int FSIZE, int EXFLAG>
static void mplusCnlmFuse_(double *rCnlm, double *aBnlm, double *aFuseWeight, int aType) noexcept {
    constexpr int tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    double *tFuseWeight = aFuseWeight + FSIZE*(aType-1);
    double *tCnlm = rCnlm;
    if (EXFLAG) {
        mplus<tSizeBnlm>(tCnlm, 1.0, aBnlm);
        tCnlm += tSizeBnlm;
    }
    for (int k = 0; k < FSIZE; ++k) {
        mplus<tSizeBnlm>(tCnlm, tFuseWeight[k], aBnlm);
        tCnlm += tSizeBnlm;
    }
}
template <int NMAX, int LMAX, int FSIZE>
static inline void mplusCnlmFuse(double *rCnlm, double *aBnlm, double *aFuseWeight, int aType) noexcept {
    mplusCnlmFuse_<NMAX, LMAX, FSIZE, FALSE>(rCnlm, aBnlm, aFuseWeight, aType);
}
template <int NMAX, int LMAX, int FSIZE>
static inline void mplusCnlmExFuse(double *rCnlm, double *aBnlm, double *aFuseWeight, int aType) noexcept {
    mplusCnlmFuse_<NMAX, LMAX, FSIZE, TRUE>(rCnlm, aBnlm, aFuseWeight, aType);
}


template <int SIZEN, int LMAX, int PFSIZE>
static void mplusAnlm(double *rAnlm, double *aCnlm, double *aPostFuseWeight) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    double *tAnlm = rAnlm;
    double *tPostFuseWeight = aPostFuseWeight;
    for (int np = 0; np < PFSIZE; ++np) {
        double *tCnlm = aCnlm;
        for (int n = 0; n < SIZEN; ++n) {
            mplus<tLMAll>(tAnlm, tPostFuseWeight[n], tCnlm);
            tCnlm += tLMAll;
        }
        tPostFuseWeight += SIZEN;
        tAnlm += tLMAll;
    }
}



template <int SIZEN, int LMAX, int PFSIZE>
static void mplusGradAnlm(double *aGradAnlm, double *rGradCnlm, double *aPostFuseWeight) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    double *tGradCnlm = rGradCnlm;
    for (int n = 0; n < SIZEN; ++n) {
        double *tGradAnlm = aGradAnlm;
        for (int np = 0; np < PFSIZE; ++np) {
            mplus<tLMAll>(tGradCnlm, aPostFuseWeight[n + np*SIZEN], tGradAnlm);
            tGradAnlm += tLMAll;
        }
        tGradCnlm += tLMAll;
    }
}


template <int NMAX, int LMAX, int FSIZE, int EXFLAG>
static void calGradBnlmFuse_(double *aGradCnlm, double *rGradBnlm, double *aFuseWeight, int aType) noexcept {
    constexpr int tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    fill<tSizeBnlm>(rGradBnlm, 0.0);
    double *tFuseWeight = aFuseWeight + FSIZE*(aType-1);
    double *tGradCnlm = aGradCnlm;
    if (EXFLAG) {
        mplus<tSizeBnlm>(rGradBnlm, 1.0, tGradCnlm);
        tGradCnlm += tSizeBnlm;
    }
    for (int k = 0; k < FSIZE; ++k) {
        mplus<tSizeBnlm>(rGradBnlm, tFuseWeight[k], tGradCnlm);
        tGradCnlm += tSizeBnlm;
    }
}
template <int NMAX, int LMAX, int FSIZE>
static inline void calGradBnlmFuse(double *aGradCnlm, double *rGradBnlm, double *aFuseWeight, int aType) noexcept {
    calGradBnlmFuse_<NMAX, LMAX, FSIZE, FALSE>(aGradCnlm, rGradBnlm, aFuseWeight, aType);
}
template <int NMAX, int LMAX, int FSIZE>
static inline void calGradBnlmExFuse(double *aGradCnlm, double *rGradBnlm, double *aFuseWeight, int aType) noexcept {
    calGradBnlmFuse_<NMAX, LMAX, FSIZE, TRUE>(aGradCnlm, rGradBnlm, aFuseWeight, aType);
}

template <int NMAX, int LMAX, int WTFLAG>
static void gradCnlm2xyz_(int j, double *aGradCnlm, double *aGradCnlmWt, double *rGradY,
                          double *aY, double aFc, double *aRn, double aWt,
                          double aFcPx, double aFcPy, double aFcPz,
                          double *aRnPx, double *aRnPy, double *aRnPz,
                          double *aYPx, double *aYPy, double *aYPz,
                          double *rFx, double *rFy, double *rFz) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    // clear gradY here
    fill<tLMAll>(rGradY, 0.0);
    double tGradFc = 0.0;
    double rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    double *tGradCnlm = aGradCnlm;
    double *tGradCnlmWt = aGradCnlmWt;
    for (int n = 0; n <= NMAX; ++n) {
        const double tRnn = aRn[n];
        const double tMul = aFc * tRnn;
        double tGradRn = 0.0;
        for (int k = 0; k < tLMAll; ++k) {
            double subGradBnlm = tGradCnlm[k];
            if (WTFLAG) subGradBnlm += aWt*tGradCnlmWt[k];
            rGradY[k] += tMul * subGradBnlm;
            tGradRn += aY[k] * subGradBnlm;
        }
        tGradCnlm += tLMAll;
        if (WTFLAG) tGradCnlmWt += tLMAll;
        
        tGradFc += tRnn * tGradRn;
        tGradRn *= aFc;
        rFxj += tGradRn*aRnPx[n];
        rFyj += tGradRn*aRnPy[n];
        rFzj += tGradRn*aRnPz[n];
    }
    for (int k = 0; k < tLMAll; ++k) {
        const double subGradY = rGradY[k];
        rFxj += subGradY*aYPx[k];
        rFyj += subGradY*aYPy[k];
        rFzj += subGradY*aYPz[k];
    }
    rFxj += aFcPx*tGradFc;
    rFyj += aFcPy*tGradFc;
    rFzj += aFcPz*tGradFc;
    rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
}
template <int NMAX, int LMAX>
static inline void gradBnlm2xyz(int j, double *aGradBnlm, double *rGradY, double *aY, double aFc, double *aRn, double aFcPx, double aFcPy, double aFcPz,
                                double *aRnPx, double *aRnPy, double *aRnPz, double *aYPx, double *aYPy, double *aYPz, double *rFx, double *rFy, double *rFz) noexcept {
    gradCnlm2xyz_<NMAX, LMAX, FALSE>(j, aGradBnlm, NULL, rGradY, aY, aFc, aRn, 0, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}
template <int NMAX, int LMAX>
static inline void gradCnlmWt2xyz(int j, double *aGradCnlm, double *aGradCnlmWt, double *rGradY, double *aY, double aFc, double *aRn, double aWt, double aFcPx, double aFcPy, double aFcPz,
                                  double *aRnPx, double *aRnPy, double *aRnPz, double *aYPx, double *aYPy, double *aYPz, double *rFx, double *rFy, double *rFz) noexcept {
    gradCnlm2xyz_<NMAX, LMAX, TRUE>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}



template <int L>
static inline void calL2Sub_(double *aCnlm, double *rFp) noexcept {
    constexpr int tLen = L+L+1;
    const double rDot = dot<tLen>(aCnlm + (L*L));
    rFp[L-1] = (PI4/(double)tLen) * rDot;
}
template <int LMAX, int NORADIAL>
static void calSphL2(double *aCnlm, double *rFp) noexcept {
    // l == 0
    double *tFp = rFp;
    if (!NORADIAL) {
        const double tCn00 = aCnlm[0];
        tFp[0] = PI4 * tCn00*tCn00;
        ++tFp;
    }
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calL2Sub_<NNAPGENS_X>(aCnlm, tFp); if (LMAX==NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline double calL3SubSub_(double *aCnlm) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr double coeff = L3_COEFF[L3IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3];
}
template <int L3IDX>
static double calL3Sub_(double *aCnlm) noexcept {
    double rFp3 = 0.0;
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==NNAPGENS_X) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 0..<110
    return rFp3;
}
template <int L3MAX>
static void calSphL3(double *aCnlm, double *rFp) noexcept {
    if (L3MAX<=1) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL3Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL3Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX==3) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL3Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX==4) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL3Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX==5) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL3Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline double calL4SubSub_(double *aCnlm) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr double coeff = L4_COEFF[L4IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3]*aCnlm[i4];
}
template <int L4IDX>
static double calL4Sub_(double *aCnlm) noexcept {
    double rFp4 = 0.0;
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==NNAPGENS_X) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 0..<100
    return rFp4;
}
template <int L4MAX>
static void calSphL4(double *aCnlm, double *rFp) noexcept {
    if (L4MAX<1) return;
    rFp[0] = calL4Sub_<0>(aCnlm);
    if (L4MAX==1) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL4Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[NNAPGENS_X] = calL4Sub_<NNAPGENS_X>(aCnlm);
// <<< NNAPGEN REPEAT 3..8
}

template <int L>
static inline void calGradL2Sub_(double *aCnlm, double *rGradCnlm, double aSubNNGrad) noexcept {
    constexpr int tStart = L*L;
    constexpr int tLen = L+L+1;
    constexpr int tEnd = tStart+tLen;
    const double tMul = (2.0*PI4/(double)tLen) * aSubNNGrad;
    for (int i = tStart; i < tEnd; ++i) {
        rGradCnlm[i] += tMul * aCnlm[i];
    }
}
template <int LMAX, int NORADIAL>
static void calGradSphL2(double *aCnlm, double *rGradCnlm, double *aNNGrad) noexcept {
    // l = 0
    double *tNNGrad = aNNGrad;
    if (!NORADIAL) {
        rGradCnlm[0] += (2.0*PI4) * aCnlm[0] * tNNGrad[0];
        ++tNNGrad;
    }
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calGradL2Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, tNNGrad[NNAPGENS_X-1]); if (LMAX==NNAPGENS_X) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline void calGradL3SubSub_(double *aCnlm, double *rGradCnlm, double aSubNNGrad) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr double coeff = L3_COEFF[L3IDX][SUBIDX];
    const double tMul = coeff * aSubNNGrad;
    const double tCnlm1 = aCnlm[i1];
    const double tCnlm2 = aCnlm[i2];
    const double tCnlm3 = aCnlm[i3];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2;
}
template <int L3IDX>
static void calGradL3Sub_(double *aCnlm, double *rGradCnlm, double aSubNNGrad) noexcept {
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==NNAPGENS_X) {return;} calGradL3SubSub_<L3IDX, NNAPGENS_X>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<110
}
template <int L3MAX>
static void calGradSphL3(double *aCnlm, double *rGradCnlm, double *aNNGrad) noexcept {
    if (L3MAX <= 1) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX == 3) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX == 4) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX == 5) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline void calGradL4SubSub_(double *aCnlm, double *rGradCnlm, double aSubNNGrad) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr double coeff = L4_COEFF[L4IDX][SUBIDX];
    const double tMul = coeff * aSubNNGrad;
    const double tCnlm1 = aCnlm[i1];
    const double tCnlm2 = aCnlm[i2];
    const double tCnlm3 = aCnlm[i3];
    const double tCnlm4 = aCnlm[i4];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3*tCnlm4;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3*tCnlm4;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2*tCnlm4;
    rGradCnlm[i4] += tMul * tCnlm1*tCnlm2*tCnlm3;
}
template <int L4IDX>
static void calGradL4Sub_(double *aCnlm, double *rGradCnlm, double aSubNNGrad) noexcept {
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==NNAPGENS_X) {return;} calGradL4SubSub_<L4IDX, NNAPGENS_X>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<100
}
template <int L4MAX>
static void calGradSphL4(double *aCnlm, double *rGradCnlm, double *aNNGrad) noexcept {
    if (L4MAX < 1) return;
    calGradL4Sub_<0>(aCnlm, rGradCnlm, aNNGrad[0]);
    if (L4MAX == 1) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<NNAPGENS_X>(aCnlm, rGradCnlm, aNNGrad[NNAPGENS_X]);
// <<< NNAPGEN REPEAT 3..8
}

}

#endif //BASIS_SPHERICAL_UTIL_H