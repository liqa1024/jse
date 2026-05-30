#include "nnap_main.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_typeiNNAP__ 1
// <<< NNAPGEN REMOVE

#define __jsefunc__

extern "C" {

__jsefunc__ int jse_nnap_calFp(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t *rFp) {
    
    int code;
    // >>> NNAPGEN SWITCH
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rFp,
        aFpHyperParam, aFpParam, NULL
    );
    if (code!=0) return code;
    // <<< NNAPGEN SWITCH (ctype) [FP TYPE]
    return 0;
}

__jsefunc__ int jse_nnap_calEnergy(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t *rOutEng) {
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rLayers,
        aFpHyperParam, aFpParam, NULL
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        ctype, rOutEng, rLayers,
        aNormParam, aNnParam, NULL, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}

__jsefunc__ int jse_nnap_calEnergyForce(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t *rOutEng, JSE_NNAP::flt_t *rGradNlDx, JSE_NNAP::flt_t *rGradNlDy, JSE_NNAP::flt_t *rGradNlDz,
    JSE_NNAP::flt_t *rFpForwardCache) {
    
    // manual clear required for backward in force
    for (int j = 0; j < aNeiNum; ++j) {
        rGradNlDx[j] = JSE_NNAP::ZERO;
        rGradNlDy[j] = JSE_NNAP::ZERO;
        rGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rLayers,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, rOutEng, rLayers,
        aNormParam, aNnParam, rNnGradCache, NULL
    );
    if (code!=0) return code;
    // manual clear required for backward in force
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rLayers, JSE_NNAP::ZERO);
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        ctype, JSE_NNAP::ONE, NULL, rLayers, NULL,
        aNormParam, aNnParam, NULL, rNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rLayers,
        rGradNlDx, rGradNlDy, rGradNlDz, aFpHyperParam,
        aFpParam, NULL, rFpForwardCache, NULL, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}


__jsefunc__ int jse_nnap_forwardEnergy(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t *rOutEng, JSE_NNAP::flt_t *rFpForwardCache, JSE_NNAP::flt_t *rNnForwardCache) {
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *rLayers = rNnForwardCache;
    JSE_NNAP::flt_t *rNnGradCache = rLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rLayers,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, rOutEng, rLayers,
        aNormParam, aNnParam, rNnGradCache, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}
__jsefunc__ int jse_nnap_backwardEnergy(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t aGradEng, JSE_NNAP::flt_t **rGradFpParam, JSE_NNAP::flt_t **rGradNnParam,
    JSE_NNAP::flt_t *aFpForwardCache, JSE_NNAP::flt_t *aNnForwardCache) {
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *tLayers = aNnForwardCache;
    JSE_NNAP::flt_t *tNnGradCache = tLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t rGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, aGradEng, tLayers, rGradLayers, NULL,
        aNormParam, aNnParam, rGradNnParam, tNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rGradLayers,
        NULL, NULL, NULL, aFpHyperParam,
        aFpParam, rGradFpParam, aFpForwardCache, NULL, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}

__jsefunc__ int jse_nnap_forwardEnergyForce(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t *rOutEng, JSE_NNAP::flt_t *rGradNlDx, JSE_NNAP::flt_t *rGradNlDy, JSE_NNAP::flt_t *rGradNlDz,
    JSE_NNAP::flt_t *rFpForwardCache, JSE_NNAP::flt_t *rNnForwardCache, JSE_NNAP::flt_t *rFpBackwardCache, JSE_NNAP::flt_t *rNnBackwardCache) {
    
    // manual clear required for backward in force
    for (int j = 0; j < aNeiNum; ++j) {
        rGradNlDx[j] = JSE_NNAP::ZERO;
        rGradNlDy[j] = JSE_NNAP::ZERO;
        rGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *rLayers = rNnForwardCache;
    JSE_NNAP::flt_t *rNnGradCache = rLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t *rNnGradGradCache = rNnBackwardCache;
    JSE_NNAP::flt_t *rAGradLayers = rNnGradGradCache + __NNAPGENX_NN_SIZE_HB__;
    JSE_NNAP::flt_t *rAGradLayersZ = rAGradLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    // manual clear required for backward in force
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rAGradLayers, JSE_NNAP::ZERO);
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_HB__>(rAGradLayersZ, JSE_NNAP::ZERO);
    
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rLayers,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        ctype, rOutEng, rLayers,
        aNormParam, aNnParam, rNnGradCache, rNnGradGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        ctype, JSE_NNAP::ONE, NULL, rAGradLayers, rAGradLayersZ,
        aNormParam, aNnParam, NULL, rNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rAGradLayers,
        rGradNlDx, rGradNlDy, rGradNlDz, aFpHyperParam,
        aFpParam, NULL, rFpForwardCache, rFpBackwardCache, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}
