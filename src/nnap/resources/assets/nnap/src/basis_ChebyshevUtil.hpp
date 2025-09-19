#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <jint NMAX>
static void mplusFpFuse(jdouble *rFp, jdouble *aFuseWeight, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize) {
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(aType-1);
    jdouble *tFp = rFp;
    for (jint k = 0; k < aFuseSize; ++k) {
        mplus<NMAX+1>(tFp, aFc*tFuseWeight[k], aRn);
        tFp += (NMAX+1);
    }
}
static void mplusFpFuse(jdouble *rFp, jdouble *aFuseWeight, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {mplusFpFuse<0>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 1: {mplusFpFuse<1>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 2: {mplusFpFuse<2>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 3: {mplusFpFuse<3>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 4: {mplusFpFuse<4>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 5: {mplusFpFuse<5>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 6: {mplusFpFuse<6>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 7: {mplusFpFuse<7>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 8: {mplusFpFuse<8>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 9: {mplusFpFuse<9>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 10: {mplusFpFuse<10>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 11: {mplusFpFuse<11>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 12: {mplusFpFuse<12>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 13: {mplusFpFuse<13>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 14: {mplusFpFuse<14>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 15: {mplusFpFuse<15>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 16: {mplusFpFuse<16>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 17: {mplusFpFuse<17>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 18: {mplusFpFuse<18>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 19: {mplusFpFuse<19>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 20: {mplusFpFuse<20>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    default: {return;}
    }
}

template <jint NMAX>
static void mplusFpRFuse(jdouble *rFp, jdouble *aFuseWeight, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize) {
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(NMAX+1)*(aType-1);
    for (jint np = 0; np < aFuseSize; ++np) {
        rFp[np] += aFc*dot<NMAX+1>(tFuseWeight, aRn);
        tFuseWeight += (NMAX+1);
    }
}
static void mplusFpRFuse(jdouble *rFp, jdouble *aFuseWeight, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {mplusFpRFuse<0>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 1: {mplusFpRFuse<1>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 2: {mplusFpRFuse<2>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 3: {mplusFpRFuse<3>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 4: {mplusFpRFuse<4>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 5: {mplusFpRFuse<5>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 6: {mplusFpRFuse<6>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 7: {mplusFpRFuse<7>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 8: {mplusFpRFuse<8>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 9: {mplusFpRFuse<9>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 10: {mplusFpRFuse<10>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 11: {mplusFpRFuse<11>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 12: {mplusFpRFuse<12>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 13: {mplusFpRFuse<13>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 14: {mplusFpRFuse<14>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 15: {mplusFpRFuse<15>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 16: {mplusFpRFuse<16>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 17: {mplusFpRFuse<17>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 18: {mplusFpRFuse<18>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 19: {mplusFpRFuse<19>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    case 20: {mplusFpRFuse<20>(rFp, aFuseWeight, aType, aFc, aRn, aFuseSize); return;}
    default: {return;}
    }
}


template <jint NMAX>
static void mplusGradParaFuse(jdouble *aGradFp, jdouble *rGradPara, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize) {
    jdouble *tGradFp = aGradFp;
    jdouble *tGradPara = rGradPara + aFuseSize*(aType-1);
    for (jint k = 0; k < aFuseSize; ++k) {
        tGradPara[k] += aFc*dot<NMAX+1>(aRn, tGradFp);
        tGradFp += (NMAX+1);
    }
}
static void mplusGradParaFuse(jdouble *aGradFp, jdouble *rGradPara, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {mplusGradParaFuse<0>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 1: {mplusGradParaFuse<1>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 2: {mplusGradParaFuse<2>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 3: {mplusGradParaFuse<3>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 4: {mplusGradParaFuse<4>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 5: {mplusGradParaFuse<5>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 6: {mplusGradParaFuse<6>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 7: {mplusGradParaFuse<7>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 8: {mplusGradParaFuse<8>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 9: {mplusGradParaFuse<9>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 10: {mplusGradParaFuse<10>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 11: {mplusGradParaFuse<11>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 12: {mplusGradParaFuse<12>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 13: {mplusGradParaFuse<13>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 14: {mplusGradParaFuse<14>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 15: {mplusGradParaFuse<15>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 16: {mplusGradParaFuse<16>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 17: {mplusGradParaFuse<17>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 18: {mplusGradParaFuse<18>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 19: {mplusGradParaFuse<19>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 20: {mplusGradParaFuse<20>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    default: {return;}
    }
}

template <jint NMAX>
static void mplusGradParaRFuse(jdouble *aGradFp, jdouble *rGradPara, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize) {
    jdouble *tGradPara = rGradPara + aFuseSize*(NMAX+1)*(aType-1);
    for (jint np = 0; np < aFuseSize; ++np) {
        mplus<NMAX+1>(tGradPara, aFc*aGradFp[np], aRn);
        tGradPara += (NMAX+1);
    }
}
static void mplusGradParaRFuse(jdouble *aGradFp, jdouble *rGradPara, jint aType, jdouble aFc, jdouble *aRn, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {mplusGradParaRFuse<0>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 1: {mplusGradParaRFuse<1>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 2: {mplusGradParaRFuse<2>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 3: {mplusGradParaRFuse<3>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 4: {mplusGradParaRFuse<4>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 5: {mplusGradParaRFuse<5>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 6: {mplusGradParaRFuse<6>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 7: {mplusGradParaRFuse<7>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 8: {mplusGradParaRFuse<8>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 9: {mplusGradParaRFuse<9>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 10: {mplusGradParaRFuse<10>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 11: {mplusGradParaRFuse<11>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 12: {mplusGradParaRFuse<12>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 13: {mplusGradParaRFuse<13>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 14: {mplusGradParaRFuse<14>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 15: {mplusGradParaRFuse<15>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 16: {mplusGradParaRFuse<16>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 17: {mplusGradParaRFuse<17>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 18: {mplusGradParaRFuse<18>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 19: {mplusGradParaRFuse<19>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    case 20: {mplusGradParaRFuse<20>(aGradFp, rGradPara, aType, aFc, aRn, aFuseSize); return;}
    default: {return;}
    }
}


template <jint NMAX>
static void calGradRnFuse(jdouble *rGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jint aType, jint aFuseSize) {
    for (jint n = 0; n <= NMAX; ++n) {
        rGradRn[n] = 0.0;
    }
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(aType-1);
    jdouble *tNNGrad = aNNGrad;
    for (jint k = 0; k < aFuseSize; ++k) {
        mplus<NMAX+1>(rGradRn, tFuseWeight[k], tNNGrad);
        tNNGrad += (NMAX+1);
    }
}
static void calGradRnFuse(jdouble *rGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {calGradRnFuse<0>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 1: {calGradRnFuse<1>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 2: {calGradRnFuse<2>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 3: {calGradRnFuse<3>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 4: {calGradRnFuse<4>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 5: {calGradRnFuse<5>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 6: {calGradRnFuse<6>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 7: {calGradRnFuse<7>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 8: {calGradRnFuse<8>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 9: {calGradRnFuse<9>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 10: {calGradRnFuse<10>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 11: {calGradRnFuse<11>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 12: {calGradRnFuse<12>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 13: {calGradRnFuse<13>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 14: {calGradRnFuse<14>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 15: {calGradRnFuse<15>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 16: {calGradRnFuse<16>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 17: {calGradRnFuse<17>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 18: {calGradRnFuse<18>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 19: {calGradRnFuse<19>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 20: {calGradRnFuse<20>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    default: {return;}
    }
}

template <jint NMAX>
static void calGradRnRFuse(jdouble *rGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jint aType, jint aFuseSize) {
    for (jint n = 0; n <= NMAX; ++n) {
        rGradRn[n] = 0.0;
    }
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(NMAX+1)*(aType-1);
    for (jint np = 0; np < aFuseSize; ++np) {
        mplus<NMAX+1>(rGradRn, aNNGrad[np], tFuseWeight);
        tFuseWeight += (NMAX+1);
    }
}
static void calGradRnRFuse(jdouble *rGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax) {
    switch (aNMax) {
    case 0: {calGradRnRFuse<0>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 1: {calGradRnRFuse<1>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 2: {calGradRnRFuse<2>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 3: {calGradRnRFuse<3>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 4: {calGradRnRFuse<4>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 5: {calGradRnRFuse<5>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 6: {calGradRnRFuse<6>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 7: {calGradRnRFuse<7>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 8: {calGradRnRFuse<8>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 9: {calGradRnRFuse<9>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 10: {calGradRnRFuse<10>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 11: {calGradRnRFuse<11>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 12: {calGradRnRFuse<12>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 13: {calGradRnRFuse<13>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 14: {calGradRnRFuse<14>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 15: {calGradRnRFuse<15>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 16: {calGradRnRFuse<16>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 17: {calGradRnRFuse<17>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 18: {calGradRnRFuse<18>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 19: {calGradRnRFuse<19>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    case 20: {calGradRnRFuse<20>(rGradRn, aNNGrad, aFuseWeight, aType, aFuseSize); return;}
    default: {return;}
    }
}

template <jint NMAX, jboolean WT>
static void gradRn2Fxyz_(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt,
                         jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                         jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    jdouble tGradFc = 0.0;
    jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradRnn = aGradRn[n];
        if (WT) tGradRnn += aWt*aGradRnWt[n];
        const jdouble tRnn = aRn[n];
        tGradFc += tRnn * tGradRnn;
        tGradRnn *= aFc;
        rFxj += tGradRnn*aRnPx[n];
        rFyj += tGradRnn*aRnPy[n];
        rFzj += tGradRnn*aRnPz[n];
    }
    rFxj += aFcPx*tGradFc;
    rFyj += aFcPy*tGradFc;
    rFzj += aFcPz*tGradFc;
    rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
}
template <jboolean WT>
static void gradRn2Fxyz_(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax,
                         jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                         jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    switch (aNMax) {
    case 0: {gradRn2Fxyz_<0, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 1: {gradRn2Fxyz_<1, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 2: {gradRn2Fxyz_<2, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 3: {gradRn2Fxyz_<3, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 4: {gradRn2Fxyz_<4, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 5: {gradRn2Fxyz_<5, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 6: {gradRn2Fxyz_<6, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 7: {gradRn2Fxyz_<7, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 8: {gradRn2Fxyz_<8, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 9: {gradRn2Fxyz_<9, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 10: {gradRn2Fxyz_<10, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 11: {gradRn2Fxyz_<11, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 12: {gradRn2Fxyz_<12, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 13: {gradRn2Fxyz_<13, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 14: {gradRn2Fxyz_<14, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 15: {gradRn2Fxyz_<15, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 16: {gradRn2Fxyz_<16, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 17: {gradRn2Fxyz_<17, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 18: {gradRn2Fxyz_<18, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 19: {gradRn2Fxyz_<19, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    case 20: {gradRn2Fxyz_<20, WT>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz); return;}
    default: {return;}
    }
}
static inline void gradRn2Fxyz(jint j, jdouble *aGradRn, jdouble aFc, jdouble *aRn, jint aNMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradRn2Fxyz_<JNI_FALSE>(j, aGradRn, NULL, aFc, aRn, 0, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz);
}
static inline void gradRnWt2Fxyz(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradRn2Fxyz_<JNI_TRUE>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz);
}


template <jint NMAX, jboolean MPLUS, jboolean WT>
static void calormplusGradNNGrad_(jdouble *rGradNNGrad, jdouble *rGradNNGradWt, jdouble aFc, jdouble *aRn, jdouble aWt,
                                  jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                                  jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    // cal gradNNgradFc
    const jdouble tGradNNGradFc = aFcPx*aGradFx + aFcPy*aGradFy + aFcPz*aGradFz;
    // cal gradNNgrad
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradNNGradRn = aRnPx[n]*aGradFx + aRnPy[n]*aGradFy + aRnPz[n]*aGradFz;
        tGradNNGradRn = aRn[n]*tGradNNGradFc + aFc*tGradNNGradRn;
        if (MPLUS) {
            rGradNNGrad[n] += tGradNNGradRn;
            if (WT) rGradNNGradWt[n] += aWt*tGradNNGradRn;
        } else {
            rGradNNGrad[n] = tGradNNGradRn;
            if (WT) rGradNNGradWt[n] = aWt*tGradNNGradRn;
        }
    }
}
template <jboolean MPLUS, jboolean WT>
static void calormplusGradNNGrad_(jdouble *rGradNNGrad, jdouble *rGradNNGradWt, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax,
                                  jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                                  jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    switch (aNMax) {
    case 0: {calormplusGradNNGrad_<0, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 1: {calormplusGradNNGrad_<1, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 2: {calormplusGradNNGrad_<2, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 3: {calormplusGradNNGrad_<3, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 4: {calormplusGradNNGrad_<4, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 5: {calormplusGradNNGrad_<5, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 6: {calormplusGradNNGrad_<6, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 7: {calormplusGradNNGrad_<7, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 8: {calormplusGradNNGrad_<8, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 9: {calormplusGradNNGrad_<9, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 10: {calormplusGradNNGrad_<10, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 11: {calormplusGradNNGrad_<11, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 12: {calormplusGradNNGrad_<12, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 13: {calormplusGradNNGrad_<13, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 14: {calormplusGradNNGrad_<14, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 15: {calormplusGradNNGrad_<15, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 16: {calormplusGradNNGrad_<16, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 17: {calormplusGradNNGrad_<17, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 18: {calormplusGradNNGrad_<18, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 19: {calormplusGradNNGrad_<19, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 20: {calormplusGradNNGrad_<20, MPLUS, WT>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    default: {return;}
    }
}
static inline void calGradNNGradRn(jdouble *rGradNNGradRn, jdouble aFc, jdouble *aRn, jint aNMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    calormplusGradNNGrad_<JNI_FALSE, JNI_FALSE>(rGradNNGradRn, NULL, aFc, aRn, 0, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}
static inline void mplusGradNNGrad(jdouble *rGradNNGrad, jdouble aFc, jdouble *aRn, jint aNMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    calormplusGradNNGrad_<JNI_TRUE, JNI_FALSE>(rGradNNGrad, NULL, aFc, aRn, 0, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}
static inline void mplusGradNNGradWt(jdouble *rGradNNGrad, jdouble *rGradNNGradWt, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    calormplusGradNNGrad_<JNI_TRUE, JNI_TRUE>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}

template <jint NMAX>
static void mplusGradNNGradFuse(jdouble *rGradNNGrad, jdouble *rGradNNGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jboolean aFixBasis) {
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(aType-1);
    jdouble *tGradNNGrad = rGradNNGrad;
    for (jint k = 0; k < aFuseSize; ++k) {
        mplus<NMAX+1>(tGradNNGrad, tFuseWeight[k], rGradNNGradRn);
        tGradNNGrad += (NMAX+1);
    }
    if (!aFixBasis) {
        jdouble *tGradPara = rGradPara + aFuseSize*(aType-1);
        jdouble *tNNGrad = aNNGrad;
        for (jint k = 0; k < aFuseSize; ++k) {
            tGradPara[k] += dot<NMAX+1>(tNNGrad, rGradNNGradRn);
            tNNGrad += (NMAX+1);
        }
    }
}
static void mplusGradNNGradFuse(jdouble *rGradNNGrad, jdouble *rGradNNGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jboolean aFixBasis) {
    switch (aNMax) {
    case 0: {mplusGradNNGradFuse<0>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 1: {mplusGradNNGradFuse<1>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 2: {mplusGradNNGradFuse<2>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 3: {mplusGradNNGradFuse<3>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 4: {mplusGradNNGradFuse<4>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 5: {mplusGradNNGradFuse<5>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 6: {mplusGradNNGradFuse<6>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 7: {mplusGradNNGradFuse<7>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 8: {mplusGradNNGradFuse<8>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 9: {mplusGradNNGradFuse<9>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 10: {mplusGradNNGradFuse<10>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 11: {mplusGradNNGradFuse<11>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 12: {mplusGradNNGradFuse<12>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 13: {mplusGradNNGradFuse<13>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 14: {mplusGradNNGradFuse<14>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 15: {mplusGradNNGradFuse<15>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 16: {mplusGradNNGradFuse<16>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 17: {mplusGradNNGradFuse<17>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 18: {mplusGradNNGradFuse<18>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 19: {mplusGradNNGradFuse<19>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 20: {mplusGradNNGradFuse<20>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    default: {return;}
    }
}

template <jint NMAX>
static void mplusGradNNGradRFuse(jdouble *rGradNNGrad, jdouble *rGradNNGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jboolean aFixBasis) {
    jdouble *tFuseWeight = aFuseWeight + aFuseSize*(NMAX+1)*(aType-1);
    for (jint np = 0; np < aFuseSize; ++np) {
        rGradNNGrad[np] += dot<NMAX+1>(tFuseWeight, rGradNNGradRn);
        tFuseWeight += (NMAX+1);
    }
    if (!aFixBasis) {
        jdouble *tGradPara = rGradPara + aFuseSize*(NMAX+1)*(aType-1);
        for (jint np = 0; np < aFuseSize; ++np) {
            mplus<NMAX+1>(tGradPara, aNNGrad[np], rGradNNGradRn);
            tGradPara += (NMAX+1);
        }
    }
}
static void mplusGradNNGradRFuse(jdouble *rGradNNGrad, jdouble *rGradNNGradRn, jdouble *aNNGrad, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jboolean aFixBasis) {
    switch (aNMax) {
    case 0: {mplusGradNNGradRFuse<0>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 1: {mplusGradNNGradRFuse<1>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 2: {mplusGradNNGradRFuse<2>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 3: {mplusGradNNGradRFuse<3>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 4: {mplusGradNNGradRFuse<4>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 5: {mplusGradNNGradRFuse<5>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 6: {mplusGradNNGradRFuse<6>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 7: {mplusGradNNGradRFuse<7>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 8: {mplusGradNNGradRFuse<8>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 9: {mplusGradNNGradRFuse<9>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 10: {mplusGradNNGradRFuse<10>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 11: {mplusGradNNGradRFuse<11>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 12: {mplusGradNNGradRFuse<12>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 13: {mplusGradNNGradRFuse<13>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 14: {mplusGradNNGradRFuse<14>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 15: {mplusGradNNGradRFuse<15>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 16: {mplusGradNNGradRFuse<16>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 17: {mplusGradNNGradRFuse<17>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 18: {mplusGradNNGradRFuse<18>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 19: {mplusGradNNGradRFuse<19>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 20: {mplusGradNNGradRFuse<20>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    default: {return;}
    }
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H