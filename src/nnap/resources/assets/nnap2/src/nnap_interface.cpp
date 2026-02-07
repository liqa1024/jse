#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "basis_SphericalChebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENX_FP_SIZE__ 84
#define __NNAPGENXX_FP_WTYPE__ JSE_NNAP::WTYPE_DEFAULT
#define __NNAPGENXX_FP_SIZE_N__ 12
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
#define __NNAPGENX_FP_SIZE_CACHEF__ 294
#define __NNAPGENX_FP_SIZE_CACHEB__ 1
#define __NNAPGENS_CTYPE_GEN__ 1
#define __NNAPGENS_ctype__ 1
#define __NNAPGENS_typeiNNAP__ 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int CTYPE_GEN>
static int fpForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int cType, flt_t *rFp,
                     flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *rFpForwardCache) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *rSubFp = rFp;
    flt_t *rSubFpForwardCache = rFpForwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[cType-1];
    flt_t *tSubFpParam = aFpParam[cType-1];
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
               __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_SIZE_N__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        &rSubFpForwardCache, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, __NNAPGENXX_FP_SIZE_N__>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    rSubFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [FP TYPE]
    if (flag) return 1;
    return 0;
}
template <int CTYPE_GEN, int NN_CACHE_GRAD>
static int normedNnForward(int cType, flt_t *rFp, flt_t *aNormParam, flt_t **aNnParam, flt_t *rNnGradCache, flt_t *rOutEng) noexcept {
    flt_t tNormMuEng = aNormParam[0];
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
    
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + __NNAPGENX_NN_SIZE_IN__;
    // norm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flt_t *tHiddenWeights = aNnParam[cType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
    flt_t *tHiddenBiases = tOutputWeights + __NNAPGENX_NN_SIZE_OW__;
    flt_t tOutputBias = tHiddenBiases[__NNAPGENX_NN_SIZE_HB__];
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    flt_t tEng = nnForward<__NNAPGENS_CTYPE_GEN__, NN_CACHE_GRAD>(rFp, tHiddenWeights, NULL, tHiddenBiases, NULL, tOutputWeights, tOutputBias, rNnGradCache);
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    flt_t *tSharedHiddenBiases = tSharedHiddenWeights + (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_SHW__ + __NNAPGENX_NN_SIZE_OW__);
    flt_t tEng = nnForward<__NNAPGENS_CTYPE_GEN__, NN_CACHE_GRAD>(rFp, tHiddenWeights, tSharedHiddenWeights, tHiddenBiases, tSharedHiddenBiases, tOutputWeights, tOutputBias, rNnGradCache);
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOutEng = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN>
static int fpBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int cType, flt_t *aGradFp,
                      flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                      flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *aFpForwardCache, flt_t *rFpBackwardCache) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tSubGradFp = aGradFp;
    flt_t *aSubFpForwardCache = aFpForwardCache;
    flt_t *rSubFpBackwardCache = rFpBackwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[cType-1];
    flt_t *tSubFpParam = aFpParam[cType-1];
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
                __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_SIZE_N__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, __NNAPGENXX_FP_SIZE_N__>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    tSubGradFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [FP TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN>
static int normedNnBackward(int cType, flt_t *rGradFp, flt_t *aNormParam, flt_t **aNnParam, flt_t *aNnGradCache, flt_t aInGradEng) noexcept {
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    flt_t *tHiddenWeights = aNnParam[cType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    nnBackward<__NNAPGENS_CTYPE_GEN__>(aInGradEng, rGradFp, tHiddenWeights, NULL, tOutputWeights, aNnGradCache);
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    nnBackward<__NNAPGENS_CTYPE_GEN__>(aInGradEng, rGradFp, tHiddenWeights, tSharedHiddenWeights, tOutputWeights, aNnGradCache);
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    flt_t *tNormSigma = aNormParam + (2+__NNAPGENX_NN_SIZE_IN__);
    // denorm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rGradFp[i] /= tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
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
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rFp[__NNAPGENX_FP_SIZE__];
    JSE_NNAP::flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFp,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        ctype, rFp, tNormParam, tNnParam, NULL, rOutEng
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
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
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    // manual clear required for backward in force
    for (int j = 0; j < tNeiNum; ++j) {
        tGradNlDx[j] = JSE_NNAP::ZERO;
        tGradNlDy[j] = JSE_NNAP::ZERO;
        tGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rFp[__NNAPGENX_FP_SIZE__];
    JSE_NNAP::flt_t rGradFp[__NNAPGENX_FP_SIZE__] = {};
    JSE_NNAP::flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFp,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHE__];
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        ctype, rFp, tNormParam, tNnParam, rNnGradCache, rOutEng
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__>(
        ctype, rGradFp, tNormParam, tNnParam, rNnGradCache, JSE_NNAP::ONE
    );
    if (code!=0) return code;
    JSE_NNAP::flt_t rFpBackwardCache[__NNAPGENX_FP_SIZE_CACHEB__] = {};
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rGradFp,
        tGradNlDx, tGradNlDy, tGradNlDz,
        tFpHyperParam, tFpParam, rFpForwardCache, rFpBackwardCache
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_statNeiNumLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    int *tDataOut = (int *)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    int *ilist = (int *)tDataIn[1];
    int *numneigh = (int *)tDataIn[2];
    
    int inum = tNums[0];
    int numneighMax = 0;
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int jnum = numneigh[i];
        if (jnum > numneighMax) numneighMax = jnum;
    }
    tDataOut[0] = numneighMax;
    
    return 0;
}

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
    
    int inum = tNums[0];
    int ntypes = tNums[1];
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
    int **tTypeIlist = (int **)tDataIn[17];
    int *tTypeInum = (int *)tDataIn[18];
    
    double *engVdwl = (double *)tDataOut[4];
    double *eatom = (double *)tDataOut[5];
    double *virial = (double *)tDataOut[6];
    double **vatom = (double **)tDataOut[7];
    double **cvatom = (double **)tDataOut[8];
    
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
        
        for (int ii = 0; ii < subInum; ++ii) {
            int i = subIlist[ii];
            double xtmp = x[i][0];
            double ytmp = x[i][1];
            double ztmp = x[i][2];
            int *jlist = firstneigh[i];
            int jnum = numneigh[i];
            
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
            // manual clear required for backward in force
            for (int j = 0; j < tNeiNum; ++j) {
                tGradNlDx[j] = JSE_NNAP::ZERO;
                tGradNlDy[j] = JSE_NNAP::ZERO;
                tGradNlDz[j] = JSE_NNAP::ZERO;
            }
            int code;
// >>> NNAPGEN SWITCH
            JSE_NNAP::flt_t rFp[__NNAPGENX_FP_SIZE__];
            JSE_NNAP::flt_t rGradFp[__NNAPGENX_FP_SIZE__] = {};
            JSE_NNAP::flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
            code = JSE_NNAP::fpForward<__NNAPGENS_typeiNNAP__>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFp,
                tFpHyperParam, tFpParam, rFpForwardCache
            );
            if (code!=0) return code;
            JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHE__];
            code = JSE_NNAP::normedNnForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
                typeiNNAP, rFp, subNormParam, tNnParam, rNnGradCache, &rEng
            );
            if (code!=0) return code;
            code = JSE_NNAP::normedNnBackward<__NNAPGENS_typeiNNAP__>(
                typeiNNAP, rGradFp, subNormParam, tNnParam, rNnGradCache, JSE_NNAP::ONE
            );
            if (code!=0) return code;
            JSE_NNAP::flt_t rFpBackwardCache[__NNAPGENX_FP_SIZE_CACHEB__] = {};
            code = JSE_NNAP::fpBackward<__NNAPGENS_typeiNNAP__>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rGradFp,
                tGradNlDx, tGradNlDy, tGradNlDz,
                tFpHyperParam, tFpParam, rFpForwardCache, rFpBackwardCache
            );
            if (code!=0) return code;
// <<< NNAPGEN SWITCH (typeiNNAP) [FP NN TYPE]
            
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

