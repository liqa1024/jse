#include "jsex_nnap_basis_Chebyshev.h"
#include "nnap_util.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fc = pow4_jse(1.0 - pow2_jse(dis/aRCut));
        // cal Rn
        jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        jdouble *tRn = aBufferNl ? (rNlRn + j*(aNMax+1)) : rNlRn;
        chebyshevFull(aNMax, tRnX, tRn);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            for (jint n = 0; n <= aNMax; ++n) {
                tRn[n] *= wt;
            }
            break;
        }
        default: {
            break;
        }}
        // cal fp
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    rFp[n] += fc*tRn[n];
                    if (rFpGradNlSize!=NULL) ++rFpGradNlSize[n];
                }
            } else {
                jint tShiftFp = (aNMax+1)*type;
                jdouble *tFpWt = rFp+tShiftFp;
                jint *tFpGradNlSizeWt = rFpGradNlSize==NULL ? NULL : (rFpGradNlSize+tShiftFp);
                for (jint n = 0; n <= aNMax; ++n) {
                    jdouble tFpn = fc*tRn[n];
                    rFp[n] += tFpn;
                    tFpWt[n] += tFpn;
                    if (rFpGradNlSize != NULL) {
                        ++rFpGradNlSize[n];
                        ++tFpGradNlSizeWt[n];
                    }
                }
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            jint tShiftFp = (aNMax+1)*(type-1);
            jdouble *tFp = rFp+tShiftFp;
            jint *tFpGradNlSize = rFpGradNlSize==NULL ? NULL : (rFpGradNlSize+tShiftFp);
            for (jint n = 0; n <= aNMax; ++n) {
                tFp[n] += fc*tRn[n];
                if (tFpGradNlSize!=NULL) ++tFpGradNlSize[n];
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint n = 0; n <= aNMax; ++n) {
                rFp[n] += fc*tRn[n];
                if (rFpGradNlSize!=NULL) ++rFpGradNlSize[n];
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    rFp[n] += fc*tRn[n];
                    if (rFpGradNlSize!=NULL) ++rFpGradNlSize[n];
                }
            } else {
                // cal weight of type here
                jdouble wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                jdouble *tFpWt = rFp+tShiftFp;
                jint *tFpGradNlSizeWt = rFpGradNlSize==NULL ? NULL : (rFpGradNlSize+tShiftFp);
                for (jint n = 0; n <= aNMax; ++n) {
                    jdouble tFpn = fc*tRn[n];
                    rFp[n] += tFpn;
                    tFpWt[n] += wt*tFpn;
                    if (rFpGradNlSize != NULL) {
                        ++rFpGradNlSize[n];
                        ++tFpGradNlSizeWt[n];
                    }
                }
            }
            break;
        }
        default: {
            break;
        }}
    }
}

#undef JSE_NNAP_CONSTANT_INIT_Chebyshev
#define JSE_NNAP_CONSTANT_INIT_Chebyshev                        \
jint tSize;                                                     \
    switch(aWType) {                                            \
case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {                  \
    tSize = aTypeNum>1 ? (aTypeNum+1)*(aNMax+1) : (aNMax+1);    \
    break;                                                      \
}                                                               \
case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {                    \
    tSize = aTypeNum*(aNMax+1);                                 \
    break;                                                      \
}                                                               \
case jsex_nnap_basis_Chebyshev_WTYPE_NONE:                      \
case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {                  \
    tSize = aNMax+1;                                            \
    break;                                                      \
}                                                               \
case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {                 \
    tSize = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);           \
    break;                                                      \
}                                                               \
default: {                                                      \
    tSize = 0;                                                  \
    break;                                                      \
}}                                                              \



JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_eval1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rNlRn, jdoubleArray rFp, jint aShiftFp, jintArray rFpGradNlSize, jint aShiftFpGradNlSize,
        jboolean aBufferNl, jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNlRn = (jdouble *)getJArrayBuf(aEnv, rNlRn);
    jdouble *tFp = (jdouble *)getJArrayBuf(aEnv, rFp);
    jint *tFpGradNlSize = rFpGradNlSize==NULL ? NULL : (jint *)getJArrayBuf(aEnv, rFpGradNlSize);
    
    // const init
    JSE_NNAP_CONSTANT_INIT_Chebyshev
    // clear fp first
    jdouble *tFp_ = tFp + aShiftFp;
    jint *tFpGradNlSize_ = tFpGradNlSize==NULL ? NULL : (tFpGradNlSize+aShiftFpGradNlSize);
    for (jint i = 0; i < tSize; ++i) {
        tFp_[i] = 0.0;
        if (tFpGradNlSize_!=NULL) tFpGradNlSize_[i] = 0;
    }
    // do cal
    calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN,
          tNlRn, tFp_, tFpGradNlSize_,
          aBufferNl, aTypeNum, aRCut, aNMax, aWType);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rNlRn, tNlRn, aBufferNl?0:JNI_ABORT);
    releaseJArrayBuf(aEnv, rFp, tFp, 0);
    if (rFpGradNlSize!=NULL) releaseJArrayBuf(aEnv, rFpGradNlSize, tFpGradNlSize, 0);
}


JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_evalGrad1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNlRn, jdoubleArray rRnPx, jdoubleArray rRnPy, jdoubleArray rRnPz, jdoubleArray rCheby2,
        jintArray rFpGradNlIndex, jint aShiftFpGradNlIndex, jintArray rFpGradFpIndex, jint aShiftFpGradFpIndex,
        jdoubleArray rFpPx, jint aShiftFpPx, jdoubleArray rFpPy, jint aShiftFpPy, jdoubleArray rFpPz, jint aShiftFpPz,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNlRn = (jdouble *)getJArrayBuf(aEnv, aNlRn);
    jdouble *tRnPx = (jdouble *)getJArrayBuf(aEnv, rRnPx);
    jdouble *tRnPy = (jdouble *)getJArrayBuf(aEnv, rRnPy);
    jdouble *tRnPz = (jdouble *)getJArrayBuf(aEnv, rRnPz);
    jdouble *tCheby2 = (jdouble *)getJArrayBuf(aEnv, rCheby2);
    jint *tFpGradNlIndex = (jint *)getJArrayBuf(aEnv, rFpGradNlIndex);
    jint *tFpGradFpIndex = (jint *)getJArrayBuf(aEnv, rFpGradFpIndex);
    jdouble *tFpPx = (jdouble *)getJArrayBuf(aEnv, rFpPx);
    jdouble *tFpPy = (jdouble *)getJArrayBuf(aEnv, rFpPy);
    jdouble *tFpPz = (jdouble *)getJArrayBuf(aEnv, rFpPz);
    
    jint tShiftFpP = 0;
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = tNlType[j];
        jdouble dx = tNlDx[j], dy = tNlDy[j], dz = tNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // init fpPxyz
        jint *tFpGradNlIndex_ = tFpGradNlIndex + tShiftFpP + aShiftFpGradNlIndex;
        jint *tFpGradFpIndex_ = tFpGradFpIndex + tShiftFpP + aShiftFpGradFpIndex;
        jdouble *tFpPx_ = tFpPx + tShiftFpP + aShiftFpPx;
        jdouble *tFpPy_ = tFpPy + tShiftFpP + aShiftFpPy;
        jdouble *tFpPz_ = tFpPz + tShiftFpP + aShiftFpPz;
        // cal fc
        jdouble fcMul = 1.0 - pow2_jse(dis/aRCut);
        jdouble fcMul3 = pow3_jse(fcMul);
        jdouble fc = fcMul3 * fcMul;
        jdouble fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        jdouble fcPx = dx * fcPMul;
        jdouble fcPy = dy * fcPMul;
        jdouble fcPz = dz * fcPMul;
        // cal Rn
        jdouble *tRn = tNlRn + j*(aNMax+1);
        const jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        chebyshev2Full(aNMax-1, tRnX, tCheby2);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, wt, dx, dy, dz);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, 1.0, dx, dy, dz);
            break;
        }
        default: {
            break;
        }}
        // cal fpPxyz
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and set to fp
                    const jdouble tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                    tFpGradNlIndex_[n] = j;
                    tFpGradFpIndex_[n] = n;
                }
                tShiftFpP += (aNMax+1);
            } else {
                jint tShiftFp = aNMax+1;
                jdouble *tFpPxWt = tFpPx_+tShiftFp;
                jdouble *tFpPyWt = tFpPy_+tShiftFp;
                jdouble *tFpPzWt = tFpPz_+tShiftFp;
                jint *tFpGradNlIndexWt = tFpGradNlIndex_+tShiftFp;
                jint *tFpGradFpIndexWt = tFpGradFpIndex_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // set to fp
                    tFpPx_[n] = subFpPx; tFpPxWt[n] = subFpPx;
                    tFpPy_[n] = subFpPy; tFpPyWt[n] = subFpPy;
                    tFpPz_[n] = subFpPz; tFpPzWt[n] = subFpPz;
                    tFpGradNlIndex_[n] = j; tFpGradNlIndexWt[n] = j;
                    tFpGradFpIndex_[n] = n; tFpGradFpIndexWt[n] = n + (aNMax+1)*type;
                }
                tShiftFpP += (aNMax+aNMax+2);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                tFpGradNlIndex_[n] = j;
                tFpGradFpIndex_[n] = n + (aNMax+1)*(type-1);
            }
            tShiftFpP += (aNMax+1);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                tFpGradNlIndex_[n] = j;
                tFpGradFpIndex_[n] = n;
            }
            tShiftFpP += (aNMax+1);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and set to fp
                    const jdouble tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                    tFpGradNlIndex_[n] = j;
                    tFpGradFpIndex_[n] = n;
                }
                tShiftFpP += (aNMax+1);
            } else {
                // cal weight of type here
                jdouble wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                jdouble *tFpPxWt = tFpPx_+tShiftFp;
                jdouble *tFpPyWt = tFpPy_+tShiftFp;
                jdouble *tFpPzWt = tFpPz_+tShiftFp;
                jint *tFpGradNlIndexWt = tFpGradNlIndex_+tShiftFp;
                jint *tFpGradFpIndexWt = tFpGradFpIndex_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // set to fp
                    tFpPx_[n] = subFpPx; tFpPxWt[n] = wt*subFpPx;
                    tFpPy_[n] = subFpPy; tFpPyWt[n] = wt*subFpPy;
                    tFpPz_[n] = subFpPz; tFpPzWt[n] = wt*subFpPz;
                    tFpGradNlIndex_[n] = j; tFpGradNlIndexWt[n] = j;
                    tFpGradFpIndex_[n] = n; tFpGradFpIndexWt[n] = n + (aNMax+1);
                }
                tShiftFpP += (aNMax+aNMax+2);
            }
            break;
        }
        default: {
            break;
        }}
    }
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlRn, tNlRn, JNI_ABORT);
    releaseJArrayBuf(aEnv, rRnPx, tRnPx, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPy, tRnPy, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPz, tRnPz, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rCheby2, tCheby2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rFpGradNlIndex, tFpGradNlIndex, 0);
    releaseJArrayBuf(aEnv, rFpGradFpIndex, tFpGradFpIndex, 0);
    releaseJArrayBuf(aEnv, rFpPx, tFpPx, 0);
    releaseJArrayBuf(aEnv, rFpPy, tFpPy, 0);
    releaseJArrayBuf(aEnv, rFpPz, tFpPz, 0);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_evalGradWithShift1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNlRn, jdoubleArray rRnPx, jdoubleArray rRnPy, jdoubleArray rRnPz, jdoubleArray rCheby2,
        jint aShiftFp, jint aRestFp, jdoubleArray rFpPx, jdoubleArray rFpPy, jdoubleArray rFpPz,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNlRn = (jdouble *)getJArrayBuf(aEnv, aNlRn);
    jdouble *tRnPx = (jdouble *)getJArrayBuf(aEnv, rRnPx);
    jdouble *tRnPy = (jdouble *)getJArrayBuf(aEnv, rRnPy);
    jdouble *tRnPz = (jdouble *)getJArrayBuf(aEnv, rRnPz);
    jdouble *tCheby2 = (jdouble *)getJArrayBuf(aEnv, rCheby2);
    jdouble *tFpPx = (jdouble *)getJArrayBuf(aEnv, rFpPx);
    jdouble *tFpPy = (jdouble *)getJArrayBuf(aEnv, rFpPy);
    jdouble *tFpPz = (jdouble *)getJArrayBuf(aEnv, rFpPz);
    
    // const init
    JSE_NNAP_CONSTANT_INIT_Chebyshev
    
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init fpPxyz
        jint tShiftFpP = j*(aShiftFp+tSize+aRestFp) + aShiftFp;
        jdouble *tFpPx_ = tFpPx+tShiftFpP;
        jdouble *tFpPy_ = tFpPy+tShiftFpP;
        jdouble *tFpPz_ = tFpPz+tShiftFpP;
        // always clear fpPxyz
        for (jint i = 0; i < tSize; ++i) {
            tFpPx_[i] = 0.0;
            tFpPy_[i] = 0.0;
            tFpPz_[i] = 0.0;
        }
        // init nl
        jint type = tNlType[j];
        jdouble dx = tNlDx[j], dy = tNlDy[j], dz = tNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fcMul = 1.0 - pow2_jse(dis/aRCut);
        jdouble fcMul3 = pow3_jse(fcMul);
        jdouble fc = fcMul3 * fcMul;
        jdouble fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        jdouble fcPx = dx * fcPMul;
        jdouble fcPy = dy * fcPMul;
        jdouble fcPz = dz * fcPMul;
        // cal Rn
        jdouble *tRn = tNlRn + j*(aNMax+1);
        const jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        chebyshev2Full(aNMax-1, tRnX, tCheby2);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, wt, dx, dy, dz);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, 1.0, dx, dy, dz);
            break;
        }
        default: {
            break;
        }}
        // cal fpPxyz
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and set to fp
                    const jdouble tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                jint tShiftFp = (aNMax+1)*type;
                jdouble *tFpPxWt = tFpPx_+tShiftFp;
                jdouble *tFpPyWt = tFpPy_+tShiftFp;
                jdouble *tFpPzWt = tFpPz_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // set to fp
                    tFpPx_[n] = subFpPx; tFpPxWt[n] = subFpPx;
                    tFpPy_[n] = subFpPy; tFpPyWt[n] = subFpPy;
                    tFpPz_[n] = subFpPz; tFpPzWt[n] = subFpPz;
                }
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            jint tShiftFp = (aNMax+1)*(type-1);
            jdouble *tFpPxWt = tFpPx_+tShiftFp;
            jdouble *tFpPyWt = tFpPy_+tShiftFp;
            jdouble *tFpPzWt = tFpPz_+tShiftFp;
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPxWt[n] = (fc*tRnPx[n] + fcPx*tRnn);
                tFpPyWt[n] = (fc*tRnPy[n] + fcPy*tRnn);
                tFpPzWt[n] = (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and set to fp
                    const jdouble tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                // cal weight of type here
                jdouble wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                jdouble *tFpPxWt = tFpPx_+tShiftFp;
                jdouble *tFpPyWt = tFpPy_+tShiftFp;
                jdouble *tFpPzWt = tFpPz_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // set to fp
                    tFpPx_[n] = subFpPx; tFpPxWt[n] = wt*subFpPx;
                    tFpPy_[n] = subFpPy; tFpPyWt[n] = wt*subFpPy;
                    tFpPz_[n] = subFpPz; tFpPzWt[n] = wt*subFpPz;
                }
            }
            break;
        }
        default: {
            break;
        }}
    }
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlRn, tNlRn, JNI_ABORT);
    releaseJArrayBuf(aEnv, rRnPx, tRnPx, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPy, tRnPy, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPz, tRnPz, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rCheby2, tCheby2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rFpPx, tFpPx, 0);
    releaseJArrayBuf(aEnv, rFpPy, tFpPy, 0);
    releaseJArrayBuf(aEnv, rFpPz, tFpPz, 0);
}


JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_evalGradAndForceDot1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNlRn, jdoubleArray rRnPx, jdoubleArray rRnPy, jdoubleArray rRnPz, jdoubleArray rCheby2,
        jdoubleArray aNNGrad, jint aShiftFp, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNlRn = (jdouble *)getJArrayBuf(aEnv, aNlRn);
    jdouble *tRnPx = (jdouble *)getJArrayBuf(aEnv, rRnPx);
    jdouble *tRnPy = (jdouble *)getJArrayBuf(aEnv, rRnPy);
    jdouble *tRnPz = (jdouble *)getJArrayBuf(aEnv, rRnPz);
    jdouble *tCheby2 = (jdouble *)getJArrayBuf(aEnv, rCheby2);
    jdouble *tNNGrad = (jdouble *)getJArrayBuf(aEnv, aNNGrad);
    jdouble *tFx = (jdouble *)getJArrayBuf(aEnv, rFx);
    jdouble *tFy = (jdouble *)getJArrayBuf(aEnv, rFy);
    jdouble *tFz = (jdouble *)getJArrayBuf(aEnv, rFz);
    
    // init nnGrad
    jdouble *tNNGrad_ = tNNGrad + aShiftFp;
    
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = tNlType[j];
        jdouble dx = tNlDx[j], dy = tNlDy[j], dz = tNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fcMul = 1.0 - pow2_jse(dis/aRCut);
        jdouble fcMul3 = pow3_jse(fcMul);
        jdouble fc = fcMul3 * fcMul;
        jdouble fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        jdouble fcPx = dx * fcPMul;
        jdouble fcPy = dy * fcPMul;
        jdouble fcPz = dz * fcPMul;
        // cal Rn
        jdouble *tRn = tNlRn + j*(aNMax+1);
        const jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        chebyshev2Full(aNMax-1, tRnX, tCheby2);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, wt, dx, dy, dz);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL:
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            calRnPxyz(tRnPx, tRnPy, tRnPz, tCheby2, aNMax, dis, aRCut, 1.0, dx, dy, dz);
            break;
        }
        default: {
            break;
        }}
        // cal fpPxyz
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and accumulate to force
                    const jdouble tRnn = tRn[n];
                    const jdouble subNNGrad = tNNGrad_[n];
                    tFx[j] += subNNGrad * (fc*tRnPx[n] + fcPx*tRnn);
                    tFy[j] += subNNGrad * (fc*tRnPy[n] + fcPy*tRnn);
                    tFz[j] += subNNGrad * (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                jint tShiftFp = (aNMax+1)*type;
                jdouble *tNNGradWt = tNNGrad_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // accumulate to force
                    const jdouble subNNGrad = tNNGrad_[n], subNNGradWt = tNNGradWt[n];
                    tFx[j] += (subNNGrad + subNNGradWt) * subFpPx;
                    tFy[j] += (subNNGrad + subNNGradWt) * subFpPy;
                    tFz[j] += (subNNGrad + subNNGradWt) * subFpPz;
                }
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            jint tShiftFp = (aNMax+1)*(type-1);
            jdouble *tNNGradWt = tNNGrad_+tShiftFp;
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and accumulate to force
                const jdouble tRnn = tRn[n];
                const jdouble subNNGradWt = tNNGradWt[n];
                tFx[j] += subNNGradWt * (fc*tRnPx[n] + fcPx*tRnn);
                tFy[j] += subNNGradWt * (fc*tRnPy[n] + fcPy*tRnn);
                tFz[j] += subNNGradWt * (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and accumulate to force
                const jdouble tRnn = tRn[n];
                const jdouble subNNGrad = tNNGrad_[n];
                tFx[j] += subNNGrad * (fc*tRnPx[n] + fcPx*tRnn);
                tFy[j] += subNNGrad * (fc*tRnPy[n] + fcPy*tRnn);
                tFz[j] += subNNGrad * (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and accumulate to force
                    const jdouble tRnn = tRn[n];
                    const jdouble subNNGrad = tNNGrad_[n];
                    tFx[j] += subNNGrad * (fc*tRnPx[n] + fcPx*tRnn);
                    tFy[j] += subNNGrad * (fc*tRnPy[n] + fcPy*tRnn);
                    tFz[j] += subNNGrad * (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                // cal weight of type here
                jdouble wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                jdouble *tNNGradWt = tNNGrad_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const jdouble tRnn = tRn[n];
                    const jdouble subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const jdouble subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const jdouble subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // accumulate to force
                    const jdouble subNNGrad = tNNGrad_[n], subNNGradWt = tNNGradWt[n];
                    tFx[j] += (subNNGrad + wt*subNNGradWt) * subFpPx;
                    tFy[j] += (subNNGrad + wt*subNNGradWt) * subFpPy;
                    tFz[j] += (subNNGrad + wt*subNNGradWt) * subFpPz;
                }
            }
            break;
        }
        default: {
            break;
        }}
    }
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlRn, tNlRn, JNI_ABORT);
    releaseJArrayBuf(aEnv, rRnPx, tRnPx, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPy, tRnPy, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rRnPz, tRnPz, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rCheby2, tCheby2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, aNNGrad, tNNGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFx, tFx, 0);
    releaseJArrayBuf(aEnv, rFy, tFy, 0);
    releaseJArrayBuf(aEnv, rFz, tFz, 0);
}

#ifdef __cplusplus
}
#endif
