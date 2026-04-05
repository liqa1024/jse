#include "nnap_interface.h"
#include "nnap_main.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_typeiNNAP__ 1
// <<< NNAPGEN REMOVE

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
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFp,
        tFpHyperParam, tFpParam, NULL
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
    JSE_NNAP::flt_t *rGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *rGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    // manual clear required for backward in force
    for (int j = 0; j < tNeiNum; ++j) {
        rGradNlDx[j] = JSE_NNAP::ZERO;
        rGradNlDy[j] = JSE_NNAP::ZERO;
        rGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHEG__];
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        ctype, rFpOrGradFp, tNormParam, tNnParam, rNnGradCache, rOutEng
    );
    if (code!=0) return code;
    // manual clear required for backward in force
    JSE_NNAP::fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, JSE_NNAP::ZERO);
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__>(
        ctype, rFpOrGradFp, tNormParam, tNnParam, rNnGradCache, JSE_NNAP::ONE
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        tFpHyperParam, tFpParam, rFpForwardCache, NULL
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
    
    JSE_NNAP::flt_t *rGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *rGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    
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
    
    double *engVdwl = (double *)tDataOut[5];
    double *eatom = (double *)tDataOut[6];
    double *virial = (double *)tDataOut[7];
    double **vatom = (double **)tDataOut[8];
    double **cvatom = (double **)tDataOut[9];
    
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
                rGradNlDx[j] = JSE_NNAP::ZERO;
                rGradNlDy[j] = JSE_NNAP::ZERO;
                rGradNlDz[j] = JSE_NNAP::ZERO;
            }
            int code;
// >>> NNAPGEN SWITCH
            JSE_NNAP::flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
            code = JSE_NNAP::fpForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFpOrGradFp,
                tFpHyperParam, tFpParam, rFpForwardCache
            );
            if (code!=0) return code;
            JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHEG__];
            code = JSE_NNAP::normedNnForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
                typeiNNAP, rFpOrGradFp, subNormParam, tNnParam, rNnGradCache, &rEng
            );
            if (code!=0) return code;
            // manual clear required for backward in force
            JSE_NNAP::fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, JSE_NNAP::ZERO);
            code = JSE_NNAP::normedNnBackward<__NNAPGENS_typeiNNAP__>(
                typeiNNAP, rFpOrGradFp, subNormParam, tNnParam, rNnGradCache, JSE_NNAP::ONE
            );
            if (code!=0) return code;
            code = JSE_NNAP::fpBackward<__NNAPGENS_typeiNNAP__, JSE_NNAP::FALSE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFpOrGradFp,
                rGradNlDx, rGradNlDy, rGradNlDz,
                tFpHyperParam, tFpParam, rFpForwardCache, NULL
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

