#ifndef BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H
#define BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H

#include "basis_EquivariantUtil.hpp"
#include "basis_SphericalChebyshev.hpp"

namespace JSE_NNAP {

template <jint WTYPE, jint FSTYLE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                  jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
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
    const jint tSizeL = (aLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aEquSize[0]*tLMAll;
    // init cache
    jint tCacheSize = tSizeAnlm;
    for (jint k = 1; k < aEquNumber; ++k) {
        tCacheSize += aEquSize[k-1]*tLMAll;
        tCacheSize += aEquSize[k]*tLMAll;
    }
    jdouble *rCnlm = rForwardCache;
    jdouble *rAnlm = rCnlm + tSizeCnlm;
    jdouble *rForwardCacheElse = rAnlm + tCacheSize;
    // clear cnlm first
    fill(rCnlm, 0.0, tSizeCnlm);
    // do cal
    calCnlm<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCut, aNMax, aLMax, aFuseWeight, aFuseSize);
    // clear anlm first
    fill(rAnlm, 0.0, tSizeAnlm);
    // cnlm -> anlm
    mplusAnlm<FSTYLE>(rAnlm, rCnlm, aEquWeight, aEquSize[0], tSizeN, aLMax);
    // scale anlm here
    multiply(rAnlm, aEquScale[0], tSizeAnlm);
    // equ layers
    jdouble *tEquWeight = aEquWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tEquWeight += aEquSize[0]*tSizeN;
    } else {
        tEquWeight += aEquSize[0]*tSizeN*(aLMax+1);
    }
    for (jint k = 1; k < aEquNumber; ++k) {
        // clear mnlm first
        const jint tSizeMnlm = aEquSize[k-1]*tLMAll;
        jdouble *rMnlm = rAnlm + tSizeMnlm; // tSizeMnlm == tSizeAnlm
        fill(rMnlm, 0.0, tSizeMnlm);
        // anlm -> mnlm
        mplusMnlm(rMnlm, rAnlm, aEquSize[k-1], aLMax);
        // clear hnlm first
        jdouble *rHnlm = rMnlm + tSizeMnlm;
        fill(rHnlm, 0.0, aEquSize[k]*tLMAll);
        // mnlm -> hnlm
        mplusAnlm<FSTYLE>(rHnlm, rAnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusAnlm<FSTYLE>(rHnlm, rMnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        // scale hnlm here
        multiply(rHnlm, aEquScale[k], aEquSize[k]*tLMAll);
        // to next layer
        rAnlm = rHnlm;
    }
    // hnlm -> Pnl
    const jint tShiftL3 = (aLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize[aEquNumber-1];
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rAnlm+tShift, rFp+tShiftFp, aLMax, JNI_FALSE);
        calL3_(rAnlm+tShift, rFp+tShiftFp+tShiftL3, aL3Max);
        calL4_(rAnlm+tShift, rFp+tShiftFp+tShiftL4, aL4Max);
    }
}
template <jint WTYPE, jint FSTYLE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                        jint aFuseSize, jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
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
    const jint tSizeL = (aLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeHnlm = aEquSize[aEquNumber-1]*tLMAll;
    // init cache
    jint tCacheSize = aEquSize[0]*tLMAll;
    for (jint k = 1; k < aEquNumber; ++k) {
        tCacheSize += aEquSize[k-1]*tLMAll;
        tCacheSize += aEquSize[k]*tLMAll;
    }
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm + tCacheSize;
    jdouble *tHnlm = tForwardCacheElse - tSizeHnlm;
    jdouble *rGradCnlm = NULL;
    jdouble *rBackwardCacheElse = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradCnlm = rBackwardCache;
        rBackwardCacheElse = rGradCnlm + tSizeCnlm + tCacheSize;
    } else {
        rBackwardCacheElse = rBackwardCache + tCacheSize;
    }
    jdouble *rGradHnlm = rBackwardCacheElse - tSizeHnlm;
    // cal grad hnlm
    const jint tShiftL3 = (aLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize[aEquNumber-1];
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp, aLMax, JNI_FALSE);
        calGradL3_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp+tShiftL3, aL3Max);
        calGradL4_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp+tShiftL4, aL4Max);
    }
    jdouble *tGradParaEqu = rGradPara;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradParaEqu += aTypeNum*aFuseSize;
        } else {
            tGradParaEqu += aTypeNum*(aNMax+1)*(aLMax+1)*aFuseSize;
        }
    }
    jdouble *tEquWeight = aEquWeight;
    jdouble *tGradParaEqu2 = tGradParaEqu;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tEquWeight += aEquSize[0]*tSizeN;
        tGradParaEqu2 += aEquSize[0]*tSizeN;
    } else {
        tEquWeight += aEquSize[0]*tSizeN*(aLMax+1);
        tGradParaEqu2 += aEquSize[0]*tSizeN*(aLMax+1);
    }
    for (jint k = 1; k < aEquNumber; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += 2*aEquSize[k]*aEquSize[k-1];
            tGradParaEqu2 += 2*aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += 2*aEquSize[k]*aEquSize[k-1]*(aLMax+1);
            tGradParaEqu2 += 2*aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
    }
    // backward layers
    for (jint k = aEquNumber-1; k > 0; --k) {
        // scale hnlm here
        multiply(rGradHnlm, aEquScale[k], aEquSize[k]*tLMAll);
        // hnlm -> mnlm
        const jint tSizeMnlm = aEquSize[k-1]*tLMAll;
        jdouble *tMnlm = tHnlm - tSizeMnlm;
        jdouble *rGradMnlm = rGradHnlm - tSizeMnlm;
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight -= aEquSize[k]*aEquSize[k-1];
            tGradParaEqu2 -= aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
            tGradParaEqu2 -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusGradParaPostFuse<FSTYLE>(rGradHnlm, tMnlm, tGradParaEqu2, aEquSize[k], aEquSize[k-1], aLMax);
        mplusGradAnlm<FSTYLE>(rGradHnlm, rGradMnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        jdouble *tAnlm = tMnlm - tSizeMnlm; // tSizeMnlm == tSizeAnlm
        jdouble *rGradAnlm = rGradMnlm - tSizeMnlm;
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight -= aEquSize[k]*aEquSize[k-1];
            tGradParaEqu2 -= aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
            tGradParaEqu2 -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusGradParaPostFuse<FSTYLE>(rGradHnlm, tAnlm, tGradParaEqu2, aEquSize[k], aEquSize[k-1], aLMax);
        mplusGradAnlm<FSTYLE>(rGradHnlm, rGradAnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        // mnlm -> anlm
        mplusGradMnlm(rGradMnlm, tAnlm, rGradAnlm, aEquSize[k-1], aLMax);
        // to last layer
        tHnlm = tAnlm;
        rGradHnlm = rGradAnlm;
    }
    // scale anlm here
    multiply(rGradHnlm, aEquScale[0], aEquSize[0]*tLMAll);
    // anlm -> cnlm
    mplusGradParaPostFuse<FSTYLE>(rGradHnlm, tCnlm, tGradParaEqu, aEquSize[0], tSizeN, aLMax);
    if (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE) return;
    mplusGradAnlm<FSTYLE>(rGradHnlm, rGradCnlm, aEquWeight, aEquSize[0], tSizeN, aLMax);
    // plus to para
    calBackwardMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCut, aNMax, aLMax, aFuseSize);
}
template <jint WTYPE, jint FSTYLE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                     jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
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
    const jint tSizeL = (aLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeHnlm = aEquSize[aEquNumber-1]*tLMAll;
    // init cache
    jint tCacheSize = aEquSize[0]*tLMAll;
    for (jint k = 1; k < aEquNumber; ++k) {
        tCacheSize += aEquSize[k-1]*tLMAll;
        tCacheSize += aEquSize[k]*tLMAll;
    }
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm + tCacheSize;
    jdouble *tHnlm = tForwardCacheElse - tSizeHnlm;
    jdouble *rGradCnlm = rForwardForceCache;
    jdouble *rForwardForceCacheElse = rGradCnlm + tSizeCnlm + tCacheSize;
    jdouble *rGradHnlm = rForwardForceCacheElse - tSizeHnlm;
    // forward need init grad here
    fill(rGradCnlm, 0.0, tSizeCnlm+tCacheSize);
    const jint tShiftL3 = (aLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize[aEquNumber-1];
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tHnlm+tShift, rGradHnlm+tShift, aNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradL3_(tHnlm+tShift, rGradHnlm+tShift, aNNGrad+tShiftFp+tShiftL3, aL3Max);
        calGradL4_(tHnlm+tShift, rGradHnlm+tShift, aNNGrad+tShiftFp+tShiftL4, aL4Max);
    }
    jdouble *tEquWeight = aEquWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tEquWeight += aEquSize[0]*tSizeN;
    } else {
        tEquWeight += aEquSize[0]*tSizeN*(aLMax+1);
    }
    for (jint k = 1; k < aEquNumber; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += 2*aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += 2*aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
    }
    // backward layers
    for (jint k = aEquNumber-1; k > 0; --k) {
        // scale hnlm here
        multiply(rGradHnlm, aEquScale[k], aEquSize[k]*tLMAll);
        // hnlm -> mnlm
        const jint tSizeMnlm = aEquSize[k-1]*tLMAll;
        jdouble *tMnlm = tHnlm - tSizeMnlm;
        jdouble *rGradMnlm = rGradHnlm - tSizeMnlm;
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight -= aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusGradAnlm<FSTYLE>(rGradHnlm, rGradMnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        jdouble *tAnlm = tMnlm - tSizeMnlm; // tSizeMnlm == tSizeAnlm
        jdouble *rGradAnlm = rGradMnlm - tSizeMnlm;
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight -= aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight -= aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusGradAnlm<FSTYLE>(rGradHnlm, rGradAnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        // mnlm -> anlm
        mplusGradMnlm(rGradMnlm, tAnlm, rGradAnlm, aEquSize[k-1], aLMax);
        // to last layer
        tHnlm = tAnlm;
        rGradHnlm = rGradAnlm;
    }
    // scale anlm here
    multiply(rGradHnlm, aEquScale[0], aEquSize[0]*tLMAll);
    // anlm -> cnlm
    mplusGradAnlm<FSTYLE>(rGradHnlm, rGradCnlm, aEquWeight, aEquSize[0], tSizeN, aLMax);
    calForceMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradCnlm, rFx, rFy, rFz, tForwardCacheElse, rForwardForceCacheElse, aFullCache, aRCut, aNMax, aLMax, aFuseWeight, aFuseSize);
}
template <jint WTYPE, jint FSTYLE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                             jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
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
    const jint tSizeL = (aLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aEquSize[0]*tLMAll;
    // init cache
    jint tCacheSize = tSizeAnlm;
    for (jint k = 1; k < aEquNumber; ++k) {
        tCacheSize += aEquSize[k-1]*tLMAll;
        tCacheSize += aEquSize[k]*tLMAll;
    }
    jdouble *tCnlm = aForwardCache;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tForwardCacheElse = tAnlm + tCacheSize;
    jdouble *tNNGradCnlm = aForwardForceCache;
    jdouble *tNNGradAnlm = tNNGradCnlm + tSizeCnlm;
    jdouble *tForwardForceCacheElse = tNNGradAnlm + tCacheSize;
    jdouble *rGradAnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradAnlm = rBackwardCache + tSizeCnlm;
    } else {
        rGradAnlm = rBackwardCache;
    }
    jdouble *rBackwardCacheElse = rGradAnlm + tCacheSize;
    jdouble *rGradNNGradCnlm = rBackwardForceCache;
    jdouble *rGradNNGradAnlm = rGradNNGradCnlm + tSizeCnlm;
    jdouble *rBackwardForceCacheElse = rGradNNGradAnlm + tCacheSize;
    
    // cal rGradNNGradCnlm
    calBackwardForceMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, tNNGradCnlm, rGradPara, tForwardCacheElse, tForwardForceCacheElse, rBackwardCacheElse, rBackwardForceCacheElse, aFixBasis, aRCut, aNMax, aLMax, aFuseWeight, aFuseSize);
    
    jdouble *tGradParaEqu = rGradPara;
    if (!aFixBasis) if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradParaEqu += aTypeNum*aFuseSize;
        } else {
            tGradParaEqu += aTypeNum*(aNMax+1)*(aLMax+1)*aFuseSize;
        }
    }
    // cnlm -> anlm
    if (!aFixBasis) {
        mplusGradParaPostFuse<FSTYLE>(tNNGradAnlm, rGradNNGradCnlm, tGradParaEqu, aEquSize[0], tSizeN, aLMax);
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradParaEqu += aEquSize[0]*tSizeN;
        } else {
            tGradParaEqu += aEquSize[0]*tSizeN*(aLMax+1);
        }
    }
    mplusAnlm<FSTYLE>(rGradNNGradAnlm, rGradNNGradCnlm, aEquWeight, aEquSize[0], tSizeN, aLMax);
    // scale anlm here
    multiply(rGradNNGradAnlm, aEquScale[0], tSizeAnlm);
    // backward backward layers
    jdouble *tEquWeight = aEquWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tEquWeight += aEquSize[0]*tSizeN;
    } else {
        tEquWeight += aEquSize[0]*tSizeN*(aLMax+1);
    }
    for (jint k = 1; k < aEquNumber; ++k) {
        const jint tSizeMnlm = aEquSize[k-1]*tLMAll;
        jdouble *tMnlm = tAnlm + tSizeMnlm; // tSizeMnlm == tSizeAnlm
        jdouble *tNNGradMnlm = tNNGradAnlm + tSizeMnlm;
        jdouble *rGradMnlm = rGradAnlm + tSizeMnlm;
        jdouble *rGradNNGradMnlm = rGradNNGradAnlm + tSizeMnlm;
        // anlm -> mnlm
        if (!aFixBasis) {
            mplusGradMnlmAnlm(tNNGradMnlm, rGradAnlm, rGradNNGradAnlm, aEquSize[k-1], aLMax);
        }
        mplusGradNNGradMnlm(rGradNNGradMnlm, tAnlm, rGradNNGradAnlm, aEquSize[k-1], aLMax);
        jdouble *tHnlm = tMnlm + tSizeMnlm;
        jdouble *tNNGradHnlm = tNNGradMnlm + tSizeMnlm;
        jdouble *rGradHnlm = rGradMnlm + tSizeMnlm;
        jdouble *rGradNNGradHnlm = rGradNNGradMnlm + tSizeMnlm;
        // mnlm -> hnlm
        if (!aFixBasis) {
            mplusGradParaPostFuse<FSTYLE>(tNNGradHnlm, rGradNNGradAnlm, tGradParaEqu, aEquSize[k], aEquSize[k-1], aLMax);
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                tGradParaEqu += aEquSize[k]*aEquSize[k-1];
            } else {
                tGradParaEqu += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
            }
            mplusGradParaPostFuse<FSTYLE>(tNNGradHnlm, rGradNNGradMnlm, tGradParaEqu, aEquSize[k], aEquSize[k-1], aLMax);
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                tGradParaEqu += aEquSize[k]*aEquSize[k-1];
            } else {
                tGradParaEqu += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
            }
        }
        mplusAnlm<FSTYLE>(rGradNNGradHnlm, rGradNNGradAnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        mplusAnlm<FSTYLE>(rGradNNGradHnlm, rGradNNGradMnlm, tEquWeight, aEquSize[k], aEquSize[k-1], aLMax);
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tEquWeight += aEquSize[k]*aEquSize[k-1];
        } else {
            tEquWeight += aEquSize[k]*aEquSize[k-1]*(aLMax+1);
        }
        // scale hnlm here
        multiply(rGradNNGradHnlm, aEquScale[k], aEquSize[k]*tLMAll);
        // to next layer
        tAnlm = tHnlm;
        tNNGradAnlm = tNNGradHnlm;
        rGradAnlm = rGradHnlm;
        rGradNNGradAnlm = rGradNNGradHnlm;
    }
    // grad grad hnlm to grad grad fp
    const jint tShiftL3 = (aLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize[aEquNumber-1];
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradNNGradL2_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradNNGradL3_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad+tShiftFp+tShiftL3, aL3Max);
        calGradNNGradL4_(tAnlm+tShift, rGradNNGradAnlm+tShift, rGradNNGrad+tShiftFp+tShiftL4, aL4Max);
    }
    if (!aFixBasis) for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradCnlmL2_(rGradAnlm+tShift, rGradNNGradAnlm+tShift, aNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradCnlmL3_(tAnlm+tShift, rGradAnlm+tShift, rGradNNGradAnlm+tShift, aNNGrad+tShiftFp+tShiftL3, aL3Max);
        calGradCnlmL4_(tAnlm+tShift, rGradAnlm+tShift, rGradNNGradAnlm+tShift, aNNGrad+tShiftFp+tShiftL4, aL4Max);
    }
}


template <jint WTYPE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                  jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calFp<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    } else {
        calFp<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                  jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calFp<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calFp<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
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
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                        jint aFuseStyle, jint aFuseSize,
                        jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calBackward<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    } else {
        calBackward<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    }
}
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                        jint aWType, jint aFuseStyle, jint aFuseSize,
                        jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackward<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackward<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FULL: {
            calBackward<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE: {
            calBackward<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FUSE: {
            calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackward<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
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
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                     jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calForce<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    } else {
        calForce<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                     jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
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
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                             jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aFuseStyle==FUSE_STYLE_LIMITED) {
        calBackwardForce<WTYPE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    } else {
        calBackwardForce<WTYPE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
    }
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                             jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aEquWeight, jint *aEquSize, jdouble *aEquScale, jint aEquNumber) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackwardForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        case WTYPE_EXFUSE: {
            calBackwardForce<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aFuseStyle, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale, aEquNumber);
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H