__jsefunc__ int jse_nnap_backwardEnergyForce(
    JSE_NNAP::flt_t *aNlDx, JSE_NNAP::flt_t *aNlDy, JSE_NNAP::flt_t *aNlDz, int *aNlType, int aNeiNum, int ctype,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t *aNormParam,
    JSE_NNAP::flt_t aBGradEng, JSE_NNAP::flt_t *aBGradAGradNlDx, JSE_NNAP::flt_t *aBGradAGradNlDy, JSE_NNAP::flt_t *aBGradAGradNlDz,
    JSE_NNAP::flt_t **rBGradFpParam, JSE_NNAP::flt_t **rBGradNnParam,
    JSE_NNAP::flt_t *aFpForwardCache, JSE_NNAP::flt_t *aNnForwardCache, JSE_NNAP::flt_t *aFpBackwardCache, JSE_NNAP::flt_t *aNnBackwardCache,
    JSE_NNAP::flt_t *rFpBackwardBackwardCache) {
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *tLayers = aNnForwardCache;
    JSE_NNAP::flt_t *tNnGradCache = tLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t *tNnGradGradCache = aNnBackwardCache;
    JSE_NNAP::flt_t *tAGradLayers = tNnGradGradCache + __NNAPGENX_NN_SIZE_HB__;
    JSE_NNAP::flt_t *tAGradLayersZ = tAGradLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t rBGradAGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    JSE_NNAP::flt_t rBGradLayersZ[__NNAPGENX_NN_SIZE_HB__] = {0};
    JSE_NNAP::flt_t rBGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    
    code = JSE_NNAP::fpBackwardBackward<__NNAPGENS_ctype__>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, tAGradLayers, rBGradAGradLayers,
        aBGradAGradNlDx, aBGradAGradNlDy, aBGradAGradNlDz,
        aFpHyperParam, aFpParam, rBGradFpParam, aFpForwardCache, aFpBackwardCache, rFpBackwardBackwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackwardBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        ctype, JSE_NNAP::ONE, NULL, tAGradLayers, rBGradAGradLayers, tAGradLayersZ, rBGradLayersZ,
        aNormParam, aNnParam, rBGradNnParam, tNnGradCache, tNnGradGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        ctype, aBGradEng, tLayers, rBGradLayers, rBGradLayersZ,
        aNormParam, aNnParam, rBGradNnParam, tNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, ctype, rBGradLayers,
        NULL, NULL, NULL, aFpHyperParam,
        aFpParam, rBGradFpParam, aFpForwardCache, NULL, rFpBackwardBackwardCache
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}


#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

__jsefunc__ int jse_nnap_statNeiNumLammps(int *ilist, int *numneigh, int inum, int *numneighMax) {
    int numneighMax_ = 0;
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int jnum = numneigh[i];
        if (jnum > numneighMax_) numneighMax_ = jnum;
    }
    numneighMax[0] = numneighMax_;
    return 0;
}

