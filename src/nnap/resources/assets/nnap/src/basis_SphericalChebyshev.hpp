#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <jint WTYPE, jboolean FULL_CACHE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache,
                    jdouble aRCut, jint aNMax, jint aLMaxMax,
                    jdouble *aFuseWeight, jint aFuseSize) noexcept {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *rRn = NULL, *rY = NULL;
    jdouble *rBnlm = NULL;
    jdouble *rRnp = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL, *rNlY = NULL;
    jdouble *rNlBnlm = NULL;
    jdouble *rNlRnp = NULL;
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
    if (WTYPE==WTYPE_RFUSE) {
        if (FULL_CACHE) {
            rNlRnp = rNlY + aNN*tLMAll;
        } else {
            rRnp = rY + tLMAll;
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
        realSphericalHarmonicsFull4(aLMaxMax, dx, dy, dz, dis, rY);
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm(rBnlm, rY, fc, rRn, aNMax, aLMaxMax);
            // mplus2cnlm
            mplusCnlmFuse(rCnlm, rBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // get Rnp ref
            if (FULL_CACHE) rRnp = rNlRnp + j*aFuseSize;
            // mplus2cnlm
            mplusCnlmRFuse(rCnlm, rY, fc, rRn, rRnp, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusCnlm(rCnlm, rY, fc, rRn, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tCnlm = rCnlm + tSizeBnlm*(type-1);
            mplusCnlm(tCnlm, rY, fc, rRn, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tCnlmWt = rCnlm + tSizeBnlm*type;
            mplusCnlmWt(rCnlm, tCnlmWt, rY, fc, rRn, 1.0, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tCnlmWt = rCnlm + tSizeBnlm;
            mplusCnlmWt(rCnlm, tCnlmWt, rY, fc, rRn, wt, aNMax, aLMaxMax);
        }
    }
}
template <jint WTYPE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jdouble aRCut, jint aNMax, jint aLMaxMax,
                    jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calCnlm<WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize);
    } else {
        calCnlm<WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize);
    }
}

template <jint WTYPE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
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
    case WTYPE_RFUSE:   {tSizeN = aFuseSize;              break;}
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
    calCnlm<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rCnlm+tShift, rFp+tShiftFp, aLMax, aNoRadial);
        calL3_(rCnlm+tShift, rFp+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calL4_(rCnlm+tShift, rFp+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
}

template <jint WTYPE>
static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aGradCnlm, jdouble *aForwardCache, jdouble *rBackwardCache,
                                jdouble aRCut, jint aNMax, jint aLMaxMax, jint aFuseSize) {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    jdouble *tNlRn = NULL;
    jdouble *tNlFc = NULL;
    jdouble *tNlY = NULL;
    jdouble *tNlBnlm = NULL;
    jdouble *rNlGradRnp = NULL;
    if (WTYPE==WTYPE_FUSE) {
        tNlBnlm = aForwardCache + aNN*(aNMax+1 + 1 + tLMAll);
    }
    if (WTYPE==WTYPE_RFUSE) {
        tNlRn = aForwardCache;
        tNlFc = tNlRn + aNN*(aNMax+1);
        tNlY = tNlFc + aNN;
        rNlGradRnp = rBackwardCache;
    }
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        if (WTYPE==WTYPE_FUSE) {
            // get bnlm
            jdouble *tBnlm = tNlBnlm + j*tSizeBnlm;
            // mplus to gradPara
            mplusGradParaFuse(aGradCnlm, tBnlm, rGradPara, type, aFuseSize, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // get Rn, fc, Y
            jdouble *tRn = tNlRn + j*(aNMax+1);
            jdouble fc = tNlFc[j];
            jdouble *tY = tNlY + j*tLMAll;
            // get gradRnp ref
            jdouble *rGradRnp = rNlGradRnp + j*aFuseSize;
            // mplus to gradPara
            mplusGradParaRFuse(aGradCnlm, tY, fc, tRn, rGradRnp, rGradPara, type, aFuseSize, aNMax, aLMaxMax);
        }
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
    } else
    if (WTYPE==WTYPE_RFUSE) {
        tSizeN = aFuseSize;
    } else {
        tSizeN = 0;
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max] + (aL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm;
    jdouble *rGradCnlm = rBackwardCache;
    jdouble *rBackwardCacheElse = rGradCnlm + tSizeCnlm;
    // cal grad cnlm
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp, aLMax, aNoRadial);
        calGradL3_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calGradL4_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
    // plus to para
    calBackwardMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCut, aNMax, tLMaxMax, aFuseSize);
}

