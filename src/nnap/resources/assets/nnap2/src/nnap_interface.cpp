#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "basis_SphericalChebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGENX_FP_NTYPES 2
#define NNAPGENX_FP_WTYPE JSE_NNAP::WTYPE_DEFAULT
#define NNAPGENX_FP_NMAX 5
#define NNAPGENX_FP_LMAX 6
#define NNAPGENX_FP_NORADIAL 0
#define NNAPGENX_FP_L3MAX 0
#define NNAPGENX_FP_L4MAX 0
#define NNAPGENX_FP_FSIZE 0
#define NNAPGENX_FP_FSTYLE JSE_NNAP::FSTYLE_LIMITED
#define NNAPGENX_FP_PFFLAG 0
#define NNAPGENX_FP_PFSIZE 0
#define NNAPGENX_FP_SIZE 84
#define NNAPGENX_FP_SIZE_FW 0
#define NNAPGENX_NN_SIZE_IN 84
#define NNAPGENX_NN_SIZE_HW (84*32)
#define NNAPGENX_NN_SIZE_HB 32
#define NNAPGENX_NN_SIZE_OW 32
#define NNAPGENS_aCType 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int FP_FULL_CACHE, int NN_CACHE_GRAD>
static int forward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int aCType,
                   flt_t *aFpHyperParam, flt_t *aFpParam, flt_t *aNnParam, flt_t *aNormParam,
                   flt_t *rFpForwardCache, flt_t *rNnForwardCache,
                   flt_t *rOutEng) noexcept {
    flt_t tNormMuEng = aNormParam[0];
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    flt_t *rFp = rNnForwardCache; // fp from nn cache, for smooth input
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphForward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NTYPES, NNAPGENX_FP_NMAX, NNAPGENX_FP_LMAX, NNAPGENX_FP_NORADIAL, NNAPGENX_FP_L3MAX, NNAPGENX_FP_L4MAX,
               NNAPGENX_FP_FSIZE, NNAPGENX_FP_PFFLAG, NNAPGENX_FP_PFSIZE, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rFp,
        rFpForwardCache, aFpHyperParam[0], aFpParam,
        aFpParam+NNAPGENX_FP_SIZE_FW, aFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyForward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NTYPES, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rFp,
        rFpForwardCache, aFpHyperParam[0], aFpParam
    );
// <<< NNAPGEN PICK [FP USE NNAPGENS_aCType]
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;
    
// >>> NNAPGEN SWITCH
    flt_t *tHiddenWeights = aNnParam;
    flt_t *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    flt_t *tHiddenBiases = tOutputWeights + NNAPGENX_NN_SIZE_OW;
    flt_t tOutputBias = tHiddenBiases[NNAPGENX_NN_SIZE_HB];
    flt_t *rLayers = rNnForwardCache; // first layer is fp
    flt_t *rSiLUGrad = NN_CACHE_GRAD ? (rLayers+(NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB)) : NULL;
    flt_t tEng = nnForward<NNAPGENS_aCType, NN_CACHE_GRAD>(rLayers, tHiddenWeights, tHiddenBiases, tOutputWeights, tOutputBias, rSiLUGrad);
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOutEng = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;
    
    return 0;
}


template <int FP_FULL_CACHE, int CLEAR_CACHE>
static int backward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int aCType,
                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                    flt_t *aFpHyperParam, flt_t *aFpParam, flt_t *aNnParam, flt_t *aNormParam,
                    flt_t *aFpForwardCache, flt_t *aNnForwardCache, flt_t *rFpBackwardCache, flt_t *rNnBackwardCache,
                    flt_t aInGradEng) noexcept {
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    flt_t *tHiddenWeights = aNnParam;
    flt_t *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    flt_t *rGradLayers = rNnBackwardCache; // first layer is fp
    flt_t *tSiLUGrad = aNnForwardCache + (NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB);
    nnBackward<NNAPGENS_aCType, CLEAR_CACHE>(aInGradEng, rGradLayers, tHiddenWeights, tOutputWeights, tSiLUGrad);
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;

// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    flt_t *tGradFp = rNnBackwardCache; // fp from nn cache, for smooth input
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        tGradFp[i] /= tNormSigma[i];
    }
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphBackward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NTYPES, NNAPGENX_FP_NMAX, NNAPGENX_FP_LMAX, NNAPGENX_FP_NORADIAL, NNAPGENX_FP_L3MAX, NNAPGENX_FP_L4MAX,
                NNAPGENX_FP_FSIZE, NNAPGENX_FP_PFFLAG, NNAPGENX_FP_PFSIZE, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpForwardCache, rFpBackwardCache, aFpHyperParam[0], aFpParam,
        aFpParam+NNAPGENX_FP_SIZE_FW, aFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyBackward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NTYPES, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpForwardCache, rFpBackwardCache, aFpHyperParam[0], aFpParam
    );
