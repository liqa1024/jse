#ifndef BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H
#define BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H

#include "basis_EquivariantUtil.hpp"
#include "basis_SphericalChebyshev.hpp"

namespace JSE_NNAP {

template <jint WTYPE, jint FSTYLE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                  jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
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
    const jint tSizeLL = (aLLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tLLMMAll = (aLLMax+1)*(aLLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aEquSize*tLMAll;
    const jint tSizeHnlm = aEquSize*2*tLLMMAll;
    // init cache
    jdouble *rCnlm = rForwardCache;
    jdouble *rAnlm = rCnlm + tSizeCnlm;
    jdouble *rHnlm = rAnlm + tSizeAnlm;
    jdouble *rForwardCacheElse = rHnlm + tSizeHnlm;
    // clear cnlm first
    for (jint i = 0; i < tSizeCnlm; ++i) {
        rCnlm[i] = 0.0;
    }
    // do cal
    calCnlm<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rCnlm, rForwardCacheElse, aFullCache, aRCut, aNMax, tLMaxMax, aFuseWeight, aFuseSize);
    // clear anlm first
    for (jint i = 0; i < tSizeAnlm; ++i) {
        rAnlm[i] = 0.0;
    }
    // cnlm -> anlm
    mplusAnlm<FSTYLE>(rAnlm, rCnlm, aEquWeight, aEquSize, tSizeN, tLMaxMax);
    // scale anlm here
    for (jint i = 0; i < tSizeAnlm; ++i) {
        rAnlm[i] *= aEquScale;
    }
    // clear hnlm first
    for (jint i = 0; i < tSizeHnlm; ++i) {
        rHnlm[i] = 0.0;
    }
    // anlm -> hnlm
    mplusHnlm(rHnlm, rAnlm, aEquSize, tLMaxMax, aLLMax);
    // hnlm -> Pnl
    const jint tShiftL3 = (aLLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize*2;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLLMMAll, tShiftFp+=tSizeLL) {
        calL2_(rHnlm+tShift, rFp+tShiftFp, aLLMax, JNI_FALSE);
        calL3_(rHnlm+tShift, rFp+tShiftFp+tShiftL3, aL3Max, JNI_TRUE);
        calL4_(rHnlm+tShift, rFp+tShiftFp+tShiftL4, aL4Max, JNI_TRUE);
    }
}
template <jint WTYPE, jint FSTYLE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache, jdouble *rBackwardCache,
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                        jint aFuseSize, jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
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
    const jint tSizeLL = (aLLMax+1) + L3NCOLS[aL3Max] + L4NCOLS[aL4Max];
    const jint tLMaxMax = aLMax>aL3Max ? (aLMax>aL4Max?aLMax:aL4Max) : (aL3Max>aL4Max?aL3Max:aL4Max);
    const jint tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    const jint tLLMMAll = (aLLMax+1)*(aLLMax+1);
    const jint tSizeCnlm = tSizeN*tLMAll;
    const jint tSizeAnlm = aEquSize*tLMAll;
    const jint tSizeHnlm = aEquSize*2*tLLMMAll;
    // init cache
    jdouble *tCnlm = aForwardCache;
    jdouble *tAnlm = tCnlm + tSizeCnlm;
    jdouble *tHnlm = tAnlm + tSizeAnlm;
    jdouble *tForwardCacheElse = tHnlm + tSizeHnlm;
    jdouble *rGradCnlm = NULL;
    jdouble *rGradAnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        rGradCnlm = rBackwardCache;
        rGradAnlm = rGradCnlm + tSizeCnlm;
    } else {
        rGradAnlm = rBackwardCache;
    }
    jdouble *rGradHnlm = rGradAnlm + tSizeAnlm;
    jdouble *rBackwardCacheElse = rGradHnlm + tSizeHnlm;
    // cal grad hnlm
    const jint tShiftL3 = (aLLMax+1);
    const jint tShiftL4 = tShiftL3 + L3NCOLS[aL3Max];
    const jint tSizeNp = aEquSize*2;
    for (jint np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLLMMAll, tShiftFp+=tSizeLL) {
        calGradL2_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp, aLLMax, JNI_FALSE);
        calGradL3_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp+tShiftL3, aL3Max, JNI_TRUE);
        calGradL4_(tHnlm+tShift, rGradHnlm+tShift, aGradFp+tShiftFp+tShiftL4, aL4Max, JNI_TRUE);
    }
    // hnlm -> anlm
    mplusGradHnlm(rGradHnlm, tAnlm, rGradAnlm, aEquSize, tLMaxMax, aLLMax);
    // anlm stuffs
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
        rGradAnlm[i] *= aEquScale;
    }
    mplusGradParaPostFuse<FSTYLE>(rGradAnlm, tCnlm, tGradPara, aEquSize, tSizeN, tLMaxMax);
    if (WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE) return;
    // anlm -> cnlm
    mplusGradAnlm<FSTYLE>(rGradAnlm, rGradCnlm, aEquWeight, aEquSize, tSizeN, tLMaxMax);
    // plus to para
    calBackwardMainLoop<WTYPE, FSTYLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rGradPara, rGradCnlm, tForwardCacheElse, rBackwardCacheElse, aRCut, aNMax, tLMaxMax, aFuseSize);
}


static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                  jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                  jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_NONE: {
            calFp<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calFp<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calFp<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseWeight, aFuseSize, aEquWeight, aEquSize, aEquScale);
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
                        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                        jint aWType, jint aFuseStyle, jint aFuseSize,
                        jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
            if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackward<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calBackward<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackward<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calBackward<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackward<WTYPE_EXFULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_FULL: {
            calBackward<WTYPE_FULL, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_NONE: {
            calBackward<WTYPE_NONE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackward<WTYPE_DEFAULT, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            return;
        }
        case WTYPE_FUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackward<WTYPE_FUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calBackward<WTYPE_FUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        case WTYPE_EXFUSE: {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                calBackward<WTYPE_EXFUSE, FUSE_STYLE_LIMITED>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            } else {
                calBackward<WTYPE_EXFUSE, FUSE_STYLE_EXTENSIVE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, rBackwardCache, aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max, aLLMax, aFuseSize, aEquWeight, aEquSize, aEquScale);
            }
            return;
        }
        default: {
            return;
        }}
    }
}
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                     jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                     jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
    
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardCache, jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max, jint aLLMax,
                             jint aWType, jint aFuseStyle, jdouble *aFuseWeight, jint aFuseSize,
                             jdouble *aEquWeight, jint aEquSize, jdouble aEquScale) noexcept {
    
}

}

#endif //BASIS_EQUIVARIANT_SPHERICAL_CHEBYSHEV_H