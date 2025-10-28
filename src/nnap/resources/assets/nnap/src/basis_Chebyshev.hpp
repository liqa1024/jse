#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeFp = (aTypeNum+1)*(aNMax+1);  break;}
    case WTYPE_FULL:    {tSizeFp = aTypeNum*(aNMax+1);      break;}
    case WTYPE_NONE:    {tSizeFp = aNMax+1;                 break;}
    case WTYPE_DEFAULT: {tSizeFp = (aNMax+aNMax+2);         break;}
    case WTYPE_FUSE:    {tSizeFp = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeFp = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeFp = 0;                       break;}
    }
    // init cache
    jdouble *rRn = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNN*(aNMax+1);
    } else {
        rRn = rForwardCache;
    }
    // clear fp first
    for (jint i = 0; i < tSizeFp; ++i) {
        rFp[i] = 0.0;
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
        // cal fp
        if (WTYPE==WTYPE_FUSE) {
            mplusFpFuse<FSTYLE>(rFp, aFuseWeight, type, fc, rRn, aFuseSize, aNMax);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusFpExFuse<FSTYLE>(rFp, aFuseWeight, type, fc, rRn, aFuseSize, aNMax);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplus(rFp, fc, rRn, aNMax+1);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tFp = rFp + (aNMax+1)*(type-1);
            mplus(tFp, fc, rRn, aNMax+1);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tFpWt = rFp + (aNMax+1)*type;
            mplus2(rFp, tFpWt, fc, fc, rRn, aNMax+1);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tFpWt = rFp + (aNMax+1);
            mplus2(rFp, tFpWt, fc, fc*wt, rRn, aNMax+1);
        }
    }
}

template <jint WTYPE, jint FSTYLE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize) noexcept {
    static_assert(WTYPE!=WTYPE_DEFAULT && WTYPE!=WTYPE_NONE && WTYPE!=WTYPE_FULL && WTYPE!=WTYPE_EXFULL, "WTYPE INVALID");
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(aNMax+1);
        // plus to para
        if (WTYPE==WTYPE_FUSE) {
            mplusGradParaFuse<FSTYLE>(aGradFp, rGradPara, type, fc, tRn, aFuseSize, aNMax);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            jdouble *tGradFpWt = aGradFp + (aNMax+1);
            mplusGradParaFuse<FSTYLE>(tGradFpWt, rGradPara, type, fc, tRn, aFuseSize, aNMax);
        }
    }
}

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rForwardForceCache;
        rNlRnPy = rNlRnPx + aNN*(aNMax+1);
        rNlRnPz = rNlRnPy + aNN*(aNMax+1);
        rNlFcPx = rNlRnPz + aNN*(aNMax+1);
        rNlFcPy = rNlFcPx + aNN;
        rNlFcPz = rNlFcPy + aNN;
        rCheby2 = rNlFcPz + aNN;
    } else {
        rRnPx = rForwardForceCache;
        rRnPy = rRnPx + (aNMax+1);
        rRnPz = rRnPy + (aNMax+1);
        rCheby2 = rRnPz + (aNMax+1);
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(aNMax+1);
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
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tGradRn = rCheby2;
            calGradRnFuse<FSTYLE>(tGradRn, aNNGrad, aFuseWeight, type, aFuseSize, aNMax);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            jdouble *tGradRn = rCheby2;
            calGradRnExFuse<FSTYLE>(tGradRn, aNNGrad, aFuseWeight, type, aFuseSize, aNMax);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2Fxyz(j, aNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradRn = aNNGrad + (aNMax+1)*(type-1);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tNNGradWt = aNNGrad + (aNMax+1)*type;
            gradRnWt2Fxyz(j, aNNGrad, tNNGradWt, fc, tRn, 1.0, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tNNGradWt = aNNGrad + (aNMax+1);
            gradRnWt2Fxyz(j, aNNGrad, tNNGradWt, fc, tRn, wt, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        }
    }
}

template <jint WTYPE, jint FSTYLE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNlRnPx = aForwardForceCache;
    jdouble *tNlRnPy = tNlRnPx + aNN*(aNMax+1);
    jdouble *tNlRnPz = tNlRnPy + aNN*(aNMax+1);
    jdouble *tNlFcPx = tNlRnPz + aNN*(aNMax+1);
    jdouble *tNlFcPy = tNlFcPx + aNN;
    jdouble *tNlFcPz = tNlFcPy + aNN;
    jdouble *rGradNNGradRn = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradNNGradRn = rBackwardForceCache;
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
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(aNMax+1);
        // get fcPxyz RnPxyz
        jdouble fcPx = tNlFcPx[j], fcPy = tNlFcPy[j], fcPz = tNlFcPz[j];
        jdouble *tRnPx = tNlRnPx + j*(aNMax+1);
        jdouble *tRnPy = tNlRnPy + j*(aNMax+1);
        jdouble *tRnPz = tNlRnPz + j*(aNMax+1);
        // get gradNNgradRn
        if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            calGradNNGradRn(rGradNNGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
        // mplus to gradNNgrad
        if (WTYPE==WTYPE_FUSE) {
            mplusGradNNGradFuse<FSTYLE>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusGradNNGradExFuse<FSTYLE>(rGradNNGrad, rGradNNGradRn, aNNGrad, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusGradNNGrad(rGradNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradNNGrad = rGradNNGrad + (aNMax+1)*(type-1);
            mplusGradNNGrad(tGradNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tGradNNGradWt = rGradNNGrad + (aNMax+1)*type;
            mplusGradNNGradWt(rGradNNGrad, tGradNNGradWt, fc, tRn, 1.0, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tGradNNGradWt = rGradNNGrad + (aNMax+1);
            mplusGradNNGradWt(rGradNNGrad, tGradNNGradWt, fc, tRn, wt, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
    }
}


template <jint WTYPE, jint FSTYLE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calFp<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    } else {
        calFp<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        default: {
            return;
        }}
    }
}


static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jint aFuseSize) noexcept {
    if (aWType==WTYPE_FUSE) {
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            calBackward<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
        } else {
            calBackward<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
        }
    } else
    if (aWType==WTYPE_EXFUSE) {
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            calBackward<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
        } else {
            calBackward<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
        }
    }
}


template <jint WTYPE, jint FSTYLE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calForce<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    } else {
        calForce<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
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
                             jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            } else {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            }
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_CHEBYSHEV_H