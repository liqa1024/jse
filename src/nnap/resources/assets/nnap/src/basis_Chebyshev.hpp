#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <jint WTYPE>
static void calSystemScale(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                           jdouble *rSystemScale, jdouble *rForwardCache,
                           jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize,
                           jdouble *aRFuncScale) noexcept {
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
    jdouble *rRn = rForwardCache;
    // clear fp first
    fill(rSystemScale, 0.0, tSizeFp);
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
        // cal fp
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tSystemScaleWt = rSystemScale + (aNMax+1);
            mplus2(rSystemScale, tSystemScaleWt, fc, fc*wt, rRn, aNMax+1);
        } else {
            mplus(rSystemScale, fc, rRn, aNMax+1);
        }
    }
    // repeat for advanced wtype
    if (WTYPE==WTYPE_FUSE) {
        jdouble *tSystemScale = rSystemScale + (aNMax+1);
        for (jint k = 1; k < aFuseSize; ++k) {
            fill(tSystemScale, rSystemScale, aNMax+1);
            tSystemScale += (aNMax+1);
        }
    } else
    if (WTYPE==WTYPE_EXFUSE) {
        jdouble *tSystemScale = rSystemScale + (aNMax+1);
        for (jint k = 0; k < aFuseSize; ++k) {
            fill(tSystemScale, rSystemScale, aNMax+1);
            tSystemScale += (aNMax+1);
        }
    } else
    if (WTYPE==WTYPE_FULL) {
        jdouble *tSystemScale = rSystemScale + (aNMax+1);
        for (jint k = 1; k < aTypeNum; ++k) {
            fill(tSystemScale, rSystemScale, aNMax+1);
            tSystemScale += (aNMax+1);
        }
    } else
    if (WTYPE==WTYPE_EXFULL) {
        jdouble *tSystemScale = rSystemScale + (aNMax+1);
        for (jint k = 0; k < aTypeNum; ++k) {
            fill(tSystemScale, rSystemScale, aNMax+1);
            tSystemScale += (aNMax+1);
        }
    }
}

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
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
    fill(rFp, 0.0, tSizeFp);
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
    // system scale here
    multiply(rFp, aSystemScale, tSizeFp);
}

template <jint WTYPE, jint FSTYLE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize, jdouble *aSystemScale) noexcept {
    static_assert(WTYPE!=WTYPE_DEFAULT && WTYPE!=WTYPE_NONE && WTYPE!=WTYPE_FULL && WTYPE!=WTYPE_EXFULL, "WTYPE INVALID");
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case WTYPE_FUSE:    {tSizeFp = aFuseSize*(aNMax+1);     break;}
    case WTYPE_EXFUSE:  {tSizeFp = (aFuseSize+1)*(aNMax+1); break;}
    default:            {tSizeFp = 0;                       break;}
    }
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *rGradFp = rBackwardCache;
    // system scale here
    mplus(rGradFp, aGradFp, aSystemScale, tSizeFp);
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
            mplusGradParaFuse<FSTYLE>(rGradFp, rGradPara, type, fc, tRn, aFuseSize, aNMax);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            jdouble *tGradFpWt = rGradFp + (aNMax+1);
            mplusGradParaFuse<FSTYLE>(tGradFpWt, rGradPara, type, fc, tRn, aFuseSize, aNMax);
        }
    }
}

