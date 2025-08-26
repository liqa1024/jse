#include "jsex_nnap_basis_Chebyshev.h"
#include "nnap_util.hpp"


template <jint NMAX, jint WTYPE, jboolean NL_SIZE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                  jboolean aBufferNl, jint aTypeNum, jdouble aRCut) {
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (aTypeNum+1)*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        tSizeFp = aTypeNum*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        tSizeFp = NMAX+1;
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (NMAX+NMAX+2);
        break;
    }
    default: {
        tSizeFp = 0;
        break;
    }}
    // clear fp first
    for (jint i = 0; i < tSizeFp; ++i) {
        rFp[i] = 0.0;
    }
    if (NL_SIZE) {
        for (jint i = 0; i < tSizeFp; ++i) {
            rFpGradNlSize[i] = 0;
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
        jdouble fc = JSE_NNAP::pow4(1.0 - JSE_NNAP::pow2(dis/aRCut));
        // cal Rn
        jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        jdouble *tRn = aBufferNl ? (rNlRn + j*(NMAX+1)) : rNlRn;
        JSE_NNAP::chebyshevFull<NMAX>(tRnX, tRn);
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            for (jint n = 0; n <= NMAX; ++n) {
                tRn[n] *= wt;
            }
        }
        // cal fp
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FULL || aTypeNum==1) {
            jint tShiftFp;
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                tShiftFp = (NMAX+1)*(type-1);
            } else {
                tShiftFp = 0;
            }
            jdouble *tFp = rFp+tShiftFp;
            jint *tFpGradNlSize;
            if (NL_SIZE) {
                tFpGradNlSize = rFpGradNlSize+tShiftFp;
            }
            for (jint n = 0; n <= NMAX; ++n) {
                tFp[n] += fc*tRn[n];
                if (NL_SIZE) ++tFpGradNlSize[n];
            }
            continue;
        }
        jdouble wt;
        jint tShiftFp;
        jdouble *tFpWt;
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
            wt = 1.0;
            tShiftFp = (NMAX+1)*type;
            tFpWt = rFp+tShiftFp;
        } else
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT) {
            // cal weight of type here
            wt = ((type&1)==1) ? type : -type;
            tShiftFp = NMAX+1;
            tFpWt = rFp+tShiftFp;
        } else {
            continue;
        }
        jint *tFpGradNlSizeWt;
        if (NL_SIZE) {
            tFpGradNlSizeWt = rFpGradNlSize+tShiftFp;
        }
        for (jint n = 0; n <= NMAX; ++n) {
            jdouble tFpn = fc*tRn[n];
            rFp[n] += tFpn;
            tFpWt[n] += wt*tFpn;
            if (NL_SIZE) {
                ++rFpGradNlSize[n];
                ++tFpGradNlSizeWt[n];
            }
        }
    }
}
template <jint WTYPE, jboolean NL_SIZE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jint aTypeNum, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFp<0, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 1: {
        calFp<1, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 2: {
        calFp<2, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 3: {
        calFp<3, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 4: {
        calFp<4, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 5: {
        calFp<5, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 6: {
        calFp<6, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 7: {
        calFp<7, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 8: {
        calFp<8, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 9: {
        calFp<9, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 10: {
        calFp<10, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 11: {
        calFp<11, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 12: {
        calFp<12, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 13: {
        calFp<13, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 14: {
        calFp<14, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 15: {
        calFp<15, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 16: {
        calFp<16, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 17: {
        calFp<17, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 18: {
        calFp<18, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 19: {
        calFp<19, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    case 20: {
        calFp<20, WTYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut);
        return;
    }
    default: {
        return;
    }}
}
template <jint WTYPE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jint aTypeNum, jdouble aRCut, jint aNMax) {
    if (rFpGradNlSize == NULL) {
        calFp<WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
    } else {
        calFp<WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
    }
}
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_SINGLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aTypeNum, aRCut, aNMax);
        return;
    }
    default: {
        return;
    }}
}


template <jint NMAX, jint WTYPE, jboolean SPARSE>
static void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                      jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                      jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp,
                      jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz, jint aTypeNum, jdouble aRCut) {
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (aTypeNum+1)*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        tSizeFp = aTypeNum*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE:
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        tSizeFp = NMAX+1;
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (NMAX+NMAX+2);
        break;
    }
    default: {
        tSizeFp = 0;
        break;
    }}
    
    jint tShiftFpP = 0;
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jdouble *tFpPx, *tFpPy, *tFpPz;
        jint *tFpGradNlIndex, *tFpGradFpIndex;
        if (!SPARSE) {
            // init fpPxyz
            tShiftFpP = j*(aShiftFp+tSizeFp+aRestFp) + aShiftFp;
            tFpPx = rFpPx + tShiftFpP;
            tFpPy = rFpPy + tShiftFpP;
            tFpPz = rFpPz + tShiftFpP;
            // always clear fpPxyz
            for (jint i = 0; i < tSizeFp; ++i) {
                tFpPx[i] = 0.0;
                tFpPy[i] = 0.0;
                tFpPz[i] = 0.0;
            }
        }
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        if (SPARSE) {
            // init fpPxyz
            tFpGradNlIndex = rFpGradNlIndex + tShiftFpP;
            tFpGradFpIndex = rFpGradFpIndex + tShiftFpP;
            tFpPx = rFpPx + tShiftFpP;
            tFpPy = rFpPy + tShiftFpP;
            tFpPz = rFpPz + tShiftFpP;
        }
        // cal fc
        jdouble fcMul = 1.0 - JSE_NNAP::pow2(dis/aRCut);
        jdouble fcMul3 = JSE_NNAP::pow3(fcMul);
        jdouble fc = fcMul3 * fcMul;
        jdouble fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        jdouble fcPx = dx * fcPMul;
        jdouble fcPy = dy * fcPMul;
        jdouble fcPz = dz * fcPMul;
        // cal Rn
        jdouble *tRn = aNlRn + j*(NMAX+1);
        const jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        JSE_NNAP::chebyshev2Full<NMAX-1>(tRnX, rCheby2);
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            JSE_NNAP::calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, wt, dx, dy, dz);
        } else {
            JSE_NNAP::calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, 1.0, dx, dy, dz);
        }
        // cal fpPxyz
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FULL || aTypeNum==1) {
            jint tShiftFp;
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                tShiftFp = SPARSE ? 0 : (NMAX+1)*(type-1);
            } else {
                tShiftFp = 0;
            }
            jdouble *tFpPx_ = tFpPx+tShiftFp;
            jdouble *tFpPy_ = tFpPy+tShiftFp;
            jdouble *tFpPz_ = tFpPz+tShiftFp;
            jint *tFpGradNlIndex_, *tFpGradFpIndex_;
            if (SPARSE) {
                tFpGradNlIndex_ = tFpGradNlIndex+tShiftFp;
                tFpGradFpIndex_ = tFpGradFpIndex+tShiftFp;
            }
            for (jint n = 0; n <= NMAX; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPx_[n] = (fc*rRnPx[n] + fcPx*tRnn);
                tFpPy_[n] = (fc*rRnPy[n] + fcPy*tRnn);
                tFpPz_[n] = (fc*rRnPz[n] + fcPz*tRnn);
                if (SPARSE) {
                    tFpGradNlIndex_[n] = j;
                    if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                        tFpGradFpIndex_[n] = n + (NMAX+1)*(type-1);
                    } else {
                        tFpGradFpIndex_[n] = n;
                    }
                }
            }
            if (SPARSE) {
                tShiftFpP += (NMAX+1);
            }
            continue;
        }
        jdouble wt;
        jint tShiftFp;
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
            wt = 1.0;
            tShiftFp = SPARSE ? (NMAX+1) : (NMAX+1)*type;
        } else
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT) {
            wt = ((type&1)==1) ? type : -type;
            tShiftFp = NMAX+1;
        } else {
            continue;
        }
        jdouble *tFpPxWt = tFpPx+tShiftFp;
        jdouble *tFpPyWt = tFpPy+tShiftFp;
        jdouble *tFpPzWt = tFpPz+tShiftFp;
        jint *tFpGradNlIndexWt, *tFpGradFpIndexWt;
        if (SPARSE) {
            tFpGradNlIndexWt = tFpGradNlIndex+tShiftFp;
            tFpGradFpIndexWt = tFpGradFpIndex+tShiftFp;
        }
        for (jint n = 0; n <= NMAX; ++n) {
            // cal subFpPxyz first
            const jdouble tRnn = tRn[n];
            const jdouble subFpPx = fc*rRnPx[n] + fcPx*tRnn;
            const jdouble subFpPy = fc*rRnPy[n] + fcPy*tRnn;
            const jdouble subFpPz = fc*rRnPz[n] + fcPz*tRnn;
            // set to fp
            tFpPx[n] = subFpPx; tFpPxWt[n] = wt*subFpPx;
            tFpPy[n] = subFpPy; tFpPyWt[n] = wt*subFpPy;
            tFpPz[n] = subFpPz; tFpPzWt[n] = wt*subFpPz;
            if (SPARSE) {
                tFpGradNlIndex[n] = j;
                tFpGradFpIndex[n] = n;
                tFpGradNlIndexWt[n] = j;
                if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
                    tFpGradFpIndexWt[n] = n + (NMAX+1)*type;
                } else {
                    tFpGradFpIndexWt[n] = n + (NMAX+1);
                }
            }
        }
        if (SPARSE) {
            tShiftFpP += (NMAX+NMAX+2);
        }
    }
}
template <jint WTYPE, jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz, jint aTypeNum, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFpGrad<0, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 1: {
        calFpGrad<1, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 2: {
        calFpGrad<2, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 3: {
        calFpGrad<3, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 4: {
        calFpGrad<4, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 5: {
        calFpGrad<5, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 6: {
        calFpGrad<6, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 7: {
        calFpGrad<7, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 8: {
        calFpGrad<8, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 9: {
        calFpGrad<9, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 10: {
        calFpGrad<10, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 11: {
        calFpGrad<11, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 12: {
        calFpGrad<12, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 13: {
        calFpGrad<13, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 14: {
        calFpGrad<14, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 15: {
        calFpGrad<15, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 16: {
        calFpGrad<16, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 17: {
        calFpGrad<17, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 18: {
        calFpGrad<18, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 19: {
        calFpGrad<19, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    case 20: {
        calFpGrad<20, WTYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut);
        return;
    }
    default: {
        return;
    }}
}
template <jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_FULL, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_NONE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_SINGLE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    default: {
        return;
    }}
}


template <jint NMAX, jint WTYPE>
static void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz, jint aTypeNum, jdouble aRCut) {
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fcMul = 1.0 - JSE_NNAP::pow2(dis/aRCut);
        jdouble fcMul3 = JSE_NNAP::pow3(fcMul);
        jdouble fc = fcMul3 * fcMul;
        jdouble fcPMul = 8.0 * fcMul3 / (aRCut*aRCut);
        jdouble fcPx = dx * fcPMul;
        jdouble fcPy = dy * fcPMul;
        jdouble fcPz = dz * fcPMul;
        // cal Rn
        jdouble *tRn = aNlRn + j*(NMAX+1);
        const jdouble tRnX = 1.0 - 2.0*dis/aRCut;
        JSE_NNAP::chebyshev2Full<NMAX-1>(tRnX, rCheby2);
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            // cal weight of type here
            jdouble wt = ((type&1)==1) ? type : -type;
            JSE_NNAP::calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, wt, dx, dy, dz);
        } else {
            JSE_NNAP::calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, 1.0, dx, dy, dz);
        }
        // cal fxyz
        jdouble tGradFc = 0.0;
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE ||
            WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FULL || aTypeNum==1) {
            jint tShiftFp;
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                tShiftFp = (NMAX+1)*(type-1);
            } else {
                tShiftFp = 0;
            }
            jdouble *tNNGrad = aNNGrad+tShiftFp;
            for (jint n = 0; n <= NMAX; ++n) {
                jdouble tGradRn = tNNGrad[n];
                jdouble tRnn = tRn[n];
                tGradFc += tRnn * tGradRn;
                tGradRn *= fc;
                rFx[j] += tGradRn*rRnPx[n];
                rFy[j] += tGradRn*rRnPy[n];
                rFz[j] += tGradRn*rRnPz[n];
            }
        } else {
            jdouble wt;
            jint tShiftFp;
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
                wt = 1.0;
                tShiftFp = (NMAX+1)*type;
            } else
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT) {
                wt = ((type&1)==1) ? type : -type;
                tShiftFp = NMAX+1;
            } else {
                continue;
            }
            jdouble *tNNGradWt = aNNGrad+tShiftFp;
            for (jint n = 0; n <= NMAX; ++n) {
                jdouble tGradRn = aNNGrad[n] + wt*tNNGradWt[n];
                jdouble tRnn = tRn[n];
                tGradFc += tRnn * tGradRn;
                tGradRn *= fc;
                rFx[j] += tGradRn*rRnPx[n];
                rFy[j] += tGradRn*rRnPy[n];
                rFz[j] += tGradRn*rRnPz[n];
            }
        }
        rFx[j] += fcPx*tGradFc;
        rFy[j] += fcPy*tGradFc;
        rFz[j] += fcPz*tGradFc;
    }
}
template <jint WTYPE>
static inline void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                    jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                                    jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz, jint aTypeNum, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFpAndForceDot<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 1: {
        calFpAndForceDot<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 2: {
        calFpAndForceDot<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 3: {
        calFpAndForceDot<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 4: {
        calFpAndForceDot<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 5: {
        calFpAndForceDot<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 6: {
        calFpAndForceDot<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 7: {
        calFpAndForceDot<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 8: {
        calFpAndForceDot<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 9: {
        calFpAndForceDot<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 10: {
        calFpAndForceDot<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 11: {
        calFpAndForceDot<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 12: {
        calFpAndForceDot<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 13: {
        calFpAndForceDot<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 14: {
        calFpAndForceDot<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 15: {
        calFpAndForceDot<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 16: {
        calFpAndForceDot<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 17: {
        calFpAndForceDot<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 18: {
        calFpAndForceDot<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 19: {
        calFpAndForceDot<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    case 20: {
        calFpAndForceDot<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut);
        return;
    }
    default: {
        return;
    }}
}
static inline void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                    jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                                    jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                                    jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calFpAndForceDot<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calFpAndForceDot<jsex_nnap_basis_Chebyshev_WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calFpAndForceDot<jsex_nnap_basis_Chebyshev_WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        calFpAndForceDot<jsex_nnap_basis_Chebyshev_WTYPE_SINGLE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calFpAndForceDot<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aTypeNum, aRCut, aNMax);
        return;
    }
    default: {
        return;
    }}
}



extern "C" {

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
    
    // do cal
    calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN,
          tNlRn, tFp+aShiftFp, tFpGradNlSize==NULL ? NULL : (tFpGradNlSize+aShiftFpGradNlSize),
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
    
    calFpGrad<JNI_TRUE>(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                        tNlRn, tRnPx, tRnPy, tRnPz, tCheby2,
                        tFpGradNlIndex+aShiftFpGradNlIndex, tFpGradFpIndex+aShiftFpGradFpIndex,
                        0, 0,
                        tFpPx+aShiftFpPx, tFpPy+aShiftFpPy, tFpPz+aShiftFpPz,
                        aTypeNum, aRCut, aNMax, aWType);
    
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
    
    calFpGrad<JNI_FALSE>(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                         tNlRn, tRnPx, tRnPy, tRnPz, tCheby2,
                         NULL, NULL,
                         aShiftFp, aRestFp,
                         tFpPx, tFpPy, tFpPz,
                         aTypeNum, aRCut, aNMax, aWType);
    
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
    
    calFpAndForceDot(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                     tNlRn, tRnPx, tRnPy, tRnPz, tCheby2,
                     tNNGrad+aShiftFp, tFx, tFy, tFz,
                     aTypeNum, aRCut, aNMax, aWType);
    
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

}