__jsefunc__ int jse_nnap_computeLammps(
    int inum, int ntypes, int eflag, int vflag, int eflagAtom, int vflagAtom, int cvflagAtom,
    double **x, double **f, int *type, int *ilist,
    int *numneigh, int **firstneigh, double *cutsq,
    int *aLmpType2NNAPType, int **rTypeIlist, int *rTypeInum,
    double *engVdwl, double *eatom, double *virial, double **vatom, double **cvatom,
    JSE_NNAP::flt_t *rNlDx, JSE_NNAP::flt_t *rNlDy, JSE_NNAP::flt_t *rNlDz, int *rNlType, int *rNlIdx,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t **aNormParam,
    JSE_NNAP::flt_t *rGradNlDx, JSE_NNAP::flt_t *rGradNlDy, JSE_NNAP::flt_t *rGradNlDz,
    JSE_NNAP::flt_t *rFpForwardCache) {
    
    /// reorder by types
    for (int typei = 1; typei <= ntypes; ++typei) {
        rTypeInum[typei] = 0;
    }
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int typei = type[i];
        rTypeIlist[typei][rTypeInum[typei]] = i;
        ++rTypeInum[typei];
    }
    
    /// begin compute here
    for (int typei = 1; typei <= ntypes; ++typei) {
        int *subIlist = rTypeIlist[typei];
        int subInum = rTypeInum[typei];
        
        const int typeiNNAP = aLmpType2NNAPType[typei];
        JSE_NNAP::flt_t *subNormParam = aNormParam[typeiNNAP-1];
        
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
                    rNlDx[tNeiNum] = (JSE_NNAP::flt_t)delx;
                    rNlDy[tNeiNum] = (JSE_NNAP::flt_t)dely;
                    rNlDz[tNeiNum] = (JSE_NNAP::flt_t)delz;
                    rNlType[tNeiNum] = aLmpType2NNAPType[type[j]];
                    rNlIdx[tNeiNum] = j;
                    ++tNeiNum;
                }
            }
            
            /// begin nnap here
            JSE_NNAP::flt_t rEng;
            // manual clear required for backward in force
            for (int j = 0; j < tNeiNum; ++j) {
                rGradNlDx[j] = JSE_NNAP::ZERO;
                rGradNlDy[j] = JSE_NNAP::ZERO;
                rGradNlDz[j] = JSE_NNAP::ZERO;
            }
            int code;
// >>> NNAPGEN SWITCH
            JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
            code = JSE_NNAP::fpForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
                rNlDx, rNlDy, rNlDz, rNlType, tNeiNum, typeiNNAP, rLayers,
                aFpHyperParam, aFpParam, rFpForwardCache
            );
            if (code!=0) return code;
            JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
            code = JSE_NNAP::normedNnForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
                typeiNNAP, &rEng, rLayers,
                subNormParam, aNnParam, rNnGradCache, NULL
            );
            if (code!=0) return code;
            // manual clear required for backward in force
            JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rLayers, JSE_NNAP::ZERO);
            code = JSE_NNAP::normedNnBackward<__NNAPGENS_typeiNNAP__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
                typeiNNAP, JSE_NNAP::ONE, NULL, rLayers, NULL,
                subNormParam, aNnParam, NULL, rNnGradCache
            );
            if (code!=0) return code;
            code = JSE_NNAP::fpBackward<__NNAPGENS_typeiNNAP__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
                rNlDx, rNlDy, rNlDz, rNlType, tNeiNum, typeiNNAP, rLayers,
                rGradNlDx, rGradNlDy, rGradNlDz, aFpHyperParam,
                aFpParam, NULL, rFpForwardCache, NULL, NULL
            );
            if (code!=0) return code;
// <<< NNAPGEN SWITCH (typeiNNAP) [FP NN TYPE]
            
            /// collect results
            if (eflag) {
                *engVdwl += rEng;
                if (eflagAtom) eatom[i] += rEng;
            }
            for (int jj = 0; jj < tNeiNum; ++jj) {
                int j = rNlIdx[jj];
                const JSE_NNAP::flt_t fx = rGradNlDx[jj];
                const JSE_NNAP::flt_t fy = rGradNlDy[jj];
                const JSE_NNAP::flt_t fz = rGradNlDz[jj];
                f[i][0] -= fx;
                f[i][1] -= fy;
                f[i][2] -= fz;
                f[j][0] += fx;
                f[j][1] += fy;
                f[j][2] += fz;
                if (vflag) {
                    const JSE_NNAP::flt_t dx = rNlDx[jj];
                    const JSE_NNAP::flt_t dy = rNlDy[jj];
                    const JSE_NNAP::flt_t dz = rNlDz[jj];
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