template <jint WTYPE, jint FSTYLE, jboolean FULL_CACHE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
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
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *rNNGrad = rForwardForceCache;
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rNNGrad + tSizeFp;
        rNlRnPy = rNlRnPx + aNN*(aNMax+1);
        rNlRnPz = rNlRnPy + aNN*(aNMax+1);
        rNlFcPx = rNlRnPz + aNN*(aNMax+1);
        rNlFcPy = rNlFcPx + aNN;
        rNlFcPz = rNlFcPy + aNN;
        rCheby2 = rNlFcPz + aNN;
    } else {
        rRnPx = rNNGrad + tSizeFp;
        rRnPy = rRnPx + (aNMax+1);
        rRnPz = rRnPy + (aNMax+1);
        rCheby2 = rRnPz + (aNMax+1);
    }
    // system scale here
    fill(rNNGrad, 0.0, tSizeFp);
    mplus(rNNGrad, aNNGrad, aSystemScale, tSizeFp);
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
        calRnPxyz(rRnPx, rRnPy, rRnPz, rCheby2, aNMax, dis, aRCut, dx, dy, dz, aRFuncScale);
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tGradRn = rCheby2;
            calGradRnFuse<FSTYLE>(tGradRn, rNNGrad, aFuseWeight, type, aFuseSize, aNMax);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            jdouble *tGradRn = rCheby2;
            calGradRnExFuse<FSTYLE>(tGradRn, rNNGrad, aFuseWeight, type, aFuseSize, aNMax);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2Fxyz(j, rNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradRn = rNNGrad + (aNMax+1)*(type-1);
            gradRn2Fxyz(j, tGradRn, fc, tRn, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tNNGradWt = rNNGrad + (aNMax+1)*type;
            gradRnWt2Fxyz(j, rNNGrad, tNNGradWt, fc, tRn, 1.0, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tNNGradWt = rNNGrad + (aNMax+1);
            gradRnWt2Fxyz(j, rNNGrad, tNNGradWt, fc, tRn, wt, aNMax, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        }
    }
}

template <jint WTYPE, jint FSTYLE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aSystemScale) noexcept {
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
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(aNMax+1);
    jdouble *tNNGrad = aForwardForceCache;
    jdouble *tNlRnPx = tNNGrad + tSizeFp;
    jdouble *tNlRnPy = tNlRnPx + aNN*(aNMax+1);
    jdouble *tNlRnPz = tNlRnPy + aNN*(aNMax+1);
    jdouble *tNlFcPx = tNlRnPz + aNN*(aNMax+1);
    jdouble *tNlFcPy = tNlFcPx + aNN;
    jdouble *tNlFcPz = tNlFcPy + aNN;
    jdouble *rSubGradNNGrad = rBackwardForceCache;
    jdouble *rGradNNGradRn = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradNNGradRn = rSubGradNNGrad + tSizeFp;
    }
    // cache only, so always clear
    fill(rSubGradNNGrad, 0.0, tSizeFp);
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
            mplusGradNNGradFuse<FSTYLE>(rSubGradNNGrad, rGradNNGradRn, tNNGrad, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusGradNNGradExFuse<FSTYLE>(rSubGradNNGrad, rGradNNGradRn, tNNGrad, aFuseWeight, rGradPara, type, aFuseSize, aNMax, aFixBasis);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusGradNNGrad(rSubGradNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradNNGrad = rSubGradNNGrad + (aNMax+1)*(type-1);
            mplusGradNNGrad(tGradNNGrad, fc, tRn, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tGradNNGradWt = rSubGradNNGrad + (aNMax+1)*type;
            mplusGradNNGradWt(rSubGradNNGrad, tGradNNGradWt, fc, tRn, 1.0, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tGradNNGradWt = rSubGradNNGrad + (aNMax+1);
            mplusGradNNGradWt(rSubGradNNGrad, tGradNNGradWt, fc, tRn, wt, aNMax, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
    }
    // system scale here
    mplus(rGradNNGrad, rSubGradNNGrad, aSystemScale, tSizeFp);
}



static void calSystemScale(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                           jdouble *rSystemScale, jdouble *rForwardCache,
                           jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseSize,
                           jdouble *aRFuncScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calSystemScale<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calSystemScale<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calSystemScale<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calSystemScale<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_EXFULL: {
            calSystemScale<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_FULL: {
            calSystemScale<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calSystemScale<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_FUSE: {
            calSystemScale<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        case WTYPE_EXFUSE: {
            calSystemScale<WTYPE_EXFUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rSystemScale, rForwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aRFuncScale);
            return;
        }
        default: {
            return;
        }}
    }
}


template <jint WTYPE, jint FSTYLE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
    if (aFullCache) {
        calFp<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
    } else {
        calFp<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
    }
}
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        default: {
            return;
        }}
    }
}


static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jint aFuseSize, jdouble *aSystemScale) noexcept {
    if (aWType==WTYPE_FUSE) {
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            calBackward<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aSystemScale);
        } else {
            calBackward<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aSystemScale);
        }
    } else
    if (aWType==WTYPE_EXFUSE) {
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            calBackward<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aSystemScale);
        } else {
            calBackward<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aFuseSize, aSystemScale);
        }
    }
}


template <jint WTYPE, jint FSTYLE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
    if (aFullCache) {
        calForce<WTYPE, FSTYLE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
    } else {
        calForce<WTYPE, FSTYLE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aRFuncScale, jdouble *aSystemScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
            } else {
                calForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aRFuncScale, aSystemScale);
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
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aSystemScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            } else {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            } else {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            } else {
                calBackwardForce<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize, aSystemScale);
            } else {
                calBackwardForce<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize,aSystemScale);
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