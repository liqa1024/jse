#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <jint WTYPE>
static void calSystemScale(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                           jdouble *rSystemScale, jdouble *rForwardCache,
                           jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax, jint aFuseSize,
                           jdouble *aRFuncScale) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeN = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeN = 0;                       break;}
    }
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeScale = tSizeN*(aLMaxMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    // init cache
    jdouble *rCnlm = rForwardCache;
    jdouble *rRn = rCnlm + tSizeCnlm;
    jdouble *rY = rRn + (aNMax+1);
    // clear cnlm first
    fill(rCnlm, 0.0, tSizeCnlm);
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fc = calFc(dis, aRCut);
        // cal Rn
        calRn(rRn, aNMax, dis, aRCut, aRFuncScale);
        // cal Y
        realSphericalHarmonicsFull4(aLMaxMax, dx, dy, dz, dis, rY);
        // scale Y here
        multiply(rY, SQRT_PI4, tLMAll);
        // cal cnlm
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tCnlmWt = rCnlm + tSizeBnlm;
            mplusCnlmWt(rCnlm, tCnlmWt, rY, fc, rRn, wt, aNMax, aLMaxMax);
        } else {
            mplusCnlm(rCnlm, rY, fc, rRn, aNMax, aLMaxMax);
        }
    }
    // repeat for advanced wtype
    if (WTYPE==WTYPE_FUSE) {
        jdouble *tCnlm = rCnlm + tSizeBnlm;
        for (jint k = 1; k < aFuseSize; ++k) {
            fill(tCnlm, rCnlm, tSizeBnlm);
            tCnlm += tSizeBnlm;
        }
    } else
    if (WTYPE==WTYPE_EXFUSE) {
        jdouble *tCnlm = rCnlm + tSizeBnlm;
        for (jint k = 0; k < aFuseSize; ++k) {
            fill(tCnlm, rCnlm, tSizeBnlm);
            tCnlm += tSizeBnlm;
        }
    } else
    if (WTYPE==WTYPE_FULL) {
        jdouble *tCnlm = rCnlm + tSizeBnlm;
        for (jint k = 1; k < aTypeNum; ++k) {
            fill(tCnlm, rCnlm, tSizeBnlm);
            tCnlm += tSizeBnlm;
        }
    } else
    if (WTYPE==WTYPE_EXFULL) {
        jdouble *tCnlm = rCnlm + tSizeBnlm;
        for (jint k = 0; k < aTypeNum; ++k) {
            fill(tCnlm, rCnlm, tSizeBnlm);
            tCnlm += tSizeBnlm;
        }
    }
    // cal scale
    for (jint n=0, tShift=0, tShiftS=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftS+=aLMaxMax+1) {
        calL2_(rCnlm+tShift, rSystemScale+tShiftS, aLMaxMax, JNI_FALSE, JNI_TRUE);
    }
    // poly scale here
    for (jint i = 0; i < tSizeScale; ++i) {
        rSystemScale[i] = sqrt((double)rSystemScale[i]);
    }
}

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache,
                    jdouble aRCut, jint aNMax, jint aLMaxMax,
                    jdouble *aFuseWeight, jint aFuseSize,
                    jdouble *aRFuncScale, jboolean aSphScale) noexcept {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
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
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
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
        calRn(rRn, aNMax, dis, aRCut, aRFuncScale);
        // cal Y
        if (FULL_CACHE) rY = rNlY + j*tLMAll;
        realSphericalHarmonicsFull4(aLMaxMax, dx, dy, dz, dis, rY);
        // scale Y here
        if (aSphScale) multiply(rY, SQRT_PI4, tLMAll);
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm(rBnlm, rY, fc, rRn, aNMax, aLMaxMax);
            // mplus2cnlm
            mplusCnlmFuse<FSTYLE>(rCnlm, rBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm(rBnlm, rY, fc, rRn, aNMax, aLMaxMax);
            // mplus2cnlm
            mplusCnlmExFuse<FSTYLE>(rCnlm, rBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
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
template <jint WTYPE, jint FSTYLE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jdouble aRCut, jint aNMax, jint aLMaxMax,
                    jdouble *aFuseWeight, jint aFuseSize,
                    jdouble *aRFuncScale, jboolean aSphScale) noexcept {
    if (aFullCache) {
        calCnlm<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize, aRFuncScale, aSphScale);
    } else {
        calCnlm<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize, aRFuncScale, aSphScale);
    }
}