// <<< NNAPGEN PICK [FP USE NNAPGENS_aCType]
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;

    return 0;
}

}


extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergy(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t *tFpHyperParam = (JSE_NNAP::flt_t *)tDataIn[5];
    JSE_NNAP::flt_t *tFpParam = (JSE_NNAP::flt_t *)tDataIn[6];
    JSE_NNAP::flt_t *tNnParam = (JSE_NNAP::flt_t *)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rNnForwardCache = (JSE_NNAP::flt_t *)tDataOut[2];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    return JSE_NNAP::forward<JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpHyperParam, tFpParam, tNnParam, tNormParam,
        rFpForwardCache, rNnForwardCache,
        rOutEng
    );
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t *tFpHyperParam = (JSE_NNAP::flt_t *)tDataIn[5];
    JSE_NNAP::flt_t *tFpParam = (JSE_NNAP::flt_t *)tDataIn[6];
    JSE_NNAP::flt_t *tNnParam = (JSE_NNAP::flt_t *)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *tGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *tGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *tGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    JSE_NNAP::flt_t *rNnForwardCache = (JSE_NNAP::flt_t *)tDataOut[5];
    JSE_NNAP::flt_t *rFpBackwardCache = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t *rNnBackwardCache = (JSE_NNAP::flt_t *)tDataOut[7];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpHyperParam, tFpParam, tNnParam, tNormParam,
        rFpForwardCache, rNnForwardCache,
        rOutEng
    );
    if (code!=0) return code;
    
    // manual clear required for backward in force
    return JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tGradNlDx, tGradNlDy, tGradNlDz,
        tFpHyperParam, tFpParam, tNnParam, tNormParam,
        rFpForwardCache, rNnForwardCache, rFpBackwardCache, rNnBackwardCache,
        1.0
    );
}

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    int *tNlIdx = (int *)tDataIn[5];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[8];
    JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[9];
    
    JSE_NNAP::flt_t *tGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *tGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *tGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    JSE_NNAP::flt_t **rNnForwardCache = (JSE_NNAP::flt_t **)tDataOut[5];
    JSE_NNAP::flt_t *rFpBackwardCache = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t **rNnBackwardCache = (JSE_NNAP::flt_t **)tDataOut[7];
    
    int numneighMax = tNums[0];
    int inum = tNums[1];
    int eflag = tNums[2];
    int vflag = tNums[3];
    int eflagAtom = tNums[4];
    int vflagAtom = tNums[5];
    int cvflagAtom = tNums[6];
    
    double **x = (double **)tDataIn[10];
    double **f = (double **)tDataOut[0];
    int *type = (int *)tDataIn[11];
    
    int *ilist = (int *)tDataIn[12];
    int *numneigh = (int *)tDataIn[13];
    int **firstneigh = (int **)tDataIn[14];
    double *cutsq = (double *)tDataIn[15];
    int *tLmpType2NNAPType = (int *)tDataIn[16];
    
    double *engVdwl = (double *)tDataOut[8];
    double *eatom = (double *)tDataOut[9];
    double *virial = (double *)tDataOut[10];
    double **vatom = (double **)tDataOut[11];
    double **cvatom = (double **)tDataOut[12];
    
    /// begin compute here
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        double xtmp = x[i][0];
        double ytmp = x[i][1];
        double ztmp = x[i][2];
        int typei = type[i];
        int *jlist = firstneigh[i];
        int jnum = numneigh[i];
        if (jnum > numneighMax) return -jnum;
        
        /// build neighbor list
        int tNeiNum = 0;
        for (int jj = 0; jj < jnum; ++jj) {
            int j = jlist[jj];
            j &= JSE_LMP_NEIGHMASK;
            // Note that dxyz in jse and lammps are defined oppositely
            double delx = x[j][0] - xtmp;
            double dely = x[j][1] - ytmp;
            double delz = x[j][2] - ztmp;
            double rsq = delx*delx + dely*dely + delz*delz;
            if (rsq < cutsq[typei]) {
                tNlDx[tNeiNum] = (JSE_NNAP::flt_t)delx;
                tNlDy[tNeiNum] = (JSE_NNAP::flt_t)dely;
                tNlDz[tNeiNum] = (JSE_NNAP::flt_t)delz;
                tNlType[tNeiNum] = tLmpType2NNAPType[type[j]];
                tNlIdx[tNeiNum] = j;
                ++tNeiNum;
            }
        }
        int typeiNNAP = tLmpType2NNAPType[typei];
        
        /// begin nnap here
        JSE_NNAP::flt_t rEng;
        int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
            tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP,
            tFpHyperParam[typeiNNAP-1], tFpParam[typeiNNAP-1], tNnParam[typeiNNAP-1], tNormParam[typeiNNAP-1],
            rFpForwardCache, rNnForwardCache[typeiNNAP-1],
            &rEng
        );
        if (code!=0) return code;
        // manual clear required for backward in force
        code = JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
            tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP,
            tGradNlDx, tGradNlDy, tGradNlDz,
            tFpHyperParam[typeiNNAP-1], tFpParam[typeiNNAP-1], tNnParam[typeiNNAP-1], tNormParam[typeiNNAP-1],
            rFpForwardCache, rNnForwardCache[typeiNNAP-1], rFpBackwardCache, rNnBackwardCache[typeiNNAP-1],
            1.0
        );
        if (code!=0) return code;
        
        /// collect results
        if (eflag) {
            *engVdwl += rEng;
            if (eflagAtom) eatom[i] += rEng;
        }
        for (int jj = 0; jj < tNeiNum; ++jj) {
            int j = tNlIdx[jj];
            const JSE_NNAP::flt_t fx = tGradNlDx[jj];
            const JSE_NNAP::flt_t fy = tGradNlDy[jj];
            const JSE_NNAP::flt_t fz = tGradNlDz[jj];
            f[i][0] -= fx;
            f[i][1] -= fy;
            f[i][2] -= fz;
            f[j][0] += fx;
            f[j][1] += fy;
            f[j][2] += fz;
            if (vflag) {
                const JSE_NNAP::flt_t dx = tNlDx[jj];
                const JSE_NNAP::flt_t dy = tNlDy[jj];
                const JSE_NNAP::flt_t dz = tNlDz[jj];
                virial[0] += dx*fx;
                virial[1] += dy*fy;
                virial[2] += dz*fz;
                virial[3] += dx*fy;
                virial[4] += dx*fz;
                virial[5] += dy*fz;
                if (vflagAtom) {
                    vatom[j][0] += dx*fx;
                    vatom[j][1] += dy*fy;
                    vatom[j][2] += dz*fz;
                    vatom[j][3] += dx*fy;
                    vatom[j][4] += dx*fz;
                    vatom[j][5] += dy*fz;
                }
                if (cvflagAtom) {
                    cvatom[j][0] += dx*fx;
                    cvatom[j][1] += dy*fy;
                    cvatom[j][2] += dz*fz;
                    cvatom[j][3] += dx*fy;
                    cvatom[j][4] += dx*fz;
                    cvatom[j][5] += dy*fz;
                    cvatom[j][6] += dy*fx;
                    cvatom[j][7] += dz*fx;
                    cvatom[j][8] += dz*fy;
                }
            }
        }
    }
    return 0;
}

}


