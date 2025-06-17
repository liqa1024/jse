#include "jsex_nnap_basis_Chebyshev.h"
#include "nnap_util.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline void calFp(double *aNlDx, double *aNlDy, double *aNlDz, jint *aNlType, jint aNN,
                         double *rNlRn, double *rFp, jboolean aBufferNl,
                         jint aTypeNum, double aRCut, jint aNMax, jint aWType) {
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        double dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        double dis = sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        double fc = pow4_jse(1.0 - pow2_jse(dis/aRCut));
        // cal Rn
        double tRnX = 1.0 - 2.0*dis/aRCut;
        double *tRn = aBufferNl ? (rNlRn + j*(aNMax+1)) : rNlRn;
        chebyshevFull(aNMax, tRnX, tRn);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            double wt = ((type&1)==1) ? type : -type;
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
                for (jint tN = 0; tN <= aNMax; ++tN) {
                    rFp[tN] += fc*tRn[tN];
                }
            } else {
                double *tFpWt = rFp + (aNMax+1)*type;
                for (jint tN = 0; tN <= aNMax; ++tN) {
                    double tFpn = fc*tRn[tN];
                    rFp[tN] += tFpn;
                    tFpWt[tN] += tFpn;
                }
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            double *tFp = rFp + (aNMax+1)*(type-1);
            for (jint tN = 0; tN <= aNMax; ++tN) {
                tFp[tN] += fc*tRn[tN];
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint tN = 0; tN <= aNMax; ++tN) {
                rFp[tN] += fc*tRn[tN];
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint tN = 0; tN <= aNMax; ++tN) {
                    rFp[tN] += fc*tRn[tN];
                }
            } else {
                // cal weight of type here
                double wt = ((type&1)==1) ? type : -type;
                double *tFpWt = rFp + (aNMax+1);
                for (jint tN = 0; tN <= aNMax; ++tN) {
                    double tFpn = fc*tRn[tN];
                    rFp[tN] += tFpn;
                    tFpWt[tN] += wt*tFpn;
                }
            }
            break;
        }
        default: {
            break;
        }}
    }
}


JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_eval1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rNlRn, jdoubleArray rFp, jint aShiftFp, jboolean aBufferNl,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    double *tNlDx = (double *)getJArrayBuf(aEnv, aNlDx);
    double *tNlDy = (double *)getJArrayBuf(aEnv, aNlDy);
    double *tNlDz = (double *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    double *tNlRn = (double *)getJArrayBuf(aEnv, rNlRn);
    double *tFp = (double *)getJArrayBuf(aEnv, rFp);
    
    // const init
    jint tSize;
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        tSize = aTypeNum>1 ? (aTypeNum+1)*(aNMax+1) : (aNMax+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        tSize = aTypeNum*(aNMax+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        tSize = aNMax+1;
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        tSize = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        break;
    }
    default: {
        tSize = 0;
        break;
    }}
    // clear fp first
    double *tFp_ = tFp + aShiftFp;
    for (jint i = 0; i < tSize; ++i) {
        tFp_[i] = 0.0;
    }
    // do cal
    calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN,
          tNlRn, tFp_, aBufferNl,
          aTypeNum, aRCut, aNMax, aWType);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rNlRn, tNlRn, aBufferNl?0:JNI_ABORT);
    releaseJArrayBuf(aEnv, rFp, tFp, 0);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_evalPartial1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNlRn, jdoubleArray rRnPx, jdoubleArray rRnPy, jdoubleArray rRnPz, jdoubleArray rCheby2,
        jint aShiftFp, jint aRestFp, jdoubleArray rFpPx, jdoubleArray rFpPy, jdoubleArray rFpPz,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    double *tNlDx = (double *)getJArrayBuf(aEnv, aNlDx);
    double *tNlDy = (double *)getJArrayBuf(aEnv, aNlDy);
    double *tNlDz = (double *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    double *tNlRn = (double *)getJArrayBuf(aEnv, aNlRn);
    double *tRnPx = (double *)getJArrayBuf(aEnv, rRnPx);
    double *tRnPy = (double *)getJArrayBuf(aEnv, rRnPy);
    double *tRnPz = (double *)getJArrayBuf(aEnv, rRnPz);
    double *tCheby2 = (double *)getJArrayBuf(aEnv, rCheby2);
    double *tFpPx = (double *)getJArrayBuf(aEnv, rFpPx);
    double *tFpPy = (double *)getJArrayBuf(aEnv, rFpPy);
    double *tFpPz = (double *)getJArrayBuf(aEnv, rFpPz);
    
    // const init
    jint tSize;
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        tSize = aTypeNum>1 ? (aTypeNum+1)*(aNMax+1) : (aNMax+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        tSize = aTypeNum*(aNMax+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        tSize = aNMax+1;
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        tSize = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        break;
    }
    default: {
        tSize = 0;
        break;
    }}
    
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init fpPxyz
        jint tShiftFpP = j*(aShiftFp+tSize+aRestFp) + aShiftFp;
        double *tFpPx_ = tFpPx+tShiftFpP;
        double *tFpPy_ = tFpPy+tShiftFpP;
        double *tFpPz_ = tFpPz+tShiftFpP;
        // init nl
        jint type = tNlType[j];
        double dx = tNlDx[j], dy = tNlDy[j], dz = tNlDz[j];
        double dis = sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) {
            // clear fpPxyz here
            for (jint i = 0; i < tSize; ++i) {
                tFpPx_[i] = 0.0;
                tFpPy_[i] = 0.0;
                tFpPz_[i] = 0.0;
            }
            continue;
        }
        // cal fc
        double fcMul = 1.0 - pow2_jse(dis/aRCut);
        double fcMul3 = pow3_jse(fcMul);
        double fc = fcMul3 * fcMul;
        double fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        double fcPx = dx * fcPMul;
        double fcPy = dy * fcPMul;
        double fcPz = dz * fcPMul;
        // cal Rn
        double *tRn = tNlRn + j*(aNMax+1);
        const double tRnX = 1.0 - 2.0*dis/aRCut;
        chebyshev2Full(aNMax-1, tRnX, tCheby2);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            double wt = ((type&1)==1) ? type : -type;
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
                    const double tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                jint tShiftFp = (aNMax+1)*type;
                double *tFpPxWt = tFpPx_+tShiftFp;
                double *tFpPyWt = tFpPy_+tShiftFp;
                double *tFpPzWt = tFpPz_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const double tRnn = tRn[n];
                    const double subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const double subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const double subFpPz = fc*tRnPz[n] + fcPz*tRnn;
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
            double *tFpPxWt = tFpPx_+tShiftFp;
            double *tFpPyWt = tFpPy_+tShiftFp;
            double *tFpPzWt = tFpPz_+tShiftFp;
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and set to fp
                const double tRnn = tRn[n];
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
                const double tRnn = tRn[n];
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
                    const double tRnn = tRn[n];
                    tFpPx_[n] = (fc*tRnPx[n] + fcPx*tRnn);
                    tFpPy_[n] = (fc*tRnPy[n] + fcPy*tRnn);
                    tFpPz_[n] = (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                // cal weight of type here
                double wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                double *tFpPxWt = tFpPx_+tShiftFp;
                double *tFpPyWt = tFpPy_+tShiftFp;
                double *tFpPzWt = tFpPz_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const double tRnn = tRn[n];
                    const double subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const double subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const double subFpPz = fc*tRnPz[n] + fcPz*tRnn;
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


JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_evalPartialAndForceDot1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNlRn, jdoubleArray rRnPx, jdoubleArray rRnPy, jdoubleArray rRnPz, jdoubleArray rCheby2,
        jdoubleArray aFpGrad, jint aShiftFp, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    // java array init
    double *tNlDx = (double *)getJArrayBuf(aEnv, aNlDx);
    double *tNlDy = (double *)getJArrayBuf(aEnv, aNlDy);
    double *tNlDz = (double *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    double *tNlRn = (double *)getJArrayBuf(aEnv, aNlRn);
    double *tRnPx = (double *)getJArrayBuf(aEnv, rRnPx);
    double *tRnPy = (double *)getJArrayBuf(aEnv, rRnPy);
    double *tRnPz = (double *)getJArrayBuf(aEnv, rRnPz);
    double *tCheby2 = (double *)getJArrayBuf(aEnv, rCheby2);
    double *tFpGrad = (double *)getJArrayBuf(aEnv, aFpGrad);
    double *tFx = (double *)getJArrayBuf(aEnv, rFx);
    double *tFy = (double *)getJArrayBuf(aEnv, rFy);
    double *tFz = (double *)getJArrayBuf(aEnv, rFz);
    
    // init fpGrad
    double *tFpGrad_ = tFpGrad + aShiftFp;
    
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = tNlType[j];
        double dx = tNlDx[j], dy = tNlDy[j], dz = tNlDz[j];
        double dis = sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        double fcMul = 1.0 - pow2_jse(dis/aRCut);
        double fcMul3 = pow3_jse(fcMul);
        double fc = fcMul3 * fcMul;
        double fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        double fcPx = dx * fcPMul;
        double fcPy = dy * fcPMul;
        double fcPz = dz * fcPMul;
        // cal Rn
        double *tRn = tNlRn + j*(aNMax+1);
        const double tRnX = 1.0 - 2.0*dis/aRCut;
        chebyshev2Full(aNMax-1, tRnX, tCheby2);
        switch(aWType) {
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            // cal weight of type here
            double wt = ((type&1)==1) ? type : -type;
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
                    const double tRnn = tRn[n];
                    const double subFpGrad = tFpGrad_[n];
                    tFx[j] += subFpGrad * (fc*tRnPx[n] + fcPx*tRnn);
                    tFy[j] += subFpGrad * (fc*tRnPy[n] + fcPy*tRnn);
                    tFz[j] += subFpGrad * (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                jint tShiftFp = (aNMax+1)*type;
                double *tFpGradWt = tFpGrad_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const double tRnn = tRn[n];
                    const double subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const double subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const double subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // accumulate to force
                    const double subFpGrad = tFpGrad_[n], subFpGradWt = tFpGradWt[n];
                    tFx[j] += (subFpGrad + subFpGradWt) * subFpPx;
                    tFy[j] += (subFpGrad + subFpGradWt) * subFpPy;
                    tFz[j] += (subFpGrad + subFpGradWt) * subFpPz;
                }
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            jint tShiftFp = (aNMax+1)*(type-1);
            double *tFpGradWt = tFpGrad_+tShiftFp;
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and accumulate to force
                const double tRnn = tRn[n];
                const double subFpGradWt = tFpGradWt[n];
                tFx[j] += subFpGradWt * (fc*tRnPx[n] + fcPx*tRnn);
                tFy[j] += subFpGradWt * (fc*tRnPy[n] + fcPy*tRnn);
                tFz[j] += subFpGradWt * (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
        case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
            for (jint n = 0; n <= aNMax; ++n) {
                // cal subFpPxyz and accumulate to force
                const double tRnn = tRn[n];
                const double subFpGrad = tFpGrad_[n];
                tFx[j] += subFpGrad * (fc*tRnPx[n] + fcPx*tRnn);
                tFy[j] += subFpGrad * (fc*tRnPy[n] + fcPy*tRnn);
                tFz[j] += subFpGrad * (fc*tRnPz[n] + fcPz*tRnn);
            }
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            if (aTypeNum == 1) {
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz and accumulate to force
                    const double tRnn = tRn[n];
                    const double subFpGrad = tFpGrad_[n];
                    tFx[j] += subFpGrad * (fc*tRnPx[n] + fcPx*tRnn);
                    tFy[j] += subFpGrad * (fc*tRnPy[n] + fcPy*tRnn);
                    tFz[j] += subFpGrad * (fc*tRnPz[n] + fcPz*tRnn);
                }
            } else {
                // cal weight of type here
                double wt = ((type&1)==1) ? type : -type;
                jint tShiftFp = aNMax+1;
                double *tFpGradWt = tFpGrad_+tShiftFp;
                for (jint n = 0; n <= aNMax; ++n) {
                    // cal subFpPxyz first
                    const double tRnn = tRn[n];
                    const double subFpPx = fc*tRnPx[n] + fcPx*tRnn;
                    const double subFpPy = fc*tRnPy[n] + fcPy*tRnn;
                    const double subFpPz = fc*tRnPz[n] + fcPz*tRnn;
                    // accumulate to force
                    const double subFpGrad = tFpGrad_[n], subFpGradWt = tFpGradWt[n];
                    tFx[j] += (subFpGrad + wt*subFpGradWt) * subFpPx;
                    tFy[j] += (subFpGrad + wt*subFpGradWt) * subFpPy;
                    tFz[j] += (subFpGrad + wt*subFpGradWt) * subFpPz;
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
    releaseJArrayBuf(aEnv, aFpGrad, tFpGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFx, tFx, 0);
    releaseJArrayBuf(aEnv, rFy, tFy, 0);
    releaseJArrayBuf(aEnv, rFz, tFz, 0);
}

#ifdef __cplusplus
}
#endif