template <jint WTYPE, jint FSTYLE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial, jint aL3Max, jint aL4Max,
                  jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                  jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeN = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeN = 0;                       break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeFp = aPostFuseWeight==NULL ? (tSizeN*tSizeL) : (aPostFuseSize*tSizeL);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aPostFuseWeight==NULL ? 0 : (aPostFuseSize*tLMAll);
    // init cache
    jdouble *rFp_ = rForwardCache;
    jdouble *rCnlm = rFp_ + tSizeFp;
    jdouble *rAnlm = rCnlm + tSizeCnlm;
    jdouble *rForwardCacheElse = rAnlm + tSizeAnlm;
    // clear cnlm first
    fill(rCnlm, 0.0, tSizeCnlm);
    // do cal
    calCnlm<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize, aRFuncScale, aSphScale);
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftS+=tLMaxMax+1) {
        multiplyLM(rCnlm+tShift, aSystemScale+tShiftS, tLMaxMax);
    }
    // cnlm -> anlm
    if (aPostFuseWeight==NULL) {
        rAnlm = rCnlm;
    } else {
        // clear anlm first
        fill(rAnlm, 0.0, tSizeAnlm);
        mplusAnlm<FSTYLE>(rAnlm, rCnlm, aPostFuseWeight, aPostFuseSize, tSizeN, tLMaxMax);
        // scale anlm here
        multiply(rAnlm, aPostFuseScale, tSizeAnlm);
    }
    const jint tSizeL2 = aNoRadial?aLMax:(aLMax+1);
    const jint tSizeL3 = L3NCOLS[aL3Max];
    const jint tSizeNp = aPostFuseWeight==NULL ? tSizeN : aPostFuseSize;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rAnlm+tShift, rFp_+tShiftFp, aLMax, aNoRadial, aSphScale);
        calL3_(rAnlm+tShift, rFp_+tShiftFp+tSizeL2, aL3Max);
        calL4_(rAnlm+tShift, rFp_+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    if (aPolyScale) {
        for (jint i = 0; i < tSizeFp; ++i) {
            rFp_[i] = tanh((double)rFp_[i]*0.5)*2.0;
        }
    }
    fill(rFp, rFp_, tSizeFp);
}

