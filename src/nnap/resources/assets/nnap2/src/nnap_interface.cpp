#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGENX_FP_WTYPE JSE_NNAP::WTYPE_DEFAULT
#define NNAPGENX_FP_NMAX 5
#define NNAPGENX_FP_FSIZE 0
#define NNAPGENX_FP_FSTYLE JSE_NNAP::FSTYLE_LIMITED
#define NNAPGENX_FP_SIZE 10
#define NNAPGENX_NN_SIZE_IN 10
#define NNAPGENX_NN_SIZE_HW (10*32)
#define NNAPGENX_NN_SIZE_HB 32
#define NNAPGENX_NN_SIZE_OW 32
#define NNAPGENS_aCType 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int FP_FULL_CACHE, int NN_CACHE_GRAD>
static int forward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, int aCType,
                   double *aFpParam, double *aNnParam, double *aNormParam,
                   double *rFpCache, double *rNnCache, double *rOutEng) noexcept {
    double tNormMuEng = aNormParam[0];
    double tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    double *tNormMu = aNormParam + 2;
    double *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    double *rFp = rNnCache; // fp from nn cache, for smooth input
    chebyForward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, NNAPGENX_FP_SIZE, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rFp,
        rFpCache, aFpParam[0], aFpParam+1
    );
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;
    
// >>> NNAPGEN SWITCH
    double *tHiddenWeights = aNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    double *tHiddenBiases = tOutputWeights + NNAPGENX_NN_SIZE_OW;
    double tOutputBias = tHiddenBiases[NNAPGENX_NN_SIZE_HB];
    double *rLayers = rNnCache; // first layer is fp
    double *rSiLUGrad = NN_CACHE_GRAD ? (rLayers+(NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB)) : NULL;
    double tEng = nnForward<NNAPGENS_aCType, NN_CACHE_GRAD>(rLayers, tHiddenWeights, tHiddenBiases, tOutputWeights, tOutputBias, rSiLUGrad);
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOutEng = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;
    
    return 0;
}


