#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <jint LMAXMAX, jint WTYPE, jboolean FULL_CACHE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache,
                    jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    constexpr jint tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *rRn = NULL, *rY = NULL;
    jdouble *rBnlm = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL, *rNlY = NULL;
    jdouble *rNlBnlm = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNN*(aNMax+1);
        rNlY = rNlFc + aNN;
    } else {
        rRn = rForwardCache;
        rY = rRn + (aNMax+1);
    }
    if (WTYPE==WTYPE_FUSE) {
        if (FULL_CACHE) {
            rNlBnlm = rNlY + aNN*tLMAll;
        } else {
            rBnlm = rY + tLMAll;
        }
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fc = calFc(dis, aRCut);
        if (FULL_CACHE) rNlFc[j] = fc;
        // cal Rn
        if (FULL_CACHE) rRn = rNlRn + j*(aNMax+1);
        calRn(rRn, aNMax, dis, aRCut);
        // cal Y
        if (FULL_CACHE) rY = rNlY + j*tLMAll;
        realSphericalHarmonicsFull4<LMAXMAX>(dx, dy, dz, dis, rY);
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm<tLMAll>(rBnlm, rY, fc, rRn, aNMax);
            // mplus2cnlm
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tCnlm = rCnlm;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                mplusBnlm2Cnlm<tLMAll>(tCnlm, rBnlm, wt, aNMax);
                tFuseWeight += aTypeNum;
                tCnlm += tSizeBnlm;
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusCnlm<tLMAll>(rCnlm, rY, fc, rRn, aNMax);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tCnlm = rCnlm + tSizeBnlm*(type-1);
            mplusCnlm<tLMAll>(tCnlm, rY, fc, rRn, aNMax);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tCnlmWt = rCnlm + tSizeBnlm*type;
            mplusCnlmWt<tLMAll>(rCnlm, tCnlmWt, rY, fc, rRn, 1.0, aNMax);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tCnlmWt = rCnlm + tSizeBnlm;
            mplusCnlmWt<tLMAll>(rCnlm, tCnlmWt, rY, fc, rRn, wt, aNMax);
        }
    }
}
template <jint LMAXMAX, jint WTYPE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jint aTypeNum, jdouble aRCut, jint aNMax,
                    jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calCnlm<LMAXMAX, WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    } else {
        calCnlm<LMAXMAX, WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    }
}
template <jint WTYPE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax,
                    jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aLMaxMax) {
    case 0: {calCnlm<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 1: {calCnlm<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 2: {calCnlm<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 3: {calCnlm<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 4: {calCnlm<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 5: {calCnlm<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 6: {calCnlm<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 7: {calCnlm<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 8: {calCnlm<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 9: {calCnlm<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 10: {calCnlm<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 11: {calCnlm<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 12: {calCnlm<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}

template <jint WTYPE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                         jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross,
                         jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1); break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);     break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);        break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);    break;}
    default:            {tSizeN = 0;                      break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max] + (aL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    // init cache
    jdouble *rCnlm = rForwardCache;
    jdouble *rForwardCacheElse = rCnlm + tSizeCnlm;
    // clear cnlm first
    for (jint i = 0; i < tSizeCnlm; ++i) {
        rCnlm[i] = 0.0;
    }
    // do cal
    calCnlm<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aTypeNum, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rCnlm+tShift, rFp+tShiftFp, aLMax, aNoRadial);
        calL3_(rCnlm+tShift, rFp+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calL4_(rCnlm+tShift, rFp+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
}

template <jint LMAXMAX, jint WTYPE>
static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aNlBnlm,  jdouble *aGradCnlm,
                                jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize) {
    constexpr jint tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        jdouble *tBnlm = aNlBnlm + j*tSizeBnlm;
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tGradPara = rGradPara;
            jdouble *tGradCnlm = aGradCnlm;
            for (jint fi = 0; fi < aFuseSize; ++fi) {
                tGradPara[type-1] += dotBnlmGradCnlm<tLMAll>(tBnlm, tGradCnlm, aNMax);
                tGradPara += aTypeNum;
                tGradCnlm += tSizeBnlm;
            }
            continue;
        }
    }
}
template <jint WTYPE>
static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aNlBnlm,  jdouble *aGradCnlm,
                                jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax, jint aFuseSize) {
    switch (aLMaxMax) {
    case 0: {calBackwardMainLoop<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 1: {calBackwardMainLoop<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 2: {calBackwardMainLoop<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 3: {calBackwardMainLoop<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 4: {calBackwardMainLoop<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 5: {calBackwardMainLoop<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 6: {calBackwardMainLoop<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 7: {calBackwardMainLoop<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 8: {calBackwardMainLoop<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 9: {calBackwardMainLoop<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 10: {calBackwardMainLoop<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 11: {calBackwardMainLoop<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    case 12: {calBackwardMainLoop<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, aNlBnlm, aGradCnlm, aTypeNum, aRCut, aNMax, aFuseSize); return;}
    default: {return;}
    }
}
template <jint WTYPE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                        jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross, jint aFuseSize) noexcept {
    static_assert(WTYPE!=WTYPE_DEFAULT && WTYPE!=WTYPE_NONE && WTYPE!=WTYPE_FULL && WTYPE!=WTYPE_EXFULL, "WTYPE INVALID");
    // const init
    jint tSizeN;
    if (WTYPE==WTYPE_FUSE) {
        tSizeN = aFuseSize*(aNMax+1);
    } else {
        tSizeN = 0;
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max] + (aL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tNlBnlm = tCnlm + tSizeCnlm + aNN*(aNMax+1 + 1 + tLMAll);
    jdouble *rGradCnlm = rBackwardCache;
    // cal grad cnlm
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp, aLMax, aNoRadial);
        calGradL3_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calGradL4_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
    // plus to para
    calBackwardMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, tNlBnlm, rGradCnlm, aTypeNum, aRCut, aNMax, tLMaxMax, aFuseSize);
}

template <jint LMAXMAX, jint WTYPE, jboolean FULL_CACHE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) {
    constexpr jint tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlY = tNlFc + aNN;
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rYPx = NULL, *rYPy = NULL, *rYPz = NULL, *rYPtheta = NULL, *rYPphi = NULL;
    jdouble *rGradBnlm = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    jdouble *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    jdouble *rNlGradBnlm = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rForwardForceCache;
        rNlRnPy = rNlRnPx + aNN*(aNMax+1);
        rNlRnPz = rNlRnPy + aNN*(aNMax+1);
        rNlFcPx = rNlRnPz + aNN*(aNMax+1);
        rNlFcPy = rNlFcPx + aNN;
        rNlFcPz = rNlFcPy + aNN;
        rNlYPx = rNlFcPz + aNN;
        rNlYPy = rNlYPx + aNN*tLMAll;
        rNlYPz = rNlYPy + aNN*tLMAll;
        rYPtheta = rNlYPz + aNN*tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
    } else {
        rRnPx = rForwardForceCache;
        rRnPy = rRnPx + (aNMax+1);
        rRnPz = rRnPy + (aNMax+1);
        rYPx = rRnPz + (aNMax+1);
        rYPy = rYPx + tLMAll;
        rYPz = rYPy + tLMAll;
        rYPtheta = rYPz + tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
    }
    if (WTYPE==WTYPE_FUSE) {
        if (FULL_CACHE) {
            rNlGradBnlm = rCheby2 + (aNMax+1);
        } else {
            rGradBnlm = rCheby2 + (aNMax+1);
        }
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn Y
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(aNMax+1);
        jdouble *tY = tNlY + j*tLMAll;
        // cal fcPxyz
        jdouble fcPx, fcPy, fcPz;
        calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        if (FULL_CACHE) {
            rNlFcPx[j] = fcPx;
            rNlFcPy[j] = fcPy;
            rNlFcPz[j] = fcPz;
        }
        // cal RnPxyz
        if (FULL_CACHE) {
            rRnPx = rNlRnPx + j*(aNMax+1);
            rRnPy = rNlRnPy + j*(aNMax+1);
            rRnPz = rNlRnPz + j*(aNMax+1);
        }
        calRnPxyz(rRnPx, rRnPy, rRnPz, rCheby2, aNMax, dis, aRCut, dx, dy, dz);
        // cal Ylm
        if (FULL_CACHE) {
            rYPx = rNlYPx + j*tLMAll;
            rYPy = rNlYPy + j*tLMAll;
            rYPz = rNlYPz + j*tLMAll;
        }
        calYPxyz<LMAXMAX>(tY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // cal fxyz
        if (WTYPE==WTYPE_FUSE) {
            if (FULL_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            for (jint k = 0; k < tSizeBnlm; ++k) {
                rGradBnlm[k] = 0.0;
            }
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tGradCnlm = aGradCnlm;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                mplusBnlm2Cnlm<tLMAll>(rGradBnlm, tGradCnlm, wt, aNMax);
                tFuseWeight += aTypeNum;
                tGradCnlm += tSizeBnlm;
            }
            gradBnlm2Fxyz<tLMAll>(j, rGradBnlm, rYPtheta, tY, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradBnlm2Fxyz<tLMAll>(j, aGradCnlm, rYPtheta, tY, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradBnlm = aGradCnlm + tSizeBnlm*(type-1);
            gradBnlm2Fxyz<tLMAll>(j, tGradBnlm, rYPtheta, tY, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tGradCnlmWt = aGradCnlm + tSizeBnlm*type;
            gradCnlmWt2Fxyz<tLMAll>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, 1.0, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            gradCnlmWt2Fxyz<tLMAll>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, wt, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        }
    }
}
template <jint LMAXMAX, jint WTYPE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) {
    if (aFullCache) {
        calForceMainLoop<LMAXMAX, WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    } else {
        calForceMainLoop<LMAXMAX, WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    }
}
template <jint WTYPE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax,
                             jdouble *aFuseWeight, jint aFuseSize) {
    switch (aLMaxMax) {
    case 0: {calForceMainLoop<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 1: {calForceMainLoop<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 2: {calForceMainLoop<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 3: {calForceMainLoop<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 4: {calForceMainLoop<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 5: {calForceMainLoop<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 6: {calForceMainLoop<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 7: {calForceMainLoop<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 8: {calForceMainLoop<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 9: {calForceMainLoop<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 10: {calForceMainLoop<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 11: {calForceMainLoop<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 12: {calForceMainLoop<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}
template <jint WTYPE>
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                            jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross,
                            jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1); break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);     break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);        break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);    break;}
    default:            {tSizeN = 0;                      break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max] + (aL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm;
    jdouble *rGradCnlm = rForwardForceCache;
    jdouble *rForwardForceCacheElse = rGradCnlm + tSizeCnlm;
    // forward need init gradCnlm here
    for (jint i = 0; i < tSizeCnlm; ++i) {
        rGradCnlm[i] = 0.0;
    }
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp, aLMax, aNoRadial);
        calGradL3_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calGradL4_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
    calForceMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradCnlm, rFx, rFy, rFz, tForwardCacheElse, rForwardForceCacheElse, aFullCache, aTypeNum, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
}

template <jint LMAXMAX, jint WTYPE>
static void calBackwardForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                     jdouble *rGradNNGradCnlm, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                                     jdouble *aNNGradCnlm, jdouble *rGradPara,
                                     jdouble *aForwardCache, jdouble *aForwardForceCache,
                                     jdouble *rBackwardForceCache, jboolean aFixBasis,
                                     jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    constexpr jint tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlY = tNlFc + aNN;
    jdouble *tNlRnPx = aForwardForceCache;
    jdouble *tNlRnPy = tNlRnPx + aNN*(aNMax+1);
    jdouble *tNlRnPz = tNlRnPy + aNN*(aNMax+1);
    jdouble *tNlFcPx = tNlRnPz + aNN*(aNMax+1);
    jdouble *tNlFcPy = tNlFcPx + aNN;
    jdouble *tNlFcPz = tNlFcPy + aNN;
    jdouble *tNlYPx = tNlFcPz + aNN;
    jdouble *tNlYPy = tNlYPx + aNN*tLMAll;
    jdouble *tNlYPz = tNlYPy + aNN*tLMAll;
    jdouble *rGradNNGradY = rBackwardForceCache;
    jdouble *rGradNNGradBnlm = NULL;
    if (WTYPE==WTYPE_FUSE) {
        rGradNNGradBnlm = rGradNNGradY + tLMAll;
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get gradFxyz
        jdouble tGradFx = aGradFx[j], tGradFy = aGradFy[j], tGradFz = aGradFz[j];
        // get fc Rn Y
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(aNMax+1);
        jdouble *tY = tNlY + j*tLMAll;
        // get fcPxyz RnPxyz YPxyz
        jdouble fcPx = tNlFcPx[j], fcPy = tNlFcPy[j], fcPz = tNlFcPz[j];
        jdouble *tRnPx = tNlRnPx + j*(aNMax+1);
        jdouble *tRnPy = tNlRnPy + j*(aNMax+1);
        jdouble *tRnPz = tNlRnPz + j*(aNMax+1);
        jdouble *tYPx = tNlYPx + j*tLMAll;
        jdouble *tYPy = tNlYPy + j*tLMAll;
        jdouble *tYPz = tNlYPz + j*tLMAll;
        // mplus to gradNNgrad
        if (WTYPE==WTYPE_FUSE) {
            calGradNNGradBnlm<tLMAll>(rGradNNGradBnlm, tY, fc, tRn, aNMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tGradNNGradCnlm = rGradNNGradCnlm;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                mplusBnlm2Cnlm<tLMAll>(tGradNNGradCnlm, rGradNNGradBnlm, wt, aNMax);
                tGradNNGradCnlm += tSizeBnlm;
                tFuseWeight += aTypeNum;
            }
            if (!aFixBasis) {
                jdouble *tGradPara = rGradPara;
                jdouble *tNNGradCnlm = aNNGradCnlm;
                for (jint k = 0; k < aFuseSize; ++k) {
                    tGradPara[type-1] += dotBnlmGradCnlm<tLMAll>(tNNGradCnlm, rGradNNGradBnlm, aNMax);
                    tNNGradCnlm += tSizeBnlm;
                    tGradPara += aTypeNum;
                }
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusGradNNGradCnlm<tLMAll>(rGradNNGradCnlm, tY, fc, tRn, aNMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradNNGradCnlm = rGradNNGradCnlm + tSizeBnlm*(type-1);
            mplusGradNNGradCnlm<tLMAll>(tGradNNGradCnlm, tY, fc, tRn, aNMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *rGradNNGradCnlmWt = rGradNNGradCnlm + tSizeBnlm*type;
            mplusGradNNGradCnlmWt<tLMAll>(rGradNNGradCnlm, rGradNNGradCnlmWt, tY, fc, tRn, 1.0, aNMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *rGradNNGradCnlmWt = rGradNNGradCnlm + tSizeBnlm;
            mplusGradNNGradCnlmWt<tLMAll>(rGradNNGradCnlm, rGradNNGradCnlmWt, tY, fc, tRn, wt, aNMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
    }
}
template <jint WTYPE>
static void calBackwardForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                     jdouble *rGradNNGradCnlm, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                                     jdouble *aNNGradCnlm, jdouble *rGradPara,
                                     jdouble *aForwardCache, jdouble *aForwardForceCache,
                                     jdouble *rBackwardForceCache, jboolean aFixBasis,
                                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax,
                                     jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aLMaxMax) {
    case 0: {calBackwardForceMainLoop<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 1: {calBackwardForceMainLoop<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 2: {calBackwardForceMainLoop<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 3: {calBackwardForceMainLoop<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 4: {calBackwardForceMainLoop<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 5: {calBackwardForceMainLoop<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 6: {calBackwardForceMainLoop<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 7: {calBackwardForceMainLoop<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 8: {calBackwardForceMainLoop<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 9: {calBackwardForceMainLoop<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 10: {calBackwardForceMainLoop<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 11: {calBackwardForceMainLoop<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    case 12: {calBackwardForceMainLoop<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, aNNGradCnlm, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}
template <jint WTYPE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                             jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross,
                             jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1); break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);     break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);        break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);    break;}
    default:            {tSizeN = 0;                      break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max] + (aL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm;
    jdouble *tNNGradCnlm = aForwardForceCache;
    jdouble *tForwardForceCacheElse = tNNGradCnlm + tSizeCnlm;
    jdouble *rGradCnlm = rBackwardCache;
    jdouble *rGradNNGradCnlm = rBackwardForceCache;
    jdouble *rBackwardForceCacheElse = rGradNNGradCnlm + tSizeCnlm;
    
    // cal rGradNNGradCnlm
    calBackwardForceMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, tNNGradCnlm, rGradPara, tForwardCacheElse, tForwardForceCacheElse, rBackwardForceCacheElse, aFixBasis, aTypeNum, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    
    // grad grad cnlm to grad grad fp
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradNNGradL2_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp, aLMax, aNoRadial);
        calGradNNGradL3_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calGradNNGradL4_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
    if (WTYPE==WTYPE_FUSE) if (!aFixBasis) {
        for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
            calGradCnlmL2_(rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp, aLMax, aNoRadial);
            calGradCnlmL3_(tCnlm+tShift, rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp+tShiftL3, aL3Max, aL3Cross);
            calGradCnlmL4_(tCnlm+tShift, rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp+tShiftL4, aL4Max, aL4Cross);
        }
    }
}


static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                         jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross,
                         jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                        jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross, jint aWType, jint aFuseSize) noexcept {
    if (aWType==WTYPE_FUSE) {
        calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseSize);
    }
}
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                            jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                             jint aL3Max, jboolean aL3Cross, jint aL4Max, jboolean aL4Cross,
                             jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H