#include "jsex_nnap_basis_Chebyshev.h"
#include "nnap_util.hpp"


template <jint NMAX, jint WTYPE, jboolean SINGLE_TYPE, jboolean NL_SIZE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jdouble aRCut) {
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
        if (SINGLE_TYPE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            for (jint n = 0; n <= NMAX; ++n) {
                rFp[n] += fc*tRn[n];
                if (NL_SIZE) ++rFpGradNlSize[n];
            }
            continue;
        }
        jdouble wt;
        jint tShiftFp;
        jdouble *tFpWt;
        switch(WTYPE) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            wt = 1.0;
            tShiftFp = (NMAX+1)*type;
            tFpWt = rFp+tShiftFp;
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            wt = 1.0;
            tShiftFp = (NMAX+1)*(type-1);
            tFpWt = rFp+tShiftFp;
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            // cal weight of type here
            wt = ((type&1)==1) ? type : -type;
            tShiftFp = NMAX+1;
            tFpWt = rFp+tShiftFp;
            break;
        }
        default: {
            continue;
        }}
        jint *tFpGradNlSizeWt;
        if (NL_SIZE) {
            tFpGradNlSizeWt = rFpGradNlSize+tShiftFp;
        }
        for (jint n = 0; n <= NMAX; ++n) {
            jdouble tFpn = fc*tRn[n];
            tFpWt[n] += wt*tFpn;
            if (NL_SIZE) ++tFpGradNlSizeWt[n];
            if (WTYPE != jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                rFp[n] += tFpn;
                if (NL_SIZE) ++rFpGradNlSize[n];
            }
        }
    }
}
template <jint WTYPE, jboolean SINGLE_TYPE, jboolean NL_SIZE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rNlRn, jdouble *rFp, jint *rFpGradNlSize,
                         jboolean aBufferNl, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFp<0, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 1: {
        calFp<1, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 2: {
        calFp<2, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 3: {
        calFp<3, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 4: {
        calFp<4, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 5: {
        calFp<5, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 6: {
        calFp<6, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 7: {
        calFp<7, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 8: {
        calFp<8, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 9: {
        calFp<9, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 10: {
        calFp<10, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 11: {
        calFp<11, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 12: {
        calFp<12, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 13: {
        calFp<13, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 14: {
        calFp<14, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 15: {
        calFp<15, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 16: {
        calFp<16, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 17: {
        calFp<17, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 18: {
        calFp<18, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 19: {
        calFp<19, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
        return;
    }
    case 20: {
        calFp<20, WTYPE, SINGLE_TYPE, NL_SIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut);
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
    if (aTypeNum == 1) {
        if (rFpGradNlSize == NULL) {
            calFp<WTYPE, JNI_TRUE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut, aNMax);
        } else {
            calFp<WTYPE, JNI_TRUE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut, aNMax);
        }
    } else {
        if (rFpGradNlSize == NULL) {
            calFp<WTYPE, JNI_FALSE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut, aNMax);
        } else {
            calFp<WTYPE, JNI_FALSE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rNlRn, rFp, rFpGradNlSize, aBufferNl, aRCut, aNMax);
        }
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


template <jint NMAX, jint WTYPE, jboolean SINGLE_TYPE, jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp, jint aSizeFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz, jdouble aRCut) {
        
    jint tShiftFpP = 0;
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jdouble *tFpPx_, *tFpPy_, *tFpPz_;
        jint *tFpGradNlIndex_, *tFpGradFpIndex_;
        if (!SPARSE) {
            // init fpPxyz
            tShiftFpP = j*(aShiftFp+aSizeFp+aRestFp) + aShiftFp;
            tFpPx_ = rFpPx + tShiftFpP;
            tFpPy_ = rFpPy + tShiftFpP;
            tFpPz_ = rFpPz + tShiftFpP;
            // always clear fpPxyz
            for (jint i = 0; i < aSizeFp; ++i) {
                tFpPx_[i] = 0.0;
                tFpPy_[i] = 0.0;
                tFpPz_[i] = 0.0;
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
            tFpGradNlIndex_ = rFpGradNlIndex + tShiftFpP;
            tFpGradFpIndex_ = rFpGradFpIndex + tShiftFpP;
            tFpPx_ = rFpPx + tShiftFpP;
            tFpPy_ = rFpPy + tShiftFpP;
            tFpPz_ = rFpPz + tShiftFpP;
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
        if (SINGLE_TYPE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            for (jint n = 0; n <= NMAX; ++n) {
                // cal subFpPxyz and set to fp
                const jdouble tRnn = tRn[n];
                tFpPx_[n] = (fc*rRnPx[n] + fcPx*tRnn);
                tFpPy_[n] = (fc*rRnPy[n] + fcPy*tRnn);
                tFpPz_[n] = (fc*rRnPz[n] + fcPz*tRnn);
                if (SPARSE) {
                    tFpGradNlIndex_[n] = j;
                    tFpGradFpIndex_[n] = n;
                }
            }
            if (SPARSE) {
                tShiftFpP += (NMAX+1);
            }
            continue;
        }
        jdouble wt;
        jint tShiftFp;
        switch(WTYPE) {
        case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
            wt = 1.0;
            tShiftFp = SPARSE ? (NMAX+1) : (NMAX+1)*type;
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
            wt = 1.0;
            tShiftFp = SPARSE ? 0 : (NMAX+1)*(type-1);
            break;
        }
        case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
            wt = ((type&1)==1) ? type : -type;
            tShiftFp = NMAX+1;
            break;
        }
        default: {
            continue;
        }}
        jdouble *tFpPxWt = tFpPx_+tShiftFp;
        jdouble *tFpPyWt = tFpPy_+tShiftFp;
        jdouble *tFpPzWt = tFpPz_+tShiftFp;
        jint *tFpGradNlIndexWt, *tFpGradFpIndexWt;
        if (SPARSE) {
            tFpGradNlIndexWt = tFpGradNlIndex_+tShiftFp;
            tFpGradFpIndexWt = tFpGradFpIndex_+tShiftFp;
        }
        for (jint n = 0; n <= NMAX; ++n) {
            // cal subFpPxyz first
            const jdouble tRnn = tRn[n];
            const jdouble subFpPx = fc*rRnPx[n] + fcPx*tRnn;
            const jdouble subFpPy = fc*rRnPy[n] + fcPy*tRnn;
            const jdouble subFpPz = fc*rRnPz[n] + fcPz*tRnn;
            // set to fp
            tFpPxWt[n] = wt*subFpPx;
            tFpPyWt[n] = wt*subFpPy;
            tFpPzWt[n] = wt*subFpPz;
            if (SPARSE) {
                tFpGradNlIndexWt[n] = j;
                if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
                    tFpGradFpIndexWt[n] = n + (NMAX+1)*type;
                } else {
                    tFpGradFpIndexWt[n] = n + (NMAX+1);
                }
            }
            if (WTYPE != jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                tFpPx_[n] = subFpPx;
                tFpPy_[n] = subFpPy;
                tFpPz_[n] = subFpPz;
                if (SPARSE) {
                    tFpGradNlIndex_[n] = j;
                    tFpGradFpIndex_[n] = n;
                }
            }
        }
        if (SPARSE) {
            if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                tShiftFpP += (NMAX+1);
            } else {
                tShiftFpP += (NMAX+NMAX+2);
            }
        }
    }
}
template <jint WTYPE, jboolean SINGLE_TYPE, jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp, jint aSizeFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFpGrad<0, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 1: {
        calFpGrad<1, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 2: {
        calFpGrad<2, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 3: {
        calFpGrad<3, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 4: {
        calFpGrad<4, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 5: {
        calFpGrad<5, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 6: {
        calFpGrad<6, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 7: {
        calFpGrad<7, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 8: {
        calFpGrad<8, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 9: {
        calFpGrad<9, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 10: {
        calFpGrad<10, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 11: {
        calFpGrad<11, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 12: {
        calFpGrad<12, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 13: {
        calFpGrad<13, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 14: {
        calFpGrad<14, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 15: {
        calFpGrad<15, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 16: {
        calFpGrad<16, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 17: {
        calFpGrad<17, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 18: {
        calFpGrad<18, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 19: {
        calFpGrad<19, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    case 20: {
        calFpGrad<20, WTYPE, SINGLE_TYPE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut);
        return;
    }
    default: {
        return;
    }}
}
template <jint WTYPE, jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp, jint aSizeFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz, jint aTypeNum, jdouble aRCut, jint aNMax) {
    if (aTypeNum == 1) {
        calFpGrad<WTYPE, JNI_TRUE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut, aNMax);
    } else {
        calFpGrad<WTYPE, JNI_FALSE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aRCut, aNMax);
    }
}
template <jboolean SPARSE>
static inline void calFpGrad(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jint *rFpGradNlIndex, jint *rFpGradFpIndex, jint aShiftFp, jint aRestFp, jint aSizeFp,
                             jdouble *rFpPx, jdouble *rFpPy, jdouble *rFpPz,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType) {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_FULL, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_NONE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_SINGLE: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_SINGLE, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calFpGrad<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT, SPARSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, rFpGradNlIndex, rFpGradFpIndex, aShiftFp, aRestFp, aSizeFp, rFpPx, rFpPy, rFpPz, aTypeNum, aRCut, aNMax);
        return;
    }
    default: {
        return;
    }}
}


template <jint NMAX, jint WTYPE, jboolean SINGLE_TYPE>
static inline void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                    jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                                    jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz, jdouble aRCut) {
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
        if (SINGLE_TYPE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE || WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_SINGLE) {
            for (jint n = 0; n <= NMAX; ++n) {
                jdouble tGradRn = aNNGrad[n];
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
            switch(WTYPE) {
            case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
                wt = 1.0;
                tShiftFp = (NMAX+1)*type;
                break;
            }
            case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
                wt = 1.0;
                tShiftFp = (NMAX+1)*(type-1);
                break;
            }
            case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
                wt = ((type&1)==1) ? type : -type;
                tShiftFp = NMAX+1;
                break;
            }
            default: {
                continue;
            }}
            jdouble *tNNGradWt = aNNGrad+tShiftFp;
            for (jint n = 0; n <= NMAX; ++n) {
                jdouble tGradRn;
                if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
                    tGradRn = wt*tNNGradWt[n];
                } else {
                    tGradRn = aNNGrad[n] + wt*tNNGradWt[n];
                }
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
template <jint WTYPE, jboolean SINGLE_TYPE>
static inline void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                    jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                                    jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz, jdouble aRCut, jint aNMax) {
    switch (aNMax) {
    case 0: {
        calFpAndForceDot<0, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 1: {
        calFpAndForceDot<1, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 2: {
        calFpAndForceDot<2, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 3: {
        calFpAndForceDot<3, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 4: {
        calFpAndForceDot<4, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 5: {
        calFpAndForceDot<5, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 6: {
        calFpAndForceDot<6, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 7: {
        calFpAndForceDot<7, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 8: {
        calFpAndForceDot<8, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 9: {
        calFpAndForceDot<9, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 10: {
        calFpAndForceDot<10, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 11: {
        calFpAndForceDot<11, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 12: {
        calFpAndForceDot<12, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 13: {
        calFpAndForceDot<13, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 14: {
        calFpAndForceDot<14, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 15: {
        calFpAndForceDot<15, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 16: {
        calFpAndForceDot<16, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 17: {
        calFpAndForceDot<17, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 18: {
        calFpAndForceDot<18, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 19: {
        calFpAndForceDot<19, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    case 20: {
        calFpAndForceDot<20, WTYPE, SINGLE_TYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut);
        return;
    }
    default: {
        return;
    }}
}
template <jint WTYPE>
static inline void calFpAndForceDot(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                                    jdouble *aNlRn, jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                                    jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                                    jint aTypeNum, jdouble aRCut, jint aNMax) {
    if (aTypeNum == 1) {
        calFpAndForceDot<WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut, aNMax);
    } else {
        calFpAndForceDot<WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNlRn, rRnPx, rRnPy, rRnPz, rCheby2, aNNGrad, rFx, rFy, rFz, aRCut, aNMax);
    }
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

#undef JSE_NNAP_CONSTANT_INIT_Chebyshev
#define JSE_NNAP_CONSTANT_INIT_Chebyshev                        \
jint tSize;                                                     \
switch(aWType) {                                                \
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
    
    // const init
    JSE_NNAP_CONSTANT_INIT_Chebyshev
    // clear fp first
    jdouble *tFp_ = tFp + aShiftFp;
    for (jint i = 0; i < tSize; ++i) {
        tFp_[i] = 0.0;
    }
    jint *tFpGradNlSize_ = tFpGradNlSize==NULL ? NULL : (tFpGradNlSize+aShiftFpGradNlSize);
    if (tFpGradNlSize_ != NULL) {
        for (jint i = 0; i < tSize; ++i) {
            tFpGradNlSize_[i] = 0;
        }
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
    
    calFpGrad<JNI_TRUE>(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                        tNlRn, tRnPx, tRnPy, tRnPz, tCheby2,
                        tFpGradNlIndex+aShiftFpGradNlIndex, tFpGradFpIndex+aShiftFpGradFpIndex,
                        0, 0, 0,
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
    
    // const init
    JSE_NNAP_CONSTANT_INIT_Chebyshev
    
    calFpGrad<JNI_FALSE>(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                         tNlRn, tRnPx, tRnPy, tRnPz, tCheby2,
                         NULL, NULL,
                         aShiftFp, aRestFp, tSize,
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
