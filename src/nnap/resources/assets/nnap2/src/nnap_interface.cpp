#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "basis_SphericalChebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENX_FP_SIZE__ 84
#define __NNAPGENXX_FP_NTYPES__ 2
#define __NNAPGENXX_FP_WTYPE__ JSE_NNAP::WTYPE_DEFAULT
#define __NNAPGENXX_FP_NMAX__ 5
#define __NNAPGENXX_FP_LMAX__ 6
#define __NNAPGENXX_FP_NORADIAL__ 0
#define __NNAPGENXX_FP_L3MAX__ 0
#define __NNAPGENXX_FP_L4MAX__ 0
#define __NNAPGENXX_FP_FSIZE__ 0
#define __NNAPGENXX_FP_FSTYLE__ JSE_NNAP::FSTYLE_LIMITED
#define __NNAPGENXX_FP_PFFLAG__ 0
#define __NNAPGENXX_FP_PFSIZE__ 0
#define __NNAPGENXX_FP_SIZE__ 84
#define __NNAPGENXX_FP_SIZE_FW__ 0
#define __NNAPGENXX_FP_SIZE_HPARAM__ 2
#define __NNAPGENXX_FP_SIZE_PARAM__ 0
#define __NNAPGENX_FP_SHARED_TYPE__ 1
#define __NNAPGENS_aCType__ 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int FP_FULL_CACHE, int NN_CACHE_GRAD>
static int forward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int aCType,
                   flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNnParam, flt_t *aNormParam,
                   flt_t *rFpForwardCache, flt_t *rNnForwardCache,
                   flt_t *rOutEng) noexcept {
    flt_t tNormMuEng = aNormParam[0];
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + __NNAPGENX_FP_SIZE__;
    flt_t *rFp = rNnForwardCache; // fp from nn cache, for smooth input
    flt_t *rSubFp = rFp;
    flt_t *rSubFpForwardCache = rFpForwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[aCType-1];
    flt_t *tSubFpParam = aFpParam[aCType-1];
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NTYPES__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
               __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        &rSubFpForwardCache, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NTYPES__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        &rSubFpForwardCache, tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    rSubFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    // norm fp here
    for (int i = 0; i < __NNAPGENX_FP_SIZE__; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;
    
// >>> NNAPGEN SWITCH
    flt_t *tHiddenWeights = aNnParam[aCType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
    flt_t *tHiddenBiases = tOutputWeights + __NNAPGENX_NN_SIZE_OW__;
    flt_t tOutputBias = tHiddenBiases[__NNAPGENX_NN_SIZE_HB__];
    flt_t *rLayers = rNnForwardCache; // first layer is fp
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    flt_t *rSiLUGrad = NN_CACHE_GRAD ? (rLayers+(__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__)) : NULL;
    flt_t tEng = nnForward<__NNAPGENS_aCType__, NN_CACHE_GRAD>(rLayers, tHiddenWeights, NULL, tHiddenBiases, NULL, tOutputWeights, tOutputBias, rSiLUGrad);
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    flt_t *tSharedHiddenBiases = tSharedHiddenWeights + (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_SHW__ + __NNAPGENX_NN_SIZE_OW__);
    flt_t *rSiLUGrad = NN_CACHE_GRAD ? (rLayers+(__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__+__NNAPGENX_NN_SIZE_SHB__)) : NULL;
    flt_t tEng = nnForward<__NNAPGENS_aCType__, NN_CACHE_GRAD>(rLayers, tHiddenWeights, tSharedHiddenWeights, tHiddenBiases, tSharedHiddenBiases, tOutputWeights, tOutputBias, rSiLUGrad);
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
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
                    flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNnParam, flt_t *aNormParam,
                    flt_t *aFpForwardCache, flt_t *aNnForwardCache, flt_t *rFpBackwardCache, flt_t *rNnBackwardCache,
                    flt_t aInGradEng) noexcept {
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    flt_t *tHiddenWeights = aNnParam[aCType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
    flt_t *rGradLayers = rNnBackwardCache; // first layer is fp
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    flt_t *tSiLUGrad = aNnForwardCache + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    nnBackward<__NNAPGENS_aCType__, CLEAR_CACHE>(aInGradEng, rGradLayers, tHiddenWeights, NULL, tOutputWeights, tSiLUGrad);
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    flt_t *tSiLUGrad = aNnForwardCache + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__+__NNAPGENX_NN_SIZE_SHB__);
    nnBackward<__NNAPGENS_aCType__, CLEAR_CACHE>(aInGradEng, rGradLayers, tHiddenWeights, tSharedHiddenWeights, tOutputWeights, tSiLUGrad);
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;
    
    // clear gradNlDxyz here
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNeiNum; ++j) {
            rGradNlDx[j] = ZERO;
            rGradNlDy[j] = ZERO;
            rGradNlDz[j] = ZERO;
        }
    }
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + __NNAPGENX_FP_SIZE__;
    flt_t *tGradFp = rNnBackwardCache; // fp from nn cache, for smooth input
    // norm fp here
    for (int i = 0; i < __NNAPGENX_FP_SIZE__; ++i) {
        tGradFp[i] /= tNormSigma[i];
    }
    flt_t *tSubGradFp = tGradFp;
    flt_t *aSubFpForwardCache = aFpForwardCache;
    flt_t *rSubFpBackwardCache = rFpBackwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[aCType-1];
    flt_t *tSubFpParam = aFpParam[aCType-1];
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NTYPES__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
                __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, &rSubFpBackwardCache, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NTYPES__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, &rSubFpBackwardCache, tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    tSubGradFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
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
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
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
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
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
    int ntypes = tNums[2];
    int eflag = tNums[3];
    int vflag = tNums[4];
    int eflagAtom = tNums[5];
    int vflagAtom = tNums[6];
    int cvflagAtom = tNums[7];
    
    double **x = (double **)tDataIn[10];
    double **f = (double **)tDataOut[0];
    int *type = (int *)tDataIn[11];
    
    int *ilist = (int *)tDataIn[12];
    int *numneigh = (int *)tDataIn[13];
    int **firstneigh = (int **)tDataIn[14];
    double *cutsq = (double *)tDataIn[15];
    int *tLmpType2NNAPType = (int *)tDataIn[16];
    int **tTypeIlist = (int **)tDataIn[17];
    int *tTypeInum = (int *)tDataIn[18];
    
    double *engVdwl = (double *)tDataOut[8];
    double *eatom = (double *)tDataOut[9];
    double *virial = (double *)tDataOut[10];
    double **vatom = (double **)tDataOut[11];
    double **cvatom = (double **)tDataOut[12];
    
    /// reorder by types
    for (int typei = 1; typei <= ntypes; ++typei) {
        tTypeInum[typei] = 0;
    }
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int typei = type[i];
        tTypeIlist[typei][tTypeInum[typei]] = i;
        ++tTypeInum[typei];
    }
    
    /// begin compute here
    for (int typei = 1; typei <= ntypes; ++typei) {
        int *subIlist = tTypeIlist[typei];
        int subInum = tTypeInum[typei];
        
        const int typeiNNAP = tLmpType2NNAPType[typei];
        JSE_NNAP::flt_t *subNormParam = tNormParam[typeiNNAP-1];
        JSE_NNAP::flt_t *subNnForwardCache = rNnForwardCache[typeiNNAP-1];
        JSE_NNAP::flt_t *subNnBackwardCache = rNnBackwardCache[typeiNNAP-1];
        
        for (int ii = 0; ii < subInum; ++ii) {
            int i = subIlist[ii];
            double xtmp = x[i][0];
            double ytmp = x[i][1];
            double ztmp = x[i][2];
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
            
            /// begin nnap here
            JSE_NNAP::flt_t rEng;
            int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP,
                tFpHyperParam, tFpParam, tNnParam, subNormParam,
                rFpForwardCache, subNnForwardCache,
                &rEng
            );
            if (code!=0) return code;
            // manual clear required for backward in force
            code = JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP,
                tGradNlDx, tGradNlDy, tGradNlDz,
                tFpHyperParam, tFpParam, tNnParam, subNormParam,
                rFpForwardCache, subNnForwardCache, rFpBackwardCache, subNnBackwardCache,
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
    }
    return 0;
}

}