template <jint WTYPE, jint FSTYLE>
static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aGradCnlm, jdouble *aForwardCache, jdouble *rBackwardCache,
                                jdouble aRCut, jint aNMax, jint aLMaxMax, jint aFuseSize) {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
    const jint tSizeBnlm = (aNMax+1)*tLMAll;
    jdouble *tNlBnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        tNlBnlm = aForwardCache + aNN*(aNMax+1 + 1 + tLMAll);
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
            mplusGradParaFuse<FSTYLE>(aGradCnlm, tBnlm, rGradPara, type, aFuseSize, aNMax, aLMaxMax);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            // get bnlm
            jdouble *tBnlm = tNlBnlm + j*tSizeBnlm;
            // mplus to gradPara
            jdouble *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            mplusGradParaFuse<FSTYLE>(tGradCnlmWt, tBnlm, rGradPara, type, aFuseSize, aNMax, aLMaxMax);
        }
    }
}
template <jint WTYPE, jint FSTYLE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial, jint aL3Max, jint aL4Max,
                        jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                        jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE && aPostFuseWeight==NULL) return;
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeN = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeN = 0;                       break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeFp = aPostFuseWeight==NULL ? (tSizeN*tSizeL) : (aPostFuseSize*tSizeL);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aPostFuseWeight==NULL ? 0 : (aPostFuseSize*tLMAll);
    // init cache
    jdouble *tFp = aForwardCache;
    jdouble *tCnlm = tFp + tSizeFp;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tForwardCacheElse = tAnlm + tSizeAnlm;
    jdouble *rGradFp = rBackwardCache;
    jdouble *rGradCnlm = NULL;
    jdouble *rGradAnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradCnlm = rGradFp + tSizeFp;
        rGradAnlm = rGradCnlm + tSizeCnlm;
    } else {
        rGradAnlm = rGradFp + tSizeFp;
    }
    jdouble *rBackwardCacheElse = rGradAnlm + tSizeAnlm;
    if (aPostFuseWeight==NULL) {
        tAnlm = tCnlm;
        rGradAnlm = rGradCnlm;
    }
    if (aPolyScale) {
        for (jint i = 0; i < tSizeFp; ++i) {
            const jdouble tSubFp = tFp[i];
            rGradFp[i] += (1.0 - 0.25*tSubFp*tSubFp) * aGradFp[i];
        }
    } else {
        for (jint i = 0; i < tSizeFp; ++i) {
            rGradFp[i] += aGradFp[i];
        }
    }
    // cal grad cnlm
    const jint tSizeL2 = aNoRadial?aLMax:(aLMax+1);
    const jint tSizeL3 = L3NCOLS[aL3Max];
    const jint tSizeNp = aPostFuseWeight==NULL ? tSizeN : aPostFuseSize;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tAnlm+tShift, rGradAnlm+tShift, rGradFp+tShiftFp, aLMax, aNoRadial, aSphScale);
        calGradL3_(tAnlm+tShift, rGradAnlm+tShift, rGradFp+tShiftFp+tSizeL2, aL3Max);
        calGradL4_(tAnlm+tShift, rGradAnlm+tShift, rGradFp+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    // anlm stuffs
    if (aPostFuseWeight!=NULL) {
        jdouble *tGradPara = rGradPara;
        if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                tGradPara += aTypeNum*aFuseSize;
            } else {
                tGradPara += aTypeNum*(aNMax+1)*(tLMaxMax+1)*aFuseSize;
            }
        }
        // scale anlm here
        for (jint i = 0; i < tSizeAnlm; ++i) {
            rGradAnlm[i] *= aPostFuseScale;
        }
        mplusGradParaPostFuse<FSTYLE>(rGradAnlm, tCnlm, tGradPara, aPostFuseSize, tSizeN, tLMaxMax);
        if (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE) return;
        // anlm -> cnlm
        mplusGradAnlm<FSTYLE>(rGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, tSizeN, tLMaxMax);
    }
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftS+=tLMaxMax+1) {
        multiplyLM(rGradCnlm+tShift, aSystemScale+tShiftS, tLMaxMax);
    }
    // plus to para
    calBackwardMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCut, aNMax, tLMaxMax, aFuseSize);
}

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache,
                             jdouble aRCut, jint aNMax, jint aLMaxMax,
                             jdouble *aFuseWeight, jint aFuseSize, jdouble *aRFuncScale) {
    const jint tLMAll = (aLMaxMax+1)*(aLMaxMax+1);
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
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
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
        calRnPxyz(rRnPx, rRnPy, rRnPz, rCheby2, aNMax, dis, aRCut, dx, dy, dz, aRFuncScale);
        // cal Ylm
        if (FULL_CACHE) {
            rYPx = rNlYPx + j*tLMAll;
            rYPy = rNlYPy + j*tLMAll;
            rYPz = rNlYPz + j*tLMAll;
        }
        calYPxyz(aLMaxMax, tY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // scale tY is scaled, so YPxyz no need scale
        // cal fxyz
        if (WTYPE==WTYPE_FUSE) {
            if (FULL_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmFuse<FSTYLE>(aGradCnlm, rGradBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
            gradBnlm2Fxyz(j, rGradBnlm, rYPtheta, tY, fc, tRn, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            if (FULL_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmExFuse<FSTYLE>(aGradCnlm, rGradBnlm, aFuseWeight, type, aFuseSize, aNMax, aLMaxMax);
            gradBnlm2Fxyz(j, rGradBnlm, rYPtheta, tY, fc, tRn, aNMax, aLMaxMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rFx, rFy, rFz);
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
template <jint WTYPE, jint FSTYLE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                             jdouble aRCut, jint aNMax, jint aLMaxMax,
                             jdouble *aFuseWeight, jint aFuseSize, jdouble *aRFuncScale) {
    if (aFullCache) {
        calForceMainLoop<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize, aRFuncScale);
    } else {
        calForceMainLoop<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCut, aNMax, aLMaxMax, aFuseWeight, aFuseSize, aRFuncScale);
    }
}
template <jint WTYPE, jint FSTYLE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial, jint aL3Max, jint aL4Max,
                     jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                     jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeN = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeN = 0;                       break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeFp = aPostFuseWeight==NULL ? (tSizeN*tSizeL) : (aPostFuseSize*tSizeL);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aPostFuseWeight==NULL ? 0 : (aPostFuseSize*tLMAll);
    // init cache
    jdouble *tFp = aForwardCache;
    jdouble *tCnlm = tFp + tSizeFp;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tForwardCacheElse = tAnlm + tSizeAnlm;
    jdouble *rNNGrad = rForwardForceCache;
    jdouble *rGradCnlm = rNNGrad + tSizeFp;
    jdouble *rGradAnlm = rGradCnlm + tSizeCnlm;
    jdouble *rForwardForceCacheElse = rGradAnlm + tSizeAnlm;
    // forward need init gradAnlm gradCnlm here
    fill(rGradAnlm, 0.0, tSizeAnlm);
    fill(rGradCnlm, 0.0, tSizeCnlm);
    if (aPostFuseWeight==NULL) {
        tAnlm = tCnlm;
        rGradAnlm = rGradCnlm;
    }
    if (aPolyScale) {
        for (jint i = 0; i < tSizeFp; ++i) {
            const jdouble tSubFp = tFp[i];
            rNNGrad[i] = (1.0 - 0.25*tSubFp*tSubFp) * aNNGrad[i];
        }
    } else {
        for (jint i = 0; i < tSizeFp; ++i) {
            rNNGrad[i] = aNNGrad[i];
        }
    }
    const jint tSizeL2 = aNoRadial?aLMax:(aLMax+1);
    const jint tSizeL3 = L3NCOLS[aL3Max];
    const jint tSizeNp = aPostFuseWeight==NULL ? tSizeN : aPostFuseSize;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tAnlm+tShift, rGradAnlm+tShift, rNNGrad+tShiftFp, aLMax, aNoRadial, aSphScale);
        calGradL3_(tAnlm+tShift, rGradAnlm+tShift, rNNGrad+tShiftFp+tSizeL2, aL3Max);
        calGradL4_(tAnlm+tShift, rGradAnlm+tShift, rNNGrad+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    if (aPostFuseWeight!=NULL) {
        // scale anlm here
        multiply(rGradAnlm, aPostFuseScale, tSizeAnlm);
        // anlm -> cnlm
        mplusGradAnlm<FSTYLE>(rGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, tSizeN, tLMaxMax);
    }
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftS+=tLMaxMax+1) {
        multiplyLM(rGradCnlm+tShift, aSystemScale+tShiftS, tLMaxMax);
    }
    calForceMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradCnlm, rFx, rFy, rFz, tForwardCacheElse, rForwardForceCacheElse, aFullCache, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize, aRFuncScale);
}

template <jint WTYPE, jint FSTYLE>
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
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
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
            calGradNNGradBnlm(rGradNNGradBnlm, tY, fc, tRn, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
            mplusGradNNGradCnlmFuse<FSTYLE>(aNNGradCnlm, rGradNNGradCnlm, rGradNNGradBnlm, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aLMaxMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            calGradNNGradBnlm(rGradNNGradBnlm, tY, fc, tRn, aNMax, aLMaxMax, tYPx, tYPy, tYPz, rGradNNGradY, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
            mplusGradNNGradCnlmExFuse<FSTYLE>(aNNGradCnlm, rGradNNGradCnlm, rGradNNGradBnlm, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aLMaxMax, aFixBasis);
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
template <jboolean PASS_BASIS>
static void polyScaleGradGrad_(jdouble *aFp, jdouble *rGradFp, jdouble *aNNGrad, jdouble *aGradNNGrad0, jdouble *rGradNNGrad, jint aSizeFp) {
    for (jint i = 0; i < aSizeFp; ++i) {
        const jdouble tSubFp = aFp[i];
        const jdouble tanhGradFp = 1.0 - 0.25*tSubFp*tSubFp;
        const jdouble tSubGradNNGrad = aGradNNGrad0[i];
        rGradNNGrad[i] += tSubGradNNGrad * tanhGradFp;
        if (PASS_BASIS) {
            rGradFp[i] += -0.5 * tSubFp * tanhGradFp * tSubGradNNGrad * aNNGrad[i];
        }
    }
}
template <jint WTYPE, jint FSTYLE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial, jint aL3Max, jint aL4Max,
                             jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                             jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    // const init
    jint tSizeN;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeN = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeN = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeN = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeN = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeN = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeN = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeN = 0;                       break;}
    }
    const jint tSizeL = (aNoRadial?aLMax:(aLMax+1)) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tSizeFp = aPostFuseWeight==NULL ? (tSizeN*tSizeL) : (aPostFuseSize*tSizeL);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aPostFuseWeight==NULL ? 0 : (aPostFuseSize*tLMAll);
    // init cache
    jdouble *tFp = aForwardCache;
    jdouble *tCnlm = tFp + tSizeFp;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tForwardCacheElse = tAnlm + tSizeAnlm;
    jdouble *tNNGrad = aForwardForceCache;
    jdouble *tNNGradCnlm = tNNGrad + tSizeFp;
    jdouble *tNNGradAnlm = tNNGradCnlm + tSizeCnlm;
    jdouble *tForwardForceCacheElse = tNNGradAnlm + tSizeAnlm;
    jdouble *rGradFp = rBackwardCache;
    jdouble *rGradCnlm = NULL;
    jdouble *rGradAnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradCnlm = rGradFp + tSizeFp;
        rGradAnlm = rGradCnlm + tSizeCnlm;
    } else {
        rGradAnlm = rGradFp + tSizeFp;
    }
    jdouble *rBackwardCacheElse = rGradAnlm + tSizeAnlm;
    jdouble *rGradNNGrad_ = rBackwardForceCache;
    jdouble *rGradNNGradCnlm = rGradNNGrad_ + tSizeFp;
    jdouble *rGradNNGradAnlm = rGradNNGradCnlm + tSizeCnlm;
    jdouble *rBackwardForceCacheElse = rGradNNGradAnlm + tSizeAnlm;
    
    // cal rGradNNGradCnlm
    calBackwardForceMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, tNNGradCnlm, rGradPara, tForwardCacheElse, tForwardForceCacheElse, rBackwardCacheElse, rBackwardForceCacheElse, aFixBasis, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<tSizeN; ++n, tShift+=tLMAll, tShiftS+=tLMaxMax+1) {
        multiplyLM(rGradNNGradCnlm+tShift, aSystemScale+tShiftS, tLMaxMax);
    }
    if (aPostFuseWeight!=NULL) {
        if (!aFixBasis) {
            jdouble *tGradPara = rGradPara;
            if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
                if (FSTYLE==FUSE_STYLE_LIMITED) {
                    tGradPara += aTypeNum*aFuseSize;
                } else {
                    tGradPara += aTypeNum*(aNMax+1)*(tLMaxMax+1)*aFuseSize;
                }
            }
            mplusGradParaPostFuse<FSTYLE>(tNNGradAnlm, rGradNNGradCnlm, tGradPara, aPostFuseSize, tSizeN, tLMaxMax);
        }
        // cnlm -> anlm
        mplusAnlm<FSTYLE>(rGradNNGradAnlm, rGradNNGradCnlm, aPostFuseWeight, aPostFuseSize, tSizeN, tLMaxMax);
        // scale anlm here
        multiply(rGradNNGradAnlm, aPostFuseScale, tSizeAnlm);
    }
    
    if (aPostFuseWeight==NULL) {
        tAnlm = tCnlm;
        rGradAnlm = rGradCnlm;
        rGradNNGradAnlm = rGradNNGradCnlm;
    }
    // grad grad anlm to grad grad fp
    const jint tSizeL2 = aNoRadial?aLMax:(aLMax+1);
    const jint tSizeL3 = L3NCOLS[aL3Max];
    const jint tSizeNp = aPostFuseWeight==NULL ? tSizeN : aPostFuseSize;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradNNGradL2_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad_+tShiftFp, aLMax, aNoRadial, aSphScale);
        calGradNNGradL3_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad_+tShiftFp+tSizeL2, aL3Max);
        calGradNNGradL4_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad_+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    if (aPolyScale) {
        if (aFixBasis || (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE && aPostFuseWeight==NULL)) {
            polyScaleGradGrad_<JNI_FALSE>(tFp, rGradFp, aNNGrad, rGradNNGrad_, rGradNNGrad, tSizeFp);
        } else {
            polyScaleGradGrad_<JNI_TRUE>(tFp, rGradFp, aNNGrad, rGradNNGrad_, rGradNNGrad, tSizeFp);
        }
    } else {
        for (jint i = 0; i < tSizeFp; ++i) {
            rGradNNGrad[i] += rGradNNGrad_[i];
        }
    }
    if (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE && aPostFuseWeight==NULL) return;
    if (!aFixBasis) for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradCnlmL2_(rGradAnlm+tShift, rGradNNGradAnlm+tShift, tNNGrad+tShiftFp, aLMax, aNoRadial, aSphScale);
        calGradCnlmL3_(tAnlm+tShift, rGradAnlm+tShift, rGradNNGradAnlm+tShift, tNNGrad+tShiftFp+tSizeL2, aL3Max);
        calGradCnlmL4_(tAnlm+tShift, rGradAnlm+tShift, rGradNNGradAnlm+tShift, tNNGrad+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
}


