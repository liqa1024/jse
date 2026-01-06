#ifndef BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H
#define BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"
#include "basis_MultiUtil.hpp"

namespace JSE_NNAP {

template <jboolean FULL_CACHE>
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache,
                    jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                    jint aNMax, jint aLMax, jdouble *aRFuncScale) noexcept {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeRFunc = (aNMax+1)*aRCutsSize;
    // init cache
    jdouble *rRkn = NULL, *rFc = NULL, *rY = NULL;
    jdouble *rNlRkn = NULL, *rNlFc = NULL, *rNlY = NULL;
    if (FULL_CACHE) {
        rNlRkn = rForwardCache;
        rNlFc = rNlRkn + aNN*tSizeRFunc;
        rNlY = rNlFc + aNN*aRCutsSize;
    } else {
        rRkn = rForwardCache;
        rFc = rRkn + tSizeRFunc;
        rY = rFc + aRCutsSize;
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCutMax) continue;
        // cal Rn, fc
        if (FULL_CACHE) rRkn = rNlRkn + j*tSizeRFunc;
        if (FULL_CACHE) rFc = rNlFc + j*aRCutsSize;
        jdouble *rRn = rRkn;
        for (jint k = 0; k < aRCutsSize; ++k, rRn += (aNMax+1)) {
            const jdouble tRCutL = aRCutsL[k];
            const jdouble tRCutR = aRCutsR[k];
            if (dis<=tRCutL || dis>=tRCutR) continue;
            calRn(rRn, aNMax, dis, tRCutL, tRCutR);
            const jdouble fc = calFc(dis, tRCutL, tRCutR);
            rFc[k] = fc;
        }
        // scale to Rkn, more simple
        multiply(rRkn, aRFuncScale, tSizeRFunc);
        // cal Y
        if (FULL_CACHE) rY = rNlY + j*tLMAll;
        realSphericalHarmonicsFull4(aLMax, dx, dy, dz, dis, rY);
        // cal cnlm
        mplusCnlmMulti(rCnlm, rY, rFc, rRkn, dis, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax);
    }
}
static void calCnlm(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                    jdouble *rCnlm, jdouble *rForwardCache, jboolean aFullCache,
                    jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                    jint aNMax, jint aLMax, jdouble *aRFuncScale) noexcept {
    if (aFullCache) {
        calCnlm<JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    } else {
        calCnlm<JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                  jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                  jdouble *aRFuncScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1+ L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tEquFuseInDim = aRCutsSize*(aNMax+1);
    const jint tSizeCnlm = tEquFuseInDim*tLMAll;
    // init cache
    jdouble *rCnlm = rForwardCache;
    jdouble *rForwardCacheElse = rCnlm + tSizeCnlm;
    // clear cnlm, anlm first
    fill(rCnlm, 0.0, tSizeCnlm);
    // do cal
    calCnlm(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    // cal L2 L3 L4
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<tEquFuseInDim; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calL2_(rCnlm+tShift, rFp+tShiftFp, aLMax, JNI_FALSE);
        calL3_(rCnlm+tShift, rFp+tShiftFp+tSizeL2, aL3Max);
        calL4_(rCnlm+tShift, rFp+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
}

static void calBackwardMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                jdouble *rGradPara, jdouble *aGradCnlm, jdouble *aForwardCache, jdouble *rBackwardCache,
                                jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                                jint aNMax, jint aLMax, jdouble *aRFuncScale) {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeRFunc = (aNMax+1)*aRCutsSize;
    // init cache
    jdouble *tNlRkn = aForwardCache;
    jdouble *tNlFc = tNlRkn + aNN*tSizeRFunc;
    jdouble *tNlY = tNlFc + aNN*aRCutsSize;
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCutMax) continue;
        // get fc Rn Y
        jdouble *tFc = tNlFc + j*aRCutsSize;
        jdouble *tRkn = tNlRkn + j*tSizeRFunc;
        jdouble *tY = tNlY + j*tLMAll;
        // mplusGradParaFuse(aGradCnlm, tBnlm, rGradPara, type, aFuseSize, aNMax, aLMaxMax);
    }
}
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                        jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                        jdouble *aRFuncScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1 + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tEquFuseInDim = aRCutsSize*(aNMax+1);
    const jint tSizeCnlm = tEquFuseInDim*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm;
    jdouble *rGradCnlm = rBackwardCache;
    jdouble *rBackwardCacheElse = rGradCnlm + tSizeCnlm;
    // cal grad cnlm
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<tEquFuseInDim; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp, aLMax, JNI_FALSE);
        calGradL3_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tSizeL2, aL3Max);
        calGradL4_(tCnlm+tShift, rGradCnlm+tShift, aGradFp+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    // plus to para
    calBackwardMainLoop(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
}

template <jboolean FULL_CACHE>
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache,
                             jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                             jint aNMax, jint aLMax, jdouble *aRFuncScale) {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeRFunc = (aNMax+1)*aRCutsSize;
    // init cache
    jdouble *tNlRkn = aForwardCache;
    jdouble *tNlFc = tNlRkn + aNN*tSizeRFunc;
    jdouble *tNlY = tNlFc + aNN*aRCutsSize;
    jdouble *rRknPx = NULL, *rRknPy = NULL, *rRknPz = NULL, *rCheby2 = NULL;
    jdouble *rFcPx = NULL, *rFcPy = NULL, *rFcPz = NULL;
    jdouble *rYPx = NULL, *rYPy = NULL, *rYPz = NULL, *rYPtheta = NULL, *rYPphi = NULL;
    jdouble *rGradRkn = NULL, *rGradFc = NULL, *rGradY = NULL;
    jdouble *rNlRknPx = NULL, *rNlRknPy = NULL, *rNlRknPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    jdouble *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    if (FULL_CACHE) {
        rNlRknPx = rForwardForceCache;
        rNlRknPy = rNlRknPx + aNN*tSizeRFunc;
        rNlRknPz = rNlRknPy + aNN*tSizeRFunc;
        rNlFcPx = rNlRknPz + aNN*tSizeRFunc;
        rNlFcPy = rNlFcPx + aNN*aRCutsSize;
        rNlFcPz = rNlFcPy + aNN*aRCutsSize;
        rNlYPx = rNlFcPz + aNN*aRCutsSize;
        rNlYPy = rNlYPx + aNN*tLMAll;
        rNlYPz = rNlYPy + aNN*tLMAll;
        rYPtheta = rNlYPz + aNN*tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
        rGradRkn = rCheby2 + (aNMax+1);
        rGradFc = rGradRkn + tSizeRFunc;
        rGradY = rGradFc + aRCutsSize;
    } else {
        rRknPx = rForwardForceCache;
        rRknPy = rRknPx + tSizeRFunc;
        rRknPz = rRknPy + tSizeRFunc;
        rFcPx = rRknPz + tSizeRFunc;
        rFcPy = rFcPx + aRCutsSize;
        rFcPz = rFcPy + aRCutsSize;
        rYPx = rFcPz + aRCutsSize;
        rYPy = rYPx + tLMAll;
        rYPz = rYPy + tLMAll;
        rYPtheta = rYPz + tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
        rGradRkn = rCheby2 + (aNMax+1);
        rGradFc = rGradRkn + tSizeRFunc;
        rGradY = rGradFc + aRCutsSize;
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCutMax) continue;
        // get fc Rn Y
        jdouble *tFc = tNlFc + j*aRCutsSize;
        jdouble *tRkn = tNlRkn + j*tSizeRFunc;
        jdouble *tY = tNlY + j*tLMAll;
        // gradcnlm to gradY, gradR, gradfc here
        fill(rGradY, 0.0, tLMAll);
        fill(rGradRkn, 0.0, tSizeRFunc);
        fill(rGradFc, 0.0, aRCutsSize);
        gradCnlm2GradYRFcMulti(aGradCnlm, rGradY, rGradRkn, rGradFc, tY, tFc, tRkn, dis, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax);
        // scale to grad Rkn here
        multiply(rGradRkn, aRFuncScale, tSizeRFunc);
        jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
        // RnPxyz to Fxyz
        if (FULL_CACHE) {
            rRknPx = rNlRknPx + j*tSizeRFunc;
            rRknPy = rNlRknPy + j*tSizeRFunc;
            rRknPz = rNlRknPz + j*tSizeRFunc;
        }
        jdouble *rRnPx = rRknPx, *rRnPy = rRknPy, *rRnPz = rRknPz;
        jdouble *rGradRn = rGradRkn;
        for (jint k = 0; k < aRCutsSize; ++k, rRnPx += (aNMax+1), rRnPy += (aNMax+1), rRnPz += (aNMax+1), rGradRn += (aNMax+1)) {
            const jdouble tRCutL = aRCutsL[k];
            const jdouble tRCutR = aRCutsR[k];
            if (dis<=tRCutL || dis>=tRCutR) continue;
            calRnPxyz(rRnPx, rRnPy, rRnPz, rCheby2, aNMax, dis, tRCutL, tRCutR, dx, dy, dz);
            gradRn2Fxyz(rGradRn, aNMax, rRnPx, rRnPy, rRnPz, rFxj, rFyj, rFzj);
        }
        // YPxyz to Fxyz
        if (FULL_CACHE) {
            rYPx = rNlYPx + j*tLMAll;
            rYPy = rNlYPy + j*tLMAll;
            rYPz = rNlYPz + j*tLMAll;
        }
        calYPxyz(aLMax, tY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        gradY2Fxyz(rGradY, aLMax, rYPx, rYPy, rYPz, rFxj, rFyj, rFzj);
        // fcPxyz to Fxyz
        if (FULL_CACHE) {
            rFcPx = rNlFcPx + j*aRCutsSize;
            rFcPy = rNlFcPy + j*aRCutsSize;
            rFcPz = rNlFcPz + j*aRCutsSize;
        }
        for (jint k = 0; k < aRCutsSize; ++k) {
            const jdouble tRCutL = aRCutsL[k];
            const jdouble tRCutR = aRCutsR[k];
            if (dis<=tRCutL || dis>=tRCutR) continue;
            // cal fcPxyz here
            jdouble fcPx, fcPy, fcPz;
            calFcPxyz(&fcPx, &fcPy, &fcPz, dis, tRCutL, tRCutR, dx, dy, dz);
            if (FULL_CACHE) {
                rFcPx[k] = fcPx;
                rFcPy[k] = fcPy;
                rFcPz[k] = fcPz;
            }
            const jdouble tSubGradFc = rGradFc[k];
            rFxj += tSubGradFc*fcPx;
            rFyj += tSubGradFc*fcPy;
            rFzj += tSubGradFc*fcPz;
        }
        rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
    }
}
static void calForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aGradCnlm, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                             jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                             jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                             jint aNMax, jint aLMax, jdouble *aRFuncScale) {
    if (aFullCache) {
        calForceMainLoop<JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    } else {
        calForceMainLoop<JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradCnlm, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                     jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                     jdouble *aRFuncScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1+ L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tEquFuseInDim = aRCutsSize*(aNMax+1);
    const jint tSizeCnlm = tEquFuseInDim*tLMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tForwardCacheElse = tCnlm + tSizeCnlm;
    jdouble *rGradCnlm = rForwardForceCache;
    jdouble *rForwardForceCacheElse = rGradCnlm + tSizeCnlm;
    // forward need init gradAnlm gradCnlm here
    fill(rGradCnlm, 0.0, tSizeCnlm);
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<tEquFuseInDim; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradL2_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradL3_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp+tSizeL2, aL3Max);
        calGradL4_(tCnlm+tShift, rGradCnlm+tShift, aNNGrad+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    calForceMainLoop(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradCnlm, rFx, rFy, rFz, tForwardCacheElse, rForwardForceCacheElse, aFullCache, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
}

static void calBackwardForceMainLoop(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                     jdouble *rGradNNGradCnlm, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                                     jdouble *aNNGradCnlm, jdouble *rGradPara,
                                     jdouble *aForwardCache, jdouble *aForwardForceCache,
                                     jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                                     jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                                     jint aNMax, jint aLMax, jdouble *aRFuncScale) noexcept {
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tSizeRFunc = (aNMax+1)*aRCutsSize;
    // init cache
    jdouble *tNlRkn = aForwardCache;
    jdouble *tNlFc = tNlRkn + aNN*tSizeRFunc;
    jdouble *tNlY = tNlFc + aNN*aRCutsSize;
    jdouble *tNlRknPx = aForwardForceCache;
    jdouble *tNlRknPy = tNlRknPx + aNN*tSizeRFunc;
    jdouble *tNlRknPz = tNlRknPy + aNN*tSizeRFunc;
    jdouble *tNlFcPx = tNlRknPz + aNN*tSizeRFunc;
    jdouble *tNlFcPy = tNlFcPx + aNN*aRCutsSize;
    jdouble *tNlFcPz = tNlFcPy + aNN*aRCutsSize;
    jdouble *tNlYPx = tNlFcPz + aNN*aRCutsSize;
    jdouble *tNlYPy = tNlYPx + aNN*tLMAll;
    jdouble *tNlYPz = tNlYPy + aNN*tLMAll;
    jdouble *rGradNNGradRkn = rBackwardForceCache;
    jdouble *rGradNNGradFc = rGradNNGradRkn + tSizeRFunc;
    jdouble *rGradNNGradY = rGradNNGradFc + aRCutsSize;
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCutMax) continue;
        // clear gradNNGradY, gradNNGradR, gradNNGradfc here
        fill(rGradNNGradY, 0.0, tLMAll);
        fill(rGradNNGradRkn, 0.0, tSizeRFunc);
        fill(rGradNNGradFc, 0.0, aRCutsSize);
        // get gradFxyz
        jdouble tGradFx = aGradFx[j], tGradFy = aGradFy[j], tGradFz = aGradFz[j];
        // gradFxyz to gradNNGradRn
        jdouble *tRknPx = tNlRknPx + j*tSizeRFunc;
        jdouble *tRknPy = tNlRknPy + j*tSizeRFunc;
        jdouble *tRknPz = tNlRknPz + j*tSizeRFunc;
        jdouble *tRnPx = tRknPx, *tRnPy = tRknPy, *tRnPz = tRknPz;
        jdouble *rGradNNGradRn = rGradNNGradRkn;
        for (jint k = 0; k < aRCutsSize; ++k, tRnPx += (aNMax+1), tRnPy += (aNMax+1), tRnPz += (aNMax+1), rGradNNGradRn += (aNMax+1)) {
            const jdouble tRCutL = aRCutsL[k];
            const jdouble tRCutR = aRCutsR[k];
            if (dis<=tRCutL || dis>=tRCutR) continue;
            gradFxyz2GradNNGradRn(rGradNNGradRn, aNMax, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
        // scale to grad grad Rkn here
        multiply(rGradNNGradRkn, aRFuncScale, tSizeRFunc);
        // gradFxyz to gradNNGradY
        jdouble *tYPx = tNlYPx + j*tLMAll;
        jdouble *tYPy = tNlYPy + j*tLMAll;
        jdouble *tYPz = tNlYPz + j*tLMAll;
        gradFxyz2GradNNGradY(rGradNNGradY, aLMax, tYPx, tYPy, tYPz, tGradFx, tGradFy, tGradFz);
        // gradFxyz to gradNNGradFc
        jdouble *tFcPx = tNlFcPx + j*aRCutsSize;
        jdouble *tFcPy = tNlFcPy + j*aRCutsSize;
        jdouble *tFcPz = tNlFcPz + j*aRCutsSize;
        for (jint k = 0; k < aRCutsSize; ++k) {
            const jdouble tRCutL = aRCutsL[k];
            const jdouble tRCutR = aRCutsR[k];
            if (dis<=tRCutL || dis>=tRCutR) continue;
            rGradNNGradFc[k] += tFcPx[k]*tGradFx + tFcPy[k]*tGradFy + tFcPz[k]*tGradFz;
        }
        // get fc Rn Y
        jdouble *tFc = tNlFc + j*aRCutsSize;
        jdouble *tRkn = tNlRkn + j*tSizeRFunc;
        jdouble *tY = tNlY + j*tLMAll;
        // mplus to gradNNgrad
        mplusGradNNGradCnlmMulti(rGradNNGradCnlm, rGradNNGradY, rGradNNGradRkn, rGradNNGradFc, tY, tFc, tRkn, dis, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax);
    }
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCutMax, jdouble *aRCutsL, jdouble *aRCutsR, jint aRCutsSize,
                             jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
                             jdouble *aRFuncScale) noexcept {
    // const init
    const jint tSizeL = aLMax+1+ L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMAll = (aLMax+1)*(aLMax+1);
    const jint tEquFuseInDim = aRCutsSize*(aNMax+1);
    const jint tSizeCnlm = tEquFuseInDim*tLMAll;
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
    calBackwardForceMainLoop(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradNNGradCnlm, aGradFx, aGradFy, aGradFz, tNNGradCnlm, rGradPara, tForwardCacheElse, tForwardForceCacheElse, rBackwardCacheElse, rBackwardForceCacheElse, aFixBasis, aRCutMax, aRCutsL, aRCutsR, aRCutsSize, aNMax, aLMax, aRFuncScale);
    // grad grad anlm to grad grad fp
    const jint tSizeL2 = aLMax+1;
    const jint tSizeL3 = L3NCOLS[aL3Max];
    for (jint np=0, tShift=0, tShiftFp=0; np<tEquFuseInDim; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradNNGradL2_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradNNGradL3_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tSizeL2, aL3Max);
        calGradNNGradL4_(tCnlm+tShift, rGradNNGradCnlm+tShift, rGradNNGrad+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
    if (!aFixBasis) for (jint np=0, tShift=0, tShiftFp=0; np<tEquFuseInDim; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradCnlmL2_(rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp, aLMax, JNI_FALSE);
        calGradCnlmL3_(tCnlm+tShift, rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp+tSizeL2, aL3Max);
        calGradCnlmL4_(tCnlm+tShift, rGradCnlm+tShift, rGradNNGradCnlm+tShift, aNNGrad+tShiftFp+tSizeL2+tSizeL3, aL4Max);
    }
}

}

#endif //BASIS_MULTI_LAYER_SPHERICAL_CHEBYSHEV_H