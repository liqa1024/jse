#ifndef BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H
#define BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"
#include "basis_MultiLayerUtil.hpp"

namespace JSE_NNAP {

template <jboolean FULL_CACHE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache,
                    jdouble aRCut, jint aNMax, jint aLMax,
                    jdouble *aRFuseWeight, jint aRFuseSize,
                    jdouble *aRFuncScale) noexcept {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    // init cache
    jdouble *rRn = NULL, *rY = NULL;
    jdouble *rRFn = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL, *rNlY = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNN*(aNMax+1);
        rNlY = rNlFc + aNN;
        rRFn = rNlY + aNN*tLMAll;
    } else {
        rRn = rForwardCache;
        rY = rRn + (aNMax+1);
        rRFn = rY + tLMAll;
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
        realSphericalHarmonicsFull4(aLMax, dx, dy, dz, dis, rY);
        // scale Y here
        multiply(rY, SQRT_PI4, tLMAll);
        // cal rfuse
        calRFuse(rRFn, rRn, aRFuseWeight, aRFuseSize, aNMax);
        // mplus2cnlm
        mplusCnlmRFuse(rCnlm, rY, fc, rRFn, aRFuseSize, aLMax);
    }
}
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jdouble aRCut, jint aNMax, jint aLMax,
                    jdouble *aRFuseWeight, jint aRFuseSize,
                    jdouble *aRFuncScale) noexcept {
    if (aFullCache) {
        calCnlm<JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMax, aRFuseWeight, aRFuseSize, aRFuncScale);
    } else {
        calCnlm<JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCut, aNMax, aLMax, aRFuseWeight, aRFuseSize, aRFuncScale);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                  jdouble *aRFuseWeight, jint aRFuseSize, jdouble *aEquFuseWeight, jint aEquFuseSize,
                  jdouble aEquFuseScale, jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1+ L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = aRFuseSize*tLMAll;
    const jint tSizeAnlm = aEquFuseSize*tLMAll;
    // init cache
    jdouble *rCnlm = rForwardCache;
    jdouble *rAnlm = rCnlm + tSizeCnlm;
    jdouble *rForwardCacheElse = rAnlm + tSizeAnlm;
    // clear cnlm, anlm first
    fill(rCnlm, 0.0, tSizeCnlm);
    fill(rAnlm, 0.0, tSizeAnlm);
    // do cal
    calCnlm(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCut, aNMax, aLMax, aRFuseWeight, aRFuseSize, aRFuncScale);
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<aRFuseSize; ++n, tShift+=tLMAll, tShiftS+=aLMax+1) {
        multiplyLM(rCnlm+tShift, aSystemScale+tShiftS, aLMax);
    }
    // cnlm -> anlm
    mplusAnlm<FUSE_STYLE_LIMITED>(rAnlm, rCnlm, aEquFuseWeight, aEquFuseSize, aRFuseSize, aLMax);
    // scale anlm here
    multiply(rAnlm, aEquFuseScale, tSizeAnlm);
    // cal L2 L3 L4
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<aEquFuseSize; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rAnlm+tShift, rFp+tShiftFp, aLMax, JNI_FALSE, JNI_TRUE);
        calL3_(rAnlm+tShift, rFp+tShiftFp+tSizeL2, aL3Max);
        calL4_(rAnlm+tShift, rFp+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
}

static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aGradCnlm, jdouble *aForwardCache, jdouble *rBackwardCache,
                                jdouble aRCut, jint aNMax, jint aLMax, jint aRFuseSize) {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlY = tNlFc + aNN;
    jdouble *rNlGradRFn = rBackwardCache;
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc
        jdouble fc = tNlFc[j];
        // get Rn
        jdouble *tRn = tNlRn + j*(aNMax+1);
        // get Y
        jdouble *tY = tNlY + j*tLMAll;
        // get grad rfuse
        jdouble *rGradRFn = rNlGradRFn + j*aRFuseSize;
        // cnlm -> rfuse
        mplusGradCnlmRFuse(aGradCnlm, tY, fc, rGradRFn, aRFuseSize, aLMax);
        // mplus to gradPara
        mplusGradParaRFuse(rGradRFn, tRn, rGradPara, aRFuseSize, aNMax);
    }
}
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                        jint aRFuseSize, jdouble *aEquFuseWeight, jint aEquFuseSize,
                        jdouble aEquFuseScale, jdouble *aSystemScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1 + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeCnlm = aRFuseSize*tLMAll;
    const jint tSizeAnlm = aEquFuseSize*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tForwardCacheElse = tAnlm + tSizeAnlm;
    jdouble *rGradCnlm = rBackwardCache;
    jdouble *rGradAnlm = rGradCnlm + tSizeCnlm;
    jdouble *rBackwardCacheElse = rGradAnlm + tSizeAnlm;
    // cal grad cnlm
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<aEquFuseSize; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp, aLMax, JNI_FALSE, JNI_TRUE);
        calGradL3_(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2, aL3Max);
        calGradL4_(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    // anlm stuffs
    jdouble *tGradPara = rGradPara + (aNMax+1)*aRFuseSize;
    // scale anlm here
    multiply(rGradAnlm, aEquFuseScale, tSizeAnlm);
    mplusGradParaPostFuse<FUSE_STYLE_LIMITED>(rGradAnlm, tCnlm, tGradPara, aEquFuseSize, aRFuseSize, aLMax);
    // anlm -> cnlm
    mplusGradAnlm<FUSE_STYLE_LIMITED>(rGradAnlm, rGradCnlm, aEquFuseWeight, aEquFuseSize, aRFuseSize, aLMax);
    // system scale here
    for (jint n=0, tShift=0, tShiftS=0; n<aRFuseSize; ++n, tShift+=tLMAll, tShiftS+=aLMax+1) {
        multiplyLM(rGradCnlm+tShift, aSystemScale+tShiftS, aLMax);
    }
    // plus to para
    calBackwardMainLoop(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCut, aNMax, aLMax, aRFuseSize);
}

}

#endif //BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H