template <jint WTYPE, jboolean FULL_CACHE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache,
                             jdouble aRCut, jint aNMax, jint aLMaxMax,
                             jdouble *aFuseWeight, jint aFuseSize) {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlY = tNlFc + aNN;
    jdouble *tNlRnp = NULL;
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rYPx = NULL, *rYPy = NULL, *rYPz = NULL, *rYPtheta = NULL, *rYPphi = NULL;
    jdouble *rGradBnlm = NULL;
    jdouble *rGradRnp = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    jdouble *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    jdouble *rNlGradBnlm = NULL;
    jdouble *rNlGradRnp = NULL;
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
    if (WTYPE==WTYPE_RFUSE) {
        tNlRnp = tNlY + aNN*tLMAll;
        if (FULL_CACHE) {
            rNlGradRnp = rCheby2 + (aNMax+1);
        } else {
            rGradRnp = rCheby2 + (aNMax+1);
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
        calYPxyz(aLMaxMax, tY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // cal fxyz
        if (WTYPE==WTYPE_FUSE) {
            if (FULL_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmFuse(aGradCnlm, rGradBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
            gradBnlm2Fxyz(j, rGradBnlm, rYPtheta, tY, fc, tRn, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // get Rnp
            jdouble *tRnp = tNlRnp + j*aFuseSize;
            if (FULL_CACHE) {
                rGradRnp = rNlGradRnp + j*aFuseSize;
            }
            // get gradY, gradRn ref
            jdouble *rGradY = rYPtheta;
            jdouble *rGradRn = rCheby2;
            gradCnlm2FxyzRFuse(j, aGradCnlm, rGradY, rGradRn, rGradRnp, tY, fc, tRn, tRnp, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradBnlm2Fxyz(j, aGradCnlm, rYPtheta, tY, fc, tRn, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradBnlm = aGradCnlm + tSizeBnlm*(type-1);
            gradBnlm2Fxyz(j, tGradBnlm, rYPtheta, tY, fc, tRn, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tGradCnlmWt = aGradCnlm + tSizeBnlm*type;
            gradCnlmWt2Fxyz(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, 1.0, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            gradCnlmWt2Fxyz(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, wt, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        }
    }
}
template <jint WTYPE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                             jdouble aRCut, jint aNMax, jint aLMaxMax,
                             jdouble *aFuseWeight, jint aFuseSize) {
    if (aFullCache) {
        calForceMainLoop<WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize);
    } else {
        calForceMainLoop<WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize);
    }
}
template <jint WTYPE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
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
    case WTYPE_RFUSE:   {tSizeN = aFuseSize;              break;}
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
    calForceMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradCnlm, rFx, rFy, rFz, tForwardCacheElse, rForwardForceCacheElse, aFullCache, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
}

template <jint WTYPE>
static void calBackwardForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                     jdouble *rGradNNGradCnlm, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                                     jdouble *aNNGradCnlm, jdouble *rGradPara,
                                     jdouble *aForwardCache, jdouble *aForwardForceCache,
                                     jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                                     jdouble aRCut, jint aNMax, jint aLMaxMax,
                                     jdouble *aFuseWeight, jint aFuseSize) noexcept {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlY = tNlFc + aNN;
    jdouble *tNlRnp = NULL;
    jdouble *tNlRnPx = aForwardForceCache;
    jdouble *tNlRnPy = tNlRnPx + aNN*(aNMax+1);
    jdouble *tNlRnPz = tNlRnPy + aNN*(aNMax+1);
    jdouble *tNlFcPx = tNlRnPz + aNN*(aNMax+1);
    jdouble *tNlFcPy = tNlFcPx + aNN;
    jdouble *tNlFcPz = tNlFcPy + aNN;
    jdouble *tNlYPx = tNlFcPz + aNN;
    jdouble *tNlYPy = tNlYPx + aNN*tLMAll;
    jdouble *tNlYPz = tNlYPy + aNN*tLMAll;
    jdouble *tNlNNGradRnp = NULL;
    jdouble *rNlGradRnp = NULL;
    jdouble *rGradNNGradY = rBackwardForceCache;
    jdouble *rGradNNGradBnlm = NULL;
    jdouble *rGradNNGradRn = NULL;
    if (WTYPE==WTYPE_FUSE) {
        rGradNNGradBnlm = rGradNNGradY + tLMAll;
    }
    if (WTYPE==WTYPE_RFUSE) {
        tNlRnp = tNlY + aNN*tLMAll;
        tNlNNGradRnp =  tNlYPz + (aNN*tLMAll) + tLMAll + tLMAll + (aNMax+1);
        rNlGradRnp = rBackwardCache;
        rGradNNGradRn = rGradNNGradY + tLMAll;
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
            calGradNNGradBnlm(rGradNNGradBnlm, tY, fc, tRn, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
            mplusGradNNGradCnlmFuse(aNNGradCnlm, rGradNNGradCnlm, rGradNNGradBnlm, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aLMaxMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            jdouble *tRnp = tNlRnp + j*aFuseSize;
            jdouble *tNNGradRnp = NULL, *rGradRnp = NULL;
            if (!aFixBasis) {
                tNNGradRnp = tNlNNGradRnp + j*aFuseSize;
                rGradRnp = rNlGradRnp + j*aFuseSize;
            }
            mplusGradNNGradCnlmRFuse(aNNGradCnlm, rGradNNGradCnlm, tY, fc, tRn, tRnp, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aLMaxMax, aFixBasis, tYPx, tYPy, tYPz, rGradNNGradY, rGradNNGradRn, tNNGradRnp, rGradRnp, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusGradNNGradCnlm(rGradNNGradCnlm, tY, fc, tRn, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradNNGradCnlm = rGradNNGradCnlm + tSizeBnlm*(type-1);
            mplusGradNNGradCnlm(tGradNNGradCnlm, tY, fc, tRn, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *rGradNNGradCnlmWt = rGradNNGradCnlm + tSizeBnlm*type;
            mplusGradNNGradCnlmWt(rGradNNGradCnlm, rGradNNGradCnlmWt, tY, fc, tRn, 1.0, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *rGradNNGradCnlmWt = rGradNNGradCnlm + tSizeBnlm;
            mplusGradNNGradCnlmWt(rGradNNGradCnlm, rGradNNGradCnlmWt, tY, fc, tRn, wt, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
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
    case WTYPE_RFUSE:   {tSizeN = aFuseSize;              break;}
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
    jdouble *rBackwardCacheElse = rGradCnlm + tSizeCnlm;
    jdouble *rGradNNGradCnlm = rBackwardForceCache;
    jdouble *rBackwardForceCacheElse = rGradNNGradCnlm + tSizeCnlm;
    
    // cal rGradNNGradCnlm
    calBackwardForceMainLoop<WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, tNNGradCnlm, rGradPara, tForwardCacheElse, tForwardForceCacheElse, rBackwardCacheElse, rBackwardForceCacheElse, aFixBasis, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    
    // grad grad cnlm to grad grad fp
    const jint tShiftL3 = aNoRadial?aLMax:(aLMax+1);
    const jint tShiftL4 = tShiftL3 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
    for (jint n=0, tShift=0, tShiftFp=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradNNGradL2_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp, aLMax, aNoRadial);
        calGradNNGradL3_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tShiftL3, aL3Max, aL3Cross);
        calGradNNGradL4_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tShiftL4, aL4Max, aL4Cross);
    }
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_RFUSE) if (!aFixBasis) {
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
        case WTYPE_RFUSE: {
            calFp<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
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
        case WTYPE_RFUSE: {
            calFp<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
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
    } else
    if (aWType==WTYPE_RFUSE) {
        calBackward<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseSize);
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
        case WTYPE_RFUSE: {
            calForce<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
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
        case WTYPE_RFUSE: {
            calForce<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
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
        case WTYPE_RFUSE: {
            calBackwardForce<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
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
        case WTYPE_RFUSE: {
            calBackwardForce<WTYPE_RFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL3Cross, aL4Max, aL4Cross, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H