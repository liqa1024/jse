#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGENX_FP_WTYPE JSE_NNAP::WTYPE_DEFAULT
#define NNAPGENX_FP_NMAX 5
#define NNAPGENX_FP_FSIZE 0
#define NNAPGENX_FP_FSTYLE JSE_NNAP::FSTYLE_LIMITED
#define NNAPGENX_FP_SIZE 10
// #define NNAPGENX_FP_SIZE_FW 0
// #define NNAPGENX_FP_CACHE_SIZE_F0 (5+1)
#define NNAPGENX_NN_SIZE_HW (10*32 + 32*24)
#define NNAPGENX_NN_SIZE_HB (32 + 24)
#define NNAPGENX_NN_SIZE_OW 24
#define NNAPGENS_ctype 1
// <<< NNAPGEN REMOVE

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_forward(void *aDataIn, void *rDataOut) {
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
    
    double *rOut = (double *)tDataOut[0];
    double *rFp = (double *)tDataOut[1];
    double *rFpCache = (double *)tDataOut[2];
    double *rNnCache = (double *)tDataOut[3];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    double tRCut = tFpParam[0];
    
    double tNormMuEng;
    double tNormSigmaEng;
    int flag = 1;
// >>> NNAPGEN SWITCH
    double *tNormMu = tNormParam;
    double *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    tNormMuEng = tNormSigma[NNAPGENX_FP_SIZE];
    tNormSigmaEng = tNormSigma[NNAPGENX_FP_SIZE+1];
    
    double *tFuseWeight = tFpParam + 1;
    JSE_NNAP::chebyForward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, NNAPGENX_FP_SIZE, JSE_NNAP::CL_NONE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, rFp,
        rFpCache, tRCut, tFuseWeight
    );
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (ctype) [FP TYPE]
    if (flag) return 1;
    
// >>> NNAPGEN SWITCH
    double *tHiddenWeights = tNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    double *tHiddenBiases = tOutputWeights + NNAPGENX_NN_SIZE_OW;
    double tOutputBias = tHiddenBiases[NNAPGENX_NN_SIZE_HB];
    double tEng = JSE_NNAP::nnForward<NNAPGENS_ctype>(rFp, tHiddenWeights, tHiddenBiases, tOutputWeights, tOutputBias, rNnCache);
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOut = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (ctype) [NN TYPE]
    if (flag) return 1;
    
    return 0;
}

}