template <int FP_FULL_CACHE, int CLEAR_CACHE>
static int backward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, int aCType,
                    double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                    double *aFpParam, double *aNnParam, double *aNormParam,
                    double *rFpCache, double *rNnCache, double aInGradEng) noexcept {
    double tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    double *tHiddenWeights = aNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    double *rGradLayers = rNnCache; // first layer is fp
    double *tSiLUGrad = rGradLayers + (NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB);
    nnBackward<NNAPGENS_aCType, CLEAR_CACHE>(aInGradEng, rGradLayers, tHiddenWeights, tOutputWeights, tSiLUGrad);
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;

// >>> NNAPGEN SWITCH
    double *tNormMu = aNormParam + 2;
    double *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    double *tGradFp = rNnCache; // fp from nn cache, for smooth input
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        tGradFp[i] /= tNormSigma[i];
    }
    chebyBackward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        rFpCache, aFpParam[0], aFpParam+1
    );
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
    double *tNlDx = (double *)tDataIn[1];
    double *tNlDy = (double *)tDataIn[2];
    double *tNlDz = (double *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    double *tFpParam = (double *)tDataIn[5];
    double *tNnParam = (double *)tDataIn[6];
    double *tNormParam = (double *)tDataIn[7];
    
    double *rOutEng = (double *)tDataOut[0];
    double *rFpCache = (double *)tDataOut[1];
    double *rNnCache = (double *)tDataOut[2];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    return JSE_NNAP::forward<JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, rOutEng
    );
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    double *tNlDx = (double *)tDataIn[1];
    double *tNlDy = (double *)tDataIn[2];
    double *tNlDz = (double *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    double *tFpParam = (double *)tDataIn[5];
    double *tNnParam = (double *)tDataIn[6];
    double *tNormParam = (double *)tDataIn[7];
    
    double *rOutEng = (double *)tDataOut[0];
    double *tGradNlDx = (double *)tDataOut[1];
    double *tGradNlDy = (double *)tDataOut[2];
    double *tGradNlDz = (double *)tDataOut[3];
    double *rFpCache = (double *)tDataOut[4];
    double *rNnCache = (double *)tDataOut[5];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, rOutEng
    );
    if (code!=0) return code;
    
    // manual clear required for backward in force
    return JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tGradNlDx, tGradNlDy, tGradNlDz,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, 1.0
    );
}

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    double *tNlDxBuf = (double *)tDataIn[1];
    double *tNlDyBuf = (double *)tDataIn[2];
    double *tNlDzBuf = (double *)tDataIn[3];
    int *tNlTypeBuf = (int *)tDataIn[4];
    int *tNlIdxBuf = (int *)tDataIn[5];
    double **tFpParam = (double **)tDataIn[6];
    double **tNnParam = (double **)tDataIn[7];
    double **tNormParam = (double **)tDataIn[8];
    
    double *tGradNlDxBuf = (double *)tDataOut[1];
    double *tGradNlDyBuf = (double *)tDataOut[2];
    double *tGradNlDzBuf = (double *)tDataOut[3];
    double *rFpCacheBuf = (double *)tDataOut[4];
    double **rNnCacheBuf = (double **)tDataOut[5];
    
    int numneighMax = tNums[0];
    int inum = tNums[1];
    int eflag = tNums[2];
    int vflag = tNums[3];
    int eflagAtom = tNums[4];
    int vflagAtom = tNums[5];
    int cvflagAtom = tNums[6];
    
    double **x = (double **)tDataIn[9];
    double **f = (double **)tDataOut[0];
    int *type = (int *)tDataIn[10];
    
    int *ilist = (int *)tDataIn[11];
    int *numneigh = (int *)tDataIn[12];
    int **firstneigh = (int **)tDataIn[13];
    double *cutsq = (double *)tDataIn[14];
    int *tLmpType2NNAPType = (int *)tDataIn[15];
    
    double *engVdwl = (double *)tDataOut[6];
    double *eatom = (double *)tDataOut[7];
    double *virial = (double *)tDataOut[8];
    double **vatom = (double **)tDataOut[9];
    double **cvatom = (double **)tDataOut[10];
    
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
                tNlDxBuf[tNeiNum] = delx;
                tNlDyBuf[tNeiNum] = dely;
                tNlDzBuf[tNeiNum] = delz;
                tNlTypeBuf[tNeiNum] = tLmpType2NNAPType[type[j]];
                tNlIdxBuf[tNeiNum] = j;
                ++tNeiNum;
            }
        }
        int typeiNNAP = tLmpType2NNAPType[typei];
        
        /// begin nnap here
        double rEng;
        int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
            tNlDxBuf, tNlDyBuf, tNlDzBuf, tNlTypeBuf, tNeiNum, typeiNNAP,
            tFpParam[typeiNNAP-1], tNnParam[typeiNNAP-1], tNormParam[typeiNNAP-1],
            rFpCacheBuf, rNnCacheBuf[typeiNNAP-1], &rEng
        );
        if (code!=0) return code;
        // manual clear required for backward in force
        code = JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
            tNlDxBuf, tNlDyBuf, tNlDzBuf, tNlTypeBuf, tNeiNum, typeiNNAP,
            tGradNlDxBuf, tGradNlDyBuf, tGradNlDzBuf,
            tFpParam[typeiNNAP-1], tNnParam[typeiNNAP-1], tNormParam[typeiNNAP-1],
            rFpCacheBuf, rNnCacheBuf[typeiNNAP-1], 1.0
        );
        if (code!=0) return code;
        
        /// collect results
        if (eflag) {
            *engVdwl += rEng;
            if (eflagAtom) eatom[i] += rEng;
        }
        for (int jj = 0; jj < tNeiNum; ++jj) {
            int j = tNlIdxBuf[jj];
            double fx = tGradNlDxBuf[jj];
            double fy = tGradNlDyBuf[jj];
            double fz = tGradNlDzBuf[jj];
            f[i][0] -= fx;
            f[i][1] -= fy;
            f[i][2] -= fz;
            f[j][0] += fx;
            f[j][1] += fy;
            f[j][2] += fz;
            if (vflag) {
                double dx = tNlDxBuf[jj];
                double dy = tNlDyBuf[jj];
                double dz = tNlDzBuf[jj];
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