static void calSystemScale(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                           jdouble *rSystemScale, jdouble *rForwardCache,
                           jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMaxMax, jint aWType, jint aFuseSize,
                           jdouble *aRFuncScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calSystemScale<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calSystemScale<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calSystemScale<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calSystemScale<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_FULL: {
            calSystemScale<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_NONE: {
            calSystemScale<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calSystemScale<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_FUSE: {
            calSystemScale<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calSystemScale<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aLMaxMax, aFuseSize, aRFuncScale);
            return;
        }
        default: {
            return;
        }}
    }
}
template <jint WTYPE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                  jint aL3Max, jint aL4Max, jint aFuseStyle,
                  jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                  jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calFp<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
    } else {
        calFp<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                  jint aL3Max, jint aL4Max, jint aWType, jint aFuseStyle,
                  jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                  jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calFp<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calFp<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    }
}
template <jint WTYPE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                        jint aL3Max, jint aL4Max, jint aFuseStyle,
                        jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                        jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calBackward<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
    } else {
        calBackward<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
    }
}
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                        jint aL3Max, jint aL4Max, jint aWType, jint aFuseStyle,
                        jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                        jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
        if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackward<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackward<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FULL: {
            calBackward<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE: {
            calBackward<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FUSE: {
            calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackward<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    }
}
template <jint WTYPE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                     jint aL3Max, jint aL4Max, jint aFuseStyle,
                     jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                     jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calForce<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
    } else {
        calForce<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                     jint aL3Max, jint aL4Max, jint aWType, jint aFuseStyle,
                     jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                     jdouble *aRFuncScale, jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    }
}
template <jint WTYPE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                             jint aL3Max, jint aL4Max, jint aFuseStyle,
                             jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                             jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calBackwardForce<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
    } else {
        calBackwardForce<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
    }
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jboolean aNoRadial,
                             jint aL3Max, jint aL4Max, jint aWType, jint aFuseStyle,
                             jdouble *aFuseWeight, jint aFuseSize, jdouble *aPostFuseWeight, jint aPostFuseSize, jdouble aPostFuseScale,
                             jdouble *aSystemScale, jboolean aPolyScale, jboolean aSphScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackwardForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackwardForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aNoRadial, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aPostFuseWeight, aPostFuseSize, aPostFuseScale, aSystemScale, aPolyScale, aSphScale);
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H